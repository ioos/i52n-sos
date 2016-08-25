package org.n52.sos.ioos.service.it;

import org.junit.AfterClass;
import org.junit.Rule;
import org.junit.runner.RunWith;

import org.n52.sos.config.SettingsManager;
import org.n52.sos.ioos.service.it.functional.DescribeSensorProcedureDescriptionFormatTest;
import org.n52.sos.ioos.service.it.functional.IoosGetObservationIntegrationTest;
import org.n52.sos.ioos.service.it.functional.IoosSensorMLFormatDescribeSensorTest;
import org.n52.sos.service.SosService;
import org.n52.sos.service.it.Client;
import org.n52.sos.service.it.ComplianceSuite;
import org.n52.sos.service.it.ComplianceSuiteRunner;
import org.n52.sos.service.it.MockHttpExecutor;
import org.n52.sos.service.it.RequestExecutor;

@RunWith(ComplianceSuiteRunner.class)
public class IOOSSOSComplianceTestSuite
        extends MockHttpExecutor
        implements ComplianceSuite {

    private final H2Database datasource = new H2Database();

    public IOOSSOSComplianceTestSuite() {
        super(SosService.class);
    }

    @Rule
    public H2Database getDatasource() {
        return datasource;
    }

    @Override
    public Client kvp() {
        return get("/kvp");
    }

    @Override
    public Client pox() {
        return post("/pox");
    }

    @Override
    public Client soap() {
        return post("/soap");
    }

    @Override
    public RequestExecutor getExecutor() {
        return this;
    }

    @Override
    public Class<?>[] getTests() {
        return new Class<?>[] {
            DescribeSensorProcedureDescriptionFormatTest.class,
            IoosSensorMLFormatDescribeSensorTest.class,
            IoosGetObservationIntegrationTest.class
        };
    }

    @AfterClass
    public static void cleanup() {
//        https://github.com/52North/SOS/issues/461
//        SettingsManager.getInstance().cleanup();
    }

}
