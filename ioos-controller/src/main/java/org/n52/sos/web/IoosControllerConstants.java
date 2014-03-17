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
package org.n52.sos.web;

/**
 * @since 4.0.0
 * 
 */
public interface IoosControllerConstants {
    interface Views {
        String ADMIN_TEST_DATA = "admin/testdata";
        
        String INFO_PROCEDURE_LATEST_TIMES = "info/procedure-latest-times";
    }

    interface Paths {
        String ADMIN_TEST_DATA = "/admin/testdata";

        String ADMIN_TEST_DATA_REMOVE = "/admin/testdata/remove";

        String ADMIN_TEST_DATA_CREATE = "/admin/testdata/create";

        //use :.* to prevent spring from interpretting anything after . as a file extension
        String INFO_EXISTS_OFFERING = "/info/exists/offering/{offeringId:.*}";
        String INFO_EXISTS_PROCEDURE = "/info/exists/procedure/{procedureId:.*}";
        String INFO_EXISTS_FEATURE = "/info/exists/feature/{featureId:.*}";
        
        String INFO_CACHE_PROCEDURES = "/info/cache/procedures";
        
        String INFO_ALL_OBSERVATIONS_TIME_RANGE = "/info/all_observations_time_range";        
        String INFO_PROCEDURE_LATEST_TIMES = "/info/procedure_latest_times";
    }
}
