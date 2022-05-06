package edu.jh.pm.tic.dicom;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.net.ApplicationEntity;
import org.dcm4che3.net.Association;
import org.dcm4che3.net.Connection;
import org.dcm4che3.net.DataWriterAdapter;
import org.dcm4che3.net.Device;
import org.dcm4che3.net.IncompatibleConnectionException;
import org.dcm4che3.net.pdu.AAssociateRQ;
import org.dcm4che3.net.pdu.PresentationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.jh.pm.tic.dicom.config.ConnectorConfig;
import edu.jh.pm.tic.dicom.config.SCUConfig;
import edu.jh.pm.tic.dicom.config.StorageConfig;
import edu.jh.pm.tic.dicom.store.MuleCStoreSCP;
import edu.jh.pm.tic.dicom.store.MuleStore;

/**
 * Copyright (c) 2022 The Johns Hopkins University
 * All rights reserved
 *
 * @author David J. Talley, Technology Innovation Center, Precision Medicine Analytics Platform, Johns Hopkins Medicine
 *
 */
class MuleSCU {
    private static final Logger log = LoggerFactory.getLogger(MuleSCU.class);
    private final ApplicationEntity ae;
    private final Connection conn = new Connection();
    private final Connection remote = new Connection();
    private final AAssociateRQ rq = new AAssociateRQ();
    private final Device device;
    private final ExecutorService executorService;
    private final ScheduledExecutorService scheduledExecutorService;
    private final int cancelAfter;
    private Association as;
    private ScheduledFuture<?> scheduledCancel;

    public MuleSCU(String operation, SCUConfig config, ConnectorConfig connector, MuleStore store, Map<String,Object> storageSOP) {
        cancelAfter = config.getCancelAfter();
        device = new Device(operation + "SCU");
        this.ae = new ApplicationEntity(connector.getAetName());
        this.as = null;
        device.addConnection(this.conn);
        device.addApplicationEntity(this.ae);
        this.ae.addConnection(this.conn);
        if (store != null) {
            MuleCStoreSCP cStoreSCP = new MuleCStoreSCP(store);
            device.setDimseRQHandler(cStoreSCP.createServiceRegistry());
        }
        rq.setCalledAET(config.getAetName());
        remote.setHostname(config.getHostname());
        remote.setPort(config.getPort());
        remote.setHttpProxy(null);
        conn.setReceivePDULength(16378);
        conn.setSendPDULength(16378);
        conn.setMaxOpsInvoked(0);
        conn.setMaxOpsPerformed(0);
        conn.setPackPDV(true);
        conn.setConnectTimeout(0);
        conn.setRequestTimeout(0);
        conn.setAcceptTimeout(0);
        conn.setReleaseTimeout(0);
        conn.setSendTimeout(0);
        conn.setStoreTimeout(config.getStoreTimeout());
        conn.setResponseTimeout(0);
        conn.setIdleTimeout(0);
        conn.setSocketCloseDelay(50);
        conn.setSendBufferSize(0);
        conn.setReceiveBufferSize(0);
        conn.setTcpNoDelay(true);
        PresentationContext pc = new PresentationContext(1, config.getInformationModelCuid(operation), config.getTransferSyntaxCodes());
        rq.addPresentationContext(pc);
        StorageConfig.setSOPFromMap(rq, storageSOP);
        executorService = Executors.newSingleThreadExecutor();
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        device.setExecutor(executorService);
        device.setScheduledExecutor(scheduledExecutorService);
    }

    public void open() throws IOException, InterruptedException, IncompatibleConnectionException, GeneralSecurityException {
        close();
        this.as = this.ae.connect(this.conn, this.remote, this.rq);
    }

    public void close() throws InterruptedException {
        if (this.scheduledCancel == null && this.as != null && this.as.isReadyForDataTransfer()) {
            this.as.waitForOutstandingRSP();
            try {
                this.as.release();
            } catch (IOException ex) {
                if (log.isTraceEnabled()) log.trace("Ignored exception {}", ex.toString());
            }
            this.as = null;
            this.scheduledCancel = null;
            executorService.shutdown();
            scheduledExecutorService.shutdown();
        }
    }

    public MuleDimseRSPHandler cmove(String cuid, Attributes keys) throws IOException, InterruptedException {
        final MuleDimseRSPHandler rspHandler = new MuleDimseRSPHandler(this.as.nextMessageID());
        this.as.cmove(cuid, 0, keys, (String)null, this.ae.getAETitle(), rspHandler);
        if (cancelAfter > 0) {
            this.scheduledCancel = device.schedule(() -> {
                try {
                    rspHandler.cancel(MuleSCU.this.as);
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
                }

            }, cancelAfter, TimeUnit.MILLISECONDS);
        }
        return rspHandler;
    }

    public MuleDimseRSPHandler cfind(String cuid, Attributes keys) throws IOException, InterruptedException {
        final MuleDimseRSPHandler rspHandler = new MuleDimseRSPHandler(this.as.nextMessageID());
        this.as.cfind(cuid, 0, keys, (String)null, rspHandler);
        if (cancelAfter > 0) {
            this.scheduledCancel = device.schedule(() -> {
                try {
                    rspHandler.cancel(MuleSCU.this.as);
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
                }

            }, cancelAfter, TimeUnit.MILLISECONDS);
        }
        return rspHandler;
    }

    public MuleDimseRSPHandler cget(String cuid, Attributes keys) throws IOException, InterruptedException {
        final MuleDimseRSPHandler rspHandler = new MuleDimseRSPHandler(this.as.nextMessageID());
        this.as.cget(cuid, 0, keys, (String)null, rspHandler);
        if (cancelAfter > 0) {
            this.scheduledCancel = device.schedule(() -> {
                try {
                    rspHandler.cancel(MuleSCU.this.as);
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
                }

            }, cancelAfter, TimeUnit.MILLISECONDS);
        }
        return rspHandler;
    }
}
