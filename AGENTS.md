# Repository Guidelines

## Project Structure & Module Organization

This repository is a reusable behavior-captcha toolkit (slider puzzle + click characters) with a Spring Boot backend and a Vue 3 frontend.

```text
backend/
  captcha-core/                  Pure Java engine (no Spring)
  captcha-spring-boot-starter/   Auto-configuration + HTTP controller
  captcha-demo/                  Runnable demo app + image assets
frontend/
  src/lib/                       Reusable Vue 3 component library
  src/demo/                      Demo app consuming the library
```

Backend source lives under `backend/captcha-core/src/main/java/com/captcha/toolkit/`; tests live under the matching `src/test/java` tree. Frontend styles are in `src/lib/style.css` (library) and `src/demo/demo.css` (demo only).

## Build, Test, and Development Commands

Backend (JDK 21; Maven uses `D:\Maven\.m2` per `conf/settings.xml`):

```powershell
$env:JAVA_HOME='D:\jdks\openjdk-21.0.2'
D:\software\apache-maven-3.9.11\bin\mvn.cmd clean install   # build + test + install
D:\software\apache-maven-3.9.11\bin\mvn.cmd -pl captcha-demo spring-boot:run
```

Frontend:

```bash
cd frontend
npm install
npm run dev          # dev server on :5173, proxies /api to :18080
npm run build:lib    # publishable component bundle
npm run build:demo   # demo site
```

## Coding Style & Naming Conventions

- Java: 4-space indentation, braces on the same line, Java 21, package root `com.captcha.toolkit.*`.
- Frontend: Vue 3 `<script setup>`, 2-space indentation, single quotes, semicolons.
- Prefer interfaces for extension points (`CaptchaFactory`, `WordFactory`, `BackgroundProvider`, `CaptchaSessionStore`) and keep all tunables in `CaptchaConfig` / `CaptchaProperties`.
- Match the existing Chinese code comments; no linter is configured, so follow surrounding style.

## Testing Guidelines

Backend uses JUnit 5. Tests live in `backend/captcha-core/src/test/java` and should target the engine API rather than internals. Name tests descriptively, e.g. `clickUsesConfiguredTargetText`. Run with `mvn test` or `mvn clean install`; all tests must pass before commit.

The frontend has no automated test suite; verify interactions manually against a running backend (`?captcha=slider`, `?captcha=click`).

## Commit & Pull Request Guidelines

Use Conventional Commits with a Chinese summary, matching history: `feat:`, `fix:`, `style:`, `refactor:`, `chore:`, `docs:`.

Example: `feat: 点选支持 target-text 指定目标字`.

For pull requests: describe the motivation and behavior change, link the related issue, and attach before/after screenshots for any visual change. Verify the backend build and frontend library build before requesting review.

## Security & Configuration Tips

Keep `captcha.debug-enabled` off in production — debug responses leak answers. Sessions are one-time and expire server-side; the default in-memory store should be replaced with a shared store (e.g., Redis) for multi-instance deployments. CORS configuration belongs to the host app, not the starter.
