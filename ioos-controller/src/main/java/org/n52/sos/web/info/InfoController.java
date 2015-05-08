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
package org.n52.sos.web.info;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

import javax.servlet.UnavailableException;

import org.joda.time.DateTime;
import org.n52.sos.cache.ContentCache;
import org.n52.sos.service.Configurator;
import org.n52.sos.util.JSONUtils;
import org.n52.sos.web.AbstractController;
import org.n52.sos.web.IoosControllerConstants;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

@Controller
public class InfoController extends AbstractController {
    private static final String EXISTS = "exists";
    private static final String MIN = "min";
    private static final String MAX = "max";

    private static final String PROCEDURE_LATEST_TIMES = "procedure_latest_times";
    
    private String existsResponse(boolean exists) {
        return JSONUtils.nodeFactory().objectNode().put(EXISTS, exists).toString();
    }

    private ContentCache getCache() throws UnavailableException {
        if (Configurator.getInstance() == null) {
            throw new UnavailableException("configurator is not available");            
        }        
        return Configurator.getInstance().getCache();        
    }

    @ResponseBody
    @RequestMapping(value = IoosControllerConstants.Paths.INFO_EXISTS_OFFERING, method = RequestMethod.GET,
        produces = "application/json; charset=UTF-8")    
    public String existsOffering(@PathVariable String offeringId) throws UnavailableException {        
        return existsResponse(getCache().getOfferings().contains(offeringId));
    }

    @ResponseBody
    @RequestMapping(value = IoosControllerConstants.Paths.INFO_EXISTS_PROCEDURE, method = RequestMethod.GET,
        produces = "application/json; charset=UTF-8")    
    public String existsProcedure(@PathVariable String procedureId) throws UnavailableException {        
        return existsResponse(getCache().getProcedures().contains(procedureId));
    }

    @ResponseBody
    @RequestMapping(value = IoosControllerConstants.Paths.INFO_EXISTS_FEATURE, method = RequestMethod.GET,
        produces = "application/json; charset=UTF-8")    
    public String existsFeature(@PathVariable String featureId) throws UnavailableException {
        return existsResponse(getCache().getFeaturesOfInterest().contains(featureId));
    }

    @ResponseBody
    @RequestMapping(value = IoosControllerConstants.Paths.INFO_CACHE_PROCEDURES, method = RequestMethod.GET, produces = "text/plain")    
    public String dumpCacheProcedures() throws UnavailableException{
        ArrayList<String> cacheProcedures = Lists.newArrayList(getCache().getProcedures());
        Collections.sort(cacheProcedures);
        return Joiner.on("\n").join(cacheProcedures);
    }
    
    @ResponseBody
    @RequestMapping(value = IoosControllerConstants.Paths.INFO_ALL_OBSERVATIONS_TIME_RANGE, method = RequestMethod.GET,
        produces = "application/json; charset=UTF-8")    
    public String allObsTimeRange() throws UnavailableException {
        Map<String,DateTime> map = Maps.newHashMap();
        map.put(MIN, getCache().getMinPhenomenonTime());
        map.put(MAX, getCache().getMaxPhenomenonTime());
        return JSONUtils.toJSON(map).toString();
    }

    @ResponseBody
    @RequestMapping(value = IoosControllerConstants.Paths.INFO_PROCEDURE_LATEST_TIMES, method = RequestMethod.GET)    
    public ModelAndView procedureLatestTime() throws UnavailableException{
        Map<String,Object> model = Maps.newHashMap();
        ArrayList<String> cacheProcedures = Lists.newArrayList(getCache().getProcedures());
        Collections.sort(cacheProcedures);
        Map<String,DateTime> procedureMaxTimes = Maps.newLinkedHashMap();
        model.put(PROCEDURE_LATEST_TIMES, procedureMaxTimes);
        for (String procedure : cacheProcedures) {
            procedureMaxTimes.put(procedure, getCache().getMaxPhenomenonTimeForProcedure(procedure));
        }
        return new ModelAndView(IoosControllerConstants.Views.INFO_PROCEDURE_LATEST_TIMES, model);
    }    
}