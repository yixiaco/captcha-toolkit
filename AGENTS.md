# Repository Guidelines

## Project Structure & Module Organization

This repository is a reusable behavior-captcha toolkit (slider puzzle + click characters) with a Spring Boot backend and a Vue 3 frontend.

```text
backend/
  captcha-core/                  Pure Java engine (no Spring), Java 17 bytecode
  captcha-spring-boot4-starter/  Boot 4 auto-configuration + HTTP controller (Java 21)
  captcha-spring-boot3-starter/  Boot 3 adapter (own Boot 3.5.16 parent, Java 17)
  captcha-spring-boot2-starter/  Boot 2.7 adapter (own Boot 2.7.18 parent, Java 17)
  captcha-demo-boot4/            Boot 4 demo app, port 18080 (Java 21)
  captcha-demo-boot3/            Boot 3 demo app, port 18080 (own Boot 3.5.16 parent, Java 17)
  captcha-demo-boot2/            Boot 2.7 demo app, port 18080 (own Boot 2.7.18 parent, Java 17)
frontend/
  src/core/                      Framework-agnostic core (HTTP protocol, behavior trace, device
                                 fingerprint, shapes, i18n) — shared by every UI layer
  src/lib/                       Reusable Vue 3 component library
  src/demo/                      Demo app consuming the library
frontend-vue2/                   Vue 2.7 package (captcha-toolkit-vue2); compiles the same
                                 frontend/src sources with @vitejs/plugin-vue2
```

Backend source lives under `backend/captcha-core/src/main/java/io/github/yixiaco/`; tests live under the matching `src/test/java` tree. Frontend styles are in `src/lib/style.css` (library) and `src/demo/demo.css` (demo only).

`captcha-spring-boot3-starter` and `captcha-spring-boot2-starter` both compile the Java sources of
`captcha-spring-boot4-starter` through `build-helper-maven-plugin` (single source of truth for all
three Boot generations). Because Boot 2.7 is still on `javax` Bean Validation, the shared sources
avoid Bean Validation annotations entirely and validate request parameters explicitly — keep it
that way, or the Boot 2.7 module stops compiling. The three Boot-3/Boot-2.7 adapters plus
`captcha-demo-boot3` do not inherit `captcha-toolkit-parent`, so keep their `<version>` in sync with
the root POM by hand.

## Build, Test, and Development Commands

Backend (JDK 21; `captcha-core` + Boot 2.7/3 modules alone build on JDK 17;
Maven uses `D:\Maven\.m2` per `conf/settings.xml`):

```powershell
$env:JAVA_HOME='D:\jdks\openjdk-21.0.2'
D:\software\apache-maven-3.9.11\bin\mvn.cmd clean install   # build + test + install
# All three demos listen on 18080 on purpose (the frontend proxies to it), run one at a time;
# override with --server.port=<port> when two must run together.
D:\software\apache-maven-3.9.11\bin\mvn.cmd -pl captcha-demo-boot4 -am spring-boot:run   # :18080
D:\software\apache-maven-3.9.11\bin\mvn.cmd -pl captcha-demo-boot3 -am spring-boot:run   # :18080
D:\software\apache-maven-3.9.11\bin\mvn.cmd -pl captcha-demo-boot2 -am spring-boot:run   # :18080
```

Run the demo with `-am` so captcha-core / starter changes are rebuilt in the same reactor;
without it, the demo resolves stale artifacts from the local Maven repo.

JDK 17 can only build the Java 17 part of the reactor
(`mvn.cmd -pl captcha-core,captcha-spring-boot2-starter,captcha-spring-boot3-starter,captcha-demo-boot2,captcha-demo-boot3 -am test`);
the Boot 4 starter and its demo target Java 21.

Releases (`mvn.cmd -Prelease ... deploy`) must run on JDK 21: under JDK 17 the javadoc attachment of
the Boot 2.7 adapter cannot resolve the build-helper-added shared sources.

Frontend:

```bash
cd frontend
npm install
npm run dev          # dev server on :5173, proxies /api to :18080 (whichever demo runs);
                     # override with VITE_API_TARGET=http://localhost:xxxx
npm run build:lib    # publishable component bundle
npm run build:demo   # demo site
```

Vue 2.7 package (separate npm package `captcha-toolkit-vue2`, shares `frontend/src`):

```bash
cd frontend-vue2
npm install
npm run dev          # dev demo on :5175, proxies /api to :18080 (VITE_API_TARGET to change)
npm run build:lib    # -> dist/captcha-toolkit-vue2.js (+ .css)
npm run smoke        # jsdom mount check: renders, portal to body, exactly one captcha request
```

The two frontend packages must stay behaviourally identical: the Vue-2-only pieces are
`frontend/src/lib/portal-vue2.vue`, the `@captcha-portal` alias wiring in both vite configs, and the
`display: contents` host nodes required by Vue 2's single-root rule.

Frontend requires Node 18+ (Vite 6). The default system Node on this machine is 16 and will fail
with `crypto$2.getRandomValues is not a function`; use Node 18+ (e.g. the Codex bundled Node 24)
and run `node_modules\vite\bin\vite.js` directly if `npm run dev` cannot pick a newer Node.

## Coding Style & Naming Conventions

- Java: 4-space indentation, braces on the same line, Java 21, package root `io.github.yixiaco.*`.
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
