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
