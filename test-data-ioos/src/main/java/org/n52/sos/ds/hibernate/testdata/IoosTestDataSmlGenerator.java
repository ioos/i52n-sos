package org.n52.sos.ds.hibernate.testdata;

import java.util.Collection;

import net.opengis.gml.DirectPositionType;
import net.opengis.gml.PointType;
import net.opengis.sensorML.x101.ClassificationDocument.Classification;
import net.opengis.sensorML.x101.ClassificationDocument.Classification.ClassifierList;
import net.opengis.sensorML.x101.ClassificationDocument.Classification.ClassifierList.Classifier;
import net.opengis.sensorML.x101.ContactDocument.Contact;
import net.opengis.sensorML.x101.ContactInfoDocument.ContactInfo;
import net.opengis.sensorML.x101.ContactInfoDocument.ContactInfo.Address;
import net.opengis.sensorML.x101.ContactListDocument.ContactList;
import net.opengis.sensorML.x101.DocumentDocument.Document;
import net.opengis.sensorML.x101.DocumentListDocument.DocumentList;
import net.opengis.sensorML.x101.IdentificationDocument.Identification;
import net.opengis.sensorML.x101.IdentificationDocument.Identification.IdentifierList;
import net.opengis.sensorML.x101.IdentificationDocument.Identification.IdentifierList.Identifier;
import net.opengis.sensorML.x101.InputsDocument.Inputs;
import net.opengis.sensorML.x101.InputsDocument.Inputs.InputList;
import net.opengis.sensorML.x101.IoComponentPropertyType;
import net.opengis.sensorML.x101.OutputsDocument.Outputs;
import net.opengis.sensorML.x101.OutputsDocument.Outputs.OutputList;
import net.opengis.sensorML.x101.ResponsiblePartyDocument.ResponsibleParty;
import net.opengis.sensorML.x101.SensorMLDocument;
import net.opengis.sensorML.x101.SensorMLDocument.SensorML;
import net.opengis.sensorML.x101.SystemType;
import net.opengis.sensorML.x101.TermDocument.Term;
import net.opengis.swe.x101.QuantityDocument.Quantity;

import org.apache.xmlbeans.XmlObject;
import org.n52.sos.ogc.sensorML.SensorMLConstants;
import org.n52.sos.util.XmlOptionsHelper;

import com.axiomalaska.ioos.sos.IoosDefConstants;
import com.axiomalaska.ioos.sos.IoosSosConstants;

public class IoosTestDataSmlGenerator {
    public static String createNetworkSensorMl(String id, String description) {
        SensorMLDocument xbSensorMlDoc = createSensorMlDoc(id, description);
        SystemType xbSystem = (SystemType) xbSensorMlDoc.getSensorML().getMemberArray(0).getProcess();
        addIdentification(xbSystem, IoosDefConstants.NETWORK_ID, IoosDefConstants.NETWORK_ID_DEF, id);
        addTestPublisherContact(xbSystem);
        return toText(xbSensorMlDoc);
    }

    public static String createStationSensorMl(String id, String description, String shortName, String longName,
            String platformType, String operatorSector, String publisher, String parentNetwork,
            String qualityControlDocumentName, String qualityControlDocumentLink, double lng, double lat) {
        SensorMLDocument xbSensorMlDoc = createSensorMlDoc(id, description);
        SystemType xbSystem = (SystemType) xbSensorMlDoc.getSensorML().getMemberArray(0).getProcess();
        addIdentification(xbSystem, IoosDefConstants.STATION_ID, IoosDefConstants.STATION_ID_DEF, id);        
        addIdentification(xbSystem, IoosDefConstants.SHORT_NAME, IoosDefConstants.SHORT_NAME_DEF, shortName);
        addIdentification(xbSystem, IoosDefConstants.LONG_NAME, IoosDefConstants.LONG_NAME_DEF, longName);

        addClassification(xbSystem, IoosDefConstants.PLATFORM_TYPE, IoosDefConstants.PLATFORM_TYPE_DEF,
                IoosSosConstants.PLATFORM_ONTOLOGY, platformType);
        addClassification(xbSystem, IoosDefConstants.OPERATOR_SECTOR, IoosDefConstants.OPERATOR_SECTOR_DEF,
                IoosSosConstants.SECTOR_ONTOLOGY, operatorSector);
        addClassification(xbSystem, IoosDefConstants.PUBLISHER, IoosDefConstants.PUBLISHER_DEF,
                IoosSosConstants.ORGANIZATION_ONTOLOGY, publisher);
        addClassification(xbSystem, IoosDefConstants.PARENT_NETWORK, IoosDefConstants.PARENT_NETWORK_DEF,
                IoosSosConstants.ORGANIZATION_ONTOLOGY, parentNetwork);

        addTestOperatorContact(xbSystem);
        addTestPublisherContact(xbSystem);

        if (qualityControlDocumentName != null && qualityControlDocumentLink != null) {
            addQualityControlDocument(xbSystem, qualityControlDocumentName, qualityControlDocumentLink);
        }

        addLocation(xbSystem, lng, lat);
        
        return toText(xbSensorMlDoc);
    }

