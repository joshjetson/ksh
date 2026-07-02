# Korean School House

Korean language learning management application built with Grails 6.2.3.

## Tech Stack

- **Backend:** Grails 6.2.3, Groovy, Spring Security
- **Frontend:** HTMX, Tailwind CSS
- **Database:** PostgreSQL (production), H2 (development)
- **Search:** PostgreSQL full-text search (generated `search_fts` tsvector columns + GIN indexes)
- **Real-time:** Server-Sent Events (hand-rolled `text/event-stream` over Servlet async)
- **Deployment:** Docker, GitHub Actions CI/CD

## Architecture

The app is built on the **Universal Declarative Architecture (UDA)** — one controller, one data service, declarative data instructions in HTMX params. See [`docs/uda.md`](docs/uda.md) for the canonical reference (instruction vocabulary, whitelists, patterns, and sharp edges).

## Development

### Prerequisites

- JDK 11
- Node.js (for Tailwind CSS)

### Setup

```bash
npm install
npm run build:css
./gradlew bootRun
```

### Default Users (Development)

| Username | Password | Role       |
|----------|----------|------------|
| admin    | admin123 | ROLE_ADMIN |
| user     | user123  | ROLE_USER  |

## Production

### Docker

```bash
docker-compose up -d
```

### Environment Variables

- `DATABASE_URL` - PostgreSQL JDBC URL
- `DATABASE_USERNAME` - Database username
- `DATABASE_PASSWORD` - Database password
