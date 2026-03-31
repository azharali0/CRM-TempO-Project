package com.crm.desktop.model;

/**
 * Client-side Interaction model.
 */
public class Interaction {
    private String id;
    private String customerId;
    private String type;
    private String subject;
    private String notes;
    private Integer duration;
    private String loggedByName;
    private String createdAt;

    public Interaction() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Integer getDuration() { return duration; }
    public void setDuration(Integer duration) { this.duration = duration; }

    public String getLoggedByName() { return loggedByName; }
    public void setLoggedByName(String loggedByName) { this.loggedByName = loggedByName; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
