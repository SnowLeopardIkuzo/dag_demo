package com.example.dag.frame.node;

import com.example.dag.frame.action.Action;
import com.example.dag.frame.condition.Condition;
import com.example.dag.frame.extraparam.ExtraParam;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.List;

@Getter
@EqualsAndHashCode(of = {"nodeName", "type", "graphStageName", "graphStageNodeName"})
@AllArgsConstructor
public class Node {
    private final String nodeName;

    private final NodeType type;

    private final String graphStageName;

    private final String graphStageNodeName;

    private final Action<?, ?> action;

    private final ExtraParam extraParam;

    private final Condition condition;

    private final long timeout;

    private final boolean forced;

    private final List<Node> predecessorNodeList;

    private final List<Node> successorNodeList;
}
