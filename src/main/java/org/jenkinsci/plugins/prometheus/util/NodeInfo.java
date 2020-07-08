package org.jenkinsci.plugins.prometheus.util;

public class NodeInfo {
    private String nodeName = "";
    private long queueDuration = 0;

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public long getQueueDuration() {
        return queueDuration;
    }

    public void setQueueDuration(long queueDuration) {
        this.queueDuration = queueDuration;
    }
}
