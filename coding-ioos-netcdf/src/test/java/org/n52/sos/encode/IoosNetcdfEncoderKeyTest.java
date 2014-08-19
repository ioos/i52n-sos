package org.n52.sos.encode;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.n52.sos.coding.CodingRepository;
import org.n52.sos.ogc.sos.Sos1Constants;
import org.n52.sos.ogc.sos.SosConstants;
import org.n52.sos.util.http.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IoosNetcdfEncoderKeyTest {
    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(IoosNetcdfEncoderKeyTest.class);

    @Test
    public void testNetcdfEncoderKey() {
        testEncoderKey(IoosNetcdfEncoder.CONTENT_TYPE_NETCDF, IoosNetcdfEncoder.class);
    }

    @Test
    public void testNetcdfZipEncoderKey() {
        testEncoderKey(IoosNetcdfZipEncoder.CONTENT_TYPE_NETCDF_ZIP, IoosNetcdfZipEncoder.class);
    }

    private void testEncoderKey(MediaType contentType, Class<? extends AbstractIoosNetcdfEncoder> encoderClass) {
        OperationEncoderKey key = new OperationEncoderKey(SosConstants.SOS, Sos1Constants.SERVICEVERSION,
                SosConstants.Operations.GetObservation.name(), contentType);
        Encoder<Object, Object> encoder = CodingRepository.getInstance().getEncoder(key);
        assertThat(encoder, instanceOf(encoderClass));
    }

}
