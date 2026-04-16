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
