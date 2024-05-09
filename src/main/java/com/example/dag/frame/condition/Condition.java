package com.example.dag.frame.condition;

import com.example.dag.frame.context.DagContext;

public interface Condition {
    boolean meet(DagContext dagContext);
}
