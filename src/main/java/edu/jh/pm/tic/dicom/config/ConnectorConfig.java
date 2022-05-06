package edu.jh.pm.tic.dicom.config;

import org.mule.api.annotations.components.Configuration;
import org.mule.api.annotations.display.FriendlyName;
import org.mule.api.annotations.display.Placement;
import org.mule.api.annotations.display.Summary;
import org.mule.api.annotations.Configurable;
import org.mule.api.annotations.Required;
import org.mule.api.annotations.param.Default;
import org.mule.api.annotations.param.Optional;

/**
 * Copyright (c) 2022 The Johns Hopkins University
 * All rights reserved
 *
 * @author David J. Talley, Technology Innovation Center, Precision Medicine Analytics Platform, Johns Hopkins Medicine
 *
 */
@Configuration(friendlyName = "Store SCP Configuration")
public class ConnectorConfig {
    @Configurable
    @Placement(tab = "General", group = "Connection")
    @FriendlyName("AE Title")
    @Summary("Application Entity Title")
    @Default("MULE_CONNECTOR")
    @Required
    private String aetName;
    public String getAetName() { return aetName; }
    public void setAetName(String aetName) { this.aetName = aetName; }

    @Configurable
    @Placement(tab = "General", group = "Connection")
    @Optional
    @Default("0.0.0.0")
    private String hostname;
    public String getHostname() { if (hostname == null || hostname == "") return "0.0.0.0"; else return hostname; }
    public void setHostname(String hostname) { this.hostname = hostname; }

    @Configurable
    @Placement(tab = "General", group = "Connection")
    @Default("104")
    @Required
    private int port;
    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

    @Configurable
    @Placement(tab = "General", group = "Store SCP")
    @Summary("Max Operations Invoked. Defaults to 0 (unlimited)")
    @Optional
    @Default("0")
    private int maxOpsInvoked;
    public int getMaxOpsInvoked() { return maxOpsInvoked; }
    public void setMaxOpsInvoked(int maxOpsInvoked) { this.maxOpsInvoked = maxOpsInvoked; }

    @Configurable
    @Placement(tab = "General", group = "Store SCP")
    @Summary("Max Operations Performed. Defaults to 0 (unlimited)")
    @Optional
    @Default("0")
    private int maxOpsPerformed;
    public int getMaxOpsPerformed() { return maxOpsPerformed; }
    public void setMaxOpsPerformed(int maxOpsPerformed) { this.maxOpsPerformed = maxOpsPerformed; }

    @Configurable
    @Placement(tab = "Protocol", group = "Buffer")
    @FriendlyName("Receive PDU Length")
    @Summary("Receive Protocol Data Units Length. Default is the max (16378)")
    @Optional
    @Default("16378")
    private int receivePduLength;
    public int getReceivePduLength() { return receivePduLength; }
    public void setReceivePduLength(int receivePduLength) { this.receivePduLength = receivePduLength; }

    @Configurable
    @Placement(tab = "Protocol", group = "Buffer")
    @FriendlyName("Send PDU Length")
    @Summary("Send Protocol Data Units Length. Default is the max (16378)")
    @Optional
    @Default("16378")
    private int sendPduLength;
    public int getSendPduLength() { return sendPduLength; }
    public void setSendPduLength(int sendPduLength) { this.sendPduLength = sendPduLength; }
    
    @Configurable
    @Placement(tab = "Protocol", group = "Buffer")
    @Summary("Default is 0 (unlimited)")
    @Optional
    @Default("0")
    private int sendBufferSize;
    public int getSendBufferSize() { return sendBufferSize; }
    public void setSendBufferSize(int sendBufferSize) { this.sendBufferSize = sendBufferSize; }

    @Configurable
    @Placement(tab = "Protocol", group = "Buffer")
    @Summary("Default is 0 (unlimited)")
    @Optional
    @Default("0")
    private int receiveBufferSize;
    public int getReceiveBufferSize() { return receiveBufferSize; }
    public void setReceiveBufferSize(int receiveBufferSize) { this.receiveBufferSize = receiveBufferSize; }

    @Configurable
    @Placement(tab = "Protocol", group = "Timings")
    @Summary("Default is 0 (unlimited)")
    @Optional
    @Default("0")
    private int connectionTimeout;
    public int getConnectionTimeout() { return connectionTimeout; }
    public void setConnectionTimeout(int connectionTimeout) { this.connectionTimeout = connectionTimeout; }

    @Configurable
    @Placement(tab = "Protocol", group = "Timings")
    @Summary("Default is 0 (unlimited)")
    @Optional
    @Default("0")
    private int requestTimeout;
    public int getRequestTimeout() { return requestTimeout; }
    public void setRequestTimeout(int requestTimeout) { this.requestTimeout = requestTimeout; }

    @Configurable
    @Placement(tab = "Protocol", group = "Timings")
    @Summary("Default is 0 (unlimited)")
    @Optional
    @Default("0")
    private int acceptTimeout;
    public int getAcceptTimeout() { return acceptTimeout; }
    public void setAcceptTimeout(int acceptTimeout) { this.acceptTimeout = acceptTimeout; }

    @Configurable
    @Placement(tab = "Protocol", group = "Timings")
    @Summary("Default is 0 (unlimited)")
    @Optional
    @Default("0")
    private int releaseTimeout;
    public int getReleaseTimeout() { return releaseTimeout; }
    public void setReleaseTimeout(int releaseTimeout) { this.releaseTimeout = releaseTimeout; }

    @Configurable
    @Placement(tab = "Protocol", group = "Timings")
    @Summary("Default is 0 (unlimited)")
    @Optional
    @Default("0")
    private int sendTimeout;
    public int getSendTimeout() { return sendTimeout; }
    public void setSendTimeout(int sendTimeout) { this.sendTimeout = sendTimeout; }

    @Configurable
    @Placement(tab = "Protocol", group = "Timings")
    @Summary("Default is 0 (unlimited)")
    @Optional
    @Default("0")
    private int responseTimeout;
    public int getResponseTimeout() { return responseTimeout; }
    public void setResponseTimeout(int responseTimeout) { this.responseTimeout = responseTimeout; }

    @Configurable
    @Placement(tab = "Protocol", group = "Timings")
    @Summary("Default is 0 (unlimited)")
    @Optional
    @Default("0")
    private int idleTimeout;
    public int getIdleTimeout() { return idleTimeout; }
    public void setIdleTimeout(int idleTimeout) { this.idleTimeout = idleTimeout; }

    @Configurable
    @Placement(tab = "Protocol", group = "Timings")
    @Summary("Default is 50")
    @Optional
    @Default("0")
    private int socketCloseDelay;
    public int getSocketCloseDelay() { return socketCloseDelay; }
    public void setSocketCloseDelay(int socketCloseDelay) { this.socketCloseDelay = socketCloseDelay; }
}