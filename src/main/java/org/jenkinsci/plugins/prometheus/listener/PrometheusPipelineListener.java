package org.jenkinsci.plugins.prometheus.listener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import org.jenkinsci.plugins.prometheus.config.PrometheusConfiguration;
import org.jenkinsci.plugins.prometheus.util.ConfigurationUtils;
import org.jenkinsci.plugins.prometheus.util.NodeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.Executor;
import hudson.model.Node;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;
import io.prometheus.client.Collector.MetricFamilySamples;
import io.prometheus.client.Collector;
import io.prometheus.client.Histogram;
import io.prometheus.client.Summary;

@Extension
public class PrometheusPipelineListener extends RunListener<Run> {
    private static final Logger logger = LoggerFactory.getLogger(PrometheusPipelineListener.class);
    public static final String GENERATION_ERROR = "Failed to collect node queue info with exception: ";
    private NodeInfo node = new NodeInfo();

    @Override
    public void onStarted(Run run, TaskListener listener) {
        try {
            getAgentNodeInfo(run, node);
            logger.debug("ON STARTED {}", node.getNodeName());
        } catch (Exception e) {
            String errorMessage = GENERATION_ERROR + e.getMessage();
            logger.debug("[{}] [{}]", errorMessage, e);
            listener.error(errorMessage);
        }
    }

    @Override
    public void onCompleted(Run run, @Nonnull TaskListener listener) {
        try {
            getAgentNodeInfo(run, node);
            CustomCollector collecter = new CustomCollector();
            collecter.collect();
            logger.debug("ON COMPLETED {}", node.getNodeName());
        } catch (Exception e) {
            String errorMessage = GENERATION_ERROR + e.getMessage();
            logger.debug("[{}] [{}]", errorMessage, e);
            listener.error(errorMessage);
        }
    }

    private class CustomCollector extends Collector {
        public List<MetricFamilySamples> collect(){
            logger.debug("Collecting node metrics for prometheus");
            List<MetricFamilySamples> data = new ArrayList<MetricFamilySamples>();

            boolean ignoreBuildMetrics = !PrometheusConfiguration.get().isCollectNodeQueueDuration();
            if (ignoreBuildMetrics) {
                logger.debug("ignoring");
                return data;
            }
            String fullname = "nodes";

            String[] labelNameArray = {node.getNodeName(), "queueDuration"};
            Summary nodeDuration = Summary.build()
                    .name(fullname + "_node_queue_duration_milliseconds_test")
                    .help("Time node spent in queue in milliseconds")
                    .labelNames(labelNameArray)
                    .create();

            Histogram nodeDurationHistogram = Histogram.build()
                .name(fullname + "_node_queue_duration_milliseconds_histogram_test")
                .help("Time node spent in queue in milliseconds as histogram")
                .labelNames(labelNameArray)
                .create();

            nodeDuration.labels(labelNameArray).observe(node.getQueueDuration());
            nodeDurationHistogram.labels(labelNameArray).observe(node.getQueueDuration());
            nodeDuration.register();
            nodeDuration.collect();
            nodeDurationHistogram.register();
            nodeDurationHistogram.collect();

            data.addAll(nodeDuration.collect());
            data.addAll(nodeDurationHistogram.collect());

            return data;
        }
    }


    public static String getAgentNodeInfo(Run buildInfo, NodeInfo nodeInfo){
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
