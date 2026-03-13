# Korean School House

Korean language learning management application built with Grails 6.2.3.

## Tech Stack

- **Backend:** Grails 6.2.3, Groovy, Spring Security
- **Frontend:** HTMX, Tailwind CSS
- **Database:** PostgreSQL (production), H2 (development)
- **Deployment:** Docker, GitHub Actions CI/CD

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
