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

package org.h2gis.functions.spatial.properties;

import org.h2gis.api.DeterministicScalarFunction;
import org.locationtech.jts.geom.Geometry;

/**
 * ST_YMax returns the maximal y-value of the given geometry.
 *
 * @author Adam Gouge
 */
public class ST_YMax extends DeterministicScalarFunction {

    public ST_YMax() {
        addProperty(PROP_REMARKS, "Returns the maximal y-value of the given geometry.");
    }

    @Override
    public String getJavaStaticMethod() {
        return "getMaxY";
    }

    /**
     * Returns the maximal y-value of the given geometry.
     *
     * @param geom Geometry
     * @return The maximal y-value of the given geometry, or null if the geometry is null.
     */
    public static Double getMaxY(Geometry geom) {
        if (geom != null) {
            return geom.getEnvelopeInternal().getMaxY();
        } else {
            return null;
        }
    }
}
