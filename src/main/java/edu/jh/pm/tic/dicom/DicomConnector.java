package edu.jh.pm.tic.dicom;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;
import org.dcm4che3.io.DicomInputStream;
import org.dcm4che3.io.DicomInputStream.IncludeBulkData;
import org.dcm4che3.net.IncompatibleConnectionException;
import org.mule.api.MuleMessage;
import org.mule.api.annotations.Config;
import org.mule.api.annotations.Connector;
import org.mule.api.annotations.Processor;
import org.mule.api.annotations.Source;
import org.mule.api.annotations.SourceStrategy;
import org.mule.api.annotations.display.FriendlyName;
import org.mule.api.annotations.display.Placement;
import org.mule.api.annotations.display.Summary;
import org.mule.api.callback.SourceCallback;
import org.mule.api.transport.PropertyScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.jh.pm.tic.dicom.config.ConnectorConfig;
import edu.jh.pm.tic.dicom.config.SCUConfig;
import edu.jh.pm.tic.dicom.models.InformationModel;
import edu.jh.pm.tic.dicom.models.RetrieveLevel;
import edu.jh.pm.tic.dicom.models.TransferSyntax;
import edu.jh.pm.tic.dicom.store.MuleFileStore;
import edu.jh.pm.tic.dicom.store.Notification;

import org.mule.api.annotations.lifecycle.Stop;
import org.mule.api.annotations.param.Default;
import org.mule.api.annotations.param.Optional;

/**
 * DICOM (Digital Imaging and COmmunications in Medicine) Connector
 * Copyright (c) 2022 The Johns Hopkins University
 * All rights reserved
 *
 * @author David J. Talley, Technology Innovation Center, Precision Medicine Analytics Platform, Johns Hopkins Medicine
 *
 */
@Connector(name="dicom", friendlyName="DICOM")
public class DicomConnector {
    private static final Logger log = LoggerFactory.getLogger(DicomConnector.class);
    private static final String SOURCE_AET = "SourceApplicationEntityTitle";
    private static final String PAYLOAD_ERROR = "Payload must be of type [org.dcm4che3.data.Attributes]";
    private MuleStoreSCP storeScp = null;

    @Config
    ConnectorConfig config;
    public ConnectorConfig getConfig() { return config; }
    public void setConfig(ConnectorConfig config) { this.config = config; }
    
    @Source(friendlyName = "Store SCP", sourceStrategy = SourceStrategy.NONE)
    @Summary("Listens for C-STORE operations. Produces a org.dcm4che3.data.Attributes object for each DICOM file received.")
    public void storeScp(SourceCallback callback) throws IOException, GeneralSecurityException {
        storeScp = new MuleStoreSCP(config, callback);
        storeScp.startReceiver();
        log.debug("Started {} on port {}", config.getAetName(), config.getPort());
    }

    @Stop
    public void disconnect() {
        if (storeScp != null) {
            storeScp.stopReceiver();
            log.debug("Stopped {}", config.getAetName());
        }
    }

    @Processor(friendlyName = "Move SCU")
    @Summary("Performs C-MOVE with remote Application Entity. Search keys are read from payload Map<String,Object>.")
    public Object moveScu(@Placement(group = "Remote Connection") @FriendlyName("AE Title") @Summary("Application Entity Title") String aetName,
    		@Placement(group = "Remote Connection") @Default("0.0.0.0") String hostname,
    		@Placement(group = "Remote Connection") @Default("104") int port,
    		@Placement(group = "Presentation Context") @Summary("Can be PatientRoot, PatientStudyOnly, CompositeInstanceRoot, HangingProtocol, ColorPalette, or StudyRoot (the default)") @Default("StudyRoot") InformationModel informationModel,
    		@Placement(group = "Presentation Context") @Optional @Summary("Can be PATIENT, STUDY, SERIES, IMAGE, or FRAME") RetrieveLevel retrieveLevel,
    		@Placement(group = "Presentation Context") @Summary("Preferred compression of VR tags") @Default("ImplicitFirst") TransferSyntax transferSyntax,
    		@Placement(group = "Presentation Context") @FriendlyName("Storage SOP Classes") @Summary("List of Storage Service-Order Pair (SOP) Classes and their TransferSyntax") @Optional Map<String,Object> sopClasses,
    		@Placement(group = "Timings") @Optional @Default("0") int storeTimeout,
    		@Placement(group = "Timings") @Optional @Default("0") @Summary("Duration in milliseconds (0 is infinite)") int cancelAfter,
            MuleMessage muleMessage) throws IOException, InterruptedException, IncompatibleConnectionException, GeneralSecurityException {
    	SCUConfig remoteConnection = new SCUConfig(aetName, hostname, port, informationModel, retrieveLevel, transferSyntax, sopClasses, storeTimeout, cancelAfter);
        Attributes keys = AttribUtils.payloadToKeys(muleMessage);
    	String level = remoteConnection.getRetrieveLevelDefault();
        if (level != null) keys.setString(Tag.QueryRetrieveLevel, VR.CS, level);
        MuleDimseRSPHandler handler = null;
        MuleSCU scu = new MuleSCU("Move", remoteConnection, config, null, remoteConnection.getSopClasses());
        try {
            scu.open();
            handler = scu.cmove(remoteConnection.getInformationModelCuid("Move"), keys);
        } finally {
            scu.close();
        }
        if (handler == null) return new ArrayList<>();
        muleMessage.addProperties(handler.getResultStatus(), PropertyScope.INBOUND);
        return muleMessage.getPayload();
    }

