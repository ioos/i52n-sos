package org.n52.sos.encode;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.n52.sos.exception.ows.NoApplicableCodeException;
import org.n52.sos.ioos.data.dataset.AbstractSensorDataset;
import org.n52.sos.ioos.om.IoosSosObservation;
import org.n52.sos.ogc.ows.OwsExceptionReport;
import org.n52.sos.response.BinaryAttachmentResponse;
import org.n52.sos.util.http.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.Files;

public class IoosNetcdfZipEncoder extends AbstractIoosNetcdfEncoder{
    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(IoosNetcdfZipEncoder.class);
    private static final String DOWNLOAD_FILENAME_FORMAT = "i52n-sos_netcdf_%s.zip";

    public static MediaType CONTENT_TYPE_NETCDF_ZIP = 
            new MediaType("application", "zip", "subtype", "x-netcdf");    

    @Override
    public MediaType getContentType() {
        return CONTENT_TYPE_NETCDF_ZIP;
    }

    protected BinaryAttachmentResponse encodeIoosObsToNetcdf(List<IoosSosObservation> ioosSosObsList) throws OwsExceptionReport {
        File tempDir = Files.createTempDir();
        
        for (IoosSosObservation ioosSosObs : ioosSosObsList) {
            for (AbstractSensorDataset sensorDataset : ioosSosObs.getSensorDatasets()) {
                File netcdfFile = new File(tempDir, getFilename(sensorDataset));
                encodeSensorDataToNetcdf(netcdfFile, sensorDataset);                
            }
        }
        
        BinaryAttachmentResponse response = null;
        ByteArrayOutputStream zipBoas = null;
        try {
            zipBoas = createZip(tempDir);
            response = new BinaryAttachmentResponse(zipBoas.toByteArray(), getContentType(),
                    String.format(DOWNLOAD_FILENAME_FORMAT, makeDateSafe(new DateTime(DateTimeZone.UTC))));
        } catch (IOException e) {
            throw new NoApplicableCodeException().causedBy(e)
                .withMessage("Couldn't create netCDF zip file");
        } finally {
            if (zipBoas != null) {
                try {
                    zipBoas.close();
                } catch (IOException e) {
                    //NOOP closing BAOS has no effect anyway
                }
            }
            tempDir.delete();            
        }

        return response;
    }

    private static ByteArrayOutputStream createZip(File dirToZip) throws IOException {  
        ByteArrayOutputStream bos = new ByteArrayOutputStream();  
        ZipOutputStream zipfile = new ZipOutputStream(bos);    
        ZipEntry zipentry = null;  
        for (File file : dirToZip.listFiles()) {  
            zipentry = new ZipEntry(file.getName());  
            zipfile.putNextEntry(zipentry);  
            zipfile.write(Files.toByteArray(file));              
        }
        zipfile.close();
        return bos;  
    }
}
