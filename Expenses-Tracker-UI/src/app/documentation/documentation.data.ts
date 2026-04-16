export const DOCUMENTATION_DATA: { [key: string]: string } = {
  'deep-research': `
# Deep Research Intelligence Hub

The Deep Research module acts as the core AI-driven analytical engine for the OmniTracker platform. It allows users to continuously synthesize, cross-reference, and chat with their structured data in a conversational format.

## System Architecture

The Deep Research paradigm relies on an advanced \`Document-Retrieval-Generation\` pipeline. When a user asks a question, several internal processes run to gather the context:

1. **Context Hydration:** The backend queries Postgres for all structured entries relating to the active TrackerApp.
2. **System Prompt Injection:** The \`AiInsightService\` injects schema details (field definitions, data types) directly into the core system prompt to enforce domain boundaries.
3. **LLM Invocation:** The hydrated payload is sent to Gemini (using the \`gemini-2.0-flash\` model).

\`\`\`mermaid
sequenceDiagram
    participant User
    participant AngularUI as Angular UI
    participant Backend as Spring Boot API
    participant AI as Gemini 2.0 Flash
    
    User->>AngularUI: Enters query
    AngularUI->>Backend: POST /api/omni/apps/{appId}/chat
    Backend->>Backend: Hydrate Tracker Schema & Entries
    Backend->>AI: Send prompt + structured data context
    AI-->>Backend: Return JSON Response
    Backend-->>AngularUI: Parsed Deep Insight Object
    AngularUI-->>User: Render Chat Message
\`\`\`

## Backend Implementation

In \`ReportIntelligenceService.java\`, the AI is tightly constrained to output exact JSON matching the \`TrackerEntry\` schemas. 

> [!important]
> The backend explicitly removes Markdown formatting from Gemini's response to prevent JSON parsing exceptions during the mapping phase.

## AI Algorithms & Tuning

We use a specific temperature \`0.2\` to minimize hallucinations while extracting structured fields. If parsing fails, a fallback regex engine dynamically cleans the response and attempts reconstruction.
  `,
  'track': `
# OmniTracker Master Dashboard

The OmniTracker dashboard acts as the unified "Personal OS", providing a premium hierarchical layout consisting of "Apps -> Trackers -> Entries".

## Data Hierarchy

The database represents this structure via three main entities, mapping down sequentially:

\`\`\`mermaid
erDiagram
    TrackerApp ||--o{ Tracker : "has many"
    TrackerApp {
        Long id
        String name
        String icon
    }
    Tracker ||--o{ TrackerEntry : "logs"
    Tracker {
        Long id
        String type
        JSON fieldDefinitions
    }
    TrackerEntry {
        Long id
        LocalDateTime date
        JSON fieldValues
    }
\`\`\`

## Frontend Implementation

The \`OmniDashboardComponent\` heavily relies on Angular's reactive \`*ngIf\` blocks to instantly switch between context views (e.g., \`APP_GRID\` vs \`TRACKER_DETAIL\`) without expensive full-page routing loads.

> [!tip]
> State is managed locally via strict view enums (\`APP_GRID\`, \`TRACKER_VIEW\`, \`REPORT_VIEW\`) to ensure seamless glassmorphic transitions.
  `,
  'profile': `
# Profile & Multi-Tenant Architecture

The OmniTracker system implements strong multi-tenancy at the row level. 

## Multi-Tenancy

Every API endpoint enforces a strict check via the \`userId\` claim extracted from the JWT token.
- Repositories use \`.findByAppIdAndUserId()\` instead of \`.findAll()\`.
- All writes are stamped with the authenticated user's ID within the Controller layer to prevent ID spoofing.

\`\`\`mermaid
flowchart TD
    Req[Incoming HTTP Request] --> AuthFilter{JWT Filter}
    AuthFilter -- Invalid --> 401[401 Unauthorized]
    AuthFilter -- Valid --> TokenExt[Extract Claims]
    TokenExt --> RequestAttr[Set RequestAttribute 'userId']
    RequestAttr --> Controller[Controller Layer]
\`\`\`
  `,
  'default': `
# Application Architecture

Welcome to the OmniTracker Technical Documentation. Choose a specific module to dive deeper into the technical implementation, architectural diagrams, and algorithms powering the platform.

\`\`\`mermaid
graph TD
    A[Angular 17 UI] --> B{Spring Boot Backend}
    B --> C[PostgreSQL Database]
    B --> D[Google Gemini Pro]
    B --> E[Plaid API]
\`\`\`
  `
};
