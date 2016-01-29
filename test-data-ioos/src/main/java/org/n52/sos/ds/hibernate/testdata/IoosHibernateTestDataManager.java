/**
 * Copyright (C) 2013
 * by 52 North Initiative for Geospatial Open Source Software GmbH
 *
 * Contact: Andreas Wytzisk
 * 52 North Initiative for Geospatial Open Source Software GmbH
 * Martin-Luther-King-Weg 24
 * 48155 Muenster, Germany
 * info@52north.org
 *
 * This program is free software; you can redistribute and/or modify it under
 * the terms of the GNU General Public License version 2 as published by the
 * Free Software Foundation.
 *
 * This program is distributed WITHOUT ANY WARRANTY; even without the implied
 * WARRANTY OF MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program (see gnu-gpl v2.txt). If not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA or
 * visit the Free Software Foundation web page, http://www.fsf.org.
 */
package org.n52.sos.ds.hibernate.testdata;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.n52.sos.cache.ContentCache;
import org.n52.sos.ds.hibernate.HibernateSessionHolder;
import org.n52.sos.ds.hibernate.dao.FeatureOfInterestDAO;
import org.n52.sos.ds.hibernate.dao.FeatureOfInterestTypeDAO;
import org.n52.sos.ds.hibernate.dao.ObservablePropertyDAO;
import org.n52.sos.ds.hibernate.dao.ObservationConstellationDAO;
import org.n52.sos.ds.hibernate.dao.ObservationTypeDAO;
import org.n52.sos.ds.hibernate.dao.OfferingDAO;
import org.n52.sos.ds.hibernate.dao.ProcedureDAO;
import org.n52.sos.ds.hibernate.dao.ProcedureDescriptionFormatDAO;
import org.n52.sos.ds.hibernate.dao.ValidProcedureTimeDAO;
import org.n52.sos.ds.hibernate.dao.series.SeriesObservationDAO;
import org.n52.sos.ds.hibernate.entities.Codespace;
import org.n52.sos.ds.hibernate.entities.FeatureOfInterest;
import org.n52.sos.ds.hibernate.entities.FeatureOfInterestType;
import org.n52.sos.ds.hibernate.entities.ObservableProperty;
import org.n52.sos.ds.hibernate.entities.ObservationConstellation;
import org.n52.sos.ds.hibernate.entities.ObservationType;
import org.n52.sos.ds.hibernate.entities.Offering;
import org.n52.sos.ds.hibernate.entities.Procedure;
import org.n52.sos.ds.hibernate.entities.ProcedureDescriptionFormat;
import org.n52.sos.ds.hibernate.entities.RelatedFeature;
import org.n52.sos.ds.hibernate.entities.Unit;
import org.n52.sos.exception.ows.NoApplicableCodeException;
import org.n52.sos.ioos.asset.AssetConstants;
import org.n52.sos.ioos.asset.NetworkAsset;
import org.n52.sos.ioos.asset.SensorAsset;
import org.n52.sos.ioos.asset.StationAsset;
import org.n52.sos.ogc.gml.CodeWithAuthority;
import org.n52.sos.ogc.gml.time.TimeInstant;
import org.n52.sos.ogc.om.OmConstants;
import org.n52.sos.ogc.om.OmObservableProperty;
import org.n52.sos.ogc.om.OmObservation;
import org.n52.sos.ogc.om.SingleObservationValue;
import org.n52.sos.ogc.om.features.SfConstants;
import org.n52.sos.ogc.om.features.samplingFeatures.SamplingFeature;
import org.n52.sos.ogc.om.values.QuantityValue;
import org.n52.sos.ogc.ows.OwsExceptionReport;
import org.n52.sos.service.Configurator;
import org.n52.sos.util.CollectionHelper;

import ucar.nc2.constants.CF;

import com.axiomalaska.ioos.sos.GeomHelper;
import com.axiomalaska.ioos.sos.IoosSosConstants;
import com.axiomalaska.ioos.sos.IoosSosUtil;
import com.axiomalaska.ioos.sos.exception.UnsupportedGeometryTypeException;
import com.axiomalaska.phenomena.Phenomena;
import com.axiomalaska.phenomena.Phenomenon;
import com.axiomalaska.phenomena.UnitCreationException;
import com.google.common.collect.Maps;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

