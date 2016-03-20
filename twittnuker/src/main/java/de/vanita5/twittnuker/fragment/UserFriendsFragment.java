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

package de.vanita5.twittnuker.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;

import de.vanita5.twittnuker.fragment.CursorSupportUsersListFragment;
import de.vanita5.twittnuker.loader.CursorSupportUsersLoader;
import de.vanita5.twittnuker.loader.UserFriendsLoader;
import de.vanita5.twittnuker.model.UserKey;
import de.vanita5.twittnuker.model.message.FriendshipTaskEvent;

public class UserFriendsFragment extends CursorSupportUsersListFragment {

    @Override
    public CursorSupportUsersLoader onCreateUsersLoader(final Context context,
                                                        @NonNull final Bundle args, boolean fromUser) {
        final UserKey accountKey = args.getParcelable(EXTRA_ACCOUNT_KEY);
        final String userId = args.getString(EXTRA_USER_ID);
        final String screenName = args.getString(EXTRA_SCREEN_NAME);
        final UserFriendsLoader loader = new UserFriendsLoader(context, accountKey, userId,
                screenName, getData(), fromUser);
        loader.setCursor(getNextCursor());
        loader.setPage(getNextPage());
        return loader;
    }

    @Override
    protected boolean shouldRemoveUser(int position, FriendshipTaskEvent event) {
        if (!event.isSucceeded()) return false;
        switch (event.getAction()) {
            case FriendshipTaskEvent.Action.UNFOLLOW:
            case FriendshipTaskEvent.Action.BLOCK: {
                return true;
            }
        }
        return false;
    }
}