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

- When bumping versions across the Dosezy ecosystem, update `"version"` in `package.json` (and sync `package-lock.json`) to match the active release or in-development version (e.g. `2.5.14`).
- Update client download asset references and release tags across Astro components (`src/pages/patient.astro`, `src/components/Hero.astro`, `src/components/Footer.astro`, etc.) when a new client version is released.
- See the root repository `AGENTS.md` (`## 2. Version Bumping & Changelogs Checklist`) for complete ecosystem-wide versioning protocols.

