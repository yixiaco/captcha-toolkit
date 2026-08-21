# Introduction

Captcha Toolkit is a reusable behavior captcha toolkit:

- Slider puzzle: drag the puzzle piece to the gap
- Click characters: click target characters in order
- Rotate: drag the slider to align a rotated disc

Answers are only stored server-side. A one-time ticket is issued after verification, and the session is destroyed on success or failure.

## Tech Stack

- Backend: Java 21 + Spring Boot 4 + Maven. The core engine has no Spring dependency.
- Frontend: Vue 3 + TypeScript + Vite 6.

## Module Structure

```text
backend/
  captcha-core/                  Pure Java engine (no Spring)
  captcha-spring-boot-starter/   Auto-configuration + HTTP controller
  captcha-demo/                  Runnable demo app + image assets
frontend/
  src/lib/                       Vue 3 component library (TypeScript)
  src/demo/                      Demo site
docs/                            VitePress documentation (this site)
```

## Key Features

- Unified template + type-specific validators for behavior checks
- Compressed trajectory payload (`td`, gzip + base64url)
- Per-client profiles: web / H5 / mini program
- Normalized coordinates (0~1) independent of screen size
- One-time tickets with server-side validation
