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

package de.vanita5.twittnuker.model.util;

import com.bluelinelabs.logansquare.typeconverters.TypeConverter;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;

import de.vanita5.twittnuker.model.CronExpression;

import java.io.IOException;
import java.text.ParseException;

public class CronExpressionConverter implements TypeConverter<CronExpression> {

    @Override
    public CronExpression parse(JsonParser jsonParser) throws IOException {
        final String string = jsonParser.getValueAsString(null);
        if (string == null) return null;
        try {
            return CronExpression.valueOf(string);
        } catch (ParseException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void serialize(CronExpression object, String fieldName, boolean writeFieldNameForObject,
                          JsonGenerator jsonGenerator) throws IOException {
        if (object == null) {
            if (fieldName != null) {
                jsonGenerator.writeNullField(fieldName);
            } else {
                jsonGenerator.writeNull();
            }
        } else {
            if (fieldName != null) {
                jsonGenerator.writeStringField(fieldName, object.toExpression());
            } else {
                jsonGenerator.writeString(object.toExpression());
            }
        }
    }

}