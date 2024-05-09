package com.example.dag.business.context;

import com.example.dag.frame.context.DagContext;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class BusinessContext extends DagContext {

    private List<String> firstList = new ArrayList<>();

    private List<String> secondList = new ArrayList<>();

    private List<String> mergedList = new ArrayList<>();

    public BusinessContext(String graphName) {
        super(graphName);
    }
}
