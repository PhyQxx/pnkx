package com.pnkx.blog.domain.po;

public class PxAnalyticsEvent {
    private String eventId;
    public String getEventId() { return eventId; }
    public void setEventId(String value) { eventId = value; }
    private String visitorId;
    public String getVisitorId() { return visitorId; }
    public void setVisitorId(String value) { visitorId = value; }
    private String sessionId;
    public String getSessionId() { return sessionId; }
    public void setSessionId(String value) { sessionId = value; }
    private String pageViewId;
    public String getPageViewId() { return pageViewId; }
    public void setPageViewId(String value) { pageViewId = value; }
    private String eventName;
    public String getEventName() { return eventName; }
    public void setEventName(String value) { eventName = value; }
    private String pageKey;
    public String getPageKey() { return pageKey; }
    public void setPageKey(String value) { pageKey = value; }
    private String contentId;
    public String getContentId() { return contentId; }
    public void setContentId(String value) { contentId = value; }
    private String operationId;
    public String getOperationId() { return operationId; }
    public void setOperationId(String value) { operationId = value; }
    private String referrerHost;
    public String getReferrerHost() { return referrerHost; }
    public void setReferrerHost(String value) { referrerHost = value; }
    private Long durationMs;
    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long value) { durationMs = value; }
    private Long occurredAt;
    public Long getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Long value) { occurredAt = value; }
    private java.time.LocalDateTime eventTime;
    public java.time.LocalDateTime getEventTime() { return eventTime; }
    public void setEventTime(java.time.LocalDateTime value) { eventTime = value; }
    private String userId;
    public String getUserId() { return userId; }
    public void setUserId(String value) { userId = value; }
}
