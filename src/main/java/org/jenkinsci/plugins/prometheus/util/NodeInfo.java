package org.jenkinsci.plugins.prometheus.util;


public class NodeInfo {
    private long queueDuration;
    private String nodeName;

    public long getQueueDuration() {
        return queueDuration;
    }

    public void setQueueDuration(long queueDuration) {
        this.queueDuration = queueDuration;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }
}
