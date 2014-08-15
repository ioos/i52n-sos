package org.n52.sos.encode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Formatter;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.joda.time.DateTime;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.n52.sos.ds.hibernate.GetObservationDAO;
import org.n52.sos.ds.hibernate.H2Configuration;
import org.n52.sos.ds.hibernate.HibernateTestCase;
import org.n52.sos.ds.hibernate.testdata.IoosHibernateTestDataManager;
import org.n52.sos.ds.hibernate.util.TemporalRestrictions;
import org.n52.sos.ioos.asset.SensorAsset;
import org.n52.sos.ogc.filter.FilterConstants.TimeOperator;
import org.n52.sos.ogc.filter.TemporalFilter;
import org.n52.sos.ogc.gml.time.TimeInstant;
import org.n52.sos.ogc.ows.OwsExceptionReport;
import org.n52.sos.ogc.sos.Sos1Constants;
import org.n52.sos.ogc.sos.SosConstants;
import org.n52.sos.request.GetObservationRequest;
import org.n52.sos.response.BinaryAttachmentResponse;
import org.n52.sos.response.GetObservationResponse;
import org.n52.sos.service.Configurator;
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

public class IoosNetcdfEncoderTest extends HibernateTestCase {
    private static final Logger LOGGER = LoggerFactory.getLogger(IoosNetcdfEncoderTest.class);

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
        List<File> netcdfFiles = makeNetcdfRequest(Phenomena.instance().AIR_TEMPERATURE, true); 
        for (File unzippedNetcdf : netcdfFiles) {
            FeatureDataset featureDataset = verifyNetcdfFile(unzippedNetcdf, FeatureType.STATION);
            NetcdfFile netcdfFile = featureDataset.getNetcdfFile();
            assertEquals(2, netcdfFile.getDimensions().size());
        }
    }    

    @Test
    public void testProfile() throws OwsExceptionReport, UnitCreationException, IOException{
        List<File> netcdfFiles = makeNetcdfRequest(Phenomena.instance().SEA_WATER_TEMPERATURE, true);
        for (File unzippedNetcdf : netcdfFiles) {
            FeatureDataset featureDataset = verifyNetcdfFile(unzippedNetcdf, FeatureType.STATION_PROFILE);
            NetcdfFile netcdfFile = featureDataset.getNetcdfFile();
            assertEquals(3, netcdfFile.getDimensions().size());
            verifyHeightAxis(netcdfFile, 20, -95.0, 0.0);
        }
    }
    
    @Test
    public void testTimeSeries() throws OwsExceptionReport, UnitCreationException, IOException{
        List<File> netcdfFiles = makeNetcdfRequest(Phenomena.instance().AIR_TEMPERATURE);
        for (File unzippedNetcdf : netcdfFiles) {
            FeatureDataset featureDataset = verifyNetcdfFile(unzippedNetcdf, FeatureType.STATION);
            NetcdfFile netcdfFile = featureDataset.getNetcdfFile();
            assertEquals(2, netcdfFile.getDimensions().size());
        }
    }

    @Test
    public void testTimeSeriesProfile() throws OwsExceptionReport, UnitCreationException, IOException{
        List<File> netcdfFiles = makeNetcdfRequest(Phenomena.instance().SEA_WATER_TEMPERATURE);
        for (File unzippedNetcdf : netcdfFiles) {
            FeatureDataset featureDataset = verifyNetcdfFile(unzippedNetcdf, FeatureType.STATION_PROFILE);
            NetcdfFile netcdfFile = featureDataset.getNetcdfFile();
            assertEquals(3, netcdfFile.getDimensions().size());
            verifyHeightAxis(netcdfFile, 20, -95.0, 0.0);            
        }
    }

    private List<File> makeNetcdfRequest(Phenomenon phenomenon) throws OwsExceptionReport,
        FileNotFoundException, IOException {
        return makeNetcdfRequest(phenomenon, false);
    }
    
    private List<File> makeNetcdfRequest(Phenomenon phenomenon, boolean latest) throws OwsExceptionReport,
            FileNotFoundException, IOException {
        GetObservationRequest req = new GetObservationRequest();
        req.setService(SosConstants.SOS);
        req.setVersion(Sos1Constants.SERVICEVERSION);
        req.setOfferings(Lists.newArrayList(IoosHibernateTestDataManager.NETWORK_ALL.getAssetId()));
        String procedure = new SensorAsset(IoosHibernateTestDataManager.TEST, "1",
                IoosSosUtil.getNameFromUri(phenomenon.getId())).getAssetId();
        req.setProcedures(Lists.newArrayList(procedure));
        req.setObservedProperties(Lists.newArrayList(phenomenon.getId()));
        if (latest) {
            //FIXME cant just ask for latest because only one profile depth will be returned. should prob be resolved...
            DateTime maxTime = Configurator.getInstance().getCache().getMaxPhenomenonTimeForProcedure(procedure);
            req.setTemporalFilters(Lists.newArrayList(new TemporalFilter(TimeOperator.TM_Equals,
                    new TimeInstant(maxTime),
                    TemporalRestrictions.PHENOMENON_TIME_VALUE_REFERENCE)));
        }
        req.setResponseFormat(IoosNetcdfEncoder.CONTENT_TYPE_NETCDF_ZIP.toString());
        GetObservationResponse resp = new GetObservationDAO().getObservation(req);
        BinaryAttachmentResponse response = new IoosNetcdfEncoder().encode(resp);
        byte[] zipBytes = response.getBytes();
        List<File> unzippedNetcdfs = Lists.newArrayList();        
        ZipInputStream zipStream = new ZipInputStream(new ByteArrayInputStream(zipBytes));        
        ZipEntry zipEntry;
        File targetDir = new File("target/netcdf-encoder-test/");
        targetDir.mkdirs();
        while((zipEntry = zipStream.getNextEntry()) != null) {
            File extractedFile = new File(targetDir, zipEntry.getName());
            unzippedNetcdfs.add(extractedFile);
            FileOutputStream fos = new FileOutputStream(extractedFile);
            byte[] byteBuff = new byte[4096];
            int bytesRead = 0;
            while ((bytesRead = zipStream.read(byteBuff)) != -1){
                fos.write(byteBuff, 0, bytesRead);
            }
            fos.close();
            zipStream.closeEntry();
        }
        zipStream.close();
        return unzippedNetcdfs;
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
}
