package com.example.expensestracker.controller;

import com.example.expensestracker.model.Asset;
import com.example.expensestracker.model.Liability;
import com.example.expensestracker.model.Transaction;
import com.example.expensestracker.model.TransactionType;
import com.example.expensestracker.model.Category;
import com.example.expensestracker.repository.AssetRepository;
import com.example.expensestracker.repository.LiabilityRepository;
import com.example.expensestracker.repository.TransactionRepository;
import com.example.expensestracker.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // For local UI tests
public class FinanceController {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AssetRepository assetRepository;

    @Autowired
    private LiabilityRepository liabilityRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    // --- Transactions ---

    @GetMapping("/transactions")
    public List<Transaction> getAllTransactions(@RequestAttribute("userId") Long userId) {
        return transactionRepository.findByUserId(userId);
    }

    @PostMapping("/transactions")
    public Transaction addTransaction(@RequestAttribute("userId") Long userId, @RequestBody Transaction transaction) {
        transaction.setUserId(userId);
        return transactionRepository.save(transaction);
    }
    
    @PutMapping("/transactions/{id}")
    public ResponseEntity<Transaction> updateTransaction(@PathVariable Long id, @RequestBody Transaction details) {
        return transactionRepository.findById(id).map(t -> {
            t.setDescription(details.getDescription());
            t.setAmount(details.getAmount());
            t.setDate(details.getDate());
            t.setType(details.getType());
            t.setCategory(details.getCategory());
            return ResponseEntity.ok(transactionRepository.save(t));
        }).orElse(ResponseEntity.notFound().build());
    }
    
    @DeleteMapping("/transactions/{id}")
    public ResponseEntity<?> deleteTransaction(@PathVariable Long id) {
        transactionRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    // --- Assets ---

    @GetMapping("/assets")
    public List<Asset> getAllAssets(@RequestAttribute("userId") Long userId) {
        return assetRepository.findByUserId(userId);
    }

    @PostMapping("/assets")
    public Asset addAsset(@RequestAttribute("userId") Long userId, @RequestBody Asset asset) {
        asset.setUserId(userId);
        return assetRepository.save(asset);
    }

    @PutMapping("/assets/{id}")
    public ResponseEntity<Asset> updateAsset(@PathVariable Long id, @RequestBody Asset details) {
        return assetRepository.findById(id).map(a -> {
            a.setName(details.getName());
            a.setCurrentValue(details.getCurrentValue());
            return ResponseEntity.ok(assetRepository.save(a));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/assets/{id}")
    public ResponseEntity<?> deleteAsset(@PathVariable Long id) {
        assetRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    // --- Liabilities ---

    @GetMapping("/liabilities")
    public List<Liability> getAllLiabilities(@RequestAttribute("userId") Long userId) {
        return liabilityRepository.findByUserId(userId);
    }

    @PostMapping("/liabilities")
    public Liability addLiability(@RequestAttribute("userId") Long userId, @RequestBody Liability liability) {
        liability.setUserId(userId);
        return liabilityRepository.save(liability);
    }

    @PutMapping("/liabilities/{id}")
    public ResponseEntity<Liability> updateLiability(@PathVariable Long id, @RequestBody Liability details) {
        return liabilityRepository.findById(id).map(l -> {
            l.setName(details.getName());
            l.setAmount(details.getAmount());
            return ResponseEntity.ok(liabilityRepository.save(l));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/liabilities/{id}")
    public ResponseEntity<?> deleteLiability(@PathVariable Long id) {
        liabilityRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    // --- Categories ---

    @GetMapping("/categories")
    public List<Category> getAllCategories(@RequestAttribute("userId") Long userId) {
        return categoryRepository.findByUserId(userId);
    }

    @PostMapping("/categories")
    public Category addCategory(@RequestAttribute("userId") Long userId, @RequestBody Category category) {
        category.setUserId(userId);
        return categoryRepository.save(category);
    }

    @PutMapping("/categories/{id}")
    public ResponseEntity<Category> updateCategory(@PathVariable Long id, @RequestBody Category details, @RequestAttribute("userId") Long userId) {
        return categoryRepository.findById(id).map(c -> {
            String oldName = c.getName();
            c.setName(details.getName());
            categoryRepository.save(c);
            
            // Cascade update to raw strings
            if (oldName != null && !oldName.equals(details.getName())) {
                List<Transaction> userT = transactionRepository.findByUserId(userId);
                for (Transaction t : userT) {
                    if (oldName.equals(t.getCategory())) {
                        t.setCategory(details.getName());
                        transactionRepository.save(t);
                    }
                }
            }
            return ResponseEntity.ok(c);
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/categories/{id}")
    public ResponseEntity<?> deleteCategory(@PathVariable Long id) {
        categoryRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    // --- Reports ---

    @GetMapping("/reports/summary")
    public Map<String, Object> getFinancialSummary(@RequestAttribute("userId") Long userId) {
        List<Transaction> incomes = transactionRepository.findByTypeAndUserId(TransactionType.INCOME, userId);
        List<Transaction> expenses = transactionRepository.findByTypeAndUserId(TransactionType.EXPENSE, userId);
        List<Asset> assets = assetRepository.findByUserId(userId);
        List<Liability> liabilities = liabilityRepository.findByUserId(userId);

        BigDecimal totalIncome = incomes.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalExpense = expenses.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalAssets = assets.stream()
                .map(Asset::getCurrentValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalLiabilities = liabilities.stream()
                .map(Liability::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Net Worth = (Assets - Liabilities) + (Total Income - Total Expense)
        BigDecimal netWorth = totalAssets.subtract(totalLiabilities).add(totalIncome).subtract(totalExpense);
        
        // Very basic future prediction: Assuming average monthly trend continues for next month.
        // For simplicity, let's just predict netWorth + (totalIncome - totalExpense).
        BigDecimal netCashFlow = totalIncome.subtract(totalExpense);
        BigDecimal predictedNetWorth1Month = netWorth.add(netCashFlow);

        Map<String, Object> report = new HashMap<>();
        report.put("totalIncome", totalIncome);
        report.put("totalExpense", totalExpense);
        report.put("totalAssets", totalAssets);
        report.put("totalLiabilities", totalLiabilities);
        report.put("netWorth", netWorth);
        report.put("predictedNetWorthNextMonth", predictedNetWorth1Month);
        
        return report;
    }
}
