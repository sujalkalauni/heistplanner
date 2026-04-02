# HeistPlanner 

> A REST API strategy game — plan heists, recruit crew, and execute with probabilistic outcomes.

Make HTTP calls to plan a heist. Recruit a hacker, driver, safecracker. Complete planning phases. Then execute — and let the engine decide if you walk away rich or in handcuffs.

---

## How It Works

1. **Create a heist** — pick your target (Art Museum → Federal Reserve)
2. **Recruit crew** — each role reduces a specific risk category
3. **Complete planning phases** — Recon, Entry Plan, Escape Route etc. reduce overall risk
4. **Check your odds** — risk score and success probability update live
5. **Execute** — probabilistic roll determines SUCCESS / PARTIAL_SUCCESS / FAILED / BUSTED
6. **Build reputation** — successful heists increase your reputation score

---

## Targets

| Target | Base Risk | Payout |
|---|---|---|
| Art Museum | 25% | Rs50,000 |
| Jewelry Store | 35% | Rs120,000 |
| Crypto Exchange | 50% | Rs750,000 |
| City Bank | 55% | Rs500,000 |
| Casino | 70% | Rs1,000,000 |
| Federal Reserve | 90% | Rs5,000,000 |

---

## Crew Roles

| Role | Reduces |
|---|---|
| HACKER | Tech/alarm risk |
| DRIVER | Escape risk |
| MUSCLE | Guard confrontation risk |
| SAFECRACKER | Vault/bank risk (massive reduction) |
| LOOKOUT | Detection risk |
| MASTERMIND | Overall risk |

Low loyalty crew (loyalty < 3) may betray you and trigger a BUST regardless of odds.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Spring Boot 3.2 |
| Auth | JWT (jjwt 0.11.5) |
| ORM | Spring Data JPA + Hibernate |
| Database | MySQL 8 |
| Game Engine | Custom probabilistic engine |
| Docs | SpringDoc OpenAPI (Swagger) |
| Tests | JUnit 5, Mockito + H2 |

---

## Getting Started

### Prerequisites
- Java 17+
- MySQL 8
- Maven 3.8+

### Setup

```bash
git clone https://github.com/sujalkalauni/heistplanner
cd heistplanner
cp src/main/resources/application.properties.example src/main/resources/application.properties
# Edit application.properties with your MySQL credentials
mvn spring-boot:run
```

API: `http://localhost:8082`
Swagger UI: `http://localhost:8082/swagger-ui.html`

---

## API Endpoints

### Auth
| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/auth/register` | Register a planner account |
| POST | `/api/auth/login` | Login, get JWT |

### Heists
| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/heists` | Create a heist |
| GET | `/api/heists/{id}` | Get heist details + current odds |
| GET | `/api/heists/mine` | List your heists |
| POST | `/api/heists/{id}/crew` | Recruit a crew member |
| POST | `/api/heists/{id}/phases` | Complete a planning phase |
| POST | `/api/heists/{id}/execute` | Execute the heist  |
| GET | `/api/heists/stats/me` | Your reputation + earnings |
| GET | `/api/heists/stats/targets` | Global target difficulty stats |

---

## Example Playthrough

```bash
# 1. Register
curl -X POST http://localhost:8082/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"ghost","email":"ghost@crew.com","password":"darkside"}'

# 2. Create a heist
curl -X POST http://localhost:8082/api/heists \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"name":"Operation Blackout","target":"CITY_BANK"}'

# 3. Recruit crew
curl -X POST http://localhost:8082/api/heists/1/crew \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"name":"Zero","role":"HACKER","skillLevel":9,"loyalty":8,"cutPercentage":15}'

# 4. Complete planning phases
curl -X POST http://localhost:8082/api/heists/1/phases \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"phase":"RECONNAISSANCE","notes":"Scoped the bank for 2 weeks"}'

# 5. Execute
curl -X POST http://localhost:8082/api/heists/1/execute \
  -H "Authorization: Bearer <token>"
```

### Example response
```json
{
  "result": "SUCCESS",
  "narrative": "Everything went according to plan. You're a legend.",
  "payout": 382500,
  "reputationChange": 10,
  "newSuccessProbability": 76
}
```

---

## Running Tests

```bash
mvn test
```

Tests use H2 in-memory — no MySQL needed.

---

## Project Structure

```
src/
├── main/java/com/heistplanner/
│   ├── engine/        # HeistEngine — probabilistic risk + outcome calculation
│   ├── config/        # SecurityConfig, GlobalExceptionHandler
│   ├── controller/    # AuthController, HeistController
│   ├── dto/           # All request/response DTOs
│   ├── entity/        # User, Heist, CrewMember, HeistPhase
│   ├── repository/    # JPA repos with analytics queries
│   ├── security/      # JwtUtils, JwtAuthFilter
│   └── service/       # AuthService, HeistService
└── test/
    ├── engine/        # HeistEngineTest — probability logic
    └── service/       # HeistServiceTest — game flow
```

---

## Author

**Sujal Kalauni** — [github.com/sujalkalauni](https://github.com/sujalkalauni)
