# Gemini 2.0 Integration & RAG Architecture

## Overview

The Nexus-HR **Deep Research** module leverages the Google Gemini 2.0 Flash model (`gemini-2.0-flash`) to provide conversational analytics over structured relational data. 

Because Large Language Models are stateless and lack access to proprietary financial or HR metrics, the platform implements a **Retrieval-Augmented Generation (RAG)** pipeline. This pipeline serializes native PostgreSQL Entity-Attribute-Value (EAV) records into deterministic JSON blocks, which are injected into the model's system prompt prior to inference.

---

## 1. Quickstart: Requesting Insights

The Angular frontend initiates insight generation by communicating with the `OmniTrackerController`. The user's query is merged with the current Application ID Context.

### Endpoint
\`POST /api/omni/apps/{appId}/chat\`

### Authorization
Requires a valid `Bearer JWT` spanning the active User Context.

### cURL Example
\`\`\`bash
curl -X POST https://api.nexus-hr.com/api/omni/apps/102/chat \\
  -H "Authorization: Bearer eyJhbGciOiJIUzI1..." \\
  -H "Content-Type: application/json" \\
  -d '{
    "message": "Calculate my total payroll expenses for the last quarter and highlight the highest cost category."
  }'
\`\`\`

### Response Object (DeepInsightReport)
The API strictly returns a structured Java Object mapped to the Angular UI schema.
\`\`\`json
{
  "summary": "Your total payroll expenses were $45,200.00. The highest category was Engineering.",
  "labels": ["Engineering", "Sales", "Support"],
  "series": [25000, 12000, 8200],
  "visualType": "PIE",
  "confidenceScore": 0.98
}
\`\`\`

---

## 2. API Reference & Model Configuration

Data is routed through the `AiInsightService` utilizing the native Google Generative Language API endpoints. 

### Core Inference Configuration

| Parameter | Value | Description |
| :--- | :--- | :--- |
| **Model ID** | `gemini-2.0-flash` | Selected for extreme speed and context-window pricing efficiency. |
| **Temperature** | `0.2` | Constrained heavily to prevent math hallucinations during JSON generation. |
| **Top P** | `0.80` | Limits token selection breadth, enforcing deterministic, analytical answers over creative writing. |
| **Top K** | `40` | Standard selection cutoff for vocabulary routing. |

---

## 3. RAG Context Hydration Algorithm

To inform Gemini about a user's data without exposing other tenants, the context window is rigorously partitioned.

### Step 3a: Postgres Retrieval
The system identifies the active `AppId` and the authenticated `UserId` and pulls the latest historical events from the `TrackerEntry` repository.

\`\`\`java
// Extracting Context from PosgreSQL inside AiInsightService.java
List<TrackerEntry> entries = trackerEntryRepository.findTop100ByTrackerIdOrderByDateDesc(trackerId);
String jsonDataContext = objectMapper.writeValueAsString(entries);
\`\`\`

### Step 3b: System Prompt Injection
The `AiInsightService` builds a strict, non-negotiable directive wrapper around the JSON data.

\`\`\`text
[SYSTEM INSTRUCTION]
You are an expert Data Scientist and Financial Analyst for Nexus-HR. 
You MUST answer the user's question based strictly on the JSON Array provided below. Do not generate fake numbers.
If the data cannot answer the question definitively, return "Insufficient Data".

Output your findings ONLY in standard JSON format matching the schema requested.
---
[DATA CONTEXT START]
[
  {"trackerId": 55, "date": "2026-04-10", "fieldValues": {"payroll_cost": 25000, "department": "Engineering"}},
  {"trackerId": 55, "date": "2026-04-11", "fieldValues": {"payroll_cost": 12000, "department": "Sales"}}
]
[DATA CONTEXT END]
\`\`\`

---

## 4. Safety Settings & Harm Categories

Because the Nexus-HR engine sits in front of potentially sensitive Employee and Expense data, we enforce high-level safety restrictions on the Gemini inference process via the API Header definitions.

\`\`\`json
"safetySettings": [
  { "category": "HARM_CATEGORY_HARASSMENT", "threshold": "BLOCK_MEDIUM_AND_ABOVE" },
  { "category": "HARM_CATEGORY_HATE_SPEECH", "threshold": "BLOCK_MEDIUM_AND_ABOVE" },
  { "category": "HARM_CATEGORY_SEXUALLY_EXPLICIT", "threshold": "BLOCK_LOW_AND_ABOVE" },
  { "category": "HARM_CATEGORY_DANGEROUS_CONTENT", "threshold": "BLOCK_MEDIUM_AND_ABOVE" }
]
\`\`\`

---

## 5. Resilience: Regex Fallbacks for Malformed JSON

Even with a top-tier LLM at `Temperature: 0.2`, the model may occasionally output Markdown backticks (e.g. \`\`\`json { ... } \`\`\`) surrounding the payload, which violently breaks standard `ObjectMapper.readValue()` processing in Java.

To combat this, `AiInsightService.java` implements a strict **Regex Fallback Cleaner** prior to deserialization:

\`\`\`java
// Extract pure JSON if the model hallucinates formatting code blocks
String cleanedResponse = rawResponse.trim();
if (cleanedResponse.startsWith("\`\`\`json")) {
    cleanedResponse = cleanedResponse.substring(7);
}
if (cleanedResponse.endsWith("\`\`\`")) {
    cleanedResponse = cleanedResponse.substring(0, cleanedResponse.length() - 3);
}

// Fallback Regex processing to sanitize string boundaries
cleanedResponse = cleanedResponse.replaceAll("^\\s*```json\\s*", "");
cleanedResponse = cleanedResponse.replaceAll("\\s*```\\s*$", "");
\`\`\`

If parsing still fails, an explicit `SystemException` is caught, and a safe, empty `DeepInsightReport` object is returned to the Angular frontend, preventing the UI `*ngIf` chains from fragmenting.
