package org.n52.sos.encode;

import java.util.Collection;

import javax.xml.namespace.QName;

import net.opengis.sos.x10.ContentsDocument.Contents;
import net.opengis.sos.x10.ContentsDocument.Contents.ObservationOfferingList;
import net.opengis.sos.x10.ObservationOfferingType;

import org.apache.xmlbeans.XmlObject;
import org.n52.oxf.xml.NcNameResolver;
import org.n52.sos.encode.sos.v1.GetCapabilitiesResponseEncoder;
import org.n52.sos.exception.ows.NoApplicableCodeException;
import org.n52.sos.ioos.asset.AbstractAsset;
import org.n52.sos.ioos.asset.AssetResolver;
import org.n52.sos.ioos.asset.SensorAsset;
import org.n52.sos.ogc.gml.GmlConstants;
import org.n52.sos.ogc.gml.time.TimePeriod;
import org.n52.sos.ogc.ows.OwsExceptionReport;
import org.n52.sos.ogc.sos.SosEnvelope;
import org.n52.sos.ogc.sos.SosObservationOffering;
import org.n52.sos.ogc.swe.SweConstants;
import org.n52.sos.util.CodingHelper;
import org.n52.sos.util.CollectionHelper;

public class IoosGetCapabilitiesResponseEncoder extends GetCapabilitiesResponseEncoder {
    @Override
    protected void setContents(Contents xbContents, Collection<SosObservationOffering> offerings, String version)
            throws OwsExceptionReport {
        ObservationOfferingList xbObservationOfferings = xbContents.addNewObservationOfferingList();

        for (SosObservationOffering offering : offerings) {
            //don't encode offerings for sensors (too verbose)
            AbstractAsset asset = AssetResolver.resolveAsset(offering.getOffering());
            if (asset instanceof SensorAsset) {
                continue;
            }
                
            ObservationOfferingType xbObservationOffering = xbObservationOfferings.addNewObservationOffering();

            // TODO check Name or ID
            xbObservationOffering.setId(NcNameResolver.fixNcName(offering.getOffering()));

            // only if fois are contained for the offering set the values of the envelope
            Encoder<XmlObject, SosEnvelope> encoder =
                    CodingHelper.getEncoder(GmlConstants.NS_GML, offering.getObservedArea());
            xbObservationOffering.addNewBoundedBy().addNewEnvelope().set(encoder.encode(offering.getObservedArea()));

            // add offering name
            xbObservationOffering.addNewName().setStringValue(offering.getOffering());

            // set observableProperties [0..*]
            for (String phenomenon : offering.getObservableProperties()) {
                xbObservationOffering.addNewObservedProperty().setHref(phenomenon);
            }

            // set up time
            if (offering.getPhenomenonTime() instanceof TimePeriod) {
                XmlObject encodeObject =
                        CodingHelper.encodeObjectToXml(SweConstants.NS_SWE_101, offering.getPhenomenonTime());
                xbObservationOffering.addNewTime().set(encodeObject);
            }

            // add feature of interests
            if (offering.isSetFeatureOfInterestTypes()) {
                for (String featureOfInterestType : offering.getFeatureOfInterestTypes()) {
                    xbObservationOffering.addNewFeatureOfInterest().setHref(featureOfInterestType);
                }
            }

            // set procedure description formats
            if (offering.isSetProcedureDescriptionFormats()) {
                for (String procedureDescriptionFormat : offering.getProcedureDescriptionFormats()) {
                    xbObservationOffering.addNewProcedure().setHref(procedureDescriptionFormat);
                }
            }

            // set procedures
            if (offering.getProcedures().contains(offering.getOffering())) {
                // set only the offering's matching procedure (according to IOOS standard)                
                xbObservationOffering.addNewProcedure().setHref(offering.getOffering());
            } else {
                // if not found, add all procedures (otherwise schema invalid)
                for (String procedure : offering.getProcedures()) {
                    xbObservationOffering.addNewProcedure().setHref(procedure);
                }                
            }

            // set features of interest
            for (String featureOfInterest : offering.getFeatureOfInterest()) {
                xbObservationOffering.addNewFeatureOfInterest().setHref(featureOfInterest);
            }

            // insert result models
            Collection<QName> resultModels = offering.getResultModels();

            if (CollectionHelper.isEmpty(resultModels)) {
                throw new NoApplicableCodeException().withMessage(
                        "No result models are contained in the database for the offering: %s! Please contact the admin of this SOS.",
                                offering);
            }

            // set responseFormat [0..*]
            if (offering.isSetResponseFormats()) {
                for (String responseFormat : offering.getResponseFormats()) {
                    xbObservationOffering.addNewResponseFormat().setStringValue(responseFormat);
                }
            }

            // set response Mode
            for (String responseMode : offering.getResponseModes()) {
                xbObservationOffering.addNewResponseMode().setStringValue(responseMode);
            }
        }        
    }
}
