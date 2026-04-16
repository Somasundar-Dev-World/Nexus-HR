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
