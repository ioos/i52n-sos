package org.n52.sos.encode;

import org.apache.xmlbeans.XmlObject;

import com.axiomalaska.ioos.sos.validator.provider.SosDocumentProvider;
import com.axiomalaska.ioos.sos.validator.provider.SosDocumentType;

public class IoosCodingSosDocumentProvider extends SosDocumentProvider{
    @Override
    protected XmlObject getDocumentXml(SosDocumentType document){
        //TODO implement document retrieval, need test config and data (in-memory H2?)
        switch(document){
            case M1_0_CAPABILITIES:                
                break;
            case M1_0_SENSOR_ML_NETWORK:
                break;
            case M1_0_SENSOR_ML_STATION:
                break;
            case M1_0_SENSOR_ML_SENSOR:
                break;
            case M1_0_OBSERVATION_COLLECTION:
                break;
            case M1_0_SWE_TIME_SERIES:
                break;
            case M1_0_SWE_TIME_SERIES_PROFILE:
                break;                
        }
        return null;
    }

}
