package org.n52.sos.decode;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import net.opengis.sensorML.x101.ProcessModelType;
import net.opengis.sensorML.x101.SensorMLDocument;
import net.opengis.sensorML.x101.SystemType;

import org.n52.sos.ogc.sensorML.elements.SmlIdentifier;
import org.n52.sos.service.ServiceConstants.SupportedTypeKey;
import org.n52.sos.util.CodingHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axiomalaska.ioos.sos.IoosDefConstants;
import com.axiomalaska.ioos.sos.IoosSosConstants;
import com.google.common.base.Joiner;

public class IoosSensorMLDecoderv101 extends SensorMLDecoderV101 {
    private static final Logger LOGGER = LoggerFactory.getLogger(IoosSensorMLDecoderv101.class);
    
    private static final Map<SupportedTypeKey, Set<String>> SUPPORTED_TYPES = Collections.singletonMap(
            SupportedTypeKey.ProcedureDescriptionFormat, Collections.singleton(IoosSosConstants.SML_PROFILE_M10));

    private static final Set<DecoderKey> DECODER_KEYS = CodingHelper.decoderKeysForElements(
            IoosSosConstants.SML_PROFILE_M10, SensorMLDocument.class, SystemType.class, ProcessModelType.class);
    
    @Override
    public Set<DecoderKey> getDecoderKeyTypes() {
        return Collections.unmodifiableSet(DECODER_KEYS);
    }

    @Override
    public Map<SupportedTypeKey, Set<String>> getSupportedTypes() {
        return SUPPORTED_TYPES;
    }

    public IoosSensorMLDecoderv101() {
        LOGGER.debug("Decoder for the following keys initialized successfully: {}!",
                Joiner.on(", ").join(DECODER_KEYS));
    }

    /**
     * Checks the URI of a System's identifiers to find the primary identifier 
     * 
     * @param definition
     *            URI of identifier
     * @return Boolean whether or not this is the primary identifier
     */
	@Override
    protected boolean isIdentificationProcedureIdentifier(final SmlIdentifier identification) {
        if( identification.getDefinition() != null && ( identification.getDefinition().equals( IoosDefConstants.NETWORK_ID_DEF )
                || identification.getDefinition().equals( IoosDefConstants.STATION_ID_DEF ) 
                || identification.getDefinition().equals( IoosDefConstants.SENSOR_ID_DEF ) ) ){                        
            return true;
        }
        return false;
    }
}