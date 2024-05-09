package com.example.dag.frame.meta;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class MetaStage {
    private String stageName;
    private String description;
    private List<String> dependency;
    private Map<String, MetaNode> nodes;
}
