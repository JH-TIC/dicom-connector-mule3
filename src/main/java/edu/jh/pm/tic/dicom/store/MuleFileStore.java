package edu.jh.pm.tic.dicom.store;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.net.PDVInputStream;

import edu.jh.pm.tic.dicom.StoreUtils;

/**
 * Copyright (c) 2022 The Johns Hopkins University
 * All rights reserved
 *
 * @author David J. Talley, Technology Innovation Center, Precision Medicine Analytics Platform, Johns Hopkins Medicine
 *
 */
public class MuleFileStore implements MuleStore {
	private final String outputFilePath;
	private final Notification notification;
	private final List<String> fileList;
	public List<String> getFileList() { return fileList; }
	
	public MuleFileStore(String outputFilePath, Notification notification) throws IOException {
		this.outputFilePath = outputFilePath;
		this.notification = notification;
		fileList = new ArrayList<>();
		StoreUtils.deleteFolder(this.outputFilePath);
		Path dir = Paths.get(this.outputFilePath);
		Files.createDirectories(dir);
	}

	@Override
	public Object process(PDVInputStream payload, Map<String, Object> inboundProperties) throws Exception {
    	// Setup tags and preface
    	String iuid = (String)inboundProperties.getOrDefault("AffectedSOPInstanceUID", null);
    	String cuid = (String)inboundProperties.getOrDefault("AffectedSOPClassUID", null);
    	String tsuid = (String)inboundProperties.getOrDefault("TransferSyntaxUID", null);
    	String icuid = (String)inboundProperties.getOrDefault("ImplementationClassUID", null);
    	String ivn = (String)inboundProperties.getOrDefault("ImplementationVersionName", null);
    	String aet = (String)inboundProperties.getOrDefault("SourceApplicationEntityTitle", null);
    	Attributes fmi = StoreUtils.createFileMetaInformation(iuid, cuid, tsuid, icuid, ivn, aet);

    	// Save to the file
		Path file = Paths.get(outputFilePath, iuid + ".dcm");
    	try (OutputStream output = Files.newOutputStream(file, StandardOpenOption.CREATE)) {
        	StoreUtils.writeTo(output, payload, fmi);
    	}
    	fileList.add(file.toString());
    	if (notification != null) notification.saved(file.toString());
    	return file.toString();
	}
}
