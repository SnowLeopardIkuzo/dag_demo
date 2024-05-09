package com.example.dag.frame.graph;

import com.example.dag.frame.node.Node;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

@Getter
@AllArgsConstructor
public class Graph {
    private final String graphName;

    private final ThreadPoolExecutor threadPoolExecutor;

    private final ThreadPoolExecutor secondThreadPoolExecutor;

    private final int stageNum;

    private final List<Node> entryNodeList;

    private final List<Node> exitNodeList;

    private final List<Node> allNodeList;

    private final boolean requireAllNodesSuccess;
}
