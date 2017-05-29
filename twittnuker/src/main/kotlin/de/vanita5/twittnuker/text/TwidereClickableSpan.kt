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

package de.vanita5.twittnuker.text

import android.text.TextPaint
import android.text.style.ClickableSpan
import org.mariotaku.ktextension.contains
import de.vanita5.twittnuker.constant.SharedPreferenceConstants


abstract class TwidereClickableSpan(val highlightStyle: Int): ClickableSpan() {

    override fun updateDrawState(ds: TextPaint) {
        if (SharedPreferenceConstants.VALUE_LINK_HIGHLIGHT_OPTION_CODE_UNDERLINE in highlightStyle) {
            ds.isUnderlineText = true
        }
        if (SharedPreferenceConstants.VALUE_LINK_HIGHLIGHT_OPTION_CODE_HIGHLIGHT in highlightStyle) {
            ds.color = ds.linkColor
        }
    }
}