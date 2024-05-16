package com.example.dag.business.service;

import com.example.dag.business.context.BusinessContext;
import com.example.dag.business.exception.BusinessException;
import com.example.dag.frame.async.AsyncTaskResult;
import com.example.dag.frame.engine.DirectedAcyclicGraphExecutorEngine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class BusinessService {

    public int sync(String graphName) {
        BusinessContext businessContext = new BusinessContext(graphName);
        DirectedAcyclicGraphExecutorEngine.syncExecuteGraph(businessContext);
        if (businessContext.isStopped()) {
            throw new BusinessException(businessContext.getStopReason());
        }
        return businessContext.getMergedList().size();
    }

    public int syncWithTimeout(String graphName, long timeout) {
        BusinessContext businessContext = new BusinessContext(graphName);
        boolean finished = DirectedAcyclicGraphExecutorEngine.syncExecuteGraphWithTimeout(businessContext, timeout);
        if (!finished || businessContext.isStopped()) {
            throw new BusinessException(businessContext.getStopReason());
        }
        return businessContext.getMergedList().size();
    }

    public int async(String graphName, long timeout) {
        BusinessContext businessContext = new BusinessContext(graphName);
        AsyncTaskResult asyncTaskResult = DirectedAcyclicGraphExecutorEngine.asyncExecuteGraph(businessContext);
        boolean finished = asyncTaskResult.get(timeout);
        if (!finished || businessContext.isStopped()) {
            throw new BusinessException(businessContext.getStopReason());
        }
        return businessContext.getMergedList().size();
    }

    public byte[] getGraphImage(String graphName) {
        return DirectedAcyclicGraphExecutorEngine.getGraphImage(graphName);
    }

}
