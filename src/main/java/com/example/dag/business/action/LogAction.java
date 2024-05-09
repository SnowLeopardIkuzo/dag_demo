package com.example.dag.business.action;

import com.example.dag.business.context.BusinessContext;
import com.example.dag.frame.action.Action;
import com.example.dag.frame.annotation.NodeAction;
import com.example.dag.frame.context.DagContext;
import com.example.dag.frame.extraparam.ExtraParam;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@NodeAction
public class LogAction implements Action<LogAction.Param, Void> {

    @Getter
    @AllArgsConstructor
    public static class Param {
        private String graphName;
        private List<String> firstList;
        private List<String> secondList;
        private List<String> mergedList;
    }

    @Override
    public Param extractParam(DagContext context, ExtraParam extraParam) {
        BusinessContext businessContext = (BusinessContext) context;
        String graphName = businessContext.getGraphName();
        List<String> firstList = businessContext.getFirstList();
        List<String> secondList = businessContext.getSecondList();
        List<String> mergedList = businessContext.getMergedList();
        return new Param(graphName, firstList, secondList, mergedList);
    }

    @Override
    public Void process(Param param) {
        log.info("graph:{}, firstList:{}, secondList:{}, mergedList:{}", param.getGraphName(),
                param.getFirstList().size(), param.getSecondList().size(), param.getMergedList().size());
        return null;
    }

    @Override
    public void saveResult(DagContext context, Void result) {

    }

}
