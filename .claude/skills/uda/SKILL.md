---
name: uda
description: Apply the Universal Declarative Architecture (UDA) when implementing, modifying, OR reviewing ANY feature in this Grails 6.2.3 + HTMX + Tailwind LMS. Use BEFORE adding a controller/service/view, writing a GSP partial or component, designing a data instruction, wiring HTMX, deciding service-vs-instruction, or judging whether a dedicated controller is justified. UDA = one UniversalController + one UniversalDataService + declarative data instructions (list/get/count/filter/filterCount/latest/latestCount/exists/search/fts/distinct/findByOrGet/currentUser/service/config/literal/param/date) written in hx-vals, generic role+ownership-aware CRUD at /universal/save|update|delete|saveBatch, authed binaries at /universal/asset, a DRY component library under views/universal/components/, and HTMX #content swaps. Triggers: "add a feature", "new domain/page/partial", "submit/save/edit/delete X", "show a list of X", "is the user enrolled/reviewed", "search X", review/audit a change for DRY/UDA conformance. Full reference: docs/uda.md.
---

# UDA — operating skill

This codebase is the **canonical UDA implementation**. On every implementation or review, follow the pattern. The full reference is **`docs/uda.md`** — read it when you need the instruction vocabulary, whitelist values, read-scoping semantics, sharp edges, or the spoofed-creator defense. This file is the fast operational checklist.

## Reflex on any "add/change a feature" request

1. **Default to no new controller and no new service.** A CRUD-shaped feature is: domain class + migration + whitelist entries + GSP partial + HTMX wiring. Batch writes use `POST /universal/saveBatch` (natural-key upsert via `kshSaveBatch(...)`), never a bespoke endpoint. Authed binaries stream via `/universal/asset` + `AUTHED_BINARY_FIELDS`, never a new controller.
2. **Reads** come from data instructions in `hx-vals`: `data[key]=instruction`. **Writes** go through `POST /universal/save|update|delete?domainName=X`. Reaction-style taps add `quiet=true` to skip the success toast.
3. **Booleans in a view** (enrolled? reviewed? owns?) use `exists:` — **never** a domain query in a `<% %>` scriptlet. **Text search** uses `fts:Domain:search_fts:q` (Postgres generated tsvector + GIN; every searchable table's column is named `search_fts`) via the `_searchBar` component.
4. **Forms return the next view**: every form includes `template` + `data[]` in `hx-vals`, targets `#content`, so the mutation response IS the next screen. ⚠ htmx `hx-vals` inheritance: `#content` carries the landing template, so nested forms MUST set their own `template` (or `"template": ""` for quiet posters) or they'll get the landing view back.
5. **Policy-shaped reads go through services** (pillar 6): visibility a criteria string can't express (wall post public/staff/self, PM/block rules, calendar layers) lives in `@ReadOnly` methods on `MessageService`/`CommunityService`/`ScheduleService`/`DashboardService`, whitelisted in `ALLOWED_SERVICE_METHODS`, returning **all-scalar maps**. Writes still go through generic CRUD, with domain-validator gating where needed (the `Message` staff-channel/muted check reads `SecurityContextHolder` — injected services don't autowire on `newInstance()`).
6. **Reuse components** from `views/universal/components/` (`_input`, `_button`, `_select`, `_textarea`, `_emptyState`, `_courseCard`, `_progressBar`, `_statCard`, `_badge` (achievement), `_pill` (status chip), `_starRating`, `_avatar`, `_avatarPicker`, `_navLink`, `_searchBar`) and tags from `KshTagLib` (`ksh:trendBars`, `ksh:statusBadge`, `ksh:roleBadge`, `ksh:credits`, `ksh:eventChip`, `ksh:eventDot`, `ksh:courseImage`). Buttons use the `.btn-primary/.btn-cta/.btn-secondary/.btn-danger/.btn-sm/.btn-delete` classes and cards use `.card` from `src/input.css`; palette is cream (bisque `#ffe4c4` = cream-200) + gold CTA + rose primary. If UI repeats, it's a component; if output is computed, it's a tag. Tailwind scans `grails-app/**/*.{gsp,html,groovy,js}` and `src/**`.
7. **Nav destinations are declared once** in the `V` map in `views/universal/index.gsp` and rendered via `_navLink` (variants `side`/`tab`/`drawer`) into the desktop sidebar, mobile bottom tab bar, and More sheet; `kshNav()` syncs the active state.

## The whitelists in `UniversalController` are the contract

