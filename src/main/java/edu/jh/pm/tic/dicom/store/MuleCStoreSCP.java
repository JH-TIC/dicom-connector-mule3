package edu.jh.pm.tic.dicom.store;

import java.util.Map;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;
import org.dcm4che3.net.Association;
import org.dcm4che3.net.PDVInputStream;
import org.dcm4che3.net.Status;
import org.dcm4che3.net.pdu.PresentationContext;
import org.dcm4che3.net.service.BasicCStoreSCP;
import org.dcm4che3.net.service.DicomServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.jh.pm.tic.dicom.AttribUtils;

/**
 * Copyright (c) 2022 The Johns Hopkins University
 * All rights reserved
 *
 * @author David J. Talley, Technology Innovation Center, Precision Medicine Analytics Platform, Johns Hopkins Medicine
 *
 */
public class MuleCStoreSCP extends BasicCStoreSCP {
    private static final Logger log = LoggerFactory.getLogger(MuleCStoreSCP.class);
    private final MuleStore store;

	public MuleCStoreSCP(MuleStore store) {
        super();
        this.store = store;
    }
	
    @Override
    protected void store(Association as, PresentationContext pc,
                         Attributes rq, PDVInputStream data, Attributes rsp) {
        int status = -1;
        try {
        	Map<String,Object> inboundProperties = AttribUtils.attributesToMap(rq);
        	inboundProperties.put("TransferSyntaxUID", pc.getTransferSyntax());
        	inboundProperties.put("ImplementationClassUID", as.getRemoteImplClassUID());
        	inboundProperties.put("ImplementationVersionName", as.getRemoteImplVersionName());
        	inboundProperties.put("SourceApplicationEntityTitle", as.getRemoteAET());
        	Object result = store.process(data, inboundProperties);
            log.debug("{}: M-WRITE {}", as, result);
            status = Status.Success;
        } catch (Exception e) {
            log.error(as.toString() + ": M-WRITE " + e.getMessage(), e);
            status = Status.ProcessingFailure;
        }
        rsp.setInt(Tag.Status, VR.US, status);
    }

    public DicomServiceRegistry createServiceRegistry() {
        DicomServiceRegistry serviceRegistry = new DicomServiceRegistry();
        serviceRegistry.addDicomService(this);
        return serviceRegistry;
    }

}
