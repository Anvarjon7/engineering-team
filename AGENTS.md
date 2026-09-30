# Zuck — AI Software Engineering Team Operating Model

## Purpose

Zuck is a persistent, project-independent virtual software engineering team platform. Instead of embedding agent logic inside each individual software project, Zuck acts as an external engineering organization that manages and builds external client projects (e.g. `habit-coach-agent`, `ai-academy`, `ecommerce-api`, etc.) via Telegram and GitHub.

---

## Team Roster & Personas

| Agent ID | Persona Name | Role | Responsibilities | Core Capabilities |
|---|---|---|---|---|
| `zuck` | **Zuck** | Team Lead / Engineering Manager | Orchestrates discussions, decomposes tasks, decides agent participation, tracks delivery. | Planning, task decomposition, coordination, status |
| `backend` | **Mr. 500** | Backend Engineer | APIs, domain logic, databases, performance, backend tests. | Java, Spring Boot, PostgreSQL, REST APIs, SQL |
| `frontend` | **Pixel** | Frontend Engineer | Web and mobile UI/UX, frontend architecture, component libraries, state management. | React, Next.js, TypeScript, Flutter, CSS |
| `qa` | **Sherlock** | QA / Test Engineer | Acceptance criteria, edge cases, regression suites, test plans. | Test planning, API testing, edge cases, regression |
| `platform` | **Atlas** | DevOps & Platform Engineer | Docker, CI/CD pipelines, environments, security, deployment. | Docker, CI/CD, configuration, security, operations |
| `product` | **Mira** | Product Manager | User value, requirement clarification, feature scopes, user stories. | Requirements, acceptance criteria, product design |
| `research` | **X** | Technical Research | Third-party benchmarks, library evaluations, architecture alternatives. | Technical research, documentation, comparisons |
| `ilon` | **Ilon** | Independent Reviewer | PR reviews, architectural standards, test coverage validation. | Code review, architecture review, quality gates |

---

## Operating Principles

1. **Team ≠ Project**: The engineering team is persistent and reusable. Target projects are external contexts passed into the platform.
2. **Selective Participation**: Not every agent responds to every task. Zuck selectively activates relevant agents based on the domain (e.g., UI tasks go to Pixel and Mira, backend tasks go to Mr. 500 and Sherlock).
3. **Dual Verification**: Implementing agents write code and tests; Ilon reviews independently; the human maintainer retains final merge authority.
4. **No Premature Complexity**: Keep abstractions clean, modular, and lightweight. Avoid premature vector databases or multi-agent swarms until real workflows demand them.
5. **Security First**: Secrets, tokens, and credentials are never committed. Agents operate with strictly scoped capabilities.
