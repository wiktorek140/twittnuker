/*
 * Twittnuker - Twitter client for Android
 *
 * Copyright (C) 2013-2016 vanita5 <mail@vanit.as>
 *
 * This program incorporates a modified version of Twidere.
 * Copyright (C) 2012-2016 Mariotaku Lee <mariotaku.lee@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.vanita5.twittnuker.fragment

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.support.v4.content.Loader
import com.squareup.otto.Subscribe
import kotlinx.android.synthetic.main.fragment_content_recyclerview.*
import org.mariotaku.sqliteqb.library.ArgsArray
import org.mariotaku.sqliteqb.library.Columns.Column
import org.mariotaku.sqliteqb.library.Expression
import de.vanita5.twittnuker.R
import de.vanita5.twittnuker.activity.HomeActivity
import de.vanita5.twittnuker.adapter.ListParcelableStatusesAdapter
import de.vanita5.twittnuker.adapter.ParcelableStatusesAdapter
import de.vanita5.twittnuker.adapter.iface.ILoadMoreSupportAdapter
import de.vanita5.twittnuker.adapter.iface.ILoadMoreSupportAdapter.IndicatorPosition
import de.vanita5.twittnuker.constant.IntentConstants.EXTRA_FROM_USER
import de.vanita5.twittnuker.loader.ExtendedObjectCursorLoader
import de.vanita5.twittnuker.model.*
import de.vanita5.twittnuker.model.message.*
import de.vanita5.twittnuker.provider.TwidereDataStore.*
import de.vanita5.twittnuker.util.DataStoreUtils
import de.vanita5.twittnuker.util.DataStoreUtils.buildStatusFilterWhereClause
import de.vanita5.twittnuker.util.DataStoreUtils.getTableNameByUri
import de.vanita5.twittnuker.util.ErrorInfoStore
import de.vanita5.twittnuker.util.Utils

abstract class CursorStatusesFragment : AbsStatusesFragment() {

    private var contentObserver: ContentObserver? = null

    abstract val errorInfoKey: String
    abstract val isFilterEnabled: Boolean
    abstract val notificationType: Int
    abstract val contentUri: Uri
    override var refreshing: Boolean
        get() = swipeLayout.isRefreshing
        set(value) {
            super.refreshing = value
        }

    override fun onStatusesLoaded(loader: Loader<List<ParcelableStatus>?>, data: List<ParcelableStatus>?) {
        showContentOrError()
    }

    override fun onCreateStatusesLoader(context: Context,
                                        args: Bundle,
                                        fromUser: Boolean): Loader<List<ParcelableStatus>?> {
        val uri = contentUri
        val table = getTableNameByUri(uri)
        val sortOrder = Statuses.DEFAULT_SORT_ORDER
        val accountKeys = accountKeys
        val accountWhere = Expression.`in`(Column(Statuses.ACCOUNT_KEY),
                ArgsArray(accountKeys.size))
        val filterWhere = getFiltersWhere(table)
        val where: Expression
        if (filterWhere != null) {
            where = Expression.and(accountWhere, filterWhere)
        } else {
            where = accountWhere
        }
        val adapter = adapter
        adapter!!.showAccountsColor = accountKeys.size > 1
        val projection = Statuses.COLUMNS
        val selectionArgs = Array(accountKeys.size) {
            accountKeys[it].toString()
        }
        val expression = processWhere(where, selectionArgs)
        return ExtendedObjectCursorLoader(context, ParcelableStatusCursorIndices::class.java, uri,
                projection, expression.sql, expression.parameters,
                sortOrder, fromUser)
    }

    override fun createMessageBusCallback(): Any {
        return CursorStatusesBusCallback()
    }


    private fun showContentOrError() {
        val accountKeys = accountKeys
        val adapter = adapter
        if (adapter!!.itemCount > 0) {
            showContent()
        } else if (accountKeys.size > 0) {
            val errorInfo = ErrorInfoStore.getErrorInfo(context,
                    errorInfoStore.get(errorInfoKey, accountKeys[0]))
            if (errorInfo != null) {
                showEmpty(errorInfo.icon, errorInfo.message)
            } else {
                showEmpty(R.drawable.ic_info_refresh, getString(R.string.swipe_down_to_refresh))
            }
        } else {
            showError(R.drawable.ic_info_accounts, getString(R.string.no_account_selected))
        }
    }

    protected inner class CursorStatusesBusCallback {

        @Subscribe
        fun notifyGetStatusesTaskChanged(event: GetStatusesTaskEvent) {
            if (event.uri != contentUri) return
            refreshing = event.running
            if (!event.running) {
                setLoadMoreIndicatorPosition(ILoadMoreSupportAdapter.NONE)
                refreshEnabled = true
                showContentOrError()
            }
        }


        @Subscribe
        fun notifyFavoriteTask(event: FavoriteTaskEvent) {
            if (event.isSucceeded) {
                val status = event.status
                val data = adapterData
                if (status == null || data == null || data.isEmpty()) return
                val adapter = adapter as ParcelableStatusesAdapter
                val firstVisiblePosition = layoutManager!!.findFirstVisibleItemPosition()
                val lastVisiblePosition = layoutManager!!.findLastVisibleItemPosition()
                val startIndex = adapter.statusStartIndex
                for (i in firstVisiblePosition..lastVisiblePosition) {
                    if (status.account_key == adapter.getAccountKey(i) && status.id == adapter.getStatusId(i)) {
                        if (data is MutableList) {
                            data[i - startIndex] = status
                        }
                        return
                    }
                }
                adapter.notifyDataSetChanged()
            }
        }

        @Subscribe
        fun notifyStatusDestroyed(event: StatusDestroyedEvent) {
        }

        @Subscribe
        fun notifyStatusListChanged(event: StatusListChangedEvent) {
            adapter!!.notifyDataSetChanged()
        }

        @Subscribe
        fun notifyStatusRetweeted(event: StatusRetweetedEvent) {
        }

        @Subscribe
        fun notifyAccountChanged(event: AccountChangedEvent) {

        }

    }

    override val accountKeys: Array<UserKey>
        get() {
            val args = arguments
            val context = context
            val accountKeys = Utils.getAccountKeys(context, args)
            if (accountKeys != null) {
                return accountKeys
            }
            if (context is HomeActivity) {
                return context.activatedAccountKeys
            }
            return DataStoreUtils.getActivatedAccountKeys(context)
        }

    override fun onStart() {
        super.onStart()
        val cr = contentResolver
        contentObserver = object : ContentObserver(Handler()) {
            override fun onChange(selfChange: Boolean) {
                reloadStatuses()
            }
        }
        cr!!.registerContentObserver(Accounts.CONTENT_URI, true, contentObserver!!)
        cr.registerContentObserver(Filters.CONTENT_URI, true, contentObserver!!)
        updateRefreshState()
        reloadStatuses()
    }

    protected fun reloadStatuses() {
        if (context == null || isDetached) return
        val args = Bundle()
        val fragmentArgs = arguments
        if (fragmentArgs != null) {
            args.putAll(fragmentArgs)
            args.putBoolean(EXTRA_FROM_USER, true)
        }
        loaderManager.restartLoader(0, args, this)
    }

    override fun onStop() {
        contentResolver?.unregisterContentObserver(contentObserver!!)
        super.onStop()
    }

    override fun hasMoreData(data: List<ParcelableStatus>?): Boolean {
        return data?.size != 0
    }

    override fun onCreateAdapter(context: Context): ListParcelableStatusesAdapter {
        return ListParcelableStatusesAdapter(context)
    }

    override fun onLoaderReset(loader: Loader<List<ParcelableStatus>?>) {
        adapter!!.setData(null)
    }

    override fun onLoadMoreContents(@IndicatorPosition position: Long) {
        // Only supports load from end, skip START flag
        if (position and ILoadMoreSupportAdapter.START !== 0L) return
        super.onLoadMoreContents(position.toLong())
        if (position == 0L) return
        getStatuses(object : SimpleRefreshTaskParam() {
            override fun getAccountKeysWorker(): Array<UserKey> {
                return this@CursorStatusesFragment.accountKeys
            }

            override fun getMaxIds(): Array<String>? {
                return getOldestStatusIds(accountKeys)
            }

            override fun getMaxSortIds(): LongArray? {
                val context = context ?: return null
                return DataStoreUtils.getOldestStatusSortIds(context, contentUri,
                        accountKeys)
            }

            override fun hasMaxIds(): Boolean {
                return true
            }

            override fun shouldAbort(): Boolean {
                return context == null
            }
        })
    }

    override fun triggerRefresh(): Boolean {
        super.triggerRefresh()
        getStatuses(object : SimpleRefreshTaskParam() {
            override fun getAccountKeysWorker(): Array<UserKey> {
                return this@CursorStatusesFragment.accountKeys
            }

            override fun hasMaxIds(): Boolean {
                return false
            }

            override fun getSinceIds(): Array<String>? {
                return getNewestStatusIds(accountKeys)
            }

            override fun getSinceSortIds(): LongArray? {
                val context = context ?: return null
                return DataStoreUtils.getNewestStatusSortIds(context, contentUri,
                        accountKeys)
            }

            override fun shouldAbort(): Boolean {
                return context == null
            }
        })
        return true
    }

    protected fun getFiltersWhere(table: String): Expression? {
        if (!isFilterEnabled) return null
        return buildStatusFilterWhereClause(table, null)
    }

    protected fun getNewestStatusIds(accountKeys: Array<UserKey>): Array<String>? {
        val context = context ?: return null
        return DataStoreUtils.getNewestStatusIds(context, contentUri, accountKeys)
    }


    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        val context = context
        if (context != null && isVisibleToUser) {
            for (accountId in accountKeys) {
                twitterWrapper.clearNotificationAsync(notificationType, accountId)
            }
        }
    }

    protected fun getOldestStatusIds(accountKeys: Array<UserKey>): Array<String>? {
        val context = context ?: return null
        return DataStoreUtils.getOldestStatusIds(context, contentUri, accountKeys)
    }

    protected open fun processWhere(where: Expression, whereArgs: Array<String>): ParameterizedExpression {
        return ParameterizedExpression(where, whereArgs)
    }

    protected abstract fun updateRefreshState()

}