package com.phungloccoffee.model.dashboard;

public record DashboardMetric(String branch, String metric, String value, MetricStatus status, String note) {
}
