package com.example.dag.frame.async;

import com.example.dag.frame.context.DagContext;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Slf4j
@AllArgsConstructor
public class AsyncTaskResult {
    private final CountDownLatch countDownLatch;
    private final DagContext dagContext;

    public boolean get(long timeout) {
        boolean finished = false;
        try {
            finished = countDownLatch.await(timeout, TimeUnit.MILLISECONDS);
            if (!finished) {
                dagContext.stop("timeout");
            }
        } catch (InterruptedException e) {
            log.error("graph {} interrupted", dagContext.getGraphName(), e);
            dagContext.stop("interrupted");
        }
        return finished;
    }

}
