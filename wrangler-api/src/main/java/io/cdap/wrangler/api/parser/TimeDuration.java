/*
 * Copyright © 2017-2019 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package io.cdap.wrangler.api.parser;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/**
 * A class to parse time duration strings like "10s", "5min", "2h", etc.
 */
public class TimeDuration implements Token {

    private final String value; // Original value provided by the user
    private final long milliSeconds; // Stores the parsed value in milliSeconds

    public TimeDuration(String value) {
        this.value = value;
        this.milliSeconds = parseMilliseconds(value);
    }

    // Helper method to convert time duration string to its millisecond equivalent
    private long parseMilliseconds(String value) {
        value = value.trim().toLowerCase();

        double number;
        String unit;

        // Splitting the number and the unit
        int i = value.length() - 1;
        while (i >= 0 && Character.isLetter(value.charAt(i))) {
            i--;
        }

        number = Double.parseDouble(value.substring(0, i + 1));
        unit = value.substring(i + 1);

        switch(unit) {
            case "ns" : return (long) (number / 1_000_000);
            case "us" : return (long) (number / 1_000);
            case "ms" : return (long) (number);
            case "s"  : return (long) (number * 1000);
            case "m"  : return (long) (number * 60 * 1000);
            case "h"  : return (long) (number * 60 * 60 * 1000);
            case "d"  : return (long) (number * 24 * 60 * 60 * 1000);
            default   : throw new IllegalArgumentException("Invalid time unit : " + unit);
        }
    }

    // Overriding the methods of token interface
    @Override
    public Object value() {
        return milliSeconds;
    }

    @Override
    public TokenType type() {
        return TokenType.TIME_DURATION;
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(milliSeconds);
    }

    public long getMilliSeconds() {
        return milliSeconds;
    }

    @Override
    public String toString() {
        return value;
    }
}
