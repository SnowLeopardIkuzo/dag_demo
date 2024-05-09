package com.example.dag.frame.task;

import com.example.dag.frame.action.Action;
import com.example.dag.frame.condition.Condition;
import com.example.dag.frame.context.DagContext;
import com.example.dag.frame.node.Node;
import lombok.Getter;

import java.util.concurrent.atomic.AtomicInteger;

public class NodeTask {

    private final AtomicInteger atomicPredecessorNum;

    @Getter
    private final Node node;

    private final DagContext dagContext;

    public NodeTask(Node node, DagContext dagContext) {
        this.node = node;
        this.atomicPredecessorNum = new AtomicInteger(node.getPredecessorNodeList().size());
        this.dagContext = dagContext;
    }

    public boolean decreaseOnePredecessorAndCheckStatus() {
        return atomicPredecessorNum.decrementAndGet() == 0;
    }

    public void execute() {
        Condition condition = node.getCondition();
        if (condition != null && !condition.meet(dagContext)) {
            return;
        }
        Action action = node.getAction();
        Object param = action.extractParam(dagContext, node.getExtraParam());
        Object result = action.process(param);
        action.saveResult(dagContext, result);
    }

}
