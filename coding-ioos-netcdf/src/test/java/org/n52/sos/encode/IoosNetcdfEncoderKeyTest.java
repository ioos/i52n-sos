package org.n52.sos.encode;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.n52.sos.coding.CodingRepository;
import org.n52.sos.ogc.sos.Sos1Constants;
import org.n52.sos.ogc.sos.SosConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IoosNetcdfEncoderKeyTest {
    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(IoosNetcdfEncoderKeyTest.class);

    @Test
    public void testEncoderKey() {
        OperationEncoderKey key = new OperationEncoderKey(SosConstants.SOS, Sos1Constants.SERVICEVERSION,
                SosConstants.Operations.GetObservation.name(), IoosNetcdfEncoder.CONTENT_TYPE_NETCDF_ZIP);
        Encoder<Object, Object> encoder = CodingRepository.getInstance().getEncoder(key);        
        assertThat(encoder, instanceOf(IoosNetcdfEncoder.class));
    }
}
