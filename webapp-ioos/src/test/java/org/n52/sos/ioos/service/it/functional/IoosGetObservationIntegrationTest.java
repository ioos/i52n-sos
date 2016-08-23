package org.n52.sos.ioos.service.it.functional;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import org.n52.sos.ds.hibernate.testdata.IoosHibernateTestDataManager;
import org.n52.sos.encode.IoosNetcdfEncoder;
import org.n52.sos.ioos.asset.NetworkAsset;
import org.n52.sos.ioos.asset.SensorAsset;
import org.n52.sos.ioos.asset.StationAsset;
import org.n52.sos.ogc.om.OmConstants;
import org.n52.sos.ogc.om.OmObservation;
import org.n52.sos.ogc.om.OmObservationConstellation;
import org.n52.sos.ogc.om.features.samplingFeatures.SamplingFeature;
import org.n52.sos.ogc.ows.OwsExceptionReport;
import org.n52.sos.ogc.sensorML.SensorMLConstants;
import org.n52.sos.ogc.sos.Sos2Constants;
import org.n52.sos.ogc.sos.SosConstants;
import org.n52.sos.ogc.wml.WaterMLConstants;
import org.n52.sos.request.operator.RequestOperatorKey;
import org.n52.sos.request.operator.RequestOperatorRepository;
import org.n52.sos.service.operator.ServiceOperatorKey;
import org.n52.sos.util.http.MediaTypes;

import com.axiomalaska.ioos.sos.IoosSosConstants;
import com.axiomalaska.ioos.sos.IoosSweConstants;
import com.beust.jcommander.internal.Lists;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;

import net.opengis.gml.AbstractFeatureType;
import net.opengis.gml.BoundingShapeType;
import net.opengis.gml.FeaturePropertyType;
import net.opengis.gml.LocationPropertyType;
import net.opengis.om.x10.ObservationCollectionDocument;
import net.opengis.om.x10.ObservationCollectionType;
import net.opengis.om.x10.ObservationPropertyType;
import net.opengis.om.x10.ObservationType;
import net.opengis.om.x10.ProcessPropertyType;
import net.opengis.ows.x11.ExceptionReportDocument;
import net.opengis.sensorML.x101.AbstractProcessType;
import net.opengis.sos.x20.InsertObservationResponseDocument;
import net.opengis.swe.x20.AbstractDataComponentType;
import net.opengis.swe.x20.DataRecordDocument;
import net.opengis.swe.x20.DataRecordType;
import net.opengis.swe.x20.DataRecordType.Field;
import net.opengis.swes.x20.InsertSensorDocument;
import net.opengis.swes.x20.InsertSensorResponseDocument;
import ucar.nc2.dataset.NetcdfDataset;

/**
 * @author Shane St Clair <shane@axiomdatascience.com>
 */

public class IoosGetObservationIntegrationTest extends AbstractIoosComplianceSuiteTest {
    private static final String NETWORK_OFFERING = "network_offering";
    private static final String STATION_OFFERING = "station_offering";
    private static final String AUTHORITY = "authority";
    private static final NetworkAsset NETWORK_ASSET = new NetworkAsset(AUTHORITY, "all");
    private static final StationAsset STATION_ASSET = new StationAsset(AUTHORITY, "station1");
    private static final SensorAsset SENSOR_ASSET = new SensorAsset(STATION_ASSET, "sensor1");
    private static final String OBS_PROP = "obs_prop";
    private static final String FEATURE = "feature";

    @Rule
    public final ErrorCollector errors = new ErrorCollector();

