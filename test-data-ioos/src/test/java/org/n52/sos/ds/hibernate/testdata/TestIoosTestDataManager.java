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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.junit.Test;
import org.n52.sos.cache.ContentCache;
import org.n52.sos.ds.GetCapabilitiesDAO;
import org.n52.sos.ds.hibernate.DescribeSensorDAO;
import org.n52.sos.ds.hibernate.GetObservationDAO;
import org.n52.sos.ds.hibernate.HibernateSessionHolder;
import org.n52.sos.ds.hibernate.HibernateTestCase;
import org.n52.sos.ds.hibernate.entities.FeatureOfInterest;
import org.n52.sos.ds.hibernate.entities.FeatureOfInterestType;
import org.n52.sos.ds.hibernate.entities.ObservableProperty;
import org.n52.sos.ds.hibernate.entities.ObservationConstellation;
import org.n52.sos.ds.hibernate.entities.ObservationType;
import org.n52.sos.ds.hibernate.entities.Offering;
import org.n52.sos.ds.hibernate.entities.Procedure;
import org.n52.sos.ds.hibernate.entities.ProcedureDescriptionFormat;
import org.n52.sos.ds.hibernate.entities.Unit;
import org.n52.sos.ds.hibernate.entities.ValidProcedureTime;
import org.n52.sos.ds.hibernate.entities.observation.series.Series;
import org.n52.sos.ds.hibernate.entities.observation.series.SeriesObservation;
import org.n52.sos.ds.hibernate.entities.observation.series.full.SeriesNumericObservation;
import org.n52.sos.ioos.asset.StationAsset;
import org.n52.sos.ogc.ows.OwsExceptionReport;
import org.n52.sos.ogc.sos.Sos2Constants;
import org.n52.sos.ogc.sos.SosConstants;
import org.n52.sos.request.DescribeSensorRequest;
import org.n52.sos.request.GetCapabilitiesRequest;
import org.n52.sos.request.GetObservationRequest;
import org.n52.sos.service.Configurator;
import org.n52.sos.util.CollectionHelper;

import com.axiomalaska.ioos.sos.IoosSosConstants;

public class TestIoosTestDataManager extends HibernateTestCase {
    /* FIXTURES */
    private final HibernateSessionHolder sessionHolder = new HibernateSessionHolder();

    @Test
    public void testIoosTestDataManager() throws OwsExceptionReport {
        assertFalse(IoosHibernateTestDataManager.hasTestData());

        IoosHibernateTestDataManager.insertTestData();
        assertTrue(IoosHibernateTestDataManager.hasTestData());
        assertEquals(IoosHibernateTestDataManager.NUM_STATIONS * 3 + 1, getCache().getProcedures().size());                

        //test requests
        testGetCapabilitiesRequest();
        testDescribeSensorRequest();
        testGetObservationRequest();

        //remove test data
        IoosHibernateTestDataManager.removeTestData();
        assertTrue(getCache().getProcedures().isEmpty());
        assertTrue(getCache().getOfferings().isEmpty());
        assertTrue(getCache().getFeaturesOfInterest().isEmpty());

        Session session = sessionHolder.getSession();
        assertTableIsEmpty(FeatureOfInterest.class, session);
        assertTableIsEmpty(FeatureOfInterestType.class, session);
        assertTableIsEmpty(ObservableProperty.class, session);
        assertTableIsEmpty(SeriesObservation.class, session);
        assertTableIsEmpty(SeriesNumericObservation.class, session);
        assertTableIsEmpty(ObservationConstellation.class, session);
        assertTableIsEmpty(ObservationType.class, session);        
        assertTableIsEmpty(Offering.class, session);
        assertTableIsEmpty(Procedure.class, session);
        assertTableIsEmpty(ProcedureDescriptionFormat.class, session);
        assertTableIsEmpty(Series.class, session);
        assertTableIsEmpty(Unit.class, session);
        assertTableIsEmpty(ValidProcedureTime.class, session);
        sessionHolder.returnSession(session);
        
        assertFalse(IoosHibernateTestDataManager.hasTestData());
    }

    private void testGetCapabilitiesRequest(){
        GetCapabilitiesRequest req = new GetCapabilitiesRequest();
        req.setService(SosConstants.SOS);
        req.setVersion(Sos2Constants.SERVICEVERSION);
        try {
            new GetCapabilitiesDAO().getCapabilities(req);
        } catch (OwsExceptionReport e) {
            fail("GetCapabilities failed: " + e.toString());
        }
    }

    private void testDescribeSensorRequest(){
        DescribeSensorRequest req = new DescribeSensorRequest();
        req.setService(SosConstants.SOS);
        req.setVersion(Sos2Constants.SERVICEVERSION);
        req.setProcedure(IoosHibernateTestDataManager.NETWORK_ALL.getAssetId());
        req.setProcedureDescriptionFormat(IoosSosConstants.SML_PROFILE_M10);
        try {
            new DescribeSensorDAO().getSensorDescription(req);
        } catch (OwsExceptionReport e) {
            fail("DescribeSensor failed: " + e.toString());
        }
    }

    private void testGetObservationRequest(){
        GetObservationRequest req = new GetObservationRequest();
        req.setService(SosConstants.SOS);
        req.setVersion(Sos2Constants.SERVICEVERSION);
        String firstStation = new StationAsset(IoosHibernateTestDataManager.TEST, "1").getAssetId();
        req.setOfferings(CollectionHelper.list(IoosHibernateTestDataManager.NETWORK_ALL.getAssetId()));
        req.setProcedures(CollectionHelper.list(firstStation));
        req.setResponseFormat(IoosSosConstants.OM_PROFILE_M10);
        try {
            new GetObservationDAO().getObservation(req);
        } catch (OwsExceptionReport e) {
            fail("GetObservation failed: " + e.toString());
        }
    }

    private ContentCache getCache(){
        return Configurator.getInstance().getCache();
    }
    
    private void assertTableIsEmpty(final Class<?> clazz, final Session session) {
        long count = (Long) session.createCriteria(clazz)
                .setProjection(Projections.rowCount()).uniqueResult();
        assertEquals(clazz.getName() + " contains records",0,count);
    }
}
