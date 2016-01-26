package org.n52.sos.encode;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class IoosEncoderUtilTest {
    @Test
    public void testIoos10Bbox() {
        assertTrue(IoosEncoderUtil.isIoos10BboxString("BBOX:-20.0,-50.0,20.0,50.0"));
        assertTrue(IoosEncoderUtil.isIoos10BboxString("BBOX:  -20.0, -50.0,  -10.0,   -30.0  "));
        assertTrue(IoosEncoderUtil.isIoos10BboxString("bBOx:-20.0,-50.0,20.0,50.0"));
        assertFalse(IoosEncoderUtil.isIoos10BboxString("BBOX:-20.0,d-50.0,-10.0,-30.0"));
    }
}