    @Before
    public void before() throws OwsExceptionReport {
        activate();

        //TODO InsertSensor should support having no observable properties if they are container procedures (networks and stations)
        //https://github.com/ioos/i52n-sos/issues/5

        //insert network
        InsertSensorDocument insertNetworkSensorDoc = createInsertSensorRequest(NETWORK_ASSET.getAssetId(), NETWORK_ASSET.getAssetId(),
                null, NETWORK_OFFERING, ImmutableList.of(OBS_PROP), SensorMLConstants.SENSORML_OUTPUT_FORMAT_URL);
        assertThat(pox().entity(insertNetworkSensorDoc.xmlText(XML_OPTIONS)).response().asXmlObject(),
                is(instanceOf(InsertSensorResponseDocument.class)));

        //insert station
        InsertSensorDocument insertStationSensorDoc = createInsertSensorRequest(STATION_ASSET.getAssetId(), STATION_ASSET.getAssetId(),
                NETWORK_ASSET.getAssetId(), STATION_OFFERING, ImmutableList.of(OBS_PROP), SensorMLConstants.SENSORML_OUTPUT_FORMAT_URL);
        assertThat(pox().entity(insertStationSensorDoc.xmlText(XML_OPTIONS)).response().asXmlObject(),
                is(instanceOf(InsertSensorResponseDocument.class)));

        //insert sensor
        InsertSensorDocument insertSensorSensorDoc = createInsertSensorRequest(SENSOR_ASSET.getAssetId(), SENSOR_ASSET.getAssetId(),
                STATION_ASSET.getAssetId(), null, ImmutableList.of(OBS_PROP), SensorMLConstants.SENSORML_OUTPUT_FORMAT_URL);
        assertThat(pox().entity(insertSensorSensorDoc.xmlText(XML_OPTIONS)).response().asXmlObject(),
                is(instanceOf(InsertSensorResponseDocument.class)));

        //insert observations
        SamplingFeature samplingFeature = createSamplingFeature(FEATURE, IoosHibernateTestDataManager.randomLat(),
                IoosHibernateTestDataManager.randomLng(), IoosHibernateTestDataManager.randomInRange(-50.0, 0.0, 2));
        OmObservationConstellation observationConstellation = createObservationConstellation(
                SENSOR_ASSET.getAssetId(), NETWORK_OFFERING, OBS_PROP, samplingFeature);
        List<OmObservation> observations = Lists.newArrayList();
        for (int i = 0; i < 100; i++ ) {
            observations.add(createNumericObservation(observationConstellation, new DateTime().minusHours(i),
                    IoosHibernateTestDataManager.randomInRange(35.0, 55.0, 2), "units"));
        }
        assertThat(pox().entity(createInsertObservationRequest(observations, ImmutableList.of(NETWORK_OFFERING, STATION_OFFERING))
                .xmlText(XML_OPTIONS)).response().asXmlObject(),
                is(instanceOf(InsertObservationResponseDocument.class)));
    }

    private void activate() {
        ServiceOperatorKey sok = new ServiceOperatorKey(SosConstants.SOS, Sos2Constants.SERVICEVERSION);
        RequestOperatorRepository.getInstance().setActive(new RequestOperatorKey(
                sok, Sos2Constants.Operations.InsertSensor.name()), true);
        RequestOperatorRepository.getInstance().setActive(new RequestOperatorKey(
                sok, SosConstants.Operations.InsertObservation.name()), true);
    }

    @Test
    public void testGetObservationNormalEncoding() throws OwsExceptionReport, XmlException {
        XmlObject getObsResponse = sendGetObservation1RequestViaPox(NETWORK_OFFERING, OmConstants.CONTENT_TYPE_OM.toString(), null,
                ImmutableList.of(STATION_ASSET.getAssetId()), ImmutableList.of(OBS_PROP),null).asXmlObject();
        assertNotNull(getObsResponse);
        assertThat(getObsResponse, is(instanceOf(ObservationCollectionDocument.class)));
        ObservationCollectionDocument obsCollectionDoc = (ObservationCollectionDocument) getObsResponse;
        ObservationCollectionType obsCollection = obsCollectionDoc.getObservationCollection();
        assertThat(obsCollection.getMemberArray().length, is(1));
        ObservationPropertyType observationProperty = obsCollection.getMemberArray()[0];
        ObservationType observation = observationProperty.getObservation();
        assertThat(observation, notNullValue());
        XmlObject result = observation.getResult();
        assertThat(result, notNullValue());

        //TODO verify
        net.opengis.swe.x101.DataArrayDocument swe101DataArray = castXmlAnyToClass(result, net.opengis.swe.x101.DataArrayDocument.class);
    }

