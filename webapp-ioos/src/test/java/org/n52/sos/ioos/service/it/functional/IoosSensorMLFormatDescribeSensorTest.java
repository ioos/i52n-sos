package org.n52.sos.ioos.service.it.functional;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import org.n52.sos.ds.hibernate.H2Configuration;
import org.n52.sos.ogc.OGCConstants;
import org.n52.sos.ogc.gml.CodeWithAuthority;
import org.n52.sos.ogc.om.OmConstants;
import org.n52.sos.ogc.om.OmObservableProperty;
import org.n52.sos.ogc.om.features.SfConstants;
import org.n52.sos.ogc.ows.OwsExceptionReport;
import org.n52.sos.ogc.sensorML.SensorML;
import org.n52.sos.ogc.sensorML.SensorMLConstants;
import org.n52.sos.ogc.sensorML.elements.SmlIdentifier;
import org.n52.sos.ogc.sos.Sos1Constants;
import org.n52.sos.ogc.sos.Sos2Constants;
import org.n52.sos.ogc.sos.SosConstants;
import org.n52.sos.request.operator.RequestOperatorKey;
import org.n52.sos.request.operator.RequestOperatorRepository;
import org.n52.sos.service.Configurator;
import org.n52.sos.service.it.AbstractComplianceSuiteTest;
import org.n52.sos.service.it.Client;
import org.n52.sos.service.operator.ServiceOperatorKey;
import org.n52.sos.util.CodingHelper;
import org.n52.sos.util.XmlOptionsHelper;
import org.n52.sos.util.http.MediaTypes;

import com.axiomalaska.ioos.sos.IoosSosConstants;

import net.opengis.sensorML.x101.SensorMLDocument;
import net.opengis.sos.x20.SosInsertionMetadataType;
import net.opengis.swes.x20.DescribeSensorDocument;
import net.opengis.swes.x20.DescribeSensorResponseDocument;
import net.opengis.swes.x20.DescribeSensorType;
import net.opengis.swes.x20.InsertSensorDocument;
import net.opengis.swes.x20.InsertSensorResponseDocument;
import net.opengis.swes.x20.InsertSensorType;

/**
 * @author Shane St Clair <shane@axiomdatascience.com>
 */

public class IoosSensorMLFormatDescribeSensorTest extends AbstractComplianceSuiteTest {
    private static final XmlOptions XML_OPTIONS = XmlOptionsHelper.getInstance().getXmlOptions();

    private static final String PROCEDURE = "procedure";

    @Rule
    public final ErrorCollector errors = new ErrorCollector();

    @Before
    public void before() throws OwsExceptionReport {
        activate();

        InsertSensorDocument insertSensorSml1Doc = createInsertSensorRequest(PROCEDURE, PROCEDURE, "offering", "obs_prop",
                SensorMLConstants.SENSORML_OUTPUT_FORMAT_URL);
        assertThat(pox().entity(insertSensorSml1Doc.xmlText(XML_OPTIONS)).response().asXmlObject(),
                is(instanceOf(InsertSensorResponseDocument.class)));
    }

    private void activate() {
        ServiceOperatorKey sok = new ServiceOperatorKey(SosConstants.SOS, Sos2Constants.SERVICEVERSION);
        RequestOperatorRepository.getInstance().setActive(new RequestOperatorKey(sok, Sos2Constants.Operations.InsertSensor.name()), true);
    }

    @After
    public void after() throws OwsExceptionReport {
        H2Configuration.truncate();
        Configurator.getInstance().getCacheController().update();
    }

    @Test
    public void testSos2DescribeSensor() {
        XmlObject responseXml  = sendDescribeSensorRequestViaPox(
                Sos2Constants.SERVICEVERSION, PROCEDURE, IoosSosConstants.SML_PROFILE_M10);
        assertThat(responseXml, is(instanceOf(DescribeSensorResponseDocument.class)));
        verifyDescribeSensorResponseDocument((DescribeSensorResponseDocument) responseXml,
                IoosSosConstants.SML_PROFILE_M10);
    }