    @Processor(friendlyName = "Find SCU")
    @Summary("Performs C-FIND with remote Application Entity. Select and Search keys are read from payload Map<String,Object>. Returns a List<Map<String,Object>>> of the results.")
    public List<Map<String,Object>> findScu(@Placement(group = "Remote Connection") @FriendlyName("AE Title") @Summary("Application Entity Title") String aetName,
    		@Placement(group = "Remote Connection") @Default("0.0.0.0") String hostname,
    		@Placement(group = "Remote Connection") @Default("104") int port,
    		@Placement(group = "Presentation Context") @Summary("Can be PatientRoot, PatientStudyOnly, CompositeInstanceRoot, HangingProtocol, ColorPalette, or StudyRoot (the default)") @Default("StudyRoot") InformationModel informationModel,
    		@Placement(group = "Presentation Context") @Optional @Summary("Can be PATIENT, STUDY, SERIES, IMAGE, or FRAME") RetrieveLevel retrieveLevel,
    		@Placement(group = "Presentation Context") @Summary("Preferred compression of VR tags") @Default("ImplicitFirst") TransferSyntax transferSyntax,
    		@Placement(group = "Timings") @Optional @Default("0") @Summary("Duration in milliseconds (0 is infinite)") int cancelAfter,
    		MuleMessage muleMessage) throws IOException, InterruptedException, IncompatibleConnectionException, GeneralSecurityException {
    	SCUConfig remoteConnection = new SCUConfig(aetName, hostname, port, informationModel, retrieveLevel, transferSyntax, new HashMap<>(), 0, cancelAfter);
        Attributes keys = AttribUtils.payloadToKeys(muleMessage);
    	String level = remoteConnection.getRetrieveLevelDefault();
        if (level != null) keys.setString(Tag.QueryRetrieveLevel, VR.CS, level);
        MuleDimseRSPHandler handler = null;
        MuleSCU scu = new MuleSCU("Find", remoteConnection, config, null, remoteConnection.getSopClasses());
        try {
            scu.open();
            handler = scu.cfind(remoteConnection.getInformationModelCuid("Find"), keys);
        } finally {
            scu.close();
        }
        if (handler == null) return new ArrayList<>();
        muleMessage.addProperties(handler.getResultStatus(), PropertyScope.INBOUND);
        return handler.getResultData();
    }

