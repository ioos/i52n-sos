package org.n52.sos.encode;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import net.opengis.gml.MetaDataPropertyDocument;
import net.opengis.gml.MetaDataPropertyType;
import net.opengis.ows.x11.OperationsMetadataDocument.OperationsMetadata;

import org.apache.xmlbeans.XmlObject;
import org.n52.sos.ogc.ows.OwsExceptionReport;
import org.n52.sos.ogc.ows.OwsOperation;
import org.n52.sos.ogc.ows.OwsOperationsMetadata;
import org.n52.sos.ogc.ows.OwsParameterValue;
import org.n52.sos.ogc.sos.Sos1Constants;
import org.n52.sos.ogc.sos.Sos2Constants;
import org.n52.sos.ogc.sos.SosConstants;
import org.n52.sos.util.MultiMaps;
import org.n52.sos.util.SetMultiMap;
import org.n52.sos.util.XmlHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IoosOwsEncoderv110 extends OwsEncoderv110 {
    /** logger */
    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(IoosOwsEncoderv110.class);
    
    private static final SetMultiMap<String,String> ENCODE_PARAMETERS = initEncodeParametersMap();

    private static final SetMultiMap<String,String> initEncodeParametersMap() {
        SetMultiMap<String,String> encodeParamatersMap = MultiMaps.newSetMultiMap();
        encodeParamatersMap.add(SosConstants.Operations.GetCapabilities.name(),
                SosConstants.GetCapabilitiesParams.Sections.name());        
        encodeParamatersMap.add(SosConstants.Operations.DescribeSensor.name(),
                Sos1Constants.DescribeSensorParams.outputFormat.name());
        encodeParamatersMap.add(SosConstants.Operations.DescribeSensor.name(),
                Sos2Constants.DescribeSensorParams.procedureDescriptionFormat.name());
        return encodeParamatersMap;
    }
    
    /**
     * Augment OwsEncoderv110 operations metadata with the IOOS template version number
     * @throws OwsExceptionReport 
     * 
     */
    protected OperationsMetadata encodeOperationsMetadata(OwsOperationsMetadata operationsMetadata) throws OwsExceptionReport{
        //filter out all parameter names except those we specifically allow
        for (final OwsOperation operationMetadata : operationsMetadata.getOperations()) {
            SortedMap<String, List<OwsParameterValue>> paramMap = new TreeMap<String, List<OwsParameterValue>>();
            for (String parameterName : operationMetadata.getParameterValues().keySet()) {
                if (ENCODE_PARAMETERS.containsKey(operationMetadata.getOperationName())
                        && ENCODE_PARAMETERS.get(operationMetadata.getOperationName()).contains(parameterName)){
                    List<OwsParameterValue> paramValues = paramMap.get(operationMetadata.getOperationName());
                    if (paramValues == null) {
                        paramValues = new ArrayList<OwsParameterValue>();
                        paramMap.put(parameterName, paramValues);
                    }
                    
                    paramValues.addAll(operationMetadata.getParameterValues().get(parameterName));
                }
            }
            operationMetadata.setParameterValues(paramMap);
        }
        
    	OperationsMetadata xbMeta = super.encodeOperationsMetadata( operationsMetadata );
        XmlObject xbExtendedCapabilities = xbMeta.addNewExtendedCapabilities();
        
        //add IOOS template milestone version
        addVersionMetadata(xbExtendedCapabilities, IoosEncoderUtil.getIoosVersionMetaData());
        
        //add IOOS 52n SOS version
        addVersionMetadata(xbExtendedCapabilities, IoosEncoderUtil.getSoftwareVersionMetaData());        
    	return xbMeta;
    }
    
    private void addVersionMetadata(XmlObject xbExtendedCapabilities, MetaDataPropertyType mdpt){
        MetaDataPropertyDocument xbIoosMilestoneMetaDoc = MetaDataPropertyDocument.Factory.newInstance();
        xbIoosMilestoneMetaDoc.addNewMetaDataProperty().set(mdpt);        
        XmlHelper.append(xbExtendedCapabilities, xbIoosMilestoneMetaDoc);        
    }
}