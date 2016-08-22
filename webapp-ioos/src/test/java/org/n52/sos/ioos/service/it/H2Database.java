package org.n52.sos.ioos.service.it;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.junit.rules.ExternalResource;

import org.n52.sos.ds.hibernate.H2Configuration;
import org.n52.sos.ds.hibernate.entities.ObservationType;
import org.n52.sos.ds.hibernate.util.ScrollableIterable;
import org.n52.sos.ogc.om.OmConstants;
import org.n52.sos.ogc.ows.OwsExceptionReport;

/**
 * @author Christian Autermann <c.autermann@52north.org>
 *
 * TODO use the H2Database class from webapp's test-jar once it's released
 * https://github.com/52North/SOS/pull/455
 *
 * @since 4.0.0
 */
public class H2Database extends ExternalResource {
    private final String[] defaultObservationTypes
            = OmConstants.OBSERVATION_TYPES
            .toArray(new String[OmConstants.OBSERVATION_TYPES.size()]);

    @Override
    protected void before() throws Throwable {
        H2Configuration.assertInitialized();
    }

    @Override
    protected void after() {
        H2Configuration.truncate();
    }

    /**
     * Removes all entries of entity {@link ObservationType} from the database.
     *
     * @throws OwsExceptionReport
     */
    protected void removeObservationTypes() throws OwsExceptionReport {
        Session session = null;
        Transaction transaction = null;
        try {
            session = getSession();
            transaction = session.beginTransaction();
            Criteria criteria = session.createCriteria(ObservationType.class);
            try (ScrollableIterable<ObservationType> i = ScrollableIterable.fromCriteria(criteria)) {
                for (final ObservationType o : i) {
                    session.delete(o);
                }
            }
            session.flush();
            transaction.commit();
        } catch (final HibernateException he) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw he;
        } finally {
            returnSession(session);
        }
    }

    /**
     * Add some default entries of entity {@link ObservationType} to the test
     * database.
     *
     * @throws OwsExceptionReport
     * @see {@link #defaultObservationTypes}
     */
    protected void addObservationTypes() throws OwsExceptionReport {
        Session session = null;
        Transaction transaction = null;
        try {
            session = getSession();
            transaction = session.beginTransaction();
            for (int i = 0; i < defaultObservationTypes.length; i++) {
                final ObservationType ot = new ObservationType();
                ot.setObservationTypeId(i);
                ot.setObservationType(defaultObservationTypes[i]);
                session.save(ot);
            }
            session.flush();
            transaction.commit();
        } catch (final HibernateException he) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw he;
        } finally {
            returnSession(session);
        }
    }

    public void recreate() {
        H2Configuration.recreate();
    }

    public void truncate() {
        H2Configuration.truncate();
    }

    public Session getSession() {
        return H2Configuration.getSession();
    }

    public void returnSession(Session session) {
        H2Configuration.returnSession(session);
    }
}
