package org.n52.sos.encode;

import net.opengis.gml.BoundingShapeType;
import net.opengis.gml.DirectPositionType;
import net.opengis.gml.EnvelopeType;
import net.opengis.gml.MetaDataPropertyType;
import net.opengis.gml.VersionDocument;

import org.n52.sos.ioos.Ioos52nSosVersionHandler;
import org.n52.sos.util.XmlHelper;
import org.n52.sos.util.XmlOptionsHelper;

import com.axiomalaska.ioos.sos.IoosSosConstants;
import com.vividsolutions.jts.geom.Envelope;

/**
 * Shared IOOS encoding methods
 */
public class IoosEncoderUtil {
    public static MetaDataPropertyType getIoosVersionMetaData(){
        return getVersionMetaData(IoosSosConstants.IOOS_TEMPLATE_VERSION,
                IoosSosConstants.IOOS_VERSION_DEFINITION, IoosSosConstants.IOOS_VERSION_M10);
    }

    public static MetaDataPropertyType getSoftwareVersionMetaData(){
        return getVersionMetaData(IoosSosConstants.SOFTWARE_VERSION, null,
                Ioos52nSosVersionHandler.getIoosVersion());
    }
    
    public static MetaDataPropertyType getVersionMetaData(String title, String definition, String version){
        MetaDataPropertyType xb_versionMetadataProperty = MetaDataPropertyType.Factory.newInstance();
        if (title != null) {
            xb_versionMetadataProperty.setTitle( title );
        }
        if (definition != null) {
            xb_versionMetadataProperty.setHref( definition );
        }
        
        if (version == null) {
            version = "unknown";
        }

        VersionDocument xb_versionDoc = VersionDocument.Factory.newInstance();
        xb_versionDoc.setVersion( version );
        XmlHelper.append( xb_versionMetadataProperty, xb_versionDoc );
        return xb_versionMetadataProperty;
    }    

    
   	public static BoundingShapeType createBoundedBy( Envelope envelope ){
    	BoundingShapeType xb_boundingShape = BoundingShapeType.Factory.newInstance(XmlOptionsHelper.getInstance().getXmlOptions());
        EnvelopeType xb_envelope = xb_boundingShape.addNewEnvelope();

        DirectPositionType xb_lowerCorner = xb_envelope.addNewLowerCorner();
		xb_lowerCorner.setStringValue(envelope.getMinY() + " " + envelope.getMinX());
        DirectPositionType xb_upperCorner = xb_envelope.addNewUpperCorner();
        xb_upperCorner.setStringValue(envelope.getMaxY() + " " + envelope.getMaxX());
        
        xb_envelope.setSrsName( IoosSosConstants.SRS_URL_PREFIX + 4326 );
        return xb_boundingShape;   		
   	}
}
