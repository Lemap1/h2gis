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
package org.h2gis.functions.io.fgb;

import org.h2gis.api.AbstractFunction;
import org.h2gis.api.EmptyProgressVisitor;
import org.h2gis.api.ScalarFunction;
import org.h2gis.functions.io.fgb.fileTable.FGBDriver;
import org.h2gis.utilities.URIUtilities;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Expose FlatGeobuffer reader function
 * @author Erwan Bocher
 * @author Nicolas Fortin
 */
public class FGBRead extends AbstractFunction implements ScalarFunction {

    public FGBRead() {
        addProperty(PROP_REMARKS, "Import a a FlatGeobuf file into a spatial table.\n "
                + "\nFGBRead(..."
                + "\n Supported arguments :"
                + "\n path of the file, table name"
                + "\n path of the file, table name, true to delete the table if exists");
    }

    @Override
    public String getJavaStaticMethod() {
        return "execute";
    }

    /**
     * Read a table and write it into a FlatGeobuf file.
     *
     * @param connection     Active connection
     * @param fileName       FlatGeobuf file name or URI
     * @param tableReference Table name or select query Note : The select query
     *                       must be enclosed in parenthesis
     * @throws IOException
     * @throws SQLException
     */
    public static void execute(Connection connection, String fileName, String tableReference, boolean deleteTable) throws SQLException, IOException {
        File file = URIUtilities.fileFromString(fileName);
        FGBDriverFunction driver = new FGBDriverFunction();
        driver.importFile(connection, tableReference, file, deleteTable ,new EmptyProgressVisitor());
    }

    /**
     * Read a table and write it into a FlatGeobuf file.
     *
     * @param connection     Active connection
     * @param fileName       FlatGeobuf file name or URI
     * @param tableReference Table name or select query Note : The select query
     *                       must be enclosed in parenthesis
     * @throws IOException
     * @throws SQLException
     */
    public static void execute(Connection connection, String fileName, String tableReference) throws IOException, SQLException {
        execute(connection, fileName, tableReference, false);
    }
}
