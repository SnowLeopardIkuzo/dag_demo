package com.example.dag.frame.meta;

import lombok.Data;

import java.util.Map;

@Data
public class MetaGraph {
    private String graphName;
    private ThreadPoolConfig threadPoolConfig;
    private ThreadPoolConfig secondThreadPoolConfig;
    private boolean requireAllNodesSuccess;
    private Map<String, MetaStage> stages;
}
