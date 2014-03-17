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
package org.n52.sos.web.admin;

import java.util.Map;

import org.n52.sos.ds.ConnectionProviderException;
import org.n52.sos.ds.hibernate.testdata.IoosHibernateTestDataManager;
import org.n52.sos.exception.ows.NoApplicableCodeException;
import org.n52.sos.ogc.ows.OwsExceptionReport;
import org.n52.sos.web.IoosControllerConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;

import com.google.common.collect.Maps;

@Controller
public class AdminTestDataController extends AbstractDatasourceController {
    private static final Logger LOG = LoggerFactory.getLogger(AdminTestDataController.class);
    private static final String HAS_TEST_DATA = "hasTestData";

    @RequestMapping(value = IoosControllerConstants.Paths.ADMIN_TEST_DATA, method = RequestMethod.GET)
    public ModelAndView get() throws OwsExceptionReport {
        Map<String, Object> model = Maps.newHashMap();
        model.put(HAS_TEST_DATA, IoosHibernateTestDataManager.hasTestData());
        return new ModelAndView(IoosControllerConstants.Views.ADMIN_TEST_DATA, model);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequestMapping(value = IoosControllerConstants.Paths.ADMIN_TEST_DATA_REMOVE, method = RequestMethod.POST)
    public void testDataDelete() throws OwsExceptionReport, ConnectionProviderException {
        if (!IoosHibernateTestDataManager.hasTestData()) {
            throw new NoApplicableCodeException().withMessage("Test data is not present in the database.");
        }
    
        LOG.info("Removing test data set.");
        IoosHibernateTestDataManager.removeTestData();
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequestMapping(value = IoosControllerConstants.Paths.ADMIN_TEST_DATA_CREATE, method = RequestMethod.POST)
    public void testDataCreate() throws OwsExceptionReport, ConnectionProviderException {
        if (IoosHibernateTestDataManager.hasTestData()) {
            throw new NoApplicableCodeException().withMessage("Test data is already present in the database.");
        }
        LOG.info("Inserting test data set.");
        IoosHibernateTestDataManager.insertTestData();
    }
}
