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

import java.util.Collections;
import java.util.Properties;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.spatial.dialect.h2geodb.GeoDBDialect;
import org.n52.sos.ds.hibernate.HibernateSessionHolder;
import org.n52.sos.event.SosEvent;
import org.n52.sos.event.SosEventListener;
import org.n52.sos.event.events.ConfiguratorInitializedEvent;
import org.n52.sos.ogc.ows.OwsExceptionReport;
import org.n52.sos.service.Configurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestDataConfiguratorInitializedListener implements SosEventListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestDataConfiguratorInitializedListener.class);
    private static final String AUTO_INSERT_H2_URL = "jdbc:h2:mem:sos-test";
    // TODO probably should be a constants class for this
    public static final String HIBERNATE_CONNECTION_URL = "hibernate.connection.url";    

    public static final Set<Class<? extends SosEvent>> EVENTS = Collections
            .<Class<? extends SosEvent>> singleton(ConfiguratorInitializedEvent.class);

    @Override
    public Set<Class<? extends SosEvent>> getTypes() {
        return EVENTS;
    }

    @Override
    public void handle(final SosEvent event) {
        //always should be a ConfiguratorInitializedEvent, but check anyway
        if (!(event instanceof ConfiguratorInitializedEvent)){
            return;
        }
        
        boolean autoInsertTestData = false;
        
        try {
            if (isAutoInsertGeoDB()){
                autoInsertTestData = true;
            }
        } catch (OwsExceptionReport e) {
            LOGGER.error("Error checking datasource for test data auto-insert", e);
        }

            
        if (autoInsertTestData){
            try {
                //cache may be stale since the cache file is preserved between restarts, but the in-memory database isn't
                //reload cache before continuing
                Configurator.getInstance().getCacheController().update();
                if (Configurator.getInstance().getCache().getProcedures().isEmpty()){
                    IoosHibernateTestDataManager.insertTestData();
                }
            } catch (OwsExceptionReport e) {
                LOGGER.error("Error auto-inserting test data", e);                
            }
        }
    }
    
    private boolean isAutoInsertGeoDB() throws OwsExceptionReport{
        HibernateSessionHolder sessionHolder = new HibernateSessionHolder();
        Session session = sessionHolder.getSession();        
        Dialect dialect = ((SessionFactoryImplementor) session.getSessionFactory()).getDialect();
        Properties props = ((SessionFactoryImplementor) session.getSessionFactory()).getProperties();
        String url = null;
        Object urlObject = props.get(HIBERNATE_CONNECTION_URL);
        if (urlObject != null && urlObject instanceof String){
            url = (String) urlObject;
        }
        sessionHolder.returnSession(session);
        return dialect != null & url != null && dialect instanceof GeoDBDialect
                && url.startsWith(AUTO_INSERT_H2_URL);
    }
}
