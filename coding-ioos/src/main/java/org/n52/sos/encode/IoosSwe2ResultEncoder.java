package org.n52.sos.encode;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import net.opengis.swe.x20.BooleanType;
import net.opengis.swe.x20.CategoryType;
import net.opengis.swe.x20.CountPropertyType;
import net.opengis.swe.x20.CountType;
import net.opengis.swe.x20.DataArrayType;
import net.opengis.swe.x20.DataArrayType.ElementType;
import net.opengis.swe.x20.DataChoiceType;
import net.opengis.swe.x20.DataChoiceType.Item;
import net.opengis.swe.x20.DataRecordDocument;
import net.opengis.swe.x20.DataRecordType;
import net.opengis.swe.x20.DataRecordType.Field;
import net.opengis.swe.x20.EncodedValuesPropertyType;
import net.opengis.swe.x20.QuantityRangeType;
import net.opengis.swe.x20.QuantityType;
import net.opengis.swe.x20.TextEncodingType;
import net.opengis.swe.x20.TextType;
import net.opengis.swe.x20.TimeType;
import net.opengis.swe.x20.UnitReference;
import net.opengis.swe.x20.VectorType;
import net.opengis.swe.x20.VectorType.Coordinate;

import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.n52.sos.exception.ows.InvalidParameterValueException;
import org.n52.sos.exception.ows.concrete.UnsupportedEncoderInputException;
import org.n52.sos.ioos.Ioos52nConstants;
import org.n52.sos.ioos.IoosXmlOptionCharEscapeMap;
import org.n52.sos.ioos.asset.AbstractAsset;
import org.n52.sos.ioos.asset.FakeStationAsset;
import org.n52.sos.ioos.asset.SensorAsset;
import org.n52.sos.ioos.asset.StationAsset;
import org.n52.sos.ioos.data.dataset.AbstractSensorDataset;
import org.n52.sos.ioos.data.subsensor.BinProfileSubSensor;
import org.n52.sos.ioos.data.subsensor.IndexedSubSensor;
import org.n52.sos.ioos.data.subsensor.PointProfileSubSensor;
import org.n52.sos.ioos.data.subsensor.ProfileSubSensor;
import org.n52.sos.ioos.data.subsensor.SubSensor;
import org.n52.sos.ioos.om.IoosSosObservation;
import org.n52.sos.ogc.OGCConstants;
import org.n52.sos.ogc.gml.time.Time;
import org.n52.sos.ogc.om.OmConstants;
import org.n52.sos.ogc.om.OmObservableProperty;
import org.n52.sos.ogc.om.values.BooleanValue;
import org.n52.sos.ogc.om.values.CategoryValue;
import org.n52.sos.ogc.om.values.CountValue;
import org.n52.sos.ogc.om.values.QuantityValue;
import org.n52.sos.ogc.om.values.TextValue;
import org.n52.sos.ogc.om.values.Value;
import org.n52.sos.ogc.swe.SweConstants;
import org.n52.sos.util.DateTimeHelper;
import org.n52.sos.util.XmlOptionsHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axiomalaska.cf4j.CFFeatureType;
import com.axiomalaska.cf4j.CFFeatureTypes;
import com.axiomalaska.ioos.sos.IoosCfConstants;
import com.axiomalaska.ioos.sos.IoosDefConstants;
import com.axiomalaska.ioos.sos.IoosSosConstants;
import com.axiomalaska.ioos.sos.IoosSosUtil;
import com.axiomalaska.ioos.sos.IoosSweConstants;
import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.vividsolutions.jts.geom.Point;

/**
 * Isolate the encoding of an ObservationCollection's om:result because IOOS SOS milestone 1
 * uses swe 2.0 in om:result and swe 1.0.1 everywhere else. Keeping om:result encoder in a
 * separate class allows for easy validation that swe 1.0.1 and 2.0 are properly isolated.
 * 
 */
public class IoosSwe2ResultEncoder {
    private static final Logger LOGGER = LoggerFactory.getLogger(IoosSwe2ResultEncoder.class);    
    
