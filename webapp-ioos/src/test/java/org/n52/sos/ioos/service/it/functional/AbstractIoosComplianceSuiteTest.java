package org.n52.sos.ioos.service.it.functional;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.Collection;
import java.util.List;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.joda.time.DateTime;
import org.junit.After;
import org.n52.sos.ds.hibernate.H2Configuration;
import org.n52.sos.ds.hibernate.util.TemporalRestrictions;
import org.n52.sos.exception.ows.concrete.InvalidSridException;
import org.n52.sos.ogc.OGCConstants;
import org.n52.sos.ogc.filter.FilterConstants;
import org.n52.sos.ogc.filter.FilterConstants.TimeOperator;
import org.n52.sos.ogc.filter.TemporalFilter;
import org.n52.sos.ogc.gml.CodeWithAuthority;
import org.n52.sos.ogc.gml.time.TimeInstant;
import org.n52.sos.ogc.gml.time.TimePeriod;
import org.n52.sos.ogc.om.OmConstants;
import org.n52.sos.ogc.om.OmObservableProperty;
import org.n52.sos.ogc.om.OmObservation;
import org.n52.sos.ogc.om.OmObservationConstellation;
import org.n52.sos.ogc.om.SingleObservationValue;
import org.n52.sos.ogc.om.features.SfConstants;
import org.n52.sos.ogc.om.features.samplingFeatures.SamplingFeature;
import org.n52.sos.ogc.om.values.QuantityValue;
import org.n52.sos.ogc.ows.OwsExceptionReport;
import org.n52.sos.ogc.sensorML.SensorML;
import org.n52.sos.ogc.sensorML.SensorMLConstants;
import org.n52.sos.ogc.sensorML.elements.SmlCapabilities;
import org.n52.sos.ogc.sensorML.elements.SmlIdentifier;
import org.n52.sos.ogc.sos.Sos1Constants;
import org.n52.sos.ogc.sos.Sos2Constants;
import org.n52.sos.ogc.sos.SosConstants;
import org.n52.sos.ogc.swe.SweField;
import org.n52.sos.ogc.swe.SweSimpleDataRecord;
import org.n52.sos.ogc.swe.simpleType.SweText;
import org.n52.sos.service.Configurator;
import org.n52.sos.service.it.AbstractComplianceSuiteTest;
import org.n52.sos.service.it.Client;
import org.n52.sos.service.it.Response;
import org.n52.sos.util.CodingHelper;
import org.n52.sos.util.XmlOptionsHelper;
import org.n52.sos.util.http.MediaTypes;

import com.axiomalaska.ioos.sos.GeomHelper;
import com.google.common.collect.ImmutableList;
import com.vividsolutions.jts.geom.Point;

import net.opengis.fes.x20.DuringDocument;
import net.opengis.sos.x10.GetObservationDocument;
import net.opengis.sos.x10.GetObservationDocument.GetObservation;
import net.opengis.sos.x20.GetObservationType;
import net.opengis.sos.x20.InsertObservationDocument;
import net.opengis.sos.x20.InsertObservationType;
import net.opengis.sos.x20.SosInsertionMetadataType;
import net.opengis.swes.x20.DescribeSensorDocument;
import net.opengis.swes.x20.DescribeSensorType;
import net.opengis.swes.x20.InsertSensorDocument;
import net.opengis.swes.x20.InsertSensorType;

public abstract class AbstractIoosComplianceSuiteTest extends AbstractComplianceSuiteTest {
    protected static final XmlOptions XML_OPTIONS = XmlOptionsHelper.getInstance().getXmlOptions();

    @After
    public void after() throws OwsExceptionReport {
        H2Configuration.truncate();
        Configurator.getInstance().getCacheController().update();
    }

    protected Client pox() {
        return getExecutor().pox()
                .contentType(MediaTypes.APPLICATION_XML.toString())
                .accept(MediaTypes.APPLICATION_XML.toString());
    }

    protected SensorML createProcedure(String identifier, String procedure, String parentProcedure,
            String offering, Collection<String> obsProps) {
        SensorML wrapper = new SensorML();
        org.n52.sos.ogc.sensorML.System sensorML = new org.n52.sos.ogc.sensorML.System();
        wrapper.addMember(sensorML);
        sensorML.addIdentifier(new SmlIdentifier(identifier, OGCConstants.URN_UNIQUE_IDENTIFIER, procedure));

        //set parent procedure
        if (parentProcedure != null) {
            SweField parentProcedureSweField = new SweField(SensorMLConstants.PARENT_PROCEDURE_FIELD_NAME,
                    new SweText().setValue(parentProcedure));
            SmlCapabilities parentProceduresSmlCapabilities = new SmlCapabilities(SensorMLConstants.ELEMENT_NAME_PARENT_PROCEDURES,
                    new SweSimpleDataRecord().addField(parentProcedureSweField));
            sensorML.addCapabilities(parentProceduresSmlCapabilities);
        }

        //set offering capabilities
        if (offering != null) {
            SweField offeringSweField = new SweField(offering, new SweText().setValue(offering)
                    .setDefinition(OGCConstants.URN_OFFERING_ID));
            SmlCapabilities offeringSmlCapabilities = new SmlCapabilities(SensorMLConstants.ELEMENT_NAME_OFFERINGS,
                    new SweSimpleDataRecord().addField(offeringSweField));
            sensorML.addCapabilities(offeringSmlCapabilities);
        }

        if (obsProps != null) {
            for (String obsProp : obsProps) {
                sensorML.addPhenomenon(new OmObservableProperty(obsProp));
            }
        }

        wrapper.setIdentifier(new CodeWithAuthority(procedure, "identifier_codespace"));
        return wrapper;
    }

