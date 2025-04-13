/*
 * Copyright © 2017-2025 Cask Data, Inc.
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

package io.cdap.wrangler.api;

import io.cdap.cdap.api.annotation.Description;
import io.cdap.cdap.api.annotation.Name;
import io.cdap.cdap.api.annotation.Plugin;
import io.cdap.wrangler.api.parser.ColumnName;
import io.cdap.wrangler.api.parser.Text;
import io.cdap.wrangler.api.parser.TokenType;
import io.cdap.wrangler.api.parser.UsageDefinition;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Imports or annotations
  */

@Plugin(type = Directive.TYPE)
@Name("aggregate-size-time")
@Description("Aggregates byte sizes and time durations into total size and total or average time.")

/**
 * new directive class, which can do aggregation, implementing the Directive interface
 */

public class AggregateSizeAndTime implements Directive {
    private String sizeColumn; // Source column for byte sizes
    private String timeColumn; // Source column for time durations
    private String totalSizeColumn; // Target column for total size
    private String timeAggColumn; // Target column for total or average time
    private String sizeUnit = "MB"; // Default output unit for size
    private String timeUnit = "s"; // Default output unit for time
    private String timeAggType = "total"; // Default: total time (not average)
    private long totalBytes = 0L; // Accumulated total bytes
    private long totalNanoseconds = 0L; // Accumulated total nanoseconds
    private long rowCount = 0L; // Count of rows for averaging
    private boolean finalized = false; // Flag to prevent multiple outputs

    @Override
    public UsageDefinition define() {
        UsageDefinition.Builder builder = UsageDefinition.builder("aggregate-size-time");
        builder.define("sizeColumn", TokenType.COLUMN_NAME);
        builder.define("timeColumn", TokenType.COLUMN_NAME);
        builder.define("totalSizeColumn", TokenType.COLUMN_NAME);
        builder.define("timeAggColumn", TokenType.COLUMN_NAME);
        builder.define("sizeUnit", TokenType.TEXT, true); // Optional argument
        builder.define("timeUnit", TokenType.TEXT, true); // Optional argument
        builder.define("timeAggType", TokenType.TEXT, true); // Optional argument
        return builder.build();
    }

    @Override
    public void initialize(Arguments args) throws DirectiveParseException {
        this.sizeColumn = ((ColumnName) args.value("sizeColumn")).value();
        this.timeColumn = ((ColumnName) args.value("timeColumn")).value();
        this.totalSizeColumn = ((ColumnName) args.value("totalSizeColumn")).value();
        this.timeAggColumn = ((ColumnName) args.value("timeAggColumn")).value();

        if (args.contains("sizeUnit")) {
            this.sizeUnit = ((Text) args.value("sizeUnit")).value().toUpperCase();
            if (!Arrays.asList("B", "KB", "MB", "GB", "TB").contains(this.sizeUnit)) {
                throw new DirectiveParseException("Invalid size unit: " + this.sizeUnit);
            }
        }

        if (args.contains("timeUnit")) {
            this.timeUnit = ((Text) args.value("timeUnit")).value().toLowerCase();
            if (!Arrays.asList("ns", "us", "ms", "s", "m", "h", "d").contains(this.timeUnit)) {
                throw new DirectiveParseException("Invalid time unit: " + this.timeUnit);
            }
        }

        if (args.contains("timeAggType")) {
            this.timeAggType = ((Text) args.value("timeAggType")).value().toLowerCase();
            if (!Arrays.asList("total", "average").contains(this.timeAggType)) {
                throw new DirectiveParseException("Invalid aggregation type: " + this.timeAggType);
            }
        }
    }

    @Override
    public List<Row> execute(List<Row> rows, ExecutorContext context)
            throws DirectiveExecutionException, ErrorRowException {
        // If finalized, return empty list to prevent further output
        if (finalized) {
            return Collections.emptyList();
        }

        // If rows is empty, assume end of data and produce final output
        if (rows.isEmpty()) {
            finalized = true;
            return produceOutput();
        }

        // Process each row to accumulate totals
        for (Row row : rows) {
            // Read byte size
            Object sizeObj = row.getValue(sizeColumn);
            long bytes = 0;
            if (sizeObj instanceof String) {
                bytes = parseByteSize((String) sizeObj);
            } else if (sizeObj instanceof Number) {
                bytes = ((Number) sizeObj).longValue();
            } else if (sizeObj != null) {
                throw new DirectiveExecutionException(
                        "Invalid byte size value in column " + sizeColumn + ": " + sizeObj
                );
            }

            // Read time duration
            Object timeObj = row.getValue(timeColumn);
            long nanoseconds = 0;
            if (timeObj instanceof String) {
                nanoseconds = parseTimeDuration((String) timeObj);
            } else if (timeObj instanceof Number) {
                nanoseconds = ((Number) timeObj).longValue();
            } else if (timeObj != null) {
                throw new DirectiveExecutionException(
                        "Invalid time duration value in column " + timeColumn + ": " + timeObj
                );
            }

            // Update running totals
            totalBytes += bytes;
            totalNanoseconds += nanoseconds;
            rowCount++;
        }

        // Return empty list during processing (output produced when rows is empty)
        return Collections.emptyList();
    }

