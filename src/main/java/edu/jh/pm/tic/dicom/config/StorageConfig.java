package edu.jh.pm.tic.dicom.config;

import java.util.List;
import java.util.Map;

import org.dcm4che3.data.UID;
import org.dcm4che3.net.pdu.AAssociateRQ;
import org.dcm4che3.net.pdu.PresentationContext;
import org.dcm4che3.net.pdu.RoleSelection;
import org.dcm4che3.tool.common.CLIUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Copyright (c) 2022 The Johns Hopkins University
 * All rights reserved
 *
 * @author David J. Talley, Technology Innovation Center, Precision Medicine Analytics Platform, Johns Hopkins Medicine
 *
 */
public class StorageConfig {
    private static final Logger log = LoggerFactory.getLogger(StorageConfig.class);
    private StorageConfig() {}
    public static void setDefaultSOP(AAssociateRQ rq) {
        setSOP(rq, UID.ComputedRadiographyImageStorage,						UID.ImplicitVRLittleEndian,UID.ExplicitVRLittleEndian);
        setSOP(rq,UID.DigitalXRayImageStorageForPresentation,				UID.ImplicitVRLittleEndian,UID.ExplicitVRLittleEndian);
        setSOP(rq,UID.DigitalXRayImageStorageForProcessing,					UID.ImplicitVRLittleEndian,UID.ExplicitVRLittleEndian);
        setSOP(rq,UID.DigitalMammographyXRayImageStorageForPresentation,	UID.ImplicitVRLittleEndian,UID.ExplicitVRLittleEndian);
        setSOP(rq,UID.DigitalMammographyXRayImageStorageForProcessing,		UID.ImplicitVRLittleEndian,UID.ExplicitVRLittleEndian);
        setSOP(rq,UID.CTImageStorage,										UID.ImplicitVRLittleEndian,UID.ExplicitVRLittleEndian);
        setSOP(rq,UID.UltrasoundMultiFrameImageStorage,						UID.ImplicitVRLittleEndian,UID.ExplicitVRLittleEndian);
        setSOP(rq,UID.MRImageStorage,										UID.ImplicitVRLittleEndian,UID.ExplicitVRLittleEndian);
        setSOP(rq,UID.UltrasoundImageStorage,								UID.ImplicitVRLittleEndian,UID.ExplicitVRLittleEndian);
        setSOP(rq,UID.SecondaryCaptureImageStorage,							UID.ImplicitVRLittleEndian,UID.ExplicitVRLittleEndian);
        setSOP(rq,UID.GrayscaleSoftcopyPresentationStateStorage,			UID.ImplicitVRLittleEndian,UID.ExplicitVRLittleEndian);
        setSOP(rq,UID.XRayAngiographicImageStorage,							UID.ImplicitVRLittleEndian,UID.ExplicitVRLittleEndian);
        setSOP(rq,UID.XRayRadiofluoroscopicImageStorage,					UID.ImplicitVRLittleEndian,UID.ExplicitVRLittleEndian);
        setSOP(rq,UID.NuclearMedicineImageStorage,							UID.ImplicitVRLittleEndian,UID.ExplicitVRLittleEndian);
        setSOP(rq,UID.VLPhotographicImageStorage,							UID.ImplicitVRLittleEndian,UID.ExplicitVRLittleEndian,UID.JPEGBaseline8Bit);
        setSOP(rq,UID.VideoPhotographicImageStorage,						UID.JPEGBaseline8Bit,UID.MPEG2MPML);
        setSOP(rq,UID.VLWholeSlideMicroscopyImageStorage,					UID.JPEGBaseline8Bit,UID.JPEG2000);
        setSOP(rq,UID.BasicTextSRStorage,									UID.ImplicitVRLittleEndian,UID.ExplicitVRLittleEndian);
        setSOP(rq,UID.EnhancedSRStorage,									UID.ImplicitVRLittleEndian,UID.ExplicitVRLittleEndian);
        setSOP(rq,UID.KeyObjectSelectionDocumentStorage,					UID.ImplicitVRLittleEndian,UID.ExplicitVRLittleEndian);
        setSOP(rq,UID.XRayRadiationDoseSRStorage,							UID.ImplicitVRLittleEndian,UID.ExplicitVRLittleEndian);
        setSOP(rq,UID.EncapsulatedPDFStorage,								UID.ImplicitVRLittleEndian,UID.ExplicitVRLittleEndian);
        setSOP(rq,UID.PositronEmissionTomographyImageStorage,				UID.ImplicitVRLittleEndian,UID.ExplicitVRLittleEndian);
    }
    private static void setSOP(AAssociateRQ rq, String cuid, String... tsuids) {
    	// Convert names to UID's if necessary
    	if (!cuid.contains(".")) cuid = CLIUtils.toUID(cuid);
    	for(int i=0; i<tsuids.length; i++) {
    		if (!tsuids[i].contains(".")) tsuids[i] = CLIUtils.toUID(tsuids[i]);
    		if (tsuids[i] == UID.JPEG2000Lossless) {
    			log.warn("Transfer Syntax JPEG2000Lossless is buggy!");
    		}
    	}
        if (!rq.containsPresentationContextFor(cuid)) {
            int numpc = rq.getNumberOfPresentationContexts();
            if (numpc >= 128) log.warn("Presentation Context has the maximum 128 SOP Classes. Cannot add {}", cuid);
            else {
                rq.addRoleSelection(new RoleSelection(cuid, false, true));
                int pcid = 2 * numpc + 1;
                rq.addPresentationContext(new PresentationContext(pcid, cuid, tsuids));
            }
        }
    }
    public static void setSOPFromMap(AAssociateRQ rq, Map<String,Object> config) {
    	if (config == null) {
    		log.debug("Using Default SOP Classes");
    		setDefaultSOP(rq);
    		return;
    	}
    	for (Map.Entry<String, Object> entry : config.entrySet()) {
    		String cuid = entry.getKey();
    		Object value = entry.getValue();
    		if (value instanceof String) {
    			// allow any whitespace character except . as a delimiter
    			String[] tsuids = ((String)value).split("[^a-zA-Z0-9\\.]+");
    			log.trace("Adding SOP {}", cuid);
    			setSOP(rq, cuid, tsuids);
    		} else if (value instanceof List<?>) {
    			@SuppressWarnings("unchecked")
				List<Object> tsuids = (List<Object>)value;
    			log.trace("Adding SOP {}", cuid);
    			setSOP(rq, cuid, tsuids.toArray(new String[0]));
    		}
    	}
    }
    
}
