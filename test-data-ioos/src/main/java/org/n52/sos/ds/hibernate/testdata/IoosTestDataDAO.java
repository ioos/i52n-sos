package org.n52.sos.ds.hibernate.testdata;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.n52.sos.ds.hibernate.entities.FeatureOfInterest;
import org.n52.sos.ds.hibernate.entities.FeatureOfInterestType;
import org.n52.sos.ds.hibernate.entities.ObservableProperty;
import org.n52.sos.ds.hibernate.entities.series.Series;
import org.n52.sos.ds.hibernate.entities.series.SeriesObservation;
import org.n52.sos.ds.hibernate.entities.ObservationConstellation;
import org.n52.sos.ds.hibernate.entities.series.SeriesObservationInfo;
import org.n52.sos.ds.hibernate.entities.ObservationInfo;
import org.n52.sos.ds.hibernate.entities.ObservationType;
import org.n52.sos.ds.hibernate.entities.Offering;
import org.n52.sos.ds.hibernate.entities.Procedure;
import org.n52.sos.ds.hibernate.entities.ProcedureDescriptionFormat;
import org.n52.sos.ds.hibernate.entities.TFeatureOfInterest;
import org.n52.sos.ds.hibernate.entities.TOffering;
import org.n52.sos.ds.hibernate.entities.TProcedure;
import org.n52.sos.ds.hibernate.entities.Unit;
import org.n52.sos.ds.hibernate.entities.ValidProcedureTime;
import org.n52.sos.ds.hibernate.util.HibernateHelper;

public class IoosTestDataDAO {
    public static void deleteOfferingObservations(final String offeringIdentifier, final Session session) {        
        @SuppressWarnings("unchecked")
        List<SeriesObservation> observations = session.createCriteria(SeriesObservation.class)
                .createAlias("offerings","off")
                .add(Restrictions.eq("off." + Offering.IDENTIFIER, offeringIdentifier))
                .list();
        
        int i = 0;
        for (SeriesObservation observation : observations) {
            session.delete(observation);
            if (i++ % 50 == 0) {
                session.flush();
                session.clear();
            }
        }
        
        session.flush();
        session.clear();
    }

    public static void deleteOfferingObservationConstellations(final String offeringIdentifierPattern, final Session session) {
        @SuppressWarnings("unchecked")
        List<ObservationConstellation> obsConsts = session.createCriteria(ObservationConstellation.class)
                .createAlias("offering","off")
                .add(Restrictions.like("off." + Offering.IDENTIFIER, offeringIdentifierPattern))
                .list();
        
        for (ObservationConstellation obsConst : obsConsts) {
            session.delete(obsConst);
        }
        session.flush();
    }    

    public static void deleteSeries(final String procedureIdentifierPattern, final Session session) {
        @SuppressWarnings("unchecked")
        List<Series> seriesList = session.createCriteria(Series.class)
                .createAlias("procedure","p")
                .add(Restrictions.like("p." + Procedure.IDENTIFIER, procedureIdentifierPattern))
                .list();
        
        for (Series series : seriesList) {
            session.delete(series);
        }
        session.flush();
    } 

    public static void deleteProcedures(final String procedureIdentifierPattern, final Session session) {
        @SuppressWarnings("unchecked")
        List<TProcedure> procedures = session.createCriteria(TProcedure.class)
                .add(Restrictions.like(Procedure.IDENTIFIER, procedureIdentifierPattern))
                .list();
        
        for (TProcedure procedure : procedures) {
            for (ValidProcedureTime vpt : procedure.getValidProcedureTimes()) {
                session.delete(vpt);
            }

            session.delete(procedure);
        }
        session.flush();
    }

    public static void deleteFeatures(final String featureIdentifierPattern, final Session session) {
        @SuppressWarnings("unchecked")
        List<TFeatureOfInterest> features = session.createCriteria(TFeatureOfInterest.class)
                .add(Restrictions.like(FeatureOfInterest.IDENTIFIER, featureIdentifierPattern))
                .list();
        
        for (TFeatureOfInterest feature : features) {
            session.delete(feature);
        }
        session.flush();
    }