    protected InsertSensorDocument createInsertSensorRequest(String identifier, String procedure, String parentProcedure,
            String offering, List<String> obsProps, String procedureDescriptionFormat) throws OwsExceptionReport {
        SensorML sml = createProcedure(identifier, procedure, parentProcedure, offering, obsProps);

        InsertSensorDocument document = InsertSensorDocument.Factory.newInstance();
        InsertSensorType insertSensor = document.addNewInsertSensor();
        insertSensor.setService(SosConstants.SOS);
        insertSensor.setVersion(Sos2Constants.SERVICEVERSION);

        if (obsProps != null) {
            for (String obsProp : obsProps) {
                insertSensor.addObservableProperty(obsProp);
            }
        }

        // Only URL formats are supported here because valid procedureDescriptionFormats
        // (checked by SosHelper.checkFormat) are determined by scanning all
        // ProcedureEncoder getSupportedProcedureDescriptionFormats(),
        // which specifies different supported formats by service and version.
        // Since InsertSensor is an SOS 2.0 operation, only formats listed by
        // ProcedureEncoders as 2.0 formats are supported.
        // Conversions to other requested formats are enabled by converters (e.g. SensorMLUrlMimeTypeConverter).
        insertSensor.setProcedureDescriptionFormat(procedureDescriptionFormat);

        insertSensor.addNewMetadata().addNewInsertionMetadata().set(createSensorInsertionMetadata());
        insertSensor.addNewProcedureDescription().set(CodingHelper.encodeObjectToXml(SensorMLConstants.NS_SML, sml));
        return document;
    }

    protected SosInsertionMetadataType createSensorInsertionMetadata() {
        SosInsertionMetadataType sosInsertionMetadata = SosInsertionMetadataType.Factory.newInstance();
        sosInsertionMetadata.addFeatureOfInterestType(OGCConstants.UNKNOWN);
        sosInsertionMetadata.addFeatureOfInterestType(SfConstants.SAMPLING_FEAT_TYPE_SF_SAMPLING_POINT);
        for (String observationType : OmConstants.OBSERVATION_TYPES) {
            sosInsertionMetadata.addObservationType(observationType);
        }
        return sosInsertionMetadata;
    }

    protected XmlObject sendDescribeSensorRequestViaPox(String version, String procedure, String procedureDescriptionFormat) {
        DescribeSensorDocument document = DescribeSensorDocument.Factory.newInstance();
        DescribeSensorType describeSensorRequest = document.addNewDescribeSensor();
        describeSensorRequest.setService(SosConstants.SOS);
        describeSensorRequest.setVersion(version);
        describeSensorRequest.setProcedure(procedure);
        describeSensorRequest.setProcedureDescriptionFormat(procedureDescriptionFormat);
        XmlObject responseXml = pox().entity(document.xmlText(XML_OPTIONS)).response().asXmlObject();
        return responseXml;
    }

    protected SamplingFeature createSamplingFeature(String featureIdentifier, double lat, double lng, double height)
            throws InvalidSridException {
        SamplingFeature samplingFeature = new SamplingFeature(new CodeWithAuthority(featureIdentifier));
        Point point = GeomHelper.createLatLngPoint(lat, lng, height);
        samplingFeature.setGeometry(point);
        return samplingFeature;
    }

    protected OmObservationConstellation createObservationConstellation(String procedure, String offering,
            String observableProperty, SamplingFeature feature) {
        OmObservationConstellation observationConstellation = new OmObservationConstellation();
        observationConstellation.setFeatureOfInterest(feature);

        OmObservableProperty omObservableProperty = new OmObservableProperty(observableProperty);
        observationConstellation.setObservableProperty(omObservableProperty);

        observationConstellation.setObservationType(OmConstants.OBS_TYPE_MEASUREMENT);
        observationConstellation.setProcedure(createProcedure(procedure, procedure, null, offering,
                ImmutableList.of(observableProperty)));
        return observationConstellation;
    }

