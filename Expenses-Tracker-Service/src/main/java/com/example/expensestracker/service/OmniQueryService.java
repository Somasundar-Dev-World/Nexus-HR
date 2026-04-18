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
        // Syntax: SELECT field1, field2 FROM TrackerName WHERE ... ORDER BY ... LIMIT ...
        Pattern pattern = Pattern.compile("(?i)SELECT (.*) FROM ([^\\s]*) (?:WHERE (.*))? (?:ORDER BY (.*))? (?:LIMIT (\\d+))?");
        Matcher matcher = pattern.matcher(query.trim());

        if (!matcher.find()) {
            throw new RuntimeException("Invalid OmniQuery syntax. Expected: SELECT <fields> FROM <tracker> [WHERE <condition>] [ORDER BY <field>] [LIMIT <n>]");
        }

        String fieldsPart = matcher.group(1).trim();
        String trackerName = matcher.group(2).trim();
        String wherePart = matcher.group(3);
        String orderByPart = matcher.group(4);
        String limitPart = matcher.group(5);

        // 1. Resolve Tracker
        Tracker tracker = findTrackerByName(trackerName, userId);
        if (tracker == null) {
            throw new RuntimeException("Tracker not found: " + trackerName);
        }

        // 2. Fetch Entries
        List<TrackerEntry> entries = trackerEntryRepository.findByTrackerIdOrderByDateAsc(tracker.getId());

        // 3. Filter (WHERE)
        List<TrackerEntry> filtered = entries;
        if (wherePart != null && !wherePart.isEmpty()) {
            filtered = applyFilters(entries, wherePart);
        }

        // 4. Sort (ORDER BY)
        if (orderByPart != null && !orderByPart.isEmpty()) {
            applySorting(filtered, orderByPart);
        }

        // 5. Limit
        if (limitPart != null) {
            int limit = Integer.parseInt(limitPart);
            filtered = filtered.stream().limit(limit).collect(Collectors.toList());
        }

        // 6. Project Fields (SELECT)
        return projectFields(filtered, fieldsPart, tracker);
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

    private List<TrackerEntry> applyFilters(List<TrackerEntry> entries, String whereClause) {
        // Simple filter: field = value, field > value, field < value
        // Pattern: ([a-zA-Z0-9_]+) (=|>|<|!=|LIKE) (.*)
        Pattern p = Pattern.compile("(?i)([a-zA-Z0-9_\\s]+) (=|>|<|!=|LIKE) (.*)");
        Matcher m = p.matcher(whereClause.trim());

        if (!m.find()) return entries;

        String field = m.group(1).trim();
        String op = m.group(2).trim();
        String val = m.group(3).trim().replace("\"", "").replace("'", "");

        return entries.stream().filter(e -> {
            Object actual = e.getFieldValues().get(field);
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

    private void applySorting(List<TrackerEntry> entries, String orderBy) {
        String[] parts = orderBy.trim().split("\\s+");
        String field = parts[0];
        boolean desc = parts.length > 1 && parts[1].equalsIgnoreCase("DESC");

        entries.sort((a, b) -> {
            Object valA = a.getFieldValues().get(field);
            Object valB = b.getFieldValues().get(field);
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

    private List<Map<String, Object>> projectFields(List<TrackerEntry> entries, String fieldsPart, Tracker tracker) {
        boolean selectAll = fieldsPart.equals("*");
        String[] selectedFields = selectAll ? new String[0] : fieldsPart.split(",");

        return entries.stream().map(e -> {
            Map<String, Object> projected = new LinkedHashMap<>();
            projected.put("id", e.getId());
            projected.put("date", e.getDate());

            if (selectAll) {
                projected.putAll(e.getFieldValues());
            } else {
                for (String f : selectedFields) {
                    String cleanF = f.trim();
                    if (e.getFieldValues().containsKey(cleanF)) {
                        projected.put(cleanF, e.getFieldValues().get(cleanF));
                    }
                }
            }
            return projected;
        }).collect(Collectors.toList());
    }
}
