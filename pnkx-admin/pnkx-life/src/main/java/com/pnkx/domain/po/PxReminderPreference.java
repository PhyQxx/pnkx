package com.pnkx.domain.po;

import java.io.Serializable;

/** Per-user reminder channel and quiet-hours preferences. */
public class PxReminderPreference implements Serializable {
    private String userId;
    private Boolean websocketEnabled;
    private Boolean emailEnabled;
    private Boolean pushEnabled;
    private String quietStart;
    private String quietEnd;

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public Boolean getWebsocketEnabled() { return websocketEnabled; }
    public void setWebsocketEnabled(Boolean websocketEnabled) { this.websocketEnabled = websocketEnabled; }
    public Boolean getEmailEnabled() { return emailEnabled; }
    public void setEmailEnabled(Boolean emailEnabled) { this.emailEnabled = emailEnabled; }
    public Boolean getPushEnabled() { return pushEnabled; }
    public void setPushEnabled(Boolean pushEnabled) { this.pushEnabled = pushEnabled; }
    public String getQuietStart() { return quietStart; }
    public void setQuietStart(String quietStart) { this.quietStart = quietStart; }
    public String getQuietEnd() { return quietEnd; }
    public void setQuietEnd(String quietEnd) { this.quietEnd = quietEnd; }
}
