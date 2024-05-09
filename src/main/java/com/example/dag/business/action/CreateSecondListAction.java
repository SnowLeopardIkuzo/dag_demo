package com.example.dag.business.action;

import com.example.dag.business.context.BusinessContext;
import com.example.dag.frame.action.Action;
import com.example.dag.frame.annotation.NodeAction;
import com.example.dag.frame.context.DagContext;
import com.example.dag.frame.extraparam.ExtraParam;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@NodeAction
public class CreateSecondListAction implements Action<CreateSecondListAction.Param, CreateSecondListAction.Result> {

    @Getter
    @AllArgsConstructor
    public static class Param {
        private long origin;
        private long bound;
        private int size;
    }

    @Getter
    @AllArgsConstructor
    public static class Result {
        private List<String> secondList;
    }

    @Override
    public Param extractParam(DagContext context, ExtraParam extraParam) {
        long origin = Long.parseLong(extraParam.getExtraParamValue("origin", "10000"));
        long bound = Long.parseLong(extraParam.getExtraParamValue("bound", "20000"));
        int size = Integer.parseInt(extraParam.getExtraParamValue("size", "2000"));
        return new Param(origin, bound, size);
    }

    @Override
    public Result process(Param param) {
        long origin = param.getOrigin();
        long bound = param.getBound();
        int size = param.getSize();
        List<String> secondList = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            secondList.add(String.valueOf(ThreadLocalRandom.current().nextLong(origin, bound)));
        }
        return new Result(secondList);
    }

    @Override
    public void saveResult(DagContext context, Result result) {
        BusinessContext businessContext = (BusinessContext) context;
        businessContext.setSecondList(result.getSecondList());
    }

}
