package org.n52.sos.encode;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import org.apache.xmlbeans.XmlObject;
import org.junit.Test;
import org.n52.sos.ogc.ows.OwsExceptionReport;
import org.n52.sos.ogc.sensorML.SensorML;
import org.n52.sos.ogc.sensorML.System;
import org.n52.sos.util.CodingHelper;

import com.axiomalaska.ioos.sos.IoosSosConstants;

public class IoosSensorMLEncoderv101Test {
	@Test
	public void should_get_ioos_sml_encoder() throws OwsExceptionReport{
		SensorML sml = new SensorML();
		sml.addMember(new System());
		Encoder<XmlObject, SensorML> encoder = CodingHelper.getEncoder(IoosSosConstants.SML_PROFILE_M10, sml);
		assertThat(encoder, notNullValue());
		assertThat(encoder, instanceOf(IoosSensorMLEncoderv101.class));
		encoder.encode(sml);
	}
}
