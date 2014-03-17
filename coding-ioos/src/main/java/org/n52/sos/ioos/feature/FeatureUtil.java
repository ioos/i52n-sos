package org.n52.sos.ioos.feature;

import java.util.HashSet;
import java.util.Set;

import org.n52.sos.ogc.om.features.samplingFeatures.SamplingFeature;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;

public class FeatureUtil {
    public static Set<Point> getFeaturePoints( Set<SamplingFeature> features ){
    	Set<Point> featurePoints = new HashSet<Point>();
    	
        for( SamplingFeature feature : features ){            
            featurePoints.addAll(getFeaturePoints( feature ));
        }

        return featurePoints;
    }
    
    public static Set<Point> getFeaturePoints( SamplingFeature feature ){
        Set<Point> points = new HashSet<Point>();
        if( feature != null ){
            Geometry geom = feature.getGeometry();
            if( geom instanceof Point ){
                points.add((Point) geom);
            } else if (geom instanceof LineString) {
                LineString lineString = (LineString) geom;
                for (int i = 0; i < lineString.getNumPoints(); i++) {
                    Point point = lineString.getPointN(i);
                    point.setSRID(lineString.getSRID());
                    points.add(point);
                }
            }
        }
        return points;
    }
    
    public static Set<Double> getFeatureHeights( SamplingFeature feature ){
        Set<Double> heights = new HashSet<Double>();
        Set<Point> points = getFeaturePoints( feature );
        for (Point point : points) {
            if( !Double.isNaN( point.getCoordinate().z ) ){
                heights.add(point.getCoordinate().z);
            }
        }                
        return heights;
    }

    public static Point clonePoint2d( Point point ){
    	if( point == null ){
    		return null;
    	}
    	if( Double.isNaN( point.getCoordinate().z ) ){
    		return point;
    	}
    	Point clonedPoint = (Point) point.clone();
    	clonedPoint.getCoordinate().z = Double.NaN;
    	return clonedPoint;
    }

    public static boolean equal2d(Point a, Point b){
        return a.getX() == b.getX() && a.getY() == b.getY();
    }    
}
