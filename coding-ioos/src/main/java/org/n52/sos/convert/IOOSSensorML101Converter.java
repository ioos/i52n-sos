package org.n52.sos.convert;

import java.util.Collections;
import java.util.List;

import org.n52.sos.convert.Converter;
import org.n52.sos.convert.ConverterException;
import org.n52.sos.convert.ConverterKeyType;
import org.n52.sos.ogc.sensorML.SensorMLConstants;
import org.n52.sos.ogc.sos.SosProcedureDescription;
import org.n52.sos.util.CollectionHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axiomalaska.ioos.sos.IoosSosConstants;
import com.google.common.base.Joiner;

/**
 * Pass-through converter from/to IOOS SML profile (text/xml; subtype="sensorML/1.0.1/profiles/ioos_sos/1.0"),
 * SensorML 1.0.1 URL (http://www.opengis.net/sensorML/1.0.1)
 * and mime type (text/xml; subtype="sensorML/1.0.1") formats.
 *
 * Add more keys here if necessary to facilitate additional pass-through encodings.
 */
public class IOOSSensorML101Converter implements Converter<SosProcedureDescription, SosProcedureDescription> {

    private static final Logger LOGGER = LoggerFactory.getLogger(IOOSSensorML101Converter.class);

    private static final List<ConverterKeyType> CONVERTER_KEY_TYPES = CollectionHelper.list(
            new ConverterKeyType(IoosSosConstants.SML_PROFILE_M10, SensorMLConstants.SENSORML_OUTPUT_FORMAT_URL),
            new ConverterKeyType(IoosSosConstants.SML_PROFILE_M10, SensorMLConstants.SENSORML_OUTPUT_FORMAT_MIME_TYPE),
            new ConverterKeyType(SensorMLConstants.SENSORML_OUTPUT_FORMAT_URL, IoosSosConstants.SML_PROFILE_M10),
            new ConverterKeyType(SensorMLConstants.SENSORML_OUTPUT_FORMAT_MIME_TYPE, IoosSosConstants.SML_PROFILE_M10));

    public IOOSSensorML101Converter() {
        LOGGER.debug("Converter for the following keys initialized successfully: {}!",
                Joiner.on(", ").join(CONVERTER_KEY_TYPES));
    }

    @Override
    public List<ConverterKeyType> getConverterKeyTypes() {
        return Collections.unmodifiableList(CONVERTER_KEY_TYPES);
    }

    @Override
    public SosProcedureDescription convert(SosProcedureDescription objectToConvert) throws ConverterException {
        return objectToConvert;
    }
}