    @Test
    public void testSos1DescribeSensor() throws OwsExceptionReport {
        XmlObject responseXml  = sendDescribeSensorRequestViaPox(
                Sos1Constants.SERVICEVERSION, PROCEDURE, IoosSosConstants.SML_PROFILE_M10);
        assertThat(responseXml, is(instanceOf(SensorMLDocument.class)));
        verifySensorMLDocument((SensorMLDocument) responseXml, PROCEDURE);
    }

    private void verifyDescribeSensorResponseDocument(DescribeSensorResponseDocument describeSensorResponseDoc,
            String expectedProcedureDescriptionFormat) {
        String procedureDescriptionFormat = describeSensorResponseDoc.getDescribeSensorResponse().getProcedureDescriptionFormat();

        // should be equal to what was requested in DescribeSensor request
        assertEquals(expectedProcedureDescriptionFormat, procedureDescriptionFormat);
    }

    private void verifySensorMLDocument(SensorMLDocument sensorMLDoc, String identifier)
            throws OwsExceptionReport {
        Object decodedXmlObject = CodingHelper.decodeXmlObject(sensorMLDoc);
        assertThat(decodedXmlObject, is(instanceOf(SensorML.class)));
        SensorML sensorML = (SensorML) decodedXmlObject;

        //should be equal to what was requested in DescribeSensor request
        assertEquals(identifier, sensorML.getIdentifier());
    }

    private XmlObject sendDescribeSensorRequestViaPox(String version, String procedure, String procedureDescriptionFormat) {
        DescribeSensorDocument document = DescribeSensorDocument.Factory.newInstance();
        DescribeSensorType describeSensorRequest = document.addNewDescribeSensor();
        describeSensorRequest.setService(SosConstants.SOS);
        describeSensorRequest.setVersion(version);
        describeSensorRequest.setProcedure(procedure);
        describeSensorRequest.setProcedureDescriptionFormat(procedureDescriptionFormat);
        XmlObject responseXml = pox().entity(document.xmlText(XML_OPTIONS)).response().asXmlObject();
        return responseXml;
    }

    protected Client pox() {
        return getExecutor().pox()
                .contentType(MediaTypes.APPLICATION_XML.toString())
                .accept(MediaTypes.APPLICATION_XML.toString());
    }

    protected SensorML createProcedure(String identifier, String procedure, String offering, String obsProp) {
        SensorML wrapper = new SensorML();
        org.n52.sos.ogc.sensorML.System sensorML = new org.n52.sos.ogc.sensorML.System();
        wrapper.addMember(sensorML);
        sensorML.addIdentifier(new SmlIdentifier(identifier, OGCConstants.URN_UNIQUE_IDENTIFIER, procedure));
        sensorML.addPhenomenon(new OmObservableProperty(obsProp));
        wrapper.setIdentifier(new CodeWithAuthority(procedure, "identifier_codespace"));
        return wrapper;
    }

    protected InsertSensorDocument createInsertSensorRequest(String identifier, String procedure, String offering,
            String obsProp, String procedureDescriptionFormat) throws OwsExceptionReport {
        SensorML sml = createProcedure(identifier, procedure, offering, obsProp);

        InsertSensorDocument document = InsertSensorDocument.Factory.newInstance();
        InsertSensorType insertSensor = document.addNewInsertSensor();
        insertSensor.setService(SosConstants.SOS);
        insertSensor.setVersion(Sos2Constants.SERVICEVERSION);
        insertSensor.addObservableProperty(obsProp);

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

    private SosInsertionMetadataType createSensorInsertionMetadata() {
        SosInsertionMetadataType sosInsertionMetadata = SosInsertionMetadataType.Factory.newInstance();
        sosInsertionMetadata.addFeatureOfInterestType(OGCConstants.UNKNOWN);
        sosInsertionMetadata.addFeatureOfInterestType(SfConstants.SAMPLING_FEAT_TYPE_SF_SAMPLING_POINT);
        for (String observationType : OmConstants.OBSERVATION_TYPES) {
            sosInsertionMetadata.addObservationType(observationType);
        }
        return sosInsertionMetadata;
    }
}
