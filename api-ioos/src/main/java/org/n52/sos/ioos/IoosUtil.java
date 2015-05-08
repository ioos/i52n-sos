package org.n52.sos.ioos;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.n52.sos.exception.ows.InvalidParameterValueException;
import org.n52.sos.exception.ows.NoApplicableCodeException;
import org.n52.sos.ioos.asset.AbstractAsset;
import org.n52.sos.ioos.asset.AssetConstants;
import org.n52.sos.ioos.asset.AssetResolver;
import org.n52.sos.ioos.asset.FakeStationAsset;
import org.n52.sos.ioos.asset.SensorAsset;
import org.n52.sos.ioos.asset.StationAsset;
import org.n52.sos.ioos.axis.AxisUtil;
import org.n52.sos.ioos.data.dataset.TimeSeriesProfileSensorDataset;
import org.n52.sos.ioos.data.dataset.TimeSeriesSensorDataset;
import org.n52.sos.ioos.data.dataset.TrajectoryProfileSensorDataset;
import org.n52.sos.ioos.data.dataset.TrajectorySensorDataset;
import org.n52.sos.ioos.data.subsensor.BinProfileSubSensor;
import org.n52.sos.ioos.data.subsensor.PointProfileSubSensor;
import org.n52.sos.ioos.data.subsensor.SubSensor;
import org.n52.sos.ioos.feature.FeatureUtil;
import org.n52.sos.ioos.om.IoosSosObservation;
import org.n52.sos.ogc.gml.AbstractFeature;
import org.n52.sos.ogc.gml.time.Time;
import org.n52.sos.ogc.gml.time.TimePeriod;
import org.n52.sos.ogc.om.AbstractPhenomenon;
import org.n52.sos.ogc.om.ObservationValue;
import org.n52.sos.ogc.om.OmCompositePhenomenon;
import org.n52.sos.ogc.om.OmObservableProperty;
import org.n52.sos.ogc.om.OmObservation;
import org.n52.sos.ogc.om.OmObservationConstellation;
import org.n52.sos.ogc.om.SingleObservationValue;
import org.n52.sos.ogc.om.features.samplingFeatures.SamplingFeature;
import org.n52.sos.ogc.om.values.QuantityValue;
import org.n52.sos.ogc.om.values.Value;
import org.n52.sos.ogc.ows.OwsExceptionReport;
import org.n52.sos.util.GeometryHandler;
import org.slf4j.Logger;

import ucar.nc2.constants.CF;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;

