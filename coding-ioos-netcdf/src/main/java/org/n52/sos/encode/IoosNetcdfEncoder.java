package org.n52.sos.encode;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.n52.sos.exception.CodedException;
import org.n52.sos.exception.ows.NoApplicableCodeException;
import org.n52.sos.exception.ows.concrete.UnsupportedEncoderInputException;
import org.n52.sos.ioos.data.dataset.AbstractSensorDataset;
import org.n52.sos.ioos.om.IoosSosObservation;
import org.n52.sos.ogc.ows.OwsExceptionReport;
import org.n52.sos.response.BinaryAttachmentResponse;
import org.n52.sos.util.CollectionHelper;
import org.n52.sos.util.http.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.Files;

public class IoosNetcdfEncoder extends AbstractIoosNetcdfEncoder{
    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(IoosNetcdfEncoder.class);

    public static MediaType CONTENT_TYPE_NETCDF = new MediaType("application", "x-netcdf");    

    @Override
    public MediaType getContentType() {
        return CONTENT_TYPE_NETCDF;
    }

    protected BinaryAttachmentResponse encodeIoosObsToNetcdf(List<IoosSosObservation> ioosSosObsList) throws OwsExceptionReport {
        if (CollectionHelper.isEmpty(ioosSosObsList)) {
            throw new NoApplicableCodeException().withMessage("No feature types to encode");
        } else if (ioosSosObsList.size() > 1) {
            throwTooManyFeatureTypesOrSensorsException(ioosSosObsList, ioosSosObsList.size(), null);
        }

        IoosSosObservation ioosSosObservation = ioosSosObsList.get(0);

        if (CollectionHelper.isEmpty(ioosSosObservation.getSensorDatasets())) {
            throw new NoApplicableCodeException().withMessage("No sensors to encode");
        } else if (ioosSosObservation.getSensorDatasets().size() > 1) {
            throwTooManyFeatureTypesOrSensorsException(ioosSosObsList, null, ioosSosObservation.getSensorDatasets().size());
        }

        AbstractSensorDataset sensorDataset = ioosSosObservation.getSensorDatasets().get(0);
        File tempDir = Files.createTempDir();
        String filename = getFilename(sensorDataset);
        File netcdfFile = new File(tempDir, filename);
        encodeSensorDataToNetcdf(netcdfFile, sensorDataset);                

        BinaryAttachmentResponse response = null;
        try {
            response = new BinaryAttachmentResponse(Files.toByteArray(netcdfFile), getContentType(),
                    String.format(filename, makeDateSafe(new DateTime(DateTimeZone.UTC))));
        } catch (IOException e) {
            throw new NoApplicableCodeException().causedBy(e).withMessage("Couldn't create netCDF file");
        } finally {
            tempDir.delete();
        }

        return response;
    }

    private void throwTooManyFeatureTypesOrSensorsException(List<IoosSosObservation> ioosSosObsList,
            Integer numFeatureTypes, Integer numSensors) throws CodedException {
        StringBuilder sb = new StringBuilder();
        sb.append("This encoder (" + CONTENT_TYPE_NETCDF.toString() + ") can only encode a single feature type");
        if (numFeatureTypes != null) {
            sb.append(" (found " + numFeatureTypes + ")");
        }
        sb.append(" and a single sensor");
        if (numSensors != null) {
            sb.append(" (found " + numSensors + ")");
        }
        sb.append(". Change your request to only return a single feature type or use the zipped netCDF encoder ("
                + IoosNetcdfZipEncoder.CONTENT_TYPE_NETCDF_ZIP.toString() + ").");
        throw new UnsupportedEncoderInputException(this, ioosSosObsList).withMessage(sb.toString());
    }
}
