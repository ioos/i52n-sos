package org.n52.sos.ioos.service.it.functional;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import org.apache.xmlbeans.XmlObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import org.n52.sos.ogc.ows.OwsExceptionReport;
import org.n52.sos.ogc.sensorML.SensorML;
import org.n52.sos.ogc.sensorML.SensorMLConstants;
import org.n52.sos.ogc.sos.Sos1Constants;
import org.n52.sos.ogc.sos.Sos2Constants;
import org.n52.sos.ogc.sos.SosConstants;
import org.n52.sos.request.operator.RequestOperatorKey;
import org.n52.sos.request.operator.RequestOperatorRepository;
import org.n52.sos.service.operator.ServiceOperatorKey;
import org.n52.sos.util.CodingHelper;

import com.axiomalaska.ioos.sos.IoosSosConstants;
import com.google.common.collect.ImmutableList;

import net.opengis.sensorML.x101.SensorMLDocument;
import net.opengis.swes.x20.DescribeSensorResponseDocument;
import net.opengis.swes.x20.InsertSensorDocument;
import net.opengis.swes.x20.InsertSensorResponseDocument;

/**
 * @author Shane St Clair <shane@axiomdatascience.com>
 */

public class IoosSensorMLFormatDescribeSensorTest extends AbstractIoosComplianceSuiteTest {
    private static final String PROCEDURE = "procedure";

    @Rule
    public final ErrorCollector errors = new ErrorCollector();

    @Before
    public void before() throws OwsExceptionReport {
        activate();

        InsertSensorDocument insertSensorSml1Doc = createInsertSensorRequest(PROCEDURE, PROCEDURE, null, "offering",
                ImmutableList.of("obs_prop"), SensorMLConstants.SENSORML_OUTPUT_FORMAT_URL);
        assertThat(pox().entity(insertSensorSml1Doc.xmlText(XML_OPTIONS)).response().asXmlObject(),
                is(instanceOf(InsertSensorResponseDocument.class)));
    }

    private void activate() {
        ServiceOperatorKey sok = new ServiceOperatorKey(SosConstants.SOS, Sos2Constants.SERVICEVERSION);
        RequestOperatorRepository.getInstance().setActive(new RequestOperatorKey(sok, Sos2Constants.Operations.InsertSensor.name()), true);
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
}
