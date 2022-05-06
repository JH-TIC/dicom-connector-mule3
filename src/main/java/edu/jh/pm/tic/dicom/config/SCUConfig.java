package edu.jh.pm.tic.dicom.config;

import java.lang.reflect.Field;
import java.util.Map;

import org.dcm4che3.data.UID;

import edu.jh.pm.tic.dicom.models.InformationModel;
import edu.jh.pm.tic.dicom.models.RetrieveLevel;
import edu.jh.pm.tic.dicom.models.TransferSyntax;

/**
 * Copyright (c) 2022 The Johns Hopkins University
 * All rights reserved
 *
 * @author David J. Talley, Technology Innovation Center, Precision Medicine Analytics Platform, Johns Hopkins Medicine
 *
 */
public class SCUConfig {
	public SCUConfig() {}
	public SCUConfig(String aetName, String hostname, int port, 
			InformationModel informationModel, RetrieveLevel retrieveLevel, TransferSyntax transferSyntax, 
			Map<String,Object> sopClasses, int storeTimeout, int cancelAfter) {
		this.aetName = aetName;
		this.hostname = hostname;
		this.port = port;
		this.informationModel = informationModel;
		this.retrieveLevel = retrieveLevel;
		this.transferSyntax = transferSyntax;
		this.sopClasses = sopClasses;
		this.storeTimeout = storeTimeout;
		this.cancelAfter = cancelAfter;
	}
	
    private String aetName;
    public String getAetName() { return aetName; }
    public void setAetName(String aetName) { this.aetName = aetName; }

    private String hostname;
    public String getHostname() { return hostname; }
    public void setHostname(String hostname) { this.hostname = hostname; }

    private int port;
    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

    private InformationModel informationModel;
    public InformationModel getInformationModel() { return informationModel; }
    public void setInformationModel(InformationModel informationModel) { this.informationModel = informationModel; }
    public String getInformationModelCuid(String operation) {
    	if (operation == "Store") return UID.Verification;
    	String cuidType;
    	switch (informationModel) {
			case CompositeInstanceRoot:
				cuidType = "Retrieve";
				break;
			case HangingProtocol:
				cuidType = "InformationModel";
				break;
			default:
				cuidType = "QueryRetrieveInformationModel";
				break;
    	}
    	String cuidName = informationModel.name() + cuidType + operation;
    	String cuid;
    	// Lookup the CUID in the UID class
    	try {
    		Field field = UID.class.getField(cuidName);
    		cuid = (String)field.get(null);
    	} catch (NoSuchFieldException | IllegalAccessException ignore) {
    		cuid = "";
    	}
    	return cuid;
    }
    
    private RetrieveLevel retrieveLevel;
    public RetrieveLevel getRetrieveLevel() { return retrieveLevel; }
    public void setRetrieveLevel(RetrieveLevel retrieveLevel) { this.retrieveLevel = retrieveLevel; }
    public String getRetrieveLevelDefault() {
    	String level;
    	if (retrieveLevel == null) {
    		switch (informationModel) {
    			case CompositeInstanceRoot:
    				level = RetrieveLevel.IMAGE.name();
    				break;
    			case HangingProtocol:
    			case ColorPalette:
    				level = null;
    				break;
    			default:
    				level = RetrieveLevel.STUDY.name();
    				break;
    		}
    	} else level = retrieveLevel.name();
    	return level;
    }
    
    private TransferSyntax transferSyntax;
    public TransferSyntax getTransferSyntax() { return transferSyntax; }
    public void setTransferSyntax(TransferSyntax transferSyntax) { this.transferSyntax = transferSyntax; }
    public String[] getTransferSyntaxCodes() {
    	String[] codes;
    	switch (transferSyntax) {
    		case ImplicitFirst:
    			codes = new String[] { UID.ImplicitVRLittleEndian, UID.ExplicitVRLittleEndian, UID.ExplicitVRBigEndian };
    			break;
    		case ExplicitFirst:
    			codes = new String[] { UID.ExplicitVRLittleEndian, UID.ExplicitVRBigEndian, UID.ImplicitVRLittleEndian };
    			break;
    		case ImplicitOnly:
    			codes = new String[] { UID.ImplicitVRLittleEndian };
    			break;
    		case ExplicitOnly:
    			codes = new String[] { UID.ExplicitVRLittleEndian };
    			break;
    		default:
    			codes = new String[] { UID.ImplicitVRLittleEndian, UID.ExplicitVRLittleEndian, UID.ExplicitVRBigEndian };
    			break;
    	}
    	return codes;
    }
    
    private Map<String,Object> sopClasses;
    public Map<String,Object> getSopClasses() { return sopClasses; }
    public void setSopClasses(Map<String,Object> sopClasses) { this.sopClasses = sopClasses; }
    
    private int storeTimeout;
    public int getStoreTimeout() { return storeTimeout; }
    public void setStoreTimeout(int storeTimeout) { this.storeTimeout = storeTimeout; }
    
    private int cancelAfter;
    public int getCancelAfter() { return cancelAfter; }
    public void setCancelAfter(int cancelAfter) { this.cancelAfter = cancelAfter; }
    
}
