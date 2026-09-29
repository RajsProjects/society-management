# Google Stitch Prompt: Society Management System

Use this prompt in Google Stitch to generate a polished, responsive web
application for **CivicLink Society Management**. This is a multi-tenant
residential society operations platform used by society administrators,
residents, finance staff, security staff, and platform administrators.

## Product direction

Design a trustworthy, calm, operational dashboard for apartment communities.
The interface should feel like a well-run residential campus: clear,
welcoming, structured, and dependable. Prioritize scanability and confidence
over decoration. Every screen must make the next action obvious.

Do not invent a consumer social-media aesthetic. This is a serious SaaS
application handling residents, complaints, bills, announcements, documents,
subscriptions, and community issues.

## Primary users and permissions

Create role-aware navigation and screens:

- **Resident:** view announcements, submit and track complaints, report issues,
  vote on community issues, view bills, and pay eligible bills.
- **Society Admin:** manage residents, flats, announcements, complaints, issues,
  bills, society settings, and subscription plans.
- **Accountant:** manage maintenance bills and payment status.
- **Security Staff:** access security-focused operational areas when enabled.
- **Platform Admin:** review society registrations, verify societies, and
  manage platform-level operations.

Never show actions that the current role cannot perform. Visually distinguish
read-only, pending approval, blocked, expired subscription, and authorized
states.

## Screens to generate

Generate a coherent screen set with shared navigation and components:

1. **Login**
   - Email and password fields
   - Clear validation and error states
   - Password visibility toggle
   - Forgot-password entry point
   - Society-aware brand context without exposing private data

2. **Resident dashboard**
   - Greeting and current society context
   - Outstanding bill summary
   - Open complaints and issue activity
   - Latest announcements
   - Quick actions: Pay bill, Raise complaint, Report issue

3. **Admin dashboard**
   - Resident, flat, bill, complaint, and issue metrics
   - Pending approvals requiring attention
   - Recent activity timeline
   - Payment collection summary
   - Subscription status and renewal warning

4. **Residents management**
   - Search, pagination, filters by status and role
   - Resident table with apartment, status, and last activity
   - Approve, deactivate, or block actions with confirmation
   - Invite resident flow
   - Empty, loading, validation, and permission-denied states

5. **Complaints**
   - Resident complaint submission form
   - Admin queue with status and category filters
   - Complaint detail drawer or page
   - Status progression: Open, In Progress, Resolved, Rejected
   - Admin note and resolution history

6. **Community issues**
   - Issue list with status, priority, creator, and vote count
   - Report issue form with optional photo attachment
   - Issue detail with vote/unvote action
   - Admin status and priority controls

7. **Announcements**
   - Announcement feed with type labels
   - Admin create/edit/delete controls
   - Emergency announcements must be visually prominent but not alarmist
   - Detail view with date, author, and society context

8. **Maintenance bills**
   - Resident bill list with status, month, amount, and due date
   - Bill detail and payment confirmation
   - Admin bill creation form
   - Filters for Pending, Paid, and Overdue
   - Clear prevention of duplicate or invalid payment actions

9. **Society registration**
   - Public multi-step registration form
   - Society details, administrator details, and document upload
   - Progress indicator and review summary
   - Pending-verification confirmation state

10. **Platform admin**
    - Society verification queue
    - Society detail and uploaded registration document access
    - Approve/reject actions with required rejection reason
    - Clear separation from society-level navigation

11. **Subscription and billing**
    - Current plan, usage, renewal date, and payment status
    - Plan comparison with transparent INR pricing
    - Upgrade/change-plan confirmation
    - Expired or past-due locked-state screen with renewal action

12. **System states**
    - Skeleton loading states matching final layouts
    - Inline validation errors
    - Toasts for successful actions
    - Empty states with useful next actions
    - 401 session-expired state
    - 403 permission-denied state
    - 404 not-found state
    - 402 subscription-expired state
    - 500 retryable server-error state

## Visual design system

Create a premium but restrained dashboard aesthetic:

- **Atmosphere:** balanced daily operations app; density 6/10, variance 5/10,
  motion 4/10.
- **Canvas:** `#F5F7F8`, a cool off-white background.
- **Surface:** `#FFFFFF`, used for elevated panels and forms.
- **Ink:** `#172126`, a deep charcoal rather than pure black.
- **Muted text:** `#647277`, for metadata and secondary descriptions.
- **Border:** `#DCE4E7`, for structural separation.
- **Accent:** `#0F766E`, a single muted teal for primary actions, active states,
  links, and focus rings.