public class IoosHibernateTestDataManager{
    public static final String TEST = "test";
    private static final String TEST_NETWORK_PATTERN = AssetConstants.URN_PREFIX + ":network:" + TEST + ":%";    
    private static final String TEST_STATION_PATTERN = AssetConstants.URN_PREFIX + ":station:" + TEST + ":%";
    private static final String TEST_SENSOR_PATTERN = AssetConstants.URN_PREFIX + ":sensor:" + TEST + ":%";
    public static final NetworkAsset NETWORK_ALL = new NetworkAsset(TEST, "all");
    public static final int NUM_STATIONS = 10;
    private static final int NUM_OBS_PER_SENSOR = 20;
    private static final int NUM_HEIGHTS_PER_PROFILE = 20;
    private static final List<String> PLATFORM_TYPES = Arrays.asList("buoy","moored_buoy","tower");

    private static final HibernateSessionHolder sessionHolder = new HibernateSessionHolder();
    private static final ProcedureDescriptionFormatDAO procedureDescriptionFormatDAO = new ProcedureDescriptionFormatDAO();
    private static final ProcedureDAO procedureDAO = new ProcedureDAO();
    private static final OfferingDAO offeringDAO = new OfferingDAO();
    private static final ObservationTypeDAO observationTypeDAO = new ObservationTypeDAO();
    private static final FeatureOfInterestTypeDAO featureOfInterestTypeDAO = new FeatureOfInterestTypeDAO();
    private static final ObservablePropertyDAO observablePropertyDAO = new ObservablePropertyDAO();
    private static final ValidProcedureTimeDAO validProcedureTimeDAO = new ValidProcedureTimeDAO();
    private static final ObservationConstellationDAO obsConstDAO = new ObservationConstellationDAO();
    private static final FeatureOfInterestDAO featureOfInterestDAO = new FeatureOfInterestDAO();
    private static final SeriesObservationDAO seriesObservationDAO = new SeriesObservationDAO();
    
