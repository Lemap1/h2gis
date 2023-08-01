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

package org.h2gis.functions.spatial.edit;

import org.h2gis.api.DeterministicScalarFunction;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.io.ParseException;

import java.sql.SQLException;
import java.util.ArrayList;

/**
 *
 * @author Erwan Bocher
 */
public class ST_CollectionExtract extends DeterministicScalarFunction{
    
    
    
    public ST_CollectionExtract() {
        addProperty(PROP_REMARKS, "Given a (multi)geometry, returns a (multi)geometry consisting only of elements of the specified dimension.\n"
                + "Dimension numbers are 1 == POINT, 2 == LINESTRING, 3 == POLYGON");
    }   

    @Override
    public String getJavaStaticMethod() {
        return "execute";
    }

    /**
     * Given a (multi)geometry, returns a (multi)geometry consisting only of
     * elements of the specified type. Sub-geometries that are not the specified
     * type are ignored. If there are no sub-geometries of the right type, an
     * EMPTY geometry will be returned. Only points, lines and polygons are
     * extracted.
     *
     * @param geometry
     * @param dimension1 one dimension to filter
     * @param dimension2 second dimension to filter
     * @return
     * @throws org.locationtech.jts.io.ParseException
     */
    public static Geometry execute(Geometry geometry, int dimension1, int dimension2) throws SQLException {
        if(geometry == null){
            return null;
        }
        if ((dimension1 < 1) || (dimension1 > 3)&&(dimension2 < 1) || (dimension2 > 3)) {
            throw new IllegalArgumentException(
                    "Dimension out of range (1..3)");
        }
        ArrayList<Geometry> geometries = new ArrayList<>();
        getGeometryByDimensions(geometries, geometry, dimension1,dimension2);
        return geometry.getFactory().buildGeometry(geometries);
    }


    /**
     * Given a (multi)geometry, returns a (multi)geometry consisting only of
     * elements of the specified type. Sub-geometries that are not the specified
     * type are ignored. If there are no sub-geometries of the right type, an
     * EMPTY geometry will be returned. Only points, lines and polygons are
     * extracted.
     * 
     * @param geometry
     * @param dimension
     * @return 
     * @throws org.locationtech.jts.io.ParseException 
     */
    public static Geometry execute(Geometry geometry, int dimension) throws SQLException {
        if(geometry == null){
            return null;
        }
        if ((dimension < 1) || (dimension > 3)) {
            throw new IllegalArgumentException(
                    "Dimension out of range (1..3)");
        }
         if (dimension == 1) {
            ArrayList<Point> points = new ArrayList<Point>();
            getPunctualGeometry(points, geometry);
            if (points.isEmpty()) {
                return geometry.getFactory().buildGeometry(points);
            } else if (points.size() == 1) {
                return points.get(0);
            } else {
                return geometry.getFactory().createMultiPoint(points.toArray(new Point[0]));
            }
        } else if (dimension == 2) {
            ArrayList<LineString> lines = new ArrayList<LineString>();
            getLinealGeometry(lines, geometry);
            if (lines.isEmpty()) {
                return geometry.getFactory().buildGeometry(lines);
            } else if (lines.size() == 1) {
                return lines.get(0);
            } else {
                return geometry.getFactory().createMultiLineString(lines.toArray(new LineString[0]));
            }
        } else if (dimension == 3) {
            ArrayList<Polygon> polygones = new ArrayList<Polygon>();
            getArealGeometry(polygones, geometry);
            if (polygones.isEmpty()) {
                return geometry.getFactory().buildGeometry(polygones);
            } else if (polygones.size() == 1) {
                return polygones.get(0);
            } else {
                return geometry.getFactory().createMultiPolygon(polygones.toArray(new Polygon[0]));
            }
        }
        return null;
    }

    /**
     * Filter dimensions from a geometry
     * @param geometries
     * @param geometry
     * @param dimension1 one dimension to filter
     * @param dimension2 second dimension to filter
     */
    private static void getGeometryByDimensions(ArrayList<Geometry> geometries, Geometry geometry, int dimension1, int dimension2) {
        int size = geometry.getNumGeometries();
        for (int i = 0; i < size; i++) {
            Geometry subGeom = geometry.getGeometryN(i);
            int dim = subGeom.getDimension()+1;
            if (subGeom instanceof GeometryCollection){
                getGeometryByDimensions(geometries, subGeom, dimension1, dimension2);
            } else if(dim==dimension1 || dim==dimension2){
                geometries.add( subGeom);
            }
        }
    }
    
    /**
     * Filter point from a geometry
     * @param points
     * @param geometry 
     */
    private static void getPunctualGeometry(ArrayList<Point> points, Geometry geometry) {
        int size = geometry.getNumGeometries();
        for (int i = 0; i < size; i++) {
            Geometry subGeom = geometry.getGeometryN(i);
            if(subGeom instanceof Point){
                points.add((Point) subGeom);
            }
            else if (subGeom instanceof GeometryCollection){
                getPunctualGeometry(points, subGeom);
            }
        }
    }
    
     /**
     * Filter line from a geometry
     * @param lines
     * @param geometry 
     */
    private static void getLinealGeometry(ArrayList<LineString> lines, Geometry geometry) {
        int size = geometry.getNumGeometries();
        for (int i = 0; i < size; i++) {
            Geometry subGeom = geometry.getGeometryN(i);
            if(subGeom instanceof LineString){
                lines.add((LineString) subGeom);
            }
            else if (subGeom instanceof GeometryCollection){
                getLinealGeometry(lines, subGeom);
            }
        }
    }
    
     /**
     * Filter polygon from a geometry
     * @param polygones
     * @param geometry 
     */
    private static void getArealGeometry(ArrayList<Polygon> polygones, Geometry geometry) {
        int size = geometry.getNumGeometries();
        for (int i = 0; i < size; i++) {
            Geometry subGeom = geometry.getGeometryN(i);
            if(subGeom instanceof Polygon){
                polygones.add((Polygon) subGeom);
            }
            else if (subGeom instanceof GeometryCollection){
                getArealGeometry(polygones, subGeom);
            }
        }
    }
}
