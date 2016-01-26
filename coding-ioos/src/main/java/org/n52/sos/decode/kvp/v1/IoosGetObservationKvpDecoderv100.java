package org.n52.sos.decode.kvp.v1;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.n52.sos.encode.IoosEncoderUtil;
import org.n52.sos.ogc.filter.FilterConstants.SpatialOperator;
import org.n52.sos.ogc.filter.SpatialFilter;
import org.n52.sos.ogc.ows.OwsExceptionReport;
import org.n52.sos.ogc.sos.SosConstants;
import org.n52.sos.request.GetObservationRequest;
import org.n52.sos.util.Constants;
import org.n52.sos.util.JTSHelper;

import com.google.common.collect.Lists;

public class IoosGetObservationKvpDecoderv100 extends GetObservationKvpDecoderv100 {
    @Override
    public GetObservationRequest decode(Map<String, String> elements) throws OwsExceptionReport {
        SpatialFilter spatialFilter = null;

        //loop through params to allow case insensitive detection
        Iterator<String> paramsIterator = elements.keySet().iterator();
        while (paramsIterator.hasNext()) {
            String param = paramsIterator.next();
            //parse IOOS 1.0 WSDD format featureOfInterest BBOX (only if spatialFilter isn't already set
            //featureOfInterest=BBOX:min_lon,min_lat,max_lon,max_lat
            if (param.equalsIgnoreCase(SosConstants.GetObservationParams.featureOfInterest.name())) {
                String foiParamValue = elements.get(param);
                if (IoosEncoderUtil.isIoos10BboxString(foiParamValue)) {
                    List<String> coordinates = Lists.newArrayList(
                            foiParamValue.substring(foiParamValue.lastIndexOf(':') + 1).split(","));

                    String lowerCorner =
                            String.format(Locale.US, "%s %s", new BigDecimal(coordinates.get(Constants.INT_0)).toString(),
                                    new BigDecimal(coordinates.get(Constants.INT_1)).toString());
                    String upperCorner =
                            String.format(Locale.US, "%s %s", new BigDecimal(coordinates.get(Constants.INT_2)).toString(),
                                    new BigDecimal(coordinates.get(Constants.INT_3)).toString());

                    spatialFilter = new SpatialFilter();
                    //fake the value reference to the SOS 2.0 standard
                    spatialFilter.setValueReference("om:featureOfInterest/*/sams:shape");
                    spatialFilter.setGeometry(JTSHelper.createGeometryFromWKT(
                            JTSHelper.createWKTPolygonFromEnvelope(lowerCorner, upperCorner), 4326));
                    spatialFilter.setOperator(SpatialOperator.BBOX);
    
                    //remove featureOfInterest parameter since its format is not
                    //correct according to the SOS 2.0 spec
                    paramsIterator.remove();
                }
            }
        }

        final GetObservationRequest request = super.decode(elements);

        if (request.getSpatialFilter() == null && spatialFilter != null) {
            request.setSpatialFilter(spatialFilter);
        }
        return request;
    }
}
