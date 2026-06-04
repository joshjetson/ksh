---
name: uda
description: Apply the Universal Declarative Architecture (UDA) when implementing, modifying, OR reviewing ANY feature in this Grails 6.2.3 + HTMX + Tailwind LMS. Use BEFORE adding a controller/service/view, writing a GSP partial or component, designing a data instruction, wiring HTMX, deciding service-vs-instruction, or judging whether a dedicated controller is justified. UDA = one UniversalController + one UniversalDataService + declarative data instructions (list/get/count/filter/filterCount/exists/search/findByOrGet/currentUser/service/literal/param/date) written in hx-vals, generic role+ownership-aware CRUD at /universal/save|update|delete, a DRY component library under views/universal/components/, and HTMX #content swaps. Triggers: "add a feature", "new domain/page/partial", "submit/save/edit/delete X", "show a list of X", "is the user enrolled/reviewed", review/audit a change for DRY/UDA conformance. Full reference: docs/uda.md.
---

# UDA — operating skill

This codebase is the **canonical UDA implementation**. On every implementation or review, follow the pattern. The full reference is **`docs/uda.md`** — read it when you need the instruction vocabulary, whitelist values, sharp edges, or the spoofed-creator defense. This file is the fast operational checklist.

## Reflex on any "add/change a feature" request

1. **Default to no new controller and no new service.** A CRUD-shaped feature is: domain class + migration + whitelist entries + GSP partial + HTMX wiring.
2. **Reads** come from data instructions in `hx-vals`: `data[key]=instruction`. **Writes** go through `POST /universal/save|update|delete?domainName=X`.
3. **Booleans in a view** (enrolled? reviewed? owns?) use `exists:` — **never** a domain query in a `<% %>` scriptlet.
4. **Forms return the next view**: every form includes `template` + `data[]` in `hx-vals`, targets `#content`, so the mutation response IS the next screen (no redirect, no second request).
5. **Reuse components** from `views/universal/components/` (`_input`, `_button`, `_select`, `_textarea`, `_emptyState`, `_courseCard`, `_progressBar`, `_badge`, `_starRating`, `_avatarPicker`). If UI repeats, it's a component. Add only optional, backward-compatible params to shared ones.

## The four whitelists in `UniversalController` are the contract

Adding a domain to a feature means editing these — nowhere else:
- `ALLOWED_DOMAINS` — readable via instructions.
- `ALLOWED_CRUD_DOMAINS` — `domain → [roles]` writable via save/update/delete.
- `OWNERSHIP_FIELDS` — `domain → ownerField`; force-set on create, enforced on update/delete (admins bypass).
- `ALLOWED_SERVICE_METHODS` — method names callable via `service:`.

`User` is read-only here (in `ALLOWED_DOMAINS`, NOT in `ALLOWED_CRUD_DOMAINS`) — user writes go through `UserController`.

## Dedicated controllers that are NOT violations

`LoginController` (auth), `UserController` (user CRUD + password + roles, field-allowlisted), `ScormController` (SCORM runtime/binary/CMI), `AvatarController` & `BrandingController` (binary serving). The test for a *new* one: *is it generic CRUD-render of a domain?* If yes → UDA. If it's auth, binary streaming, a third-party runtime, or external ingest → dedicated is fine; state why.

## Non-negotiables (reject on sight)

- New controller/service for CRUD-shaped work.
- Domain query in a GSP scriptlet → `exists:`/`filter:`/`service:`.
- `save(flush: true)` without a documented reason; missing `failOnError: true`.
- Ownership hidden field (`name="user.id"`) without an `OWNERSHIP_FIELDS` entry.
- Stripping params before the data binder; calling `discard()` on `currentUser`.
- Criteria using DB column names instead of camelCase Groovy properties.
- Large `byte[]` mapped without `lazy: true`.
- Per-feature `UrlMappings` entries; hover-only interactions; tap targets under `min-h-[44px]`.

## Checklists & deep reference

`docs/uda.md` has: the full instruction vocabulary table, criteria-value resolution rules, the add-a-feature / add-an-instruction / PR-review checklists, the "my partial isn't getting its data" debug flow, and the sharp edges (colon delimiter, GSP apostrophe parser, param passthrough). Consult it before designing anything non-trivial. Keep its whitelist snapshot in sync when you edit `UniversalController`.
