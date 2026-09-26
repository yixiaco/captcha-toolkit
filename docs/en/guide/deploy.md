# Deployment & Docs

## Preview Docs Locally

```bash
cd docs
npm install
npm run dev
```

The docs dev server runs on `http://localhost:5174` (separate from the frontend demo at `5173`).

Build the static site:

```bash
npm run build
```

Output goes to `docs/.vitepress/dist`.

## GitHub Pages

`.github/workflows/deploy-docs.yml` builds and deploys on pushes to `master`:

1. Install dependencies (`npm ci`)
2. Run `vitepress build`
3. Upload `docs/.vitepress/dist`
4. Deploy

Set Settings → Pages → Source to **GitHub Actions**.

::: warning Base path
`docs/.vitepress/config.mjs` uses `base: '/captcha-toolkit/'` to match the repository name.
Change it to `/` when deploying to a custom domain.
:::

## Deploying the Project

Backend:

```powershell
cd backend
$env:JAVA_HOME='D:\jdks\openjdk-21.0.2'
D:\software\apache-maven-3.9.11\bin\mvn.cmd clean install
```

Frontend:

```bash
cd frontend
npm install
npm run build:demo
```

The demo output is `frontend/dist-demo`. In production, set `captcha.debug-enabled: false`
and replace the in-memory session store.

## Publishing to Maven Central

`captcha-core`, `captcha-spring-boot4-starter` and `captcha-spring-boot3-starter` are published as
`io.github.yixiaco:captcha-core`, `io.github.yixiaco:captcha-spring-boot4-starter` and
`io.github.yixiaco:captcha-spring-boot3-starter`.
The groupId must be a verified namespace (this project's is verified automatically
through the GitHub account), and the Java package root matches it.

The `release` profile generates the sources/javadoc attachments, signs them with GPG,
then uploads through the Central Portal:

```powershell
cd backend
$env:JAVA_HOME='D:\jdks\openjdk-21.0.2'
# GPG ships with Git; put its bin directory on PATH or keyboxd cannot start
$env:PATH="C:\Program Files\Git\usr\bin;$env:PATH"
D:\software\apache-maven-3.9.11\bin\mvn.cmd -Prelease -pl captcha-core,captcha-spring-boot4-starter,captcha-spring-boot3-starter -am deploy
```

Notes:

- Credentials live in the `<id>sonatype</id>` server of `settings.xml` (Central Portal user token)
- The parent POM `captcha-toolkit-parent` must be published too, otherwise consumers cannot resolve child POMs
- `captcha-demo-boot4` / `captcha-demo-boot3` are marked as skip-publish; only core and the two starters are released
- `captcha-spring-boot3-starter` uses Spring Boot 3.5.16 as its parent (it cannot inherit
  `captcha-toolkit-parent`) and carries its own copy of the release profile; bump its `<version>` too
- Bump the version before every release: Central never allows overwriting a published version

## Publishing to npm

The Vue component library is published as `captcha-toolkit-vue`. The `prepublishOnly`
hook runs `build:lib` first, so a stale `dist` can never be published.

```bash
cd frontend
npm publish
```

Notes:

- Vue is a peer dependency (externalized at build time) and is never bundled, so the
  host project cannot end up with two copies of Vue
- Only `dist` is published (`files`); README and LICENSE are attached automatically
- With 2FA enabled, `_authToken` must be a granular access token with “bypass 2FA”;
  alternatively pass a one-time code: `npm publish --otp=<code>`
- The version lives in `frontend/package.json`; published versions cannot be overwritten
