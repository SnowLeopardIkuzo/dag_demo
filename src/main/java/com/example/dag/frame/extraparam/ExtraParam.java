package com.example.dag.frame.extraparam;

import java.util.Map;

public class ExtraParam {

    private final Map<String, String> extraParamMap;

    public ExtraParam(Map<String, String> extraParamMap) {
        this.extraParamMap = extraParamMap;
    }

    public String getExtraParamValue(String key) {
        return this.getExtraParamValue(key, null);
    }

    public String getExtraParamValue(String key, String defaultValue) {
        if (extraParamMap == null || extraParamMap.isEmpty()) {
            return defaultValue;
        }
        return extraParamMap.getOrDefault(key, defaultValue);
    }

}
