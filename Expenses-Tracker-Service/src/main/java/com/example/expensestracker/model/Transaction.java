package com.example.expensestracker.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate date;
    private LocalDate originalDate;
    
    private String accountType;
    private String accountName;
    private String accountNumber;
    private String institutionName;
    
    private String name;
    private String customName;
    
    @Column(precision = 19, scale = 4)
    private BigDecimal amount;
    
    private String description;
    private String category;
    
    @Column(length = 1000)
    private String note;
    
    private String ignoredFrom;
    private Boolean taxDeductible;
    
    @Column(length = 500)
    private String transactionTags;

    @Enumerated(EnumType.STRING)
    private TransactionType type;

    private Long userId;

    public Transaction() {
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public LocalDate getOriginalDate() { return originalDate; }
    public void setOriginalDate(LocalDate originalDate) { this.originalDate = originalDate; }

    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }

    public String getAccountName() { return accountName; }
    public void setAccountName(String accountName) { this.accountName = accountName; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public String getInstitutionName() { return institutionName; }
    public void setInstitutionName(String institutionName) { this.institutionName = institutionName; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCustomName() { return customName; }
    public void setCustomName(String customName) { this.customName = customName; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public String getIgnoredFrom() { return ignoredFrom; }
    public void setIgnoredFrom(String ignoredFrom) { this.ignoredFrom = ignoredFrom; }

    public Boolean getTaxDeductible() { return taxDeductible; }
    public void setTaxDeductible(Boolean taxDeductible) { this.taxDeductible = taxDeductible; }

    public String getTransactionTags() { return transactionTags; }
    public void setTransactionTags(String transactionTags) { this.transactionTags = transactionTags; }

    public TransactionType getType() { return type; }
    public void setType(TransactionType type) { this.type = type; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
}
