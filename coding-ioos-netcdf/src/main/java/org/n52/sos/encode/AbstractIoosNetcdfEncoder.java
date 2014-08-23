package org.n52.sos.encode;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.n52.sos.ds.AbstractDescribeSensorDAO;
import org.n52.sos.ds.OperationDAO;
import org.n52.sos.ds.OperationDAORepository;
import org.n52.sos.exception.CodedException;
import org.n52.sos.exception.ows.NoApplicableCodeException;
import org.n52.sos.exception.ows.concrete.UnsupportedEncoderInputException;
import org.n52.sos.ioos.IoosUtil;
import org.n52.sos.ioos.asset.SensorAsset;
import org.n52.sos.ioos.asset.StationAsset;
import org.n52.sos.ioos.data.dataset.AbstractSensorDataset;
import org.n52.sos.ioos.data.dataset.IStaticAltitudeDataset;
import org.n52.sos.ioos.data.dataset.IStaticLocationDataset;
import org.n52.sos.ioos.data.subsensor.BinProfileSubSensor;
import org.n52.sos.ioos.data.subsensor.ProfileSubSensor;
import org.n52.sos.ioos.data.subsensor.SubSensor;
import org.n52.sos.ioos.om.IoosSosObservation;
import org.n52.sos.netcdf.IoosNc4ChunkingStrategy;
import org.n52.sos.ogc.gml.time.Time;
import org.n52.sos.ogc.gml.time.TimeInstant;
import org.n52.sos.ogc.om.OmConstants;
import org.n52.sos.ogc.om.OmObservableProperty;
import org.n52.sos.ogc.om.OmObservation;
import org.n52.sos.ogc.om.values.Value;
import org.n52.sos.ogc.ows.OwsExceptionReport;
import org.n52.sos.ogc.sensorML.AbstractProcess;
import org.n52.sos.ogc.sensorML.SensorML;
import org.n52.sos.ogc.sensorML.SmlContact;
import org.n52.sos.ogc.sensorML.SmlContactList;
import org.n52.sos.ogc.sensorML.SmlResponsibleParty;
import org.n52.sos.ogc.sensorML.System;
import org.n52.sos.ogc.sensorML.elements.SmlClassifier;
import org.n52.sos.ogc.sensorML.elements.SmlIdentifier;
import org.n52.sos.ogc.sos.Sos1Constants;
import org.n52.sos.ogc.sos.Sos2Constants;
import org.n52.sos.ogc.sos.SosConstants;
import org.n52.sos.ogc.sos.SosConstants.HelperValues;
import org.n52.sos.ogc.sos.SosProcedureDescription;
import org.n52.sos.request.DescribeSensorRequest;
import org.n52.sos.response.BinaryAttachmentResponse;
import org.n52.sos.response.DescribeSensorResponse;
import org.n52.sos.response.GetObservationByIdResponse;
import org.n52.sos.response.GetObservationResponse;
import org.n52.sos.service.ServiceConstants.SupportedTypeKey;
import org.n52.sos.util.CollectionHelper;
import org.n52.sos.w3c.SchemaLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ucar.ma2.Array;
import ucar.ma2.ArrayChar;
import ucar.ma2.ArrayDouble;
import ucar.ma2.ArrayFloat;
import ucar.ma2.DataType;
import ucar.ma2.Index;
import ucar.ma2.IndexIterator;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.NetcdfFileWriter.Version;
import ucar.nc2.Variable;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;

