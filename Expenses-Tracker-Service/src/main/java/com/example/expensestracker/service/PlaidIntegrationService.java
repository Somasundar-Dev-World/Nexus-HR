package com.example.expensestracker.service;

import com.example.expensestracker.model.Tracker;
import com.example.expensestracker.model.TrackerEntry;
import com.example.expensestracker.model.TrackerIntegration;
import com.example.expensestracker.model.User;
import com.example.expensestracker.repository.TrackerEntryRepository;
import com.example.expensestracker.repository.TrackerIntegrationRepository;
import com.example.expensestracker.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class PlaidIntegrationService {

    private static final String PLAID_ENV_URL = "https://sandbox.plaid.com"; // Use sandbox for generic testing

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TrackerIntegrationRepository integrationRepository;
    
    @Autowired
    private TrackerEntryRepository entryRepository;

    public String createLinkToken(User user) throws Exception {
        if (user.getPlaidClientId() == null || user.getPlaidSecret() == null) {
            throw new RuntimeException("Plaid API keys are missing in your User Settings.");
        }

        String url = PLAID_ENV_URL + "/link/token/create";

        Map<String, Object> request = new HashMap<>();
        request.put("client_id", user.getPlaidClientId());
        request.put("secret", user.getPlaidSecret());
        request.put("client_name", "Omni Tracker App");
        request.put("language", "en");
        request.put("country_codes", Collections.singletonList("US"));
        
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("client_user_id", user.getId().toString());
        request.put("user", userMap);
        
        request.put("products", Collections.singletonList("transactions"));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
        JsonNode root = objectMapper.readTree(response.getBody());
        return root.path("link_token").asText();
    }

    public TrackerIntegration exchangePublicToken(String publicToken, String institutionName, Tracker tracker, User user) throws Exception {
        String url = PLAID_ENV_URL + "/item/public_token/exchange";

        Map<String, Object> request = new HashMap<>();
        request.put("client_id", user.getPlaidClientId());
        request.put("secret", user.getPlaidSecret());
        request.put("public_token", publicToken);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
        JsonNode root = objectMapper.readTree(response.getBody());
        
        String accessToken = root.path("access_token").asText();
        String itemId = root.path("item_id").asText();

        // Create or Update Integration
        Optional<TrackerIntegration> existing = integrationRepository.findByTrackerIdAndProvider(tracker.getId(), "PLAID");
        TrackerIntegration integration = existing.orElseGet(TrackerIntegration::new);
        
        integration.setTrackerId(tracker.getId());
        integration.setProvider("PLAID");
        integration.setInstitutionName(institutionName);
        integration.setAccessToken(accessToken);
        integration.setItemId(itemId);
        // By default, map is empty. The user will populate it next.
        if (integration.getFieldMapping() == null) {
            integration.setFieldMapping(new HashMap<>());
        }
        
        return integrationRepository.save(integration);
    }
    
    public Map<String, Object> setMapping(Long trackerId, Map<String, String> mapping) {
        TrackerIntegration integration = integrationRepository.findByTrackerIdAndProvider(trackerId, "PLAID")
            .orElseThrow(() -> new RuntimeException("No Plaid integration found for this tracker."));
        integration.setFieldMapping(mapping);
        integrationRepository.save(integration);
        return Collections.singletonMap("status", "success");
    }

    public int syncTransactions(Tracker tracker, User user) throws Exception {
        TrackerIntegration integration = integrationRepository.findByTrackerIdAndProvider(tracker.getId(), "PLAID")
                .orElseThrow(() -> new RuntimeException("Plaid is not connected to this tracker"));

        String url = PLAID_ENV_URL + "/transactions/sync";

        Map<String, Object> request = new HashMap<>();
        request.put("client_id", user.getPlaidClientId());
        request.put("secret", user.getPlaidSecret());
        request.put("access_token", integration.getAccessToken());
        
        if (integration.getSyncCursor() != null && !integration.getSyncCursor().trim().isEmpty()) {
            request.put("cursor", integration.getSyncCursor());
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

        int addedCount = 0;
        boolean hasMore = true;

        while (hasMore) {
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());

            JsonNode added = root.path("added");
            if (added.isArray() && added.size() > 0) {
                for (JsonNode tx : added) {
                    // Apply mappings
                    Map<String, Object> fieldValues = new HashMap<>();
                    Map<String, String> mapping = integration.getFieldMapping();
                    
                    if (mapping != null) {
                        for (Map.Entry<String, String> entry : mapping.entrySet()) {
                            String plaidField = entry.getKey(); // e.g. "amount", "name", "date"
                            String localField = entry.getValue();
                            
                            JsonNode val = tx.path(plaidField);
                            if (!val.isMissingNode() && !val.isNull()) {
                                if (val.isNumber()) {
                                    fieldValues.put(localField, val.asDouble());
                                } else if (val.isBoolean()) {
                                    fieldValues.put(localField, val.asBoolean());
                                } else {
                                    fieldValues.put(localField, val.asText());
                                }
                            }
                        }
                    }

                    if (!fieldValues.isEmpty()) {
                        TrackerEntry newEntry = new TrackerEntry();
                        newEntry.setTrackerId(tracker.getId());
                        newEntry.setUserId(user.getId());
                        newEntry.setFieldValues(fieldValues);
                        entryRepository.save(newEntry);
                        addedCount++;
                    }
                }
            }

            JsonNode nextCursor = root.path("next_cursor");
            hasMore = root.path("has_more").asBoolean();
            
            if (!nextCursor.isMissingNode() && !nextCursor.isNull()) {
                integration.setSyncCursor(nextCursor.asText());
                request.put("cursor", nextCursor.asText());
            } else {
                hasMore = false;
            }
        }

        integrationRepository.save(integration);
        return addedCount;
    }
}
