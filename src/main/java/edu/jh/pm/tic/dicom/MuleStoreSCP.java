package edu.jh.pm.tic.dicom;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.dcm4che3.data.UID;
import org.dcm4che3.net.ApplicationEntity;
import org.dcm4che3.net.Connection;
import org.dcm4che3.net.Device;
import org.dcm4che3.net.TransferCapability;
import org.mule.api.callback.SourceCallback;

import edu.jh.pm.tic.dicom.config.ConnectorConfig;
import edu.jh.pm.tic.dicom.store.MuleCStoreSCP;
import edu.jh.pm.tic.dicom.store.MuleProcessStore;
import edu.jh.pm.tic.dicom.store.MuleStore;

/**
 * Copyright (c) 2022 The Johns Hopkins University
 * All rights reserved
 *
 * @author David J. Talley, Technology Innovation Center, Precision Medicine Analytics Platform, Johns Hopkins Medicine
 *
 */
class MuleStoreSCP {
	private final Device device = new Device("storescp");

	public void startReceiver() throws IOException, GeneralSecurityException {
		device.bindConnections();
	}
	
	public void stopReceiver() {
		device.unbindConnections();
	}
	
	public MuleStoreSCP(ConnectorConfig config, SourceCallback callback) {
        // Create the Connection
        Connection conn = new Connection();
        conn.setReceivePDULength(Connection.DEF_MAX_PDU_LENGTH);
        conn.setSendPDULength(Connection.DEF_MAX_PDU_LENGTH);
        conn.setMaxOpsInvoked(config.getMaxOpsInvoked());
        conn.setMaxOpsPerformed(config.getMaxOpsPerformed());
        conn.setPackPDV(true);
        conn.setConnectTimeout(config.getConnectionTimeout());
        conn.setRequestTimeout(config.getRequestTimeout());
        conn.setAcceptTimeout(config.getAcceptTimeout());
        conn.setReleaseTimeout(config.getReleaseTimeout());
        conn.setSendTimeout(config.getSendTimeout());
        conn.setResponseTimeout(config.getResponseTimeout());
        conn.setIdleTimeout(config.getIdleTimeout());
        conn.setSocketCloseDelay(config.getSocketCloseDelay());
        conn.setSendBufferSize(config.getSendBufferSize());
        conn.setReceiveBufferSize(config.getReceiveBufferSize());
        conn.setTcpNoDelay(true);
      	conn.setPort(config.getPort());
        conn.setHostname(config.getHostname());
        
        // Create the Application Entity
        ApplicationEntity ae = new ApplicationEntity(config.getAetName());
        ae.setAssociationAcceptor(true);
        ae.addConnection(conn);
        // Accept all transfer types
        ae.addTransferCapability(new TransferCapability((String)null, "*", TransferCapability.Role.SCP, UID.ImplicitVRLittleEndian, UID.ExplicitVRLittleEndian, UID.ExplicitVRBigEndian));

        MuleStore store = new MuleProcessStore(callback);
        MuleCStoreSCP cStoreSCP = new MuleCStoreSCP(store);
        
        // Configure the Device
        ExecutorService executorService = Executors.newCachedThreadPool();
        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        device.setDimseRQHandler(cStoreSCP.createServiceRegistry());
        device.addConnection(conn);
        device.addApplicationEntity(ae);
        device.setScheduledExecutor(scheduledExecutorService);
        device.setExecutor(executorService);        
	}

}
