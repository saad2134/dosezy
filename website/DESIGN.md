# DESIGN.md - Dosezy Design System & Aesthetic Standard (v2.2.2)

## 1. Vision & Tone
Dosezy is a high-assurance, open-source, local-first medicine tracking platform. The landing page design features **icon-extracted corner purple & violet aesthetics**: sleek obsidian background, radiant violet/purple ambient glows, glassmorphic surface depth, crisp high-contrast typography, and vibrant status accents (Purple/Violet for primary brand highlights, Cyan for tech precision, Sunset Amber for adherence/late reminders, Crimson for emergency features).

---

## 2. Color Palette & Tokens (Extracted from App Icon)

```css
@import "tailwindcss";

@theme {
  /* Fonts */
  --font-sans: 'Inter', system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  --font-display: 'Outfit', 'Inter', sans-serif;

  /* Obsidian Palette */
  --color-obsidian-950: #030407;
  --color-obsidian-900: #0a0c10;
  --color-obsidian-800: #12151c;

  /* Icon-Extracted Corner Purple & Violet Accents */
  --color-brand-purple: #8b5cf6;
  --color-brand-violet: #7c3aed;
  --color-brand-magenta: #a855f7;
  --color-brand-cyan: #00d8ff;
  --color-brand-blue: #0277bd;
  --color-brand-light-blue: #4fc3f7;
  --color-brand-amber: #f97316;
  --color-brand-crimson: #ef4444;

  /* Glass & Borders */
  --color-surface-glass: rgba(18, 21, 28, 0.7);
  --color-border-subtle: rgba(255, 255, 255, 0.08);
  --color-border-glow: rgba(139, 92, 246, 0.3);

  /* Shadows & Glows */
  --shadow-purple-glow: 0 0 50px -10px rgba(139, 92, 246, 0.25);
  --shadow-cyan-glow: 0 0 40px -10px rgba(0, 216, 255, 0.2);
}
```

---

## 3. Visual Patterns
- **Corner Glow Accent**: Top-right / bottom-left radial background spotlights featuring `#7c3aed` and `#a855f7`.
- **Badges**: Purple/Violet subtle pills (`bg-purple-500/10 text-purple-300 border-purple-500/20`).
- **Store Badges**: Official local `get-it-on-github.png` and `get-it-on-fdroid.png` graphics.
- **Licensing Badge**: Single `GPL-3.0` badge in footer.