    protected OmObservation createNumericObservation(OmObservationConstellation observationConstellation,
            DateTime time, Double value, String units) {
        OmObservation observation = new OmObservation();
        observation.setObservationConstellation(observationConstellation);
        observation.setResultTime(new TimeInstant(time));
        observation.setValue(new SingleObservationValue<>(new TimeInstant(time), new QuantityValue(value, units)));
        return observation;
    }

    protected InsertObservationDocument createInsertObservationRequest(Collection<OmObservation> observations,
            List<String> offerings) throws OwsExceptionReport {
        InsertObservationDocument document = InsertObservationDocument.Factory.newInstance();
        InsertObservationType insertObservation = document.addNewInsertObservation();
        insertObservation.setService(SosConstants.SOS);
        insertObservation.setVersion(Sos2Constants.SERVICEVERSION);

        if (offerings != null) {
            for (String offering : offerings) {
                insertObservation.addNewOffering().setStringValue(offering);
            }
        }

        for (OmObservation observation : observations) {
            insertObservation.addNewObservation().addNewOMObservation().set(CodingHelper
                    .encodeObjectToXml(OmConstants.NS_OM_2, observation));
        }
        return document;
    }

    protected Response sendGetObservation1RequestViaPox(String offering, String responseFormat, String acceptType,
            Collection<String> procedures, Collection<String> observedProperties, TimePeriod timePeriod) throws OwsExceptionReport {
        GetObservationDocument document = GetObservationDocument.Factory.newInstance();
        GetObservation getObservationRequest = document.addNewGetObservation();
        getObservationRequest.setService(SosConstants.SOS);
        getObservationRequest.setVersion(Sos1Constants.SERVICEVERSION);
        getObservationRequest.setOffering(offering);
        getObservationRequest.setResponseFormat(responseFormat);

        if (procedures != null) {
            for (String procedure : procedures) {
                getObservationRequest.addProcedure(procedure);
            }
        }

        if (observedProperties != null) {
            for (String observedProperty : observedProperties) {
                getObservationRequest.addObservedProperty(observedProperty);
            }
        }

        if (timePeriod != null) {
            TemporalFilter temporalFilter = new TemporalFilter(
                    TimeOperator.TM_During, timePeriod,
                    TemporalRestrictions.PHENOMENON_TIME_VALUE_REFERENCE);
            XmlObject encodedTemporalFilter = CodingHelper.encodeObjectToXml(FilterConstants.NS_FES_2, temporalFilter);
            assertThat(encodedTemporalFilter, is(instanceOf(DuringDocument.class)));
            DuringDocument duringDoc = (DuringDocument) encodedTemporalFilter;
            getObservationRequest.addNewEventTime().addNewTemporalOps().set(duringDoc.getDuring());
        }

        Client client = pox().entity(document.xmlText(XML_OPTIONS));
        if (acceptType != null) {
            client.accept(acceptType);
        }
        return client.response();
    }

    protected Response sendGetObservation2RequestViaPox(String offering, String responseFormat, String acceptType,
            Collection<String> procedures, Collection<String> observedProperties, TimePeriod timePeriod) throws OwsExceptionReport {
        net.opengis.sos.x20.GetObservationDocument document = net.opengis.sos.x20.GetObservationDocument.Factory.newInstance();
        GetObservationType getObservationRequest = document.addNewGetObservation();
        getObservationRequest.setService(SosConstants.SOS);
        getObservationRequest.setVersion(Sos2Constants.SERVICEVERSION);
        getObservationRequest.addOffering(offering);
        getObservationRequest.setResponseFormat(responseFormat);

        if (procedures != null) {
            for (String procedure : procedures) {
                getObservationRequest.addProcedure(procedure);
            }
        }

        if (observedProperties != null) {
            for (String observedProperty : observedProperties) {
                getObservationRequest.addObservedProperty(observedProperty);
            }
        }

        if (timePeriod != null) {
            TemporalFilter temporalFilter = new TemporalFilter(
                    TimeOperator.TM_During, timePeriod,
                    TemporalRestrictions.PHENOMENON_TIME_VALUE_REFERENCE);
            XmlObject encodedTemporalFilter = CodingHelper.encodeObjectToXml(FilterConstants.NS_FES_2, temporalFilter);
            assertThat(encodedTemporalFilter, is(instanceOf(DuringDocument.class)));
            DuringDocument duringDoc = (DuringDocument) encodedTemporalFilter;
            getObservationRequest.addNewTemporalFilter().addNewTemporalOps().set(duringDoc.getDuring());
        }

        Client client = pox().entity(document.xmlText(XML_OPTIONS));
        if (acceptType != null) {
            client.accept(acceptType);
        }
        return client.response();
    }

    @SuppressWarnings("unchecked")
    protected <T> T castXmlAnyToClass(XmlObject xmlObject, Class<T> clazz) throws XmlException {
        //reparse from string
        //TODO is there a better way?
        XmlObject parsedObject = XmlObject.Factory.parse(xmlObject.toString());
        assertThat(parsedObject, is(instanceOf(clazz)));
        return (T) parsedObject;
    }
}