    @Processor(friendlyName = "Get SCU")
    @Summary("Performs C-GET with a remote Application Entity. Search keys are read from payload Map<String,Object>. Returns a list of full file paths.")
    public List<String> getScu(@Placement(group = "Remote Connection") @FriendlyName("AE Title") @Summary("Application Entity Title") String aetName,
    		@Placement(group = "Remote Connection") @Default("0.0.0.0") String hostname,
    		@Placement(group = "Remote Connection") @Default("104") int port,
    		@Placement(group = "Presentation Context") @Summary("Can be PatientRoot, PatientStudyOnly, CompositeInstanceRoot, HangingProtocol, ColorPalette, or StudyRoot (the default)") @Default("StudyRoot") InformationModel informationModel,
    		@Placement(group = "Presentation Context") @Optional @Summary("Can be PATIENT, STUDY, SERIES, IMAGE, or FRAME") RetrieveLevel retrieveLevel,
    		@Placement(group = "Presentation Context") @Summary("Preferred compression of VR tags") @Default("ImplicitFirst") TransferSyntax transferSyntax,
    		@Placement(group = "Presentation Context") @FriendlyName("Storage SOP Classes") @Summary("List of Storage Service-Order Pair (SOP) Classes and their TransferSyntax") @Optional Map<String,Object> sopClasses,
    		@Placement(group = "Timings") @Optional @Default("0") int storeTimeout,
    		@Placement(group = "Timings") @Optional @Default("0") @Summary("Duration in milliseconds (0 is infinite)") int cancelAfter,
            @Summary("Folder where all files are saved") String outputFilePath,
            @Optional @Summary("Instance of a class that implements edu.jh.pm.dicom.store.Notification") Notification notification,
            MuleMessage muleMessage) throws IOException, InterruptedException, IncompatibleConnectionException, GeneralSecurityException {
    	SCUConfig remoteConnection = new SCUConfig(aetName, hostname, port, informationModel, retrieveLevel, transferSyntax, sopClasses, storeTimeout, cancelAfter);
    	Attributes keys = AttribUtils.payloadToKeys(muleMessage);
    	String level = remoteConnection.getRetrieveLevelDefault();
        if (level != null) keys.setString(Tag.QueryRetrieveLevel, VR.CS, level);
        MuleDimseRSPHandler handler = null;
        MuleFileStore fileStore = new MuleFileStore(outputFilePath, notification);
        MuleSCU scu = new MuleSCU("Get", remoteConnection, config, fileStore, remoteConnection.getSopClasses());
        try {
            scu.open();
            handler = scu.cget(remoteConnection.getInformationModelCuid("Get"), keys);
        } finally {
            scu.close();
        }
        if (notification != null) notification.finished();
        if (handler != null) {
            // C-GET incorrectly reports everything as failed. This is a hack to autocorrect.
            handler.updateResults(fileStore.getFileList().size());
            muleMessage.addProperties(handler.getResultStatus(), PropertyScope.INBOUND);
            return fileStore.getFileList();
        } else {
            StoreUtils.deleteFolder(outputFilePath);
            return new ArrayList<>();
        }
    }
    
    @Processor(friendlyName = "Store SCU")
    @Summary("Performs C-STORE with a remote Application Entity.")
    public List<Map<String,Object>> storeScu(
    		@Placement(group = "Remote Connection") @FriendlyName("AE Title") @Summary("Application Entity Title") String aetName, 
    		@Placement(group = "Remote Connection") @Default("0.0.0.0") String hostname, 
    		@Placement(group = "Remote Connection") @Default("104") int port, 
    		@Placement(group = "Timings") @Default("0") @Summary("Duration in milliseconds (0 is infinite)") int cancelAfter, 
    		MuleMessage muleMessage) throws IOException, InterruptedException, IncompatibleConnectionException, GeneralSecurityException {
        MuleDimseRSPHandler handler = null;
        Object payload = muleMessage.getPayload();
        if (!(payload instanceof Attributes)) throw new IOException(PAYLOAD_ERROR);
        Attributes data = (Attributes)payload;
        
        String iuid = getProperty("AffectedSOPInstanceUID", muleMessage);
        if (iuid == null) iuid = getProperty("MediaStorageSOPInstanceUID", muleMessage);
        if (iuid == null) iuid = data.getString(Tag.SOPInstanceUID);
        
        String cuid = getProperty("AffectedSOPClassUID", muleMessage);
        if (cuid == null) cuid = getProperty("MediaStorageSOPClassUID", muleMessage);
        if (cuid == null) cuid = data.getString(Tag.SOPClassUID);
        
        String tsuid = getProperty("TransferSyntaxUID", muleMessage);
        if (tsuid == null) tsuid = data.getString(Tag.TransferSyntaxUID);
        if (tsuid == null) throw new IOException("Missing TransferSyntaxUID from inbound or outbound properties");
        
        MuleStoreSCU scu = new MuleStoreSCU(config, cuid, tsuid, aetName, hostname, port, cancelAfter);
    	try {
    		scu.open();
    		handler = scu.cstore(iuid, data);
    	} finally {
    		scu.close();
    	}
        if (handler == null) return new ArrayList<>();
        muleMessage.addProperties(handler.getResultStatus(), PropertyScope.INBOUND);
        return handler.getResultData();
    }

