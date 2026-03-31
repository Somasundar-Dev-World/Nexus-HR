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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
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

    @PostMapping("/transactions/bulk")
    public List<Transaction> addTransactionsBulk(@RequestAttribute("userId") Long userId, @RequestBody List<Transaction> transactions) {
        // Fetch existing transactions to deduplicate
        List<Transaction> existingTransactions = transactionRepository.findByUserId(userId);
        
        // Map existing by composite key: Date + Amount + Description + Account
        Map<String, Transaction> existingMap = new HashMap<>();
        for (Transaction t : existingTransactions) {
            existingMap.put(generateCompositeKey(t), t);
        }

        List<Transaction> finalToSave = new ArrayList<>();
        
        for (Transaction incoming : transactions) {
            String key = generateCompositeKey(incoming);
            if (existingMap.containsKey(key)) {
                // UPDATE existing record with any NEW data from CSV
                Transaction existing = existingMap.get(key);
                updateIfAvailable(existing, incoming);
                finalToSave.add(existing);
            } else {
                // NEW record
                incoming.setUserId(userId);
                finalToSave.add(incoming);
            }
        }
        
        return transactionRepository.saveAll(finalToSave);
    }
    
    private String generateCompositeKey(Transaction t) {
        // Handle potential nulls
        String date = (t.getDate() != null) ? t.getDate().toString() : "0000-01-01";
        String amount = (t.getAmount() != null) ? t.getAmount().stripTrailingZeros().toPlainString() : "0.00";
        String desc = (t.getDescription() != null) ? t.getDescription().trim().toLowerCase() : "none";
        String acc = (t.getAccountNumber() != null) ? t.getAccountNumber().trim() : "none";
        
        return date + "|" + amount + "|" + desc + "|" + acc;
    }

    private void updateIfAvailable(Transaction existing, Transaction incoming) {
        // Protect manual edits: Only update if existing is null/empty but incoming has data
        if ((existing.getCategory() == null || existing.getCategory().isEmpty()) && incoming.getCategory() != null) {
            existing.setCategory(incoming.getCategory());
        }
        if (incoming.getOriginalDate() != null) existing.setOriginalDate(incoming.getOriginalDate());
        if (incoming.getInstitutionName() != null) existing.setInstitutionName(incoming.getInstitutionName());
        if (incoming.getAccountName() != null) existing.setAccountName(incoming.getAccountName());
        if (incoming.getAccountType() != null) existing.setAccountType(incoming.getAccountType());
        if (incoming.getNote() != null && (existing.getNote() == null || existing.getNote().isEmpty())) {
            existing.setNote(incoming.getNote());
        }
        if (incoming.getIgnoredFrom() != null) existing.setIgnoredFrom(incoming.getIgnoredFrom());
        if (incoming.getTaxDeductible() != null) existing.setTaxDeductible(incoming.getTaxDeductible());
        if (incoming.getTransactionTags() != null) existing.setTransactionTags(incoming.getTransactionTags());
    }
    
    @PutMapping("/transactions/{id}")
    public ResponseEntity<Transaction> updateTransaction(@PathVariable Long id, @RequestBody Transaction details) {
        return transactionRepository.findById(id).map(t -> {
            t.setDate(details.getDate());
            t.setOriginalDate(details.getOriginalDate());
            t.setAccountType(details.getAccountType());
            t.setAccountName(details.getAccountName());
            t.setAccountNumber(details.getAccountNumber());
            t.setInstitutionName(details.getInstitutionName());
            t.setName(details.getName());
            t.setCustomName(details.getCustomName());
            t.setAmount(details.getAmount());
            t.setDescription(details.getDescription());
            t.setCategory(details.getCategory());
            t.setNote(details.getNote());
            t.setIgnoredFrom(details.getIgnoredFrom());
            t.setTaxDeductible(details.getTaxDeductible());
            t.setTransactionTags(details.getTransactionTags());
            t.setType(details.getType());
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
            a.setInstitutionName(details.getInstitutionName());
            a.setAccountNumber(details.getAccountNumber());
            a.setNote(details.getNote());
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
            l.setInstitutionName(details.getInstitutionName());
            l.setAccountNumber(details.getAccountNumber());
            l.setNote(details.getNote());
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

        BigDecimal totalIncome = incomes.stream().map(Transaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalExpense = expenses.stream().map(Transaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalAssets = assets.stream().map(Asset::getCurrentValue).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalLiabilities = liabilities.stream().map(Liability::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal netWorth = totalAssets.subtract(totalLiabilities);
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

    @GetMapping("/reports/category-spending")
    public Map<String, BigDecimal> getCategorySpending(
        @RequestAttribute("userId") Long userId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        List<Transaction> expenses;
        if (startDate != null && endDate != null) {
            expenses = transactionRepository.findByUserIdAndDateBetween(userId, startDate, endDate).stream()
                    .filter(t -> t.getType() == TransactionType.EXPENSE)
                    .collect(Collectors.toList());
        } else {
            expenses = transactionRepository.findByTypeAndUserId(TransactionType.EXPENSE, userId);
        }

        return expenses.stream()
                .filter(t -> t.getCategory() != null)
                .collect(Collectors.groupingBy(
                        Transaction::getCategory,
                        Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)
                ));
    }

    @GetMapping("/reports/monthly-trend")
    public Map<String, Map<String, BigDecimal>> getMonthlyTrend(@RequestAttribute("userId") Long userId) {
        List<Transaction> transactions = transactionRepository.findByUserId(userId);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");

        // TreeMap keeps the months sorted
        Map<String, Map<String, BigDecimal>> trend = new TreeMap<>();

        for (Transaction t : transactions) {
            String month = t.getDate().format(formatter);
            trend.putIfAbsent(month, new HashMap<>());
            Map<String, BigDecimal> monthData = trend.get(month);
            
            String type = t.getType().name();
            monthData.put(type, monthData.getOrDefault(type, BigDecimal.ZERO).add(t.getAmount()));
        }

        return trend;
    }

    @GetMapping("/reports/frequent-merchants")
    public List<Map<String, Object>> getFrequentMerchants(
        @RequestAttribute("userId") Long userId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        List<Transaction> expenses;
        if (startDate != null && endDate != null) {
            expenses = transactionRepository.findByUserIdAndDateBetween(userId, startDate, endDate).stream()
                    .filter(t -> t.getType() == TransactionType.EXPENSE)
                    .collect(Collectors.toList());
        } else {
            expenses = transactionRepository.findByTypeAndUserId(TransactionType.EXPENSE, userId);
        }
        
        Map<String, List<Transaction>> groupedByMerchant = expenses.stream()
                .filter(t -> t.getDescription() != null)
                .collect(Collectors.groupingBy(Transaction::getDescription));

        return groupedByMerchant.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> map = new HashMap<>();
                    String name = entry.getKey();
                    List<Transaction> ts = entry.getValue();
                    BigDecimal total = ts.stream().map(Transaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal avg = total.divide(BigDecimal.valueOf(ts.size()), 2, RoundingMode.HALF_UP);
                    
                    map.put("name", name);
                    map.put("count", ts.size());
                    map.put("total", total);
                    map.put("average", avg);
                    return map;
                })
                .sorted((a, b) -> Integer.compare((int) b.get("count"), (int) a.get("count")))
                .limit(5)
                .collect(Collectors.toList());
    }

    @GetMapping("/reports/largest-purchases")
    public List<Transaction> getLargestPurchases(
        @RequestAttribute("userId") Long userId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        List<Transaction> expenses;
        if (startDate != null && endDate != null) {
            expenses = transactionRepository.findByUserIdAndDateBetween(userId, startDate, endDate).stream()
                    .filter(t -> t.getType() == TransactionType.EXPENSE)
                    .collect(Collectors.toList());
        } else {
            expenses = transactionRepository.findByTypeAndUserId(TransactionType.EXPENSE, userId);
        }

        return expenses.stream()
                .sorted((a, b) -> b.getAmount().compareTo(a.getAmount()))
                .limit(5)
                .collect(Collectors.toList());
    }

    @GetMapping("/reports/spending-comparison")
    public Map<String, Object> getSpendingComparison(
        @RequestAttribute("userId") Long userId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate) {

        LocalDate targetBaseDate = (startDate != null) ? startDate : LocalDate.now();
        YearMonth currentMonth = YearMonth.from(targetBaseDate);
        YearMonth previousMonth = currentMonth.minusMonths(1);

        List<Transaction> transactions = transactionRepository.findByUserId(userId);

        BigDecimal currentIncome = sumByMonthAndType(transactions, currentMonth, TransactionType.INCOME);
        BigDecimal currentExpense = sumByMonthAndType(transactions, currentMonth, TransactionType.EXPENSE);
        BigDecimal currentBills = sumBillsByMonth(transactions, currentMonth);

        BigDecimal previousIncome = sumByMonthAndType(transactions, previousMonth, TransactionType.INCOME);
        BigDecimal previousExpense = sumByMonthAndType(transactions, previousMonth, TransactionType.EXPENSE);
        BigDecimal previousBills = sumBillsByMonth(transactions, previousMonth);

        Map<String, Object> comparison = new HashMap<>();
        comparison.put("currentIncome", currentIncome);
        comparison.put("currentSpending", currentExpense);
        comparison.put("currentBills", currentBills);
        
        comparison.put("incomeChange", calculateChange(currentIncome, previousIncome));
        comparison.put("spendingChange", calculateChange(currentExpense, previousExpense));
        comparison.put("billsChange", calculateChange(currentBills, previousBills));

        return comparison;
    }

    private BigDecimal sumByMonthAndType(List<Transaction> transactions, YearMonth ym, TransactionType type) {
        return transactions.stream()
                .filter(t -> YearMonth.from(t.getDate()).equals(ym) && t.getType() == type)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumBillsByMonth(List<Transaction> transactions, YearMonth ym) {
        return transactions.stream()
                .filter(t -> YearMonth.from(t.getDate()).equals(ym) && t.getType() == TransactionType.EXPENSE)
                .filter(t -> {
                    String cat = (t.getCategory() != null) ? t.getCategory().toLowerCase() : "";
                    return cat.contains("bill") || cat.contains("utility") || cat.contains("mortgage") || cat.contains("rent") || cat.contains("loan");
                })
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateChange(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return current.subtract(previous).divide(previous, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
    }
}
