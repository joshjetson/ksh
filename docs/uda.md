# UDA — Universal Declarative Architecture

> **About this document.** Architectural reference for the Korean School House (KSH) LMS — what UDA is, how it's structured, and the rules that keep it DRY. This codebase is the **canonical reference implementation** of UDA. It doubles as the operating manual for AI tooling: `.claude/skills/uda/SKILL.md` points here for depth, and `.claude/agents/uda-expert.md` mirrors this document. Keep all three in lockstep — when you change the pattern or the whitelists, update this file. Written in second person because of that dual purpose; it applies to anyone touching the UDA surface.

You are working in the **Universal Declarative Architecture (UDA)** — a pattern the user invented and instantiates in this Grails 6.2.3 + HTMX + Tailwind codebase (Korean School House — a Korean-language LMS). Your job is to explain, defend, extend, and review UDA-shaped code with precision, and to reach for the pattern before writing any new controller or service.

## The one-paragraph description

UDA collapses the conventional MVC "controller per feature, service per feature, view per feature" sprawl into **one controller (`UniversalController`)**, **one data service (`UniversalDataService`)**, and **a vocabulary of declarative data instructions** the frontend writes in HTMX params. HTMX is the only client-side framework. The frontend declares what view it wants and what data that view needs (`?template=courses/browse&data[courses]=list:Course&data[user]=currentUser`). The backend resolves each instruction against whitelisted domains — with role-aware read scoping — builds a model map, renders a GSP partial, and HTMX swaps it into `#content`. CRUD is generic (`POST /universal/save?domainName=Review`), including batch upsert (`saveBatch`). New feature work means: domain class + migration + whitelist entries + GSP partial + HTMX wiring. **Almost no new controller or service code.** The one sanctioned service-code path: reads whose visibility is *policy-shaped* (who may see this post/message?) go through small read-side services exposed via the `service:` instruction (pillar 6).

## The mental model

Think of UDA as a **declarative query language for a single render endpoint**, paired with a **role + ownership-aware generic CRUD endpoint**, and a **DRY component library**. The view layer is in charge of what data it needs; the backend is in charge of resolving instructions safely. There is no business logic in templates and no template logic in controllers.

```
                   ┌──────────────────────────────────┐
                   │  HTMX in browser                 │
                   │  hx-get / hx-post + hx-vals      │
                   │  declares: template + data[*]    │
                   └──────────────┬───────────────────┘
                                  │  /universal/showView, /universal/save, ...
                                  ▼
        ┌────────────────────────────────────────────────────┐
        │  UniversalController                               │
        │   • whitelists (domains, CRUD roles, services,     │
        │     read scopes, author stamps, binaries, config)  │
        │   • parses data[*] instructions                    │
        │   • role + ownership gates on CRUD                 │
        │   • role-scoped reads (READ_SCOPE / MANAGER_READ)  │
        │   • renders partial template with resolved model   │
        └──────────────┬─────────────────────────────────────┘
                       │
                       ▼
        ┌────────────────────────────────────────────────────┐
        │  UniversalDataService (agnostic)                   │
        │   list, count, getById, filter, filterCount,       │
        │   filterLatest(+Count), distinctValues, search,    │
        │   ftsSearch, exists, findByOrGet, save, update,    │
        │   delete, upsertBatch                              │
        └──────────────┬─────────────────────────────────────┘
                       │
                       ▼
                   ┌──────────────────────────────────┐
                   │  GORM domain class               │
                   └──────────────────────────────────┘
```

Alongside the agnostic engine sit small **read-side services** (`MessageService`, `CommunityService`, `ScheduleService`, `DashboardService`) invoked via the `service:` instruction. They exist for visibility policies and aggregations that a declarative criteria string can't express (pillar 6). Writes still flow through the generic CRUD engine.

The shell — `views/universal/index.gsp` — is the **only full page** after login. It is a responsive shell: desktop sidebar, mobile bottom tab bar, mobile "More" bottom sheet — all rendering the same `_navLink` component — and a single `#content` div. Every navigation and every action is an HTMX swap. The browser never reloads.

---

# The pillars (memorize these)

## 1. The single render endpoint

`GET /universal/showView?template=path/name&data[key]=instruction[&moreParams=...]`

- `template` — the GSP partial path under `grails-app/views/universal/` (drop the `_` prefix). Example: `template=courses/browse` renders `_browse.gsp`.
- `data[key]=instruction` — repeat per model key. Each instruction is resolved into a value bound to that key in the rendered model.
- Any other params (e.g. `courseId=7`) are part of the request and can be referenced by instructions like `get:Course:courseId` or by `param:` resolution.

The same endpoint serves HTMX (returns the partial fragment) and direct browser GET (returns a full view via the `view` param). HTMX is detected via `HX-Request: true`.

## 2. The data-instruction vocabulary

Instructions are colon-delimited. This is the dialect — defined in `_instructionHandlers` in `UniversalController`. All read instructions are **scope-aware** (see pillar 3): on a `READ_SCOPE_FIELDS` domain a non-manager only sees their own rows; `MANAGER_READ_DOMAINS` are manager-only.

| Instruction | Meaning | Example |
|---|---|---|
| `list:Domain` | All instances of Domain (paginated, capped at 100; read-scoped) | `data[courses]=list:Course` |
| `count:Domain` | Total count (read-scoped) | `data[total]=count:Course` |
| `get:Domain:paramName` | Single instance by ID from `params[paramName]` (default `id`); row-ownership checked on scoped domains | `data[course]=get:Course:courseId` |
| `filter:Domain:f=v,f=v` | Criteria-based list (see criteria coercion below) | `data[mine]=filter:CourseEnrollment:user.id=currentUserId` |
| `filterCount:Domain:f=v` | Count for the same criteria (uses SQL COUNT) | `data[n]=filterCount:CourseEnrollment:user.id=currentUserId` |
| `latest:Domain:dateField:criteria` | Rows where `dateField = max(dateField)` among the matches — "the most recent snapshot" | `data[roster]=latest:Attendance:day:user.id=currentUserId` |
| `latestCount:Domain:dateField:criteria` | Count companion of `latest:` | `data[n]=latestCount:Attendance:day:status=absent` |
| `exists:Domain:f=v` | Boolean — does any matching record exist | `data[enrolled]=exists:CourseEnrollment:user.id=currentUserId,course.id=courseId` |
| `search:Domain:fields:paramName` | `ilike` across one or more fields, escaped wildcards. Refused on read-scoped domains (no criteria arg to scope) | `data[q]=search:Course:longTitle,shortTitle:searchTerm` |
| `fts:Domain:search_fts:qParam` | Postgres full-text search via the domain's STORED generated `search_fts` tsvector column + GIN index, `ts_rank`-ordered; query param defaults to `q`. Refused on read-scoped domains | `data[items]=fts:Course:search_fts:q` |
| `distinct:Domain:field` | Unique non-null values of one field, ascending. Refused on read-scoped domains | `data[statuses]=distinct:CourseEnrollment:status` |
| `findByOrGet:Domain:field:paramName` | Try `findByX(value)` first; fall back to `get(id)` | `data[c]=findByOrGet:Course:shortTitle:title` |
| `currentUser` | The logged-in `User` (re-loaded fresh; do NOT discard) | `data[user]=currentUser` |
| `service:method` | Call a whitelisted `UniversalDataService` method | `data[cmi]=service:getCmiData` |
| `service:serviceName:method` | Call another wired service's whitelisted method (`getServiceByName`) | `data[wall]=service:communityService:generalWall` |
| `config:key` | A whitelisted, non-sensitive config value (gated by `ALLOWED_CONFIG_KEYS` — **currently empty**) | `data[flag]=config:some.key` |
| `literal:value` | Coerced literal (`true`/`false`/raw string) | `data[mode]=literal:browse` |
| `param:name` | Raw value from `params[name]`, or `''` | `data[q]=param:searchTerm` |
| `date:today` | Today's date with cleared time | `data[today]=date:today` |

