/*
 * Twittnuker - Twitter client for Android
 *
 * Copyright (C) 2013-2017 vanita5 <mail@vanit.as>
 *
 * This program incorporates a modified version of Twidere.
 * Copyright (C) 2012-2017 Mariotaku Lee <mariotaku.lee@gmail.com>
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

package de.vanita5.twittnuker.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Rect
import android.net.Uri
import android.os.BadParcelableException
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.NavUtils
import android.support.v4.view.WindowCompat
import android.support.v7.widget.Toolbar
import android.text.TextUtils
import android.view.KeyEvent
import android.view.MenuItem
import android.view.View
import android.view.Window
import kotlinx.android.synthetic.main.activity_link_handler.*
import org.mariotaku.kpreferences.get
import org.mariotaku.ktextension.set
import org.mariotaku.ktextension.toDouble
import de.vanita5.twittnuker.Constants.*
import de.vanita5.twittnuker.R
import de.vanita5.twittnuker.activity.iface.IControlBarActivity
import de.vanita5.twittnuker.activity.iface.IControlBarActivity.ControlBarShowHideHelper
import de.vanita5.twittnuker.constant.*
import de.vanita5.twittnuker.fragment.*
import de.vanita5.twittnuker.fragment.filter.FiltersFragment
import de.vanita5.twittnuker.fragment.filter.FiltersImportBlocksFragment
import de.vanita5.twittnuker.fragment.filter.FiltersImportMutesFragment
import de.vanita5.twittnuker.fragment.filter.FiltersSubscriptionsFragment
import de.vanita5.twittnuker.fragment.iface.IBaseFragment
import de.vanita5.twittnuker.fragment.iface.IBaseFragment.SystemWindowsInsetsCallback
import de.vanita5.twittnuker.fragment.iface.IToolBarSupportFragment
import de.vanita5.twittnuker.fragment.iface.SupportFragmentCallback
import de.vanita5.twittnuker.fragment.message.MessageConversationInfoFragment
import de.vanita5.twittnuker.fragment.message.MessageNewConversationFragment
import de.vanita5.twittnuker.fragment.message.MessagesConversationFragment
import de.vanita5.twittnuker.fragment.message.MessagesEntriesFragment
import de.vanita5.twittnuker.graphic.EmptyDrawable
import de.vanita5.twittnuker.model.UserKey
import de.vanita5.twittnuker.model.analyzer.PurchaseFinished
import de.vanita5.twittnuker.util.*
import de.vanita5.twittnuker.util.KeyboardShortcutsHandler.KeyboardShortcutCallback
import de.vanita5.twittnuker.util.linkhandler.TwidereLinkMatcher
import de.vanita5.twittnuker.util.theme.getCurrentThemeResource

class LinkHandlerActivity : BaseActivity(), SystemWindowsInsetsCallback, IControlBarActivity,
        SupportFragmentCallback {

    private lateinit var multiSelectHandler: MultiSelectEventHandler
    private lateinit var controlBarShowHideHelper: ControlBarShowHideHelper
    private var finishOnly: Boolean = false
    private var actionBarHeight: Int = 0
    private var subtitle: CharSequence? = null
    private var hideOffsetNotSupported: Boolean = false

    override val currentVisibleFragment: Fragment?
        get() = supportFragmentManager.findFragmentByTag("content_fragment")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        multiSelectHandler = MultiSelectEventHandler(this)
        controlBarShowHideHelper = ControlBarShowHideHelper(this)
        multiSelectHandler.dispatchOnCreate()

        val uri = intent.data ?: run {
            finish()
            return
        }
        val linkId = TwidereLinkMatcher.match(uri)
        intent.setExtrasClassLoader(classLoader)
        val fragment: Fragment
        try {
            fragment = createFragmentForIntent(this, linkId, intent) ?: run {
                finish()
                return
            }
        } catch (e: Utils.NoAccountException) {
            val selectIntent = Intent(this, AccountSelectorActivity::class.java)
            val accountHost: String? = intent.getStringExtra(EXTRA_ACCOUNT_HOST) ?:
                    uri.getQueryParameter(QUERY_PARAM_ACCOUNT_HOST) ?: e.accountHost
            selectIntent.putExtra(EXTRA_SINGLE_SELECTION, true)
            selectIntent.putExtra(EXTRA_SELECT_ONLY_ITEM_AUTOMATICALLY, true)
            selectIntent.putExtra(EXTRA_ACCOUNT_HOST, accountHost)
            selectIntent.putExtra(EXTRA_START_INTENT, intent)
            startActivity(selectIntent)
            finish()
            return
        }

        val contentFragmentId: Int

        if (fragment is IToolBarSupportFragment) {
            if (!fragment.setupWindow(this)) {
                supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
                supportRequestWindowFeature(WindowCompat.FEATURE_ACTION_MODE_OVERLAY)
            }
            contentFragmentId = android.R.id.content
        } else {
            setContentView(R.layout.activity_link_handler)
            toolbar?.let { toolbar ->
                if (supportActionBar != null) {
                    toolbar.visibility = View.GONE
                    windowOverlay?.visibility = View.GONE
                } else {
                    toolbar.visibility = View.VISIBLE
                    windowOverlay?.visibility = View.VISIBLE
                    setSupportActionBar(toolbar)
                }
            }
            contentFragmentId = R.id.contentFragment
        }

        setupActionBarOption()
        val ft = supportFragmentManager.beginTransaction()
        ft.replace(contentFragmentId, fragment, "content_fragment")
        ft.commit()
        setTitle(linkId, uri)
        finishOnly = uri.getQueryParameter(QUERY_PARAM_FINISH_ONLY)?.toBoolean() ?: false

        if (fragment is IToolBarSupportFragment) {
            ThemeUtils.setCompatContentViewOverlay(window, EmptyDrawable())
        }
    }

    override fun onStart() {
        super.onStart()
        multiSelectHandler.dispatchOnStart()
    }


    override fun onStop() {
        multiSelectHandler.dispatchOnStop()
        super.onStop()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_PURCHASE_EXTRA_FEATURES -> {
                if (resultCode == Activity.RESULT_OK) {
                    Analyzer.log(PurchaseFinished.create(data!!))
                }
            }
            else -> {
                super.onActivityResult(requestCode, resultCode, data)
            }
        }

    }

    override fun triggerRefresh(position: Int): Boolean {
        return false
    }

    override fun onFitSystemWindows(insets: Rect) {
        super.onFitSystemWindows(insets)
        val fragment = currentVisibleFragment
        if (fragment is IBaseFragment<*>) {
            fragment.requestFitSystemWindows()
        }
    }

    override fun getSystemWindowsInsets(insets: Rect): Boolean {
        return super.getSystemWindowsInsets(insets)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                if (finishOnly) {
                    finish()
                } else {
                    NavUtils.navigateUpFromSameTask(this)
                }
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun handleKeyboardShortcutSingle(handler: KeyboardShortcutsHandler, keyCode: Int, event: KeyEvent, metaState: Int): Boolean {
        if (shouldFragmentTakeAllKeyboardShortcuts()) {
            return handleFragmentKeyboardShortcutSingle(handler, keyCode, event, metaState)
        }
        if (handleFragmentKeyboardShortcutSingle(handler, keyCode, event, metaState)) return true
        val action = handler.getKeyAction(KeyboardShortcutConstants.CONTEXT_TAG_NAVIGATION, keyCode, event, metaState)
        if (KeyboardShortcutConstants.ACTION_NAVIGATION_BACK == action) {
            onBackPressed()
            return true
        }
        return handler.handleKey(this, null, keyCode, event, metaState)
    }

    private fun shouldFragmentTakeAllKeyboardShortcuts(): Boolean {
        val fragment = currentVisibleFragment
        return fragment is KeyboardShortcutsHandler.TakeAllKeyboardShortcut
    }

    override fun handleKeyboardShortcutRepeat(handler: KeyboardShortcutsHandler, keyCode: Int, repeatCount: Int, event: KeyEvent, metaState: Int): Boolean {
        if (shouldFragmentTakeAllKeyboardShortcuts()) {
            handleFragmentKeyboardShortcutRepeat(handler, keyCode, repeatCount, event, metaState)
        }
        if (handleFragmentKeyboardShortcutRepeat(handler, keyCode, repeatCount, event, metaState))
            return true
        return super.handleKeyboardShortcutRepeat(handler, keyCode, repeatCount, event, metaState)
    }

    override fun isKeyboardShortcutHandled(handler: KeyboardShortcutsHandler, keyCode: Int, event: KeyEvent, metaState: Int): Boolean {
        if (isFragmentKeyboardShortcutHandled(handler, keyCode, event, metaState)) return true
        return super.isKeyboardShortcutHandled(handler, keyCode, event, metaState)
    }

    private fun isFragmentKeyboardShortcutHandled(handler: KeyboardShortcutsHandler,
                                                  keyCode: Int, event: KeyEvent, metaState: Int): Boolean {
        val fragment = currentVisibleFragment
        if (fragment is KeyboardShortcutCallback) {
            return fragment.isKeyboardShortcutHandled(handler, keyCode, event, metaState)
        }
        return false
    }

    override fun setSupportActionBar(toolbar: Toolbar?) {
        super.setSupportActionBar(toolbar)
        setupActionBarOption()
    }

    fun setSubtitle(subtitle: CharSequence?) {
        this.subtitle = subtitle
        setupActionBarOption()
    }

    override fun setControlBarVisibleAnimate(visible: Boolean, listener: ControlBarShowHideHelper.ControlBarAnimationListener?) {
        // Currently only search page needs this pattern, so we only enable this feature for it.
        if (currentVisibleFragment !is HideUiOnScroll) return
        controlBarShowHideHelper.setControlBarVisibleAnimate(visible, listener)
    }

    override var controlBarOffset: Float
        get() {
            val fragment = currentVisibleFragment
            val actionBar = supportActionBar
            if (fragment is IToolBarSupportFragment) {
                return fragment.controlBarOffset
            } else if (actionBar != null) {
                return actionBar.hideOffset / controlBarHeight.toFloat()
            }
            return 0f
        }
        set(offset) {
            val fragment = currentVisibleFragment
            val actionBar = supportActionBar
            if (fragment is IToolBarSupportFragment) {
                fragment.controlBarOffset = offset
            } else if (actionBar != null && !hideOffsetNotSupported) {
                try {
                    actionBar.hideOffset = (controlBarHeight * offset).toInt()
                } catch (e: UnsupportedOperationException) {
                    // Some device will throw this exception
                    hideOffsetNotSupported = true
                }

            }
            notifyControlBarOffsetChanged()
        }

    override val controlBarHeight: Int
        get() {
            val fragment = currentVisibleFragment
            val actionBar = supportActionBar
            if (fragment is IToolBarSupportFragment) {
                return fragment.controlBarHeight
            } else if (actionBar != null) {
                return actionBar.height
            }
            if (actionBarHeight != 0) return actionBarHeight
            actionBarHeight = ThemeUtils.getActionBarHeight(this)
            return actionBarHeight
        }

    override fun getThemeResource(preferences: SharedPreferences, theme: String, themeColor: Int): Int {
        if (preferences[floatingDetailedContentsKey]) {
            return super.getThemeResource(preferences, theme, themeColor)
        }
        return getCurrentThemeResource(this, theme, R.style.Theme_Twidere)
    }

    private fun setTitle(linkId: Int, uri: Uri): Boolean {
        setSubtitle(null)
        when (linkId) {
            LINK_ID_STATUS -> {
                setTitle(R.string.title_status)
            }
            LINK_ID_USER -> {
                setTitle(R.string.title_user)
            }
            LINK_ID_USER_TIMELINE -> {
                setTitle(R.string.title_statuses)
            }
            LINK_ID_USER_FAVORITES -> {
                if (preferences.getBoolean(SharedPreferenceConstants.KEY_I_WANT_MY_STARS_BACK)) {
                    setTitle(R.string.title_favorites)
                } else {
                    setTitle(R.string.title_likes)
                }
            }
            LINK_ID_USER_FOLLOWERS -> {
                setTitle(R.string.title_followers)
            }
            LINK_ID_USER_FRIENDS -> {
                setTitle(R.string.title_following)
            }
            LINK_ID_USER_BLOCKS -> {
                setTitle(R.string.title_blocked_users)
            }
            LINK_ID_MUTES_USERS -> {
                setTitle(R.string.action_twitter_muted_users)
            }
            LINK_ID_USER_LIST -> {
                setTitle(R.string.user_list)
            }
            LINK_ID_GROUP -> {
                setTitle(R.string.group)
            }
            LINK_ID_USER_LISTS -> {
                setTitle(R.string.user_lists)
            }
            LINK_ID_USER_GROUPS -> {
                setTitle(R.string.groups)
            }
            LINK_ID_USER_LIST_TIMELINE -> {
                setTitle(R.string.list_timeline)
            }
            LINK_ID_USER_LIST_MEMBERS -> {
                setTitle(R.string.list_members)
            }
            LINK_ID_USER_LIST_SUBSCRIBERS -> {
                setTitle(R.string.list_subscribers)
            }
            LINK_ID_USER_LIST_MEMBERSHIPS -> {
                setTitle(R.string.lists_following_user)
            }
            LINK_ID_SAVED_SEARCHES -> {
                setTitle(R.string.saved_searches)
            }
            LINK_ID_USER_MENTIONS -> {
                setTitle(R.string.user_mentions)
            }
            LINK_ID_INCOMING_FRIENDSHIPS -> {
                setTitle(R.string.incoming_friendships)
            }
            LINK_ID_ITEMS -> {
            }// TODO show title
            LINK_ID_USER_MEDIA_TIMELINE -> {
                setTitle(R.string.media)
            }
            LINK_ID_STATUS_RETWEETERS -> {
                setTitle(R.string.title_users_retweeted_this)
            }
            LINK_ID_STATUS_FAVORITERS -> {
                if (preferences[iWantMyStarsBackKey]) {
                    setTitle(R.string.title_users_favorited_this)
                } else {
                    setTitle(R.string.title_users_liked_this)
                }
            }
            LINK_ID_SEARCH -> {
                setTitle(android.R.string.search_go)
                setSubtitle(uri.getQueryParameter(QUERY_PARAM_QUERY))
            }
            LINK_ID_ACCOUNTS -> {
                setTitle(R.string.title_accounts)
            }
            LINK_ID_DRAFTS -> {
                setTitle(R.string.title_drafts)
            }
            LINK_ID_FILTERS -> {
                setTitle(R.string.title_filters)
            }
            LINK_ID_MAP -> {
                setTitle(R.string.action_view_map)
            }
            LINK_ID_PROFILE_EDITOR -> {
                setTitle(R.string.title_edit_profile)
            }
            LINK_ID_SCHEDULED_STATUSES -> {
                title = getString(R.string.scheduled_statuses)
            }
            LINK_ID_MESSAGES -> {
                title = getString(R.string.title_direct_messages)
            }
            LINK_ID_MESSAGES_CONVERSATION -> {
                title = getString(R.string.title_direct_messages)
            }
            LINK_ID_MESSAGES_CONVERSATION_NEW -> {
                title = getString(R.string.title_direct_messages_conversation_new)
            }
            LINK_ID_MESSAGES_CONVERSATION_INFO -> {
                title = getString(R.string.title_direct_messages_conversation_info)
            }
            LINK_ID_INTERACTIONS -> {
                title = getString(R.string.interactions)
            }
            LINK_ID_PUBLIC_TIMELINE -> {
                title = getString(R.string.public_timeline)
            }
            LINK_ID_FILTERS_IMPORT_BLOCKS -> {
                title = getString(R.string.title_select_users)
            }
            LINK_ID_FILTERS_IMPORT_MUTES -> {
                title = getString(R.string.title_select_users)
            }
            LINK_ID_FILTERS_SUBSCRIPTIONS_ADD,
            LINK_ID_FILTERS_SUBSCRIPTIONS -> {
                title = getString(R.string.title_manage_filter_subscriptions)
            }
            else -> {
                title = getString(R.string.app_name)
            }
        }
        return true
    }

    private fun handleFragmentKeyboardShortcutRepeat(handler: KeyboardShortcutsHandler, keyCode: Int,
                                                     repeatCount: Int, event: KeyEvent, metaState: Int): Boolean {
        val fragment = currentVisibleFragment
        if (fragment is KeyboardShortcutCallback) {
            return fragment.handleKeyboardShortcutRepeat(handler, keyCode,
                    repeatCount, event, metaState)
        }
        return false
    }

    private fun handleFragmentKeyboardShortcutSingle(handler: KeyboardShortcutsHandler, keyCode: Int,
                                                     event: KeyEvent, metaState: Int): Boolean {
        val fragment = currentVisibleFragment
        if (fragment is KeyboardShortcutCallback) {
            if (fragment.handleKeyboardShortcutSingle(handler, keyCode,
                    event, metaState)) {
                return true
            }
        }
        return false
    }

    private fun setupActionBarOption() {
        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.subtitle = subtitle
        }
    }

    interface HideUiOnScroll


    @Throws(Utils.NoAccountException::class)
    fun createFragmentForIntent(context: Context, linkId: Int, intent: Intent): Fragment? {
        intent.setExtrasClassLoader(context.classLoader)
        val extras = intent.extras
        val uri = intent.data
        val fragment: Fragment
        if (uri == null) return null
        val args = Bundle()
        if (extras != null) {
            try {
                args.putAll(extras)
            } catch (e: BadParcelableException) {
                // When called by external app with wrong params
                return null
            }

        }
        var userHost: String? = null
        var accountRequired = true
        when (linkId) {
            LINK_ID_ACCOUNTS -> {
                accountRequired = false
                fragment = AccountsManagerFragment()
            }
            LINK_ID_DRAFTS -> {
                accountRequired = false
                fragment = DraftsFragment()
            }
            LINK_ID_FILTERS -> {
                accountRequired = false
                fragment = FiltersFragment()
            }
            LINK_ID_PROFILE_EDITOR -> {
                fragment = UserProfileEditorFragment()
            }
            LINK_ID_MAP -> {
                accountRequired = false
                if (!args.containsKey(EXTRA_LATITUDE) && !args.containsKey(EXTRA_LONGITUDE)) {
                    val lat = uri.getQueryParameter(QUERY_PARAM_LAT).toDouble(Double.NaN)
                    val lng = uri.getQueryParameter(QUERY_PARAM_LNG).toDouble(Double.NaN)
                    if (lat.isNaN() || lng.isNaN()) return null
                    args.putDouble(EXTRA_LATITUDE, lat)
                    args.putDouble(EXTRA_LONGITUDE, lng)
                }
                fragment = MapFragmentFactory.instance.createMapFragment(context)
            }
            LINK_ID_STATUS -> {
                fragment = StatusFragment()
                if (!args.containsKey(EXTRA_STATUS_ID)) {
                    val paramStatusId = uri.getQueryParameter(QUERY_PARAM_STATUS_ID)
                    args.putString(EXTRA_STATUS_ID, paramStatusId)
                }
            }
            LINK_ID_USER -> {
                fragment = UserFragment()
                val paramScreenName = uri.getQueryParameter(QUERY_PARAM_SCREEN_NAME)
                val paramUserKey = Utils.getUserKeyParam(uri)?.let(UserKey::valueOf) ?: args.getParcelable(EXTRA_USER_KEY)
                if (!args.containsKey(EXTRA_SCREEN_NAME)) {
                    args.putString(EXTRA_SCREEN_NAME, paramScreenName)
                }
                args.putParcelable(EXTRA_USER_KEY, paramUserKey)
                if (paramUserKey != null) {
                    userHost = paramUserKey.host
                }
                args.putString(EXTRA_REFERRAL, intent.getStringExtra(EXTRA_REFERRAL))
            }
            LINK_ID_USER_LIST_MEMBERSHIPS -> {
                fragment = UserListMembershipsFragment()
                val paramScreenName = uri.getQueryParameter(QUERY_PARAM_SCREEN_NAME)
                val paramUserKey = Utils.getUserKeyParam(uri)?.let(UserKey::valueOf)
                if (!args.containsKey(EXTRA_SCREEN_NAME)) {
                    args.putString(EXTRA_SCREEN_NAME, paramScreenName)
                }
                if (!args.containsKey(EXTRA_USER_KEY)) {
                    args.putParcelable(EXTRA_USER_KEY, paramUserKey)
                }
            }
            LINK_ID_USER_TIMELINE -> {
                fragment = UserTimelineFragment()
                val paramScreenName = uri.getQueryParameter(QUERY_PARAM_SCREEN_NAME)
                val paramUserKey = Utils.getUserKeyParam(uri)?.let(UserKey::valueOf)
                if (!args.containsKey(EXTRA_SCREEN_NAME)) {
                    args.putString(EXTRA_SCREEN_NAME, paramScreenName)
                }
                if (!args.containsKey(EXTRA_USER_KEY)) {
                    args.putParcelable(EXTRA_USER_KEY, paramUserKey)
                }
                if (TextUtils.isEmpty(paramScreenName) && paramUserKey == null) return null
            }
            LINK_ID_USER_MEDIA_TIMELINE -> {
                fragment = UserMediaTimelineFragment()
                val paramScreenName = uri.getQueryParameter(QUERY_PARAM_SCREEN_NAME)
                val paramUserKey = Utils.getUserKeyParam(uri)?.let(UserKey::valueOf)
                if (!args.containsKey(EXTRA_SCREEN_NAME)) {
                    args.putString(EXTRA_SCREEN_NAME, paramScreenName)
                }
                if (!args.containsKey(EXTRA_USER_KEY)) {
                    args.putParcelable(EXTRA_USER_KEY, paramUserKey)
                }
                if (TextUtils.isEmpty(paramScreenName) && paramUserKey == null) return null
            }
            LINK_ID_USER_FAVORITES -> {
                fragment = UserFavoritesFragment()
                val paramScreenName = uri.getQueryParameter(QUERY_PARAM_SCREEN_NAME)
                val paramUserKey = Utils.getUserKeyParam(uri)?.let(UserKey::valueOf)
                if (!args.containsKey(EXTRA_SCREEN_NAME)) {
                    args.putString(EXTRA_SCREEN_NAME, paramScreenName)
                }
                if (!args.containsKey(EXTRA_USER_KEY)) {
                    args.putParcelable(EXTRA_USER_KEY, paramUserKey)
                }
                if (!args.containsKey(EXTRA_SCREEN_NAME) && !args.containsKey(EXTRA_USER_KEY))
                    return null
            }
            LINK_ID_USER_FOLLOWERS -> {
                fragment = UserFollowersFragment()
                val paramScreenName = uri.getQueryParameter(QUERY_PARAM_SCREEN_NAME)
                val paramUserKey = Utils.getUserKeyParam(uri)?.let(UserKey::valueOf)
                if (!args.containsKey(EXTRA_SCREEN_NAME)) {
                    args.putString(EXTRA_SCREEN_NAME, paramScreenName)
                }
                if (!args.containsKey(EXTRA_USER_KEY)) {
                    args.putParcelable(EXTRA_USER_KEY, paramUserKey)
                }
                if (TextUtils.isEmpty(paramScreenName) && paramUserKey == null) return null
            }
            LINK_ID_USER_FRIENDS -> {
                fragment = UserFriendsFragment()
                val paramScreenName = uri.getQueryParameter(QUERY_PARAM_SCREEN_NAME)
                val paramUserKey = Utils.getUserKeyParam(uri)?.let(UserKey::valueOf)
                if (!args.containsKey(EXTRA_SCREEN_NAME)) {
                    args.putString(EXTRA_SCREEN_NAME, paramScreenName)
                }
                if (!args.containsKey(EXTRA_USER_KEY)) {
                    args.putParcelable(EXTRA_USER_KEY, paramUserKey)
                }
                if (TextUtils.isEmpty(paramScreenName) && paramUserKey == null) return null
            }
            LINK_ID_USER_BLOCKS -> {
                fragment = UserBlocksListFragment()
                args[EXTRA_SIMPLE_LAYOUT] = true
            }
            LINK_ID_MUTES_USERS -> {
                fragment = MutesUsersListFragment()
                args[EXTRA_SIMPLE_LAYOUT] = true
            }
            LINK_ID_MESSAGES -> {
                fragment = MessagesEntriesFragment()
            }
            LINK_ID_MESSAGES_CONVERSATION -> {
                fragment = MessagesConversationFragment()
                accountRequired = true
                val conversationId = uri.getQueryParameter(QUERY_PARAM_CONVERSATION_ID) ?: return null
                args.putString(EXTRA_CONVERSATION_ID, conversationId)
            }
            LINK_ID_MESSAGES_CONVERSATION_NEW -> {
                fragment = MessageNewConversationFragment()
                accountRequired = true
            }
            LINK_ID_MESSAGES_CONVERSATION_INFO -> {
                fragment = MessageConversationInfoFragment()
                val conversationId = uri.getQueryParameter(QUERY_PARAM_CONVERSATION_ID) ?: return null
                args.putString(EXTRA_CONVERSATION_ID, conversationId)
                accountRequired = true
            }
            LINK_ID_INTERACTIONS -> {
                fragment = InteractionsTimelineFragment()
            }
            LINK_ID_PUBLIC_TIMELINE -> {
                fragment = PublicTimelineFragment()
            }
            LINK_ID_USER_LIST -> {
                fragment = UserListFragment()
                val paramScreenName = uri.getQueryParameter(QUERY_PARAM_SCREEN_NAME)
                val paramUserKey = Utils.getUserKeyParam(uri)?.let(UserKey::valueOf)
                val paramListId = uri.getQueryParameter(QUERY_PARAM_LIST_ID)
                val paramListName = uri.getQueryParameter(QUERY_PARAM_LIST_NAME)
                if ((TextUtils.isEmpty(paramListName) || TextUtils.isEmpty(paramScreenName) && paramUserKey == null) && TextUtils.isEmpty(paramListId)) {
                    return null
                }
                args.putString(EXTRA_LIST_ID, paramListId)
                args.putParcelable(EXTRA_USER_KEY, paramUserKey)
                args.putString(EXTRA_SCREEN_NAME, paramScreenName)
                args.putString(EXTRA_LIST_NAME, paramListName)
            }
            LINK_ID_GROUP -> {
                fragment = GroupFragment()
                val paramGroupId = uri.getQueryParameter(QUERY_PARAM_GROUP_ID)
                val paramGroupName = uri.getQueryParameter(QUERY_PARAM_GROUP_NAME)
                if (TextUtils.isEmpty(paramGroupId) && TextUtils.isEmpty(paramGroupName))
                    return null
                args.putString(EXTRA_GROUP_ID, paramGroupId)
                args.putString(EXTRA_GROUP_NAME, paramGroupName)
            }
            LINK_ID_USER_LISTS -> {
                fragment = ListsFragment()
                val paramScreenName = uri.getQueryParameter(QUERY_PARAM_SCREEN_NAME)
                val paramUserKey = Utils.getUserKeyParam(uri)?.let(UserKey::valueOf)
                if (!args.containsKey(EXTRA_SCREEN_NAME)) {
                    args.putString(EXTRA_SCREEN_NAME, paramScreenName)
                }
                if (!args.containsKey(EXTRA_USER_KEY)) {
                    args.putParcelable(EXTRA_USER_KEY, paramUserKey)
                }
                if (TextUtils.isEmpty(paramScreenName) && paramUserKey == null) return null
            }
            LINK_ID_USER_GROUPS -> {
                fragment = UserGroupsFragment()
                val paramScreenName = uri.getQueryParameter(QUERY_PARAM_SCREEN_NAME)
                val paramUserKey = Utils.getUserKeyParam(uri)?.let(UserKey::valueOf)
                if (!args.containsKey(EXTRA_SCREEN_NAME)) {
                    args.putString(EXTRA_SCREEN_NAME, paramScreenName)
                }
                if (!args.containsKey(EXTRA_USER_KEY)) {
                    args.putParcelable(EXTRA_USER_KEY, paramUserKey)
                }
                if (TextUtils.isEmpty(paramScreenName) && paramUserKey == null) return null
            }
            LINK_ID_USER_LIST_TIMELINE -> {
                fragment = UserListTimelineFragment()
                val paramScreenName = uri.getQueryParameter(QUERY_PARAM_SCREEN_NAME)
                val paramUserKey = Utils.getUserKeyParam(uri)?.let(UserKey::valueOf)
                val paramListId = uri.getQueryParameter(QUERY_PARAM_LIST_ID)
                val paramListName = uri.getQueryParameter(QUERY_PARAM_LIST_NAME)
                if ((TextUtils.isEmpty(paramListName) || TextUtils.isEmpty(paramScreenName) && paramUserKey == null) && TextUtils.isEmpty(paramListId)) {
                    return null
                }
                args.putString(EXTRA_LIST_ID, paramListId)
                args.putParcelable(EXTRA_USER_KEY, paramUserKey)
                args.putString(EXTRA_SCREEN_NAME, paramScreenName)
                args.putString(EXTRA_LIST_NAME, paramListName)
            }
            LINK_ID_USER_LIST_MEMBERS -> {
                fragment = UserListMembersFragment()
                val paramScreenName = uri.getQueryParameter(QUERY_PARAM_SCREEN_NAME)
                val paramUserKey = Utils.getUserKeyParam(uri)?.let(UserKey::valueOf)
                val paramListId = uri.getQueryParameter(QUERY_PARAM_LIST_ID)
                val paramListName = uri.getQueryParameter(QUERY_PARAM_LIST_NAME)
                if ((TextUtils.isEmpty(paramListName) || TextUtils.isEmpty(paramScreenName) && paramUserKey == null) && TextUtils.isEmpty(paramListId))
                    return null
                args.putString(EXTRA_LIST_ID, paramListId)
                args.putParcelable(EXTRA_USER_KEY, paramUserKey)
                args.putString(EXTRA_SCREEN_NAME, paramScreenName)
                args.putString(EXTRA_LIST_NAME, paramListName)
            }
            LINK_ID_USER_LIST_SUBSCRIBERS -> {
                fragment = UserListSubscribersFragment()
                val paramScreenName = uri.getQueryParameter(QUERY_PARAM_SCREEN_NAME)
                val paramUserKey = Utils.getUserKeyParam(uri)?.let(UserKey::valueOf)
                val paramListId = uri.getQueryParameter(QUERY_PARAM_LIST_ID)
                val paramListName = uri.getQueryParameter(QUERY_PARAM_LIST_NAME)
                if (TextUtils.isEmpty(paramListId) && (TextUtils.isEmpty(paramListName) || TextUtils.isEmpty(paramScreenName) && paramUserKey == null))
                    return null
                args.putString(EXTRA_LIST_ID, paramListId)
                args.putParcelable(EXTRA_USER_KEY, paramUserKey)
                args.putString(EXTRA_SCREEN_NAME, paramScreenName)
                args.putString(EXTRA_LIST_NAME, paramListName)
            }
            LINK_ID_SAVED_SEARCHES -> {
                fragment = SavedSearchesListFragment()
            }
            LINK_ID_USER_MENTIONS -> {
                fragment = UserMentionsFragment()
                val paramScreenName = uri.getQueryParameter(QUERY_PARAM_SCREEN_NAME)
                if (!args.containsKey(EXTRA_SCREEN_NAME) && !TextUtils.isEmpty(paramScreenName)) {
                    args.putString(EXTRA_SCREEN_NAME, paramScreenName)
                }
                if (TextUtils.isEmpty(args.getString(EXTRA_SCREEN_NAME))) return null
            }
            LINK_ID_INCOMING_FRIENDSHIPS -> {
                fragment = IncomingFriendshipsFragment()
            }
            LINK_ID_ITEMS -> {
                accountRequired = false
                fragment = ItemsListFragment()
            }
            LINK_ID_STATUS_RETWEETERS -> {
                fragment = StatusRetweetersListFragment()
                if (!args.containsKey(EXTRA_STATUS_ID)) {
                    val paramStatusId = uri.getQueryParameter(QUERY_PARAM_STATUS_ID)
                    args.putString(EXTRA_STATUS_ID, paramStatusId)
                }
            }
            LINK_ID_STATUS_FAVORITERS -> {
                fragment = StatusFavoritersListFragment()
                if (!args.containsKey(EXTRA_STATUS_ID)) {
                    val paramStatusId = uri.getQueryParameter(QUERY_PARAM_STATUS_ID)
                    args.putString(EXTRA_STATUS_ID, paramStatusId)
                }
            }
            LINK_ID_SEARCH -> {
                val paramQuery = uri.getQueryParameter(QUERY_PARAM_QUERY)
                if (!args.containsKey(EXTRA_QUERY) && !TextUtils.isEmpty(paramQuery)) {
                    args.putString(EXTRA_QUERY, paramQuery)
                }
                if (!args.containsKey(EXTRA_QUERY)) {
                    return null
                }
                fragment = SearchFragment()
            }
            LINK_ID_FILTERS_IMPORT_BLOCKS -> {
                fragment = FiltersImportBlocksFragment()
            }
            LINK_ID_FILTERS_IMPORT_MUTES -> {
                fragment = FiltersImportMutesFragment()
            }
            LINK_ID_FILTERS_SUBSCRIPTIONS -> {
                fragment = FiltersSubscriptionsFragment()
                accountRequired = false
            }
            LINK_ID_FILTERS_SUBSCRIPTIONS_ADD -> {
                val url = uri.getQueryParameter("url") ?: return null
                val name = uri.getQueryParameter("name")
                fragment = FiltersSubscriptionsFragment()
                args.putString(IntentConstants.EXTRA_ACTION, FiltersSubscriptionsFragment.ACTION_ADD_URL_SUBSCRIPTION)
                args.putString(FiltersSubscriptionsFragment.EXTRA_ADD_SUBSCRIPTION_URL, url)
                args.putString(FiltersSubscriptionsFragment.EXTRA_ADD_SUBSCRIPTION_NAME, name)
                accountRequired = false
            }
            else -> {
                return null
            }
        }
        var accountKey = args.getParcelable<UserKey>(EXTRA_ACCOUNT_KEY)
        if (accountKey == null) {
            accountKey = uri.getQueryParameter(QUERY_PARAM_ACCOUNT_KEY)?.let(UserKey::valueOf)
        }
        if (accountKey == null) {
            val accountId = uri.getQueryParameter(CompatibilityConstants.QUERY_PARAM_ACCOUNT_ID)
            val paramAccountName = uri.getQueryParameter(QUERY_PARAM_ACCOUNT_NAME)
            DataStoreUtils.prepareDatabase(context)
            if (accountId != null) {
                accountKey = DataStoreUtils.findAccountKey(context, accountId)
                args.putParcelable(EXTRA_ACCOUNT_KEY, accountKey)
            } else if (paramAccountName != null) {
                accountKey = DataStoreUtils.findAccountKeyByScreenName(context, paramAccountName)
            }
        }

        if (accountRequired && accountKey == null) {
            val exception = Utils.NoAccountException()
            exception.accountHost = userHost
            throw exception
        }
        args.putParcelable(EXTRA_ACCOUNT_KEY, accountKey)
        fragment.arguments = args
        return fragment
    }
}