    @Override
    public void destroy() {
        // Reset state in case the directive is reused
        totalBytes = 0L;
        totalNanoseconds = 0L;
        rowCount = 0L;
        finalized = false;
    }

    private List<Row> produceOutput() {
        // Convert totals to output units
        double sizeOutput = convertBytes(totalBytes, sizeUnit);
        double timeOutput = convertNanoseconds(totalNanoseconds, timeUnit);
        if (timeAggType.equals("average") && rowCount > 0) {
            timeOutput /= rowCount;
        }

        // Create output row
        Row outputRow = new Row();
        outputRow.add(totalSizeColumn, sizeOutput);
        outputRow.add(timeAggColumn, timeOutput);

        return Collections.singletonList(outputRow);
    }

    private long parseByteSize(String sizeStr) throws DirectiveExecutionException {
        try {
            String numberPart = sizeStr.replaceAll("[^0-9.]", "");
            String unitPart = sizeStr.replaceAll("[0-9.]", "").toUpperCase();
            double value = Double.parseDouble(numberPart);
            if (unitPart.equals("B")) {
                return (long) value;
            } else if (unitPart.equals("KB")) {
                return (long) (value * 1024);
            } else if (unitPart.equals("MB")) {
                return (long) (value * 1024 * 1024);
            } else if (unitPart.equals("GB")) {
                return (long) (value * 1024 * 1024 * 1024);
            } else if (unitPart.equals("TB")) {
                return (long) (value * 1024 * 1024 * 1024 * 1024);
            } else {
                throw new DirectiveExecutionException("Invalid byte size unit: " + unitPart);
            }
        } catch (NumberFormatException e) {
            throw new DirectiveExecutionException("Failed to parse byte size: " + sizeStr, e);
        }
    }

    private long parseTimeDuration(String timeStr) throws DirectiveExecutionException {
        try {
            String numberPart = timeStr.replaceAll("[^0-9.]", "");
            String unitPart = timeStr.replaceAll("[0-9.]", "").toLowerCase();
            double value = Double.parseDouble(numberPart);
            if (unitPart.equals("ns")) {
                return (long) value;
            } else if (unitPart.equals("us")) {
                return (long) (value * 1_000);
            } else if (unitPart.equals("ms")) {
                return (long) (value * 1_000_000);
            } else if (unitPart.equals("s")) {
                return (long) (value * 1_000_000_000);
            } else if (unitPart.equals("m")) {
                return (long) (value * 60 * 1_000_000_000);
            } else if (unitPart.equals("h")) {
                return (long) (value * 60 * 60 * 1_000_000_000);
            } else if (unitPart.equals("d")) {
                return (long) (value * 24 * 60 * 60 * 1_000_000_000);
            } else {
                throw new DirectiveExecutionException("Invalid time unit: " + unitPart);
            }
        } catch (NumberFormatException e) {
            throw new DirectiveExecutionException("Failed to parse time duration: " + timeStr, e);
        }
    }

    private double convertBytes(long bytes, String unit) {
        if (unit.equalsIgnoreCase("B")) {
            return bytes;
        } else if (unit.equalsIgnoreCase("KB")) {
            return bytes / 1024.0;
        } else if (unit.equalsIgnoreCase("MB")) {
            return bytes / (1024.0 * 1024);
        } else if (unit.equalsIgnoreCase("GB")) {
            return bytes / (1024.0 * 1024 * 1024);
        } else if (unit.equalsIgnoreCase("TB")) {
            return bytes / (1024.0 * 1024 * 1024 * 1024);
        } else {
            return bytes / (1024.0 * 1024); // Default to MB
        }
    }

    private double convertNanoseconds(long nanoseconds, String unit) {
        if (unit.equalsIgnoreCase("ns")) {
            return nanoseconds;
        } else if (unit.equalsIgnoreCase("us")) {
            return nanoseconds / 1_000.0;
        } else if (unit.equalsIgnoreCase("ms")) {
            return nanoseconds / 1_000_000.0;
        } else if (unit.equalsIgnoreCase("s")) {
            return nanoseconds / 1_000_000_000.0;
        } else if (unit.equalsIgnoreCase("m")) {
            return nanoseconds / (60.0 * 1_000_000_000);
        } else if (unit.equalsIgnoreCase("h")) {
            return nanoseconds / (60.0 * 60 * 1_000_000_000);
        } else if (unit.equalsIgnoreCase("d")) {
            return nanoseconds / (24.0 * 60 * 60 * 1_000_000_000);
        } else {
            return nanoseconds / 1_000_000_000.0; // Default to seconds
        }
    }
}