### Criteria-value resolution (the magic in `filter`/`filterCount`/`exists`/`latest`)

Inside `f=v` pairs, each value is resolved (`resolveCriteriaValues`) before it hits the service:
1. `currentUserId` → the logged-in user's id (most common token).
2. `activeTerm` → the id of the single active `Term` (`Term.findByActive(true)`), `'0'` when none — so rosters/attendance/grades filter declaratively by `term.id=activeTerm`.
3. A request param name → its value (e.g. with `courseId=7`, `course.id=courseId` becomes `course.id=7`).
4. Otherwise → literal.

In the service layer, `buildCriteriaBlock` then coerces values further:

| Value form | Meaning |
|---|---|
| `today` / `week` | date range (whole today / last 7 days) |
| `yyyy-MM-dd` | that whole calendar day on a date/timestamp field |
| `true` / `false` | boolean equality |
| `null` | `IS NULL` |
| `in:a\|b\|c` | membership test (`inList`). An **empty set uses a match-nothing sentinel** (`[-1L]`), so a scoped read with no allowed IDs sees no rows — never "no filter" |
| numeric | Long equality |
| anything else | string equality |

Association paths: `assoc.id=value` uses the FK column (no join); `assoc.field=value` (non-id) creates an **alias + INNER JOIN** automatically.

### Criteria fields use Groovy property names, NOT DB columns

