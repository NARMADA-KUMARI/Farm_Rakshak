package com.farmrakshak.shared.constants;

public final class KafkaTopics {
    private KafkaTopics() {}

    public static final String CROP_ANALYSIS_TOPIC = "crop-analysis-topic";
    public static final String ANALYSIS_RESULT_TOPIC = "analysis-result-topic";
    public static final String NOTIFICATION_TOPIC = "notification-topic";
    public static final String AUDIT_TOPIC = "audit-topic";

    public static final String FARM_EVENT_TOPIC = "farm-event-topic";
    public static final String FARM_CROP_EVENT_TOPIC = "farm-crop-event-topic";
    public static final String MARKET_ALERT_TOPIC = "market-alert-topic";
    public static final String DISEASE_ALERT_TOPIC = "disease-alert-topic";
}
