package org.jenkinsci.plugins.prometheus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.prometheus.client.Collector;
import io.prometheus.client.Summary;
import org.jenkinsci.plugins.prometheus.config.PrometheusConfiguration;
import org.jenkinsci.plugins.prometheus.util.NodeInfo;
// import org.jenkinsci.plugins.prometheus.listener.PrometheusPipelineListener;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import hudson.model.AbstractBuild;
import hudson.model.Executor;
import hudson.model.Node;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;

public class NodeCollector extends Collector {
    private static final Logger logger = LoggerFactory.getLogger(NodeCollector.class);
    private NodeInfo node = new NodeInfo();

    @Override
    public List<MetricFamilySamples> collect(){
        logger.debug("Collecting node metrics for prometheus");
        List<MetricFamilySamples> data = new ArrayList<MetricFamilySamples>();

        boolean ignoreBuildMetrics = !PrometheusConfiguration.get().isCollectNodeQueueDuration();
        if (ignoreBuildMetrics) {
            logger.debug("not collecting node queue duration");
            return data;
        }
        String fullname = "nodes";

        String[] labelKeyArray = {"nodeName"};
        String nodeName = cleanNodeName(node.getNodeName());
        String[] labelValueArray = {nodeName};
        Summary nodeDuration = Summary.build()
                .name(fullname + "_node_queue_duration_milliseconds_test")
                .help("Time node spent in queue in milliseconds")
                .labelNames(labelKeyArray)
                .create();

        nodeDuration.labels(labelValueArray).observe(node.getQueueDuration());
        nodeDuration.collect();

        data.addAll(nodeDuration.collect());

        return data;
    }

    private String cleanNodeName(String nodeName) {
        boolean useFullNodeName = !PrometheusConfiguration.get().isUseNodeFullName();
        if (useFullNodeName) {
            logger.debug("not cleaning node name");
            return nodeName;
        }
        if (nodeName.equals("master")) {
            return nodeName;
        }
        String[] splitName = nodeName.split("-");
        int numSplits = splitName.length;
        String cleanNodeName = nodeName.replace("-" + splitName[numSplits - 1], "");

        return cleanNodeName;
    }


    public class PrometheusPipelineListener extends RunListener<Run> {
        public static final String GENERATION_ERROR = "Failed to collect node queue info with exception: ";
    
        @Override
        public void onCompleted(Run run, @Nonnull TaskListener listener) {
            try {
                getAgentNodeInfo(run, node);
                // PipelineNodeCollector collecter = new PipelineNodeCollector();
                // collecter.collect();
                collect();
                logger.debug("ON COMPLETED {}", node.getNodeName());
            } catch (Exception e) {
                String errorMessage = GENERATION_ERROR + e.getMessage();
                logger.debug("[{}] [{}]", errorMessage, e);
                listener.error(errorMessage);
            }
        }
    
        public String getAgentNodeInfo(Run buildInfo, NodeInfo nodeInfo){
            Executor executor = buildInfo.getExecutor();
            if (executor != null) {
    
                //TEST LOGS - REMOVE LATER
                logger.debug("NAME: {}", executor.getName());
                logger.debug("OWNER: {}", executor.getOwner());
                logger.debug("DISPLAY NAME: {}", executor.getDisplayName());
                Node node = executor.getOwner().getNode();
                if (node != null) {
                    logger.debug("OWNER NODE NAME: {}", node);
                    logger.debug("OWNER NODE NAME: {}", node.getNodeName());
                    logger.debug("OWNER NODE LABEL STRING: [{}]", node.getLabelString());
                }
    
                nodeInfo.setQueueDuration(executor.getTimeSpentInQueue());
    
                if (buildInfo instanceof AbstractBuild) {
                    String builtOnStr = ((AbstractBuild) buildInfo).getBuiltOnStr();
                    if ("".equals(builtOnStr)) {
                        logger.debug("setting here");
                        nodeInfo.setNodeName("master");
                    } else {
                        logger.debug("setting here 2");
                        nodeInfo.setNodeName(builtOnStr);
                    }
                } else {
                    logger.debug("setting default here 3");
                    nodeInfo.setNodeName("master");
                }
                logger.debug("queue duration is [{}] for job [{}] running on [{}]", nodeInfo.getQueueDuration(), buildInfo.getFullDisplayName(), nodeInfo.getNodeName());
                return nodeInfo.getNodeName();
            }
            logger.debug("unable to establish executor for job {}", buildInfo.getDisplayName());
            return nodeInfo.getNodeName();
        }
    }
}
