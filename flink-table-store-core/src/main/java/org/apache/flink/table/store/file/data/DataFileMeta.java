/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.table.store.file.data;

import org.apache.flink.table.data.binary.BinaryRowData;
import org.apache.flink.table.data.binary.BinaryRowDataUtil;
import org.apache.flink.table.store.file.stats.FieldStats;
import org.apache.flink.table.store.file.stats.FieldStatsArraySerializer;
import org.apache.flink.table.types.logical.BigIntType;
import org.apache.flink.table.types.logical.IntType;
import org.apache.flink.table.types.logical.RowType;
import org.apache.flink.table.types.logical.VarCharType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static org.apache.flink.util.Preconditions.checkArgument;

/** Metadata of a data file. */
public class DataFileMeta {

    // Append only data files don't have any key columns and meaningful level value. it will use
    // the following dummy values.
    public static final FieldStats[] EMPTY_KEY_STATS = new FieldStats[0];
    public static final BinaryRowData EMPTY_MIN_KEY = BinaryRowDataUtil.EMPTY_ROW;
    public static final BinaryRowData EMPTY_MAX_KEY = BinaryRowDataUtil.EMPTY_ROW;
    public static final int DUMMY_LEVEL = 0;

    private final String fileName;
    private final long fileSize;
    private final long rowCount;

    private final BinaryRowData minKey;
    private final BinaryRowData maxKey;
    private final FieldStats[] keyStats;
    private final FieldStats[] valueStats;

    private final long minSequenceNumber;
    private final long maxSequenceNumber;
    private final int level;

    public static DataFileMeta forAppend(
            String fileName,
            long fileSize,
            long rowCount,
            FieldStats[] rowStats,
            long minSequenceNumber,
            long maxSequenceNumber) {
        return new DataFileMeta(
                fileName,
                fileSize,
                rowCount,
                EMPTY_MIN_KEY,
                EMPTY_MAX_KEY,
                EMPTY_KEY_STATS,
                rowStats,
                minSequenceNumber,
                maxSequenceNumber,
                DUMMY_LEVEL);
    }

    public DataFileMeta(
            String fileName,
            long fileSize,
            long rowCount,
            BinaryRowData minKey,
            BinaryRowData maxKey,
            FieldStats[] keyStats,
            FieldStats[] valueStats,
            long minSequenceNumber,
            long maxSequenceNumber,
            int level) {
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.rowCount = rowCount;

        this.minKey = minKey;
        this.maxKey = maxKey;
        this.keyStats = keyStats;
        this.valueStats = valueStats;

        this.minSequenceNumber = minSequenceNumber;
        this.maxSequenceNumber = maxSequenceNumber;
        this.level = level;
    }

    public String fileName() {
        return fileName;
    }

    public long fileSize() {
        return fileSize;
    }

    public long rowCount() {
        return rowCount;
    }

    public BinaryRowData minKey() {
        return minKey;
    }

    public BinaryRowData maxKey() {
        return maxKey;
    }

    public FieldStats[] keyStats() {
        return keyStats;
    }

    public FieldStats[] valueStats() {
        return valueStats;
    }

    public long minSequenceNumber() {
        return minSequenceNumber;
    }

    public long maxSequenceNumber() {
        return maxSequenceNumber;
    }

    public int level() {
        return level;
    }

    public DataFileMeta upgrade(int newLevel) {
        checkArgument(newLevel > this.level);
        return new DataFileMeta(
                fileName,
                fileSize,
                rowCount,
                minKey,
                maxKey,
                keyStats,
                valueStats,
                minSequenceNumber,
                maxSequenceNumber,
                newLevel);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof DataFileMeta)) {
            return false;
        }
        DataFileMeta that = (DataFileMeta) o;
        return Objects.equals(fileName, that.fileName)
                && fileSize == that.fileSize
                && rowCount == that.rowCount
                && Objects.equals(minKey, that.minKey)
                && Objects.equals(maxKey, that.maxKey)
                && Arrays.equals(keyStats, that.keyStats)
                && Arrays.equals(valueStats, that.valueStats)
                && minSequenceNumber == that.minSequenceNumber
                && maxSequenceNumber == that.maxSequenceNumber
                && level == that.level;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                fileName,
                fileSize,
                rowCount,
                minKey,
                maxKey,
                // by default, hash code of arrays are computed by reference, not by content.
                // so we must use Arrays.hashCode to hash by content.
                Arrays.hashCode(keyStats),
                Arrays.hashCode(valueStats),
                minSequenceNumber,
                maxSequenceNumber,
                level);
    }

    @Override
    public String toString() {
        return String.format(
                "{%s, %d, %d, %s, %s, %s, %s, %d, %d, %d}",
                fileName,
                fileSize,
                rowCount,
                minKey,
                maxKey,
                Arrays.toString(keyStats),
                Arrays.toString(valueStats),
                minSequenceNumber,
                maxSequenceNumber,
                level);
    }

    public static RowType schema(RowType keyType, RowType valueType) {
        List<RowType.RowField> fields = new ArrayList<>();
        fields.add(new RowType.RowField("_FILE_NAME", new VarCharType(false, Integer.MAX_VALUE)));
        fields.add(new RowType.RowField("_FILE_SIZE", new BigIntType(false)));
        fields.add(new RowType.RowField("_ROW_COUNT", new BigIntType(false)));
        fields.add(new RowType.RowField("_MIN_KEY", keyType));
        fields.add(new RowType.RowField("_MAX_KEY", keyType));
        fields.add(new RowType.RowField("_KEY_STATS", FieldStatsArraySerializer.schema(keyType)));
        fields.add(
                new RowType.RowField("_VALUE_STATS", FieldStatsArraySerializer.schema(valueType)));
        fields.add(new RowType.RowField("_MIN_SEQUENCE_NUMBER", new BigIntType(false)));
        fields.add(new RowType.RowField("_MAX_SEQUENCE_NUMBER", new BigIntType(false)));
        fields.add(new RowType.RowField("_LEVEL", new IntType(false)));
        return new RowType(fields);
    }
}
