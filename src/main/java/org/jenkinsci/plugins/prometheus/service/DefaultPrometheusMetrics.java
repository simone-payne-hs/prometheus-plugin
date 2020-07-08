package org.jenkinsci.plugins.prometheus.service;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.dropwizard.DropwizardExports;
import io.prometheus.client.exporter.common.TextFormat;
import io.prometheus.client.hotspot.DefaultExports;
import jenkins.metrics.api.Metrics;
import org.jenkinsci.plugins.prometheus.DiskUsageCollector;
import org.jenkinsci.plugins.prometheus.JenkinsStatusCollector;
import org.jenkinsci.plugins.prometheus.JobCollector;
import org.jenkinsci.plugins.prometheus.NodeCollector;
// import org.jenkinsci.plugins.prometheus.listener.PrometheusPipelineListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.util.concurrent.atomic.AtomicReference;

public class DefaultPrometheusMetrics implements PrometheusMetrics {

    private static final Logger logger = LoggerFactory.getLogger(DefaultPrometheusMetrics.class);

    private final CollectorRegistry collectorRegistry;
    private final AtomicReference<String> cachedMetrics;

    public DefaultPrometheusMetrics() {
        CollectorRegistry collectorRegistry = CollectorRegistry.defaultRegistry;
        collectorRegistry.register(new JobCollector());
        collectorRegistry.register(new JenkinsStatusCollector());
        collectorRegistry.register(new DropwizardExports(Metrics.metricRegistry()));
        collectorRegistry.register(new DiskUsageCollector());
        collectorRegistry.register(new NodeCollector());
        // PrometheusPipelineListener.PipelineNodeCollector nodeCollector = new PrometheusPipelineListener.PipelineNodeCollector();
        // collectorRegistry.register(nodeCollector);
        DefaultExports.initialize();

        this.collectorRegistry = collectorRegistry;
        this.cachedMetrics = new AtomicReference<>("");
    }

    @Override
    public String getMetrics() {
        return cachedMetrics.get();
    }

    @Override
    public void collectMetrics() {
        try (StringWriter buffer = new StringWriter()) {
            TextFormat.write004(buffer, collectorRegistry.metricFamilySamples());
            cachedMetrics.set(buffer.toString());
        } catch (IOException e) {
            logger.debug("Unable to collect metrics");
        }
    }
}
