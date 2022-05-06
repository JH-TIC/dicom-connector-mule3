package edu.jh.pm.tic.dicom.store;

import java.util.Map;

import org.dcm4che3.net.PDVInputStream;

/**
 * Copyright (c) 2022 The Johns Hopkins University
 * All rights reserved
 *
 * @author David J. Talley, Technology Innovation Center, Precision Medicine Analytics Platform, Johns Hopkins Medicine
 *
 */
public interface MuleStore {
	public Object process(PDVInputStream payload, Map<String,Object> inboundProperties) throws Exception;
}
