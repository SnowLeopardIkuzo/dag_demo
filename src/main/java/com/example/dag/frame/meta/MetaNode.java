package com.example.dag.frame.meta;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class MetaNode {
    private String nodeName;
    private String description;
    private List<String> dependency;
    private String clz;
    private String conditionClz;
    private Map<String, String> extraParam;
    private boolean forced;
    private long timeout;
}
