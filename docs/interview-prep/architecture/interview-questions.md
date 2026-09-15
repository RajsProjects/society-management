# Interview Questions and Answer Prompts

Use these prompts for rehearsal. Answer from this project first, then generalize.

## Project understanding

1. What problem does the Society Management system solve?
2. Why did you choose a modular monolith instead of microservices?
3. Walk through a complaint creation request from HTTP to MongoDB.
4. Which modules are business modules and which are cross-cutting infrastructure?
5. What would you change before calling this production-ready?

## Security

6. How does a JWT become an authenticated Spring Security principal?
7. Where do you enforce tenant isolation?
8. Why are roles alone insufficient for a resident accessing a resource?
9. How would you revoke a JWT before expiry?
10. How would rate limiting work with two Kubernetes replicas?

## Data and consistency

11. Why MongoDB for this domain? When would PostgreSQL be a better choice?
12. Which queries need indexes and how would you prove the index helps?
13. How do you prevent duplicate payment webhooks from double-applying a payment?
14. What is the source of truth for a payment?
15. How would you migrate a document shape without downtime?

## Reliability and scale

16. What happens when email succeeds but the API response fails?
17. What happens when Razorpay times out after creating a payment?
18. Which work should move to an asynchronous worker?
19. What metrics and alerts would you create first?
20. How would you diagnose a slow issue-list endpoint?

## Testing and delivery

21. What does a useful service unit test cover?
22. Which integrations need contract or test-container tests?
23. How do readiness and liveness differ?
24. How would you roll out a schema change safely?
25. What load-test scenario represents a real society-management workload?

## Strong answer pattern

Use **context -> current design -> trade-off -> failure mode -> improvement**:

> “We use X because of Y. The request goes through A, B, and C. The main risk is D, especially when E happens. I would improve it with F, while preserving G.”
