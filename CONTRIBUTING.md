# Contributing to Zuck Platform

## Workflow Standards

All contributors and AI agents must follow this workflow to maintain stability, security, and traceability.

### 1. Branch Strategy
- Never push directly to `main`.
- Branch naming conventions:
  - `feature/<short-description>`
  - `fix/<short-description>`
  - `chore/<short-description>`
  - `refactor/<short-description>`
  - `test/<short-description>`

### 2. Testing Requirement
- Every new feature, command, or agent capability must be accompanied by comprehensive automated tests (`mvn clean test`).
- PRs with failing tests or decreased test coverage will not be accepted.

### 3. Review & Approval
- PRs must pass CI before merge.
- Independent AI review (`Ilon`) checks code quality and architecture alignment.
- The human maintainer retains final merge authority.

### 4. Security & Credentials
- Never commit `.env` files, API tokens (`TELEGRAM_BOT_TOKEN`), or secret keys.
- Use environment variables or `.env.example` as a template.
