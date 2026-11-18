## Development

When starting the dev server, use background mode:

```
astro dev --background
```

Manage the background server with `astro dev stop`, `astro dev status`, and `astro dev logs`.

## Documentation

Full documentation: https://docs.astro.build

Consult these guides before working on related tasks:

- [Adding pages, dynamic routes, or middleware](https://docs.astro.build/en/guides/routing/)
- [Working with Astro components](https://docs.astro.build/en/basics/astro-components/)
- [Using React, Vue, Svelte, or other framework components](https://docs.astro.build/en/guides/framework-components/)
- [Adding or managing content](https://docs.astro.build/en/guides/content-collections/)
- [Adding styles or using Tailwind](https://docs.astro.build/en/guides/styling/)
- [Supporting multiple languages](https://docs.astro.build/en/guides/internationalization/)

## Versioning & Releases

- **Mandatory Ecosystem Synchronization**: Whenever bumping the app version across the Dosezy ecosystem (both for active releases and preparing in-development versions like `2.5.14`), all version strings across the website project must be updated every time.
- **Files to Update**:
  - `package.json`: Update `"version"` to match the new version string (e.g. `"2.5.14"`).
  - `package-lock.json`: Synchronize via `npm install`.
  - `DESIGN.md`: Update title version header (e.g. `(v2.5.14)`).
  - `src/pages/patient.astro`: Update client version header (e.g. `DOSEZY PATIENT CLIENT (v2.5.14)`).
  - `src/components/Footer.astro`: Update release tag link (`/releases/tag/v2.5.14`), title attribute, and badge text (`v2.5.14`).
  - `src/components/DownloadSection.astro`: Update download badge description (e.g. `v2.5.14 APK`).
  - `src/components/FutureRoadmap.astro`: Update roadmap status badge (e.g. `AVAILABLE NOW • v2.5.14`).
  - `src/components/AiAssistantChat.astro`: Update AI assistant download response (e.g. `Patient App (v2.5.14)`).
  - `src/components/Hero.astro`: Update any active release tags or links.
- **Verification**: Run `npm run build` to confirm all 17 static routes and sitemaps compile with zero errors.
- See the root repository `AGENTS.md` (`## 2. Version Bumping & Changelogs Checklist`) for complete ecosystem-wide versioning protocols.

