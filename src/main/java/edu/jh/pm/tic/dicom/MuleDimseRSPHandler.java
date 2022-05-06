package edu.jh.pm.tic.dicom;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.net.Association;
import org.dcm4che3.net.DimseRSPHandler;
import org.dcm4che3.net.Status;

/**
 * Copyright (c) 2022 The Johns Hopkins University
 * All rights reserved
 *
 * @author David J. Talley, Technology Innovation Center, Precision Medicine Analytics Platform, Johns Hopkins Medicine
 *
 */
class MuleDimseRSPHandler extends DimseRSPHandler {
    private static final String STATUS_TEXT = "StatusText";
    private static final String MESSAGE_ID = "MessageID";
    private int status = -1;
    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public MuleDimseRSPHandler(int msgId) {
        super(msgId);
    }

    private final Map<String,Object> resultStatus = new HashMap<>();
    public Map<String,Object> getResultStatus() { return resultStatus; }
    
    private final List<Map<String,Object>> resultData = new ArrayList<>();
    public List<Map<String,Object>> getResultData() { return resultData; }

    @Override
    public void onDimseRSP(Association as, Attributes cmd, Attributes data) {
        super.onDimseRSP(as, cmd, data);
        if (resultStatus.containsKey(MESSAGE_ID)) resultStatus.replace(MESSAGE_ID, this.getMessageID());
        else resultStatus.put(MESSAGE_ID, this.getMessageID());
        if (cmd != null && !cmd.isEmpty()) {
            status = cmd.getInt(Tag.Status, -1);
            if (resultStatus.containsKey(STATUS_TEXT)) resultStatus.replace(STATUS_TEXT, getStatusText());
            else resultStatus.put(STATUS_TEXT, getStatusText());
        	Map<String,Object> cmdMap = AttribUtils.attributesToMap(cmd);
        	for (Map.Entry<String, Object> entry : cmdMap.entrySet()) {
        		if (resultStatus.containsKey(entry.getKey())) resultStatus.replace(entry.getKey(), entry.getValue());
        		else resultStatus.put(entry.getKey(), entry.getValue());
        	}
        }
        if (data != null && !data.isEmpty()) {
        	Map<String,Object> dataMap = AttribUtils.attributesToMap(data);
        	resultData.add(dataMap);
        }
    }

    public String getStatusText() {
        if (status < 0) return "NotSet";
        String value = "Unknown";
        for (Field f : Status.class.getFields()) {
            try {
                Object v = f.get(null);
                if ((v instanceof Integer) && ((int)v == status)) {
                    value = f.getName();
                    break;
                }
            } catch (IllegalArgumentException | IllegalAccessException ignore) { }
        }
        // By specification, statuses C000 through CFFF are considered Unable to Process
        if (value.equals("Unknown") && status >= 0xC000 && status <= 0xCFFF) value = "UnableToProcess";
        return value;
    }
    
    // C-GET sometimes incorrectly reports everything as failed. This is a hack to autocorrect.
    public void updateResults(int numberActualCompleted) {
        if (resultStatus.isEmpty()) return;
        int completed = (int)resultStatus.get("NumberOfCompletedSuboperations");
        int failed = (int)resultStatus.get("NumberOfFailedSuboperations");
        int remaining = (int)resultStatus.get("NumberOfRemainingSuboperations");
        int warning = (int)resultStatus.get("NumberOfWarningSuboperations");
        int resultStatusValue = (int)resultStatus.get("Status");
        // Fix what's reported as completed
        if (((completed + failed) == numberActualCompleted) && (completed < numberActualCompleted)) {
            failed -= (numberActualCompleted - completed);
            completed = numberActualCompleted;
            resultStatus.replace("NumberOfCompletedSuboperations", completed);
            resultStatus.replace("NumberOfFailedSuboperations", failed);
        }
        if (completed > 0 && failed == 0 && remaining == 0 && warning == 0 && resultStatusValue != Status.Success) {
            status = Status.Success;
        	resultStatus.replace("Status", status);
        	resultStatus.replace(STATUS_TEXT, getStatusText());
        }
    }
}
