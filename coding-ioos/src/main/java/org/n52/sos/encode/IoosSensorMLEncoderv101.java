package org.n52.sos.encode;

import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.opengis.gml.EnvelopeType;
import net.opengis.sensorML.x101.AbstractProcessType;
import net.opengis.sensorML.x101.CapabilitiesDocument.Capabilities;
import net.opengis.sensorML.x101.DocumentationDocument.Documentation;
import net.opengis.sensorML.x101.IdentificationDocument.Identification;
import net.opengis.sensorML.x101.SensorMLDocument;
import net.opengis.swe.x101.AnyScalarPropertyType;
import net.opengis.swe.x101.DataComponentPropertyType;
import net.opengis.swe.x101.DataRecordType;
import net.opengis.swe.x101.SimpleDataRecordType;
import net.opengis.swe.x101.TextDocument.Text;
import net.opengis.swe.x101.TimeRangeDocument.TimeRange;

import org.apache.xmlbeans.XmlObject;
import org.joda.time.DateTime;
import org.n52.sos.binding.BindingConstants;
import org.n52.sos.binding.BindingRepository;
import org.n52.sos.cache.ContentCache;
import org.n52.sos.exception.CodedException;
import org.n52.sos.exception.ows.NoApplicableCodeException;
import org.n52.sos.ioos.asset.AbstractAsset;
import org.n52.sos.ioos.asset.AssetResolver;
import org.n52.sos.ioos.asset.NetworkAsset;
import org.n52.sos.ioos.asset.SensorAsset;
import org.n52.sos.ioos.asset.StationAsset;
import org.n52.sos.ogc.gml.GmlConstants;
import org.n52.sos.ogc.ows.OwsExceptionReport;
import org.n52.sos.ogc.sensorML.AbstractProcess;
import org.n52.sos.ogc.sensorML.AbstractSensorML;
import org.n52.sos.ogc.sensorML.SensorML;
import org.n52.sos.ogc.sensorML.SensorMLConstants;
import org.n52.sos.ogc.sensorML.System;
import org.n52.sos.ogc.sensorML.elements.AbstractSmlDocumentation;
import org.n52.sos.ogc.sensorML.elements.SmlCapabilities;
import org.n52.sos.ogc.sensorML.elements.SmlComponent;
import org.n52.sos.ogc.sensorML.elements.SmlDocumentation;
import org.n52.sos.ogc.sensorML.elements.SmlIdentifier;
import org.n52.sos.ogc.sensorML.elements.SmlIo;
import org.n52.sos.ogc.sos.Sos1Constants;
import org.n52.sos.ogc.sos.Sos2Constants;
import org.n52.sos.ogc.sos.SosConstants;
import org.n52.sos.ogc.sos.SosEnvelope;
import org.n52.sos.ogc.sos.SosProcedureDescription;
import org.n52.sos.ogc.swe.RangeValue;
import org.n52.sos.ogc.swe.SweConstants;
import org.n52.sos.ogc.swe.SweDataRecord;
import org.n52.sos.ogc.swe.SweField;
import org.n52.sos.ogc.swe.simpleType.SweAbstractUomType;
import org.n52.sos.ogc.swe.simpleType.SweTimeRange;
import org.n52.sos.service.Configurator;
import org.n52.sos.service.ProcedureDescriptionSettings;
import org.n52.sos.service.ServiceConfiguration;
import org.n52.sos.service.ServiceConstants.SupportedTypeKey;
import org.n52.sos.service.operator.ServiceOperatorRepository;
import org.n52.sos.util.CodingHelper;
import org.n52.sos.util.DateTimeHelper;
import org.n52.sos.util.SosHelper;
import org.n52.sos.util.XmlOptionsHelper;
import org.n52.sos.util.http.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axiomalaska.ioos.sos.IoosDefConstants;
import com.axiomalaska.ioos.sos.IoosSosConstants;
import com.axiomalaska.ioos.sos.IoosSweConstants;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class IoosSensorMLEncoderv101 extends SensorMLEncoderv101 {
    private static final Logger LOGGER = LoggerFactory.getLogger(IoosSensorMLEncoderv101.class);

    private static final MediaType IOOS_SENSORML_CONTENT_TYPE_M10 =
            new MediaType("text", "xml", "subtype", IoosSosConstants.SML_SUBTYPE_M10);
    
    private static final String HREF_FLAG ="encode_identification_as_href";
    
    private static final Map<SupportedTypeKey, Set<String>> SUPPORTED_TYPES = Collections.singletonMap(
            SupportedTypeKey.ProcedureDescriptionFormat, Collections.singleton(
                    IOOS_SENSORML_CONTENT_TYPE_M10.toString()));

    private static final Set<EncoderKey> ENCODER_KEYS = CodingHelper.encoderKeysForElements(
            IOOS_SENSORML_CONTENT_TYPE_M10.toString(), SosProcedureDescription.class, AbstractSensorML.class);

    private static final Map<String, Map<String, Set<String>>> SUPPORTED_PROCEDURE_DESCRIPTION_FORMATS = getFormats();

    @Override
    public Set<EncoderKey> getEncoderKeyType() {
        return Collections.unmodifiableSet(ENCODER_KEYS);
    }

    @Override
    public Map<SupportedTypeKey, Set<String>> getSupportedTypes() {
        return Collections.unmodifiableMap(SUPPORTED_TYPES);
    }

    @Override
    public MediaType getContentType() {
        return IOOS_SENSORML_CONTENT_TYPE_M10;
    }
    
    public IoosSensorMLEncoderv101() {
        ProcedureDescriptionSettings.getInstance().setUseServiceContactAsProcedureContact(false);
        
        LOGGER.debug("Encoder for the following keys initialized successfully: {}!",
                Joiner.on(", ").join(ENCODER_KEYS));
    }

    private static Map<String, Map<String, Set<String>>> getFormats() {
        Map<String, Set<String>> map = Maps.newHashMap();
        map.put(Sos1Constants.SERVICEVERSION, Collections.singleton(IOOS_SENSORML_CONTENT_TYPE_M10.toString()));
        map.put(Sos2Constants.SERVICEVERSION, Collections.singleton(IOOS_SENSORML_CONTENT_TYPE_M10.toString()));
        return Collections.singletonMap(SosConstants.SOS, map);
    }

    @Override
    public Set<String> getSupportedProcedureDescriptionFormats(String service, String version) {
        if (SUPPORTED_PROCEDURE_DESCRIPTION_FORMATS.get(service) != null
                && SUPPORTED_PROCEDURE_DESCRIPTION_FORMATS.get(service).get(version) != null) {
            return SUPPORTED_PROCEDURE_DESCRIPTION_FORMATS.get(service).get(version);
        }
        return Collections.emptySet();
    }

    @Override
	protected SensorMLDocument createSensorMLDescription(final SensorML smlSensorDesc) throws OwsExceptionReport {
        SensorMLDocument xbSmlDoc = super.createSensorMLDescription(smlSensorDesc);
        addIoosExtras(smlSensorDesc.getIdentifier(), xbSmlDoc);
        return xbSmlDoc;
	}
    
    @Override
    protected XmlObject createSensorDescriptionFromString(final AbstractSensorML sensorDesc) throws OwsExceptionReport {
    	XmlObject xmlObject = super.createSensorDescriptionFromString(sensorDesc);
    	if (xmlObject instanceof SensorMLDocument) {
    	    addIoosExtras(sensorDesc.getIdentifier(), (SensorMLDocument) xmlObject);
    	}
    	return xmlObject;
    }

    private void addIoosExtras(final String procedure, final SensorMLDocument xbSmlDoc) throws OwsExceptionReport {
        addServiceMetadata(xbSmlDoc);
        AbstractProcessType xbAbstractProcess = getAbstractProcessMemberFromWrapper(xbSmlDoc);
        if (xbAbstractProcess != null) {
            addObservationTimeRange(procedure, xbAbstractProcess);
            addSpatialBounds(procedure, xbAbstractProcess);
        }
    }

    private void addServiceMetadata(SensorMLDocument xbSmlDoc) {
        removeExistingCapabilities(xbSmlDoc.getSensorML(), IoosSosConstants.IOOS_SERVICE_METADATA);

        //add new service metadata capabilities if needed
        Capabilities xbServiceMetadataCapabilities = xbSmlDoc.getSensorML().addNewCapabilities();
        xbServiceMetadataCapabilities.setName(IoosSosConstants.IOOS_SERVICE_METADATA);            

        SimpleDataRecordType xbSimpleDataRecord = (SimpleDataRecordType) xbServiceMetadataCapabilities.addNewAbstractDataRecord()
                .substitute(SweConstants.QN_SIMPLEDATARECORD_SWE_101, SimpleDataRecordType.type);
        
        //template version
        addMetadataField(xbSimpleDataRecord, IoosSosConstants.IOOS_TEMPLATE_VERSION,
                IoosSosConstants.IOOS_VERSION_DEFINITION, IoosSosConstants.IOOS_VERSION_M10);

        //software version
        addMetadataField(xbSimpleDataRecord, IoosSosConstants.SOFTWARE_VERSION,
                null, Ioos52nSosVersionHandler.getIoosVersion());        
    }

    private void addMetadataField(SimpleDataRecordType xbSimpleDataRecord, String name, String definition,
            String value) {
        AnyScalarPropertyType xbField = xbSimpleDataRecord.addNewField();
        if (name != null) {
            xbField.setName(name);
        }        
        Text xbText = xbField.addNewText();
        if (definition != null) {
            xbText.setDefinition(definition);
        }
        if (value == null) {
            value = "unknown";
        }
        xbText.setValue(value);        
    }

    private void addObservationTimeRange(String procedure, AbstractProcessType xbAbstractProcess) {
        removeExistingCapabilities(xbAbstractProcess, IoosSweConstants.OBSERVATION_TIME_RANGE);

        Capabilities xbObsTimeRangeCap = xbAbstractProcess.addNewCapabilities();
        xbObsTimeRangeCap.setName(IoosSweConstants.OBSERVATION_TIME_RANGE);

        DataRecordType xbDataRecord = (DataRecordType) xbObsTimeRangeCap.addNewAbstractDataRecord()
                .substitute(SweConstants.QN_DATA_RECORD_SWE_101, DataRecordType.type);
        DataComponentPropertyType xbField = xbDataRecord.addNewField();
        xbField.setName(IoosSweConstants.OBSERVATION_TIME_RANGE);
        TimeRange xbTimeRange = xbField.addNewTimeRange();
        xbTimeRange.setDefinition(IoosSweConstants.OBSERVATION_TIME_RANGE_DEF);
        if (Configurator.getInstance() != null) {
            DateTime minProcPhenTime = Configurator.getInstance().getCache().getMinPhenomenonTimeForProcedure(procedure);
            DateTime maxProcPhenTime = Configurator.getInstance().getCache().getMaxPhenomenonTimeForProcedure(procedure);
            if (minProcPhenTime != null && maxProcPhenTime != null) {
                xbTimeRange.setValue(Lists.newArrayList(DateTimeHelper.formatDateTime2IsoString(minProcPhenTime),
                        DateTimeHelper.formatDateTime2IsoString(maxProcPhenTime)));            
            }
        }
    }

    private void addSpatialBounds(String procedure, AbstractProcessType xbAbstractProcess) throws OwsExceptionReport {
        //remove existing bounded by
        while (xbAbstractProcess.isSetBoundedBy()) {
            xbAbstractProcess.unsetBoundedBy();
        }

        Configurator configurator = Configurator.getInstance();
        if (configurator == null || configurator.getCache() == null) {
            //if configurator is null this might be a test, just return
            return;
        }

        ContentCache cache = configurator.getCache();
        //assume that the procedure has an equivalent offering (sensors won't have this)
        if (cache.hasEnvelopeForOffering(procedure)) {
            SosEnvelope envelopeForOffering = cache.getEnvelopeForOffering(procedure);            
            EnvelopeType xbEnvelope = xbAbstractProcess.addNewBoundedBy().addNewEnvelope();
            xbEnvelope.set(CodingHelper.encodeObjectToXml(GmlConstants.NS_GML, envelopeForOffering));
        }
    }

    private AbstractProcessType getAbstractProcessMemberFromWrapper(SensorMLDocument xbSmlDoc) {
        if (xbSmlDoc.getSensorML() != null && xbSmlDoc.getSensorML().getMemberArray() != null
                && xbSmlDoc.getSensorML().getMemberArray().length > 0
                && xbSmlDoc.getSensorML().getMemberArray(0).getProcess() != null) {
            return (AbstractProcessType) xbSmlDoc.getSensorML().getMemberArray(0).getProcess();
        }
        return null;
    }

    @Override
    protected List<SmlComponent> createComponentsForChildProcedures(
            final Set<SosProcedureDescription> childProcedures) throws CodedException {
        final List<SmlComponent> smlComponents = new LinkedList<SmlComponent>();
        for (final SosProcedureDescription childProcedure : childProcedures) {
            System childSystem = null;
            
            if (childProcedure instanceof System){
                childSystem = (System) childProcedure;
            } else if (childProcedure instanceof SensorML) { 
                // unwrap child procedure            
                SensorML childSensorML = (SensorML) childProcedure;
                if (childSensorML.isWrapper()){
                    AbstractProcess childWrappedAbstractProcess = childSensorML.getMembers().get(0);
                    if (childWrappedAbstractProcess instanceof System) {
                        childSystem = (System) childWrappedAbstractProcess;
                        childSystem.setIdentifier(childProcedure.getIdentifier());
                    }
                }
            }
            
            if (childSystem == null || childSystem.getIdentifier() == null
                    || childSystem.getIdentifier().isEmpty()){
                smlComponents.addAll(super.createComponentsForChildProcedures(
                        Sets.newHashSet(childProcedure)));
                continue;
            }
            
            AbstractAsset asset = AssetResolver.resolveAsset(childSystem.getIdentifier());
            if (asset == null || !(asset instanceof StationAsset || asset instanceof SensorAsset)) {
                smlComponents.addAll(super.createComponentsForChildProcedures(
                        Sets.newHashSet(childProcedure)));                
                continue;
            }
            
            final SmlComponent component = new SmlComponent(asset.getAssetShortId());            
            System system = new System();
            component.setProcess(system);
            
            if (asset instanceof StationAsset){
                //stationID
                system.addIdentifier(new SmlIdentifier(IoosDefConstants.STATION_ID,
                        IoosDefConstants.STATION_ID_DEF, asset.getAssetId()));

                for (SmlIdentifier smlIdentifier : childSystem.getIdentifications()) {
                    //shortName
                    if (smlIdentifier.getDefinition().equals(IoosDefConstants.SHORT_NAME_DEF)) {
                        system.addIdentifier(smlIdentifier);
                    }
                }
                
                //observationTimeRange
                DateTime minProcedureObsTime = Configurator.getInstance().getCache()
                        .getMinPhenomenonTimeForProcedure(childSystem.getIdentifier());
                DateTime maxProcedureObsTime = Configurator.getInstance().getCache()
                        .getMaxPhenomenonTimeForProcedure(childSystem.getIdentifier());
                if (minProcedureObsTime != null && maxProcedureObsTime != null) {
                    RangeValue<DateTime> obsTimeRangeValue = new RangeValue<DateTime>();
                    obsTimeRangeValue.setRangeStart(minProcedureObsTime);
                    obsTimeRangeValue.setRangeEnd(maxProcedureObsTime);

                    SweTimeRange obsTimeRange = new SweTimeRange();
                    obsTimeRange.setValue(obsTimeRangeValue);
                    
                    SweField obsTimeRangeField = new SweField(IoosSweConstants.OBSERVATION_TIME_RANGE, obsTimeRange);
                    
                    SweDataRecord obsTimeRangeDataRecord = new SweDataRecord();
                    obsTimeRangeDataRecord.addField(obsTimeRangeField);
                    
                    system.addCapabilities(new SmlCapabilities(IoosSweConstants.OBSERVATION_TIME_RANGE,
                            obsTimeRangeDataRecord));
                }

                //location
                system.setLocation(childSystem.getLocation());        

                //outputs
                system.setOutputs(Lists.newArrayList(createGenericIos(
                        aggregateProcedureIos(childSystem))));
                
            } else if (asset instanceof SensorAsset) {
                //TODO enable this once pull request is accepted
                //system.setGmlId(asset.getAssetShortId());

                //sensorID, use fake definition to signal custom encoder method that it should be an href
                system.addIdentifier(new SmlIdentifier(IoosDefConstants.SENSOR_ID,
                        HREF_FLAG, asset.getAssetId()));
                
                //documentation link
                SmlDocumentation smlDocumentation = new SmlDocumentation();
                smlDocumentation.setFormat(HREF_FLAG);
                try {
                    if (BindingRepository.getInstance()
                            .isBindingSupported(BindingConstants.KVP_BINDING_ENDPOINT)) {
                        final String version = ServiceOperatorRepository.getInstance()
                                .getSupportedVersions(SosConstants.SOS).contains(Sos1Constants.SERVICEVERSION) ? Sos1Constants.SERVICEVERSION
                                        : Sos2Constants.SERVICEVERSION;

                        smlDocumentation.setOnlineResource(SosHelper.getDescribeSensorUrl(version, ServiceConfiguration.getInstance()
                                .getServiceURL(), childProcedure.getIdentifier(),
                                BindingConstants.KVP_BINDING_ENDPOINT, childProcedure.getDescriptionFormat()));
                    } else {
                        smlDocumentation.setOnlineResource(childProcedure.getIdentifier());
                    }
                } catch (final UnsupportedEncodingException uee) {
                    throw new NoApplicableCodeException().withMessage("Error while encoding DescribeSensor URL")
                            .causedBy(uee);
                }                
                system.addDocumentation(smlDocumentation);                
                
                //outputs
                system.setOutputs(childSystem.getOutputs());
            }
            
            smlComponents.add(component);
        }
        return smlComponents;
    }

    private Set<SmlIo<?>> aggregateProcedureIos(Collection<? extends SosProcedureDescription> procedures) {
        Set<SmlIo<?>> ios = Sets.newHashSet();
        for (SosProcedureDescription procedure : procedures) {
            ios.addAll(aggregateProcedureIos(procedure));
        }
        return ios;
    }
    
    private Set<SmlIo<?>> aggregateProcedureIos(SosProcedureDescription procedure) {
        Set<SmlIo<?>> ios = Sets.newHashSet();
        if (procedure instanceof AbstractProcess) {
            AbstractProcess process = (AbstractProcess) procedure;
            ios.addAll(process.getOutputs());            
        } else if (procedure instanceof SensorML) {
            SensorML sml = (SensorML) procedure;
            ios.addAll(aggregateProcedureIos(sml.getMembers()));
        }
        ios.addAll(aggregateProcedureIos(procedure.getChildProcedures()));
        return ios;        
    } 

    private Collection<SmlIo<?>> createGenericIos(Collection<SmlIo<?>> ios) {
        Set<SmlIo<?>> genericIos = Sets.newHashSet();
        for (SmlIo<?> io : ios) {
            genericIos.add(createGenericIo(io));
        }
        return genericIos;
    }

    private <T> SmlIo<T> createGenericIo(SmlIo<T> io) {
        if (io == null || !io.isSetValue() || !io.getIoValue().isSetDefinition()){
            return io;
        }

        SmlIo<T> genericIo = new SmlIo<T>(io.getIoValue());
        String[] def = io.getIoValue().getDefinition().split("[:/]");
        genericIo.setIoName(def[def.length - 1]);        
        if (genericIo.getIoValue() instanceof SweAbstractUomType) {
            ((SweAbstractUomType<?>) genericIo.getIoValue()).setUom(null);
        }
        return genericIo;
    }
    
    @Override
    protected void addSpecialCapabilities(final AbstractProcess abstractProcess) {
        if (abstractProcess.isSetFeaturesOfInterest()) {
            abstractProcess.addCapabilities(createCapabilitiesFrom(                    
                    SensorMLConstants.ELEMENT_NAME_FEATURES_OF_INTEREST,
                    SensorMLConstants.FEATURE_OF_INTEREST_FIELD_DEFINITION,
                    createValueNamePairs(SensorMLConstants.FEATURE_OF_INTEREST_FIELD_NAME,
                            abstractProcess.getFeaturesOfInterest())));
        }

        if (abstractProcess.isSetOfferings()) {
            abstractProcess.addCapabilities(createCapabilitiesFrom(                    
                    SensorMLConstants.ELEMENT_NAME_OFFERINGS,
                    SensorMLConstants.OFFERING_FIELD_DEFINITION,
                    convertOfferingsToMap(abstractProcess.getOfferings())));
        }
        
        if (abstractProcess.isSetParentProcedures()) {
            Set<String> networkParents = Sets.newHashSet();
            Set<String> stationParents = Sets.newHashSet();
            Set<String> genericParents = Sets.newHashSet();
            
            for (String parentProcedure : abstractProcess.getParentProcedures()) {
                AbstractAsset asset = AssetResolver.resolveAsset(parentProcedure);
                if (asset instanceof NetworkAsset) {
                    networkParents.add(parentProcedure);
                } else if (asset instanceof StationAsset ){
                    stationParents.add(parentProcedure);                    
                } else {
                    genericParents.add(parentProcedure);
                }
            }

            if (!networkParents.isEmpty()){
                abstractProcess.addCapabilities(createCapabilitiesFrom(                    
                        IoosSosConstants.PARENT_NETWORK_PROCEDURES_LABEL,
                        IoosDefConstants.NETWORK_ID_DEF,
                        createValueNamePairs(IoosSosConstants.PARENT_NETWORK_FIELD_NAME,
                                networkParents)));
            }

            if (!stationParents.isEmpty()){
                abstractProcess.addCapabilities(createCapabilitiesFrom(                    
                        IoosSosConstants.PARENT_STATION_PROCEDURE_LABEL,
                        IoosDefConstants.STATION_ID_DEF,
                        createValueNamePairs(IoosSosConstants.PARENT_STATION_FIELD_NAME,
                                abstractProcess.getParentProcedures())));
            }

            if (!genericParents.isEmpty()) {
                abstractProcess.addCapabilities(createCapabilitiesFrom(                    
                        IoosSosConstants.PARENT_STATION_PROCEDURE_LABEL,
                        IoosDefConstants.STATION_ID_DEF,
                        createValueNamePairs(IoosSosConstants.PARENT_STATION_FIELD_NAME,
                                abstractProcess.getParentProcedures())));
            }
        }
    }
    

    protected Identification[] createIdentification(final List<SmlIdentifier> identifications) {
        if (identifications.size() == 1 && identifications.get(0).isSetDefinition() &&
                identifications.get(0).getDefinition().equals(HREF_FLAG)) {
            final Identification xbIdentification =
                    Identification.Factory.newInstance(XmlOptionsHelper.getInstance().getXmlOptions());
            xbIdentification.setHref(identifications.get(0).getValue());
            return new Identification[] { xbIdentification };
        }
        return super.createIdentification(identifications);
    }
    
    @Override
    protected Documentation[] createDocumentationArray(final List<AbstractSmlDocumentation> sosDocumentation) {
        if (sosDocumentation.size() == 1 && sosDocumentation.get(0) instanceof SmlDocumentation
                && ((SmlDocumentation) sosDocumentation.get(0)).isSetFormat()
                && ((SmlDocumentation) sosDocumentation.get(0)).getFormat().equals(HREF_FLAG)){
            final Documentation xbDocumentation = Documentation.Factory.newInstance();
            xbDocumentation.setHref(((SmlDocumentation) sosDocumentation.get(0)).getOnlineResource());
            return new Documentation[] { xbDocumentation };
        }
        return super.createDocumentationArray(sosDocumentation);
    }

    /**
     * Remove an sml:capabilities from a SensorMLDocument if it exists
     * @param xbSmlDoc The SensorMLDocument to operate on
     * @param capabilitiesName Name of the capabilities to remove
     */
    private void removeExistingCapabilities(XmlObject xbXmlObject, String capabilitiesName) {
        Capabilities[] capabilitiesArray = null;
        if (xbXmlObject instanceof SensorMLDocument.SensorML) {
            capabilitiesArray = ((SensorMLDocument.SensorML) xbXmlObject).getCapabilitiesArray();
        } else if (xbXmlObject instanceof AbstractProcessType) {
            capabilitiesArray = ((AbstractProcessType) xbXmlObject).getCapabilitiesArray();
        } else {
            throw new IllegalArgumentException(String.format("Encountered unsupported XmlObject type '%s'",
                    xbXmlObject.getClass().getSimpleName()));
        }
        
        //remove existing service metadata if found
        if (capabilitiesArray != null) {
            boolean fullIteration = false;
            while (!fullIteration) {
                for (int i = 0; i < capabilitiesArray.length; i++) {
                    Capabilities xbCapabilities = capabilitiesArray[i];
                    if (xbCapabilities.isSetName() && xbCapabilities.getName().equals(capabilitiesName)) {
                        //remove this capabilities and start the loop over, since there may be others
                        //and the array index will have shifted after the delete
                        if (xbXmlObject instanceof SensorMLDocument.SensorML) {
                            ((SensorMLDocument.SensorML) xbXmlObject).removeCapabilities(i);
                        } else if (xbXmlObject instanceof AbstractProcessType) {
                            ((AbstractProcessType) xbXmlObject).removeCapabilities(i);
                        }
                        //don't need to check for other types since we already did above
                    }
                }
                fullIteration = true;
            }
        }        
    }
}