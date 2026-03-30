package com.example.expensestracker.repository;

import com.example.expensestracker.model.Transaction;
import com.example.expensestracker.model.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByTypeAndUserId(TransactionType type, Long userId);
    List<Transaction> findByUserId(Long userId);
}
