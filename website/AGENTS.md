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

## Pull Request Naming Protocols

The repository enforces strict naming conventions depending on the target branch and purpose of the Pull Request:

1. **New Version Development PRs (Feature or Fix Branches -> `develop`)**:
   * **Mandatory Title Format**:
     `v<version>(<component>): <concise description>`
   * **Syntax Breakdown**:
     * `v<version>`: The active in-development version being targeted (e.g. `v2.5.14`).
     * `(<component>)`: The ecosystem component or module being updated (`website`, `patient-android`, `caregiver-web`, `server`, `patient-ios`, `docs`).
     * `<concise description>`: A clear, lowercase, comma-separated summary of the primary technical deliverables.
   * **Canonical Examples**:
     * `v2.5.14(website): bump version to v2.5.14 and document ecosystem version synchronization protocol` (Reference: [PR #130](https://github.com/saad2134/dosezy/pull/130))
     * `v2.5.14(patient-android): rolling intake intervals, pharmacy custom days, and stock unit precision` (Reference: [PR #131](https://github.com/saad2134/dosezy/pull/131))
   * **Scope**: Strictly mandatory for all new version development pull requests.

2. **Production Release PRs (`develop` -> `main`)**:
   * **Mandatory Title Format**:
     `RELEASE v<version>`
   * **Canonical Examples**:
     * `RELEASE v2.5.14`
     * `RELEASE v2.5.13`
   * **Scope**: Strictly mandatory for every release pull request promoting the finalized `develop` branch into `main`.

## Production Release Gate: Zero Open PRs Pointed at develop

* **Mandatory Merge Precondition**: Pull requests merging `develop` into `main` (`RELEASE v<version>`) **MUST ONLY** be mergeable when there are **zero open pull requests pointed at `develop`**.
* **Rationale**: Ensures all active feature, improvement, fix, and alignment branches targeting the release have been fully reviewed, merged, or closed before promoting code into production.
* **Automated CI Enforcement**: Enforced by the `Main Branch Source Guard` workflow (`.github/workflows/main-branch-guard.yml`), which queries open PRs targeting `develop` via `gh pr list --base develop --state open`. If any PR remains open against `develop`, the check fails and blocks merging `develop` into `main`.