    public static XmlObject encodeResult(Encoder<?,?> encoder, IoosSosObservation ioosSosObs )
            throws InvalidParameterValueException, UnsupportedEncoderInputException{       
        CFFeatureType featureType = ioosSosObs.getFeatureType();
        
        //TODO move to OM encoder constructor? might just need to be done once

        //fix xml options for IOOS
        XmlOptions xmlOpts = XmlOptionsHelper.getInstance().getXmlOptions();
        @SuppressWarnings("unchecked")
        Map<String,String> prefixes = (Map<String, String>) xmlOpts.get( XmlOptions.SAVE_SUGGESTED_PREFIXES );
        prefixes.put( SweConstants.NS_SWE_20, Ioos52nConstants.SWE2_PREFIX );
        xmlOpts.setSaveSuggestedPrefixes( prefixes );
        IoosXmlOptionCharEscapeMap charMap = new IoosXmlOptionCharEscapeMap();
        charMap.setEscapeString(Ioos52nConstants.BLOCK_SEPARATOR_TO_ESCAPE, Ioos52nConstants.BLOCK_SEPARATOR_ESCAPED);
        xmlOpts.setSaveSubstituteCharacters(charMap);
        
        // == DATA BLOCK ==
        // == USE SWE 2.0 HERE (AND ONLY HERE) ==
        //parent data record
        DataRecordDocument xb_dataRecordDoc = DataRecordDocument.Factory.newInstance( xmlOpts );
        DataRecordType xb_dataRecord = xb_dataRecordDoc.addNewDataRecord();
        xb_dataRecord.setDefinition(IoosSweConstants.OBSERVATION_RECORD_DEF);
        
        
        //STATIC DATA
        Field xb_stationsField = xb_dataRecord.addNewField();
        xb_stationsField.setName(IoosSweConstants.STATIONS);
        
        DataRecordType xb_staticStationsDataRecord = (DataRecordType) xb_stationsField.addNewAbstractDataComponent()
                .substitute( Ioos52nConstants.QN_DATA_RECORD_SWE_200, DataRecordType.type );
        xb_staticStationsDataRecord.setDefinition(IoosSweConstants.STATIONS_DEF);
        
        //loop through stations
        for( StationAsset station : ioosSosObs.getStations() ){
            //station
            Field xb_stationField = xb_staticStationsDataRecord.addNewField();
            xb_stationField.setName( station.getAssetShortId() );
            
            DataRecordType xb_staticStationDataRecord = (DataRecordType) xb_stationField.addNewAbstractDataComponent()
                    .substitute( Ioos52nConstants.QN_DATA_RECORD_SWE_200, DataRecordType.type );
            xb_staticStationDataRecord.setId( station.getAssetShortId() + "_" + featureType.getName().toLowerCase() );
            xb_staticStationDataRecord.setDefinition(IoosSweConstants.STATION_DEF);
            
            //station id
            Field xb_stationIdField = xb_staticStationDataRecord.addNewField();
            xb_stationIdField.setName(IoosDefConstants.STATION_ID);
            
            TextType xb_stationIdText = (TextType) xb_stationIdField.addNewAbstractDataComponent()
                    .substitute( Ioos52nConstants.QN_TEXT_SWE_200, TextType.type );

            if( station instanceof FakeStationAsset ){
                xb_stationIdText.setDefinition( OGCConstants.URN_UNIQUE_IDENTIFIER );            	
            } else {
                xb_stationIdText.setDefinition( IoosDefConstants.STATION_ID_DEF );            	
            }
            xb_stationIdText.setValue( station.getAssetId() );

            //station location
            Point stationPoint = ioosSosObs.getSingularStationPoint( station );
            
            if( stationPoint != null ){	
        		IoosEncoderUtil.checkSrid( stationPoint.getSRID(), LOGGER );        
            }
            
            createLocationVector(xb_staticStationDataRecord, station, stationPoint);
            
            //sensors
            Field xb_sensorsField = xb_staticStationDataRecord.addNewField();
            xb_sensorsField.setName(IoosSweConstants.SENSORS);
            
            DataRecordType xb_sensorsDataRecord = (DataRecordType) xb_sensorsField.addNewAbstractDataComponent()
                    .substitute( Ioos52nConstants.QN_DATA_RECORD_SWE_200, DataRecordType.type );
            xb_sensorsDataRecord.setDefinition(IoosSweConstants.SENSORS_DEF);
            
            //sensor
            for( SensorAsset sensor : ioosSosObs.getSensors( station )){
                Field xb_sensorField = xb_sensorsDataRecord.addNewField();
                xb_sensorField.setName( sensor.getAssetShortId() );

                DataRecordType xb_sensorDataRecord = (DataRecordType) xb_sensorField.addNewAbstractDataComponent()
                        .substitute( Ioos52nConstants.QN_DATA_RECORD_SWE_200, DataRecordType.type );
                xb_sensorDataRecord.setId( sensor.getAssetShortId() );
                xb_sensorDataRecord.setDefinition(IoosSweConstants.SENSOR_DEF);
                
                //sensor id
                Field xb_sensorIdField = xb_sensorDataRecord.addNewField();
                xb_sensorIdField.setName(IoosDefConstants.SENSOR_ID);

                TextType xb_sensorIdText = (TextType) xb_sensorIdField.addNewAbstractDataComponent()
                        .substitute( Ioos52nConstants.QN_TEXT_SWE_200, TextType.type );

                xb_sensorIdText.setDefinition(IoosDefConstants.SENSOR_ID_DEF);
                xb_sensorIdText.setValue( sensor.getAssetId() );
                
                //TODO sensorOrientation (orientation,pitch,roll)
                //https://code.google.com/p/ioostech/source/browse/trunk/templates/Milestone1.0/SWE-SingleStation-TimeSeriesProfile.xml#121
                //no idea how to handle these, since they don't belong to a specific observation
                
                //sensorLocation (lat/lng/z)                
                //TODO use sensor point instead of station point
                if (featureType.equals(CFFeatureTypes.TIME_SERIES_PROFILE) 
                        || featureType.equals(CFFeatureTypes.PROFILE)) {
                    //create full sensor location for profiles
                    createLocationVector(xb_sensorDataRecord, sensor, stationPoint);
                } else {
                    //default to just showing height
                    Field xbSensorHeightField = xb_sensorDataRecord.addNewField();
                    xbSensorHeightField.setName(IoosCfConstants.HEIGHT);

                    QuantityType xbSensorHeightQuantity = (QuantityType) xbSensorHeightField.addNewAbstractDataComponent()
                            .substitute( Ioos52nConstants.QN_QUANTITY_SWE_200, QuantityType.type );
                    xbSensorHeightQuantity.setDefinition(IoosCfConstants.HEIGHT_DEF);
                    xbSensorHeightQuantity.setAxisID( IoosSosConstants.HEIGHT_AXIS_ID );
                    xbSensorHeightQuantity.setReferenceFrame(getFrame(station));
                    xbSensorHeightQuantity.addNewUom().setCode( IoosSosConstants.METER_UOM );
                    if( stationPoint != null && stationPoint.getCoordinate() != null
                            && !Double.isNaN(stationPoint.getCoordinate().z ) ){
                        xbSensorHeightQuantity.setValue( stationPoint.getCoordinate().z );
                    }                    
                }

                //encode indexed subsensors
                List<SubSensor> subSensors = ioosSosObs.getSensorDataset(sensor).getSubSensors();
                if (subSensors != null && !subSensors.isEmpty()) {
                    List<PointProfileSubSensor> profilePoints = new ArrayList<PointProfileSubSensor>();
                    List<BinProfileSubSensor> profileBins = new ArrayList<BinProfileSubSensor>();
                    for (SubSensor subSensor : subSensors ){
                        if( subSensor instanceof PointProfileSubSensor) {
                            profilePoints.add((PointProfileSubSensor) subSensor);
                        } else if( subSensor instanceof BinProfileSubSensor) {
                            profileBins.add((BinProfileSubSensor) subSensor);
                        }
                    }
                    
                    //check to see if more than one type was encountered
                    if (Collections.frequency(Arrays.asList(!profilePoints.isEmpty(),
                            !profileBins.isEmpty()), true) > 1){
                        throw new UnsupportedEncoderInputException(encoder,
                                "Multiple subsensor types encountered in the same sensor"); 
                    }
                    
                    if (!profilePoints.isEmpty()){
                        //PROFILE POINTS (thermistor, etc)
                        encodeProfileSubSensors(xb_sensorDataRecord, sensor, profilePoints);
                    } else if  (!profileBins.isEmpty()){
                        //PROFILE BINS (ADCP, etc)
                        encodeProfileSubSensors(xb_sensorDataRecord, sensor, profileBins);                        
                    }
                }
            }
        }
        
        //DYNAMIC DATA
        //(time, any varying location data, and sensor observations )
        //dynamic data references above static data using sensor asset short id
        Field xb_observationDataField = xb_dataRecord.addNewField();
        xb_observationDataField.setName(IoosSweConstants.OBSERVATION_DATA);
        
        DataArrayType xb_dataArray = (DataArrayType) xb_observationDataField.addNewAbstractDataComponent()
            .substitute( Ioos52nConstants.QN_DATA_ARRAY_SWE_200, DataArrayType.type );
        xb_dataArray.setDefinition(IoosSweConstants.SENSOR_OBSERVATION_COLLECTION_DEF);
        
        CountPropertyType xb_elementCount = xb_dataArray.addNewElementCount();
        CountType xb_dataArrayCount = xb_elementCount.addNewCount();

        //observations
        ElementType xb_elementType = xb_dataArray.addNewElementType();
        xb_elementType.setName(IoosSweConstants.OBSERVATIONS);
        
        DataRecordType xb_observationsDataRecord = (DataRecordType) xb_elementType.addNewAbstractDataComponent()
                .substitute( Ioos52nConstants.QN_DATA_RECORD_SWE_200, DataRecordType.type );
        xb_observationsDataRecord.setDefinition(IoosSweConstants.SENSOR_OBSERVATIONS_DEF);
        
        Field xb_timeField = xb_observationsDataRecord.addNewField();
        xb_timeField.setName( IoosSosConstants.TIME );
        
        TimeType xb_time = (TimeType) xb_timeField.addNewAbstractDataComponent()
                .substitute( Ioos52nConstants.QN_TIME_SWE_200, TimeType.type );
        xb_time.setDefinition( OmConstants.PHEN_SAMPLING_TIME );
        xb_time.addNewUom().setHref( OmConstants.PHEN_UOM_ISO8601 );
        
        Field xb_sensorField = xb_observationsDataRecord.addNewField();
        xb_sensorField.setName(IoosSweConstants.SENSOR);
        
        DataChoiceType xb_dataChoice = (DataChoiceType) xb_sensorField.addNewAbstractDataComponent()
                .substitute( Ioos52nConstants.QN_DATA_CHOICE_SWE_200, DataChoiceType.type );
        xb_dataChoice.setDefinition(IoosSweConstants.SENSORS_DEF);
        
        //get value types for phen, XXX pretty hacky
        @SuppressWarnings("rawtypes")
        Map<OmObservableProperty,Class<? extends Value>> obsPropValueTypes = mapValueTypesForObsProps( ioosSosObs );        
        
        //loop through stations and sensors to create data choices for observed properties        
        for( StationAsset station : ioosSosObs.getStations() ){
            for( SensorAsset sensor : ioosSosObs.getSensors(station) ){
            AbstractSensorDataset sensorDataset = ioosSosObs.getSensorDataset( sensor );
                Item xb_sensorItem = xb_dataChoice.addNewItem();
                xb_sensorItem.setName( sensor.getAssetShortId() );

                DataRecordType xb_sensorDataRecord = (DataRecordType) xb_sensorItem.addNewAbstractDataComponent()
                        .substitute( Ioos52nConstants.QN_DATA_RECORD_SWE_200, DataRecordType.type );
                xb_sensorDataRecord.setDefinition(IoosSweConstants.SENSOR_DEF);

                DataRecordType xbDataRecordForObsProps = xb_sensorDataRecord;
                
                if (featureType.equals(CFFeatureTypes.TIME_SERIES_PROFILE) 
                        || featureType.equals(CFFeatureTypes.PROFILE)) {
                    Field xbProfileField = xb_sensorDataRecord.addNewField();
                    xbProfileField.setName(IoosSweConstants.PROFILE);
                    
                    DataArrayType xbProfileDataArray = (DataArrayType) xbProfileField.addNewAbstractDataComponent()
                            .substitute(SweConstants.QN_DATA_ARRAY_SWE_200, DataArrayType.type);
                    xbProfileDataArray.setDefinition(IoosSweConstants.PROFILE_DEF);
                    
                    //empty count, which will be encoded in values block
                    xbProfileDataArray.addNewElementCount().addNewCount();
                    
                    ElementType xbElementType = xbProfileDataArray.addNewElementType();
                    xbElementType.setName(IoosSweConstants.PROFILE_OBSERVATION);
                   
                    DataRecordType xbProfileObservationDataRecord = (DataRecordType) xbElementType.addNewAbstractDataComponent()
                            .substitute( Ioos52nConstants.QN_DATA_RECORD_SWE_200, DataRecordType.type );
                    xbProfileObservationDataRecord.setDefinition(IoosSweConstants.PROFILE_OBSERVATION_DEF);
                    
                    //profile index field
                    Field xbProfileIndexField = xbProfileObservationDataRecord.addNewField();
                    xbProfileIndexField.setName(IoosSweConstants.PROFILE_INDEX);
                    CountType xbProfileIndexCount = (CountType) xbProfileIndexField.addNewAbstractDataComponent()
                            .substitute( Ioos52nConstants.QN_COUNT_SWE_200, CountType.type );
                    xbProfileIndexCount.setDefinition(IoosSweConstants.PROFILE_INDEX_DEF);
                    int numSubSensors = sensorDataset.getSubSensors().size();
                    xbProfileIndexCount.addNewConstraint().addNewAllowedValues().addInterval(
                            Lists.newArrayList(0, numSubSensors - 1));
                    
                    xbDataRecordForObsProps = xbProfileObservationDataRecord;
                }

                for( OmObservableProperty obsProp : sensorDataset.getPhenomena() ){                	
                    addPhenToDataRecord( obsPropValueTypes.get( obsProp ), xbDataRecordForObsProps, obsProp );
                }
            }
        }
        fixDataChoice(xb_dataChoice);
        
        addTextEncoding(xb_dataArray);

        //values
        EncodedValuesResult encodedValues = createResultString( ioosSosObs );
        addValues(xb_dataArray, encodedValues.getEncodedValuesString());
        xb_dataArrayCount.setValue( BigInteger.valueOf( encodedValues.getCount() ) );

        return xb_dataRecordDoc;
    }

