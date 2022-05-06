package edu.jh.pm.tic.dicom.store;

/**
 * Copyright (c) 2022 The Johns Hopkins University
 * All rights reserved
 *
 * @author David J. Talley, Technology Innovation Center, Precision Medicine Analytics Platform, Johns Hopkins Medicine
 *
 */
public interface Notification {
	/***
	 * Called after every E-GET file is saved
	 */
	public void saved(String fileName);
	/***
	 * Called after the E-GET has finished downloading all files
	 */
	public void finished();
}
