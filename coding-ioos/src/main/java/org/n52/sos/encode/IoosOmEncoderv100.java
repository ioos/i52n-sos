package org.n52.sos.encode;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.opengis.gml.AssociationType;
import net.opengis.gml.CodeType;
import net.opengis.gml.DescriptionDocument;
import net.opengis.gml.FeatureCollectionType;
import net.opengis.gml.FeaturePropertyType;
import net.opengis.gml.GenericMetaDataDocument;
import net.opengis.gml.GenericMetaDataType;
import net.opengis.gml.MemberDocument;
import net.opengis.gml.MetaDataPropertyType;
import net.opengis.gml.MultiPointType;
import net.opengis.gml.NameDocument;
import net.opengis.gml.PointArrayPropertyType;
import net.opengis.gml.PointType;
import net.opengis.gml.TimePeriodType;
import net.opengis.om.x10.ObservationCollectionDocument;
import net.opengis.om.x10.ObservationCollectionType;
import net.opengis.om.x10.ObservationDocument;
import net.opengis.om.x10.ObservationPropertyType;
import net.opengis.om.x10.ObservationType;
import net.opengis.om.x10.ProcessPropertyType;
import net.opengis.swe.x101.CompositePhenomenonType;
import net.opengis.swe.x101.PhenomenonPropertyType;
import net.opengis.swe.x101.TimeObjectPropertyType;

import org.apache.xmlbeans.XmlObject;
import org.joda.time.DateTime;
import org.n52.sos.config.annotation.Configurable;
import org.n52.sos.config.annotation.Setting;
import org.n52.sos.exception.ows.concrete.UnsupportedEncoderInputException;
import org.n52.sos.ioos.Ioos52nConstants;
import org.n52.sos.ioos.IoosSettings;
import org.n52.sos.ioos.IoosUtil;
import org.n52.sos.ioos.asset.StationAsset;
import org.n52.sos.ioos.feature.FeatureUtil;
import org.n52.sos.ioos.om.IoosSosObservation;
import org.n52.sos.ogc.gml.GmlConstants;
import org.n52.sos.ogc.om.OmConstants;
import org.n52.sos.ogc.om.OmObservableProperty;
import org.n52.sos.ogc.om.OmObservation;
import org.n52.sos.ogc.om.features.SfConstants;
import org.n52.sos.ogc.ows.OwsExceptionReport;
import org.n52.sos.ogc.sos.Sos1Constants;
import org.n52.sos.ogc.sos.SosConstants;
import org.n52.sos.ogc.sos.SosConstants.HelperValues;
import org.n52.sos.ogc.swe.SweConstants;
import org.n52.sos.response.GetObservationByIdResponse;
import org.n52.sos.response.GetObservationResponse;
import org.n52.sos.service.ServiceConstants.SupportedTypeKey;
import org.n52.sos.util.CodingHelper;
import org.n52.sos.util.GeometryHandler;
import org.n52.sos.util.JTSHelper;
import org.n52.sos.util.XmlHelper;
import org.n52.sos.util.XmlOptionsHelper;
import org.n52.sos.util.http.MediaType;
import org.n52.sos.w3c.SchemaLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axiomalaska.ioos.sos.IoosSosConstants;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.vividsolutions.jts.geom.Point;

@Configurable
public class IoosOmEncoderv100 implements ObservationEncoder<XmlObject, Object>{
    /** logger */
    private static final Logger LOGGER = LoggerFactory.getLogger(IoosOmEncoderv100.class);
    
    private static MediaType CONTENT_TYPE_IOOS_OM_M10 =
            new MediaType("text", "xml", "subtype", IoosSosConstants.OM_SUBTYPE_M10);    

    private static final Map<SupportedTypeKey, Set<String>> SUPPORTED_TYPES = Collections.singletonMap(
            SupportedTypeKey.ObservationType, Collections.singleton(OmConstants.OBS_TYPE_MEASUREMENT));

