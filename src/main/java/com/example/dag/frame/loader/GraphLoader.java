package com.example.dag.frame.loader;

import com.example.dag.frame.action.Action;
import com.example.dag.frame.condition.Condition;
import com.example.dag.frame.exception.DagException;
import com.example.dag.frame.extraparam.ExtraParam;
import com.example.dag.frame.graph.Graph;
import com.example.dag.frame.meta.MetaGraph;
import com.example.dag.frame.meta.MetaNode;
import com.example.dag.frame.meta.MetaStage;
import com.example.dag.frame.node.Node;
import com.example.dag.frame.node.NodeType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
public class GraphLoader {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final String START = "Start";

    private static final String END = "End";

    public static Map<String, Graph> load(String path, ApplicationContext applicationContext) {
        log.info("start loading graphs from {}", path);
        // read graph config file
        List<MetaGraph> metaGraphs = new ArrayList<>();
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources(path);
            for (Resource resource : resources) {
                if (resource.isReadable()) {
                    String content = resource.getContentAsString(StandardCharsets.UTF_8);
                    MetaGraph metaGraph = objectMapper.readValue(content, MetaGraph.class);
                    metaGraphs.add(metaGraph);
                }
            }
        } catch (Exception e) {
            throw new DagException("load graph file error", e);
        }

        // check graph meta data
        checkMetaGraphs(metaGraphs);

        // construct real graph with spring bean
        Map<String, Graph> graphMap = new HashMap<>(metaGraphs.size());
        for (MetaGraph metaGraph : metaGraphs) {
            Graph graph = constructRealGraph(metaGraph, applicationContext);
            graphMap.put(graph.getGraphName(), graph);
        }

