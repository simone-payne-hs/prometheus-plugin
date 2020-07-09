package org.jenkinsci.plugins.prometheus.listener;

import hudson.Extension;
import hudson.model.Computer;
import hudson.model.TaskListener;
import hudson.slaves.ComputerListener;

import java.util.ArrayList;
import java.util.List;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.prometheus.client.Summary;
import io.prometheus.client.Gauge;
import io.prometheus.client.Collector;
import org.jenkinsci.plugins.prometheus.util.NodeInfo;
import org.jenkinsci.plugins.prometheus.config.PrometheusConfiguration;

/**
    * Triggered when computer launched or online
    * Reports metrics on how long it takes for computer to boot
*/
@Extension
public class PipelineComputerListener extends ComputerListener {
    private static final Logger logger = LoggerFactory.getLogger(PipelineComputerListener.class);

    public static final String PRELAUNCH_ERROR = "Failed to collect node onfo on start with exception: ";
    public static final String ONLINE_ERROR = "Failed to collect node onfo on start with exception: ";

    private NodeInfo node = new NodeInfo();

    String[] labelKeyArray = {"nodeName"};
    private Summary nodeDuration = Summary.build().name("nodes_node_queue_duration_milliseconds_test_PIPELINE")
            .help("Time node spent in queue in milliseconds").labelNames(labelKeyArray).register();

    private Gauge nodeDurationGauge = Gauge.build().name("nodes_node_queue_duration_milliseconds_test_GAUGE_PIPELINE")
            .help("Time node spent in queue in milliseconds as gauge").labelNames(labelKeyArray).register();


    @Override
    public void preLaunch(Computer computer, TaskListener listener) throws IOException, InterruptedException {
        try {
            node.setLaunchTime(java.lang.System.currentTimeMillis());;
        } catch (Exception e) {
            String errorMessage = PRELAUNCH_ERROR + e.getMessage();
            logger.debug("[{}] [{}] v2", errorMessage, e);
            listener.error(errorMessage);
        }
    }

    @Override
    public void onOnline(Computer computer, TaskListener listener) throws IOException, InterruptedException {
        try {
            node.setNodeName(computer.getName());
            node.setStartTime(java.lang.System.currentTimeMillis());

            // log to Prometheus
            PipelineNodeCollector collecter = new PipelineNodeCollector();
            collecter.collect();
        } catch (Exception e) {
            String errorMessage = ONLINE_ERROR + e.getMessage();
            logger.debug("[{}] [{}] v2", errorMessage, e);
            listener.error(errorMessage);
        }
    }
    
    /**
     * For Jenkins jobs running ephemeral nodes on kubernetes.
     * Removes random string at end of node name.
     * @param nodeName name of node to be cleaned
     * @return Shortened string of node name
     */
    private String cleanNodeName(String nodeName) {
        boolean useFullNodeName = !PrometheusConfiguration.get().isUseNodeFullName();
        if (useFullNodeName) {
            logger.debug("not cleaning node name");
            return nodeName;
        }
        if (nodeName.equals("master") || nodeName.equals("")) {
            return "master";
        }
        String[] splitName = nodeName.split("-");
        int numSplits = splitName.length;
        String cleanedNodeName = nodeName.replace("-" + splitName[numSplits-1], "");

        return cleanedNodeName;
    }

    /**
     * Log node information to prometheus endpoint
     */
    public class PipelineNodeCollector extends Collector {
        public List<MetricFamilySamples> collect() {
            logger.debug("Collecting node metrics for prometheus");
            List<MetricFamilySamples> data = new ArrayList<MetricFamilySamples>();

            // Don't run if collecting node queue duration toggled off.
            boolean ignoreBuildMetrics = !PrometheusConfiguration.get().isCollectNodeQueueDuration();
            if (ignoreBuildMetrics) {
                logger.debug("not collecting node queue duration");
                return data;
            }

            String nodeName = cleanNodeName(node.getNodeName());
            String[] labelValueArray = { nodeName };
            node.setQueueDuration(node.getStartTime() - node.getLaunchTime());

            nodeDuration.labels(labelValueArray).observe(node.getQueueDuration());
            nodeDuration.collect();
            nodeDurationGauge.labels(labelValueArray).set(node.getQueueDuration());
            nodeDurationGauge.collect();

            data.addAll(nodeDuration.collect());
            data.addAll(nodeDurationGauge.collect());

            return data;
        }
    }
}
