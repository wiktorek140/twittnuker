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

package de.vanita5.twittnuker.view

import android.content.Context
import android.text.Spannable
import android.text.method.BaseMovementMethod
import android.text.method.MovementMethod
import android.text.style.ClickableSpan
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.TextView
import org.mariotaku.chameleon.view.ChameleonTextView
import de.vanita5.twittnuker.extension.setupEmojiFactory

/**
 * Returns true when not clicking links
 * Created by mariotaku on 15/11/20.
 */
class TimelineContentTextView(
        context: Context,
        attrs: AttributeSet? = null
) : ChameleonTextView(context, attrs) {

    init {
        setupEmojiFactory()
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        // FIXME simple workaround to https://code.google.com/p/android/issues/detail?id=191430
        // Android clears TextView when setText(), so setText before touch
        if (event.actionMasked == MotionEvent.ACTION_DOWN && isTextSelectable) {
            if (selectionEnd != selectionStart) {
                val text = text
                setText(null)
                setText(text)
            }
        }
        return super.dispatchTouchEvent(event)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isTextSelectable) {
            return super.onTouchEvent(event)
        }
        return super.onTouchEvent(event)
    }

    override fun getDefaultMovementMethod(): MovementMethod {
        return InternalMovementMethod()
    }

    override fun setClickable(clickable: Boolean) {
        super.setClickable(clickable && isTextSelectable)
    }

    override fun setLongClickable(longClickable: Boolean) {
        super.setLongClickable(longClickable && isTextSelectable)
    }

    override fun onTextContextMenuItem(id: Int): Boolean {
        try {
            return super.onTextContextMenuItem(id)
        } catch (e: AbstractMethodError) {
            // http://crashes.to/s/69acd0ea0de
            return true
        }
    }

    internal class InternalMovementMethod : BaseMovementMethod() {
        private var targetSpan: ClickableSpan? = null

        override fun initialize(widget: TextView, text: Spannable) {

        }

        override fun onTouchEvent(widget: TextView, text: Spannable, event: MotionEvent): Boolean {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    val layout = widget.layout
                    val x = event.x - widget.paddingLeft + widget.scrollX
                    val y = event.y - widget.paddingTop + widget.scrollY
                    val line = layout.getLineForVertical(Math.round(y))
                    val offset = layout.getOffsetForHorizontal(line, x)
                    if (x <= layout.getLineWidth(line)) {
                        targetSpan = text.getSpans(offset, offset, ClickableSpan::class.java).firstOrNull()
                    } else {
                        targetSpan = null
                    }
                }
                MotionEvent.ACTION_UP -> {
                    targetSpan?.onClick(widget)
                    val handled = targetSpan != null
                    targetSpan = null
                    return handled
                }
                MotionEvent.ACTION_CANCEL -> {
                    targetSpan = null
                }
            }
            return targetSpan != null
        }

    }
}