    public static void insertTestData() throws OwsExceptionReport{
        if (hasTestData()){
            throw new NoApplicableCodeException().withMessage("Test data already inserted!");
        }

        Map<String,Codespace> codespaceCache = Maps.newHashMap();
        Map<String,Unit> unitCache = Maps.newHashMap();
        
        Session session = sessionHolder.getSession();
        Transaction tx = session.beginTransaction();

        final ObservationType measurementObsType = observationTypeDAO
                .getOrInsertObservationType(OmConstants.OBS_TYPE_MEASUREMENT, session);

        final FeatureOfInterestType samplingPointFoiType = featureOfInterestTypeDAO
                .getOrInsertFeatureOfInterestType(SfConstants.SAMPLING_FEAT_TYPE_SF_SAMPLING_POINT, session);
                
        final Offering hNetworkOffering = offeringDAO.getAndUpdateOrInsertNewOffering(NETWORK_ALL.getAssetId(),
                NETWORK_ALL.getAssetId(), new ArrayList<RelatedFeature>(), CollectionHelper.list(measurementObsType),
                CollectionHelper.list(samplingPointFoiType), session);        
        
        Phenomenon airTempPhen = null;
        Phenomenon waterTempPhen = null;
        try {
            airTempPhen = Phenomena.instance().AIR_TEMPERATURE;
            waterTempPhen = Phenomena.instance().SEA_WATER_TEMPERATURE;
        } catch (UnitCreationException e) {
            throw new NoApplicableCodeException().causedBy(e);
        }
        
        final ObservableProperty hAirTempObsProp = observablePropertyDAO.getOrInsertObservableProperty(
                CollectionHelper.list(new OmObservableProperty(airTempPhen.getId())), session).get(0);
        final String airTempUnit = IoosSosConstants.UDUNITS_URN_PREFIX + airTempPhen.getUnit().getSymbol();        

        final ObservableProperty hWaterTempObsProp = observablePropertyDAO.getOrInsertObservableProperty(
                CollectionHelper.list(new OmObservableProperty(waterTempPhen.getId())), session).get(0);
        final String waterTempUnit = IoosSosConstants.UDUNITS_URN_PREFIX + waterTempPhen.getUnit().getSymbol();

        final ProcedureDescriptionFormat pdf = procedureDescriptionFormatDAO
                .getOrInsertProcedureDescriptionFormat(IoosSosConstants.SML_PROFILE_M10, session);
        
        //network procedure
        final String networkAllSml = IoosTestDataSmlGenerator.createNetworkSensorMl(NETWORK_ALL.getAssetId(),
                "All inclusive test sensor network", "Test data network", "Test data network procedure", TEST);
        final Procedure networkProcedure = insertProcedure(NETWORK_ALL.getAssetId(), pdf,
                new ArrayList<String>(), networkAllSml, session);
        
        //stations
        for (int i = 0; i < NUM_STATIONS; i++) {
            Collections.shuffle(PLATFORM_TYPES);
            final StationAsset station = new StationAsset(TEST, Integer.toString(i));
            final double stationLat = randomLat();
            final double stationLng = randomLng();
            final String stationSml = IoosTestDataSmlGenerator.createStationSensorMl(station.getAssetId(),
                    "Test station " + i, "Test station " + i, "Station number " + i + " for testing", PLATFORM_TYPES.get(0),
                    "gov_federal", "IOOS", TEST, "IOOS",
                    "Station " + i + " Quality Page", "http://somesite.gov/qc/station" + i,
                    stationLng, stationLat);
            final Procedure stationProcedure = insertProcedure(station.getAssetId(), pdf, CollectionHelper.list(NETWORK_ALL.getAssetId()),
                    stationSml, session);
                        
            final Offering hStationOffering = offeringDAO.getAndUpdateOrInsertNewOffering(station.getAssetId(),
                    station.getAssetId(), new ArrayList<RelatedFeature>(), CollectionHelper.list(measurementObsType),
                    CollectionHelper.list(samplingPointFoiType), session);                    

            SamplingFeature stationFeature = new SamplingFeature(new CodeWithAuthority(station.getAssetId()));
            double stationHeight = Math.round(Math.random() * 30.0) / 10.0;
            Point stationPoint = GeomHelper.createLatLngPoint(stationLat, stationLng, stationHeight);
            stationFeature.setGeometry(stationPoint);
            FeatureOfInterest hStationFeature = featureOfInterestDAO.checkOrInsertFeatureOfInterest(stationFeature, session);

            //sensors
            
            //air temp timeseries
            createSensor(i, CF.FeatureType.timeSeries, hNetworkOffering, networkProcedure, hStationOffering, stationProcedure, station,
                    hStationFeature, stationPoint, airTempPhen, hAirTempObsProp, airTempUnit, measurementObsType, pdf,
                    codespaceCache, unitCache, session);
            
            //water temp timeseriesprofile
            createSensor(i, CF.FeatureType.timeSeriesProfile, hNetworkOffering, networkProcedure, hStationOffering, stationProcedure, station,
                    hStationFeature, stationPoint, waterTempPhen, hWaterTempObsProp, waterTempUnit, measurementObsType, pdf,
                    codespaceCache, unitCache, session);            
        }

        tx.commit();
        sessionHolder.returnSession(session);
        Configurator.getInstance().getCacheController().update();
    }

