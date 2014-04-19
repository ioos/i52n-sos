package org.n52.sos.encode;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import net.opengis.sensorML.x101.CapabilitiesDocument.Capabilities;
import net.opengis.sensorML.x101.SensorMLDocument;
import net.opengis.swe.x101.AnyScalarPropertyType;
import net.opengis.swe.x101.SimpleDataRecordType;
import net.opengis.swe.x101.TextDocument.Text;

import org.apache.xmlbeans.XmlObject;
import org.junit.Test;
import org.n52.sos.exception.ows.concrete.UnsupportedEncoderInputException;
import org.n52.sos.ogc.ows.OwsExceptionReport;
import org.n52.sos.ogc.sensorML.SensorML;
import org.n52.sos.ogc.sensorML.System;
import org.n52.sos.ogc.sensorML.elements.SmlCapabilities;
import org.n52.sos.util.CodingHelper;

import com.axiomalaska.ioos.sos.IoosSosConstants;

public class IoosSensorMLEncoderv101Test {
	@Test
	public void should_get_ioos_sml_encoder() throws OwsExceptionReport{
		Encoder<XmlObject, SensorML> encoder = CodingHelper.getEncoder(IoosSosConstants.SML_PROFILE_M10, getBlankSensorML());
		assertThat(encoder, notNullValue());
		assertThat(encoder, instanceOf(IoosSensorMLEncoderv101.class));
	}

    @Test
    public void should_add_service_metadata() throws OwsExceptionReport{
        SensorMLDocument xbSensorMLDoc = encodeSensorML(getBlankSensorML());
        Capabilities xbServiceMetadataCapabilities = findCapabilities(xbSensorMLDoc.getSensorML().getCapabilitiesArray(),
                IoosSosConstants.IOOS_SERVICE_METADATA);
        assertNotNull(xbServiceMetadataCapabilities.getAbstractDataRecord());
        assertThat(xbServiceMetadataCapabilities.getAbstractDataRecord(), instanceOf(SimpleDataRecordType.class));
        SimpleDataRecordType xbSimpleDataRecord = (SimpleDataRecordType) xbServiceMetadataCapabilities.getAbstractDataRecord();
        
        AnyScalarPropertyType xbTemplateVersionField = findField(xbSimpleDataRecord, IoosSosConstants.IOOS_TEMPLATE_VERSION);
        Text xbTemplateVersionText = xbTemplateVersionField.getText();
        assertEquals(xbTemplateVersionText.getDefinition(), IoosSosConstants.IOOS_VERSION_DEFINITION);

        findField(xbSimpleDataRecord, IoosSosConstants.SOFTWARE_VERSION);
    }

    @Test
    public void should_replace_existing_service_metadata() throws OwsExceptionReport{
        SensorML sml = getBlankSensorML();
        SmlCapabilities smlCapabilities = new SmlCapabilities();
        sml.addCapabilities(smlCapabilities);
        smlCapabilities.setName(IoosSosConstants.IOOS_SERVICE_METADATA);
        SensorMLDocument xbSensorMLDoc = encodeSensorML(sml);
        assertEquals(1, xbSensorMLDoc.getSensorML().getCapabilitiesArray().length);
    }

    private Capabilities findCapabilities(Capabilities[] xbCapabilitiesArray, String capabilitiesName){
        for (Capabilities xbCapabilities : xbCapabilitiesArray) {
            if (xbCapabilities.getName().equals(IoosSosConstants.IOOS_SERVICE_METADATA)){
                return xbCapabilities;
            }
        }
        fail(String.format("Couldn't find sml:capabilities '%s'", capabilitiesName));
        return null;
    }

    private AnyScalarPropertyType findField(SimpleDataRecordType xbSimpleDataRecord, String fieldName){
        for (AnyScalarPropertyType xbField : xbSimpleDataRecord.getFieldArray()){
            if (xbField.getName().equals(fieldName)) {
                return xbField;
            }
        }
        fail(String.format("Couldn't find field '%s'", fieldName));
        return null;
    }
    
    private SensorMLDocument encodeSensorML(SensorML sml) throws UnsupportedEncoderInputException, OwsExceptionReport {
        XmlObject encoded = CodingHelper.encodeObjectToXml(IoosSosConstants.SML_PROFILE_M10, sml);
        assertThat(encoded, instanceOf(SensorMLDocument.class));
        return (SensorMLDocument) encoded;
    }

    private SensorML getBlankSensorML() {
        SensorML sml = new SensorML();
        sml.addMember(new System());
        return sml;        
    }
}
