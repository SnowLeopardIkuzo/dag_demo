package com.example.dag.frame.loader;

import com.example.dag.frame.exception.DagException;
import com.example.dag.frame.graph.Graph;
import com.example.dag.frame.node.Node;
import com.example.dag.frame.node.NodeType;
import guru.nidi.graphviz.attribute.*;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.model.MutableNode;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static guru.nidi.graphviz.model.Factory.mutGraph;
import static guru.nidi.graphviz.model.Factory.mutNode;

@Slf4j
public class GraphImageLoader {

    public static Map<String, byte[]> loadGraphImage(Map<String, Graph> graphMap) {
        log.info("start loading graph images");

        Map<String, byte[]> graphImageMap = new HashMap<>(graphMap.size());
        for (Graph graph : graphMap.values()) {
            MutableGraph mutableGraph = mutGraph(graph.getGraphName()).setDirected(true)
                    .graphAttrs().add(Rank.dir(Rank.RankDir.TOP_TO_BOTTOM));

            Map<Node, MutableNode> nodeMap = new HashMap<>(graph.getAllNodeList().size());
            for (Node node : graph.getAllNodeList()) {
                MutableNode mutableNode = mutNode(node.getNodeName());
                if (node.getType() == NodeType.NORMAL) {
                    if (node.isForced()) {
                        mutableNode.add(Color.GREEN);
                    }

                    List<String> htmlLineList = new ArrayList<>();
                    htmlLineList.add("<b>" + node.getNodeName() + "</b>");
                    if (node.getCondition() != null) {
                        htmlLineList.add(node.getCondition().getClass().getSimpleName());
                    }
                    if (node.getTimeout() > 0) {
                        htmlLineList.add("<i>timeout: " + node.getTimeout() + "ms</i>");
                    }
                    if (node.isForced()) {
                        htmlLineList.add("<i>forced: true</i>");
                    }

                    mutableNode.setName(Label.htmlLines(htmlLineList.toArray(String[]::new)));
                } else {
                    mutableNode.add(Shape.RECTANGLE, Style.FILLED);
                }
                nodeMap.put(node, mutableNode);
            }

            for (Node node : graph.getAllNodeList()) {
                MutableNode mutableNode = nodeMap.get(node);
                for (Node successorNode : node.getSuccessorNodeList()) {
                    mutableNode.addLink(nodeMap.get(successorNode));
                }
            }

            for (MutableNode mutableNode : nodeMap.values()) {
                mutableGraph.add(mutableNode);
            }

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            try {
                Graphviz.fromGraph(mutableGraph).render(Format.PNG).toOutputStream(byteArrayOutputStream);
            } catch (IOException e) {
                throw new DagException("load graph image " + graph.getGraphName() + " error", e);
            }
            graphImageMap.put(graph.getGraphName(), byteArrayOutputStream.toByteArray());
        }

        log.info("end loading graph images");
        return graphImageMap;
    }

}