    private static void encodeProfileSubSensors(DataRecordType xbDataRecord, SensorAsset sensor,
            List<? extends ProfileSubSensor> subSensors){
        if (subSensors == null || subSensors.isEmpty() ){
            return;
        }
        ProfileSubSensor firstSubSensor = subSensors.get(0);
        
        Field xbSubSensorField = xbDataRecord.addNewField();
        
        DataArrayType xbDataArray = (DataArrayType) xbSubSensorField.addNewAbstractDataComponent()
                .substitute(SweConstants.QN_DATA_ARRAY_SWE_200, DataArrayType.type);
        xbDataArray.addNewElementCount().addNewCount().setValue(BigInteger.valueOf(subSensors.size()));        
        ElementType xbElementType = xbDataArray.addNewElementType();
        
        DataRecordType xbProfileDataRecord = (DataRecordType) xbElementType.addNewAbstractDataComponent()
                .substitute(SweConstants.QN_DATA_RECORD_SWE_200, DataRecordType.type);
        
        Field xbHeightField = xbProfileDataRecord.addNewField();        
        QuantityType xbHeightQuantity = (QuantityType) xbHeightField.addNewAbstractDataComponent()
                .substitute(SweConstants.QN_QUANTITY_SWE_200, QuantityType.type);                        
        xbHeightQuantity.setAxisID(IoosSosConstants.HEIGHT_AXIS_ID);
        xbHeightQuantity.setDefinition(IoosCfConstants.HEIGHT_DEF);
        xbHeightQuantity.setReferenceFrame(getFrame(sensor));
        xbHeightQuantity.addNewUom().setCode(IoosSosConstants.METER_UOM);
        
        if (firstSubSensor instanceof PointProfileSubSensor) {
            xbSubSensorField.setName(IoosSweConstants.PROFILE_HEIGHTS);
            xbDataArray.setDefinition(IoosSweConstants.PROFILE_HEIGHTS_DEF);
            xbElementType.setName(IoosSweConstants.PROFILE_DEFINITION);
            xbProfileDataRecord.setDefinition(IoosSweConstants.PROFILE_HEIGHT_DEF);
            xbHeightField.setName(IoosCfConstants.HEIGHT);                        
        } else if (firstSubSensor instanceof BinProfileSubSensor) {
            xbSubSensorField.setName(IoosSweConstants.PROFILE_BINS);
            xbDataArray.setDefinition(IoosSweConstants.PROFILE_BINS_DEF);
            xbElementType.setName(IoosSweConstants.PROFILE_BIN_DESCRIPTION);
            xbProfileDataRecord.setDefinition(IoosSweConstants.PROFILE_BIN_DEF);
            xbHeightField.setName(IoosSweConstants.BIN_CENTER);
            
            Field xbBinEdgesField = xbProfileDataRecord.addNewField();
            xbBinEdgesField.setName(IoosSweConstants.BIN_EDGES);
            QuantityRangeType xbBinEdgesQuantityRange = (QuantityRangeType) xbBinEdgesField.addNewAbstractDataComponent()
                    .substitute(SweConstants.QN_QUANTITY_RANGE_SWE_200, QuantityRangeType.type);                        
            xbBinEdgesQuantityRange.setAxisID(IoosSosConstants.HEIGHT_AXIS_ID);
            xbBinEdgesQuantityRange.setDefinition(IoosSweConstants.PROFILE_BIN_EDGES_DEF);
            xbBinEdgesQuantityRange.setReferenceFrame(getFrame(sensor));
            xbBinEdgesQuantityRange.addNewUom().setCode(IoosSosConstants.METER_UOM);
        }
        
        //encode values
        StringBuilder encodedValues = new StringBuilder();
        for (ProfileSubSensor subSensor : subSensors) {            
            encodedValues.append(subSensor.getHeight());
            if (subSensor instanceof BinProfileSubSensor) {
                BinProfileSubSensor bpss = (BinProfileSubSensor) subSensor;
                encodedValues.append(Ioos52nConstants.TOKEN_SEPARATOR);
                encodedValues.append(bpss.getTopHeight() + " " + bpss.getBottomHeight());
            }            
            encodedValues.append(Ioos52nConstants.BLOCK_SEPARATOR);            
        }
        
        addTextEncoding(xbDataArray);        
        addValues(xbDataArray, encodedValues.toString());        
    }
    

