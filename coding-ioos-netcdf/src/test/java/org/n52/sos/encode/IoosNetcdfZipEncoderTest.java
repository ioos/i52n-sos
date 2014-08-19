package org.n52.sos.encode;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.n52.sos.ogc.ows.OwsExceptionReport;
import org.n52.sos.response.BinaryAttachmentResponse;
import org.n52.sos.response.GetObservationResponse;

import com.google.common.collect.Lists;

public class IoosNetcdfZipEncoderTest extends AbstractIoosNetcdfEncoderTest{
    public IoosNetcdfZipEncoderTest() {
        super(IoosNetcdfZipEncoder.CONTENT_TYPE_NETCDF_ZIP);
    }

    @Override
    protected List<File> getNetcdfFiles(GetObservationResponse getObsResponse) throws OwsExceptionReport, FileNotFoundException, IOException {
      BinaryAttachmentResponse response = new IoosNetcdfZipEncoder().encode(getObsResponse);
      byte[] zipBytes = response.getBytes();
      List<File> unzippedNetcdfs = Lists.newArrayList();        
      ZipInputStream zipStream = new ZipInputStream(new ByteArrayInputStream(zipBytes));        
      ZipEntry zipEntry;
      File targetDir = new File("target/netcdf-zip-encoder-test/");
      targetDir.mkdirs();
      while((zipEntry = zipStream.getNextEntry()) != null) {
          File extractedFile = new File(targetDir, zipEntry.getName());
          unzippedNetcdfs.add(extractedFile);
          FileOutputStream fos = new FileOutputStream(extractedFile);
          byte[] byteBuff = new byte[4096];
          int bytesRead = 0;
          while ((bytesRead = zipStream.read(byteBuff)) != -1){
              fos.write(byteBuff, 0, bytesRead);
          }
          fos.close();
          zipStream.closeEntry();
      }
      zipStream.close();
      return unzippedNetcdfs;
    }
}