public class IoosUtil {
    /**
     * Organizes SosObservation collection into a list of IoosSosObservation blocks, each of which contain
     * a single feature type
     * 
     * @param omObservations The collection of observations to transform
     * @return List<IoosSosObservation> ready for encoding
     * @throws OwsExceptionReport 
     */
    public static List<IoosSosObservation> createIoosSosObservations(List<OmObservation> omObservations )
            throws OwsExceptionReport {
        // the main map of observation value strings by asset, time, phenomenon, and subsensor (height, profile bin, etc)
        Map<SensorAsset,Map<Time,Map<OmObservableProperty,Map<SubSensor,Value<?>>>>> obsValuesMap
            = new HashMap<SensorAsset,Map<Time,Map<OmObservableProperty,Map<SubSensor,Value<?>>>>>();

        //keep track of other data associated with station
        SetMultimap<StationAsset,Point> stationPoints = HashMultimap.create();
        
        SetMultimap<SensorAsset,OmObservableProperty> sensorPhens = HashMultimap.create();
//        Map<StationAsset,TimePeriod> stationPeriodMap = new HashMap<StationAsset,TimePeriod>();

        // maps to keep track of unique dimension values by sensor (these may or may not vary, determining the feature type)        
        SetMultimap<SensorAsset,Double> sensorLngs = HashMultimap.create();
        SetMultimap<SensorAsset,Double> sensorLats = HashMultimap.create();
        SetMultimap<SensorAsset,Double> sensorHeights = HashMultimap.create();

        int fakeStationCounter = 0;
        Map<String,FakeStationAsset> fakeStations = new HashMap<String,FakeStationAsset>();
        
        for( OmObservation sosObs : omObservations ){
            OmObservationConstellation obsConst = sosObs.getObservationConstellation();

            //first, resolve the procId to an asset type
            String procId = obsConst.getProcedure().getIdentifier();
            AbstractAsset asset = AssetResolver.resolveAsset( procId );
            if( asset == null ){
                //not a valid asset, but force it to a fake station to output anyway
                if( fakeStations.get( procId ) == null ){
                    fakeStations.put( procId, new FakeStationAsset( AssetConstants.FAKE + ++fakeStationCounter, procId ) );
                }
                asset = fakeStations.get( procId );
            }

            //resolve  sensor 
            SensorAsset sensor;
            if( asset instanceof SensorAsset ){
                //observation is on a sensor procedure (data is correct)
                sensor = (SensorAsset) asset;
            } else if( asset instanceof StationAsset ){
                //observation is on a station procedure, use the procedure for station and unknown for sensor
                sensor = new SensorAsset( (StationAsset) asset, AssetConstants.UNKNOWN );            
            } else {
                //not a station or sensor, don't use this data
                //TODO: throw error?                
                continue;
            }

            AbstractPhenomenon absPhen = obsConst.getObservableProperty();
            Map<String,OmObservableProperty> phenomenaMap = new HashMap<String,OmObservableProperty>();
            if( absPhen instanceof OmCompositePhenomenon ){
                for( OmObservableProperty phen : ( (OmCompositePhenomenon) absPhen).getPhenomenonComponents() ){
                    phenomenaMap.put( phen.getIdentifier(), phen );
                }
            } else {
                OmObservableProperty phen = (OmObservableProperty) absPhen;
                phenomenaMap.put( phen.getIdentifier(), phen );
            }
            List<OmObservableProperty> phenomena = new ArrayList<OmObservableProperty>( phenomenaMap.values() );
            sensorPhens.putAll( sensor, phenomena );

            //get foi
            AbstractFeature aFoi = obsConst.getFeatureOfInterest();
            if( !( aFoi instanceof SamplingFeature ) ){
                throw new NoApplicableCodeException().withMessage("Encountered a feature which isn't a SamplingFeature");
            }
            SamplingFeature foi = (SamplingFeature) aFoi;

            Set<Point> points = FeatureUtil.getFeaturePoints( foi );
            for (Point point : points ){
                try {
                    //TODO is this correct?
                    point = (Point) GeometryHandler.getInstance().switchCoordinateAxisOrderIfNeeded(point);
                } catch (OwsExceptionReport e) {
                    throw new NoApplicableCodeException().withMessage("Exception while normalizing feature coordinate axis order.");
                }
                stationPoints.put( sensor.getStationAsset(), FeatureUtil.clonePoint2d( point ) );
                sensorLngs.put(sensor, point.getX());
                sensorLats.put(sensor, point.getY());
            }            
            Set<Double> featureHeights = FeatureUtil.getFeatureHeights( foi );
            sensorHeights.putAll(sensor, featureHeights);

            boolean isLng = false;
            boolean isLat = false;
            boolean isZ = false;
            boolean isDepth = false;

            //axes shouldn't be composite phenomena
            if( phenomena.size() == 1 ){
                OmObservableProperty phenomenon = phenomena.get(0);
                isLng = AxisUtil.isLng( phenomenon.getIdentifier() );
                isLat = AxisUtil.isLat( phenomenon.getIdentifier() );
                isZ = AxisUtil.isZ( phenomenon.getIdentifier() );
                isDepth = AxisUtil.isDepth( phenomenon.getIdentifier() );
            }

            String phenId = obsConst.getObservableProperty().getIdentifier();
            ObservationValue<?> iObsValue = sosObs.getValue();
            if( !(iObsValue instanceof SingleObservationValue) ){
                throw new NoApplicableCodeException().withMessage("Only SingleObservationValues are supported.");
            }
            SingleObservationValue<?> singleObsValue = (SingleObservationValue<?>) iObsValue;
            Time obsTime = singleObsValue.getPhenomenonTime();
            
            //TODO Quality
            
            Value<?> obsValue = singleObsValue.getValue();
            if( !(obsValue instanceof QuantityValue) ){
                throw new NoApplicableCodeException().withMessage("Only QuantityValues are supported.");                
            }
            QuantityValue quantityValue = (QuantityValue) obsValue;

            //add dimensional values to procedure dimension tracking maps
            if( isLng ){
                sensorLngs.get( sensor ).add( quantityValue.getValue().doubleValue() );
            }

            if( isLat ){
                sensorLats.get( sensor ).add( quantityValue.getValue().doubleValue() );
            }
            
            if( isZ ){                
                Double zValue = quantityValue.getValue().doubleValue();
                if( isDepth ){
                    zValue = 0 - zValue;
                }
                sensorHeights.get( sensor ).add( zValue );
            }                    

            //get the sensor's data map
            Map<Time,Map<OmObservableProperty,Map<SubSensor,Value<?>>>> sensorObsMap = obsValuesMap.get( sensor );
            if( sensorObsMap == null ){
                sensorObsMap = new HashMap<Time,Map<OmObservableProperty,Map<SubSensor,Value<?>>>>();
                obsValuesMap.put( sensor, sensorObsMap );
            }            
            
            //get the map of the asset's phenomena by time
            Map<OmObservableProperty,Map<SubSensor,Value<?>>> obsPropMap = sensorObsMap.get( obsTime );
            if( obsPropMap == null ){
                obsPropMap = new HashMap<OmObservableProperty,Map<SubSensor,Value<?>>>();
                sensorObsMap.put( obsTime, obsPropMap );
            }

            OmObservableProperty phen = phenomenaMap.get( phenId );                        
            Map<SubSensor,Value<?>> subSensorMap = obsPropMap.get( phen );
            if( subSensorMap == null ){
                subSensorMap = new HashMap<SubSensor,Value<?>>();
                obsPropMap.put( phen, subSensorMap );                        
            }

            //add obs value to subsensor map (null subsensors are ok)
            subSensorMap.put(createSubSensor(sensor, foi), obsValue);                   
        }
        
        //now we know about each station's dimensions, sort into CF feature types
        
        //sampling time periods
//        TimePeriod pointSamplingTimePeriod = new TimePeriod();
        TimePeriod timeSeriesSamplingTimePeriod = new TimePeriod();
//        TimePeriod profileSamplingTimePeriod = new TimePeriod(); 
        TimePeriod timeSeriesProfileSamplingTimePeriod = new TimePeriod();
        TimePeriod trajectorySamplingTimePeriod = new TimePeriod();
        TimePeriod trajectoryProfileSamplingTimePeriod = new TimePeriod();
        
        //station datasets
//        Map<SensorAsset,PointSensorDataset> pointSensorDatasets =
//                new HashMap<SensorAsset,PointSensorDataset>();
        Map<SensorAsset,TimeSeriesSensorDataset> timeSeriesSensorDatasets =
                new HashMap<SensorAsset,TimeSeriesSensorDataset>();
//        Map<SensorAsset,ProfileSensorDataset> profileSensorDatasets =
//                new HashMap<SensorAsset,ProfileSensorDataset>();
        Map<SensorAsset,TimeSeriesProfileSensorDataset> timeSeriesProfileSensorDatasets =
                new HashMap<SensorAsset,TimeSeriesProfileSensorDataset>();
        Map<SensorAsset,TrajectorySensorDataset> trajectorySensorDatasets =
                new HashMap<SensorAsset,TrajectorySensorDataset>();
        Map<SensorAsset,TrajectoryProfileSensorDataset> trajectoryProfileSensorDatasets =
                new HashMap<SensorAsset,TrajectoryProfileSensorDataset>();
                
        //phenomena
//        Set<OmObservableProperty> pointPhenomena = new HashSet<OmObservableProperty>();
        Set<OmObservableProperty> timeSeriesPhenomena = new HashSet<OmObservableProperty>();
//        Set<OmObservableProperty> profilePhenomena = new HashSet<OmObservableProperty>();
        Set<OmObservableProperty> timeSeriesProfilePhenomena = new HashSet<OmObservableProperty>();
        Set<OmObservableProperty> trajectoryPhenomena = new HashSet<OmObservableProperty>();
        Set<OmObservableProperty> trajectoryProfilePhenomena = new HashSet<OmObservableProperty>();
        
        //envelopes
//        Envelope pointEnvelope = new Envelope(); 
        Envelope timeSeriesEnvelope = new Envelope();
//        Envelope profileEnvelope = new Envelope();
        Envelope timeSeriesProfileEnvelope = new Envelope();
        Envelope trajectoryEnvelope = new Envelope();
        Envelope trajectoryProfileEnvelope = new Envelope();

        //station points
//        SetMultimap<StationAsset,Point> pointStationPoints = HashMultimap.create(); 
        SetMultimap<StationAsset,Point> timeSeriesStationPoints = HashMultimap.create();
//        SetMultimap<StationAsset,Point> profileStationPoints = HashMultimap.create();
        SetMultimap<StationAsset,Point> timeSeriesProfileStationPoints = HashMultimap.create();
        SetMultimap<StationAsset,Point> trajectoryStationPoints = HashMultimap.create();
        SetMultimap<StationAsset,Point> trajectoryProfileStationPoints = HashMultimap.create();

//        SetMultimap<SensorAsset,Double> pointSensorHeights = HashMultimap.create(); 
        SetMultimap<SensorAsset,Double> timeSeriesSensorHeights = HashMultimap.create();
//        SetMultimap<SensorAsset,Double> profileSensorHeights = HashMultimap.create();
        SetMultimap<SensorAsset,Double> timeSeriesProfileSensorHeights = HashMultimap.create();
        SetMultimap<SensorAsset,Double> trajectorySensorHeights = HashMultimap.create();
        SetMultimap<SensorAsset,Double> trajectoryProfileSensorHeights = HashMultimap.create();
        
        for( Map.Entry<SensorAsset,Map<Time,Map<OmObservableProperty,Map<SubSensor,Value<?>>>>> obsValuesEntry
                : obsValuesMap.entrySet() ){
            SensorAsset sensor = obsValuesEntry.getKey();
            StationAsset station = sensor.getStationAsset();
            Set<Time> sensorTimes = obsValuesEntry.getValue().keySet();
            
            int lngCount = sensorLngs.get( sensor ).size();
            int latCount = sensorLats.get( sensor ).size();
            int heightCount = sensorHeights.get( sensor ).size();
//            int timeCount = sensorTimes.size();
            
            boolean locationVaries = lngCount > 0 && latCount > 0 && (lngCount > 1 || latCount > 1);
            boolean heightVaries = heightCount > 1;
//            boolean timeVaries = timeCount > 1;
            
            //set static dimension values where applicable
            Double staticLng = null;
            Double staticLat = null;
            Double staticHeight = null;
//            Time staticTime = null;
            if( !locationVaries ){
                if( !sensorLngs.get( sensor ).isEmpty() ){
                    staticLng = sensorLngs.get( sensor ).iterator().next();
                }
                if( !sensorLats.get( sensor ).isEmpty() ){
                    staticLat = sensorLats.get( sensor ).iterator().next();
                }
            }
            if( !heightVaries ){
                if( !sensorHeights.get( sensor ).isEmpty() ){                
                    staticHeight = sensorHeights.get( sensor ).iterator().next();
                }
            }
//            if( !timeVaries ){
//                if( !sensorTimes.isEmpty() ){                
//                    staticTime = sensorTimes.iterator().next();
//                }
//            }
            
            //put data on applicable feature type maps
//            if( !locationVaries && !heightVaries && !timeVaries ){
//                //point
//                pointSamplingTimePeriod.extendToContain( sensorTimes );
//                pointSensorDatasets.put( sensor, new PointSensorDataset( sensor, staticLng, staticLat,
//                        staticHeight, staticTime, obsValuesEntry.getValue() ) );
//                pointPhenomena.addAll( sensorPhens.get( sensor ) );
//                if( staticLng != null && staticLat != null ){ 
//                    pointEnvelope.expandToInclude( staticLng, staticLat );
//                }
//                pointStationPoints.putAll( station, stationPoints.get( station ) );
//                if( sensorHeights.get( sensor ) != null ){
//                    pointSensorHeights.putAll( sensor, sensorHeights.get( sensor ) );                 
//                }
//            } else if( !locationVaries && !heightVaries && timeVaries){
          if( !locationVaries && !heightVaries ){            
                //time series
                timeSeriesSamplingTimePeriod.extendToContain( sensorTimes );
                timeSeriesSensorDatasets.put( sensor, new TimeSeriesSensorDataset( sensor, staticLng, staticLat,
                        staticHeight, obsValuesEntry.getValue() ) );
                timeSeriesPhenomena.addAll( sensorPhens.get( sensor ) );
                if( staticLng != null && staticLat != null ){
                    timeSeriesEnvelope.expandToInclude( staticLng, staticLat );
                }
                timeSeriesStationPoints.putAll( station, stationPoints.get( station ) );
                if( sensorHeights.get( sensor ) != null ){                
                    timeSeriesSensorHeights.putAll( sensor, sensorHeights.get( sensor ) );
                }
//            } else if( !locationVaries && heightVaries && !timeVaries ){
//                //profile
//                profileSamplingTimePeriod.extendToContain( sensorTimes );
//                profileSensorDatasets.put( sensor, new ProfileSensorDataset( sensor, staticLng, staticLat,
//                        staticTime, obsValuesEntry.getValue() ) );
//                profilePhenomena.addAll( sensorPhens.get( sensor ) );
//                if( staticLng != null && staticLat != null ){
//                    profileEnvelope.expandToInclude( staticLng, staticLat );
//                }
//                profileStationPoints.putAll( station, stationPoints.get( station ) );
//                if( sensorHeights.get( sensor ) != null ){                
//                  profileSensorHeights.putAll( sensor, sensorHeights.get( sensor ) );
//                }
//            } else if( !locationVaries && heightVaries && timeVaries ){
          } else if( !locationVaries && heightVaries ){                
                //time series profile
                timeSeriesProfileSamplingTimePeriod.extendToContain( sensorTimes );
                timeSeriesProfileSensorDatasets.put( sensor, new TimeSeriesProfileSensorDataset( sensor,
                        staticLng, staticLat, obsValuesEntry.getValue() ) );
                timeSeriesProfilePhenomena.addAll( sensorPhens.get( sensor ) );
                if( staticLng != null && staticLat != null ){
                    timeSeriesProfileEnvelope.expandToInclude( staticLng, staticLat );
                }
                timeSeriesProfileStationPoints.putAll( station, stationPoints.get( station ) );
                if( sensorHeights.get( sensor ) != null ){
                    timeSeriesProfileSensorHeights.putAll( sensor, sensorHeights.get( sensor ) );
                }
//            } else if( locationVaries && !heightVaries && timeVaries ){
          } else if( locationVaries && !heightVaries ){                
                //trajectory
                trajectorySamplingTimePeriod.extendToContain( sensorTimes );
                trajectorySensorDatasets.put( sensor, new TrajectorySensorDataset( sensor, staticHeight,
                        obsValuesEntry.getValue() ) );
                trajectoryPhenomena.addAll( sensorPhens.get( sensor ) );
                expandEnvelopeToInclude( trajectoryEnvelope, sensorLngs.get( sensor ), sensorLats.get( sensor ) );
                trajectoryStationPoints.putAll( station, stationPoints.get( station ) );
                if( sensorHeights.get( sensor ) != null ){
                    trajectorySensorHeights.putAll( sensor, sensorHeights.get( sensor ) );
                }
//            } else if( locationVaries && heightVaries && timeVaries ){
          } else if( locationVaries && heightVaries ){                
                //trajectory profile
                trajectoryProfileSamplingTimePeriod.extendToContain( sensorTimes );
                trajectoryProfileSensorDatasets.put( sensor, new TrajectoryProfileSensorDataset( sensor, 
                        obsValuesEntry.getValue() ) );
                trajectoryProfilePhenomena.addAll( sensorPhens.get( sensor ) );
                expandEnvelopeToInclude( trajectoryProfileEnvelope, sensorLngs.get( sensor ), sensorLats.get( sensor ) );
                trajectoryProfileStationPoints.putAll( station, stationPoints.get( station ) );
                if( sensorHeights.get( sensor ) != null ){
                    trajectoryProfileSensorHeights.putAll( sensor, sensorHeights.get( sensor ) );
                }
            }                     
        }
                
        //build IoosSosObservations        
        List<IoosSosObservation> iSosObsList = new ArrayList<IoosSosObservation>();

        //timeSeries
        if( timeSeriesSensorDatasets.size() > 0 ){
            iSosObsList.add( new IoosSosObservation( CF.FeatureType.timeSeries, timeSeriesSamplingTimePeriod,
                    timeSeriesSensorDatasets, timeSeriesPhenomena, timeSeriesEnvelope, timeSeriesStationPoints ) );
        }

        //time series profile
        if( timeSeriesProfileSensorDatasets.size() > 0 ){
            iSosObsList.add( new IoosSosObservation( CF.FeatureType.timeSeriesProfile, timeSeriesProfileSamplingTimePeriod,
                    timeSeriesProfileSensorDatasets, timeSeriesProfilePhenomena, timeSeriesProfileEnvelope, timeSeriesProfileStationPoints ) );
        }

        //trajectory
        if( trajectorySensorDatasets.size() > 0 ){
            iSosObsList.add( new IoosSosObservation( CF.FeatureType.trajectory, trajectorySamplingTimePeriod,
                    trajectorySensorDatasets, trajectoryPhenomena, trajectoryEnvelope, trajectoryStationPoints ) );
        }

        //trajectoryProfile
        if( trajectoryProfileSensorDatasets.size() > 0 ){
            iSosObsList.add( new IoosSosObservation( CF.FeatureType.trajectoryProfile, trajectoryProfileSamplingTimePeriod,
                    trajectoryProfileSensorDatasets, trajectoryProfilePhenomena, trajectoryProfileEnvelope, trajectoryProfileStationPoints ) );
        }        
        return iSosObsList;
    }

