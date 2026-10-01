# Zuck — Autonomous AI Software Engineering Team Platform

> A reusable, persistent virtual AI Engineering Team platform managing software projects through Telegram and GitHub.

---

## 🏛️ Architecture Overview

Zuck decouples the **engineering organization** from individual **software projects**. The team is persistent, multi-disciplinary, and project-agnostic.

```text
                        Anwar (Maintainer)
                                │
                      Telegram (Voice / Text)
                                │
                                ▼
                     TelegramTeamLeadService
                                │
                                ▼
                         TeamCoordinator
                                │
              ┌─────────────────┼─────────────────┐
              ▼                 ▼                 ▼
       EngineeringTeam    LlmClient (Gemini)   ProjectRegistry
              │                 │                 │
              ▼                 ▼                 ▼
        AgentRegistry     Multimodal Audio    External Client Projects:
        (8 Personas)      Dynamic Dialogue    • habit-coach-agent (Java)
                          Structured JSON     • ai-academy (TypeScript)
```

---

## 👥 The Virtual Engineering Team

The platform maintains 8 persistent agents with distinct personas, domain authority, and responsibilities:

| Icon | Agent ID | Persona Name | Role | Responsibilities | Core Stack / Capabilities |
|---|---|---|---|---|---|
| 🤖 | `zuck` | **Zuck** | Team Lead / Engineering Manager | Orchestrates meetings, decomposes tasks, decides participation, and tracks delivery. | Task decomposition, planning, coordination |
| 🧑‍💻 | `backend` | **Mr. 500** | Senior Backend Engineer | APIs, domain logic, persistence, databases, performance, and backend tests. | Java 21, Spring Boot, PostgreSQL, JPA, REST APIs |
| 🎨 | `frontend` | **Pixel** | Senior Frontend Engineer | UI/UX, responsive layouts, web and mobile components, client state. | React, Next.js, TypeScript, Flutter, CSS |
| 🔍 | `qa` | **Sherlock** | QA / Test Engineer | Acceptance criteria, edge cases, regression test suites, boundary testing. | Test planning, edge case detection, API testing |
| 🛠️ | `platform` | **Atlas** | DevOps & Platform Engineer | Docker, CI/CD pipelines, environments, security, performance, deployments. | Docker Compose, GitHub Actions, Linux, security |
| 📋 | `product` | **Mira** | Product Manager | User value, requirement clarification, feature scopes, user stories, roadmaps. | Requirements, acceptance criteria, product design |
| 🔬 | `research` | **X** | Technical Research | Third-party benchmarks, library evaluations, architecture trade-offs. | Technical research, documentation, benchmarks |
| ⚖️ | `ilon` | **Ilon** | Independent Reviewer | PR reviews, architectural standards, code quality, security boundaries. | Code review, architecture review, quality gates |

---

## 🎙️ Telegram & Voice Capabilities

### 1. Multimodal Voice Interaction
Send voice notes directly in your Telegram group or private chat. The platform downloads the voice note (`audio/ogg`) and uses Google Gemini 2.5 Flash to accurately transcribe spoken speech into text before initiating team coordination.

### 2. Natural Language Team Collaboration
Mention any project and idea naturally in Telegram (e.g., *"Zuck, let's add a streak counter in habit-coach project"*). Zuck will:
1. Detect and switch to the target client project (`habit-coach-agent`).
2. Convene the relevant specialists based on domain keywords and core-team fallbacks.
3. Conduct a multi-turn, in-character engineering meeting using structured JSON.
4. Output the meeting discussion with distinct avatar emojis and formatted roles.

### 3. Telegram Slash Commands

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

## ⚙️ Configuration & Environment

Create a `.env` file or export the following variables:

```bash
# Telegram Configuration
TELEGRAM_BOT_TOKEN="<your_bot_token>"
TELEGRAM_ALLOWED_USER_ID="<your_telegram_user_id>"
TELEGRAM_ALLOWED_CHAT_ID="<your_group_or_private_chat_id>"
TELEGRAM_NATURAL_LANGUAGE=true

# Google Gemini API (Multimodal Audio & Collaboration)
GEMINI_API_KEY="<your_gemini_api_key>"
GEMINI_MODEL=gemini-2.5-flash
```

---

## 🚀 Running Locally

```bash
# Run unit and integration tests (16/16 tests passing)
./mvnw clean test

# Start the platform
./mvnw spring-boot:run
```

Runs on port `8082` by default.
