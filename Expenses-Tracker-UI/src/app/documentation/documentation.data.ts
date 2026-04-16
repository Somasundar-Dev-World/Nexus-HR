export const DOCUMENTATION_DATA: { [key: string]: string } = {
  'deep-research': `
# Deep Research Intelligence Hub

The **Deep Research** module is the pinnacle cognitive engine of the OmniTracker platform. Unlike standard querying systems, it utilizes a Retrieval-Augmented Generation (RAG) architecture powered by Google's Gemini 2.0 Flash LLM, allowing users to converse naturally with their structured numeric and categorical data.

## 1. System Architecture & RAG Pipeline

Deep Research doesn't just pass strings to an AI; it intelligently serializes SQL database states into highly structured context windows.

\`\`\`mermaid
sequenceDiagram
    participant User
    participant Angular UI
    participant OmniTrackerController
    participant DeepInsightCache
    participant AiInsightService
    participant Gemini Prompt Builder
    participant Google Gemini 2.0 API
    
    User->>Angular UI: Submits natural language question
    Angular UI->>OmniTrackerController: POST /api/omni/apps/{appId}/chat (message + history)
    OmniTrackerController->>AiInsightService: chatWithApp(appId, userId, payload)
    AiInsightService->>DeepInsightCache: Check semantic cache for recent identical schema hashes
    AiInsightService->>Gemini Prompt Builder: Aggregate Tracker schemas + Entry JSON
    Gemini Prompt Builder-->>Gemini Prompt Builder: Apply Strict JSON Directives
    Gemini Prompt Builder->>Google Gemini 2.0 API: Execute Model inference
    Google Gemini 2.0 API-->>AiInsightService: Raw generation (Markdown/JSON mixed)
    AiInsightService->>AiInsightService: Regex Fallback & Clean JSON Array
    AiInsightService-->>Angular UI: Structured <DeepInsightReport> entity
    Angular UI-->>User: Renders Chat bubble + charts
\`\`\`

## 2. Context Hydration Strategy

To prevent the LLM from hallucinating answers based on its training data, the backend (Spring Boot) must *hydrate* the prompt with the exact deterministic state of the user's data.

### Schema Injection
When a user targets an App (e.g., "Personal Wealth"), the system gathers all child \`Tracker\` definitions. A \`Tracker\` contains dynamic \`fieldDefinitions\`, which defines the exact datatypes (Currency, Text, Category). The prompt explicitly defines the boundary of these schemas.

### Entry Limiters
Passing thousands of rows of \`TrackerEntry\` objects into the LLM context is expensive and degrades generation quality. Our engine limits the context to the most recent 100 entries per tracker or pre-aggregates them if the volume is too high, maintaining statistical accuracy without breaching token limits.

## 3. Strict Model Directives

The **Gemini Prompt Builder** constructs the system prompt utilizing specific guardrails:
1. **Persona:** "You are an elite, highly analytical forensic accountant and data scientist."
2. **Formatting:** "You MUST output standard JSON only. Outputting conversational text outside the JSON boundaries will cause fatal system errors."
3. **Data Safety:** "If you do not see data corresponding to the user's question, state 'Insufficient Data'."

## 4. Frontend Resilience: The "Sanity Shield"

AI can occasionally generate extreme anomalies (e.g., hallucinatory integers representing "dates" instead of values). The Angular \`OmniDashboardComponent\` implements an aggressive **Sanity Shield**.

> [!CAUTION]
> **Sanity Shield Protocol**  
> Before rendering any AI-generated ApexChart, the frontend loop iterates through the generated \`labels\` and \`series\` arrays. Any numeric data point exceeding \`$100,000,000,000\` (100 Billion) or containing \`null\` labels is instantly zeroed out or stripped to prevent the glassmorphic rendering canvas from crashing.
  `,

  'track': `
# OmniTracker Core Architecture

The OmniTracker system forms the beating heart of Nexus-HR's "Personal OS". It is designed to capture highly dynamic, heterogeneous data types within a strict relational database structure, utilizing the **Entity-Attribute-Value (EAV)** paradigm.

## 1. Relational EAV Database Schema

OmniTracker solves the problem of "dynamic forms" by storing metadata definitions in a schema table and the actual payload inside PostgreSQL JSONB / Map columns.

\`\`\`mermaid
erDiagram
    User ||--o{ TrackerApp : owns
    TrackerApp ||--o{ Tracker : contains
    TrackerApp {
        Long id
        String name
        String icon
    }
    Tracker ||--o{ TrackerEntry : logs
    Tracker {
        Long id
        String name
        String type
        Map fieldDefinitions "Defines schema, e.g., {amount: 'number', category: 'string'}"
    }
    TrackerEntry {
        Long id
        LocalDateTime date
        Map fieldValues "Stores payload, e.g., {amount: 200.5, category: 'Food'}"
    }
    Tracker ||--o| TrackerIntegration : optionally_has
\`\`\`

### Data Marshalling
The JPA models utilize a specialized \`JpaMapConverter\` class implementing \`AttributeConverter<Map<String, Object>, String>\`. This seamlessly bridges Java memory states with the underlying database text/JSON capacities.

## 2. Frontend State Machine & Rendering Loop

The \`OmniDashboardComponent\` spans over 1,500 lines of highly optimized TypeScript code. It eschews standard Angular deep-linking (which would require constant reload of states) in favor of a localized state-machine using the \`viewMode\` enumeration.

### The View Modes
1. \`APP_GRID\` - The highest hierarchical view showing macro categories.
2. \`TRACKER_VIEW\` - Focuses on a specific App's child schemas.
3. \`REPORT_VIEW\` - Triggers the rendering of AI-architected intelligence suites.

### DOM Race Condition Management
When switching views dynamically via \`*ngIf\`, DOM elements like the \`div#report-main-chart\` are momentarily destroyed and recreated. To prevent ApexCharts from failing to attach to non-existent DOM nodes, our engine implements a precise **micro-task delay** (50ms - 150ms) guaranteeing the Angular Change Detection cycle finishes painting the DOM before the virtualization engine attaches.
  `,

  'reports': `
# Intelligent Reports Module

The Intelligence Reports system dynamically translates abstract, highly-complex user queries into stunning visual metrics using ApexCharts and deep server-side data transformation.

## 1. AI-Driven Report Architecture

Instead of hard-coding SQL views, reports are generated dynamically by asking the AI to "Design" a query. The \`AiReport\` entity stores an abstract \`querySpec\`, which the backend parser translates to live aggregation.

### The Query Specification JSON
\`\`\`json
{
  "name": "Financial Burn Rate",
  "visualType": "BAR",
  "querySpec": {
    "trackers": ["Robinhood Account", "Chase Checking"],
    "aggregate": { "type": "SUM", "field": "Amount" },
    "groupBy": "Date(MONTH)"
  },
  "config": {
    "xAxis": "Fiscal Month",
    "yAxis": "Total Expenditure"
  }
}
\`\`\`

## 2. Server-Side Aggregation Engine

When the \`executeReport()\` endpoint is called via the \`OmniTrackerController\`, the backend executes the following algorithmic steps:

1. **Resolution:** Identifies exactly which Trackers map to the target strings (using a customized fuzzy-logic "Sanitized Contains" matching to defeat slight AI hallucination like "Robin Hood" vs "Robinhood").
2. **Collection:** Queries the \`TrackerEntryRepository\` for all entries ordered temporally.
3. **Folding:** Loops through the raw data structure and executes algorithmic \`GROUP BY\` logic internally over the \`Map<String, Object> fieldValues\`.
4. **Formatting:** Packs the finalized matrices into a robust \`labels\` and \`series\` payload that precisely marries up to the ApexCharts API signature.

> [!important]
> **Defensive Empty State Check:** 
> If the query aggregation yields 0 rows (e.g. an improperly mapped field name), the API responds with an empty \`.labels\` array. The frontend observes this and gracefully morphs from the Rendering Canvas into a bespoke \`detail-empty\` visual state ("Desert 🏜️").
  `,

  'plaid': `
# Plaid Financial Integrations Network

The OmniTracker architecture connects out to the Plaid API to securely pull real-world banking and credit data into the application without storing user banking credentials.

## 1. Authentication Handshake (OAuth)

The Plaid flow relies on a distinct initialization and token exchange handshake:

\`\`\`mermaid
sequenceDiagram
    participant User
    participant Angular Client (Plaid SDK)
    participant Java Backend
    participant Plaid Network
    
    User->>Angular Client: Clicks "Connect Bank"
    Angular Client->>Java Backend: Request Link Token
    Java Backend->>Plaid Network: POST /link/token/create
    Plaid Network-->>Java Backend: Returns Link Token
    Java Backend-->>Angular Client: Returns Link Token
    Angular Client->>Plaid Network: Initialize IFRAME Handshake
    User->>Plaid Network: Authenticates inside secure IFRAME
    Plaid Network-->>Angular Client: Provides Public Token
    Angular Client->>Java Backend: Exchange Public Token
    Java Backend->>Plaid Network: POST /item/public_token/exchange
    Plaid Network-->>Java Backend: Provides Persistent Access Token
    Java Backend->>Java Backend: Persist Access Token defensively to DB Schema
\`\`\`

## 2. Syncing Engine

Once securely linked, the \`PlaidIntegrationService\` implements a polling / cron paradigm to fetch delta-sync transaction events. Every Plaid transaction is forcefully marshalled into the abstract \`TrackerEntry\` system by mapping generic Plaid fields (\`name\`, \`amount\`, \`category\`) against the customized \`Tracker\` definitions the user established in the UI.
  `,

  'profile': `
# Security & Identity Subsystem

Nexus-HR provides strict identity management using custom Spring Security context implementations based off standard JWT architecture.

## 1. Authentication Control Plane

User login actions bounce against the \`AuthController\`. Upon credential validation (bcrypt verified), a JSON Web Token (JWT) is minted with a standard 10-hour lifespan.

## 2. Multi-Tenant Interception

Data exfiltration risk between separate tenant instances is eliminated through a rigorous Request Interception paradigm. When an HTTP Request strikes our Gateway:

1. **JwtAuthenticationFilter:** The request header \`Authorization: Bearer <token>\` is parsed via asymmetric key validation.
2. **Claim Detachment:** The \`userId\` claim is detached and mapped as a strict \`HttpServletRequest\` attribute.
3. **Controller Guardrails:** ALL secure controllers use \`@RequestAttribute("userId") Long userId\` directly injected into the method scope. At no point is the \`userId\` payload respected if sent from the Angular client-side JSON body, rendering arbitrary ID spoofing technically impossible.

\`\`\`mermaid
flowchart TD
    Req[Incoming HTTP Request] --> AuthFilter{JWT Filter}
    AuthFilter -- Invalid/Missing --> 401[401 Unauthorized]
    AuthFilter -- Valid --> TokenExt[Extract Claims]
    TokenExt --> RequestAttr[Set RequestAttribute 'userId']
    RequestAttr --> Controller[Controller Layer]
    Controller --> ServiceLayer[Service Methods]
    ServiceLayer --> DbCall[DB repository.findByUserId(userId)]
\`\`\`

> [!WARNING]
> Developers attempting to write new Repository queries in the backend must ALWAYS implement \`findByUserIdAndX\` interfaces over simplistic \`findAll()\` structures.
  `,

  'default': `
# Nexus-HR / OmniTracker Technical Foundation

Welcome to the comprehensive internal Engineering Handbook. This platform represents a monolithic Spring Boot Java Application tied to an extreme-performance Angular 17.3 UI layer.

## Architecture Ecosystem

\`\`\`mermaid
graph TD
    UI[Angular 17 SPA]
    UI -- "RESTful HTTPS (+ JWT Header)" --> API[Spring Boot 3 API Hub]
    
    API -- "Hibernate ORM / JDBC" --> DB[(PostgreSQL relational DB)]
    API -- "Gemini AI Endpoints" --> ML[Google Cloud ML Cortex]
    API -- "OAuth2 Financials" --> Plaid[Plaid Network API]
    
    subgraph Core Features
    UI---Omni[OmniTracker Engine]
    UI---Legacy[Finance Legacy Dashboard]
    UI---Auth[Identity Service]
    Omni---ML
    end
\`\`\`

## How to sequence this documentation
Use the sidebar navigation to deeply review distinct aspects of our system engineering methodology. You will find specific details concerning how we built the AI interactions, rendering safeguards, and secure authentication pathways.
  `
};