        log.info("load graphs success: {}", graphMap.keySet());
        return graphMap;
    }

    private static Graph constructRealGraph(MetaGraph metaGraph, ApplicationContext applicationContext) {
        Map<String, Node> stageNodeMap = new HashMap<>(metaGraph.getStages().size() * 2);
        List<Node> allNodeList = new ArrayList<>();

        for (MetaStage metaStage : metaGraph.getStages().values()) {
            Map<String, Node> currentStageNodeMap = new HashMap<>(metaStage.getNodes().size());

            // construct real node
            String graphStageName = metaGraph.getGraphName() + "-" + metaStage.getStageName();

            for (MetaNode metaNode : metaStage.getNodes().values()) {
                String nodeName = metaNode.getNodeName();
                String graphStageNodeName = graphStageName + '-' + nodeName;

                Action<?, ?> action;
                Condition condition = null;
                try {
                    Class<?> clz = Class.forName(metaNode.getClz());
                    action = (Action<?, ?>) applicationContext.getBean(clz);

                    if (metaNode.getConditionClz() != null) {
                        Class<?> conditionClz = Class.forName(metaNode.getConditionClz());
                        condition = (Condition) applicationContext.getBean(conditionClz);
                    }
                } catch (Exception e) {
                    throw new DagException("get " + graphStageNodeName + " spring bean error", e);
                }

                ExtraParam extraParam = new ExtraParam(metaNode.getExtraParam());

                Node node = new Node(nodeName, NodeType.NORMAL, graphStageName, graphStageNodeName,
                        action, extraParam, condition, metaNode.getTimeout(), metaNode.isForced(),
                        new ArrayList<>(), new ArrayList<>());

                currentStageNodeMap.put(node.getNodeName(), node);
            }

            // construct start node of stage
            Node stageStartNode = new Node(metaStage.getStageName() + START, NodeType.STAGE_START,
                    graphStageName, graphStageName + "-" + metaStage.getStageName() + START,
                    null, null, null, 0, false, new ArrayList<>(), new ArrayList<>());
            // construct end node of stage
            Node stageEndNode = new Node(metaStage.getStageName() + END, NodeType.STAGE_END,
                    graphStageName, graphStageName + "-" + metaStage.getStageName() + END,
                    null, null, null, 0, false, new ArrayList<>(), new ArrayList<>());

            // find predecessor and successor nodes for each normal node
            for (Node node : currentStageNodeMap.values()) {
                MetaNode metaNode = metaStage.getNodes().get(node.getNodeName());
                for (String dependentNodeName : metaNode.getDependency()) {
                    Node dependentNode = currentStageNodeMap.get(dependentNodeName);
                    dependentNode.getSuccessorNodeList().add(node);
                    node.getPredecessorNodeList().add(dependentNode);
                }
            }

            for (Node node : currentStageNodeMap.values()) {
                // find successor nodes for start node of current stage
                if (node.getPredecessorNodeList().isEmpty()) {
                    node.getPredecessorNodeList().add(stageStartNode);
                    stageStartNode.getSuccessorNodeList().add(node);
                }
                // find predecessor nodes for end node of current stage
                if (node.getSuccessorNodeList().isEmpty()) {
                    node.getSuccessorNodeList().add(stageEndNode);
                    stageEndNode.getPredecessorNodeList().add(node);
                }
            }

            stageNodeMap.put(stageStartNode.getNodeName(), stageStartNode);
            stageNodeMap.put(stageEndNode.getNodeName(), stageEndNode);

            allNodeList.addAll(currentStageNodeMap.values());
            allNodeList.add(stageStartNode);
            allNodeList.add(stageEndNode);
        }

        // find predecessor and successor nodes for start nodes of stages and end nodes of stages
        for (MetaStage metaStage : metaGraph.getStages().values()) {
            Node stageStartNode = stageNodeMap.get(metaStage.getStageName() + START);
            for (String dependentStage : metaStage.getDependency()) {
                Node dependentStageEndNode = stageNodeMap.get(dependentStage + END);
                dependentStageEndNode.getSuccessorNodeList().add(stageStartNode);
                stageStartNode.getPredecessorNodeList().add(dependentStageEndNode);
            }
        }

        // find entry nodes and exit nodes
        List<Node> entryNodeList = new ArrayList<>();
        List<Node> exitNodeList = new ArrayList<>();
        for (Node stageNode : stageNodeMap.values()) {
            if (stageNode.getPredecessorNodeList().isEmpty()) {
                entryNodeList.add(stageNode);
            }
            if (stageNode.getSuccessorNodeList().isEmpty()) {
                exitNodeList.add(stageNode);
            }
        }

        // construct thread pool
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(metaGraph.getThreadPoolConfig().getCoreSize(),
                metaGraph.getThreadPoolConfig().getMaxSize(),
                1, TimeUnit.HOURS,
                new LinkedBlockingDeque<>(metaGraph.getThreadPoolConfig().getQueueSize()),
                new CustomizableThreadFactory(metaGraph.getGraphName() + "-1st-"));
        ThreadPoolExecutor secondThreadPoolExecutor = null;
        if (metaGraph.getSecondThreadPoolConfig() != null) {
            secondThreadPoolExecutor = new ThreadPoolExecutor(metaGraph.getSecondThreadPoolConfig().getCoreSize(),
                    metaGraph.getSecondThreadPoolConfig().getMaxSize(),
                    1, TimeUnit.HOURS,
                    new LinkedBlockingDeque<>(metaGraph.getSecondThreadPoolConfig().getQueueSize()),
                    new CustomizableThreadFactory(metaGraph.getGraphName() + "-2nd-"));
        }

        return new Graph(metaGraph.getGraphName(), threadPoolExecutor, secondThreadPoolExecutor, metaGraph.getStages().size(),
                entryNodeList, exitNodeList, allNodeList, metaGraph.isRequireAllNodesSuccess());
    }

    private static void checkMetaGraphs(List<MetaGraph> metaGraphs) {
        Set<String> graphNameSet = new HashSet<>(metaGraphs.size());
        for (MetaGraph metaGraph : metaGraphs) {
            String graphName = metaGraph.getGraphName();

            // graph name
            if (graphName == null || graphName.isBlank()) {
                throw new DagException("empty graph name");
            }
            boolean added = graphNameSet.add(graphName);
            if (!added) {
                throw new DagException(graphName + " duplicate graphName");
            }

            // thread pool config
            if (metaGraph.getThreadPoolConfig() == null) {
                throw new DagException(graphName + " null thread pool config");
            }
            if (metaGraph.getThreadPoolConfig().getCoreSize() <= 0
                    || metaGraph.getThreadPoolConfig().getMaxSize() < metaGraph.getThreadPoolConfig().getCoreSize()
                    || metaGraph.getThreadPoolConfig().getQueueSize() <= 0) {
                throw new DagException(graphName + " invalid thread pool config");
            }

            // second thread pool config
            boolean hasSecondThreadPoolConfig = metaGraph.getSecondThreadPoolConfig() != null;
            if (hasSecondThreadPoolConfig) {
                if (metaGraph.getSecondThreadPoolConfig().getCoreSize() <= 0
                        || metaGraph.getSecondThreadPoolConfig().getMaxSize() < metaGraph.getSecondThreadPoolConfig().getCoreSize()
                        || metaGraph.getSecondThreadPoolConfig().getQueueSize() <= 0) {
                    throw new DagException(graphName + "invalid second thread pool config");
                }
            }

            // stageMap
            if (metaGraph.getStages() == null || metaGraph.getStages().isEmpty()) {
                throw new DagException(graphName + " empty stageMap ");
            }
            if (metaGraph.getStages().containsKey(null) || metaGraph.getStages().containsValue(null)) {
                throw new DagException(graphName + " stageMap contains null");
            }

            boolean hasNodeTimeout = false;
            Set<String> nodeNameSet = new HashSet<>();

            for (Map.Entry<String, MetaStage> stageEntry : metaGraph.getStages().entrySet()) {
                MetaStage metaStage = stageEntry.getValue();

                // stage name
                if (!stageEntry.getKey().equals(metaStage.getStageName())) {
                    throw new DagException(graphName
                            + " different stage name " + stageEntry.getKey() + " " + metaStage.getStageName());
                }
                if (metaStage.getStageName().isBlank()) {
                    throw new DagException(graphName + " empty stage name");
                }

                // stage dependency
                if (metaStage.getDependency() == null) {
                    throw new DagException(graphName + " stage " + metaStage.getStageName() + " null dependency");
                }
                for (String stage : metaStage.getDependency()) {
                    if (!metaGraph.getStages().containsKey(stage)) {
                        throw new DagException(graphName + " stage " + metaStage.getStageName() + " invalid dependency " + stage);
                    }
                }
                if (metaStage.getDependency().stream().distinct().count() < metaStage.getDependency().size()) {
                    throw new DagException(graphName + " stage " + metaStage.getStageName() + " duplicate dependency");
                }

                // nodeMap
                if (metaStage.getNodes() == null || metaStage.getNodes().isEmpty()) {
                    throw new DagException(graphName + " stage " + metaStage.getStageName() + " empty nodeMap ");
                }
                if (metaStage.getNodes().containsKey(null) || metaStage.getNodes().containsValue(null)) {
                    throw new DagException(graphName + " stage " + metaStage.getStageName() + " nodeMap contains null");
                }

                for (Map.Entry<String, MetaNode> nodeEntry : metaStage.getNodes().entrySet()) {
                    MetaNode metaNode = nodeEntry.getValue();

                    // node name
                    if (!nodeEntry.getKey().equals(metaNode.getNodeName())) {
                        throw new DagException(graphName
                                + " different node name " + nodeEntry.getKey() + " " + metaNode.getNodeName());
                    }
                    if (metaNode.getNodeName().isBlank()) {
                        throw new DagException(graphName + " empty node name");
                    }
                    boolean nodeNameAdded = nodeNameSet.add(metaNode.getNodeName());
                    if (!nodeNameAdded) {
                        throw new DagException(graphName + " " + metaNode.getNodeName() + " duplicate node name");
                    }

                    // node dependency
                    if (metaNode.getDependency() == null) {
                        throw new DagException(graphName + " node " + metaNode.getNodeName() + " null dependency");
                    }
                    for (String node : metaNode.getDependency()) {
                        if (!metaStage.getNodes().containsKey(node)) {
                            throw new DagException(graphName + " node " + metaNode.getNodeName() + " invalid dependency " + node);
                        }
                    }
                    if (metaNode.getDependency().stream().distinct().count() < metaNode.getDependency().size()) {
                        throw new DagException(graphName + " node " + metaNode.getNodeName() + " duplicate dependency");
                    }

                    // node class
                    if (metaNode.getClz() == null || metaNode.getClz().isBlank()) {
                        throw new DagException(graphName + " node " + metaNode.getNodeName() + " null action class");
                    }
                    if (metaNode.getConditionClz() != null && metaNode.getConditionClz().isBlank()) {
                        throw new DagException(graphName + " " + metaNode.getNodeName() + " blank condition class");
                    }

                    // node timeout
                    if (metaNode.getTimeout() < 0) {
                        throw new DagException(graphName + " node " + metaNode.getNodeName()
                                + " invalid timeout " + metaNode.getTimeout());
                    }
                    if (metaNode.getTimeout() > 0) {
                        hasNodeTimeout = true;
                    }

                }

                // node cyclic dependency
                for (MetaNode metaNode : metaStage.getNodes().values()) {
                    Set<String> visitedNode = new HashSet<>(metaStage.getNodes().size());
                    Queue<String> nodeNameQueue = new ArrayDeque<>(metaStage.getNodes().size());
                    nodeNameQueue.addAll(metaNode.getDependency());
                    while (!nodeNameQueue.isEmpty()) {
                        String nodeName = nodeNameQueue.poll();
                        if (nodeName.equals(metaNode.getNodeName())) {
                            throw new DagException(metaGraph.getGraphName() + " stage " + metaStage.getStageName()
                                    + " exists node cyclic dependency");
                        }
                        boolean neverVisited = visitedNode.add(nodeName);
                        if (!neverVisited) {
                            MetaNode node = metaStage.getNodes().get(nodeName);
                            nodeNameQueue.addAll(node.getDependency());
                        }
                    }
                }

            }

            // second thread pool config and node timeout
            if (hasSecondThreadPoolConfig && !hasNodeTimeout) {
                throw new DagException(graphName + " useless second thread pool");
            }
            if (!hasSecondThreadPoolConfig && hasNodeTimeout) {
                throw new DagException(graphName + " null second thread pool");
            }

            // stage cyclic dependency
            for (MetaStage metaStage : metaGraph.getStages().values()) {
                Set<String> visitedStage = new HashSet<>(metaGraph.getStages().size());
                Queue<String> stageNameQueue = new ArrayDeque<>(metaGraph.getStages().size());
                stageNameQueue.addAll(metaStage.getDependency());
                while (!stageNameQueue.isEmpty()) {
                    String stageName = stageNameQueue.poll();
                    if (stageName.equals(metaStage.getStageName())) {
                        throw new DagException(metaGraph.getGraphName() + " exists stage cyclic dependency");
                    }
                    boolean neverVisited = visitedStage.add(stageName);
                    if (!neverVisited) {
                        MetaStage stage = metaGraph.getStages().get(stageName);
                        stageNameQueue.addAll(stage.getDependency());
                    }
                }
            }

        }
    }

}