    public static String createSensorSensorMl(String id, String description, String shortName, String longName,
           Collection<SimpleIo> ios ) {
        SensorMLDocument xbSensorMlDoc = createSensorMlDoc(id, description);
        SystemType xbSystem = (SystemType) xbSensorMlDoc.getSensorML().getMemberArray(0).getProcess();
        addIdentification(xbSystem, IoosDefConstants.SENSOR_ID, IoosDefConstants.SENSOR_ID_DEF, id);        
        addIdentification(xbSystem, IoosDefConstants.SHORT_NAME, IoosDefConstants.SHORT_NAME_DEF, shortName);
        addIdentification(xbSystem, IoosDefConstants.LONG_NAME, IoosDefConstants.LONG_NAME_DEF, longName);

        for (SimpleIo io : ios) {
            addIo(xbSystem, io);
        }        
        return toText(xbSensorMlDoc);
    }    

    private static String toText(XmlObject xbObject) {
        return xbObject.xmlText(XmlOptionsHelper.getInstance().getXmlOptions());
    }

    private static SensorMLDocument createSensorMlDoc(String id, String description) {
        SensorMLDocument xbSensorMlDoc = SensorMLDocument.Factory.newInstance();
        SensorML xbSensorMl = xbSensorMlDoc.addNewSensorML();
        xbSensorMl.setVersion(SensorMLConstants.VERSION_V101);
        
        SystemType xbSystem = (SystemType) xbSensorMl.addNewMember().addNewProcess()
                .substitute(SensorMLConstants.SYSTEM_QNAME, SystemType.type);
        
        xbSystem.addNewDescription().setStringValue(description);
        xbSystem.addNewName().setStringValue(id);
        return xbSensorMlDoc;
    }

    private static void addIdentification(SystemType xbSystem, String name, String definition, String value) {
        Identification xbIdentification = xbSystem.getIdentificationArray().length == 0 ?
                xbSystem.addNewIdentification() : xbSystem.getIdentificationArray(0);

        IdentifierList xbIdentifierList = xbIdentification.getIdentifierList() == null ?
                xbIdentification.addNewIdentifierList() : xbIdentification.getIdentifierList();
                
        Identifier xbIdentifier = xbIdentifierList.addNewIdentifier();
        xbIdentifier.setName(name);
        Term xbTerm = xbIdentifier.addNewTerm();
        xbTerm.setDefinition(definition);
        xbTerm.setValue(value);
    }

    private static void addClassification(SystemType xbSystem, String name, String definition, String codeSpace,
            String value) {
        Classification xbClassification = xbSystem.getClassificationArray().length == 0 ?
                xbSystem.addNewClassification() : xbSystem.getClassificationArray(0);

        ClassifierList xbClassifierList = xbClassification.getClassifierList() == null ?
                xbClassification.addNewClassifierList() : xbClassification.getClassifierList();
                
        Classifier xbClassifier = xbClassifierList.addNewClassifier();
        xbClassifier.setName(name);
        Term xbTerm = xbClassifier.addNewTerm();
        xbTerm.setDefinition(definition);
        xbTerm.addNewCodeSpace().setHref(codeSpace);
        xbTerm.setValue(value);
    }

