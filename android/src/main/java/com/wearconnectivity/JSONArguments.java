package com.wearconnectivity;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

public class JSONArguments {
    public static ReadableMap fromJSONObject(JSONObject jsonObject) {
        WritableMap map = Arguments.createMap();
        Iterator<String> keys = jsonObject.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            try {
                Object value = jsonObject.get(key);
                if (value instanceof String) {
                    map.putString(key, (String) value);
                } else if (value instanceof Boolean) {
                    map.putBoolean(key, (Boolean) value);
                } else if (value instanceof Integer || value instanceof Long) {
                    map.putInt(key, ((Number) value).intValue());
                } else if (value instanceof Float || value instanceof Double) {
                    map.putDouble(key, ((Number) value).doubleValue());
                } else if (value == JSONObject.NULL) {
                    map.putNull(key);
                } else if (value instanceof JSONObject) {
                    map.putMap(key, fromJSONObject((JSONObject) value));
                } else if (value instanceof JSONArray) {
                    map.putArray(key, fromJSONArray((JSONArray) value));
                } else {
                    throw new IllegalArgumentException("Unsupported type: " + value.getClass().getName());
                }
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }
        return map;
    }

    public static ReadableArray fromJSONArray(JSONArray jsonArray) {
        WritableArray array = Arguments.createArray();
        for (int i = 0; i < jsonArray.length(); i++) {
            try {
                Object value = jsonArray.get(i);
                if (value instanceof String) {
                    array.pushString((String) value);
                } else if (value instanceof Boolean) {
                    array.pushBoolean((Boolean) value);
                } else if (value instanceof Integer || value instanceof Long) {
                    array.pushInt(((Number) value).intValue());
                } else if (value instanceof Float || value instanceof Double) {
                    array.pushDouble(((Number) value).doubleValue());
                } else if (value == JSONObject.NULL) {
                    array.pushNull();
                } else if (value instanceof JSONObject) {
                    array.pushMap(fromJSONObject((JSONObject) value));
                } else if (value instanceof JSONArray) {
                    array.pushArray(fromJSONArray((JSONArray) value));
                } else {
                    throw new IllegalArgumentException("Unsupported type: " + value.getClass().getName());
                }
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }
        return array;
    }
}
