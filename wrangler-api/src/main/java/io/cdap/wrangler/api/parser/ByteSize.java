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
 * A class to parse byte size strings like "10MB", "2GB", etc.
 */
public class ByteSize implements Token {

    private final String value; // Original String provided by the user
    private final long bytes; // Stores the parsed value in bytes

    public ByteSize(String value) {
        this.value = value;
        this.bytes = parseBytes(value);
    }

    // Helper method to parse the string in bytes
    private long parseBytes(String value) {
        value = value.trim().toUpperCase();
        double number;
        String unit;

        // Separate the number and the unit
        int i = value.length() - 1;
        while (i >= 0 && Character.isLetter(value.charAt(i))) {
            i--;
        }

        number = Double.parseDouble(value.substring(0, i + 1));
        unit = value.substring(i + 1);

        switch(unit) {
            case "B" : return (long) (number);
            case "KB" : return (long) (number * 1024);
            case "MB" : return (long) (number * 1024 * 1024);
            case "GB" : return (long) (number * 1024 * 1024 * 1024);
            case "TB" : return (long) (number * 1024L * 1024 * 1024 * 1024);
            default: throw new IllegalArgumentException("Invalid byte unit: " + unit);
        }
    }

//    Overriding the interface methods
    @Override
    public Object value() {
        return bytes;
    }

    @Override
    public TokenType type() {
        return TokenType.BYTE_SIZE;
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(bytes);
    }

    public long getBytes() {
        return bytes;
    }

    @Override
    public String toString() {
        return value;
    }
}
