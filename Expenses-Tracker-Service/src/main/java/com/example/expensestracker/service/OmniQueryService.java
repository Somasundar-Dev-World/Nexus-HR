package com.example.expensestracker.service;

import com.example.expensestracker.model.*;
import com.example.expensestracker.repository.*;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class OmniQueryService {

    private final TrackerRepository trackerRepository;
    private final TrackerEntryRepository trackerEntryRepository;

    public OmniQueryService(TrackerRepository trackerRepository, TrackerEntryRepository trackerEntryRepository) {
        this.trackerRepository = trackerRepository;
        this.trackerEntryRepository = trackerEntryRepository;
    }

    public List<Map<String, Object>> executeQuery(String query, Long userId) {
        String cleanQuery = query.trim().replaceAll("\\n", " ").replaceAll("\\s+", " ");
        // Syntax: SELECT <fields> FROM <trackerA> [JOIN <trackerB> ON <keyA> = <keyB>] [WHERE <condition>] [ORDER BY <field>] [LIMIT <n>]
        Pattern pattern = Pattern.compile("(?i)SELECT\\s+(.+?)\\s+FROM\\s+(\".+?\"|[^\\s]+)(?:\\s+JOIN\\s+(\".+?\"|[^\\s]+)\\s+ON\\s+(.+?)\\s*=\\s*(.+?))?(?:\\s+WHERE\\s+(.+?))?(?:\\s+ORDER\\s+BY\\s+(.+?))?(?:\\s+LIMIT\\s+(\\d+))?");
        Matcher matcher = pattern.matcher(cleanQuery);

        if (!matcher.find()) {
            throw new RuntimeException("Invalid OmniQuery syntax. Expected: SELECT <fields> FROM <tracker> [JOIN <trackerB> ON <keyA>=<keyB>] [WHERE <condition>] [ORDER BY <field>] [LIMIT <n>]");
        }

        String fieldsPart = matcher.group(1).trim();
        String trackerAName = matcher.group(2).trim();
        String trackerBName = matcher.group(3);
        String joinKeyA = matcher.group(4);
        String joinKeyB = matcher.group(5);
        String wherePart = matcher.group(6);
        String orderByPart = matcher.group(7);
        String limitPart = matcher.group(8);

        // 1. Resolve Primary Tracker
        Tracker trackerA = findTrackerByName(trackerAName, userId);
        if (trackerA == null) throw new RuntimeException("Primary Tracker not found: " + trackerAName);
        List<TrackerEntry> entriesA = trackerEntryRepository.findByTrackerIdOrderByDateAsc(trackerA.getId());

        List<Map<String, Object>> convergedResults = new ArrayList<>();

        if (trackerBName != null) {
            // 2. Perform JOIN Logic
            Tracker trackerB = findTrackerByName(trackerBName, userId);
            if (trackerB == null) throw new RuntimeException("Joined Tracker not found: " + trackerBName);
            List<TrackerEntry> entriesB = trackerEntryRepository.findByTrackerIdOrderByDateAsc(trackerB.getId());

            // Simple Hash Join
            for (TrackerEntry a : entriesA) {
                Object valA = a.getFieldValues().get(joinKeyA.trim());
                if (valA == null) continue;

                for (TrackerEntry b : entriesB) {
                    Object valB = b.getFieldValues().get(joinKeyB.trim());
                    if (valB != null && valA.toString().equalsIgnoreCase(valB.toString())) {
                        Map<String, Object> joined = new LinkedHashMap<>();
                        // Prefix fields to avoid collisions
                        a.getFieldValues().forEach((k, v) -> joined.put(trackerA.getName() + " " + k, v));
                        b.getFieldValues().forEach((k, v) -> joined.put(trackerB.getName() + " " + k, v));
                        joined.put("_dateA", a.getDate());
                        joined.put("_dateB", b.getDate());
                        convergedResults.add(joined);
                    }
                }
            }
        } else {
            // Standard Single Tracker Case
            for (TrackerEntry a : entriesA) {
                Map<String, Object> row = new LinkedHashMap<>(a.getFieldValues());
                row.put("id", a.getId());
                row.put("date", a.getDate());
                convergedResults.add(row);
            }
        }

        // 3. Filter (WHERE)
        if (wherePart != null && !wherePart.isEmpty()) {
            convergedResults = applyFiltersOnList(convergedResults, wherePart);
        }

        // 4. Sort (ORDER BY)
        if (orderByPart != null && !orderByPart.isEmpty()) {
            applySortingOnList(convergedResults, orderByPart);
        }

        // 5. Limit
        if (limitPart != null) {
            int limit = Integer.parseInt(limitPart);
            convergedResults = convergedResults.stream().limit(limit).collect(Collectors.toList());
        }

        // 6. Project Fields (SELECT)
        return projectFieldsFromList(convergedResults, fieldsPart);
    }

    private Tracker findTrackerByName(String name, Long userId) {
        // Strip quotes if any
        String cleanName = name.replace("\"", "").replace("'", "");
        List<Tracker> trackers = trackerRepository.findByUserId(userId);
        return trackers.stream()
                .filter(t -> t.getName().equalsIgnoreCase(cleanName))
                .findFirst()
                .orElse(null);
    }

    private List<Map<String, Object>> applyFiltersOnList(List<Map<String, Object>> results, String whereClause) {
        Pattern p = Pattern.compile("(?i)([a-zA-Z0-9_\\s]+) (=|>|<|!=|LIKE) (.*)");
        Matcher m = p.matcher(whereClause.trim());

        if (!m.find()) return results;

        String field = m.group(1).trim();
        String op = m.group(2).trim();
        String val = m.group(3).trim().replace("\"", "").replace("'", "");

        return results.stream().filter(row -> {
            Object actual = row.get(field);
            if (actual == null) return false;

            String sActual = actual.toString();
            switch (op.toUpperCase()) {
                case "=": return sActual.equalsIgnoreCase(val);
                case ">": try { return Double.parseDouble(sActual) > Double.parseDouble(val); } catch (Exception ex) { return false; }
                case "<": try { return Double.parseDouble(sActual) < Double.parseDouble(val); } catch (Exception ex) { return false; }
                case "!=": return !sActual.equalsIgnoreCase(val);
                case "LIKE": return sActual.toLowerCase().contains(val.toLowerCase());
                default: return false;
            }
        }).collect(Collectors.toList());
    }

    private void applySortingOnList(List<Map<String, Object>> results, String orderBy) {
        String[] parts = orderBy.trim().split("\\s+");
        String field = parts[0];
        boolean desc = parts.length > 1 && parts[1].equalsIgnoreCase("DESC");

        results.sort((a, b) -> {
            Object valA = a.get(field);
            Object valB = b.get(field);
            if (valA == null || valB == null) return 0;
            
            int res;
            try {
                res = Double.compare(Double.parseDouble(valA.toString()), Double.parseDouble(valB.toString()));
            } catch (Exception e) {
                res = valA.toString().compareToIgnoreCase(valB.toString());
            }
            return desc ? -res : res;
        });
    }

    private List<Map<String, Object>> projectFieldsFromList(List<Map<String, Object>> results, String fieldsPart) {
        if (fieldsPart.equals("*")) return results;
        
        String[] selectedFields = fieldsPart.split(",");
        return results.stream().map(row -> {
            Map<String, Object> projected = new LinkedHashMap<>();
            for (String f : selectedFields) {
                String cleanF = f.trim();
                if (row.containsKey(cleanF)) {
                    projected.put(cleanF, row.get(cleanF));
                }
            }
            return projected;
        }).collect(Collectors.toList());
    }
}
// Hotfix: Regex for quoted identifiers - cb00014

// Trigggering Deployment: 2026-04-18T03:09:00Z
