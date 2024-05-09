package com.example.dag.business.action;

import com.example.dag.frame.action.Action;
import com.example.dag.frame.annotation.NodeAction;
import com.example.dag.frame.context.DagContext;
import com.example.dag.frame.extraparam.ExtraParam;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@NodeAction
public class SimpleAction implements Action<SimpleAction.Param, SimpleAction.Result> {

    public static class Param {

    }

    public static class Result {

    }

    @Override
    public Param extractParam(DagContext context, ExtraParam extraParam) {
        return null;
    }

    @Override
    public Result process(Param param) {
        try {
            Thread.sleep(ThreadLocalRandom.current().nextLong(10, 50));
        } catch (InterruptedException e) {
            log.error("simple action interrupted", e);
        }
        log.info("simple action process");
        return null;
    }

    @Override
    public void saveResult(DagContext context, Result result) {

    }

}
