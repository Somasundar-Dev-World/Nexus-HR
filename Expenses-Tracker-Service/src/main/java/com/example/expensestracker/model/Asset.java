package com.example.expensestracker.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
public class Asset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private BigDecimal currentValue;
    
    private String institutionName;
    private String accountNumber;
    
    @Column(length = 1000)
    private String note;
        
    private Long userId;

    public Asset() {
    }

    public Asset(String name, BigDecimal currentValue) {
        this.name = name;
        this.currentValue = currentValue;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public BigDecimal getCurrentValue() { return currentValue; }
    public void setCurrentValue(BigDecimal currentValue) { this.currentValue = currentValue; }

    public String getInstitutionName() { return institutionName; }
    public void setInstitutionName(String institutionName) { this.institutionName = institutionName; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
}
