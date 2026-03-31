package com.crm.desktop.model;

/**
 * Client-side Lead model.
 */
public class Lead {
    private String id;
    private String title;
    private String stage;
    private String value;
    private String expectedCloseDate;
    private int probability;
    private String lostReason;
    private String customerName;
    private String ownerName;
    private String createdAt;
    private int version;

    public Lead() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getStage() { return stage; }
    public void setStage(String stage) { this.stage = stage; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public String getExpectedCloseDate() { return expectedCloseDate; }
    public void setExpectedCloseDate(String expectedCloseDate) { this.expectedCloseDate = expectedCloseDate; }

    public int getProbability() { return probability; }
    public void setProbability(int probability) { this.probability = probability; }

    public String getLostReason() { return lostReason; }
    public void setLostReason(String lostReason) { this.lostReason = lostReason; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
}
