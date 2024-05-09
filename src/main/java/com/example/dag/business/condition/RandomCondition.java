package com.example.dag.business.condition;

import com.example.dag.frame.annotation.NodeCondition;
import com.example.dag.frame.condition.Condition;
import com.example.dag.frame.context.DagContext;

import java.util.concurrent.ThreadLocalRandom;

@NodeCondition
public class RandomCondition implements Condition {
    @Override
    public boolean meet(DagContext dagContext) {
        return ThreadLocalRandom.current().nextBoolean();
    }
}
