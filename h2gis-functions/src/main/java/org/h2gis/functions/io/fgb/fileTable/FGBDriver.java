/**
 * H2GIS is a library that brings spatial support to the H2 Database Engine
 * <a href="http://www.h2database.com">http://www.h2database.com</a>. H2GIS is developed by CNRS
 * <a href="http://www.cnrs.fr/">http://www.cnrs.fr/</a>.
 *
 * This code is part of the H2GIS project. H2GIS is free software;
 * you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation;
 * version 3.0 of the License.
 *
 * H2GIS is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details <http://www.gnu.org/licenses/>.
 *
 *
 * For more information, please consult: <a href="http://www.h2gis.org/">http://www.h2gis.org/</a>
 * or contact directly: info_at_h2gis.org
 */
package org.h2gis.functions.io.fgb.fileTable;

import com.google.common.io.LittleEndianDataInputStream;
import org.h2.index.Cursor;
import org.h2.result.Row;
import org.h2.result.SearchRow;
import org.h2.value.Value;
import org.h2.value.ValueBigint;
import org.h2.value.ValueBoolean;
import org.h2.value.ValueDate;
import org.h2.value.ValueDouble;
import org.h2.value.ValueGeometry;
import org.h2.value.ValueInteger;
import org.h2.value.ValueNull;
import org.h2.value.ValueSmallint;
import org.h2.value.ValueVarchar;
import org.h2gis.api.FileDriver;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.io.WKTReader;
import org.wololo.flatgeobuf.ColumnMeta;
import org.wololo.flatgeobuf.HeaderMeta;
import org.wololo.flatgeobuf.PackedRTree;
import org.wololo.flatgeobuf.generated.ColumnType;
import org.wololo.flatgeobuf.generated.Feature;
import org.wololo.flatgeobuf.generated.Geometry;
import org.wololo.flatgeobuf.generated.GeometryType;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.NavigableMap;
import java.util.NoSuchElementException;
import java.util.TreeMap;

public class FGBDriver implements FileDriver {
    private HeaderMeta headerMeta;
    private int fieldCount;
    private int geometryFieldIndex = 0;
    private int srid = 0;
    private FileInputStream fis;
    private FileChannel fileChannel;
    private Value[] currentRow = new Value[0];
    private long rowIdPrevious = -1;

    private boolean cacheRowAddress = true;

    /**
     * Address of the first feature
     */
    private long featuresOffset;

    private NavigableMap<Integer, Long> rowIndexToFileLocation;

    /**
     * Init file header for DBF File
     *
     * @param fgbFile DBF File path
     * @throws IOException
     */
    public void initDriverFromFile(File fgbFile) throws IOException {
        // Read columns from files metadata
        fis = new FileInputStream(fgbFile);
        this.fileChannel = fis.getChannel();
        headerMeta = HeaderMeta.read(fis);
        fieldCount = headerMeta.columns.size() + 1;
        long treeSize =
                headerMeta.featuresCount > 0 && headerMeta.indexNodeSize > 0
                        ?
                        PackedRTree.calcSize(
                                (int)headerMeta.featuresCount, headerMeta.indexNodeSize)
                        : 0;
        featuresOffset = headerMeta.offset + treeSize;
        srid = headerMeta.srid;
        rowIndexToFileLocation = new TreeMap<>();
        currentRow = new Value[fieldCount];
    }

    public boolean isCacheRowAddress() {
        return cacheRowAddress;
    }

    /**
     * @param cacheRowAddress If true the feature file address will be cached in order to reduce random access time.
     *                      If the file will be read sequentially only you can disable the cache in order
     *                      to reduce the memory footprint
     */
    public void setCacheRowAddress(boolean cacheRowAddress) {
        this.cacheRowAddress = cacheRowAddress;
    }

    public HeaderMeta getHeader() {
        return headerMeta;
    }

    @Override
    public long getRowCount() {
        return headerMeta.featuresCount;
    }

    @Override
    public int getEstimatedRowSize(long rowId) {
        int totalSize = 0;
        for(ColumnMeta column : headerMeta.columns) {
            totalSize += column.width;
        }
        return totalSize;
    }

    @Override
    public int getFieldCount() {
        return fieldCount;
    }

    @Override
    public void close() throws IOException {
        if (fis != null) fis.close();
    }

    public Cursor queryIndex(Envelope queryEnvelope) throws IOException {
        fileChannel.position(headerMeta.offset);
        PackedRTree.SearchResult searchResult = PackedRTree.search(fis, 0, (int)headerMeta.featuresCount,
                headerMeta.indexNodeSize, queryEnvelope);
        return new FGBDriverCursor(searchResult, this);
    }

    /**
     * Using the Spatial index it is possible to quickly cache the file address of all features.
     * Using this function before doing a random access should reduce the access time.
     * @throws IOException
     */
    public void cacheFeatureAddressFromIndex() throws IOException {
        if(headerMeta.indexNodeSize > 0) {
            fileChannel.position(headerMeta.offset);
            LittleEndianDataInputStream data = new LittleEndianDataInputStream(Channels.newInputStream(fileChannel));
            long[] fids = new long[(int) headerMeta.featuresCount];
            for (long id = 0; id < fids.length; id++) {
                fids[(int) id] = id;
            }
            long[] featuresAddress = PackedRTree.readFeatureOffsets(data, fids, headerMeta);
            for (int i = 0, featuresAddressLength = featuresAddress.length; i < featuresAddressLength; i++) {
                long address = featuresAddress[i];
                rowIndexToFileLocation.put(i, address + featuresOffset);
            }
        }
    }