    private static void createLocationVector(DataRecordType xbDataRecord, AbstractAsset asset, Point point) {
        Field xbField = xbDataRecord.addNewField();
        
        
        VectorType xbVector = (VectorType) xbField.addNewAbstractDataComponent()
                .substitute( Ioos52nConstants.QN_VECTOR_SWE_200, VectorType.type );
        
        if (asset instanceof StationAsset) {
            xbField.setName(IoosSweConstants.PLATFORM_LOCATION);
            xbVector.setDefinition(IoosSweConstants.PLATFORM_LOCATION_DEF);    
        } else if (asset instanceof SensorAsset) {
            xbField.setName(IoosSweConstants.SENSOR_LOCATION);
            xbVector.setDefinition(IoosSweConstants.SENSOR_LOCATION_DEF);
        }

        xbVector.setReferenceFrame( IoosSosConstants.LOCATION_REFERENCE_FRAME );
        xbVector.setLocalFrame(getFrame(asset));

        //latitude
        Coordinate xb_latitudeCoordinate = xbVector.addNewCoordinate();
        xb_latitudeCoordinate.setName(IoosCfConstants.LATITUDE);
        QuantityType xb_latitudeQuantity = xb_latitudeCoordinate.addNewQuantity();
        xb_latitudeQuantity.setDefinition(IoosCfConstants.LATITUDE_DEF);
        xb_latitudeQuantity.setAxisID(IoosSosConstants.LAT_AXIS_ID);
        xb_latitudeQuantity.addNewUom().setCode(IoosSosConstants.DEGREE_UOM);
        if (point != null) {
            xb_latitudeQuantity.setValue(point.getY());
        }

        //longitude
        Coordinate xb_longitudeCoordinate = xbVector.addNewCoordinate();
        xb_longitudeCoordinate.setName(IoosCfConstants.LONGITUDE);
        QuantityType xb_longitudeQuantity = xb_longitudeCoordinate.addNewQuantity();
        xb_longitudeQuantity.setDefinition(IoosCfConstants.LONGITUDE_DEF);
        xb_longitudeQuantity.setAxisID( IoosSosConstants.LNG_AXIS_ID );
        xb_longitudeQuantity.addNewUom().setCode( IoosSosConstants.DEGREE_UOM );
        if( point != null ){
            xb_longitudeQuantity.setValue(point.getX());
        }
        
        //height
        Coordinate xb_heightCoordinate = xbVector.addNewCoordinate();
        xb_heightCoordinate.setName(IoosCfConstants.HEIGHT);
        QuantityType xb_heightQuantity = xb_heightCoordinate.addNewQuantity();
        xb_heightQuantity.setDefinition(IoosCfConstants.HEIGHT_DEF);
        xb_heightQuantity.setAxisID(IoosSosConstants.HEIGHT_AXIS_ID);
        xb_heightQuantity.addNewUom().setCode( IoosSosConstants.METER_UOM );
        if (point != null && point.getCoordinate() != null && !Double.isNaN(point.getCoordinate().z )) {
            xb_heightQuantity.setValue(point.getCoordinate().z);
        }
    }