    @Test
    public void testGetObservationIoosSweEncoding() throws OwsExceptionReport, XmlException {
        XmlObject getObsResponse = sendGetObservation1RequestViaPox(NETWORK_OFFERING, IoosSosConstants.OM_PROFILE_M10,
                null, ImmutableList.of(STATION_ASSET.getAssetId()), ImmutableList.of(OBS_PROP),null).asXmlObject();
        assertNotNull(getObsResponse);
        assertThat(getObsResponse, is(instanceOf(ObservationCollectionDocument.class)));
        ObservationCollectionDocument obsCollectionDoc = (ObservationCollectionDocument) getObsResponse;
        ObservationCollectionType obsCollection = obsCollectionDoc.getObservationCollection();

        //TODO verify
        BoundingShapeType boundedBy = obsCollection.getBoundedBy();

        //TODO verify
        LocationPropertyType location = obsCollection.getLocation();

        assertThat(obsCollection.getMemberArray().length, is(1));
        ObservationPropertyType observationProperty = obsCollection.getMemberArray()[0];
        ObservationType observation = observationProperty.getObservation();
        assertThat(observation, notNullValue());

        //TODO verify
        FeaturePropertyType featureOfInterest = observation.getFeatureOfInterest();
        AbstractFeatureType feature = featureOfInterest.getFeature();

        //TODO verify
        ProcessPropertyType procedure = observation.getProcedure();
        AbstractProcessType process = procedure.getProcess();

        XmlObject result = observation.getResult();
        assertThat(result, notNullValue());

        //TODO verify
        DataRecordDocument swe2DataRecordDocument = castXmlAnyToClass(result, DataRecordDocument.class);
        DataRecordType swe2DataRecord = swe2DataRecordDocument.getDataRecord();

        //observationRecord definition
        assertThat(swe2DataRecord.getDefinition(), is(IoosSweConstants.OBSERVATION_RECORD_DEF));

        Field[] fields = swe2DataRecord.getFieldArray();
        Field stationsField = fields[0];
        assertThat(stationsField.getName(), is(IoosSweConstants.STATIONS));

        AbstractDataComponentType stationsAbstractDataComponent = stationsField.getAbstractDataComponent();
        assertThat(stationsAbstractDataComponent, is(instanceOf(DataRecordType.class)));
        DataRecordType stationsDataRecord = (DataRecordType) stationsAbstractDataComponent;
        assertThat(stationsDataRecord.getDefinition(), is(IoosSweConstants.STATIONS_DEF));
    }

    @Test
    public void testGetObservationIoosNetCDFEncoding() throws OwsExceptionReport, XmlException, IOException {
        InputStream getObsResponse = sendGetObservation1RequestViaPox(NETWORK_OFFERING, IoosNetcdfEncoder.CONTENT_TYPE_NETCDF.toString(),
                null, ImmutableList.of(STATION_ASSET.getAssetId()), ImmutableList.of(OBS_PROP),null).asInputStream();
        File tempNetcdfFile = File.createTempFile("i52n-sos-netcdf-test", ".nc");
        FileOutputStream fileOutputStream = new FileOutputStream(tempNetcdfFile);
        IOUtils.copy(getObsResponse, fileOutputStream);
        getObsResponse.close();
        fileOutputStream.close();

        //TODO FIX THIS "Error getting sensor description for urn:ioos:station:unknown:fake1"
        NetcdfDataset netcdfDataset = NetcdfDataset.openDataset(tempNetcdfFile.getAbsolutePath());
        assertThat(netcdfDataset, notNullValue());
    }

    @Test
    //TODO only works in SOS 2.0 (URL format), add mime types to WMLEncoders for SOS 1.0? (WaterMLConstants.WML_CONTENT_TYPE)
    public void testGetObservationWaterMLEncoding() throws OwsExceptionReport, XmlException, IOException {
        XmlObject getObsResponse = sendGetObservation2RequestViaPox(NETWORK_OFFERING, WaterMLConstants.NS_WML_20,
                null, ImmutableList.of(STATION_ASSET.getAssetId()), ImmutableList.of(OBS_PROP),null).asXmlObject();
        assertThat(getObsResponse, notNullValue());
        assertThat(getObsResponse, is(not(instanceOf(ExceptionReportDocument.class))));
        //TODO check this?
    }

    @Test
    //TODO only works in SOS 2.0 (URL format), add mime types to WMLEncoders for SOS 1.0? (WaterMLConstants.WML_DR_CONTENT_TYPE)
    public void testGetObservationWaterMLDomainRangeEncoding() throws OwsExceptionReport, XmlException, IOException {
        XmlObject getObsResponse = sendGetObservation2RequestViaPox(NETWORK_OFFERING, WaterMLConstants.NS_WML_20_DR, null,
                ImmutableList.of(STATION_ASSET.getAssetId()), ImmutableList.of(OBS_PROP),null).asXmlObject();
        assertThat(getObsResponse, notNullValue());
        assertThat(getObsResponse, is(not(instanceOf(ExceptionReportDocument.class))));
        //TODO check this?
    }

    @Test
    @Ignore
    //TODO json only works if the Accept header is set to application/json
    public void testGetObservationGeoJSONEncoding() throws OwsExceptionReport, XmlException, IOException {
        InputStream getObsResponse = sendGetObservation1RequestViaPox(NETWORK_OFFERING,
                MediaTypes.APPLICATION_JSON.toString(), MediaTypes.APPLICATION_JSON.toString(), 
                ImmutableList.of(STATION_ASSET.getAssetId()), ImmutableList.of(OBS_PROP),null).asInputStream();
        assertThat(getObsResponse, notNullValue());
        Map jsonMap = new ObjectMapper().readValue(getObsResponse, Map.class);
        getObsResponse.close();
        assertThat(jsonMap, notNullValue());
        //TODO check this?
    }
}
