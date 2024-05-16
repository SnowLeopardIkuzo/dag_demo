package com.example.dag.frame.engine;

import com.example.dag.frame.async.AsyncTaskResult;
import com.example.dag.frame.context.DagContext;
import com.example.dag.frame.exception.DagException;
import com.example.dag.frame.graph.Graph;
import com.example.dag.frame.loader.GraphImageLoader;
import com.example.dag.frame.loader.GraphLoader;
import com.example.dag.frame.recorder.DagRecorder;
import com.example.dag.frame.task.TaskDispatcher;
import org.springframework.context.ApplicationContext;

import java.util.HashMap;
import java.util.Map;

public class DirectedAcyclicGraphExecutorEngine {

    private static Map<String, Graph> graphMap = new HashMap<>();

    private static Map<String, byte[]> graphImageMap = new HashMap<>();

    public static synchronized void loadGraph(String path, ApplicationContext applicationContext) {
        graphMap = GraphLoader.load(path, applicationContext);
        graphImageMap = GraphImageLoader.loadGraphImage(graphMap);
    }

    public static void syncExecuteGraph(DagContext dagContext) {
        String graphName = dagContext.getGraphName();
        Graph graph = graphMap.get(graphName);
        if (graph == null) {
            throw new DagException("graph " + graphName + " not found");
        }
        dagContext.setDagRecorder(new DagRecorder(graph.getStageNum(), graph.getAllNodeList().size()));
        TaskDispatcher taskDispatcher = new TaskDispatcher(graph, dagContext);
        taskDispatcher.dispatch();
    }

    public static boolean syncExecuteGraphWithTimeout(DagContext dagContext, long timeout) {
        String graphName = dagContext.getGraphName();
        Graph graph = graphMap.get(graphName);
        if (graph == null) {
            throw new DagException("graph " + graphName + " not found");
        }
        dagContext.setDagRecorder(new DagRecorder(graph.getStageNum(), graph.getAllNodeList().size()));
        TaskDispatcher taskDispatcher = new TaskDispatcher(graph, dagContext);
        return taskDispatcher.dispatch(timeout);
    }

    public static AsyncTaskResult asyncExecuteGraph(DagContext dagContext) {
        String graphName = dagContext.getGraphName();
        Graph graph = graphMap.get(graphName);
        if (graph == null) {
            throw new DagException("graph " + graphName + " not found");
        }
        dagContext.setDagRecorder(new DagRecorder(graph.getStageNum(), graph.getAllNodeList().size()));
        TaskDispatcher taskDispatcher = new TaskDispatcher(graph, dagContext);
        return taskDispatcher.asyncDispatch();
    }

    public static byte[] getGraphImage(String graphName) {
        return graphImageMap.get(graphName);
    }

}
