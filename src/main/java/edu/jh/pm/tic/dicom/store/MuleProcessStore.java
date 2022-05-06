package edu.jh.pm.tic.dicom.store;

//import java.io.ByteArrayOutputStream;
import java.util.Map;

import org.dcm4che3.net.PDVInputStream;
import org.mule.api.callback.SourceCallback;

/**
 * Copyright (c) 2022 The Johns Hopkins University
 * All rights reserved
 *
 * @author David J. Talley, Technology Innovation Center, Precision Medicine Analytics Platform, Johns Hopkins Medicine
 *
 */
public class MuleProcessStore implements MuleStore {
	private final SourceCallback callback;

	public MuleProcessStore(SourceCallback callback) {
		this.callback = callback;
	}
	
	@Override
	public Object process(PDVInputStream payload, Map<String, Object> inboundProperties) throws Exception {
		String tsuid = (String)inboundProperties.get("TransferSyntaxUID");
		return callback.process(payload.readDataset(tsuid), inboundProperties);
	}

}