    public static void deleteOfferings(final String offeringIdentifierPattern, final Session session) {
        @SuppressWarnings("unchecked")
        List<TOffering> offerings = session.createCriteria(TOffering.class)
                .add(Restrictions.like(Offering.IDENTIFIER, offeringIdentifierPattern))
                .list();
        
        for (TOffering offering : offerings) {
            session.delete(offering);
        }
        session.flush();
    }

    public static void deleteOrphanProcedureDescriptionFormats(final Session session) {
        @SuppressWarnings("unchecked")
        List<ProcedureDescriptionFormat> pdfs = session.createCriteria(ProcedureDescriptionFormat.class).list();
        for (ProcedureDescriptionFormat pdf : pdfs ) {
            long count = (Long) session.createCriteria(Procedure.class)
                    .add(Restrictions.eq("procedureDescriptionFormat", pdf))
                    .setProjection(Projections.rowCount())
                    .uniqueResult();
            if (count == 0) {
                session.delete(pdf);
            }
        }
        session.flush();
    }
    
    public static void deleteOrphanFeatureOfInterestTypes(final Session session) {
        @SuppressWarnings("unchecked")
        List<FeatureOfInterestType> featureTypes = session.createCriteria(FeatureOfInterestType.class).list();
        for (FeatureOfInterestType featureType : featureTypes ) {
            long count = (Long) session.createCriteria(FeatureOfInterest.class)
                    .add(Restrictions.eq("featureOfInterestType", featureType))
                    .setProjection(Projections.rowCount())
                    .uniqueResult();
            long offeringCount = (Long) session.createCriteria(TOffering.class, "off")
                    .createAlias("off.featureOfInterestTypes", "foiType")
                    .add(Restrictions.eq("foiType.featureOfInterestTypeId", featureType.getFeatureOfInterestTypeId()))
                    .setProjection(Projections.rowCount())
                    .uniqueResult();
            if (count + offeringCount == 0) {
                session.delete(featureType);
            }
        }
        session.flush();
    }

    public static void deleteOrphanObservableProperties(final Session session) {
        @SuppressWarnings("unchecked")
        List<ObservableProperty> obsProps = session.createCriteria(ObservableProperty.class).list();
        for (ObservableProperty obsProp : obsProps ) {
            long count = (Long) session.createCriteria(ObservationConstellation.class)
                    .add(Restrictions.eq("observableProperty", obsProp))
                    .setProjection(Projections.rowCount())
                    .uniqueResult();

            if(count == 0) {
                count = (Long) session.createCriteria(Series.class)
                        .add(Restrictions.eq("observableProperty", obsProp))
                        .setProjection(Projections.rowCount())
                        .uniqueResult();                
            }

            if (HibernateHelper.isEntitySupported(ObservationInfo.class, session)) {
                if(count == 0) {
                    count = (Long) session.createCriteria(ObservationInfo.class)
                            .add(Restrictions.eq("observableProperty", obsProp))
                            .setProjection(Projections.rowCount())
                            .uniqueResult();                
                }
            }

            if (count == 0) {
                session.delete(obsProp);
            }
        }
        session.flush();
    }

    public static void deleteOrphanObservationType(final Session session) {
        @SuppressWarnings("unchecked")
        List<ObservationType> obsTypes = session.createCriteria(ObservationType.class).list();
        for (ObservationType obsType : obsTypes ) {
            long count = (Long) session.createCriteria(ObservationConstellation.class)
                    .add(Restrictions.eq("observationType", obsType))
                    .setProjection(Projections.rowCount())
                    .uniqueResult();

            if (count == 0) {
                session.delete(obsType);
            }
        }
        session.flush();
    }

    public static void deleteOrphanUnits(final Session session) {
        @SuppressWarnings("unchecked")
        List<Unit> units = session.createCriteria(Unit.class).list();
        for (Unit unit : units ) {
            long count = (Long) session.createCriteria(SeriesObservationInfo.class)
                    .add(Restrictions.eq("unit", unit))
                    .setProjection(Projections.rowCount())
                    .uniqueResult();

            if (count == 0) {
                session.delete(unit);
            }
        }
        session.flush();
    }            
}