    private static final Set<String> CONFORMANCE_CLASSES = ImmutableSet.of(
            "http://www.opengis.net/spec/OMXML/1.0/conf/measurement",
            "http://www.opengis.net/spec/OMXML/1.0/conf/categoryObservation",
            "http://www.opengis.net/spec/OMXML/1.0/conf/countObservation",
            "http://www.opengis.net/spec/OMXML/1.0/conf/truthObservation",
            "http://www.opengis.net/spec/OMXML/1.0/conf/geometryObservation",
            "http://www.opengis.net/spec/OMXML/1.0/conf/textObservation");

    private static final Map<String, Map<String, Set<String>>> SUPPORTED_RESPONSE_FORMATS = Collections.singletonMap(
            SosConstants.SOS, Collections.singletonMap(Sos1Constants.SERVICEVERSION,
                    Collections.singleton(CONTENT_TYPE_IOOS_OM_M10.toString())));

    private static final Set<EncoderKey> ENCODER_KEYS = CodingHelper.encoderKeysForElements( CONTENT_TYPE_IOOS_OM_M10.toString(),
            IoosSosObservation.class, GetObservationResponse.class,
            GetObservationByIdResponse.class, OmObservation.class);

    /**
     * Disclaimer from IoosSettings
     */
    private String disclaimer;
    
