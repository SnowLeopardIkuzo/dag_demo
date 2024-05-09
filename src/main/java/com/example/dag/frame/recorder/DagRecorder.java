package com.example.dag.frame.recorder;

import lombok.Getter;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class DagRecorder {
    private final Map<String, Long> stageCostTime;

    private final Map<String, Long> nodeCostTime;

    private final Set<String> failedNodes;

    private final long startTime;

    public DagRecorder(int stageNum, int nodeNum) {
        this.stageCostTime = new ConcurrentHashMap<>(stageNum);
        this.nodeCostTime = new ConcurrentHashMap<>(nodeNum);
        this.failedNodes = ConcurrentHashMap.newKeySet();
        this.startTime = System.currentTimeMillis();
    }

    public void recordNode(String graphNodeName, long time) {
        nodeCostTime.put(graphNodeName, time);
    }

    public void recordStageStart(String graphStageName) {
        stageCostTime.put(graphStageName, System.currentTimeMillis());
    }

    public void recordStageEnd(String graphStageName) {
        stageCostTime.put(graphStageName, System.currentTimeMillis() - stageCostTime.get(graphStageName));
    }

}
