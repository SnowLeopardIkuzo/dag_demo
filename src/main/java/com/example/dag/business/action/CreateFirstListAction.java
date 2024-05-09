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
public class CreateFirstListAction implements Action<CreateFirstListAction.Param, CreateFirstListAction.Result> {

    @Getter
    @AllArgsConstructor
    public static class Param {
        private int size;
    }

    @Getter
    @AllArgsConstructor
    public static class Result {
        private List<String> firstList;
    }

    @Override
    public Param extractParam(DagContext context, ExtraParam extraParam) {
        int size = Integer.parseInt(extraParam.getExtraParamValue("size", "1000"));
        return new Param(size);
    }

    @Override
    public Result process(Param param) {
        int size = param.getSize();
        List<String> firstList = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            firstList.add(String.valueOf(ThreadLocalRandom.current().nextLong()));
        }
        return new Result(firstList);
    }

    @Override
    public void saveResult(DagContext context, Result result) {
        BusinessContext businessContext = (BusinessContext) context;
        businessContext.setFirstList(result.getFirstList());
    }

}