    @SuppressWarnings("rawtypes")
    private static Map<OmObservableProperty,Class<? extends Value>> mapValueTypesForObsProps( IoosSosObservation ioosSosObs ){
    	Map<OmObservableProperty,Class<? extends Value>> obsPropValueTypeMap = new HashMap<OmObservableProperty,Class<? extends Value>>();
        for( AbstractSensorDataset sensorDataset : ioosSosObs.getSensorDatasets() ){
        	for( Map<OmObservableProperty, Map<SubSensor, Value<?>>> sensorDataValues : sensorDataset.getDataValues().values() ){
        		for( OmObservableProperty obsProp : sensorDataValues.keySet() ){
        			if( obsPropValueTypeMap.get( obsProp ) == null ){
                    	for( Value<?> value : sensorDataValues.get( obsProp ).values() ){
                    		obsPropValueTypeMap.put( obsProp, value.getClass() );
                    	}
        			}
        		}
        	}        	
        }
        return obsPropValueTypeMap;
    }
    
    /**
     * Add a phenomenon component to the DataRecord, based on the phenomenon's value type 
     * 
     * @param valueType
     * @param xb_dataRecord The DataRecord to add the phenomenon component to
     * @param obsProp The observable property to add
     */
    @SuppressWarnings("rawtypes")
    public static void addPhenToDataRecord( Class<? extends Value> valueType, DataRecordType xb_dataRecord,
    		OmObservableProperty phenComponent ){
        Field xbField = xb_dataRecord.addNewField();
        if( valueType != null ){
        	if( valueType.equals( BooleanValue.class ) ){
                BooleanType xbBool = (BooleanType) xbField.addNewAbstractDataComponent()
                    .substitute( Ioos52nConstants.QN_BOOLEAN_SWE_200, BooleanType.type );
                xbBool.setDefinition(phenComponent.getIdentifier());
        	} else if( valueType.equals( CountValue.class ) ){
                CountType xbCount = (CountType) xbField.addNewAbstractDataComponent()
                    .substitute( Ioos52nConstants.QN_COUNT_SWE_200, CountType.type );
                xbCount.setDefinition(phenComponent.getIdentifier());
        	} else if( valueType.equals( QuantityValue.class ) ){
                QuantityType xbQuantity = (QuantityType) xbField.addNewAbstractDataComponent()
                    .substitute( Ioos52nConstants.QN_QUANTITY_SWE_200, QuantityType.type );
                xbQuantity.setDefinition(phenComponent.getIdentifier());
                if (phenComponent.getUnit() != null) {
                    xbQuantity.setUom(createUnitReference(phenComponent.getUnit()));
                }
        	} else if( valueType.equals( TextValue.class ) ){
                TextType xbText = (TextType) xbField.addNewAbstractDataComponent()
                    .substitute( Ioos52nConstants.QN_TEXT_SWE_200, TextType.type );
                xbText.setDefinition(phenComponent.getIdentifier());
        	} else if( valueType.equals( CategoryValue.class ) ){
                CategoryType xbCategory = (CategoryType) xbField.addNewAbstractDataComponent()
                    .substitute( Ioos52nConstants.QN_CATEGORY_SWE_200, CategoryType.type );
                xbCategory.setDefinition(phenComponent.getIdentifier());
        	}
        }
        if( !xbField.isSetAbstractDataComponent() ){
            TextType xbText = (TextType) xbField.addNewAbstractDataComponent()
                    .substitute( Ioos52nConstants.QN_TEXT_SWE_200, TextType.type );
            xbText.setDefinition(phenComponent.getIdentifier());
        }
        //set name to phen uri ending
        xbField.setName(IoosSosUtil.getNameFromUri(phenComponent.getIdentifier()));
    }
    