    public static void checkSrid( int srid, Logger logger ) throws InvalidParameterValueException{
        if(  !Ioos52nConstants.ALLOWED_EPSGS.contains( srid ) ){
            throw new InvalidParameterValueException("EPSG", Integer.toString( srid ) );
        }        
    }
    
    public static void expandEnvelopeToInclude( Envelope env, Set<Double> lngs, Set<Double> lats ){
        for( double lng : lngs ){
            env.expandToInclude( lng, env.getMinY() );
        }
        for( double lat : lats ){
            env.expandToInclude( env.getMinX(), lat );
        }         
    }
    
    public static Envelope createEnvelope( Collection<OmObservation> observationCollection ){
        Envelope envelope = null;
        
        for (OmObservation sosObservation : observationCollection) {
            sosObservation.getObservationConstellation().getFeatureOfInterest();
            SamplingFeature samplingFeature = (SamplingFeature) sosObservation.getObservationConstellation().getFeatureOfInterest();
            if( envelope == null ){
                envelope = samplingFeature.getGeometry().getEnvelopeInternal();
            } else {
                envelope.expandToInclude(samplingFeature.getGeometry().getEnvelopeInternal());              
            }
        }
        return envelope;
    }

    public static Envelope swapEnvelopeAxisOrder( Envelope envelope ){
        if (envelope == null) {
            return null;
        }
        return new Envelope(envelope.getMinY(), envelope.getMaxY(),
                envelope.getMinX(), envelope.getMaxX());
    }       
    
