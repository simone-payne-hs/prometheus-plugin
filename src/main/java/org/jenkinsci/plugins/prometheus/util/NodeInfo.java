package org.jenkinsci.plugins.prometheus.util;

public class NodeInfo {
    private String nodeName = "";
    private long queueDuration = 0;
    private long launchTime = 0;
    private long startTime = 0;

    public String getNodeName() {
        return nodeName;
    }
    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }
    public long getLaunchTime() {
        return launchTime;
    }
    public void setLaunchTime(long launchTime) {
        this.launchTime = launchTime;
    }
    public long getStartTime() {
        return startTime;
    }
    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }
    public long getQueueDuration() {
        return queueDuration;
    }
    public void setQueueDuration(long queueDuration) {
        this.queueDuration = queueDuration;
    }
}