    private static UnitReference createUnitReference(String uom) {
        if (Strings.isNullOrEmpty(uom)){
            return null;
        }
        UnitReference unitReference = UnitReference.Factory.newInstance(XmlOptionsHelper.getInstance().getXmlOptions());
        if (uom.startsWith("urn:") || uom.startsWith("http://")) {
            unitReference.setHref(uom);
        } else {
            unitReference.setCode(uom);
        }
        return unitReference;
    }    
    
    private static EncodedValuesResult createResultString( IoosSosObservation ioosSosObs ){
        CFFeatureType featureType = ioosSosObs.getFeatureType();
        
        // value matrix which should be built
        StringBuffer valueMatrix = new StringBuffer();

        //keep a reference to subsensors
        ListMultimap<SensorAsset,SubSensor> sensorSubSensors = ArrayListMultimap.create();
        
        //remap the values by time, station, sensor, observation foi altitude, phenomenon to value
        SortedMap<Time, SortedMap<StationAsset, SortedMap<SensorAsset,SortedMap<SubSensor,SortedMap<OmObservableProperty, Value<?>>>>>> values = 
                new TreeMap<Time, SortedMap<StationAsset, SortedMap<SensorAsset, SortedMap<SubSensor, SortedMap<OmObservableProperty, Value<?>>>>>>();
        for( StationAsset station : ioosSosObs.getStations() ){
            for( SensorAsset sensor : ioosSosObs.getSensors(station) ){
                AbstractSensorDataset sensorDataset = ioosSosObs.getSensorDataset(sensor);                

                for( Entry<Time, Map<OmObservableProperty, Map<SubSensor, Value<?>>>> timeEntry : sensorDataset.getDataValues().entrySet() ){

                    Time time = timeEntry.getKey();

                    SortedMap<StationAsset, SortedMap<SensorAsset, SortedMap<SubSensor, SortedMap<OmObservableProperty, Value<?>>>>> timeValues = values.get( time );
                    if( timeValues == null ){
                        timeValues = new TreeMap<StationAsset, SortedMap<SensorAsset, SortedMap<SubSensor, SortedMap<OmObservableProperty, Value<?>>>>>();
                        values.put( time, timeValues );
                    }

                    SortedMap<SensorAsset, SortedMap<SubSensor, SortedMap<OmObservableProperty, Value<?>>>> stationValues = timeValues.get( station );
                    if( stationValues == null ){
                        stationValues = new TreeMap<SensorAsset, SortedMap<SubSensor, SortedMap<OmObservableProperty, Value<?>>>>();
                        timeValues.put( station, stationValues );
                    }

                    //track subsensors
                    sensorSubSensors.putAll(sensor, sensorDataset.getSubSensors());

                    SortedMap<SubSensor, SortedMap<OmObservableProperty, Value<?>>> sensorValues = stationValues.get( sensor );
                    if( sensorValues == null ){
                        //provide a null handling comparator for subsensor keys
                        sensorValues = new TreeMap<SubSensor, SortedMap<OmObservableProperty, Value<?>>>(
                            new Comparator<SubSensor>(){
                                @Override
                                public int compare(SubSensor o1, SubSensor o2) {
                                    if(o1 == null ^ o2 == null ){
                                        return o1 == null ? -1 : 1;
                                    }
                                    if( o1 == null && o2 == null ){
                                        return 0;
                                    }
                                    return o1.compareTo(o2);
                                }
                            });                                
                        stationValues.put( sensor, sensorValues );
                    }

                    for( Entry<OmObservableProperty, Map<SubSensor,Value<?>>> phenEntry : timeEntry.getValue().entrySet() ){
                        OmObservableProperty phen = phenEntry.getKey();
                        for( Entry<SubSensor,Value<?>> subSensorEntry : phenEntry.getValue().entrySet() ){
                            SubSensor subSensor = subSensorEntry.getKey();
                            Value<?> value = subSensorEntry.getValue();
                            
                            SortedMap<OmObservableProperty,Value<?>> subSensorValues = sensorValues.get( subSensor );
                            if( subSensorValues == null ){
                                subSensorValues = new TreeMap<OmObservableProperty, Value<?>>();
                                sensorValues.put( subSensor, subSensorValues );
                            }
                            
                            subSensorValues.put( phen, value );
                        }
                    }
                }
            }
        }

        //now loop through the organized data and encode it
        int recordCount = 0;
        
        //time
        for( Time time : values.keySet() ){            
            SortedMap<StationAsset, SortedMap<SensorAsset, SortedMap<SubSensor, SortedMap<OmObservableProperty, Value<?>>>>> stationSensorMap = values.get( time );
            //station
            for( StationAsset station : stationSensorMap.keySet() ){
                SortedMap<SensorAsset, SortedMap<SubSensor, SortedMap<OmObservableProperty, Value<?>>>> sensorGeomMap = stationSensorMap.get( station );
                //sensor
                for( SensorAsset sensor : sensorGeomMap.keySet() ){
                    SortedMap<SubSensor, SortedMap<OmObservableProperty, Value<?>>> subSensorMap = sensorGeomMap.get( sensor );
                    
                    //encode
                    
                    //time
                    valueMatrix.append( DateTimeHelper.format( time ) );
                    valueMatrix.append( Ioos52nConstants.TOKEN_SEPARATOR );

                    //sensor data choice (short urn)
                    valueMatrix.append( sensor.getAssetShortId() );
                    valueMatrix.append( Ioos52nConstants.TOKEN_SEPARATOR );

                    //subsensor dataarray element count
                    if (featureType.equals(CFFeatureTypes.TIME_SERIES_PROFILE) 
                            || featureType.equals(CFFeatureTypes.PROFILE)) {
                        valueMatrix.append( subSensorMap.size() );
                        valueMatrix.append( Ioos52nConstants.TOKEN_SEPARATOR );
                    }

                    //subsensor (profile height, profile bin, etc)
                    for( SubSensor subSensor : subSensorMap.keySet() ){
                        //encode index for profiles                        
                        if (featureType.equals(CFFeatureTypes.TIME_SERIES_PROFILE) || featureType.equals(CFFeatureTypes.PROFILE)) {                        
                            if (subSensor != null && subSensor instanceof IndexedSubSensor){
                                //output subsensor index
                                valueMatrix.append( sensorSubSensors.get(sensor).indexOf(subSensor) );                                
                            }
                            valueMatrix.append( Ioos52nConstants.TOKEN_SEPARATOR );                            
                        }

                        SortedMap<OmObservableProperty, Value<?>> obsPropMap = subSensorMap.get( subSensor );                                                
                        //output data values                            
                        for( OmObservableProperty phen : obsPropMap.keySet() ){
                        	Value<?> value = obsPropMap.get( phen );

                        	//TODO check value type 

                            //value
                            if( value != null ){
                                valueMatrix.append( value.getValue() );                            
                            }
                            valueMatrix.append( Ioos52nConstants.TOKEN_SEPARATOR );
                        }
                    }

                    //delete last TokenSeperator
                    valueMatrix.delete( valueMatrix.length() - Ioos52nConstants.TOKEN_SEPARATOR.length(), valueMatrix.length() );
                    valueMatrix.append( Ioos52nConstants.BLOCK_SEPARATOR );
                    
                    recordCount++;                    
                }                
            }                            
        }
        
        return new EncodedValuesResult( valueMatrix.toString(), recordCount );
    }
    