    public static SubSensor createSubSensor(SensorAsset sensor, SamplingFeature foi) {
        //return null if sensor or station id is same as foi
        if (sensor.getAssetId().equals(foi.getIdentifierCodeWithAuthority().getValue()) ||
                sensor.getStationAsset().getAssetId().equals(foi.getIdentifierCodeWithAuthority().getValue())) {
            return null;
        }
                
        //check for valid subsensor
        SubSensor subSensor = null;
        Geometry geom = foi.getGeometry();
        if (geom instanceof Point){
            Point point = (Point) geom;
            //profile height
            if (!Double.isNaN(point.getCoordinate().z)) {
                subSensor = new PointProfileSubSensor(point.getCoordinate().z);
            }                
        } else if (geom instanceof LineString) {
            LineString lineString = (LineString) geom;
            //profile bin
            if (lineString.getNumPoints() == 2) {
                Point topPoint = lineString.getPointN(0);
                Point bottomPoint = lineString.getPointN(1);
                
                if (FeatureUtil.equal2d(topPoint, bottomPoint)
                        && !Double.isNaN(topPoint.getCoordinate().z)
                        && !Double.isNaN(bottomPoint.getCoordinate().z)){
                    double topHeight = Math.max(topPoint.getCoordinate().z, bottomPoint.getCoordinate().z);
                    double bottomHeight = Math.min(topPoint.getCoordinate().z, bottomPoint.getCoordinate().z);
                    subSensor = new BinProfileSubSensor(topHeight, bottomHeight);
                }
            }
        }                
        return subSensor;
    }
}
