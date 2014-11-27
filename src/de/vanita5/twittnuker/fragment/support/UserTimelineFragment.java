/*
 * Twittnuker - Twitter client for Android
 *
 * Copyright (C) 2013-2014 vanita5 <mail@vanita5.de>
 *
 * This program incorporates a modified version of Twidere.
 * Copyright (C) 2012-2014 Mariotaku Lee <mariotaku.lee@gmail.com>
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

package de.vanita5.twittnuker.fragment.support;

import static de.vanita5.twittnuker.util.Utils.getAccountId;
import static de.vanita5.twittnuker.util.Utils.getAccountScreenName;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.OnScrollListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import de.vanita5.twittnuker.R;
import de.vanita5.twittnuker.adapter.ParcelableStatusesAdapter;
import de.vanita5.twittnuker.adapter.decorator.DividerItemDecoration;
import de.vanita5.twittnuker.adapter.iface.IStatusesListAdapter;
import de.vanita5.twittnuker.loader.support.UserTimelineLoader;
import de.vanita5.twittnuker.model.ParcelableStatus;
import de.vanita5.twittnuker.util.Utils;

import java.util.List;

public class UserTimelineFragment extends BaseSupportFragment
		implements LoaderCallbacks<List<ParcelableStatus>>, OnRefreshListener {

    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView mRecyclerView;

    private ParcelableStatusesAdapter mAdapter;
    private OnScrollListener mOnScrollListener = new OnScrollListener() {
	    @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);
	    }

	    @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            final LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
            final LoaderManager lm = getLoaderManager();
            if (lm.hasRunningLoaders()) return;
            if (layoutManager.findLastVisibleItemPosition() == mAdapter.getItemCount() - 1) {
                getStatuses(mAdapter.getStatus(mAdapter.getStatusCount() - 1).id, 0);
            }
        }
    };

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
        final View view = getView();
        assert view != null;
        final Context context = view.getContext();
        final boolean compact = Utils.isCompactCards(context);
        mSwipeRefreshLayout.setOnRefreshListener(this);
        mSwipeRefreshLayout.setColorSchemeColors(Color.RED, Color.GREEN, Color.BLUE);
        mAdapter = new ParcelableStatusesAdapter(context, compact);
        final LinearLayoutManager layoutManager = new LinearLayoutManager(context);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(layoutManager);
        if (compact) {
            mRecyclerView.addItemDecoration(new DividerItemDecoration(context, layoutManager.getOrientation()));
	    }
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setOnScrollListener(mOnScrollListener);
        getLoaderManager().initLoader(0, getArguments(), this);
    }


    public int getStatuses(final long maxId, final long sinceId) {
        final Bundle args = new Bundle(getArguments());
        args.putLong(EXTRA_MAX_ID, maxId);
        args.putLong(EXTRA_SINCE_ID, sinceId);
        getLoaderManager().restartLoader(0, args, this);
        return -1;
    }

	@Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mSwipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_layout);
        mRecyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
	}

	@Override
    protected void fitSystemWindows(Rect insets) {
        super.fitSystemWindows(insets);
        mRecyclerView.setClipToPadding(false);
        mRecyclerView.setPadding(insets.left, insets.top, insets.right, insets.bottom);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_recycler_view, container, false);
    }

    @Override
    public Loader<List<ParcelableStatus>> onCreateLoader(int id, Bundle args) {
        mSwipeRefreshLayout.setRefreshing(true);
        final List<ParcelableStatus> data = mAdapter.getData();
        final Context context = getActivity();
        final long accountId = args.getLong(EXTRA_ACCOUNT_ID, -1);
        final long maxId = args.getLong(EXTRA_MAX_ID, -1);
        final long sinceId = args.getLong(EXTRA_SINCE_ID, -1);
        final long userId = args.getLong(EXTRA_USER_ID, -1);
        final String screenName = args.getString(EXTRA_SCREEN_NAME);
        final int tabPosition = args.getInt(EXTRA_TAB_POSITION, -1);
        return new UserTimelineLoader(context, accountId, userId, screenName, maxId, sinceId, data,
                null, tabPosition);
	}

    @Override
    public void onLoadFinished(Loader<List<ParcelableStatus>> loader, List<ParcelableStatus> data) {
        mSwipeRefreshLayout.setRefreshing(false);
        mAdapter.setData(data);
	}

	@Override
    public void onLoaderReset(Loader<List<ParcelableStatus>> loader) {
	}


    @Override
    public void onRefresh() {
        if (mAdapter.getStatusCount() > 0) {
            getStatuses(0, mAdapter.getStatus(0).id);
        } else {
            getStatuses(0, 0);
        }
    }
}
