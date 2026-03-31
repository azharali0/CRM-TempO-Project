package com.crm.desktop.model;

/**
 * Client-side Customer model.
 */
public class Customer {
    private String id;
    private String name;
    private String email;
    private String phone;
    private String maskedPhone;
    private String company;
    private String address;
    private String city;
    private String status;
    private String assignedToName;
    private String createdAt;
    private int version;

    public Customer() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getMaskedPhone() { return maskedPhone; }
    public void setMaskedPhone(String maskedPhone) { this.maskedPhone = maskedPhone; }

    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getAssignedToName() { return assignedToName; }
    public void setAssignedToName(String assignedToName) { this.assignedToName = assignedToName; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
}
