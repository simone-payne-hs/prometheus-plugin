package org.jenkinsci.plugins.prometheus.listener;

import hudson.Extension;
import hudson.model.*;
import hudson.model.listeners.RunListener;
import hudson.model.Run;

import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.jenkinsci.plugins.prometheus.util.NodeInfo;


@Extension
public class PrometheusPipelineListener extends RunListener<Run> {
    private static final Logger logger = LoggerFactory.getLogger(PrometheusPipelineListener.class);
    public static final String GENERATION_ERROR = "Failed to collect node queue info with exception: ";

    @Override
    public void onStarted(Run run, TaskListener listener) {
        try {
            NodeInfo node = new NodeInfo();
            String test = getAgentNodeInfo(run, node);
            logger.debug("{ON STARTED}", test);
        } catch (Exception e) {
            String errorMessage = GENERATION_ERROR + e.getMessage();
            logger.debug("[{}] [{}]", errorMessage, e);
            listener.error(errorMessage);
        }
    }

    @Override
    public void onCompleted(Run run, @Nonnull TaskListener listener) {
        try {
            NodeInfo node = new NodeInfo();
            getAgentNodeInfo(run, node);
            logger.debug("{ON COMPLETED}", node.getNodeName());
        } catch (Exception e) {
            String errorMessage = GENERATION_ERROR + e.getMessage();
            logger.debug("[{}] [{}]", errorMessage, e);
            listener.error(errorMessage);
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
