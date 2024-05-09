package com.example.dag.frame.task;

import com.example.dag.frame.async.AsyncTaskResult;
import com.example.dag.frame.context.DagContext;
import com.example.dag.frame.exception.DagException;
import com.example.dag.frame.graph.Graph;
import com.example.dag.frame.node.Node;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class TaskDispatcher {

    private final Graph graph;

    private final DagContext dagContext;

    private final Map<Node, NodeTask> nodeTaskMap;

    private final CountDownLatch exitNodeCountDownLatch;

    private final AtomicBoolean recorded;

    public TaskDispatcher(Graph graph, DagContext dagContext) {
        this.graph = graph;
        this.dagContext = dagContext;
        Map<Node, NodeTask> map = new HashMap<>(graph.getAllNodeList().size());
        for (Node node : graph.getAllNodeList()) {
            map.put(node, new NodeTask(node, dagContext));
        }
        this.nodeTaskMap = Collections.unmodifiableMap(map);
        this.exitNodeCountDownLatch = new CountDownLatch(graph.getExitNodeList().size());
        this.recorded = new AtomicBoolean(false);
    }

    public void dispatch() {
        startEntryNodeLoopTask();
        try {
            exitNodeCountDownLatch.await();
        } catch (InterruptedException e) {
            log.error("graph {} interrupted", graph.getGraphName(), e);
            dagContext.stop("interrupted");
        }
    }

    public boolean dispatch(long timeout) {
        startEntryNodeLoopTask();
        boolean finished = false;
        try {
            finished = exitNodeCountDownLatch.await(timeout, TimeUnit.MILLISECONDS);
            if (!finished) {
                dagContext.stop("timeout");
            }
        } catch (InterruptedException e) {
            log.error("graph {} interrupted", graph.getGraphName(), e);
            dagContext.stop("interrupted");
        }
        return finished;
    }

    public AsyncTaskResult asyncDispatch() {
        startEntryNodeLoopTask();
        return new AsyncTaskResult(exitNodeCountDownLatch, dagContext);
    }

    private void startEntryNodeLoopTask() {
        try {
            for (Node entryNode : graph.getEntryNodeList()) {
                graph.getThreadPoolExecutor().execute(
                        new LoopTask(graph, dagContext, nodeTaskMap, nodeTaskMap.get(entryNode), exitNodeCountDownLatch, recorded));
            }
        } catch (Exception e) {
            log.error("graph {} start entry node loop task error", graph.getGraphName(), e);
            // if any loop task fails to start, stop the graph task
            dagContext.stop("startEntryNodeLoopTaskError");
            // throw an exception to stop further processing,
            // otherwise the `exitNodeCountDownLatch.await()` method will never return
            throw new DagException(graph.getGraphName() + " start loop task error", e);
        }
    }

}
