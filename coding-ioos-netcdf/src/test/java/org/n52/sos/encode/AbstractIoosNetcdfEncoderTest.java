package org.n52.sos.encode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.Formatter;
import java.util.List;

import org.joda.time.DateTime;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.n52.sos.ds.hibernate.GetObservationDAO;
import org.n52.sos.ds.hibernate.H2Configuration;
import org.n52.sos.ds.hibernate.HibernateTestCase;
import org.n52.sos.ds.hibernate.testdata.IoosHibernateTestDataManager;
import org.n52.sos.ds.hibernate.util.TemporalRestrictions;
import org.n52.sos.ioos.asset.AbstractAsset;
import org.n52.sos.ioos.asset.SensorAsset;
import org.n52.sos.ogc.filter.FilterConstants.TimeOperator;
import org.n52.sos.ogc.filter.TemporalFilter;
import org.n52.sos.ogc.gml.time.TimeInstant;
import org.n52.sos.ogc.ows.OwsExceptionReport;
import org.n52.sos.ogc.sos.Sos1Constants;
import org.n52.sos.ogc.sos.SosConstants;
import org.n52.sos.request.GetObservationRequest;
import org.n52.sos.response.GetObservationResponse;
import org.n52.sos.service.Configurator;
import org.n52.sos.util.http.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ucar.ma2.Array;
import ucar.ma2.ArrayDouble;
import ucar.nc2.NetcdfFile;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.CoordinateSystem;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDataset.Enhance;
import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.ft.FeatureDatasetFactoryManager;
import ucar.nc2.ft.FeatureDatasetPoint;

