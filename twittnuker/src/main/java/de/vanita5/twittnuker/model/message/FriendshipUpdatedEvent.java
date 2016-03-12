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

package de.vanita5.twittnuker.model.message;

import android.support.annotation.NonNull;

import de.vanita5.twittnuker.api.twitter.model.Relationship;
import de.vanita5.twittnuker.model.AccountKey;

public class FriendshipUpdatedEvent {

    @NonNull
    AccountKey accountKey;
    long userId;
    @NonNull
    Relationship relationship;

    public FriendshipUpdatedEvent(@NonNull AccountKey accountKey, long userId, @NonNull Relationship relationship) {
        this.accountKey = accountKey;
        this.userId = userId;
        this.relationship = relationship;
    }

    @NonNull
    public AccountKey getAccountKey() {
        return accountKey;
    }

    public long getUserId() {
        return userId;
    }

    @NonNull
    public Relationship getRelationship() {
        return relationship;
    }

    public boolean isAccount(long accountId, String accountHost) {
        return accountKey.isAccount(accountId, accountHost);
    }

    public boolean isUser(long id) {
        return userId == id;
    }

    public boolean isAccount(AccountKey accountKey) {
        return this.accountKey.equals(accountKey);
    }
}