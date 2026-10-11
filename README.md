# MediAuth — Insurance Pre-Authorization Platform

A full-stack portfolio project that demonstrates AI-assisted medical insurance claim pre-authorization. Patients submit claims, an LLM (Groq `openai/gpt-oss-20b` by default) reviews them against policy rules, and human reviewers handle edge cases.

**Live demo stack:** Render (backend) + Vercel (frontend) + Neon (PostgreSQL)

---

## Architecture

```
┌─────────────────────┐     JWT / REST      ┌──────────────────────────┐
│  React Frontend     │ ◄──────────────────► │  Spring Boot Backend     │
│  (Vercel)           │                      │  (Render)                │
└─────────────────────┘                      │                          │
                                             │  ┌────────────────────┐  │
                                             │  │  ClaimService      │  │
                                             │  │  AuthService       │  │
                                             │  │  GroqDecision      │  │
                                             │  │  Service           │  │
                                             │  └────────┬───────────┘  │
                                             └───────────┼──────────────┘
                                                         │
                              ┌──────────────────────────┼──────────────┐
                              │  PostgreSQL (Neon)        │              │
                              │  patients, policies,      │              │
                              │  claims, claim_documents, │              │
                              │  claim_history            │              │
                              └──────────────────────────┘
                                                         │
                                             ┌───────────▼──────────────┐
                                             │  Groq API                │
                                             │  openai/gpt-oss-20b      │
                                             └──────────────────────────┘
```

### Key flows

1. **Claim submission** → saved to DB → `GroqDecisionService` sends policy + claim context to Groq → parses `{decision, rationale, confidence}` JSON → persists on `Claim` entity → status becomes `AI_REVIEWED` or `NEEDS_HUMAN_REVIEW`
2. **Reviewer override** → `PATCH /api/claims/{id}/review` (REVIEWER role only) → recorded in `ClaimHistory`
3. **Auth** → BCrypt-hashed passwords, 24-hour JWT, `ROLE_PATIENT` / `ROLE_REVIEWER` spring authorities

---

## Project Structure

```
MediAuth/ (GitHub repository root)
├── backend/
│   ├── pom.xml
│   └── src/main/java/com/mediauth/
│       ├── MediAuthApplication.java
│       ├── config/          DataSeeder.java
│       ├── controller/      AuthController.java, ClaimController.java
│       ├── dto/             *Request / *Response POJOs
│       ├── entity/          Patient, Policy, Claim, ClaimDocument, ClaimHistory, enums
│       ├── exception/       GlobalExceptionHandler.java
│       ├── repository/      *Repository interfaces
│       ├── security/        JwtTokenProvider, JwtAuthFilter, SecurityConfig, UserDetailsServiceImpl
│       └── service/         AuthService, ClaimService, GroqDecisionService
└── frontend/
    ├── package.json
    └── src/
        ├── App.js
        ├── AuthContext.js
        ├── api.js
        ├── index.css
        └── pages/
            ├── AuthPage.js        Login + Register
            ├── DashboardPage.js   Patient claim list + detail view
            ├── SubmitClaimPage.js Claim submission form
            └── ReviewerPage.js    Reviewer queue + review form
```

---

## Environment Variables

### Backend (set in Render or the ignored project-root `.env.local` for local development)

| Variable | Description | Example |
|---|---|---|
| `DATABASE_URL` | Neon PostgreSQL JDBC URL | `jdbc:postgresql://ep-xxx.us-east-2.aws.neon.tech/neondb?sslmode=require` |
| `DATABASE_USERNAME` | Database role/user | `neondb_owner` |
| `DATABASE_PASSWORD` | Database role password | Set in your database provider |
| `GROQ_API_KEY` | Groq API key (free tier available at console.groq.com) | `gsk_xxx` |
| `GROQ_MODEL` | Groq chat model (defaults to `openai/gpt-oss-20b`) | `openai/gpt-oss-20b` |
| `JWT_SECRET` | Random 32+ char secret for signing JWTs | `my-super-secret-key-for-jwt-signing-32chars` |
| `PORT` | Server port (auto-set by Render) | `8080` |

### Frontend (set in Vercel or `.env`)

| Variable | Description | Example |
|---|---|---|
| `REACT_APP_API_URL` | Backend base URL | `https://mediauth-api.onrender.com` |

---