    private static void createSensor(int i, CF.FeatureType featureType, Offering hNetworkOffering, Procedure networkProcedure,
            Offering hStationOffering, Procedure stationProcedure, StationAsset station, FeatureOfInterest hStationFeature,
            Point stationPoint, Phenomenon phen, ObservableProperty obsProp, String unit, ObservationType obsType, ProcedureDescriptionFormat pdf,
            Map<String,Codespace> codespaceCache, Map<String,Unit> unitCache, Session session) throws OwsExceptionReport {
        String phenShortName = getPhenomenonShortId(phen.getId());
        final SimpleIo airTempSimpleIo = new SimpleIo(phenShortName, phen.getId(), unit);
        final SensorAsset sensor = new SensorAsset(TEST, Integer.toString(i), phenShortName);
        final String sensorSml = IoosTestDataSmlGenerator.createSensorSensorMl(sensor.getAssetId(),
                "Test station " + i + " " + phen.getName() + " sensor",
                "Test station " + i + " " + phen.getName() + " sensor",
                "Station number " + i + " " + phen.getName() + " sensor",
                CollectionHelper.list(airTempSimpleIo));
        final Procedure sensorProcedure = insertProcedure(sensor.getAssetId(), pdf, CollectionHelper.list(station.getAssetId()),
                sensorSml, session);

        Set<ObservationConstellation> obsConsts = new HashSet<ObservationConstellation>();
        Set<ObservationConstellation> sensorObsConsts = new HashSet<ObservationConstellation>();
        
        // when offering and procedure are same hiddenChild is false, otherwise true
        ObservationConstellation sensorForNetworkOffering = obsConstDAO.checkOrInsertObservationConstellation(
                sensorProcedure, obsProp, hNetworkOffering, true, session);            
        obsConsts.add(sensorForNetworkOffering);
        sensorObsConsts.add(sensorForNetworkOffering);            
                
        obsConsts.add(obsConstDAO.checkOrInsertObservationConstellation(stationProcedure,
                obsProp, hNetworkOffering, true, session));            
        obsConsts.add(obsConstDAO.checkOrInsertObservationConstellation(networkProcedure,
                obsProp, hNetworkOffering, false, session));

        ObservationConstellation sensorForStationOffering = obsConstDAO.checkOrInsertObservationConstellation(
                sensorProcedure, obsProp, hStationOffering, true, session);
        obsConsts.add(sensorForStationOffering);
        sensorObsConsts.add(sensorForStationOffering);
        
        obsConsts.add(obsConstDAO.checkOrInsertObservationConstellation(stationProcedure,
                obsProp, hStationOffering, false, session));

        //set measurement types on sesnor obs consts
        for (ObservationConstellation obsConst : sensorObsConsts) {
            obsConst.setObservationType(obsType);
            session.save(obsConst);
        }
        
        //add values
        DateTime obsEndTime = new DateTime(DateTimeZone.UTC).withMillisOfSecond(0).withSecondOfMinute(0).withMinuteOfHour(0);
        if (featureType.equals(CF.FeatureType.timeSeriesProfile)) {
            for (int h = 0; h < NUM_HEIGHTS_PER_PROFILE; h++) {
                Geometry foiGeom = GeomHelper.createLatLngPoint(stationPoint.getY(), stationPoint.getX(), 0 - 5.0 * h);
                String foiId;
                try {
                    foiId = IoosSosUtil.createObservationFeatureOfInterestId(station, stationPoint, sensor, stationPoint, foiGeom);
                } catch (UnsupportedGeometryTypeException e) {
                    throw new NoApplicableCodeException().causedBy(e);
                }
                SamplingFeature profileFeature = new SamplingFeature(new CodeWithAuthority(foiId));
                profileFeature.setGeometry(foiGeom);
                FeatureOfInterest hProfileFeature = featureOfInterestDAO.checkOrInsertFeatureOfInterest(profileFeature, session);
                insertObservation(sensorObsConsts, obsEndTime, hProfileFeature, unit, codespaceCache, unitCache, session);
            }
        } else {
            //normal time series
            insertObservation(sensorObsConsts, obsEndTime, hStationFeature, unit, codespaceCache, unitCache, session);                 
        }
    }

