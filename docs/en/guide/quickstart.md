# Quick Start

## Requirements

- JDK 21 (needed to build every module and run the Boot 4 demo; JDK 17 is enough for
  `captcha-core` and the Boot 3 modules)
- Maven 3.9+ (this repo uses `D:\software\apache-maven-3.9.11`, local repo `D:\Maven\.m2`)
- Node.js 18+ (Vite 6 requirement)

## Start the Backend

```powershell
cd backend
$env:JAVA_HOME='D:\jdks\openjdk-21.0.2'
D:\software\apache-maven-3.9.11\bin\mvn.cmd clean install
# Boot 4 demo: http://localhost:18080 (the frontend dev server proxies to it)
D:\software\apache-maven-3.9.11\bin\mvn.cmd -pl captcha-demo-boot4 -am spring-boot:run
```

Boot 3 / JDK 17 demo: the same configuration and API on port 18081 (it can run next to the Boot 4 demo):

```powershell
cd backend
$env:JAVA_HOME='D:\jdks\graalvm-jdk-17.0.12'
D:\software\apache-maven-3.9.11\bin\mvn.cmd -pl captcha-demo-boot3 -am spring-boot:run
```

Both demos run with `captcha.debug-enabled: true` for easy self-checking; turn it off in production.

::: tip After changing captcha-core or starter
Run `mvn.cmd -pl captcha-core,captcha-spring-boot4-starter,captcha-spring-boot3-starter install -DskipTests` first,
otherwise the demo may use stale artifacts from the local repository.
:::

## Start the Frontend

```bash
cd frontend
npm install
npm run dev
```

The dev server listens on `http://localhost:5173` and proxies `/api` to `:18080`.

Common checks:

```bash
npm run type-check   # vue-tsc type checking
npm run lint         # ESLint
npm run build:lib    # build the library (includes .d.ts)
```

## Open the Demo

- `http://localhost:5173/?captcha=slider` slider puzzle
- `http://localhost:5173/?captcha=click` click characters
- `http://localhost:5173/?captcha=rotate` rotate
- `http://localhost:5173/?captcha=angle` angle
- `http://localhost:5173/?captcha=scratch` scratch
- `http://localhost:5173/?captcha=curve` curve drawing
- `http://localhost:5173/?captcha=slide-curve` slide curve
- `http://localhost:5173/?captcha=swing-tile` swing tile
- `http://localhost:5173/?captcha=auto` (alias `random`) server-decided type

Picking a concrete type is a debug-only convenience: the demo runs with `debug-enabled: true`, so
`?captcha=` is honoured there; in production the server picks from the `captcha.types` pool. For
slider debugging, append a shape, e.g. `?captcha=slider&shape=classic`.

## API Self Check

```bash
curl http://localhost:18080/api/captcha/types
```

```json
{"types":["angle","click","curve","rotate","scratch","slide-curve","slider","swing-tile"],"shapes":{"slider":["classic","leaf","triangle","circle","diamond","star","heart","moon","hexagon"]}}
```
