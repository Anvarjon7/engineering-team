# Zuck — Autonomous AI Software Engineering Team Platform

> A reusable, persistent AI Engineering Team platform managing software projects across Telegram and GitHub.

---

## Architecture Overview

```text
                        Anwar (Human)
                              │
                           Telegram
                              │
                              ▼
                  TelegramTeamLeadService
                              │
                              ▼
                       TeamCoordinator
                       /              \
                      /                \
                     ▼                  ▼
             EngineeringTeam    CurrentProjectContext
                    │                   │
                    ▼                   ▼
              AgentRegistry      ProjectRegistry
                                        │
                                        ▼
                             External Client Projects:
                             • habit-coach-agent
                             • ai-academy
                             • ecommerce-api
```

---

## The Virtual Engineering Team

The platform maintains 8 persistent agents with distinct personas and responsibilities:

* 🤖 **Zuck** — Team Lead & Engineering Manager
* 🧑‍💻 **Mr. 500** — Senior Backend Engineer (Java / Spring Boot / PostgreSQL / REST APIs)
* 🎨 **Pixel** — Senior Frontend Engineer (React / Next.js / Flutter / UI)
* 📋 **Mira** — Product Manager (Requirements / User Stories / Acceptance Criteria)
* 🔍 **Sherlock** — QA Engineer (Test Suites / Edge Cases / Regressions)
* 🛠️ **Atlas** — DevOps & Platform Engineer (Docker / CI/CD / Infrastructure)
* 🔬 **X** — Technical Research & Architecture Investigation
* ⚖️ **Ilon** — Independent Code & PR Reviewer

---

## Telegram Commands

| Command | Action |
|---|---|
| `/task <description>` | Decomposes task and coordinates relevant team members |
| `/team` | Lists all 8 team members and their capabilities |
| `/project` or `/current` | Displays currently active project context |
| `/projects` | Lists all registered external projects |
| `/switch <projectId>` | Switches active project context |
| `/whoami` | Shows your Telegram User ID and Chat ID |
| `/help` | Shows command guide |

---

## Configuration

Copy `.env.example` to `.env` or set environment variables:

```bash
TELEGRAM_BOT_TOKEN=your_bot_token_here
TELEGRAM_ALLOWED_USER_ID=your_telegram_id
TELEGRAM_ALLOWED_CHAT_ID=your_chat_id
```

### Running Locally

```bash
./mvnw clean test
./mvnw spring-boot:run
```

Runs on port `8082` by default.