    private String getProperty(String name, MuleMessage muleMessage) {
        String value = muleMessage.getInboundProperty(name);
        if (value == null || value.isEmpty()) value = muleMessage.getOutboundProperty(name);
        return value;
    }

    @Processor(friendlyName = "Store File")
    @Summary("Saves org.dcm4che3.data.Attributes as a DICOM file.")
    public String storeFile(String filePath, @Optional @Summary("Defaults to InstanceUID.dcm") String fileName, MuleMessage muleMessage) throws IOException {
        Object payload = muleMessage.getPayload();
        if (!(payload instanceof Attributes)) throw new IOException(PAYLOAD_ERROR);
        Attributes data = (Attributes)payload;
        // Setup tags and preface
        String iuid = getProperty("AffectedSOPInstanceUID", muleMessage);
        if (iuid == null) iuid = getProperty("MediaStorageSOPInstanceUID", muleMessage);
        if (iuid == null) iuid = data.getString(Tag.SOPInstanceUID);
        
        String cuid = getProperty("AffectedSOPClassUID", muleMessage);
        if (cuid == null) cuid = getProperty("MediaStorageSOPClassUID", muleMessage);
        if (cuid == null) cuid = data.getString(Tag.SOPClassUID);
        
        String tsuid = getProperty("TransferSyntaxUID", muleMessage);
        if (tsuid == null) tsuid = data.getString(Tag.TransferSyntaxUID);
        if (tsuid == null) throw new IOException("Missing TransferSyntaxUID from inbound or outbound properties");
        
        String icuid = getProperty("ImplementationClassUID", muleMessage);
        if (icuid == null) icuid = data.getString(Tag.ImplementationClassUID);
        String ivn = getProperty("ImplementationVersionName", muleMessage);
        if (ivn == null) ivn = data.getString(Tag.ImplementationVersionName);
        String aet = getProperty(SOURCE_AET, muleMessage);
        if (aet == null) aet = config.getAetName();

        // Save to the file
        Path dir = Paths.get(filePath);
        if (!Files.exists(dir)) Files.createDirectories(dir);
        Path file;
        if (fileName == null || fileName.isEmpty()) {
            file = Paths.get(filePath, String.format("%s.dcm", iuid));
        } else {
            file = Paths.get(filePath, fileName);
        }

        Attributes fmi = StoreUtils.createFileMetaInformation(iuid, cuid, tsuid, icuid, ivn, aet);
        try (OutputStream output = Files.newOutputStream(file, StandardOpenOption.CREATE)) {
        	StoreUtils.writeTo(output,  data, fmi);
        }
        return file.toString();
    }
    
    @Processor(friendlyName = "Read File")
    @Summary("Reads a DICOM file as org.dcm4che3.data.Attributes")
    public Attributes readFile(String fileName, MuleMessage muleMessage) throws IOException {
    	File file = new File(fileName);
    	try (DicomInputStream dis = new DicomInputStream(file)) {
    		dis.setIncludeBulkData(IncludeBulkData.URI);
    		Attributes fmi = dis.getFileMetaInformation();
    		Map<String, Object> properties = AttribUtils.attributesToMap(fmi);
    	    if (properties.containsKey(SOURCE_AET)) properties.replace(SOURCE_AET, config.getAetName());
    	    else properties.put(SOURCE_AET, config.getAetName());
            muleMessage.addProperties(properties, PropertyScope.INBOUND);
    		return dis.readDataset();
    	}
    }

    @Processor(friendlyName = "Set Tags")
    @Summary("Sets tags on a DICOM file")
    public Attributes setTags(Map<String, Object> tags, MuleMessage muleMessage) throws IOException {
        Object payload = muleMessage.getPayload();
        if (!(payload instanceof Attributes)) throw new IOException(PAYLOAD_ERROR);
        Attributes data = (Attributes)payload;
        AttribUtils.addKeys(data, tags);
        muleMessage.setProperty(SOURCE_AET, config.getAetName(), PropertyScope.INBOUND);
        return data;
    }
}