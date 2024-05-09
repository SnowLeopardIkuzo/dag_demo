package com.example.dag.frame.task;

import com.example.dag.frame.context.DagContext;
import com.example.dag.frame.graph.Graph;
import com.example.dag.frame.node.Node;
import com.example.dag.frame.node.NodeType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class LoopTask implements Runnable {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final Graph graph;

    private final DagContext dagContext;

    private final Map<Node, NodeTask> nodeTaskMap;

    private final NodeTask firstNodeTask;

    private final CountDownLatch exitNodeCountDownLatch;

    private final AtomicBoolean recorded;


    public LoopTask(Graph graph, DagContext dagContext, Map<Node, NodeTask> nodeTaskMap, NodeTask firstNodeTask,
                    CountDownLatch exitNodeCountDownLatch, AtomicBoolean recorded) {
        this.graph = graph;
        this.dagContext = dagContext;
        this.nodeTaskMap = nodeTaskMap;
        this.firstNodeTask = firstNodeTask;
        this.exitNodeCountDownLatch = exitNodeCountDownLatch;
        this.recorded = recorded;
    }

    @Override
    public void run() {
        try {
            NodeTask currentNodeTask = firstNodeTask;

            while (currentNodeTask != null) {
                // execute current node task
                if (currentNodeTask.getNode().getType() == NodeType.NORMAL) {
                    // normal node
                    executeNormalNodeTask(currentNodeTask);
                } else if (currentNodeTask.getNode().getType() == NodeType.STAGE_START) {
                    // start node of stage
                    executeStageStartNodeTask(currentNodeTask);
                } else if (currentNodeTask.getNode().getType() == NodeType.STAGE_END) {
                    // end node of stage
                    executeStageEndNodeTask(currentNodeTask);
                }

                // check if current node is an exit node
                if (currentNodeTask.getNode().getSuccessorNodeList().isEmpty()) {
                    // one exit node finished
                    exitNodeCountDownLatch.countDown();

                    // check if all exit nodes are finished
                    if (exitNodeCountDownLatch.getCount() == 0) {
                        // record task info at the end of the graph task
                        if (recorded.compareAndSet(false, true)) {
                            // only record once
                            recordGraphTaskInfo();
                        }
                    }

                    break;
                }

                // check the status of successor node tasks and execute ready node tasks
                NodeTask nextNodeTask = null;
                for (Node node : currentNodeTask.getNode().getSuccessorNodeList()) {
                    NodeTask nodeTask = nodeTaskMap.get(node);

                    boolean ready = nodeTask.decreaseOnePredecessorAndCheckStatus();
                    if (!ready) {
                        continue;
                    }

                    if (nextNodeTask == null) {
                        // current thread will execute the first ready node task on next loop iteration
                        nextNodeTask = nodeTask;
                    } else {
                        // thread pool executes other ready node tasks
                        graph.getThreadPoolExecutor().execute(
                                new LoopTask(graph, dagContext, nodeTaskMap, nodeTask, exitNodeCountDownLatch, recorded));
                    }
                }
                currentNodeTask = nextNodeTask;
            }
        } catch (Exception e) {
            // unable to retrieve subsequent node tasks, stop the graph task
            log.error("graph {} loopTask error", graph.getGraphName(), e);
            dagContext.stop("loopTaskError");
            // avoid blocking the caller thread indefinitely on `exitNodeCountDownLatch.await()`
            while (exitNodeCountDownLatch.getCount() > 0) {
                exitNodeCountDownLatch.countDown();
            }
        }
    }

    private void executeNormalNodeTask(NodeTask normalNodeTask) {
        if (dagContext.isStopped() && !normalNodeTask.getNode().isForced()) {
            return;
        }

        long start = System.currentTimeMillis();
        boolean success;
        if (normalNodeTask.getNode().getTimeout() > 0) {
            success = executeNormalNodeTaskWithTimeout(normalNodeTask);
        } else {
            success = executeNormalNodeTaskDirectly(normalNodeTask);
        }
        long cost = System.currentTimeMillis() - start;
        log.info("execute node {} cost {}ms", normalNodeTask.getNode().getGraphStageNodeName(), cost);
        dagContext.getDagRecorder().recordNode(normalNodeTask.getNode().getGraphStageNodeName(), cost);

        if (!success) {
            dagContext.getDagRecorder().getFailedNodes().add(normalNodeTask.getNode().getGraphStageNodeName());
            if (graph.isRequireAllNodesSuccess()) {
                dagContext.stop(normalNodeTask.getNode().getNodeName() + "Failed");
            }
        }
    }

    private boolean executeNormalNodeTaskDirectly(NodeTask normalNodeTask) {
        boolean success = false;
        try {
            normalNodeTask.execute();
            success = true;
        } catch (Exception e) {
            log.error("execute node {} error", normalNodeTask.getNode().getGraphStageNodeName(), e);
        }
        return success;
    }

    private boolean executeNormalNodeTaskWithTimeout(NodeTask normalNodeTask) {
        boolean success = false;
        Future<Boolean> future = null;
        try {
            future = graph.getSecondThreadPoolExecutor().submit(() -> {
                normalNodeTask.execute();
                return true;
            });
            success = future.get(normalNodeTask.getNode().getTimeout(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            log.error("get node {} future timeout:{}",
                    normalNodeTask.getNode().getGraphStageNodeName(), normalNodeTask.getNode().getTimeout(), e);
            future.cancel(true);
        } catch (ExecutionException e) {
            log.error("execute node {} error", normalNodeTask.getNode().getGraphStageNodeName(), e);
        } catch (InterruptedException e) {
            log.error("get node {} future interrupted", normalNodeTask.getNode().getGraphStageNodeName(), e);
        } catch (Exception e) {
            log.error("get node {} future error", normalNodeTask.getNode().getGraphStageNodeName(), e);
        }
        return success;
    }

    private void executeStageStartNodeTask(NodeTask currentNodeTask) {
        dagContext.getDagRecorder().recordStageStart(currentNodeTask.getNode().getGraphStageName());
    }

    private void executeStageEndNodeTask(NodeTask currentNodeTask) {
        dagContext.getDagRecorder().recordStageEnd(currentNodeTask.getNode().getGraphStageName());
    }

    private void recordGraphTaskInfo() {
        try {
            long cost = System.currentTimeMillis() - dagContext.getDagRecorder().getStartTime();
            String stageCost = objectMapper.writeValueAsString(dagContext.getDagRecorder().getStageCostTime());
            String nodeCost = objectMapper.writeValueAsString(dagContext.getDagRecorder().getNodeCostTime());
            String failedNodes = objectMapper.writeValueAsString(dagContext.getDagRecorder().getFailedNodes());
            if (!dagContext.isStopped()) {
                log.info("execute graph {} cost {}ms, stage:{}, node:{}, failedNodes:{}",
                        graph.getGraphName(), cost, stageCost, nodeCost, failedNodes);
            } else {
                log.info("graph {} stopped by {}, execute graph {} cost {}ms, stage:{}, node:{}, failedNodes:{}",
                        graph.getGraphName(), dagContext.getStopReason(), graph.getGraphName(), cost, stageCost,
                        nodeCost, failedNodes);
            }
        } catch (Exception e) {
            log.error("record graph {} error", graph.getGraphName(), e);
        }
    }

}