    public IoosOmEncoderv100() {
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
        nameSpacePrefixMap.put(OmConstants.NS_OM, OmConstants.NS_OM_PREFIX);
        nameSpacePrefixMap.put(SweConstants.NS_SWE_20, Ioos52nConstants.SWE2_PREFIX);        
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
    public MediaType getContentType() {
        return CONTENT_TYPE_IOOS_OM_M10;
    }
    
    @Override
    public Set<SchemaLocation> getSchemaLocations() {
        return ImmutableSet.of(
                Sos1Constants.SOS1_SCHEMA_LOCATION,
                SfConstants.SA_SCHEMA_LOCATION,
                OmConstants.OM_100_SCHEMA_LOCATION,
                SweConstants.SWE_101_SCHEMA_LOCATION,
                SweConstants.SWE_20_SCHEMA_LOCATION);
    }

    @Override
    public XmlObject encode(Object element) throws OwsExceptionReport {
        return encode(element, new EnumMap<HelperValues, String>(HelperValues.class));
    }

    @Override
    public XmlObject encode(Object element, Map<HelperValues, String> additionalValues) throws OwsExceptionReport {
        if (element instanceof IoosSosObservation) {
            IoosSosObservation ioosSosObs = (IoosSosObservation) element;
               return encodeIoosObservation(ioosSosObs);
        } else if (element instanceof GetObservationResponse) {
            GetObservationResponse response = (GetObservationResponse) element;
            return createObservationCollection(response.getObservationCollection(), response.getResultModel());
        } else if (element instanceof GetObservationByIdResponse) {
            GetObservationByIdResponse response = (GetObservationByIdResponse) element;
            return createObservationCollection(response.getObservationCollection(), response.getResultModel());
        }
        throw new UnsupportedEncoderInputException(this, element);
    }

    @Setting(IoosSettings.DISCLAIMER)
    public void setDisclaimer(final String disclaimer) {
        this.disclaimer = disclaimer;
    }

    protected XmlObject createObservationCollection(List<OmObservation> sosObservationCollection, String resultModel)
            throws OwsExceptionReport {
        // create ObservationCollectionDocument and add Collection
        ObservationCollectionDocument xb_obsColDoc = ObservationCollectionDocument.Factory.newInstance(
                XmlOptionsHelper.getInstance().getXmlOptions());
        ObservationCollectionType xb_obsCol = xb_obsColDoc.addNewObservationCollection();
        xb_obsCol.setId(SosConstants.OBS_COL_ID_PREFIX + new DateTime().getMillis());

        //top level disclaimer
        if (disclaimer != null && !disclaimer.trim().isEmpty()) {
            MetaDataPropertyType xb_disclaimerMetadataProperty = xb_obsCol.addNewMetaDataProperty();
            xb_disclaimerMetadataProperty.setTitle("disclaimer");
            GenericMetaDataDocument xb_disclaimerMetadataDoc = GenericMetaDataDocument.Factory.newInstance();
            GenericMetaDataType xb_disclaimerMetadata = xb_disclaimerMetadataDoc.addNewGenericMetaData();
            DescriptionDocument xb_disclaimerDescriptionDoc = DescriptionDocument.Factory.newInstance();
            xb_disclaimerDescriptionDoc.addNewDescription().setStringValue(disclaimer.trim());
            XmlHelper.append( xb_disclaimerMetadata, xb_disclaimerDescriptionDoc );
            XmlHelper.append( xb_disclaimerMetadataProperty, xb_disclaimerMetadataDoc );
        }

        //ioos template version        
        MetaDataPropertyType xb_templateVersionMetadataProperty = xb_obsCol.addNewMetaDataProperty();
        xb_templateVersionMetadataProperty.set( IoosEncoderUtil.getIoosVersionMetaData() );

        //software version        
        MetaDataPropertyType xb_softwareVersionMetadataProperty = xb_obsCol.addNewMetaDataProperty();
        xb_softwareVersionMetadataProperty.set( IoosEncoderUtil.getSoftwareVersionMetaData() );
        
        if (sosObservationCollection != null) {
            if ( sosObservationCollection.size() > 0) {
                xb_obsCol.addNewBoundedBy();
                xb_obsCol.setBoundedBy( IoosEncoderUtil.createBoundedBy(IoosUtil.swapEnvelopeAxisOrder(
                        IoosUtil.createEnvelope(sosObservationCollection))));
                
                //add the observation members (one per feature type)
                List<IoosSosObservation> ioosSosObsList = IoosUtil.createIoosSosObservations(sosObservationCollection);

                for( IoosSosObservation ioosSosObs : ioosSosObsList ){
                    xb_obsCol.addNewMember().set(encodeIoosObservation(ioosSosObs));
                }
            } else {
                ObservationPropertyType xb_obs = xb_obsCol.addNewMember();
                xb_obs.setHref( GmlConstants.NIL_INAPPLICABLE );
            }
        } else {
            ObservationPropertyType xb_obs = xb_obsCol.addNewMember();
            xb_obs.setHref( GmlConstants.NIL_INAPPLICABLE );
        }

        // set schema location
        XmlHelper.makeGmlIdsUnique(xb_obsColDoc.getDomNode());
        return xb_obsColDoc;
    }

    protected XmlObject encodeIoosObservation( IoosSosObservation ioosSosObs ) throws OwsExceptionReport {
        ObservationDocument xb_obsDoc = ObservationDocument.Factory.newInstance();
        ObservationType xb_obsType = xb_obsDoc.addNewObservation();

        //== metadata block ==

        //description
        xb_obsType.addNewDescription().setStringValue( ioosSosObs.getFeatureType().name() );

        //sampling time
        TimeObjectPropertyType xb_samplingTime = xb_obsType.addNewSamplingTime();
        TimePeriodType xb_timePeriod = (TimePeriodType) xb_samplingTime.addNewTimeObject()
                .substitute( GmlConstants.QN_TIME_PERIOD, TimePeriodType.type );
        xb_timePeriod.set( CodingHelper.encodeObjectToXml( GmlConstants.NS_GML, ioosSosObs.getSamplingTime() ) );

        //procedures (stations)
        ProcessPropertyType xb_procedure = xb_obsType.addNewProcedure();
        XmlObject xb_process = xb_procedure.addNewProcess2();
        xb_process.substitute( Ioos52nConstants.QN_PROCESS, XmlObject.type );

        for( StationAsset station : ioosSosObs.getStations() ){
            MemberDocument xb_memberDoc = MemberDocument.Factory.newInstance();
            AssociationType xb_member = (AssociationType) xb_memberDoc.addNewMember()
                    .substitute( Ioos52nConstants.QN_MEMBER, AssociationType.type );
            xb_member.setHref( station.getAssetId() );
            XmlHelper.append( xb_process, xb_member );
        }

        //observedProperty
        PhenomenonPropertyType xb_observedProperty = xb_obsType.addNewObservedProperty();
        CompositePhenomenonType xb_compPhen = (CompositePhenomenonType) xb_observedProperty.addNewPhenomenon()
                .substitute( Ioos52nConstants.QN_COMPOSITE_PHENOMENON, CompositePhenomenonType.type );
        xb_compPhen.setDimension( BigInteger.valueOf( ioosSosObs.getPhenomena().size() ) );
        xb_compPhen.setId( IoosSosConstants.OBSERVED_PROPERTIES_ID_PREFIX );
        xb_compPhen.addNewName().setStringValue("Response Observed Properties");
        
        //get a map and list to alphabetize OmObservableProperty (or implement Comparable there)
        Map<String,OmObservableProperty> phenMap = new HashMap<String,OmObservableProperty>();
        List<String> phenIdList = new ArrayList<String>();
        for( OmObservableProperty sosObsProp : ioosSosObs.getPhenomena() ){
            phenMap.put( sosObsProp.getIdentifier(), sosObsProp );
            phenIdList.add( sosObsProp.getIdentifier() );
        }
        Collections.sort( phenIdList );
        
        for( String phenId : phenIdList ){
            OmObservableProperty sosObsProp = phenMap.get( phenId );
            xb_compPhen.addNewComponent().setHref( sosObsProp.getIdentifier() );
        }

        // featureOfInterest
        FeaturePropertyType xb_FeatureProp = xb_obsType.addNewFeatureOfInterest();
        FeatureCollectionType xb_featureCollection = (FeatureCollectionType) xb_FeatureProp.addNewFeature()
                .substitute( GmlConstants.QN_FEATURE_COLLECTION, FeatureCollectionType.type );

        //feature type metadata
        MetaDataPropertyType xb_featureTypeMetaDataProp = xb_featureCollection.addNewMetaDataProperty();
        xb_featureTypeMetaDataProp.setTitle( IoosSosConstants.FEATURE_TYPE );        
        GenericMetaDataDocument xb_featureTypeMetadataDoc = GenericMetaDataDocument.Factory.newInstance();
        GenericMetaDataType xb_featureTypeMetadata = xb_featureTypeMetadataDoc.addNewGenericMetaData();
        NameDocument xb_featureTypeNameDoc = NameDocument.Factory.newInstance();
        CodeType xb_featureTypeName = xb_featureTypeNameDoc.addNewName();
        xb_featureTypeName.setStringValue( ioosSosObs.getFeatureType().name() );
        xb_featureTypeName.setCodeSpace( IoosSosConstants.CF_FEATURE_TYPES_CODESPACE );
        XmlHelper.append( xb_featureTypeMetadata, xb_featureTypeNameDoc );
        XmlHelper.append( xb_featureTypeMetaDataProp, xb_featureTypeMetadataDoc );

        // setBoundedBy
        if (ioosSosObs.getEnvelope() != null) {
            xb_featureCollection.setBoundedBy( IoosEncoderUtil.createBoundedBy( ioosSosObs.getEnvelope() ) );
        }

        MultiPointType xb_multipoint = (MultiPointType) xb_featureCollection.addNewLocation()
                .addNewGeometry().substitute( GmlConstants.QN_MULTIPOINT, MultiPointType.type );
        xb_multipoint.setSrsName( IoosSosConstants.SRS_URL_PREFIX + 4326 );
        PointArrayPropertyType xb_pointMembers = xb_multipoint.addNewPointMembers();

        for( StationAsset station : ioosSosObs.getSortedStationsWithPoints() ){
            Point stationPoint = ioosSosObs.getStationPoint( station );
            if( stationPoint != null ){
                // make stationPoint 2D, since gml:location shouldn't contain z
                stationPoint = FeatureUtil.clonePoint2d( stationPoint );
                //switch coord axis order back if necessary, since we switched them to x,y order above
                stationPoint = (Point) GeometryHandler.getInstance()
                        .switchCoordinateAxisFromToDatasourceIfNeeded(stationPoint);
                PointType xb_point = xb_pointMembers.addNewPoint();
                xb_point.addNewName().setStringValue( station.getAssetId() );
                xb_point.addNewPos().setStringValue( JTSHelper.getCoordinatesString( stationPoint ) );
            }
        }

        XmlObject xb_sweResult = IoosSwe2ResultEncoder.encodeResult( this, ioosSosObs );
        
        XmlObject xb_result = xb_obsType.addNewResult();
        xb_result.set( xb_sweResult );
        return xb_obsDoc;
    }

    @Override
    public boolean supportsResultStreamingForMergedValues() {
        // TODO Auto-generated method stub
        return false;
    }
}
