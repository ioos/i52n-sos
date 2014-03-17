package org.n52.sos.encode;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Ioos52nSosVersionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(Ioos52nSosVersionHandler.class);
    
    private String ioosVersion;
    private static Ioos52nSosVersionHandler instance;
    
    private static final String META_IOOS_PROPERTIES = "meta.ioos.properties";
    private static final String IOOS_VERSION = "IOOS_VERSION";
    
    private Ioos52nSosVersionHandler(){
        Properties properties = new Properties();
        InputStream in = getClass().getClassLoader().getResourceAsStream(META_IOOS_PROPERTIES);
        if (in != null) {
            try {
                properties.load(in);            
            } catch (IOException e) {
                LOGGER.warn("Couldn't get IOOS version from " + META_IOOS_PROPERTIES);
            }
        }

        if (properties.containsKey(IOOS_VERSION)){
            ioosVersion = properties.getProperty(IOOS_VERSION);
        }
    }
    
    public static String getIoosVersion(){
        if (instance == null){
            instance = new Ioos52nSosVersionHandler();
        }
        return instance.ioosVersion();
    }
    
    private String ioosVersion(){
        return ioosVersion;
    }
}
