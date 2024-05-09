package com.example.dag.frame.action;

import com.example.dag.frame.context.DagContext;
import com.example.dag.frame.extraparam.ExtraParam;

public interface Action<P, R> {
    P extractParam(DagContext context, ExtraParam extraParam);

    R process(P param);

    void saveResult(DagContext context, R result);
}