    /**
     * @param featureAddress Feature address in the file relative to the first feature
     * @return
     * @throws IOException
     */
    public Value[] getFieldsFromFileLocation(long featureAddress) throws IOException {
        Value[] values = new Value[fieldCount];
        fileChannel.position(featuresOffset + featureAddress);
        // Read the current row from the input stream
        LittleEndianDataInputStream data = new LittleEndianDataInputStream(Channels.newInputStream(fileChannel));
        int featureSize = data.readInt();
        byte[] bytes = new byte[featureSize];
        data.readFully(bytes);
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        Feature feature = Feature.getRootAsFeature(bb);
        Geometry geometry = feature.geometry();
        byte geometryType = headerMeta.geometryType;
        if (geometry != null) {
            if (geometryType == GeometryType.Unknown) {
                geometryType = (byte) geometry.type();
            }
            org.locationtech.jts.geom.Geometry jtsGeometry =
                    GeometryConversions.deserialize(geometry, geometryType);
            if (jtsGeometry != null) {
                jtsGeometry.setSRID(srid);
                currentRow[geometryFieldIndex] = ValueGeometry.getFromGeometry(jtsGeometry);
            } else {
                currentRow[geometryFieldIndex] = ValueNull.INSTANCE;
            }
        }
        // Read columns
        int propertiesLength = feature.propertiesLength();
        if (propertiesLength > 0) {
            List<ColumnMeta> columns = headerMeta.columns;
            ByteBuffer propertiesBB = feature.propertiesAsByteBuffer();
            while (propertiesBB.hasRemaining()) {
                short propertyIndex = propertiesBB.getShort();
                ColumnMeta columnMeta = columns.get(propertyIndex);
                byte type = columnMeta.type;
                if(propertyIndex >= geometryFieldIndex) {
                    propertyIndex += 1;
                }
                switch (type) {
                    case ColumnType.Bool:
                        currentRow[propertyIndex] = ValueBoolean.get(propertiesBB.get() > 0);
                        break;
                    case ColumnType.Byte:
                        currentRow[propertyIndex] = ValueSmallint.get(propertiesBB.get());
                        break;
                    case ColumnType.Short:
                        currentRow[propertyIndex] = ValueSmallint.get(propertiesBB.getShort());
                        break;
                    case ColumnType.Int:
                        currentRow[propertyIndex] = ValueInteger.get(propertiesBB.getInt());
                        break;
                    case ColumnType.Long:
                        currentRow[propertyIndex] = ValueBigint.get(propertiesBB.getLong());
                        break;
                    case ColumnType.Double:
                        currentRow[propertyIndex] = ValueDouble.get(propertiesBB.getDouble());
                        break;
                    case ColumnType.DateTime:
                        currentRow[propertyIndex] = ValueDate.parse(readString(propertiesBB));
                        break;
                    case ColumnType.String:
                        currentRow[propertyIndex] = ValueVarchar.get(readString(propertiesBB));
                        break;
                    default:
                        throw new RuntimeException("Unknown type");
                }
            }
        }
        return values;
    }

    @Override
    public Value getField(long rowId, int columnId) throws IOException {
        int featureSize;
        try {
            if(rowId == 0) {
                fileChannel.position(featuresOffset);
                rowIdPrevious = -1;
            } else if (rowId > rowIdPrevious + 1 || rowId < rowIdPrevious) {
                // We have to seek to the desired location
                Integer lowerKey = rowIndexToFileLocation.floorKey((int)rowId);
                if(lowerKey == null) {
                    fileChannel.position(featuresOffset);
                    rowIdPrevious = -1;
                } else {
                    fileChannel.position(rowIndexToFileLocation.get(lowerKey));
                    rowIdPrevious = lowerKey - 1;
                }
                // Make our way until rowId
                while (rowIdPrevious + 1 < rowId) {
                    LittleEndianDataInputStream data = new LittleEndianDataInputStream(Channels.newInputStream(fileChannel));
                    featureSize = data.readInt();
                    fileChannel.position(fileChannel.position() + featureSize);
                    rowIdPrevious++;
                    if(cacheRowAddress) {
                        rowIndexToFileLocation.put((int) rowIdPrevious + 1, fileChannel.position());
                    }
                }
            }
            if (rowIdPrevious + 1 == rowId) {
                // Read the current row from the input stream
                rowIdPrevious = rowId;
                currentRow = getFieldsFromFileLocation(fileChannel.position()-featuresOffset);
                if(cacheRowAddress) {
                    rowIndexToFileLocation.put((int) rowId + 1, fileChannel.position());
                }
            }
            return currentRow[columnId];
        } catch (IOException e) {
            throw new NoSuchElementException();
        }
    }

    @Override
    public void insertRow(Object[] values) throws IOException {
        throw new IOException("Unsupported write operation");
    }

    private String readString(ByteBuffer bb) {
            int length = bb.getInt();
            byte[] stringBytes = new byte[length];
            bb.get(stringBytes, 0, length);
            return  new String(stringBytes, StandardCharsets.UTF_8);
    }

    /**
     * @return Field index of the geometry when using the method getField
     */
    public int getGeometryFieldIndex() {
        return geometryFieldIndex;
    }

    public static class FGBDriverCursor implements Cursor {
        PackedRTree.SearchResult searchResult;
        FGBDriver fgbDriver;

        int position = -1;

        public FGBDriverCursor(PackedRTree.SearchResult searchResult, FGBDriver fgbDriver) {
            this.searchResult = searchResult;
            this.fgbDriver = fgbDriver;
        }

        @Override
        public Row get() {
            return null;
        }

        @Override
        public SearchRow getSearchRow() {
            return null;
        }

        @Override
        public boolean next() {
            return position < searchResult.hits.size() - 1;
        }

        @Override
        public boolean previous() {
            return position > 0;
        }
    }
}
