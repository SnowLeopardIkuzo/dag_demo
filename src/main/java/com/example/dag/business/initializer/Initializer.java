package com.example.dag.business.initializer;

import com.example.dag.frame.engine.DirectedAcyclicGraphExecutorEngine;
import org.springframework.beans.BeansException;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class Initializer implements CommandLineRunner, ApplicationContextAware {

    public static final String PATH = "classpath*:dag/**";

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void run(String... args) throws Exception {
        DirectedAcyclicGraphExecutorEngine.loadGraph(PATH, applicationContext);
    }

}
