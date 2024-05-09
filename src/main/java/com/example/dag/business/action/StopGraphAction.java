package com.example.dag.business.action;

import com.example.dag.frame.action.Action;
import com.example.dag.frame.annotation.NodeAction;
import com.example.dag.frame.context.DagContext;
import com.example.dag.frame.extraparam.ExtraParam;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@NodeAction
public class StopGraphAction implements Action<StopGraphAction.Param, StopGraphAction.Result> {

    @Getter
    @AllArgsConstructor
    public static class Param {
        private String reason;
    }

    @Getter
    @AllArgsConstructor
    public static class Result {
        private String reason;
    }

    @Override
    public Param extractParam(DagContext context, ExtraParam extraParam) {
        return new Param(extraParam.getExtraParamValue("stopReason", "defaultReason"));
    }

    @Override
    public Result process(Param param) {
        try {
            Thread.sleep(ThreadLocalRandom.current().nextLong(10, 50));
        } catch (InterruptedException e) {
            log.error("stop graph interrupted", e);
        }
        return new Result(param.getReason());
    }

    @Override
    public void saveResult(DagContext context, Result result) {
        if (result.getReason() != null) {
            context.stop(result.getReason());
        }
    }

}
