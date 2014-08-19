package org.n52.sos.encode;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.n52.sos.exception.ows.concrete.UnsupportedEncoderInputException;
import org.n52.sos.ogc.ows.OwsExceptionReport;
import org.n52.sos.response.BinaryAttachmentResponse;
import org.n52.sos.response.GetObservationResponse;

import com.axiomalaska.phenomena.Phenomena;
import com.axiomalaska.phenomena.Phenomenon;
import com.axiomalaska.phenomena.UnitCreationException;
import com.google.common.collect.Lists;

public class IoosNetcdfEncoderTest extends AbstractIoosNetcdfEncoderTest{
    public IoosNetcdfEncoderTest() {
        super(IoosNetcdfEncoder.CONTENT_TYPE_NETCDF);
    }

    @Override
    protected List<File> getNetcdfFiles(GetObservationResponse getObsResponse) throws OwsExceptionReport, FileNotFoundException, IOException {
      BinaryAttachmentResponse response = new IoosNetcdfEncoder().encode(getObsResponse);
      File targetDir = new File("target/netcdf-encoder-test/");
      targetDir.mkdirs();
      File netcdfFile = new File(targetDir, response.getFilename());
      FileUtils.writeByteArrayToFile(netcdfFile, response.getBytes());
      return Lists.newArrayList(netcdfFile);
    }

    @Test(expected=UnsupportedEncoderInputException.class)
    public void testTooManyFeatureTypesRequest() throws FileNotFoundException, OwsExceptionReport,
            IOException, UnitCreationException {
        Phenomenon airTemp = Phenomena.instance().AIR_TEMPERATURE;
        Phenomenon waterTemp = Phenomena.instance().SEA_WATER_TEMPERATURE;
        makeNetcdfRequest(Lists.newArrayList(makeSensor("1", airTemp), makeSensor("1", waterTemp)),
                Lists.newArrayList(airTemp, waterTemp), false);
    }

    @Test(expected=UnsupportedEncoderInputException.class)
    public void testTooManySensorsRequest() throws FileNotFoundException, OwsExceptionReport,
            IOException, UnitCreationException {
        Phenomenon airTemp = Phenomena.instance().AIR_TEMPERATURE;
        makeNetcdfRequest(Lists.newArrayList(makeSensor("1", airTemp), makeSensor("2", airTemp)),
                Lists.newArrayList(airTemp), false);
    }
}
