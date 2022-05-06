package edu.jh.pm.tic.dicom;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.ElementDictionary;
import org.dcm4che3.data.Sequence;
import org.dcm4che3.data.VR;
import org.dcm4che3.util.TagUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mule.api.MuleMessage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Copyright (c) 2022 The Johns Hopkins University
 * All rights reserved
 *
 * @author David J. Talley, Technology Innovation Center, Precision Medicine Analytics Platform, Johns Hopkins Medicine
 *
 */
public class AttribUtils {
    private static final String UNKNOWN = "Unknown";
    private static final Pattern hexTag = Pattern.compile("^0[xX][0-9a-fA-F]{8}$");
    private static final Pattern intTag = Pattern.compile("^\\d+$");
    private static final Pattern pairTag = Pattern.compile("^([0-9a-fA-F]{4})\\W([0-9a-fA-F]{4})$");

    private AttribUtils() { }

    private static int stringToTag(String name) {
        int tag = -1;
        if (hexTag.matcher(name).matches()) {
            tag = Integer.decode(name);
        } else if (intTag.matcher(name).matches()) {
            tag = Integer.parseInt(name, 16);
        } else if (pairTag.matcher(name).matches()) {
            Matcher m = pairTag.matcher(name);
            if (m.find()) {
                int group = Integer.decode("0x" + m.group(1));
                int element = Integer.decode("0x" + m.group(2));
                tag = (group << 16) + element;
            }
        } else {
            tag = ElementDictionary.tagForKeyword(name, (String)null);
        }
        if (tag == -1) {
            throw new IllegalArgumentException(name);
        }
        return tag;
    }

    public static void addKeys(Attributes keys, Map<String, Object> entities) {
        Map<Integer,Object> privateTags = new HashMap<>();
        for (Map.Entry<String, Object> key : entities.entrySet()) {
            int tag = stringToTag(key.getKey());
            if (TagUtils.isPrivateCreator(tag)) {
                keys.setString(tag, VR.LO, (String) key.getValue());
            } else if (TagUtils.isPrivateTag(tag)) {
                privateTags.put(tag, key.getValue());
            } else {
                VR vr = ElementDictionary.vrOf(tag, null);
                keys.setValue(tag, vr, key.getValue());
            }
        }
        // Add the private tags
        for (Map.Entry<Integer, Object> key : privateTags.entrySet()) {
            int tag = key.getKey();
            int creatorTag = TagUtils.creatorTagOf(tag);
            String privateCreator = keys.getString(creatorTag, UNKNOWN);
            keys.setString(privateCreator, tag, VR.LO, (String)key.getValue());
        }
    }
    public static Attributes toKeys(Map<String, Object> entities) {
        Attributes keys = new Attributes();
        addKeys(keys, entities);
        return keys;
    }

    public static Map<String,Object> attributesToMap(Attributes data) {
        Map<String,Object> map = new HashMap<>();
        if (data != null) {
            for (int tag : data.tags()) {
                tagToMap(data, tag, map);
            }
        }
        return map;
    }


    public static Attributes payloadToKeys(MuleMessage muleMessage) throws IOException {
        Object payload = muleMessage.getPayload();
        if (payload instanceof String) {
            String payloadString = (String)payload;
            int fo = payloadString.indexOf('{');
            int fl = payloadString.indexOf('[');
            int lo = payloadString.lastIndexOf('}');
            if (fo >= 0 && lo > 0 && fo < lo && fo > fl) { // looks like a json object
                JSONObject json = new JSONObject(payloadString);
                Map<String,Object> map = jsonToMap(json);
                return AttribUtils.toKeys(map);
            }
        } else if (payload instanceof Map<?,?>) {
            @SuppressWarnings("unchecked")
            Map<String,Object> map = (Map<String,Object>)payload;
            return AttribUtils.toKeys(map);
        }
        throw new IOException("Payload must be a Map<String,Object> or String with a JSON Object");
    }

    public static Map<String, Object> jsonToMap(JSONObject jsonobj)  throws JSONException {
        Map<String, Object> map = new HashMap<>();
        @SuppressWarnings("unchecked")
        Iterator<String> keys = jsonobj.keys();
        while(keys.hasNext()) {
            String key = keys.next();
            Object value = jsonobj.get(key);
            if (value instanceof JSONArray) {
                value = jsonToList((JSONArray) value);
            } else if (value instanceof JSONObject) {
                value = jsonToMap((JSONObject) value);
            }
            map.put(key, value);
        }   return map;
    }

    public static List<Object> jsonToList(JSONArray array) throws JSONException {
        List<Object> list = new ArrayList<>();
        for(int i = 0; i < array.length(); i++) {
            Object value = array.get(i);
            if (value instanceof JSONArray) {
                value = jsonToList((JSONArray) value);
            }
            else if (value instanceof JSONObject) {
                value = jsonToMap((JSONObject) value);
            }
            list.add(value);
        }   return list;
    }

    private static void tagToMap(Attributes data, int tag, Map<String,Object> map) {
        // Get private creator of this tag
        String privateCreator = null;
        String tagName;
        if (TagUtils.isPrivateCreator(tag)) return;
        else if (TagUtils.isPrivateTag(tag)) {
            int creatorTag = TagUtils.creatorTagOf(tag);
            privateCreator = data.getString(creatorTag, UNKNOWN);
            tagName = String.format("%s (%04x,%04x)", privateCreator, tag >> 16, tag & 0xFFFF);
        } else {
            tagName = ElementDictionary.keywordOf(tag, null);
        }
        if (tagName.isEmpty()) tagName = UNKNOWN;
        Object tagValue = data.getValue(privateCreator, tag);
        if (tagValue instanceof Sequence) {
            Sequence tagSeq = (Sequence)tagValue;
            List<Map<String,Object>> list = new ArrayList<>();
            for (Attributes d : tagSeq) {
                Map<String,Object> m = new HashMap<>();
                for (int t : d.tags()) {
                    tagToMap(d, t, m);
                }
                list.add(m);
            }
            map.put(tagName, list);
        } else {
            vrToMap(data, tag, tagName, map);
        }
    }

    private static void vrToMap(Attributes data, int tag, String tagName, Map<String,Object> map) {
        VR vr = data.getVR(tag);
        if (vr.isIntType()) map.put(tagName, data.getInt(tag, 0));
        else if (vr.isStringType()) map.put(tagName, data.getString(tag));
        else {
            switch (vr.code()) {
                case 17988: // double
                case 20292: // double
                    map.put(tagName, data.getDouble(tag, 0));
                    break;
                case 17996: // float
                case 20294: // float
                    map.put(tagName, data.getFloat(tag, 0));
                    break;
                case 20300: // int
                case 21324: // int
                case 21843: // ushort
                    map.put(tagName, data.getInt(tag, 0));
                    break;
                case 20311: // short
                case 21331: // short
                    map.put(tagName, (short)data.getInt(tag, 0));
                    break;
                case 20310: // long
                case 21334: // long
                case 21836: // uint
                case 21846: // ulong
                    map.put(tagName, data.getLong(tag, 0));
                    break;
                case 16724: // tag (binary)
                case 20290: // byte
                case 21838: // byte
                case 21329: // sequence
                    break;
                default: // string
                    map.put(tagName, data.getString(tag));
                    break;
            }
        }
    }
}
