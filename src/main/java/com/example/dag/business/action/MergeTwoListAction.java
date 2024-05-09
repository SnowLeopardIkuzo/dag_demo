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

@NodeAction
public class MergeTwoListAction implements Action<MergeTwoListAction.Param, MergeTwoListAction.Result> {

    @Getter
    @AllArgsConstructor
    public static class Param {
        private List<String> firstList;
        private List<String> secondList;
    }

    @Getter
    @AllArgsConstructor
    public static class Result {
        private List<String> mergedList;
    }

    @Override
    public Param extractParam(DagContext context, ExtraParam extraParam) {
        BusinessContext businessContext = (BusinessContext) context;
        return new Param(businessContext.getFirstList(), businessContext.getSecondList());
    }

    @Override
    public Result process(Param param) {
        List<String> firstList = param.getFirstList();
        List<String> secondList = param.getSecondList();
        List<String> mergedList = new ArrayList<>(firstList);
        mergedList.addAll(secondList);
        return new Result(mergedList);
    }

    @Override
    public void saveResult(DagContext context, Result result) {
        BusinessContext businessContext = (BusinessContext) context;
        businessContext.setMergedList(result.getMergedList());
    }
}
