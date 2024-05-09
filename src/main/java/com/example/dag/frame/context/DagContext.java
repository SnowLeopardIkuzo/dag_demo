package com.example.dag.frame.context;

import com.example.dag.frame.recorder.DagRecorder;
import lombok.Getter;
import lombok.Setter;

@Getter
public class DagContext {

    private final String graphName;

    @Setter
    private DagRecorder dagRecorder;

    private volatile boolean stopped;

    private volatile String stopReason;

    public DagContext(String graphName) {
        this.graphName = graphName;
        this.stopped = false;
        this.stopReason = null;
    }

    public void stop(String stopReason) {
        this.stopped = true;
        this.stopReason = stopReason;
    }

}