`filter:CourseEnrollment:completedAt=null` — correct (camelCase property name).
`filter:CourseEnrollment:completed_at=null` — wrong (column name; Hibernate criteria can't resolve it).

### `exists` is special — it replaces scriptlet queries

Anywhere a GSP needs a boolean ("is the user enrolled? has the user reviewed this? does the user own this badge?"), use `exists:`. **Never** put a domain query in a scriptlet. This is one of the most-violated rules — watch for it during review.

```html
<!-- right -->
"data[enrolled]": "exists:CourseEnrollment:user.id=currentUserId,course.id=courseId"
<g:if test="${enrolled}">Already enrolled</g:if>

<!-- WRONG -->
<% def enrolled = CourseEnrollment.findByUserAndCourse(user, course) %>
<g:if test="${enrolled}">Already enrolled</g:if>
```

Real examples already wired in this codebase (see the `exists` handler comment block in `UniversalController`):

```
data[enrolled] = exists:CourseEnrollment:user.id=currentUserId,course.id=courseId
data[reviewed] = exists:Review:user.id=currentUserId,course.id=courseId
data[hasBadge] = exists:UserBadge:user.id=currentUserId,badge.id=badgeId
data[isCreator]= exists:Course:creator.id=currentUserId,id=courseId
```

### The FTS convention

Every searchable table gets a **STORED generated `search_fts` tsvector column + GIN index** via migration (see `add-fts.groovy` and the per-feature migrations that copy its shape). Generated columns auto-maintain — no trigger or backfill to drift. Everything uses the `'simple'` regconfig (lowercase + tokenize, no stemming), matching the `to_tsquery('simple', 'term:*')` prefix queries in `UniversalDataService.ftsSearch` — and behaving predictably for Korean text. The column is *always* named `search_fts`; the instruction is always `fts:Domain:search_fts:q`. `ftsSearch` validates the column name against `\w+` and binds the term as a `?` parameter; `tableNameFor` maps `User` → `app_user` (the security plugin reserves `user`). Adding FTS to a new domain = one changeSet + a `_searchBar` render — no service code.

## 3. Whitelists are the contract

Nine declarations in `UniversalController` define the entire read/write surface area. Adding a domain, service method, or config key anywhere else is wrong. **These are the live values — keep this section in sync when you edit the controller.**

```groovy
// Domains readable via data instructions (list/get/filter/exists/fts/...).
ALLOWED_DOMAINS = [
    'Course', 'CourseEnrollment', 'Badge', 'UserBadge', 'Review', 'User', 'AppConfig',
    'DiscountCode', 'BlackoutDate', 'EnrollmentChangeRequest', 'CourseReward',
    'Announcement',   // dashboard feed — all authed read
    'Event',          // calendar — all authed read
    'FileResource',   // files library — all authed read; binary via asset
    'MediaPost',      // media feed — all authed read; binary via asset
    'UserSetting',    // self-only (OWNERSHIP + READ_SCOPE → user) — discoverable toggle
    'UserBlock',      // self-only (OWNERSHIP + READ_SCOPE → owner) — personal block list
    'CommunityPost',  // wall reads go through CommunityService (visibility rules);
                      // generic read is MANAGER_READ-only for the moderator edit form
    'PostPin',        // self-only reactions (OWNERSHIP → user)
    'PostCheer',
    'Classroom',           // cohort names — all authed read
    'ClassroomStaff',      // teacher↔classroom assignment — teacher reads own (READ_SCOPE)
    'Term',                // school terms — all authed read; admin CRUD
    'ClassroomMembership', // learner reads own memberships (READ_SCOPE); teachers all
    'Attendance',          // learner reads own rows (READ_SCOPE); teachers all
    'Grade',               // learner reads own rows (READ_SCOPE); teachers all
]

// Non-owned back-office domains only managers (admin/teacher) may READ via instructions.
MANAGER_READ_DOMAINS = ['CommunityPost']

ALLOWED_SERVICE_METHODS = [
    'getCmiData', 'adminStats', 'studentStats',
    'monthView', 'dayView', 'yearView',   // ScheduleService — calendar drill-in
    'channelList',    // MessageService — channels the user may see
    'channelView',    // MessageService — one channel + its messages (access-checked)
    'channelMembers', // MessageService — channel member strip (moderators/admins)
    'memberActions',  // MessageService — one member's moderation actions (moderators)
    'channelPeople',  // MessageService — discoverable people in a channel (to PM)
    'personActions',  // MessageService — one person's PM/block actions
    'dmList',         // MessageService — the current user's direct-message threads
    'startDm',        // MessageService — find-or-create a DM (write; access-checked)
    'searchMessages', // MessageService — FTS over the user's visible messages
    'searchPeople',   // MessageService — FTS over people the user may PM
    'generalWall',    // CommunityService — the public (staff too, for managers) weekly wall
    'myWall',         // CommunityService — the user's own posts + pinned posts
    'searchPosts',    // CommunityService — moderator FTS across all posts (managers only)
    'postCard',       // CommunityService — one card, re-fetched after a cheer/pin/hide
]

// Config values exposed via the `config:` instruction — non-sensitive only.
ALLOWED_CONFIG_KEYS = []   // currently empty

// A CRUD value is either a flat List (same roles for every op) or a Map keyed by
// operation (create/update/delete) for per-operation control.
ALLOWED_CRUD_DOMAINS = [
    'Course'          : ['ROLE_TEACHER', 'ROLE_ADMIN'],
    'CourseEnrollment': [create: ['ROLE_USER', 'ROLE_TEACHER', 'ROLE_ADMIN'],
                         update: ['ROLE_ADMIN'], delete: ['ROLE_ADMIN']],
    'Review'          : ['ROLE_USER', 'ROLE_ADMIN'],
    'Badge'           : ['ROLE_TEACHER', 'ROLE_ADMIN'],
    'UserBadge'       : ['ROLE_ADMIN'],
    'DiscountCode'    : ['ROLE_TEACHER', 'ROLE_ADMIN'],
    'BlackoutDate'    : ['ROLE_TEACHER', 'ROLE_ADMIN'],
    'CourseReward'    : ['ROLE_TEACHER', 'ROLE_ADMIN'],
    'EnrollmentChangeRequest': [create: ['ROLE_USER', 'ROLE_TEACHER', 'ROLE_ADMIN'],
                                update: ['ROLE_ADMIN'], delete: ['ROLE_ADMIN']],
    'AppConfig'       : ['ROLE_ADMIN'],
    'Announcement'    : ['ROLE_TEACHER', 'ROLE_ADMIN'],
    'Event'           : ['ROLE_TEACHER', 'ROLE_ADMIN'],
    'FileResource'    : ['ROLE_TEACHER', 'ROLE_ADMIN'],
    'MediaPost'       : ['ROLE_TEACHER', 'ROLE_ADMIN'],
    // Self-service privacy — any signed-in user manages their OWN row (owner-scoped).
    'UserSetting'     : ['ROLE_USER', 'ROLE_TEACHER', 'ROLE_ADMIN'],
    'UserBlock'       : ['ROLE_USER', 'ROLE_TEACHER', 'ROLE_ADMIN'],
    // Messaging: admin manages channels; anyone may post a message (the Message
    // validator blocks posting into staff channels), delete is owner-or-admin.
    'Channel'         : ['ROLE_ADMIN'],
    'Message'         : [create: ['ROLE_USER', 'ROLE_TEACHER', 'ROLE_ADMIN'],
                         delete: ['ROLE_USER', 'ROLE_TEACHER', 'ROLE_ADMIN']],
    // Channel moderation (mute/ban) — moderators (community leaders) + admins.
    'ChannelMembership': ['ROLE_MODERATOR', 'ROLE_ADMIN'],
    // Community wall: any signed-in user posts; moderators/staff moderate (edit/delete).
    'CommunityPost'   : [create: ['ROLE_USER', 'ROLE_TEACHER', 'ROLE_ADMIN'],
                         update: ['ROLE_TEACHER', 'ROLE_ADMIN', 'ROLE_MODERATOR'],
                         delete: ['ROLE_TEACHER', 'ROLE_ADMIN', 'ROLE_MODERATOR']],
    // Wall reactions — any signed-in user cheers/pins on their own behalf (owner-scoped).
    'PostPin'         : ['ROLE_USER', 'ROLE_TEACHER', 'ROLE_ADMIN'],
    'PostCheer'       : ['ROLE_USER', 'ROLE_TEACHER', 'ROLE_ADMIN'],
    // Live classrooms: admin shapes the school; teachers run rosters/attendance/grades.
    'Classroom'           : ['ROLE_ADMIN'],
    'ClassroomStaff'      : ['ROLE_ADMIN'],
    'Term'                : ['ROLE_ADMIN'],
    'ClassroomMembership' : ['ROLE_TEACHER', 'ROLE_ADMIN'],
    'Attendance'          : ['ROLE_TEACHER', 'ROLE_ADMIN'],
    'Grade'               : ['ROLE_TEACHER', 'ROLE_ADMIN'],
]

// Row-level WRITE ownership: force-set on create, owner-checked on update/delete
// (ROLE_ADMIN bypasses). WRITES ONLY in KSH — see the divergence note below.
OWNERSHIP_FIELDS = [
    'Course'          : 'creator',
    'CourseEnrollment': 'user',
    'Review'          : 'user',
    'EnrollmentChangeRequest': 'requestedBy',
    'UserSetting'     : 'user',
    'UserBlock'       : 'owner',
    'Message'         : 'author',  // force-set author on create; delete is author-or-admin
    'PostPin'         : 'user',
    'PostCheer'       : 'user',
]

// Read-scoped rows: a non-manager sees only rows where this field is them (AND-ed
// into every read instruction); managers see all.
READ_SCOPE_FIELDS = [
    'UserSetting'         : 'user',
    'UserBlock'           : 'owner',
    'ClassroomStaff'      : 'staff',   // a teacher sees only their own assignments
    'ClassroomMembership' : 'user',
    'Attendance'          : 'user',
    'Grade'               : 'user',
]

// Author attribution — force-set to the creator on create, WITHOUT read scoping or
// owner-only writes: shared content stamped with who posted it (anti-spoof).
AUTHOR_FIELDS = [
    'Announcement' : 'author',
    'MediaPost'    : 'author',
    'CommunityPost': 'author',
    'Grade'        : 'gradedBy',   // stamp the teacher who entered it
]

// Authenticated binary fields — byte[] fields streamable to logged-in users via
// GET /universal/asset (ownership-aware).
AUTHED_BINARY_FIELDS = [
    'FileResource': ['file'],
    'MediaPost'   : ['image'],
]
```

- `ALLOWED_DOMAINS` — domains readable via data instructions (`list`, `get`, `filter`, `exists`, `fts`, etc.).
- `MANAGER_READ_DOMAINS` — domains only **managers** (`ROLE_ADMIN` or `ROLE_TEACHER`) may read generically. Used when non-manager reads must go through a policy-enforcing service instead: `CommunityPost` reads for students go through `CommunityService` (public/staff/self visibility can't be a declarative scope); the generic read exists solely for the moderator edit form.
- `READ_SCOPE_FIELDS` — the **read-scoping axis**: `domain → ownerField`. A non-manager sees only rows where that field is them — the scope is AND-ed into *every* read instruction (`list`/`count` are expressed as `filter`/`filterCount` so scoping lives in one path), and `get`/`findByOrGet`/`show`/`asset` row-check via `ownsOrManager`. `search`/`fts`/`distinct` have no criteria arg to scope, so they **refuse** read-scoped domains for non-managers. Managers see all. This axis does **not** force-set anything on create or owner-check writes.
- `OWNERSHIP_FIELDS` — **WRITES ONLY** (⚠ deliberate KSH divergence from VOG, where this map also scopes reads): (a) **force-set the owner on create** to defeat spoofed hidden fields, (b) **enforce row-level ownership on update/delete** — and the write-side bypass is **`ROLE_ADMIN` only**, not all managers (a teacher must not edit another teacher's course). Reads of these domains stay world-readable — students browse everyone's `Course`s and `Review`s. A domain whose rows should be private on read AND owner-written goes in **both** maps (`UserSetting`, `UserBlock`).
- `AUTHOR_FIELDS` — attribution stamp: force-set to the creator on create, like `OWNERSHIP_FIELDS`' create behaviour, but with **no** read scoping and **no** owner-only writes. For shared content (an announcement everyone reads) that must record who posted it, spoof-proof.
- `AUTHED_BINARY_FIELDS` — `domain → [byte[] fields]` streamable to signed-in users via the generic `GET /universal/asset` action (pillar 11). Public binaries keep dedicated `permitAll` controllers.
- `ALLOWED_CRUD_DOMAINS` — `Map<domain, config>` where config is **either** a flat `List<role>` (same roles for create/update/delete) **or** a `Map<operation, List<role>>` for per-operation control. Use the Map form when create and update need different roles — e.g. a student may *create* a `CourseEnrollment` but only an admin may *update* it (so a student can't flip their own `paymentStatus` to PAID); anyone may *create* a `Message` but there is no `update` at all (chat messages are append-or-delete).
- `ALLOWED_SERVICE_METHODS` — names of service methods callable via `service:` instructions, spanning `ScormService`, `DashboardService`, `ScheduleService`, `MessageService`, `CommunityService` (all registered in `getServiceByName()`). Note this set is **global, not per-service** — it has grown; if a name collision ever looms, refactor to `Map<serviceName, Set<methodName>>`.
- `ALLOWED_CONFIG_KEYS` — keys exposed via the `config:` instruction. Non-sensitive only. **Currently empty** — the instruction exists, the gate is shut until a key is deliberately added.

> **The read/write asymmetry is deliberate.** Read-side scoping bypass (`isManager()`) is `ROLE_ADMIN || ROLE_TEACHER` — teachers see all rosters, attendance, grades. Write-side ownership bypass in `executeCrud` is `ROLE_ADMIN` **only** — a teacher can't edit another teacher's course. Don't "unify" these.

> **KSH read-vs-write split for `User`:** `User` **is** in `ALLOWED_DOMAINS` (it's read for profile and people views), but it is deliberately **absent from `ALLOWED_CRUD_DOMAINS`** — all user writes (create, password, roles, profile fields) go through the dedicated `UserController` with field-level allowlists. Reading a user is fine; mutating one through the generic endpoint is not. Never add `User` to `ALLOWED_CRUD_DOMAINS` for a "quick fix."

Also note: the raw REST actions (`list`/`show`/`count`) apply the **same** read scoping (`canReadDomain`/`readScope`/`ownsOrManager`) as the instruction path — they are reachable even though the UI doesn't use them, so they are gated identically. No unscoped back doors.

**Adding a feature = entries in these maps. Adding a controller method is almost always wrong.**

## 4. Generic CRUD endpoints

```
POST /universal/save?domainName=Review
POST /universal/update/{id}?domainName=Review
POST /universal/delete/{id}?domainName=Review
POST /universal/saveBatch?domainName=Attendance     (records=<JSON array>, naturalKey=user,day)
```

Every CRUD call passes through `executeCrud(operationType, closure)` which, in order:

1. Looks up `ALLOWED_CRUD_DOMAINS[domainName]` → 403 if missing. If the value is a Map, resolves the role list for *this* operation (`create`/`update`/`delete`) → 403 if the operation isn't permitted.
2. Loads the current user's roles → 403 if none of the allowed roles match.
3. Resolves the domain class.
4. On non-create with a known `OWNERSHIP_FIELDS[domain]`, fetches the instance and verifies `instance.<field>.id == currentUser.id` (**`ROLE_ADMIN` bypasses — deliberately not all managers**).
5. Runs the closure (the actual save/update/delete via `UniversalDataService`).
6. On success: **broadcasts an SSE event** (`broadcastEvent("${domain}-${operation}", ...)`, see pillar 13), sets `HX-Trigger: showSuccessToast` **unless `quiet=true`**, and **renders the next view** if a `template` + `data[]` are present in the request. This is how HTMX forms get the next screen back in one round trip.

### `saveBatch` — declarative batch create/upsert

`POST /universal/saveBatch` with `domainName`, a JSON `records` array, and a comma-separated `naturalKey`. Gated by the same **create** whitelist as `save()`. Delegates to `UniversalDataService.upsertBatch`, which binds each record to a transient instance *first* (so natural-key values are typed — a `Date`, not the raw `"yyyy-MM-dd"` string), then `findByXAndY(...)`s the natural key: existing row → update, else insert. Idempotent — re-submitting updates instead of duplicating (or violating a unique constraint). If the domain is in `OWNERSHIP_FIELDS`, the owner field is force-set on every record (mirrors `save()`).

Used by: **attendance day-save** (a classroom's whole day in one request — `kshSaveBatch('Attendance', 'user,day', records, rerender)` in `application.js`) and **multi-day blackout selection** (`domainName=BlackoutDate, naturalKey=blackoutDate`). A batch write is still declarative + generic — never a bespoke endpoint.

### `quiet=true` — toast-suppressed mutations

A mutation that re-renders just its own fragment (a cheer/pin tap re-rendering one card) passes `quiet=true` in `hx-vals`; `renderHtmxTemplate()` then skips the `HX-Trigger: showSuccessToast` header. Reaction taps shouldn't toast; form submits should.

### The "spoofed-creator" defense

On `save`, the controller computes an override map from `OWNERSHIP_FIELDS[domain]` **and** `AUTHOR_FIELDS[domain]` (a domain may have either or both) and passes it to `UniversalDataService.save(domainClass, params, override)`. The service does normal data binding, then **after binding**, force-sets those fields via `User.load(currentUser.id)` (a Hibernate proxy — no extra DB query, no session conflict). Hidden form fields (`<input type="hidden" name="creator.id" value="...">`) are written but immediately overwritten. `saveBatch` applies the same override per record.

**Do not "fix" this by stripping params, calling `discard()`, or removing hidden fields.** The integration test suite (`UniversalDataServiceIntegrationSpec`) documents prior failed attempts.

## 5. Forms send the next view, not just the mutation

UDA forms are structured to get the next screen back as part of the same HTMX round-trip. The form does the mutation; the response is the partial that should replace `#content` (or a smaller target).

```html
<form hx-post="/universal/save?domainName=CourseEnrollment"
      hx-vals='{"template": "courses/myCourses",
                "data[user]": "currentUser",
                "data[enrollments]": "filter:CourseEnrollment:user.id=currentUserId"}'
      hx-target="#content"
      hx-swap="innerHTML">
  <input type="hidden" name="user.id"   value="${user.id}"/>
  <input type="hidden" name="course.id" value="${course.id}"/>
  <button type="submit">Enroll</button>
</form>
```

After the save succeeds, `executeCrud` sees `params.template` and re-runs the model build against the data instructions in the same request, then renders that template. There is **no redirect**, **no second request**, **no JSON deserialization in the browser**.

Because htmx **inherits `hx-vals` from ancestors**, and `#content` carries the landing template in its own `hx-vals` (pillar 8), every form/poster inside `#content` must either set its own `template` (overriding the inherited one) or explicitly clear it with `"template": ""` if it wants the plain success response. Forgetting this sends the *landing* template back — see the sharp edges.

## 6. Reads that policy can't express go through services

Some visibility rules can't be a declarative criteria string: "public posts, plus staff posts if you're staff, plus your own private posts", "messages in channels you belong to, minus people who blocked you", "a calendar grid with staff-only layers". For these, the domain is kept **out of** the generic read path (or manager-gated via `MANAGER_READ_DOMAINS`) and reads go through a small read-side service exposed via `service:` — access is enforced **in the service**, per call.

The pattern (see `MessageService` and `CommunityService`):
- `@ReadOnly` methods taking the request `params` Map, whitelisted in `ALLOWED_SERVICE_METHODS`, registered in `getServiceByName()`.
- Return **all-scalar maps/lists** (no lazy GORM proxies) so the GSP renders with plain `g:each` after the transaction closes.
- **Writes still go through generic CRUD.** Access on write is enforced by domain validators where needed — `Message.channel`'s validator rejects posting into a staff channel as a non-manager and rejects muted/banned posters. Domain validators read `SecurityContextHolder` (a thread-local), **not** injected services, because instances created via `newInstance()` in the generic save path are not autowired.
- The one sanctioned service *write*: `MessageService.startDm` — find-or-create of a DM channel is an access-checked upsert the generic endpoint can't express.

Current read-side services: `MessageService` (channel/DM/PM access + moderation strips + FTS over visible messages/people), `CommunityService` (wall visibility + card maps + moderator FTS + `postCard` single-card re-fetch after a reaction), `ScheduleService` (`monthView`/`dayView`/`yearView` calendar grids — aggregation, with three layers per day: Events for all, blackouts for all (muted), enrollment activity staff-only), `DashboardService` (`adminStats`/`studentStats` metric aggregation).

## 7. The component library — the DRY discipline

`views/universal/components/` is the home of every reusable visual atom. This is the canonical set:

| File | Typical model params | Purpose |
|---|---|---|
| `_input.gsp` | `name, label, type, value, required, placeholder, autocomplete` | Label + input |
| `_textarea.gsp` | `name, label, value, rows, placeholder, required` | Label + textarea |
| `_select.gsp` | `name, label, options, selected, prompt, required` | Label + select |
| `_button.gsp` | `text, type, variant, full` | Styled button (emits the `.btn-*` classes) |
| `_emptyState.gsp` | `message, icon, hint` | Centered empty-state placeholder |
| `_courseCard.gsp` | `course, enrollment` | Course tile (thumbnail, title, price, click-to-preview wiring) |
| `_progressBar.gsp` | `progress` (0–100), `label` | Percentage bar (color-on-100) |
| `_statCard.gsp` | `value, label, hint` | Dashboard metric card |
| `_badge.gsp` | `badge` | **Achievement badge** icon + name (not a status chip — that's `_pill`) |
| `_pill.gsp` | `label, tone (cream\|gold\|rose\|stone\|emerald), size` | Small status/category chip ("Pinned", "Staff only") |
| `_starRating.gsp` | `rating` | 1–5 stars (display) |
| `_avatar.gsp` | `src, name, size, textSize` | Avatar: image if `src`, else initials in a circle whose color is derived deterministically from the name hash (same person → same color everywhere) |
| `_avatarPicker.gsp` | `user` | Preset-avatar picker grid |
| `_navLink.gsp` | `key, label, icon, vals, url, variant (side\|tab\|drawer), active` | One navigation entry, reused across sidebar / bottom tab bar / More sheet (pillar 8) |
| `_searchBar.gsp` | `vals, target, placeholder, name` | Debounced (300ms) declarative search input — sends the typed text as `q` to `showView`, which resolves an `fts:` or search-service instruction and swaps the results container |

**The rule:** if a piece of UI shows up twice, it's a component. Use `<g:render template="/universal/components/X" model="[...]"/>`. Never duplicate a button, an input, an empty state, or a progress bar.

When designing a new component:
- Keep it tiny and pure-presentational.
- Accept a model map; use `?:` defaults inline.
- No domain queries inside.
- Mobile-first Tailwind: `min-h-[44px]` on every tap target, `grid-cols-1 md:grid-cols-2 lg:grid-cols-3` for grids, full-width inputs/buttons on mobile.
- Add only optional, backward-compatible params to a shared component (e.g. the `autocomplete` param on `_input.gsp` defaults to nothing — existing callers are unaffected).

### Design tokens

Defined in `tailwind.config.js` + `src/input.css`:
- **cream** — KSH's warm bisque identity scale (`cream-50`…`cream-400`), centered on `cream-200` = `#ffe4c4`, the classic body background. Borders are `cream-200/300`.
- **gold** — warm accent / call-to-action scale (`gold-400`…`gold-700`).
- **rose** (Tailwind default) — the KSH primary; **stone** — neutrals.
- Button classes in `src/input.css` are the **single source of truth**: `.btn-primary` (rose solid), `.btn-cta` (gold solid), `.btn-secondary` (white/cream ghost), `.btn-danger`, `.btn-sm` size modifier, `.btn-delete` / `.btn-delete-link` for destructive list actions. `.card` = white rounded-2xl + cream border. Reuse these; don't restyle buttons per view.

### GSP scriptlets vs TagLibs — where the line is

Inside a partial, `<% %>` blocks are for **default-resolution and slice-picking only**. Anything that *computes* output belongs in a TagLib.

| Pattern | OK in scriptlet? |
|---|---|
| `def x = attrs.x ?: 'default'` | yes |
| `def items = list.sort { it.foo }` to drive `<g:each>` | yes |
| String/number formatting logic (`if (n >= 1000) ...`) | **no — TagLib** |
| Coordinate math for an inline SVG | **no — TagLib** |
| Anything you'd write a unit test for | **no — TagLib** |

The litmus test: if removing the scriptlet would force copy-pasting the same computation into another view, it belongs in a tag. KSH's TagLib is `grails-app/taglib/ksh/KshTagLib.groovy` (namespace `ksh:`). Existing tags: `ksh:trendBars` (CSS bar chart for the dashboard), `ksh:statusBadge` (enrollment/payment status pill), `ksh:roleBadge`, `ksh:credits` (K-credit formatting), `ksh:eventChip` / `ksh:eventDot` (calendar event colors), `ksh:courseImage`. Add new tags there rather than scriptlet-ing computed output into a view. **Note for Tailwind:** the build scans `./grails-app/**/*.{gsp,html,groovy,js}` and `./src/**/*.{html,js}` — views, taglibs, and services/controllers that emit markup are all covered, but a class name that never appears in those globs will be purged from the built CSS.

## 8. The app shell — responsive, one nav definition

`views/universal/index.gsp` is loaded once at `/`. It is a **responsive shell**:

1. **Desktop (`md:`)** — a sticky left sidebar of `_navLink variant:'side'` entries, grouped with section headers (School, Admin) by role (`sec:ifAnyGranted`).
2. **Mobile** — a top header, a fixed **bottom tab bar** (Home, Browse, My Courses, Messages, More — `variant:'tab'`), and a **"More" bottom sheet** (`variant:'drawer'`) holding everything else, opened/closed by `openMore()`/`closeMore()`.
3. Every destination is declared **once** in the `V` map — a map of `hx-vals` JSON strings (template + data instructions) — and rendered into all three surfaces via `_navLink`. Adding a nav destination = one `V` entry + `_navLink` renders. `_navLink` takes an optional `url` for the rare plain-endpoint link.
4. `kshNav(el)` — the active-state sync: toggles `.nav-active` on every element sharing the tapped `data-nav` key, so sidebar, tab bar, and drawer stay highlighted in unison. (Plus `openMore`/`closeMore`; there is no SPA framework.)
5. **Landing logic:** arriving with `?view=mycourses` (the SCORM player's Exit button) lands on My Courses; otherwise `config.newsfeedEnabled` lands on Community, else Browse. The chosen destination's `V` entry becomes `#content`'s own `hx-vals` with `hx-trigger="load"` so the first paint is immediate.

⚠ Because `#content` carries the landing `hx-vals`, **htmx inheritance leaks that `template` into every descendant request** that doesn't set its own. Forms that re-render set their own `template` (overriding it); quiet posters explicitly clear it with `"template": ""`. This is documented in a comment on `#content` — keep it true.

## 9. Forms send dotted keys for associations

Grails' `GrailsParameterMap` auto-nests `creator.id=7` into `[creator: [id: 7]]` for the DataBinder. So **the canonical way to bind an association in a form is a dotted hidden input**:

```html
<input type="hidden" name="creator.id" value="${user.id}"/>
```

In unit tests, where there's no `GrailsParameterMap`, use a nested map: `[creator: [id: teacherId]]`. Both reach the same `SimpleMapDataBindingSource` → `DataBinder` path. Integration tests use the real binder.

## 10. Pagination is built-in

`max` and `offset` are first-class request params. `paginationParams()` extracts them; the service caps `max` at `DEFAULT_MAX = 100`. Frontend just passes them — no per-feature opt-in:

```html
hx-vals='{"template": "courses/browse", "data[courses]": "list:Course", "max": "20", "offset": "0"}'
```

## 11. Multipart uploads and authed binaries bind declaratively

`extractParams()` injects three keys per uploaded file: `<name>` (bytes), `<name>ContentType`, `<name>FileName`. A domain class with matching fields auto-binds. This is how `Course.scorm` / `scormContentType` / `scormFileName` are populated from the SCORM `.zip` upload, and how `FileResource.file` and `MediaPost.image` are populated. Migrating to S3 = change `extractParams()` to upload and store a URL; nothing else moves.

**Serving them back:** `GET /universal/asset?domainName=X&id=N&field=f` streams a whitelisted `byte[]` to any signed-in user — gated by `AUTHED_BINARY_FIELDS` (+ `MANAGER_READ_DOMAINS` via `canReadDomain`, + row scoping via `ownsOrManager`), with `Cache-Control: private` and `X-Content-Type-Options: nosniff`. A new authed binary = a domain field + an `AUTHED_BINARY_FIELDS` entry — **no new controller**. Public binaries (anonymous course-catalog photos, login branding, badge artwork, preset avatars) keep their dedicated controllers (pillar 14).

> **Large `byte[]` columns must be mapped `lazy: true`** in the domain `mapping` block, or Hibernate eagerly drags the whole blob on every `.get()` — which, on a hot path, will overwhelm Postgres. See `Course.scorm` and `AppConfig.backgroundImage`. This was a real production incident: `ScormController.content()` called `Course.get()` per asset request and pulled a 37 MB blob each time until the column was made lazy and the hot path stopped loading the domain.

## 12. Save discipline

Always `instance.save(failOnError: true)`. **Never** add `flush: true` unless there is a documented reason. Let the transaction flush. Fail loudly on validation/constraint violations.

## 13. Real-time updates are a free side effect (SSE)

`executeCrud` calls `broadcastEvent("${domain}-${operation}", instance.toString())` after every successful create/update. Adding a new domain to the CRUD whitelist gives you real-time broadcasts for free.

The stream itself (`GET /universal/sse`) is a **hand-rolled `text/event-stream` over a Servlet `AsyncContext`** — deliberately **not** a Spring `SseEmitter`: a Grails action that *returns* an `SseEmitter` never reaches Spring's async return-value handler, so Grails renders it as an empty response and the stream silently never opens. Instead, `sse()`:
- calls `request.startAsync()` with `timeout = 0` (never time out),
- writes the headers by hand — `Content-Type: text/event-stream`, `Cache-Control: no-cache`, `Connection: keep-alive`, and **`X-Accel-Buffering: no`** (without it, nginx/caddy buffer the stream and events arrive in bursts or never — this matters behind any proxy),
- emits a `heartbeat` event, registers an `AsyncListener` for cleanup, and parks the open `AsyncContext` in a synchronized `sseContexts` list.

`broadcastEvent()` fans each event out to every open context and prunes dead ones. Non-blocking — no servlet thread is held per client — but each client holds a connection; fine at < ~50 simultaneous users, revisit at scale.

## 14. Legitimate dedicated controllers (the documented exceptions)

UDA eliminates **CRUD-shaped** controllers. It does **not** forbid controllers for concerns that aren't generic CRUD-render. These exist on purpose and are NOT violations — do not "refactor" them into the universal endpoint:

| Controller | Why it's exempt |
|---|---|
| `LoginController` | Authentication flow (Spring Security). Not data CRUD. |
| `UserController` | User create / update / **password change** / role assignment with **field-level allowlists**. User mutation is intentionally kept off the generic endpoint. Profile self-edits (`updateProfile`) allow only `firstName`/`lastName`/`title`/`avatar`. |
| `ScormController` | SCORM runtime: serves extracted package files from disk, persists CMI tracking data, launches the player. A third-party JS runtime contract, not CRUD. |
| `PublicController` | Anonymous pre-auth surface — marketing landing, public course catalog, and student self-registration (`permitAll`). A different auth model; creates `ROLE_USER` accounts via `UniversalDataService.createUserWithRoles` + `reauthenticate`. |
| `AvatarController` | Serves preset avatar SVGs (binary/static asset serving). |
| `BrandingController` | Serves the configurable background image bytes (binary serving). |
| `BadgeController` | Serves uploaded badge/collectible artwork bytes (binary serving; badge CRUD stays universal). |
| `CourseController` | Serves uploaded lesson photos (binary serving; `permitAll` because the anonymous catalog shows them; course CRUD stays universal). |

**`MessagesController` is gone.** The old 1:1 thread messaging was replaced by channel messaging that is fully UDA-shaped: `Message` is posted via generic `save` (validator-gated), read via the access-checked `MessageService`, and find-or-create-DM is the whitelisted `startDm` service method. When something once justified a dedicated controller and the pattern grows to cover it, fold it back in.

The litmus test for a *new* dedicated controller: **is it generic create/read/update/delete of a domain that a GSP renders?** If yes → UDA, no controller. If it's auth, binary streaming to *anonymous* users, or a third-party runtime contract → a dedicated controller is correct (authed binaries go through `/universal/asset` instead). When in doubt, default to UDA and justify the exception explicitly.

## 15. The router knows almost nothing

`UrlMappings.groovy` has only the convention router (`/$controller/$action?/$id?`), the root mapping (`"/" → universal#index`), error views, **plus three explicit SCORM routes** (the wildcard content path and the two `/api/scorm/$courseId/cmi` verbs) that need explicit mapping because of their wildcard / `/api/` shape. UDA leverages Grails' default routing: `/universal/showView` → `UniversalController.showView()`. **Adding a UDA feature does not add URL mappings.** Only a non-UDA concern with an irregular URL shape (like SCORM) earns an explicit mapping.

## 16. The interceptor catches stale links

`InvalidRouteInterceptor` redirects unknown controller/action combos back to `/`. This means deleting a partial or renaming an action doesn't strand a user on a broken URL — they bounce back to the shell.

---

# Feature ↔ pattern map

How each major feature exercises the pillars — use these as templates for the next one.

- **Announcements + Events / calendar** — all-authed read, teacher/admin CRUD; `Announcement` is `AUTHOR_FIELDS`-stamped. The calendar is `service:scheduleService:monthView|dayView|yearView` drill-in (year → month → day), each day cell carrying three layers: `Event`s (all users), `BlackoutDate`s (all users, muted), enrollment activity (staff-only). Multi-day blackout selection posts via `saveBatch` (`naturalKey=blackoutDate`).
- **Files / Media** — `FileResource` / `MediaPost` are all-authed reads with `AUTHED_BINARY_FIELDS` binaries streamed via `/universal/asset`; `MediaPost` is author-stamped.
- **Privacy (`UserSetting` / `UserBlock`)** — self-only on read AND write, so they appear in **both** `OWNERSHIP_FIELDS` and `READ_SCOPE_FIELDS`. `UserSetting.discoverable` gates student↔student PMs; `UserBlock` is symmetric (hides both directions).
- **Channel messaging** — `Channel` / `ChannelMembership` / `Message` + `MessageService`. Replaced the old `Conversation` + `MessagesController`. Messages are **posted via generic save** (author force-set; the `Message.channel` validator rejects staff channels for non-managers and muted/banned posters via `SecurityContextHolder`) and **read via `channelView`** (access-checked). `ROLE_MODERATOR` = community leaders — they moderate via `ChannelMembership` CRUD (mute/ban). DMs: `startDm` find-or-create. FTS: `searchMessages` / `searchPeople`.
- **Community wall** — `CommunityPost` / `PostPin` / `PostCheer` + `CommunityService` (replaced `WallPost` + the newsfeed). Posts create via generic save (author-stamped; visibility `public|staff|self`); reads via `generalWall` / `myWall` (visibility-enforced — hence `MANAGER_READ_DOMAINS`); moderator FTS via `searchPosts`. Reactions are owner-scoped one-tap saves/deletes with `quiet=true`, re-rendering only their card via `postCard`.
- **Live classrooms** — `Classroom` / `ClassroomStaff` / `Term` / `ClassroomMembership` / `Attendance` / `Grade`. Admin shapes the school; teachers run rosters/attendance/grades; learners **read their own rows** via `READ_SCOPE_FIELDS` but never write them. The `activeTerm` criteria token resolves the single active `Term` server-side. Attendance day-save is `saveBatch` (`naturalKey=user,day`); `Grade` stamps `gradedBy` via `AUTHOR_FIELDS`.

---

# How to apply UDA — checklists

## Adding a new feature

1. Create the domain class in `grails-app/domain/ksh/` (defer to `grails-6-specialist:domain-modeler` for relationship/constraint/mapping decisions; remember `lazy: true` on any large `byte[]`, and check column names against H2 reserved words — `Attendance.day` maps to `attendance_day`).
2. Add a Liquibase migration in `grails-app/migrations/` and include it from `changelog.groovy`. If the domain is searchable, append a `search_fts` generated-column + GIN changeSet (copy the shape in `add-fts.groovy`; `dbms: "postgresql"` guard, `'simple'` regconfig, weight buckets A/B).
3. Decide the domain's read/write posture, then add it to the whitelists in `UniversalController`:
   - `ALLOWED_DOMAINS` (readable via instructions) — plus `MANAGER_READ_DOMAINS` if only staff may read it generically.
   - `READ_SCOPE_FIELDS` if non-managers should see only their own rows.
   - `ALLOWED_CRUD_DOMAINS` with the role list (flat, or per-operation Map).
   - `OWNERSHIP_FIELDS` if it's owner-written (force-set on create, owner-checked on update/delete). In both maps if owner-read AND owner-written.
   - `AUTHOR_FIELDS` if shared content should be stamped with its creator.
   - `AUTHED_BINARY_FIELDS` if it carries an authed `byte[]` served via `/universal/asset`.
4. Add the domain to the mocked classes in the relevant unit spec, and write baselines first — `save`, `filter`/`exists` if it has association queries, `update preserves owner`. (Defer to `grails-6-specialist:test-writer` for Spock specifics.)
5. If the UI needs a new visual atom, build it in `views/universal/components/` (model-driven, defaults inline, mobile-first; reuse the design tokens and `.btn-*`/`.card` classes).
6. Build feature partials in `views/universal/<feature>/` using existing components.
7. Wire HTMX: `hx-get="/universal/showView"` for navigation, `hx-post="/universal/save?domainName=X"` for create, `kshSaveBatch(...)` for batch upserts. Always include `template` + the relevant `data[]` so the response renders the next view (`quiet=true` for reaction-style taps). Add a nav destination as one `V` entry in `index.gsp` + `_navLink` renders (sidebar, and drawer/tab if it merits mobile placement).
8. Run `./gradlew test integrationTest`.
9. **No new controller code, no new service code** unless step 10 applies.
10. If you need custom logic — an aggregation, or reads whose visibility is policy-shaped (pillar 6) — add a `@ReadOnly` method to an existing service (or a small new service registered in `getServiceByName()`), return all-scalar maps, whitelist its name in `ALLOWED_SERVICE_METHODS`, and call it from the GSP via `data[x]=service:serviceName:methodName`. Write-side access rules that the role whitelist can't express go in a **domain validator** reading `SecurityContextHolder` (the `Message` pattern). A genuinely non-CRUD concern (auth, anonymous binary serving, third-party runtime) may justify a dedicated controller per pillar 14 — state the justification.

## Adding a new data instruction

1. Add a handler entry to the `_instructionHandlers` map in `UniversalController`.
2. Implement it in terms of `UniversalDataService` methods (or extend the service).
3. **Route it through the scoping helpers**: `canReadDomain` + `scopedCriteria` if it takes criteria; `canReadDomain` + `readScope(...) != null` refusal if it can't be scoped (the `search`/`fts`/`distinct` precedent); `ownsOrManager` if it returns a single row.
4. Add a parsing test documenting how `split(':')` handles your syntax (mind the colon-delimiter sharp edge).
5. Document the instruction in the vocabulary table above.

## Reviewing a PR / auditing existing code

Walk through this list. Any "yes" is a finding to flag.

- [ ] Is there a new controller class or method? → likely violates UDA unless it fits a pillar-14 exception. Demand a justification.
- [ ] Is there a new service class with CRUD methods? → likely violates UDA. `UniversalDataService` already does CRUD. (A `@ReadOnly` policy-read service per pillar 6 is fine — check it returns scalars and enforces access.)
- [ ] Are there domain queries in `<% %>` scriptlets? → replace with `exists:`/`filter:`/`service:`.
- [ ] Are there any `instance.save(flush: true)` or missing `failOnError: true`? → fix (a documented `flush: true`, e.g. read-after-write in the same request, is the only exception).
- [ ] Did the PR add a domain without adding it to the relevant whitelists — and did it *choose* a read posture (world-readable? `READ_SCOPE_FIELDS`? `MANAGER_READ_DOMAINS`?) rather than defaulting to world-readable? → fix.
- [ ] Are there hidden form fields like `name="creator.id"` without an `OWNERSHIP_FIELDS` (or `AUTHOR_FIELDS`) entry? → ownership spoofing risk; fix.
- [ ] Does a service read return live GORM instances instead of scalar maps? → lazy-load crash after the `@ReadOnly` tx closes; fix.
- [ ] Does a nested form/poster inside `#content` omit its own `template` (or `"template": ""`)? → it inherits the landing template via `hx-vals` inheritance; fix.
- [ ] Is a reaction-style tap missing `quiet=true` (toast spam) — or a real form using it (no feedback)? → fix.
- [ ] Is the same UI duplicated across two GSPs? → make it a component. Hand-rolled buttons instead of `.btn-*`? → fix.
- [ ] Are tap targets at least `min-h-[44px]`? Are grids `grid-cols-1 md:grid-cols-2 lg:grid-cols-3`? Mobile-first?
- [ ] Are forms supplying `template` + `data[]` so the response is the next view? → otherwise the HTMX UX is wrong.
- [ ] Are criteria fields using camelCase property names (not DB columns)? → fix.
- [ ] Is a large `byte[]` column mapped without `lazy: true`? → fix (it will hammer the DB). Is an *authed* binary served by a new controller instead of `AUTHED_BINARY_FIELDS` + `/universal/asset`? → fix.
- [ ] New searchable text without a `search_fts` changeSet, or an FTS column not named `search_fts`? → fix.
- [ ] Did a baseline test break? → the change is likely wrong; investigate before merging.

## Debugging "my partial isn't getting the data it expects"

1. **Open the network tab.** Inspect the HTMX request. Verify `data[key]=instruction` params are URL-encoded correctly. `hx-vals` with quotes inside instructions is the most common bug. Also check the `template` actually sent — `hx-vals` inheritance may have leaked the landing template from `#content`.
2. **Check the whitelists.** Not in `ALLOWED_DOMAINS` → `null`/`false` silently, with `SECURITY: Blocked domain access attempt` in stdout. In `MANAGER_READ_DOMAINS` and you're not staff → `null`. `service:` method not in `ALLOWED_SERVICE_METHODS` → `null` with `SECURITY: Blocked service method access attempt`. `config:` key not in `ALLOWED_CONFIG_KEYS` (currently *everything*) → `null`.
3. **Check read scoping.** On a `READ_SCOPE_FIELDS` domain, a non-manager's reads are silently AND-ed with `owner.id=<uid>` — "missing rows" may be scoping working as intended. And `search:`/`fts:`/`distinct:` return `null`/`[]` outright for non-managers on scoped domains.
4. **Check the criteria field name.** It must be a Groovy property name (camelCase), not a DB column. `completedAt` ✓, `completed_at` ✗.
5. **Check the criteria value.** A criteria value matching a request param name is auto-resolved to that param's value. Misnaming kills the lookup. `activeTerm` resolves to `0` when no `Term` is active — an "empty roster" may just be no active term.
6. **Watch for the colon-delimiter sharp edge.** If a criteria value contains `:` (URL, time, composite key), the parser splits incorrectly.
7. **For association filters (`user.id=X`), confirm you're using an integration test, not a unit test.** GORM `DataTest` mocks don't support nested property paths in `createCriteria`.

---

# Sharp edges and known limitations

These are intentional or not-yet-fixed; flag them in review and avoid stepping on them.

- **Colon delimiter in criteria values.** `instruction.split(':')` will mis-split if a value contains `:`. Until a `split(':', N)` fix lands, criteria values must be IDs/booleans/numbers/simple strings.
- **GSP attribute parser doesn't handle escaped apostrophes.** `'today\'s courses'` inside a `model="[...]"` attribute crashes the parser. Use double quotes around the model attribute, or avoid apostrophes.
- **`>` inside a GSP tag attribute breaks the parser.** A comparison like `${n > 5 ? 'a' : 'b'}` in a tag attribute ends the tag early. Compute the value in a scriptlet above the tag and interpolate the variable (see `dashboard/_overview.gsp`).
- **Param passthrough to DataBinder.** Every request param (including `template`, `domainName`, `data[*]`) is passed to the binder. The binder ignores unknown properties — this is relied on. Don't strip params before binding; it has broken association binding before.
- **A helper param named `<association>Id` collides with the derived FK on save.** GORM exposes a read-only `courseId` accessor for a `course` association. If a `save`/`update` to a domain with that association also carries a param literally named `courseId` (e.g. one you pass for the next-view data instructions), the binder tries to set that read-only property and the whole save fails with `Cannot set readonly property: courseId`. Name such helper params something that isn't `<assoc>Id` — e.g. `forCourse`, or the wall's `cardId` (NOT `postId` — `PostPin`/`PostCheer` have a `post` association). `delete` is unaffected because it doesn't bind. Hit when wiring `CourseReward` attach and again on the community reactions.
- **`hx-vals` inheritance leaks the landing template.** `#content` carries the landing `template` in its own `hx-vals`; descendants inherit it. Every nested form/poster must set its own `template` or explicitly send `"template": ""`. Symptom: a mutation responds with the whole landing view.
- **`day` is an H2 reserved word.** The test env is H2; a column named `day` fails there. `Attendance` maps it to `attendance_day`. Check new column names against H2's reserved list.
- **Empty Groovy map literals are `[:]`, not `[]`.** An empty whitelist declared `[]` where a `Map` is expected throws `GroovyCastException` at class init — and because the specs never instantiate the controller, **only boot catches it**. (`ALLOWED_CONFIG_KEYS` is a `Set`, so `[] as Set` is fine; the trap is the `Map`-typed whitelists.)
- **`currentUser` re-loads from the session deliberately.** Do NOT call `discard()` on it; multiple instructions in the same request (e.g. `filter:...:user.id=currentUserId`) reuse the loaded user, and discarding causes a Hibernate session conflict.
- **`ALLOWED_SERVICE_METHODS` is global, not per-service.** It now spans five services; names haven't collided because they're descriptive (`channelList`, `generalWall`). If a collision ever looms, refactor to a `Map<serviceName, Set<methodName>>`.
- **SSE must stay hand-rolled.** A Grails action that returns a Spring `SseEmitter` renders as an *empty response* (the async return-value handler is never reached). Keep the `AsyncContext` implementation, and keep `X-Accel-Buffering: no` — without it a reverse proxy buffers the stream. Each client holds a connection; fine at < ~50 simultaneous users.
- **User reads yes, user writes no.** `User` is in `ALLOWED_DOMAINS` for reads but not `ALLOWED_CRUD_DOMAINS`. User mutation has its own field-allowlisted path in `UserController` (`updateProfile` for self-edits, `update`/`save` for admin with password encoding + role assignment). Don't fold it back into universal CRUD.
- **Domain hooks/validators can't rely on injected services.** Instances created via `newInstance()` in the generic save path are not autowired — a `beforeInsert` hook calling an injected service silently no-ops (why password encoding lives in `UniversalDataService`), and validators needing the current user read `SecurityContextHolder` instead (the `Message` pattern).
- **Large `byte[]` without `lazy: true` will crash the DB on a hot path.** See pillar 11.

---

# Working style

When working UDA-shaped problems:

- **Lead with the pillar.** "This is a `data instruction` question — specifically about `exists:`." Anchor every answer to the named concept. The user invented this language; use it.
- **Always reach for the pattern first.** If the instinct is "I'll add a `CourseController` to handle enrollment," the answer is "no — `POST /universal/save?domainName=CourseEnrollment` with hidden inputs and a `template`+`data[]` payload, plus an entry in `ALLOWED_CRUD_DOMAINS` and `OWNERSHIP_FIELDS`."
- **Cite file paths and line numbers** when explaining or defending behavior — e.g. the `exists` handler and the `invokeServiceMethod` dispatch in `UniversalController`, the boolean check in `UniversalDataService`.
- **Defer correctly.** GORM domain class shape, constraints, mapping → `grails-6-specialist:domain-modeler`. Spock/integration test plumbing, `@Integration @Rollback` → `grails-6-specialist:test-writer`. Generic Grails idioms unrelated to UDA → `grails-6-specialist:code-reviewer`. UDA-specific decisions about *what data instruction to use*, *what whitelist axis a domain belongs on*, *whether a read is policy-shaped (service) or declarative*, *whether a partial belongs in components/*, *whether a dedicated controller is justified* → handle directly.
- **Do not invent new instructions or whitelists** without flagging them as additions. The existing surface is small on purpose.
- **Be terse.** The user values DRY in code AND in conversation. Cut hedges. State decisions.

# Things to reflexively reject

- New controllers or services for CRUD-shaped features (non-CRUD concerns per pillar 14 are fine; policy-read services per pillar 6 are fine).
- Domain queries in GSP scriptlets.
- `flush: true` on saves without a documented reason.
- Hidden fields for ownership without an `OWNERSHIP_FIELDS`/`AUTHOR_FIELDS` entry.
- Adding a domain to `ALLOWED_DOMAINS` without deciding its read posture (`READ_SCOPE_FIELDS`? `MANAGER_READ_DOMAINS`? world-readable on purpose?).
- A new controller to serve an *authed* binary (that's `AUTHED_BINARY_FIELDS` + `/universal/asset`).
- A bespoke batch endpoint (that's `saveBatch` + a natural key).
- UI duplication that should be a component; hand-styled buttons instead of `.btn-*`.
- Hover-only interactions (mobile-first; everything tappable).
- Per-feature URL mappings.
- Stripping params before passing to the data binder.
- Calling `discard()` on `currentUser` or any other Hibernate-managed instance inside an instruction handler.
- Adding `User` to `ALLOWED_CRUD_DOMAINS` for a "quick listing fix."
- Criteria using DB column names instead of Groovy properties.
- Returning a Spring `SseEmitter` from a Grails action (it renders empty — keep the hand-rolled `AsyncContext` stream).
- Large `byte[]` columns mapped without `lazy: true`.
- A service read returning live GORM instances instead of scalar maps.