import com.axiomalaska.ioos.sos.IoosSosUtil;
import com.axiomalaska.phenomena.Phenomena;
import com.axiomalaska.phenomena.Phenomenon;
import com.axiomalaska.phenomena.UnitCreationException;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public abstract class AbstractIoosNetcdfEncoderTest extends HibernateTestCase {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractIoosNetcdfEncoderTest.class);

    private MediaType responseFormat;
    
    protected AbstractIoosNetcdfEncoderTest(MediaType responseFormat) {
        this.responseFormat = responseFormat;
    }
    
    @BeforeClass
    public static void setUpTestData() throws OwsExceptionReport {
        assertFalse(IoosHibernateTestDataManager.hasTestData());
        IoosHibernateTestDataManager.insertTestData();        
    }

    @AfterClass
    public static void tearDownTestData() throws OwsExceptionReport {
        IoosHibernateTestDataManager.removeTestData();
        H2Configuration.recreate();
    }

    @Test
    public void testPoint() throws OwsExceptionReport, UnitCreationException, IOException{
        for (File netcdf : makeNetcdfRequest(Phenomena.instance().AIR_TEMPERATURE, true)) {
            FeatureDataset featureDataset = verifyNetcdfFile(netcdf, FeatureType.STATION);
            NetcdfFile netcdfFile = featureDataset.getNetcdfFile();
            assertEquals(2, netcdfFile.getDimensions().size());
        }
    }    

    @Test
    public void testProfile() throws OwsExceptionReport, UnitCreationException, IOException{
        for (File netcdf : makeNetcdfRequest(Phenomena.instance().SEA_WATER_TEMPERATURE, true)) {
            FeatureDataset featureDataset = verifyNetcdfFile(netcdf, FeatureType.STATION_PROFILE);
            NetcdfFile netcdfFile = featureDataset.getNetcdfFile();
            assertEquals(3, netcdfFile.getDimensions().size());
            verifyHeightAxis(netcdfFile, 20, -95.0, 0.0);
        }
    }

    @Test
    public void testTimeSeries() throws OwsExceptionReport, UnitCreationException, IOException{
        for (File netcdf : makeNetcdfRequest(Phenomena.instance().AIR_TEMPERATURE)) {
            FeatureDataset featureDataset = verifyNetcdfFile(netcdf, FeatureType.STATION);
            NetcdfFile netcdfFile = featureDataset.getNetcdfFile();
            assertEquals(2, netcdfFile.getDimensions().size());
        }
    }

    @Test
    public void testTimeSeriesProfile() throws OwsExceptionReport, UnitCreationException, IOException{
        for (File netcdf : makeNetcdfRequest(Phenomena.instance().SEA_WATER_TEMPERATURE)) {
            FeatureDataset featureDataset = verifyNetcdfFile(netcdf, FeatureType.STATION_PROFILE);
            NetcdfFile netcdfFile = featureDataset.getNetcdfFile();
            assertEquals(3, netcdfFile.getDimensions().size());
            verifyHeightAxis(netcdfFile, 20, -95.0, 0.0);            
        }
    }

    private List<File> makeNetcdfRequest(Phenomenon phenomenon) throws OwsExceptionReport,
        FileNotFoundException, IOException {
        return makeNetcdfRequest(phenomenon, false);
    }

    protected abstract List<File> getNetcdfFiles(GetObservationResponse response) throws OwsExceptionReport, FileNotFoundException, IOException;

    private List<File> makeNetcdfRequest(Phenomenon phenomenon, boolean latest) throws OwsExceptionReport,
            FileNotFoundException, IOException {       
        return makeNetcdfRequest(Lists.newArrayList(makeSensor("1", phenomenon)),
                Lists.newArrayList(phenomenon), latest);
    }

    protected List<File> makeNetcdfRequest(Collection<? extends AbstractAsset> assets, Collection<Phenomenon> phenomena,
            boolean latest) throws OwsExceptionReport, FileNotFoundException, IOException {
        GetObservationRequest req = new GetObservationRequest();
        req.setService(SosConstants.SOS);
        req.setVersion(Sos1Constants.SERVICEVERSION);
        req.setOfferings(Lists.newArrayList(IoosHibernateTestDataManager.NETWORK_ALL.getAssetId()));

        //procedures
        List<String> procedures = Lists.newArrayList();
        for (AbstractAsset asset : assets) {
            procedures.add(asset.getAssetId());
        }
        req.setProcedures(procedures);

        //phenomena
        List<String> obsProps = Lists.newArrayList();
        for (Phenomenon phenomenon : phenomena) {
            obsProps.add(phenomenon.getId());
        }
        req.setObservedProperties(obsProps);

        if (latest) {
            //FIXME cant just ask for latest because only one profile depth will be returned. should prob be resolved...
            DateTime maxTime = null;
            for (String procedure : procedures) {
                DateTime procedureMaxTime = Configurator.getInstance().getCache().getMaxPhenomenonTimeForProcedure(procedure);
                if (maxTime == null || procedureMaxTime.isAfter(maxTime)) {
                    maxTime = procedureMaxTime;
                }
            }
            req.setTemporalFilters(Lists.newArrayList(new TemporalFilter(TimeOperator.TM_Equals,
                    new TimeInstant(maxTime),
                    TemporalRestrictions.PHENOMENON_TIME_VALUE_REFERENCE)));
        }
        req.setResponseFormat(responseFormat.toString());
        GetObservationResponse resp = new GetObservationDAO().getObservation(req);

        List<File> netcdfFiles = getNetcdfFiles(resp);

        //check number of files
        assertEquals(1, netcdfFiles.size());

        return netcdfFiles;
    }

    private FeatureDataset verifyNetcdfFile(File netcdfFile, FeatureType featureType) throws IOException {        
        FeatureDataset featureDataset = null;
        Formatter errlog = new Formatter();
        try {
            featureDataset = FeatureDatasetFactoryManager.open(FeatureType.ANY_POINT, netcdfFile.getAbsolutePath(), null, errlog);
        } catch (IOException e) {
            LOGGER.error(errlog.toString());
            throw e;
        }
        if (!errlog.toString().trim().isEmpty()) {
            fail(errlog.toString());
        }
        assertTrue(featureDataset.getFeatureType().equals(featureType));
        assertThat(featureDataset, instanceOf(FeatureDatasetPoint.class));
        
        //verify that axes is increasing
        NetcdfDataset netcdfDataset = NetcdfDataset.wrap(featureDataset.getNetcdfFile(), Sets.newHashSet(Enhance.CoordSystems));
        List<CoordinateSystem> coordinateSystems = netcdfDataset.getCoordinateSystems();
//        assertEquals(1, coordinateSystems.size());
        for (CoordinateSystem coordinateSystem : coordinateSystems) {
            for (CoordinateAxis axis : coordinateSystem.getCoordinateAxes()) {
                verifyAxisIsMonotonic(axis);
            }
        }
        
        return featureDataset;
    }

    private void verifyAxisIsMonotonic(CoordinateAxis axis) throws IOException{
        Array array = axis.read();
        if (array instanceof ArrayDouble) {
            ArrayDouble arrayDouble = (ArrayDouble) array;
            Double prevVal = null;
            int direction = 0;
            while (arrayDouble.hasNext()){
                double val = arrayDouble.nextDouble();
                if (prevVal != null) {
                    if (direction == 0) {
                        if (prevVal < val) {
                            direction = 1;
                        } else {
                            direction = -1;
                        }
                    }
                    if (direction == 1) {
                        assertThat(val, greaterThan(prevVal));
                    } else {
                        assertThat(val, lessThan(prevVal));                        
                    }
                }
                prevVal = val;
            }
        }
    }
    
    private void verifyHeightAxis(NetcdfFile netcdfFile, int size, double minHeight, double maxHeight) throws IOException {
        NetcdfDataset netcdfDataset = NetcdfDataset.wrap(netcdfFile, Sets.newHashSet(Enhance.CoordSystems));
        assertNotNull(netcdfDataset);
        List<CoordinateSystem> coordinateSystems = netcdfDataset.getCoordinateSystems();
//        assertEquals(1, coordinateSystems.size());
        boolean heightAxisFound = false;
        for (CoordinateSystem coordinateSystem : coordinateSystems) {
            CoordinateAxis heightAxis = coordinateSystem.getHeightAxis();
            if (heightAxis != null) {
                heightAxisFound = true;
                assertEquals(size, heightAxis.getSize());        
                assertEquals(maxHeight, heightAxis.getMaxValue(), 0.0);
                assertEquals(minHeight, heightAxis.getMinValue(), 0.0);
            }
        }
        assertTrue("Height axis not found in any coordinate system", heightAxisFound);
    }

    public SensorAsset makeSensor(String id, Phenomenon phenomenon) {
        return new SensorAsset(IoosHibernateTestDataManager.TEST, id,
                IoosSosUtil.getNameFromUri(phenomenon.getId()));
    }
}