- **Success:** `#15803D`.
- **Warning:** `#B45309`.
- **Danger:** `#B42318`.

Use only the single teal accent for primary brand emphasis. Do not use neon
gradients, purple/blue glow effects, or multiple competing accent colors.

## Typography

- Use **Satoshi** or **Geist** for display and body text.
- Use **Geist Mono** for invoice numbers, timestamps, payment references, and
  dense numeric metrics.
- Headlines should be confident and compact, not oversized.
- Body text must be highly readable with relaxed line height.
- Use weight and color for hierarchy before increasing font size.
- Avoid generic serif fonts and avoid Inter.

## Layout and components

- Use a max-width content container around 1440px.
- Use a persistent desktop sidebar with society name, role, and navigation.
- Collapse to a compact top bar plus bottom or drawer navigation on mobile.
- Use a 12-column grid on desktop and a single-column flow below 768px.
- Use generous but efficient spacing: 8px base rhythm, 24px panel padding.
- Use cards only when elevation communicates hierarchy; use dividers for dense
  tables and operational lists.
- Use table headers, aligned numeric columns, clear status badges, and sticky
  action areas where useful.
- Buttons must have at least a 44px touch target and clear loading/disabled
  states.
- Forms use labels above inputs, helper text below where needed, and inline
  errors directly under the invalid field.
- Use confirmation dialogs for destructive actions and payment submission.
- Use restrained 12px corner radii; avoid excessive pill-shaped containers.

## Navigation and information architecture

Desktop navigation:

- Overview
- Residents
- Flats
- Maintenance Bills
- Complaints
- Community Issues
- Announcements
- Documents
- Subscription
- Society Settings

Keep Platform Admin navigation separate:

- Verification Queue
- Societies
- Platform Users
- Platform Health

Always show the active society name and current user role. Never imply that a
user can access another society's records.

## Interaction and security-aware UX

- Use optimistic feedback only for reversible local interactions such as
  toggling a vote; confirm server success before showing permanent state.
- Disable duplicate submissions while requests are in progress.
- Never display passwords, secrets, JWTs, API keys, or full payment credentials.
- Mask sensitive identifiers and show only the minimum required information.
- Make unauthorized actions unavailable, not merely hidden behind an error.
- Use neutral error copy that does not reveal whether another account exists.
- Show session expiration with a clear re-login action.
- Keep tenant context visible in admin screens to reduce cross-society mistakes.
- For expired subscriptions, preserve read access while clearly explaining why
  write actions are unavailable.

## Responsive behavior

- Mobile-first layout with no horizontal scrolling.
- Collapse all multi-column layouts below 768px.
- Tables become stacked list rows or horizontally contained data regions.
- Keep all controls at least 44px high.
- Preserve the order: page title, primary action, filters, content, pagination.
- Use `clamp()` for headline sizing and responsive spacing.
- Do not use overlapping text, floating controls that cover content, or
  viewport-dependent absolute positioning.

## Copy style

Use concise, human operational language:

- “Review pending residents”
- “Create maintenance bill”
- “Payment required”
- “Complaint updated”
- “This society is awaiting verification”
- “You do not have permission to perform this action”

Avoid vague marketing language such as “seamless”, “next-generation”,
“revolutionary”, “elevate”, or “unlock your potential”.

## Explicitly avoid

- No emojis in the interface.
- No pure black backgrounds or text.
- No purple/blue neon gradients or outer glows.
- No generic three-equal-card feature rows.
- No centered marketing hero for authenticated dashboards.
- No fake statistics or invented residents.
- No names such as “John Doe”, “Acme”, or “Nexus”.
- No exposed credentials or sensitive data in mock content.
- No giant decorative illustrations that reduce operational density.
- No circular spinner-only loading states; use skeletons matching the layout.
- No horizontal scrolling on mobile.
- No ambiguous unlabeled icon-only actions.
- No destructive action without confirmation.
- No content overlap or unreadable contrast.

## Stitch output request

Generate the complete visual system and representative screens above as one
cohesive product. Reuse the same sidebar, header, tables, forms, filters,
status badges, dialogs, empty states, and responsive rules across all screens.
Prioritize production-quality information architecture and realistic society
operations over decorative mockups.
