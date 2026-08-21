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