    private static ContactList getOrAddContactList(SystemType xbSystem) {
        if (xbSystem.sizeOfContactArray() == 0){
            xbSystem.addNewContact();
        }
        Contact xbContact = xbSystem.getContactArray(0);
        if (!xbContact.isSetContactList()) {
            xbContact.addNewContactList();
        }
        return xbContact.getContactList();
    }

    private static void addTestOperatorContact(SystemType xbSystem) {
        ContactList xbContactList = getOrAddContactList(xbSystem);
        ContactList.Member xbOperator = xbContactList.addNewMember();
        xbOperator.setRole(IoosDefConstants.OPERATOR_DEF);
        ResponsibleParty xbOperatorRp = xbOperator.addNewResponsibleParty();
        xbOperatorRp.setOrganizationName("NDBC");
        ContactInfo xbOperatorCi = xbOperatorRp.addNewContactInfo();
        Address xbOperatorAddress = xbOperatorCi.addNewAddress();
        xbOperatorAddress.addNewDeliveryPoint().setStringValue("Bldg. 3205");
        xbOperatorAddress.setCity("Stennis Space Center");
        xbOperatorAddress.setAdministrativeArea("MS");
        xbOperatorAddress.setPostalCode("39529");
        xbOperatorAddress.setCountry("USA");
        xbOperatorAddress.setElectronicMailAddress("webmaster.ndbc@noaa.gov");
        xbOperatorCi.addNewOnlineResource().setHref("http://www.ndbc.noaa.gov/");
    }

    private static void addTestPublisherContact(SystemType xbSystem) {
        ContactList xbContactList = getOrAddContactList(xbSystem);
        ContactList.Member xbPublisher = xbContactList.addNewMember();
        xbPublisher.setRole(IoosDefConstants.PUBLISHER_DEF);
        ResponsibleParty xbPublisherRp = xbPublisher.addNewResponsibleParty();
        xbPublisherRp.setOrganizationName("Some Publisher");
        ContactInfo xbPublisherCi = xbPublisherRp.addNewContactInfo();
        Address xbPublisherAddress = xbPublisherCi.addNewAddress();
        xbPublisherAddress.setCountry("USA");
        xbPublisherAddress.setElectronicMailAddress("info@somepublisher.org");
        xbPublisherCi.addNewOnlineResource().setHref("http://somepublisher.org");        
    }
    
    private static void addQualityControlDocument(SystemType xbSystem, String description, String onlineResource) {
        DocumentList.Member xbDocMember = xbSystem.addNewDocumentation().addNewDocumentList().addNewMember();
        xbDocMember.setName("document1");
        xbDocMember.setArcrole("qualityControlDocument");
        Document xbDocument = xbDocMember.addNewDocument();
        xbDocument.addNewDescription().setStringValue(description);
        xbDocument.setFormat("html");
        xbDocument.addNewOnlineResource().setHref(onlineResource);
    }

    private static void addLocation(SystemType xbSystem, double lng, double lat) {
        PointType xbPoint = xbSystem.addNewSmlLocation().addNewPoint();
        xbPoint.setSrsName(IoosSosConstants.EPSG_4326_DEF);
        DirectPositionType xbPos = xbPoint.addNewPos();
        xbPos.setStringValue(lng + " " + lat);
    }
    
    private static void addIo(SystemType xbSystem, SimpleIo io) {
        Inputs xbInputs = xbSystem.getInputs() == null ?
                xbSystem.addNewInputs() : xbSystem.getInputs();

        InputList xbInputList = xbInputs.getInputList() == null ?
                xbInputs.addNewInputList() : xbInputs.getInputList();

        IoComponentPropertyType xbInput = xbInputList.addNewInput();
        xbInput.setName(io.getName());
        xbInput.addNewObservableProperty().setDefinition(io.getDefinition());
        
        Outputs xbOutputs = xbSystem.getOutputs() == null ?
                xbSystem.addNewOutputs() : xbSystem.getOutputs();

        OutputList xbOutputList = xbOutputs.getOutputList() == null ?
                xbOutputs.addNewOutputList() : xbOutputs.getOutputList();

        IoComponentPropertyType xbOutput = xbOutputList.addNewOutput();
        xbOutput.setName(io.getName());
        Quantity xbQuantity = xbOutput.addNewQuantity();
        xbQuantity.setDefinition(io.getDefinition());
        xbQuantity.addNewUom().setHref(io.getUnit());        
    }
}
