export const DOCUMENTATION_DATA: { [key: string]: string } = {
  'deep-research': `
# Masterclass: Artificial Intelligence & Retrieval-Augmented Generation (RAG)

Artificial Intelligence within modern applications typically isn't just a "chatbot plug-in." True platform intelligence requires bridging the gap between proprietary relational data and Large Language Models (LLMs) like Google Gemini.

## 1. The Hallucination Problem
LLMs are pre-trained on the public internet. If a user asks the AI "What was my highest expense last month?", the LLM inherently does not know the answer. If forced, it will "hallucinate" (invent) an answer to satisfy the prompt. 

To solve this, we use **Retrieval-Augmented Generation (RAG)**.

## 2. RAG Concept & Implementation

RAG fundamentally means: **Retrieve** the facts from your local database first, **Augment** your prompt with those facts, and then ask the AI to **Generate** an answer based *only* on what you provided.

### Step 1: Context Hydration (Spring Boot)
When a call hits the \`OmniTrackerController\`, we execute a query to pull the user's \`TrackerEntries\`.
\`\`\`java
// Extracting Context from PosgreSQL
List<TrackerEntry> recentEntries = trackerEntryRepository.findTop100ByTrackerIdOrderByDateDesc(trackerId);
String jsonDataContext = objectMapper.writeValueAsString(recentEntries);
\`\`\`

### Step 2: Prompt Engineering
We inject this JSON block into a massive "System Prompt". The system prompt acts as strict instructions.
\`\`\`text
SYSTEM PROMPT:
You are an expert Data Scientist. You MUST answer the user's question based ONLY on the following JSON data.
Do NOT invent data. If the answer is not in the JSON, say "Insufficient Data".
Respond ONLY in the following JSON format:
{
  "type": "METRIC",
  "title": "Expense Output",
  "value": "$500"
}
---
[RAW DATA START]
{ jsonDataContext }
[RAW DATA END]
\`\`\`

### Step 3: Inference Calling
We pass this entire payload via REST to Google's Gemini API. 
\`\`\`mermaid
sequenceDiagram
    participant App as Spring Boot API
    participant AI as Gemini 2.0 Flash
    
    App->>App: Serialize User Data to JSON String
    App->>AI: POST /v1beta/models/gemini-2.0-flash:generateContent
    Note over App,AI: Prompt contains strict formatting rules + Data Context
    AI-->>App: Returns JSON payload
    App->>App: Match regex / sanitize layout
\`\`\`

## 3. Strict Model Directives & Temperature Tuning

When calling Gemini, we pass a configuration object that alters the model's behavior:
- **Temperature (\`0.2\`):** Temperature controls "creativity". A temperature of \`1.0\` is highly creative (good for writing poems). A temperature of \`0.2\` is highly deterministic (good for parsing CSVs and financial data). We use a highly deterministic setting to prevent math errors.
- **Top-K & Top-P:** These settings control the probability distributions of words chosen. We lock these tightly so the LLM behaves more like a "computational engine" rather than a "conversational bot".
  `,

  'track': `
# Masterclass: Angular 17 Architecture & Standalone Components

This system is built using Angular 17, representing a massive shift in how Angular applications are structured compared to earlier versions (v14 and below).

## 1. Standalone Components Explained
Historically, Angular required \`@NgModule\` to declare components and manage dependencies. Angular 17 introduces **Standalone Components** as the default.

\`\`\`typescript
@Component({
  selector: 'app-omni-dashboard',
  standalone: true, // No NgModule Required!
  imports: [CommonModule, FormsModule, RouterLink], // Import specific dependencies directly
  templateUrl: './omni-dashboard.component.html'
})
export class OmniDashboardComponent { ... }
\`\`\`
By setting \`standalone: true\`, the component imports exactly what it needs (like \`CommonModule\` for \`*ngIf\` or \`FormsModule\` for \`[(ngModel)]\`). This enables aggressive tree-shaking by Webpack, severely reducing the initial JavaScript bundle size.

## 2. Advanced State Management (Local)

The \`OmniDashboardComponent\` is highly dynamic. Instead of using complex deep-routing (which forces total page re-renders), it acts as a **Local State Machine**.

\`\`\`typescript
// The State Machine
type ViewMode = 'APP_GRID' | 'TRACKER_GRID' | 'TRACKER_DETAIL' | 'DEEP_RESEARCH';
viewMode: ViewMode = 'APP_GRID';

// Navigation functions don't hit the Router, they just flip the state
openDeepResearch() {
  this.viewMode = 'DEEP_RESEARCH';
}
\`\`\`

In the HTML template, the physical DOM is rapidly swapped using Structural Directives:
\`\`\`html
<!-- Structural Directives control DOM instantiation -->
<div *ngIf="viewMode === 'APP_GRID'">
  <app-grid-view></app-grid-view>
</div>

<div *ngIf="viewMode === 'DEEP_RESEARCH'">
  <deep-research-view></deep-research-view>
</div>
\`\`\`

## 3. Dealing with DOM Race Conditions

A common danger in Angular is the **DOM Race Condition**. Because \`*ngIf\` physically adds/removes elements from the DOM, querying those elements immediately after changing a state variable will fail.

\`\`\`typescript
// THE DANGER:
this.viewMode = 'REPORT_VIEW';
document.querySelector('#my-chart'); // DANGER! Returns null, Angular hasn't updated the DOM yet!

// THE SOLUTION: (Micro-task queues)
this.viewMode = 'REPORT_VIEW';
setTimeout(() => {
  // Executes in the next tick of the Event Loop, AFTER Angular's Change Detection
  document.querySelector('#my-chart'); // Success!
}, 50);
\`\`\`
We use this \`setTimeout\` trick whenever initializing external libraries (like ApexCharts) that demand a physical HTML element to attach to.
  `,

  'backend': `
# Masterclass: Spring Boot 3 & Controller Architecture

Spring Boot 3 (using Java 17+) is an enterprise-grade framework that utilizes **Inversion of Control (IoC)** and **Dependency Injection (DI)**.

## 1. Dependency Injection Doctrine

Instead of manually instantiating classes (\`new TrackerRepository()\`), Spring acts as an \`ApplicationContext\` (a giant invisible factory).

\`\`\`java
@Service // Informs Spring this is a managed bean
public class ReportIntelligenceService {

    // Final variables prevent accidental mutation
    private final TrackerRepository trackerRepository;

    // CONSTRUCTOR INJECTION
    // Spring automatically provides the correct instance at runtime
    public ReportIntelligenceService(TrackerRepository trackerRepository) {
        this.trackerRepository = trackerRepository;
    }
}
\`\`\`
**Why Constructor Injection?** It allows testing frameworks (like JUnit/Mockito) to easily pass Mock objects into the service without needing Reflection API hacks, and ensures the service cannot exist in an invalid state.

## 2. The Request Lifecycle (REST)

When you make a network call from Angular, it follows a strict pipeline:

\`\`\`mermaid
flowchart LR
    Network(HTTP Request) --> DispatcherServlet{Tomcat / Dispatcher}
    DispatcherServlet --> Filter(Security JWT Filter)
    Filter --> Controller(@RestController)
    Controller --> Service(@Service)
    Service --> Repos(@Repository)
    Repos --> DB[(PostgreSQL)]
\`\`\`

### The Controller Layer
Controllers are purely routing mechanisms. **They should never contain business logic.**
\`\`\`java
@RestController
@RequestMapping("/api/omni/apps")
public class OmniTrackerController {
    
    // GOOD: Controller just maps the route and delegates to the Service
    @GetMapping("/{appId}")
    public ResponseEntity<OmniApp> getApp(@PathVariable Long appId) {
        return ResponseEntity.ok(appService.getAppDetails(appId));
    }
}
\`\`\`
If a Controller contains \`if/else\` logic calculating financial sums, it is an architectural violation known as a "Fat Controller." All math, filtering, and aggregation occurs in the **Service Layer**.
  `,

  'database': `
# Masterclass: PostgreSQL & The EAV Pattern via JSONB

Most apps use a strict Relational Schema (e.g. Table: \`Employees\`, Columns: \`Name, Age, Department\`). This is highly performant but inflexible.

Nexus-HR provides an "OmniTracker"—a feature where Users define their *own* forms. A user could track "Calories", "Stock Prices", or "Mileage". It is impossible to dynamically create a new PostgreSQL table every time a user makes a form.

To solve this we use the **Entity-Attribute-Value (EAV)** pattern via specialized PostgreSQL constructs.

## 1. The Concept

Instead of a table with columns for every possible datapoint, we have a generic \`TrackerEntry\` table.

| ID | Tracker_ID | Log_Date | Payload (JSON) |
|---|---|---|---|
| 1 | 55 (Fitness) | 2026-04-10 | \`{"pushups": 50, "miles": 2.5}\` |
| 2 | 12 (Finance) | 2026-04-11 | \`{"cost": 49.99, "merchant": "Amazon"}\` |

## 2. Implementation in JPA (Hibernate)

We cannot map a dynamic JSON payload to strict Java fields trivially. We use an \`@Convert\` annotation to marshal data.

\`\`\`java
@Entity
public class TrackerEntry {
    @Id
    private Long id;

    // Use a custom compiler to translate Java Maps to Database Strings
    @Convert(converter = JpaMapConverter.class) 
    @Column(length = 2000)
    private Map<String, Object> fieldValues;
}
\`\`\`

**The Converter class:**
\`\`\`java
@Converter
public class JpaMapConverter implements AttributeConverter<Map<String, Object>, String> {
    private final ObjectMapper mapper = new ObjectMapper();

    // Java -> Postgres
    public String convertToDatabaseColumn(Map<String, Object> meta) {
        return mapper.writeValueAsString(meta); 
    }

    // Postgres -> Java
    public Map<String, Object> convertToEntityAttribute(String dbData) {
        return mapper.readValue(dbData, new TypeReference<Map<String, Object>>() {});
    }
}
\`\`\`

## 3. Querying JSON Constraints

Because we store data in JSON strings (or JSONB in optimized Postgres environments), we can no longer do simple SQL like:
\`SELECT * FROM Entries WHERE cost > 50\`

To query, aggregate, and sum data for the Intelligence Reports, the backend must load the Entries into memory, parse the Maps, and execute Java stream aggregations:
\`\`\`java
// Stream Aggregation in Java
double totalCost = entries.stream()
    .mapToDouble(e -> Double.parseDouble(e.getFieldValues().get("cost").toString()))
    .sum();
\`\`\`
*Note: For enterprise scale (>1M rows), this architecture would require migrating the \`@Column\` explicitly to Postgres \`JSONB\` datatype and issuing native JSON-index queries (\`WHERE payload->>'cost' > '50'\`).*
  `,

  'profile': `
# Masterclass: Security & Identity with Stateless JWTs

Modern Single Page Applications (SPAs) like Angular do not use "Sessions". Historically, a server remembered who you were by storing a \`SessionId\` in server memory. This breaks scaling (what happens if your traffic routes to a different server?).

We resolve this using **Stateless JSON Web Tokens (JWT)**.

## 1. What is a JWT?

A JWT is a cryptographically signed string. It is conceptually a passport. 

\`\`\`json
// Header (Algorithm used)
{ "alg": "HS256" }
.
// Payload (The facts about the user)
{ "userId": 404, "role": "ADMIN", "exp": 1740000000 }
.
// Signature (The wax seal)
Base64URL( HMACSHA256( Header + "." + Payload, "SUPER_SECRET_KEY" ))
\`\`\`

When a user logs in, the Server provides this Token. The server then **forgets who the user is**.

## 2. The Auth Guard (Frontend Execution)
When Angular tries to navigate to a restricted page (\`/track\`), it triggers an \`AuthGuard\`.

\`\`\`typescript
@Injectable({ providedIn: 'root' })
export class AuthGuard implements CanActivate {
  canActivate(): boolean {
    const token = localStorage.getItem('jwt_token');
    if (token) return true; // Let them pass
    
    this.router.navigate(['/login']); // Kick them out
    return false;
  }
}
\`\`\`
The Angular client sends this token attached to the \`Authorization: Bearer <TOKEN>\` header on every subsequent HTTP request.

## 3. The Backend Verification Filter

Because anyone can fake an Angular frontend request, the Spring Boot API must intercept and verify the cryptographically signed "passport".

\`\`\`java
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) {
        String token = request.getHeader("Authorization");
        
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            
            // MATH HAPPENS HERE: The server tries to re-hash the header+payload using its secret key.
            // If the hash matches the signature, the token was NOT tampered with.
            if (jwtService.validateToken(token)) {
                Long userId = jwtService.extractUserId(token);
                // We securely staple the unforgeable userId to the server request
                request.setAttribute("userId", userId); 
            }
        }
        chain.doFilter(request, response);
    }
}
\`\`\`

By extracting the \`userId\` on the backend using the un-forgeable token, we effectively eliminate **Insecure Direct Object Reference (IDOR)** vulnerabilities. An attacker cannot merely change a parameter like \`?userId=12\` because the Controller inherently ignores it and solely trusts the \`request.getAttribute("userId")\` derived mathematically from the token.
  `,

  'default': `
# Application Architecture & Fullstack Foundations

Welcome to the internal Engineering Masterclass. Choose a module from the sidebar to begin learning the fundamental platform principles governing modern web application architecture.

## The Fullstack Blueprint

The modern web is highly decoupled. Rather than rendering HTML on a server (e.g. PHP, legacy Ruby on Rails), modern applications execute a distinct Frontend and Backend separation.

\`\`\`mermaid
graph TD
    Client[Browser Context]
    Angular[Angular 17 UI State Engine]
    API[Spring Boot 3 REST API]
    DB[(PostgreSQL)]
    
    Client -->|Loads JS Bundle| Angular
    Angular -->|AJAX XHR / JSON| API
    API -->|TCP/IP SQL Queries| DB
\`\`\`

### 1. The Client (Angular)
The browser downloads the entire Angular application (the "Bundle") when you visit the website. From that point on, you are running a complete desktop-like application *inside* your browser tab. Clicking links (like navigating from Dashboard to Reports) does not ping the server for a new page; Angular's internal Router simply rewaps the DOM locally, providing instantaneous transitions.

### 2. The API (Spring Boot)
The server acts purely as a data-broker and security bouncer. It exposes specific URL paths (e.g., \`GET /api/omni/apps\`). It receives JSON payloads, mathematically verifies the user's JWT authorization token, calculates business logic, issues SQL checks against PostgreSQL, and returns pure JSON text back over HTTPS. 
  `
};