Adding a domain to a feature means editing these — nowhere else (live values: docs/uda.md pillar 3, or the controller itself):
- `ALLOWED_DOMAINS` — readable via instructions.
- `MANAGER_READ_DOMAINS` — generic read is manager-only (ADMIN/TEACHER); non-manager reads go through a policy service (currently `CommunityPost`).
- `READ_SCOPE_FIELDS` — **read-scoping axis**: non-managers see only rows where `field` is them (AND-ed into every read; `search`/`fts`/`distinct` refuse scoped domains). Managers see all.
- `ALLOWED_CRUD_DOMAINS` — `domain → [roles]` (flat) or `domain → [create:…, update:…, delete:…]` per-op.
- `OWNERSHIP_FIELDS` — **WRITES ONLY in KSH** (divergence from VOG): force-set owner on create, owner-check update/delete with **ROLE_ADMIN-only** bypass (a teacher can't edit another teacher's course). Reads stay world-readable (Course/Review). Owner-read AND owner-written domains go in BOTH this and `READ_SCOPE_FIELDS` (`UserSetting`, `UserBlock`).
- `AUTHOR_FIELDS` — stamp the creator on shared content (Announcement/MediaPost/CommunityPost author, Grade gradedBy); no read scoping, no owner-only writes.
- `AUTHED_BINARY_FIELDS` — `byte[]` fields streamable via `/universal/asset` (FileResource.file, MediaPost.image).
- `ALLOWED_SERVICE_METHODS` — method names callable via `service:` (spans Scorm/Dashboard/Schedule/Message/Community services).
- `ALLOWED_CONFIG_KEYS` — `config:` instruction gate; **currently empty**.

Note the asymmetry: read-side manager bypass is ADMIN||TEACHER; write-side ownership bypass is ADMIN only — deliberate. Criteria tokens: `currentUserId`, `activeTerm` (the single active Term's id, else 0). Criteria value forms include `null`, `in:a|b|c` (empty → match-nothing sentinel), `yyyy-MM-dd` whole-day, and `assoc.field` (auto aliased JOIN). `User` is read-only here (in `ALLOWED_DOMAINS`, NOT in `ALLOWED_CRUD_DOMAINS`) — user writes go through `UserController`.

## Dedicated controllers that are NOT violations

`LoginController` (auth), `UserController` (user CRUD + password + roles, field-allowlisted), `ScormController` (SCORM runtime/CMI), `PublicController` (anonymous landing + self-registration, `permitAll`), `AvatarController`, `BrandingController`, `BadgeController`, `CourseController` (all four: binary serving). **`MessagesController` is gone** — channel messaging is UDA now (generic `save` of `Message` + access-checked `MessageService` reads). The test for a *new* controller: *is it generic CRUD-render of a domain?* If yes → UDA. Auth, anonymous binary streaming, or a third-party runtime → dedicated is fine; state why. Authed binaries → `/universal/asset`, not a controller.

## SSE

Hand-rolled `text/event-stream` over a Servlet `AsyncContext` (`sseContexts` + `broadcastEvent`). **Never return a Spring `SseEmitter` from a Grails action** — Grails renders it as an empty response. Keep `X-Accel-Buffering: no` (proxies buffer the stream without it).

## Non-negotiables (reject on sight)

- New controller/service for CRUD-shaped work; bespoke batch endpoints (use `saveBatch`); new controllers for authed binaries (use `asset`).
- Domain query in a GSP scriptlet → `exists:`/`filter:`/`fts:`/`service:`.
- `save(flush: true)` without a documented reason; missing `failOnError: true`.
- Ownership/author hidden field (`name="user.id"`) without an `OWNERSHIP_FIELDS`/`AUTHOR_FIELDS` entry; adding a domain without choosing its read posture (world-readable vs `READ_SCOPE_FIELDS` vs `MANAGER_READ_DOMAINS`).
- Stripping params before the data binder; calling `discard()` on `currentUser`.
- Criteria using DB column names instead of camelCase Groovy properties.
- Large `byte[]` mapped without `lazy: true`; a service read returning live GORM instances instead of scalar maps.
- Helper params named `<assoc>Id` on saves (read-only derived FK — use `forCourse`/`cardId`-style names); nested forms without their own `template` (hx-vals inheritance leak).
- Per-feature `UrlMappings` entries; hover-only interactions; tap targets under `min-h-[44px]`; hand-styled buttons instead of `.btn-*`.

## Checklists & deep reference

`docs/uda.md` has: the full instruction vocabulary table (incl. `fts`/`latest`/`latestCount`/`distinct`/`config`), criteria-value resolution rules, all nine whitelist axes with live values, the feature↔pattern map (announcements/calendar, files/media, privacy, channel messaging, community wall, live classrooms), the add-a-feature / add-an-instruction / PR-review checklists, the "my partial isn't getting its data" debug flow, and the sharp edges (colon delimiter, GSP apostrophe + `>`-in-attribute parser, H2 `day` reserved word, `[:]` vs `[]`, hx-vals inheritance, `<assoc>Id` collision, param passthrough). Consult it before designing anything non-trivial. Keep its whitelist snapshot in sync when you edit `UniversalController`.