    private static void insertObservation(Set<ObservationConstellation> obsConsts, DateTime obsEndTime, FeatureOfInterest feature, String unit,
            Map<String,Codespace> codespaceCache, Map<String,Unit> unitCache, Session session) throws OwsExceptionReport{
        for (int j = 0; j < NUM_OBS_PER_SENSOR; j++){
            OmObservation obs = new OmObservation();
            double obsValue = randomInRange(5.0,  32.0, 3);
            DateTime obsTime = obsEndTime.minusHours(j);
            QuantityValue quantityValue = new QuantityValue(obsValue, unit);
            obs.setValue(new SingleObservationValue<Double>(new TimeInstant(obsTime), quantityValue));
            seriesObservationDAO.insertObservationSingleValue(obsConsts, feature, obs, codespaceCache, unitCache, session);
        }
        session.flush();
        session.clear();
    }

    private static Procedure insertProcedure(String identifier, ProcedureDescriptionFormat pdf, Collection<String> parentProcedures,
            String procedureXml, Session session) {
        Procedure procedure = procedureDAO.getOrInsertProcedure(identifier, pdf, parentProcedures, session);
        validProcedureTimeDAO.insertValidProcedureTime(procedure, pdf, procedureXml, new DateTime(DateTimeZone.UTC), session);
        return procedure;
    }
    
    public static boolean hasTestData() throws OwsExceptionReport{
        return !(getCache().getOfferingsForProcedure(NETWORK_ALL.getAssetId()).isEmpty());
    }

    public static void removeTestData() throws OwsExceptionReport{
        if (!hasTestData()) {
            throw new NoApplicableCodeException().withMessage("Test data is not inserted!");
        }
        
        Session session = sessionHolder.getSession();
        Transaction tx = session.beginTransaction();
        
        IoosTestDataDAO.deleteOfferingObservations(NETWORK_ALL.getAssetId(), session);

        IoosTestDataDAO.deleteOfferingObservationConstellations(TEST_NETWORK_PATTERN, session);
        IoosTestDataDAO.deleteOfferingObservationConstellations(TEST_STATION_PATTERN, session);        
        IoosTestDataDAO.deleteOfferingObservationConstellations(TEST_SENSOR_PATTERN, session);

        IoosTestDataDAO.deleteOfferings(TEST_NETWORK_PATTERN, session);
        IoosTestDataDAO.deleteOfferings(TEST_STATION_PATTERN, session);
        IoosTestDataDAO.deleteOfferings(TEST_SENSOR_PATTERN, session);

        IoosTestDataDAO.deleteSeries(TEST_SENSOR_PATTERN, session);

        IoosTestDataDAO.deleteProcedures(TEST_NETWORK_PATTERN, session);
        IoosTestDataDAO.deleteProcedures(TEST_STATION_PATTERN, session);
        IoosTestDataDAO.deleteProcedures(TEST_SENSOR_PATTERN, session);
        
        IoosTestDataDAO.deleteFeatures(TEST_STATION_PATTERN, session);
        
        IoosTestDataDAO.deleteOrphanFeatureOfInterestTypes(session);
        IoosTestDataDAO.deleteOrphanObservableProperties(session);
        IoosTestDataDAO.deleteOrphanObservationType(session);
        IoosTestDataDAO.deleteOrphanProcedureDescriptionFormats(session);
        IoosTestDataDAO.deleteOrphanUnits(session);

        tx.commit();
        sessionHolder.returnSession(session);
        Configurator.getInstance().getCacheController().update();
    }
    
    private static double randomLng(){
        //make test data lngs three digits for easy differentiation from lats
        double lng = randomInRange(100.0, 180.0, 6);
        //return a negative number if lng is even
        if ((int) Math.round(lng) % 2 == 0) {
            return 0 - lng;
        } else {
            return lng;
        }
    }

    private static double randomLat(){
        //stay away from the poles because they often break software
        return randomInRange(-75.0, 75.0, 6);
    }
    
    private static double randomInRange(double min, double max, int decimalPlaces){
        double unroundedValue = min + Math.random() * (max - min);
        double co = Math.pow(10, decimalPlaces);
        return Math.round(unroundedValue * co) / co;        
    }

    private static ContentCache getCache(){
        return Configurator.getInstance().getCache();
    }
    
    private static String getPhenomenonShortId(String phenomenon){
        String[] split = phenomenon.split("/");
        return split[split.length - 1];
    }
}
