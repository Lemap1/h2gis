/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.h2gis.functions.spatial.edit;

import org.h2gis.api.DeterministicScalarFunction;
import org.h2gis.utilities.jts_utils.CoordinateUtils;
import org.locationtech.jts.geom.*;

import java.util.ArrayList;

/**
 * Remove duplicate coordinates in a geometry
 * 
 * @author Erwan Bocher CNRS
 */
public class ST_RemoveDuplicatedCoordinates extends DeterministicScalarFunction {
    
    
    public ST_RemoveDuplicatedCoordinates() {
        addProperty(PROP_REMARKS, "Returns a version of the given geometry without duplicated coordinates.");
    }

    @Override
    public String getJavaStaticMethod() {
        return "removeDuplicatedCoordinates";
    }

    /**
     * Returns a version of the given geometry with duplicated coordinates removed.
     *
     * @param geometry {@link Geometry}
     * @return Geometry without duplicated coordinates
     */
    public static Geometry removeDuplicatedCoordinates(Geometry geometry) {
        return removeCoordinates(geometry);
    }

    /**
     * Removes duplicated coordinates within a geometry.
     *
     * @param geom {@link Geometry}
     * @return Geometry without duplicated coordinates
     */
    public static Geometry removeCoordinates(Geometry geom) {
        if(geom ==null){
            return null;
        }
        else if (geom.isEmpty()) {
            return geom;
        } else if (geom instanceof Point){
            return geom;        }
         else if (geom instanceof MultiPoint) {
            return removeCoordinates((MultiPoint) geom);
        } else if (geom instanceof LineString) {
            return removeCoordinates((LineString) geom);
        } else if (geom instanceof MultiLineString) {
            return removeCoordinates((MultiLineString) geom);
        } else if (geom instanceof Polygon) {
            return removeCoordinates((Polygon) geom);
        } else if (geom instanceof MultiPolygon) {
            return removeCoordinates((MultiPolygon) geom);
        } else if (geom instanceof GeometryCollection) {
            return removeCoordinates((GeometryCollection) geom);
        }
        return null;
    }
    
    
    /**
     * Removes duplicated coordinates within a MultiPoint.
     *
     * @param g {@link MultiPoint}
     * @return MultiPoint without duplicated points
     */
    public static MultiPoint removeCoordinates(MultiPoint g) {
        Coordinate[] coords = CoordinateUtils.removeDuplicatedCoordinates(g.getCoordinates(),false);
        return g.getFactory().createMultiPointFromCoords(coords);
    }

    /**
     * Removes duplicated coordinates within a LineString.
     *
     * @param g {@link LineString}
     * @return LineString without duplicated coordinates
     */
    public static LineString removeCoordinates(LineString g) {
        Coordinate[] coords = CoordinateUtils.removeDuplicatedCoordinates(g.getCoordinates(), false);
        return g.getFactory().createLineString(coords);
    }

    /**
     * Removes duplicated coordinates within a linearRing.
     *
     * @param g {@link LinearRing}
     * @return LinearRing without duplicated coordinates
     */
    public static LinearRing removeCoordinates(LinearRing g) {
        Coordinate[] coords = CoordinateUtils.removeDuplicatedCoordinates(g.getCoordinates(),false);
        return g.getFactory().createLinearRing(coords);
    }

    /**
     * Removes duplicated coordinates in a MultiLineString.
     *
     * @param g {@link MultiLineString}
     * @return MultiLineString without duplicated coordinates
     */
    public static MultiLineString removeCoordinates(MultiLineString g) {
        ArrayList<LineString> lines = new ArrayList<LineString>();
        int size = g.getNumGeometries();
        for (int i = 0; i < size; i++) {
            LineString line = (LineString) g.getGeometryN(i);
            lines.add(removeCoordinates(line));
        }
        return g.getFactory().createMultiLineString(GeometryFactory.toLineStringArray(lines));
    }

    /**
     * Removes duplicated coordinates within a Polygon.
     *
     * @param poly {@link Polygon}
     * @return Polygon without duplicated coordinates
     */
    public static Polygon removeCoordinates(Polygon poly) {
        GeometryFactory factory =poly.getFactory();
        Coordinate[] shellCoords = CoordinateUtils.removeDuplicatedCoordinates(poly.getExteriorRing().getCoordinates(),true);
        LinearRing shell = factory.createLinearRing(shellCoords);
        ArrayList<LinearRing> holes = new ArrayList<LinearRing>();
        for (int i = 0; i < poly.getNumInteriorRing(); i++) {
            Coordinate[] holeCoords = CoordinateUtils.removeDuplicatedCoordinates(poly.getInteriorRingN(i).getCoordinates(),true);
            holes.add(factory.createLinearRing(holeCoords));
        }
        return factory.createPolygon(shell, GeometryFactory.toLinearRingArray(holes));
    }

    /**
     * Removes duplicated coordinates within a MultiPolygon.
     *
     * @param g {@link MultiPolygon}
     * @return MultiPolygon without duplicated coordinates
     */
    public static MultiPolygon removeCoordinates(MultiPolygon g) {
        ArrayList<Polygon> polys = new ArrayList<Polygon>();
        int size = g.getNumGeometries();
        for (int i = 0; i < size; i++) {
            Polygon poly = (Polygon) g.getGeometryN(i);
            polys.add(removeCoordinates(poly));
        }
        return g.getFactory().createMultiPolygon(GeometryFactory.toPolygonArray(polys));
    }

    /**
     * Removes duplicated coordinates within a GeometryCollection
     *
     * @param g {@link GeometryCollection}
     * @return GeometryCollection without duplicated coordinates
     */
    public static GeometryCollection removeCoordinates(GeometryCollection g) {
        ArrayList<Geometry> geoms = new ArrayList<Geometry>();
        int size = g.getNumGeometries();
        for (int i = 0; i < size; i++) {
            Geometry geom = g.getGeometryN(i);
            geoms.add(removeCoordinates(geom));
        }
        return g.getFactory().createGeometryCollection(GeometryFactory.toGeometryArray(geoms));
    }
}
    
