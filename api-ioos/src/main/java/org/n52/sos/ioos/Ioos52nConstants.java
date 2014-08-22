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
package org.n52.sos.ioos;

import java.util.Arrays;
import java.util.List;

import javax.xml.namespace.QName;

import org.n52.sos.encode.EncoderKey;
import org.n52.sos.encode.XmlEncoderKey;
import org.n52.sos.ogc.gml.GmlConstants;
import org.n52.sos.ogc.gml.time.Time;
import org.n52.sos.ogc.om.OmConstants;
import org.n52.sos.ogc.swe.SweConstants;

public interface Ioos52nConstants {
	EncoderKey GML_ENCODER_KEY = new XmlEncoderKey(GmlConstants.NS_GML, Time.class );	
	List<Integer> ALLOWED_EPSGS = Arrays.asList( 4326, 4979 );    
    //define a prefix for swe2 here due to our unholy mix of swe 1 and 2
    String SWE2_PREFIX = "swe2";

    String DECIMAL_SEPARATOR = ".";
    String TOKEN_SEPARATOR = ",";
    String BLOCK_SEPARATOR = Character.toString((char) 10);    
    //have to use a random ASCII character that xmlbeans will replace with the proper xml entity (&#10;)
    char BLOCK_SEPARATOR_TO_ESCAPE = (char) 255;
    String BLOCK_SEPARATOR_ESCAPED = "&#10;";

    QName QN_BOOLEAN_SWE_200 = new QName(SweConstants.NS_SWE_20, SweConstants.EN_BOOLEAN, SWE2_PREFIX );    
    QName QN_CATEGORY_SWE_200 = new QName(SweConstants.NS_SWE_20, SweConstants.EN_CATEGORY, SWE2_PREFIX );    
    QName QN_COUNT_SWE_200 = new QName(SweConstants.NS_SWE_20, SweConstants.EN_COUNT, SWE2_PREFIX );    
    QName QN_QUANTITY_SWE_200 = new QName(SweConstants.NS_SWE_20, SweConstants.EN_QUANTITY, SWE2_PREFIX );    
    QName QN_TEXT_SWE_200 = new QName(SweConstants.NS_SWE_20, SweConstants.EN_TEXT, SWE2_PREFIX );    
    QName QN_TIME_SWE_200 = new QName(SweConstants.NS_SWE_20, SweConstants.EN_TIME, SWE2_PREFIX );    
    QName QN_TEXT_ENCODING_SWE_200 = new QName(SweConstants.NS_SWE_20, SweConstants.EN_TEXT_ENCODING, SWE2_PREFIX );
    QName QN_VECTOR_SWE_200 = new QName( SweConstants.NS_SWE_20, SweConstants.EN_VECTOR, SWE2_PREFIX );    
    QName QN_DATA_CHOICE_SWE_200 = new QName( SweConstants.NS_SWE_20, SweConstants.EN_DATA_CHOICE, SWE2_PREFIX );
    QName QN_DATA_RECORD_SWE_200 = new QName( SweConstants.NS_SWE_20, SweConstants.EN_DATA_RECORD, SWE2_PREFIX );
    QName QN_DATA_ARRAY_SWE_200 = new QName( SweConstants.NS_SWE_20, SweConstants.EN_DATA_ARRAY, SWE2_PREFIX );
    QName QN_PROCESS = new QName( OmConstants.NS_OM, OmConstants.EN_PROCESS, OmConstants.NS_OM_PREFIX );
    QName QN_MEMBER = new QName( GmlConstants.NS_GML, OmConstants.EN_MEMBER, GmlConstants.NS_GML_PREFIX );
    QName QN_COMPOSITE_PHENOMENON = new QName( SweConstants.NS_SWE_101, OmConstants.EN_COMPOSITE_PHENOMENON, SweConstants.NS_SWE_PREFIX );	
}