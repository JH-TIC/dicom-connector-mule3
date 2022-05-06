package edu.jh.pm.tic.dicom;

import edu.jh.pm.tic.dicom.config.ConnectorConfig;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.UID;
import org.dcm4che3.net.*;
import org.dcm4che3.net.pdu.AAssociateRQ;
import org.dcm4che3.net.pdu.PresentationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

class MuleStoreSCU {
    private static final Logger log = LoggerFactory.getLogger(MuleStoreSCU.class);
    private final ApplicationEntity ae;
    private final Connection conn = new Connection();
    private final Connection remote = new Connection();
    private final AAssociateRQ rq = new AAssociateRQ();
    private final Device device;
    private final ExecutorService executorService;
    private final ScheduledExecutorService scheduledExecutorService;
    private final int cancelAfter;
    private final String cuid;
    private final String tsuid;
    private Association as;
    private ScheduledFuture<?> scheduledCancel;

    public MuleStoreSCU(ConnectorConfig connector, String cuid, String tsuid, String remoteAetName, String remoteHostname, int remotePort, int cancelAfter) {
        this.cancelAfter = cancelAfter;
        this.cuid = cuid;
        this.tsuid = tsuid;
        device = new Device("StoreSCU");
        this.ae = new ApplicationEntity(connector.getAetName());
        this.as = null;
        device.addConnection(this.conn);
        device.addApplicationEntity(this.ae);
        this.ae.addConnection(this.conn);
        rq.setCalledAET(remoteAetName);
        remote.setHostname(remoteHostname);
        remote.setPort(remotePort);
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
        conn.setStoreTimeout(0);
        conn.setResponseTimeout(0);
        conn.setIdleTimeout(0);
        conn.setSocketCloseDelay(50);
        conn.setSendBufferSize(0);
        conn.setReceiveBufferSize(0);
        conn.setTcpNoDelay(true);
        int pcid = 1;
        rq.addPresentationContext(new PresentationContext(pcid++, UID.Verification, tsuid));
        rq.addPresentationContext(new PresentationContext(pcid++, cuid, tsuid));
        if (!tsuid.equals(UID.ExplicitVRLittleEndian)) rq.addPresentationContext(new PresentationContext(pcid++, cuid, UID.ExplicitVRLittleEndian));
        if (!tsuid.equals(UID.ImplicitVRLittleEndian)) rq.addPresentationContext(new PresentationContext(pcid++, cuid, UID.ImplicitVRLittleEndian));
        executorService = Executors.newSingleThreadExecutor();
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        device.setExecutor(executorService);
        device.setScheduledExecutor(scheduledExecutorService);
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

    public void open() throws IOException, InterruptedException, IncompatibleConnectionException, GeneralSecurityException {
        close();
        this.as = this.ae.connect(this.conn, this.remote, this.rq);
    }

    public MuleDimseRSPHandler cstore(String iuid, Attributes data) throws IOException, InterruptedException {
        final MuleDimseRSPHandler rspHandler = new MuleDimseRSPHandler(this.as.nextMessageID());
        this.as.cstore(cuid, iuid, 0, new DataWriterAdapter(data), tsuid, rspHandler);
        if (cancelAfter > 0) {
            this.scheduledCancel = device.schedule(() -> {
                try {
                    rspHandler.cancel(MuleStoreSCU.this.as);
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
                }

            }, cancelAfter, TimeUnit.MILLISECONDS);
        }
        return rspHandler;
    }
}