## Local Development

### Prerequisites
- Java 21+
- Maven 3.8+
- Node.js 18+
- A PostgreSQL database (Neon free tier or local)
- A Groq API key (free at [console.groq.com](https://console.groq.com))

### Backend

```bash
cd backend

# For local development, set DATABASE_PASSWORD and GROQ_API_KEY in
# the workspace-root .env.local. The backend imports this file at startup.
# A persistent local JWT_SECRET is stored there as well.

mvn spring-boot:run
# Server starts on http://localhost:8080
```

For local development, copy `.env.local.example` to `.env.local` in the repository root and set the database password, Groq API key, and a unique JWT secret. Never commit `.env.local`.

On first startup, demo data is seeded automatically:
- **Patient**: `alice@example.com` / `password123` (Policy: POL-HEALTH-001)
- **Patient**: `bob@example.com` / `password123` (Policy: POL-HOSP-002)
- **Reviewer**: `reviewer@example.com` / `reviewer123`

### Frontend

```bash
cd frontend
npm install
# For local dev pointing to local backend (proxy already configured in package.json)
npm start
# App opens at http://localhost:3000
```

For production frontend build pointing to Render:
```bash
REACT_APP_API_URL=https://your-backend.onrender.com npm run build
```

---

## API Reference

### Auth

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/api/auth/register` | Public | Register (role: PATIENT \| REVIEWER) |
| POST | `/api/auth/login` | Public | Login → returns JWT |

### Claims

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/api/claims` | PATIENT | Submit a claim (triggers AI review) |
| GET | `/api/claims` | Any | List claims (patients see own; reviewers see all) |
| GET | `/api/claims/{id}` | Any | Claim detail with documents + history |
| PATCH | `/api/claims/{id}/review` | REVIEWER | Override AI decision |
| POST | `/api/claims/{id}/documents` | Any | Attach a document URL |

**Query params for GET /api/claims:** `page`, `size`, `status`

### Example: Submit a claim
```bash
curl -X POST http://localhost:8080/api/claims \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "claimType": "SURGERY",
    "diagnosis": "Acute appendicitis (K35.8)",
    "procedure": "Laparoscopic appendectomy",
    "estimatedAmount": 18000
  }'
```

---

## Deployment

Push the entire `MediAuth` project directory to one GitHub repository. Its root contains both `backend/` and `frontend/`; do not push the parent workspace directory. The `.gitignore` excludes local environment files, dependencies, and generated build output.

### Neon (Database)
1. Create a free project at [neon.tech](https://neon.tech)
2. Copy the connection string — use the **JDBC** format:  
   `jdbc:postgresql://ep-xxx.us-east-2.aws.neon.tech/neondb?sslmode=require`

### Render (Backend)
1. Push code to GitHub
2. New Web Service → connect repo → set **Root Directory** to `backend`
3. **Build command:** `mvn clean package -DskipTests`
4. **Start command:** `java -jar target/mediauth-backend-0.0.1-SNAPSHOT.jar`
5. Add environment variables: `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`, `GROQ_API_KEY`, and a unique `JWT_SECRET`

### Vercel (Frontend)
1. New Project → connect the same repo → set **Root Directory** to `frontend`
2. Framework: Create React App
3. Add environment variable: `REACT_APP_API_URL=https://your-render-service.onrender.com`
4. Redeploy after setting the variable; Create React App embeds it at build time.

---

## Tech Stack

| Layer | Technology |
|-------|------------|
| Language | Java 21 |
| Framework | Spring Boot 3.2 |
| ORM | Spring Data JPA + Hibernate |
| Database | PostgreSQL (Neon) |
| Auth | Spring Security + JWT (jjwt 0.12) |
| AI | Groq API — openai/gpt-oss-20b by default (`GROQ_MODEL` configurable) |
| HTTP Client | Spring WebFlux (WebClient) |
| Frontend | React 18 + JavaScript |
| HTTP | Axios |
| Deployment | Render + Vercel + Neon |

---

## Claim Status Flow

```
SUBMITTED ──► AI_REVIEWED ──► APPROVED (auto, high confidence)
                │             REJECTED (auto, high confidence)
                └──► NEEDS_HUMAN_REVIEW ──► APPROVED (reviewer)
                                         └► REJECTED (reviewer)
                                              └► SETTLED
```