    private static void addTextEncoding(DataArrayType xbDataArray) {
        TextEncodingType xb_textEncoding = (TextEncodingType) xbDataArray.addNewEncoding().addNewAbstractEncoding()
                .substitute( SweConstants.QN_TEXT_ENCODING_SWE_200, TextEncodingType.type );
        xb_textEncoding.setDecimalSeparator( Ioos52nConstants.DECIMAL_SEPARATOR );
        xb_textEncoding.setTokenSeparator( Ioos52nConstants.TOKEN_SEPARATOR );
        xb_textEncoding.setBlockSeparator( Character.toString(Ioos52nConstants.BLOCK_SEPARATOR_TO_ESCAPE) );
    }
    
    private static void addValues(DataArrayType xbDataArray, String values){
        EncodedValuesPropertyType xb_values = xbDataArray.addNewValues();        
        xb_values.newCursor().setTextValue( values );           
    }
    
    private static String getFrame(AbstractAsset asset) {
        return "#" + asset.getAssetShortId() + IoosSweConstants.FRAME_SUFFIX;
    }
    
    private static void fixDataChoice(DataChoiceType xbDataChoice) {
        if (xbDataChoice.getItemArray().length < 2) {
            xbDataChoice.addNewItem().setName(IoosSosConstants.DUMMY_ITEM);
        }
    }
}