import com.axiomalaska.cf4j.CFStandardNames;
import com.axiomalaska.cf4j.constants.ACDDConstants;
import com.axiomalaska.cf4j.constants.CFConstants;
import com.axiomalaska.cf4j.constants.NODCConstants;
import com.axiomalaska.ioos.sos.IoosCfConstants;
import com.axiomalaska.ioos.sos.IoosDefConstants;
import com.axiomalaska.ioos.sos.IoosNetcdfConstants;
import com.axiomalaska.ioos.sos.IoosSosConstants;
import com.axiomalaska.ioos.sos.IoosSosUtil;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public abstract class AbstractIoosNetcdfEncoder implements ObservationEncoder<BinaryAttachmentResponse, Object>{
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractIoosNetcdfEncoder.class);
    private static final String DEFINITION = "definition";
    private static final double DOUBLE_FILL_VALUE = -9999.9;
    private static final float FLOAT_FILL_VALUE = -9999.9f;
    private static final int CHUNK_SIZE_TIME = 1000;

    private static final Map<SupportedTypeKey, Set<String>> SUPPORTED_TYPES = Collections.singletonMap(
            SupportedTypeKey.ObservationType, Collections.singleton(OmConstants.OBS_TYPE_MEASUREMENT));

    private static final Set<String> CONFORMANCE_CLASSES = ImmutableSet.of(
            "http://www.opengis.net/spec/OMXML/1.0/conf/measurement");

    private final Map<String, Map<String, Set<String>>> SUPPORTED_RESPONSE_FORMATS = Collections.singletonMap(
            SosConstants.SOS, (Map<String, Set<String>>) new ImmutableMap.Builder<String, Set<String>>()
            .put(Sos1Constants.SERVICEVERSION, Collections.singleton(getContentType().toString()))
            .put(Sos2Constants.SERVICEVERSION, Collections.singleton(getContentType().toString()))
            .build());

    private final Set<EncoderKey> ENCODER_KEYS = Sets.newHashSet(
          (EncoderKey) new OperationEncoderKey(SosConstants.SOS, Sos1Constants.SERVICEVERSION,
                  SosConstants.Operations.GetObservation, getContentType()),
          (EncoderKey) new OperationEncoderKey(SosConstants.SOS, Sos2Constants.SERVICEVERSION,
                  SosConstants.Operations.GetObservation, getContentType()));

    
    public AbstractIoosNetcdfEncoder() {
        LOGGER.debug("Encoder for the following keys initialized successfully: {}!",
                Joiner.on(", ").join(ENCODER_KEYS));
    }
    
    @Override
    public Set<EncoderKey> getEncoderKeyType() {
        return Collections.unmodifiableSet(ENCODER_KEYS);
    }

    @Override
    public Map<SupportedTypeKey, Set<String>> getSupportedTypes() {
        return Collections.unmodifiableMap(SUPPORTED_TYPES);
    }

    @Override
    public Set<String> getConformanceClasses() {
        return Collections.unmodifiableSet(CONFORMANCE_CLASSES);
    }

    @Override
    public void addNamespacePrefixToMap(Map<String, String> nameSpacePrefixMap) {
        // NOOP, no need (we're not encoding xml)
    }    

    @Override
    public boolean isObservationAndMeasurmentV20Type() {
        return false;
    }

    @Override
    public Set<String> getSupportedResponseFormats(String service, String version) {
        if (SUPPORTED_RESPONSE_FORMATS.get(service) != null) {
            if (SUPPORTED_RESPONSE_FORMATS.get(service).get(version) != null) {
                return SUPPORTED_RESPONSE_FORMATS.get(service).get(version);
            }
        }
        return Collections.emptySet();
    }

    @Override
    public boolean shouldObservationsWithSameXBeMerged() {
        return false;
    }

    @Override
    public Set<SchemaLocation> getSchemaLocations() {
        //NOOP
        return null;
    }

    @Override
    public BinaryAttachmentResponse encode(Object element) throws OwsExceptionReport {
        return encode(element, new EnumMap<HelperValues, String>(HelperValues.class));
    }

    @Override
    public BinaryAttachmentResponse encode(Object objectToEncode, Map<HelperValues, String> additionalValues)
            throws OwsExceptionReport {
        if (objectToEncode instanceof GetObservationResponse) {
            GetObservationResponse response = (GetObservationResponse) objectToEncode;
            return encodeGetObsResponse(response.getObservationCollection());
        } else if (objectToEncode instanceof GetObservationByIdResponse) {
            GetObservationByIdResponse response = (GetObservationByIdResponse) objectToEncode;
            return encodeGetObsResponse(response.getObservationCollection());
        }
        throw new UnsupportedEncoderInputException(this, objectToEncode);        
    }

    private BinaryAttachmentResponse encodeGetObsResponse(List<OmObservation> sosObservationCollection)
            throws OwsExceptionReport{
        List<IoosSosObservation> ioosSosObsList = IoosUtil.createIoosSosObservations(sosObservationCollection);

        if (ioosSosObsList.isEmpty()) {
            throw new NoApplicableCodeException().withMessage("No feature types to encode.");
        }

        return encodeIoosObsToNetcdf(ioosSosObsList);
    }

    protected abstract BinaryAttachmentResponse encodeIoosObsToNetcdf(List<IoosSosObservation> ioosSosObsList)
            throws OwsExceptionReport;
    
    protected void encodeSensorDataToNetcdf(File netcdfFile, AbstractSensorDataset sensorDataset)
            throws OwsExceptionReport {
        SensorAsset sensor = sensorDataset.getSensor();
        StationAsset station = sensor.getStationAsset();

        System stationSystem = getSensorSystem(station.getAssetId());
//        System sensorSystem = getSensorSystem(sensor.getAssetId());        

        List<Time> times = Lists.newArrayList(sensorDataset.getTimes());
        Collections.sort(times);
        DateTime firstTime = getDateTime(times.get(0));
        DateTime lastTime = getDateTime(times.get(times.size() - 1));

        NetcdfFileWriter writer = null;
        try {
            writer = NetcdfFileWriter.createNew(Version.netcdf4, netcdfFile.getAbsolutePath(),
                    new IoosNc4ChunkingStrategy());
        } catch (IOException e) {
            throw new NoApplicableCodeException().causedBy(e)
                .withMessage("Error creating netCDF temp file.");
        }
        //set fill on, doesn't seem to have any effect though
        writer.setFill(true);
        
        String featureTypeName = sensorDataset.getFeatureType().name();

        int numTimes = sensorDataset.getTimes().size();        
        //FIXME shouldn't assume that all subsensors are heights (or rename subsensors if they are)
        int numHeights = sensorDataset.getSubSensors().size();

        //global attributes
        writer.addGroupAttribute(null, new Attribute(CFConstants.CONVENTIONS, CFConstants.CF_1_6));
        writer.addGroupAttribute(null, new Attribute(ACDDConstants.METADATA_CONVENTIONS,
                ACDDConstants.UNIDATA_DATASET_DISCOVERY_1_0));
        writer.addGroupAttribute(null, new Attribute(CFConstants.FEATURE_TYPE, featureTypeName));
        writer.addGroupAttribute(null, new Attribute(ACDDConstants.CDM_DATA_TYPE,
                CF.FeatureType.convert(sensorDataset.getFeatureType()).name()));
        writer.addGroupAttribute(null, new Attribute(NODCConstants.NODC_TEMPLATE_VERSION,
                getNodcTemplateVersion(sensorDataset.getFeatureType())));
        writer.addGroupAttribute(null, new Attribute(ACDDConstants.STANDARD_NAME_VOCABULARY,
                CFConstants.CF_1_6));
        writer.addGroupAttribute(null, new Attribute(NODCConstants.PLATFORM, NODCConstants.PLATFORM));
        writer.addGroupAttribute(null, new Attribute(NODCConstants.INSTRUMENT, NODCConstants.INSTRUMENT));
        writer.addGroupAttribute(null, new Attribute(ACDDConstants.TITLE,
                sensor.getAssetId()));
        writer.addGroupAttribute(null, new Attribute(ACDDConstants.SUMMARY,
                "Sensor observations for " + sensor.getAssetId()
                + ", feature type " + sensorDataset.getFeatureType().name()));
        //TODO adjust processing_level?
        writer.addGroupAttribute(null, new Attribute(ACDDConstants.PROCESSING_LEVEL,
                ACDDConstants.NONE));
        writer.addGroupAttribute(null, new Attribute(ACDDConstants.DATE_CREATED,
                new DateTime(DateTimeZone.UTC).toString()));
        writer.addGroupAttribute(null, new Attribute(ACDDConstants.LICENSE,
                ACDDConstants.LICENSE_FREELY_DISTRIBUTED));
        writer.addGroupAttribute(null, new Attribute(NODCConstants.UUID,
                UUID.randomUUID().toString()));
        writer.addGroupAttribute(null, new Attribute(ACDDConstants.ID,
                sensor.getAssetId()));
        writer.addGroupAttribute(null, new Attribute(ACDDConstants.NAMING_AUTHORITY,
                sensor.getAuthority()));

        //keywords
        LinkedHashSet<String> keywords = Sets.newLinkedHashSet();
        keywords.add(sensor.getAuthority());
        keywords.add(sensor.getStation());
        keywords.add(sensor.getSensor());
        for (OmObservableProperty obsProp : sensorDataset.getPhenomena()) {
            keywords.add(IoosSosUtil.getNameFromUri(obsProp.getIdentifier()));
        }
        writer.addGroupAttribute(null, new Attribute(ACDDConstants.KEYWORDS,
                Joiner.on(",").join(keywords)));

        //parentNetwork -> institution
        String parentNetwork = getClassifier(stationSystem, IoosDefConstants.PARENT_NETWORK_DEF);
        if (parentNetwork != null){
            writer.addGroupAttribute(null, new Attribute(ACDDConstants.INSTITUTION, parentNetwork));            
        }

        //sponsor -> acknowledgement
        String sponsor = getClassifier(stationSystem, IoosDefConstants.SPONSOR_DEF);
        if (sponsor != null){
            writer.addGroupAttribute(null, new Attribute(ACDDConstants.ACKNOWLEDGEMENT, sponsor));            
        }
        
        //operator -> contributor
        SmlResponsibleParty operator = getResponsibleParty(stationSystem, IoosDefConstants.OPERATOR_DEF);
        if (operator != null && !Strings.isNullOrEmpty(operator.getOrganizationName())){
            writer.addGroupAttribute(null, new Attribute(ACDDConstants.CONTRIBUTOR_ROLE,
                    IoosDefConstants.OPERATOR));
            writer.addGroupAttribute(null, new Attribute(ACDDConstants.CONTRIBUTOR_NAME, operator.getOrganizationName()));
        }

        //publisher
        SmlResponsibleParty publisher = getResponsibleParty(stationSystem, IoosDefConstants.PUBLISHER_DEF);
        if (publisher != null) {
            if (!Strings.isNullOrEmpty(publisher.getOrganizationName())){
                writer.addGroupAttribute(null, new Attribute(ACDDConstants.PUBLISHER_NAME, publisher.getOrganizationName()));
            }
            if (!Strings.isNullOrEmpty(publisher.getEmail())){
                writer.addGroupAttribute(null, new Attribute(ACDDConstants.PUBLISHER_EMAIL, publisher.getEmail()));
            }
            if (!CollectionHelper.isEmpty(publisher.getOnlineResources())){                
                writer.addGroupAttribute(null, new Attribute(ACDDConstants.PUBLISHER_URL, publisher.getOnlineResources().get(0)));
            }            
            
        }

        //geospatial extent
        //FIXME when trajectories are implemented, bbox should be calculated in AbstractSensorDataset during construction
        if (sensorDataset instanceof IStaticLocationDataset) {
            IStaticLocationDataset staticLocationDataset = (IStaticLocationDataset) sensorDataset;
            writer.addGroupAttribute(null, new Attribute(ACDDConstants.GEOSPATIAL_LAT_MIN,
                    staticLocationDataset.getLat()));
            writer.addGroupAttribute(null, new Attribute(ACDDConstants.GEOSPATIAL_LAT_MAX,
                    staticLocationDataset.getLat()));
            writer.addGroupAttribute(null, new Attribute(ACDDConstants.GEOSPATIAL_LAT_UNITS,
                    CFConstants.UNITS_DEGREES_NORTH));            
            writer.addGroupAttribute(null, new Attribute(ACDDConstants.GEOSPATIAL_LON_MIN,
                    staticLocationDataset.getLng()));
            writer.addGroupAttribute(null, new Attribute(ACDDConstants.GEOSPATIAL_LON_MAX,
                    staticLocationDataset.getLng()));
            writer.addGroupAttribute(null, new Attribute(ACDDConstants.GEOSPATIAL_LON_UNITS,
                    CFConstants.UNITS_DEGREES_EAST));            
        } else {
            throw new NoApplicableCodeException().withMessage("Trajectory encoding is not supported (bbox)");
        }

        //temporal extent
        writer.addGroupAttribute(null, new Attribute(ACDDConstants.TIME_COVERAGE_START, firstTime.toString()));            
        writer.addGroupAttribute(null, new Attribute(ACDDConstants.TIME_COVERAGE_END, lastTime.toString()));

        //add appropriate dims for feature type
        Dimension dFeatureTypeInstance = writer.addDimension(null, CFConstants.FEATURE_TYPE_INSTANCE,
                station.getAssetId().length());
        Dimension dTime = null;
        Dimension dZ = null;
        
        List<Dimension> noDims = Lists.newArrayList();
        List<Dimension> featureTypeInstanceDims = Lists.newArrayList(dFeatureTypeInstance);                
        List<Dimension> timeDims = Lists.newArrayList();
        List<Dimension> latLngDims = Lists.newArrayList();
        List<Dimension> zDims = Lists.newArrayList();
        List<Dimension> obsPropDims = Lists.newArrayList();

        //set up time dimension
//        if (sensorDataset instanceof IStaticTimeDataset) {
//            timeDims.add(dFeatureTypeInstance);
//        } else {
            if (!(sensorDataset instanceof IStaticLocationDataset)){
//                timeDims.add(dFeatureTypeInstance);
            }
//            dTime = writer.addDimension(null, CFStandardNames.TIME.getName(), numTimes);
            dTime = writer.addUnlimitedDimension(CFStandardNames.TIME.getName());
            dTime.setLength(numTimes);
            timeDims.add(dTime);
            if (!(sensorDataset instanceof IStaticLocationDataset)){            
                zDims.add(dTime);
            }
            obsPropDims.add(dTime);
//        }

        //set up lat/lng dimensions
        if (!(sensorDataset instanceof IStaticLocationDataset)){
            latLngDims.add(dTime);
        }

        //set up z dimensions
        if (sensorDataset instanceof IStaticAltitudeDataset){
//            zDims.add(dFeatureTypeInstance);
        } else {
            if (sensorDataset instanceof IStaticLocationDataset) {
                // profile/timeSeriesProfile
                dZ = writer.addDimension(null, CFConstants.Z, numHeights);
                dZ.setLength(numHeights);
                zDims.add(dZ);
                obsPropDims.add(dZ);
            } else {
                //trajectory
//                zDims.add(dFeatureTypeInstance);
                zDims.add(dTime);
            }
        }

        //feature type instance var
        Variable vFeatureTypeInstance = writer.addVariable(null, CFConstants.FEATURE_TYPE_INSTANCE,
                DataType.CHAR, featureTypeInstanceDims);
        vFeatureTypeInstance.addAttribute(new Attribute(CFConstants.LONG_NAME, "Identifier for each feature type instance"));
        String cfRole = getCfRole(sensorDataset.getFeatureType());
        if (cfRole != null) {
            vFeatureTypeInstance.addAttribute(new Attribute(CFConstants.CF_ROLE, getCfRole(sensorDataset.getFeatureType())));
        }
        ArrayChar.D1 featureTypeInstanceArray = new ArrayChar.D1(station.getAssetId().length());
        featureTypeInstanceArray.setString(station.getAssetId());

        //crs var
        Variable vCrs = writer.addVariable(null, NODCConstants.CRS, DataType.INT, noDims);
        vCrs.addAttribute(new Attribute(CFConstants.LONG_NAME, IoosSosConstants.EPSG_4326_DEF));
        vCrs.addAttribute(new Attribute(CFConstants.GRID_MAPPING_NAME, CFConstants.GRID_MAPPING_NAME_WGS84));
        vCrs.addAttribute(new Attribute(CFConstants.EPSG_CODE, CFConstants.EPSG_CODE_WGS84));
        vCrs.addAttribute(new Attribute(CFConstants.SEMI_MAJOR_AXIS, CFConstants.SEMI_MAJOR_AXIS_WGS84));
        vCrs.addAttribute(new Attribute(CFConstants.INVERSE_FLATTENING, CFConstants.INVERSE_FLATTENING_WGS84));

        //time var
        Variable vTime = writer.addVariable(null, CFStandardNames.TIME.getName(), DataType.DOUBLE, timeDims); 
        vTime.addAttribute(new Attribute(CFConstants.STANDARD_NAME, CFStandardNames.TIME.getName()));
        vTime.addAttribute(new Attribute(CFConstants.UNITS, CFConstants.UNITS_TIME));
        vTime.addAttribute(new Attribute(CFConstants.AXIS, CFConstants.AXIS_T));
        vTime.addAttribute(new Attribute(CFConstants.FILL_VALUE, DOUBLE_FILL_VALUE));
        if (numTimes > 1) {
            vTime.addAttribute(new Attribute(CDM.CHUNK_SIZE, CHUNK_SIZE_TIME));            
        }
        ArrayDouble timeArray = new ArrayDouble(getDimShapes(timeDims));
        initArrayWithFillValue(timeArray, FLOAT_FILL_VALUE);

        //lat var
        Variable vLat = writer.addVariable(null, CFStandardNames.LATITUDE.getName(), DataType.FLOAT, latLngDims);
        vLat.addAttribute(new Attribute(CFConstants.STANDARD_NAME, CFStandardNames.LATITUDE.getName()));
        vLat.addAttribute(new Attribute(CFConstants.LONG_NAME, IoosCfConstants.LATITUDE_DEF));
        vLat.addAttribute(new Attribute(CFConstants.UNITS, CFConstants.UNITS_DEGREES_NORTH));
        vLat.addAttribute(new Attribute(CFConstants.AXIS, CFConstants.AXIS_Y));
        vLat.addAttribute(new Attribute(CFConstants.FILL_VALUE, FLOAT_FILL_VALUE));

        //lon var
        Variable vLon = writer.addVariable(null, CFStandardNames.LONGITUDE.getName(), DataType.FLOAT, latLngDims);
        vLon.addAttribute(new Attribute(CFConstants.STANDARD_NAME, CFStandardNames.LONGITUDE.getName()));
        vLon.addAttribute(new Attribute(CFConstants.LONG_NAME, IoosCfConstants.LONGITUDE_DEF));
        vLon.addAttribute(new Attribute(CFConstants.UNITS, CFConstants.UNITS_DEGREES_EAST));
        vLon.addAttribute(new Attribute(CFConstants.AXIS, CFConstants.AXIS_X));
        vLon.addAttribute(new Attribute(CFConstants.FILL_VALUE, FLOAT_FILL_VALUE));
        ArrayFloat latArray = null;
        ArrayFloat lonArray = null;
        if (sensorDataset instanceof IStaticLocationDataset) {
            IStaticLocationDataset staticLocationDataset = (IStaticLocationDataset) sensorDataset;
            if (staticLocationDataset.getLat() != null && staticLocationDataset.getLng() != null) {
                latArray = new ArrayFloat.D1(1);
                lonArray = new ArrayFloat.D1(1);
                Index latIndex = latArray.getIndex();
                Index lonIndex = lonArray.getIndex();
                latIndex.set(0);            
                lonIndex.set(0);
                latArray.set(latIndex, staticLocationDataset.getLat().floatValue());
                lonArray.set(lonIndex, staticLocationDataset.getLng().floatValue());
            }
        } else {
            //TODO support varying lat/lons
            throw new NoApplicableCodeException().withMessage("Varying lat/lngs are not yet supported.");
        }        
        
        //height var
        Variable vHeight = writer.addVariable(null, CFStandardNames.HEIGHT.getName(), DataType.FLOAT, zDims);
        vHeight.addAttribute(new Attribute(CFConstants.STANDARD_NAME, CFStandardNames.HEIGHT.getName()));
        vHeight.addAttribute(new Attribute(CFConstants.LONG_NAME, IoosCfConstants.HEIGHT_DEF));
        vHeight.addAttribute(new Attribute(CFConstants.UNITS, CFConstants.UNITS_METERS));
        vHeight.addAttribute(new Attribute(CFConstants.AXIS, CFConstants.AXIS_Z));
        vHeight.addAttribute(new Attribute(CFConstants.POSITIVE, CFConstants.POSITIVE_UP));
        vHeight.addAttribute(new Attribute(CFConstants.FILL_VALUE, FLOAT_FILL_VALUE));
        ArrayFloat heightArray = new ArrayFloat(getDimShapes(zDims));
        initArrayWithFillValue(heightArray, FLOAT_FILL_VALUE);
        if (sensorDataset instanceof IStaticAltitudeDataset) {
            IStaticAltitudeDataset staticAltitudeDataset = (IStaticAltitudeDataset) sensorDataset;
            Index heightIndex = heightArray.getIndex();
//            heightIndex.set(0);            
            if (staticAltitudeDataset.getAlt() != null) {                
                heightArray.set(heightIndex, staticAltitudeDataset.getAlt().floatValue());
            } else {
                heightArray.set(heightIndex, FLOAT_FILL_VALUE);
            }
        }

        //platform container var
        Variable vPlatform = writer.addVariable(null, NODCConstants.PLATFORM, DataType.INT, noDims);
        //stationId
        vPlatform.addAttribute(new Attribute(IoosNetcdfConstants.IOOS_CODE,
                sensorDataset.getSensor().getStationAsset().getAssetId()));        
        //platform description
        if (!stationSystem.getDescriptions().isEmpty()){
            vPlatform.addAttribute(new Attribute(CFConstants.COMMENT, stationSystem.getDescriptions().get(0)));
        }
        //wmo code
        addAttributeIfIdentifierExists(vPlatform, stationSystem, IoosDefConstants.WMO_ID_DEF, CFConstants.WMO_CODE);
        //short_name
        addAttributeIfIdentifierExists(vPlatform, stationSystem, IoosDefConstants.SHORT_NAME_DEF, IoosNetcdfConstants.SHORT_NAME);
        //long_name
        addAttributeIfIdentifierExists(vPlatform, stationSystem, IoosDefConstants.LONG_NAME_DEF, CFConstants.LONG_NAME);
        //source
        addAttributeIfClassifierExists(vPlatform, stationSystem, IoosDefConstants.PLATFORM_TYPE_DEF, CFConstants.SOURCE);

        
        //instrument container var
        Variable vInstrument = writer.addVariable(null, NODCConstants.INSTRUMENT, DataType.INT, noDims);
        vInstrument.addAttribute(new Attribute(DEFINITION, IoosDefConstants.SENSOR_ID_DEF));
        vInstrument.addAttribute(new Attribute(CFConstants.LONG_NAME, sensorDataset.getSensor().getAssetId()));        
        
        String coordinateString = Joiner.on(' ').join(Lists.newArrayList(
                vTime.getFullName(), vLat.getFullName(), vLon.getFullName(), vHeight.getFullName()));
        
        Map<OmObservableProperty,Variable> obsPropVarMap = Maps.newHashMap();
        Map<Variable,ArrayFloat> varDataArrayMap = Maps.newHashMap();
        for (OmObservableProperty obsProp : sensorDataset.getPhenomena()) {
            //obs prop var
            String standardName = IoosSosUtil.getNameFromUri(obsProp.getIdentifier());
            Variable vObsProp = writer.addVariable(null, standardName, DataType.FLOAT, obsPropDims);
            vObsProp.addAttribute(new Attribute(CFConstants.STANDARD_NAME, standardName));
            vObsProp.addAttribute(new Attribute(CFConstants.LONG_NAME, obsProp.getIdentifier()));
            vObsProp.addAttribute(new Attribute(CFConstants.COORDINATES, coordinateString));
            vObsProp.addAttribute(new Attribute(CFConstants.FILL_VALUE, FLOAT_FILL_VALUE));
            vObsProp.addAttribute(new Attribute(CFConstants.UNITS,
                    IoosSosUtil.getNameFromUri(obsProp.getUnit())));
            obsPropVarMap.put(obsProp, vObsProp);

            //init obs prop data array
            ArrayFloat obsPropArray = new ArrayFloat(getDimShapes(obsPropDims));
            initArrayWithFillValue(obsPropArray, FLOAT_FILL_VALUE);
            varDataArrayMap.put(vObsProp, obsPropArray);
        }

        //populate heights array for profile 
        if (zDims.size() == 1 && hasDimension(zDims, dZ) && !sensorDataset.getSubSensors().isEmpty()) {
            Index heightIndex = heightArray.getIndex();
            int heightIndexCounter = 0;
            List<SubSensor> subSensors = sensorDataset.getSubSensors();
            Float consistentBinHeight = null; 
            for (SubSensor subSensor : sensorDataset.getSubSensors()) {
                if (subSensor instanceof ProfileSubSensor) {
                    heightIndex.setDim(0, heightIndexCounter++);
                    heightArray.set(heightIndex, Double.valueOf(((ProfileSubSensor) subSensor).getHeight()).floatValue());

                    //check for consistent bin size
                    if (subSensor instanceof BinProfileSubSensor) {
                        float binHeight = Double.valueOf((((BinProfileSubSensor) subSensor).getBinHeight())).floatValue();
                        if (consistentBinHeight == null) {
                            consistentBinHeight = binHeight;
                        } else if (consistentBinHeight > FLOAT_FILL_VALUE && consistentBinHeight != binHeight) {
                            //mark bin height as inconsistent
                            consistentBinHeight = FLOAT_FILL_VALUE;
                        }
                    }
                } else {
                    throw new NoApplicableCodeException().withMessage("Non-profile subsensors not supported.");
                }
            }

            //global attributes: geospatial_vertical_min/max/units/resolution/positive
            writer.addGroupAttribute(null, new Attribute(ACDDConstants.GEOSPATIAL_VERTICAL_UNITS,
                    CFConstants.UNITS_METERS));
            writer.addGroupAttribute(null, new Attribute(ACDDConstants.GEOSPATIAL_VERTICAL_POSITIVE,
                    CFConstants.POSITIVE_UP));
            
            SubSensor firstSubSensor = subSensors.get(0);
            SubSensor lastSubSensor = subSensors.get(subSensors.size() - 1);
            if (firstSubSensor instanceof ProfileSubSensor) {
                writer.addGroupAttribute(null, new Attribute(ACDDConstants.GEOSPATIAL_VERTICAL_MAX,
                        ((ProfileSubSensor) firstSubSensor).getHeight()));
            }
            if (lastSubSensor instanceof ProfileSubSensor) {
                writer.addGroupAttribute(null, new Attribute(ACDDConstants.GEOSPATIAL_VERTICAL_MIN,
                        ((ProfileSubSensor) lastSubSensor).getHeight()));
            }
            
            String verticalResolution = null;
            if (consistentBinHeight == null) {
                verticalResolution = ACDDConstants.POINT;
            } else if (consistentBinHeight != FLOAT_FILL_VALUE){
                verticalResolution = consistentBinHeight + " " + CFConstants.UNITS_METERS + " "
                        + ACDDConstants.BINNED;
            }
            if (verticalResolution != null) {
                writer.addGroupAttribute(null, new Attribute(ACDDConstants.GEOSPATIAL_VERTICAL_RESOLUTION,
                        verticalResolution));                                
            }
            
        }

        //iterate through sensorDataset, set values
        int timeCounter = 0;
        for (Time time : sensorDataset.getTimes()) {
            //set time value            
            Index timeIndex = timeArray.getIndex();
            int timeIndexCounter = 0;
//            if (hasDimension(timeDims, dFeatureTypeInstance)) {
//                timeIndex.setDim(timeIndexCounter++, 0);    
//            }
            if (hasDimension(timeDims, dTime)) {
                timeIndex.setDim(timeIndexCounter++, timeCounter++);
            }
            timeArray.set(timeIndex, getSecondsSinceEpoch(time));

            //data values
            Map<OmObservableProperty, Map<SubSensor, Value<?>>> obsPropMap = sensorDataset.getDataValues().get(time);
            for (OmObservableProperty obsProp : obsPropMap.keySet()) {
                Variable variable = obsPropVarMap.get(obsProp);
                ArrayFloat array = varDataArrayMap.get(variable);
                for (Entry<SubSensor, Value<?>> subSensorEntry : obsPropMap.get(obsProp).entrySet()) {
                    SubSensor subSensor = subSensorEntry.getKey();
                    Value<?> value = subSensorEntry.getValue();
                    Object valObj = value.getValue();
                    if (!(valObj instanceof BigDecimal)){
                        throw new NoApplicableCodeException()
                            .withMessage("Value class " + valObj.getClass().getCanonicalName() + " not supported");                    
                    }
                    
                    Index index = array.getIndex();
                    int obsPropDimCounter = 0;
                    for (Dimension dim : obsPropDims){
//                        if (dim.equals(dFeatureTypeInstance)){
                            //feature type instance index                            
//                            index.setDim(obsPropDimCounter++, 0);
//                        } else if (dim.equals(dTime)){
                        if (dim.equals(dTime)){
                            //time index dim                            
                            index.setDim(obsPropDimCounter++, timeCounter - 1);                            
                        } else if (dim.equals(dZ)){
                            //height index dim
                            index.setDim(obsPropDimCounter++, sensorDataset.getSubSensors().indexOf(subSensor));                            
                        }
                    }
                    array.set(index, ((BigDecimal) valObj).floatValue());                    
                }
            }
        }

        //create the empty netCDF with dims/vars/attributes defined
        try {
            writer.create();
        } catch (IOException e) {
            throw new NoApplicableCodeException().causedBy(e).withMessage("Couldn't create empty netCDF file");
        }

        //fill the netCDF file with data
        try {
            writer.write(vFeatureTypeInstance, featureTypeInstanceArray);
            writer.write(vTime, timeArray);
            writer.write(vLat, latArray);
            writer.write(vLon, lonArray);
            writer.write(vHeight, heightArray);
            for (Entry<Variable,ArrayFloat> varEntry : varDataArrayMap.entrySet()) {
                Variable vObsProp = varEntry.getKey();
                ArrayFloat varData = varEntry.getValue();
                writer.write(vObsProp, varData);
            }            
        } catch (Exception e) {
            throw new NoApplicableCodeException().causedBy(e).withMessage("Error writing netCDF variable data");
        }

        try {
            writer.close();
        } catch (IOException e) {
            throw new NoApplicableCodeException().causedBy(e)
            .withMessage("Error closign netCDF data for sensor " + sensorDataset.getSensor().getAssetId());
        }
    }

    private String getNodcTemplateVersion(CF.FeatureType featureType) throws CodedException{
        if (featureType.equals(CF.FeatureType.timeSeries)) {
            return NODCConstants.NODC_TIMESERIES_ORTHOGONAL_TEMPLATE_1_0;
        } else if (featureType.equals(CF.FeatureType.timeSeriesProfile)) {
            return NODCConstants.NODC_TIMESERIESPROFILE_ORTHOGONAL_TEMPLATE_1_0;
        }
        throw new NoApplicableCodeException().withMessage("Feature type " + featureType.name()
                + " is not supported for netCDF output");
    }

    private String getCfRole(CF.FeatureType featureType) throws CodedException {
        if (featureType.equals(CF.FeatureType.timeSeries)) {
            return CF.TIMESERIES_ID;
        } else if (featureType.equals(CF.FeatureType.timeSeriesProfile)) {
            return CF.TIMESERIES_ID;
        } else if (featureType.equals(CF.FeatureType.trajectory) || featureType.equals(CF.FeatureType.trajectoryProfile)) {
            return CF.TRAJECTORY_ID;
        } else {
            throw new NoApplicableCodeException().withMessage("Feature type " + featureType.name()
                    + " is not supported for netCDF output");
        }
    }

    private static DateTime getDateTime(Time time) throws CodedException{
        if (!(time instanceof TimeInstant)){
            throw new NoApplicableCodeException()
                .withMessage("Time class " + time.getClass().getCanonicalName() + " not supported");
        }
        TimeInstant timeInstant = (TimeInstant) time;
        return timeInstant.getValue();        
    }
    
    private static double getSecondsSinceEpoch(Time time) throws CodedException{
        return getDateTime(time).toDate().getTime() / 1000;        
    }

    private static int[] getDimShapes(List<Dimension> dims) {
        int[] dimShapes = new int[dims.size()];
        int dimCounter = 0;
        for (Dimension dim : dims ){
            dimShapes[dimCounter++] = dim.getLength();            
        }
        return dimShapes;
    }

    private static boolean hasDimension(List<Dimension> dims, Dimension dim) {
        return dim != null && dims.contains(dim);
    }

    private static void initArrayWithFillValue(Array array, Object fillValue) {
        IndexIterator indexIterator = array.getIndexIterator();
        while(indexIterator.hasNext()){
            indexIterator.setObjectNext(fillValue);;
        }
    }

    private static System getSensorSystem(String procedure) throws OwsExceptionReport{
        DescribeSensorRequest req = new DescribeSensorRequest();
        req.setService(SosConstants.SOS);
        req.setVersion(Sos1Constants.SERVICEVERSION);
        req.setProcedure(procedure);
        req.setProcedureDescriptionFormat(IoosSosConstants.SML_PROFILE_M10);
        DescribeSensorResponse resp;
        try {
            resp = getDescribeSensorDAO().getSensorDescription(req);
        } catch (OwsExceptionReport e) {
            throw new NoApplicableCodeException().withMessage("Error getting sensor description for " + procedure)
                .causedBy(e);
        }
        SosProcedureDescription sosProcedureDescription = resp.getProcedureDescriptions().get(0);
        System system = null;
        if (sosProcedureDescription instanceof System){
            system = (System) sosProcedureDescription;
        } else if (sosProcedureDescription instanceof SensorML) {
            SensorML sml = (SensorML) sosProcedureDescription;
            if (sml.isWrapper()) {
                AbstractProcess ap = sml.getMembers().get(0);
                if (ap instanceof System) {
                    system = (System) ap;
                }
            }
        }
        if (system == null){
            throw new NoApplicableCodeException().withMessage("Only system procedure descriptions are supported, found "
                    + sosProcedureDescription.getClass().getName() + " for " + procedure);
        }
        return system;
    }

    private static AbstractDescribeSensorDAO getDescribeSensorDAO() throws CodedException {
        OperationDAO operationDAO = OperationDAORepository.getInstance().getOperationDAO(SosConstants.SOS,
                SosConstants.Operations.DescribeSensor.toString());
        if (operationDAO != null && operationDAO instanceof AbstractDescribeSensorDAO) {
            return (AbstractDescribeSensorDAO) operationDAO;
        }
        throw new NoApplicableCodeException().withMessage("Could not get DescribeSensor DAO");
    }
    
    private static String getIdentifier(System system, String identifierDefinition) {
        for (SmlIdentifier smlIdentifier : system.getIdentifications()) {
            if (smlIdentifier.getDefinition().equals(identifierDefinition)){
                return smlIdentifier.getValue();
            }
        }
        return null;
    }

    private static void addAttributeIfIdentifierExists(Variable variable, System system,
            String identifierDefinition, String attributeName) {
        String value = getIdentifier(system, identifierDefinition);
        if (value != null) {
            variable.addAttribute(new Attribute(attributeName, value));
        }        
    }

    private static String getClassifier(System system, String classifierDefinition) {
        for (SmlClassifier smlClassifier : system.getClassifications()) {
            if (smlClassifier.getDefinition().equals(classifierDefinition)){
                return smlClassifier.getValue();
            }
        }
        return null;
    }

    private static void addAttributeIfClassifierExists(Variable variable, System system,
            String classifierDefinition, String attributeName) {
        String value = getClassifier(system, classifierDefinition);
        if (value != null) {
            variable.addAttribute(new Attribute(attributeName, value));
        }        
    }

    private static SmlResponsibleParty getResponsibleParty(System system, String contactRole) {
        if (!CollectionHelper.isEmpty(system.getContact())) {
            return getResponsibleParty(system.getContact(), contactRole);    
        }
        return null;
    }

    private static SmlResponsibleParty getResponsibleParty(List<SmlContact> contacts, String contactRole) {
        for (SmlContact contact : contacts) {
            if (contact instanceof SmlContactList) {
                SmlResponsibleParty respParty = getResponsibleParty(((SmlContactList) contact).getMembers(),
                        contactRole);
                if (respParty != null) {
                    return respParty;
                }
            } else if (contact.getRole() != null && contact.getRole().equals(contactRole) &&
                    contact instanceof SmlResponsibleParty) {
                return (SmlResponsibleParty) contact;
            }
        }
        return null;
        
    }

    protected static String makeDateSafe(DateTime dt) {
        return dt.toString().replace(":", "");
    }

    @Override
    public boolean supportsResultStreamingForMergedValues() {
        // TODO Auto-generated method stub
        return false;
    }

    protected String getFilename(AbstractSensorDataset sensorDataset) throws OwsExceptionReport {
        List<Time> times = Lists.newArrayList(sensorDataset.getTimes());
        Collections.sort(times);
        DateTime firstTime = getDateTime(times.get(0));
        DateTime lastTime = getDateTime(times.get(times.size() - 1));

        StringBuffer pathBuffer = new StringBuffer(sensorDataset.getSensor().getAssetShortId());
        pathBuffer.append("_" + sensorDataset.getFeatureType().name().toLowerCase());
        pathBuffer.append("_" + makeDateSafe(firstTime));
//        if (!(sensorDataset instanceof IStaticTimeDataset)) {
            pathBuffer.append("_" + makeDateSafe(lastTime));
//        }
        pathBuffer.append("_" + Long.toString(java.lang.System.nanoTime()) + ".nc");
        return pathBuffer.toString();
    }
}
