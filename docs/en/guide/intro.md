# Introduction

Captcha Toolkit is a reusable behavior captcha toolkit:

- Slider puzzle: drag the puzzle piece to the gap
- Click characters: click target characters in order
- Rotate: drag the slider to align a rotated disc
- Angle: drag the slider to rotate the central circular image back to the upright orientation
- Scratch: drag the slider left to right to sweep the coating away and stop as soon as all prompted shapes appear
- Curve drawing: trace the guide curve from the green start to the red end
- Slide curve: the curve is anchored at both ends; drag the slider to swing it into the real groove (with fake grooves)
- Swing tile: drag the slider to move the tile along a multi-order Bézier curve into the target groove, swinging its orientation along the path (with fake grooves)

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

- Unified template + type-specific validators (slider / click / rotate / angle / scratch / curve / slide-curve / swing-tile) for behavior checks
- Compressed trajectory payload (`td`, gzip + base64url)
- Per-client profiles: web / H5 / mini program
- Normalized coordinates (0~1) independent of screen size
- One-time tickets with server-side validation
