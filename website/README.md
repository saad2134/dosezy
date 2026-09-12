# 🌐 Dosezy Official Web Portal & Landing Page

<div align="center">

![Dosezy Web](https://img.shields.io/badge/Framework-Astro-FF5D01?style=for-the-badge&logo=astro&logoColor=white)
![Styling](https://img.shields.io/badge/Styling-Tailwind_CSS_v4-38B2AC?style=for-the-badge&logo=tailwind-css&logoColor=white)
![Type Checking](https://img.shields.io/badge/TypeScript-Ready-3178C6?style=for-the-badge&logo=typescript&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-green?style=for-the-badge)

<p align="center">
  The official marketing portal, landing page, and documentation hub for <strong>Dosezy</strong> — the smart, open-source, local-first medicine tracking and adherence platform.
</p>

</div>

---

## ✨ Features & Highlights

- ⚡ **Ultra-Fast Performance:** Built with **Astro** for zero-JS by default, instant page transitions, and optimal SEO scores.
- 🎨 **Modern Obsidian & Violet Aesthetic:** Tailored Tailwind CSS v4 design system with deep obsidian surfaces (`#030407`), glassmorphic cards, corner violet/purple glows, and high-contrast typography (`Inter` & `Outfit`).
- 📱 **Interactive Product Showcase:**
  - **Interactive 12-First Time Picker:** Live browser demonstration of Dosezy's intuitive grid time selection UI.
  - **Slot-Based Alarm Showcase:** Visual preview of grouped medicine reminders and one-tap compliance actions.
  - **Dynamic Product Roadmap:** Comprehensive roadmap tracking Android milestones, self-hostable sync servers, and the Caregiver Cloud Platform.
- 🛍️ **Store & Direct Downloads:** Seamless direct APK download buttons, F-Droid links, and upcoming store badges.
- 🌐 **Responsive & Accessible:** Fluid mobile-first layout with smooth micro-animations and accessibility standards.

---

## 🛠️ Tech Stack

- **Framework:** [Astro](https://astro.build/)
- **Styling:** [Tailwind CSS v4](https://tailwindcss.com/) with `@tailwindcss/vite`
- **Typography:** `Inter` (sans) and `Outfit` (display)
- **Icons & Assets:** Optimized SVG icons, localized screenshots, and official platform badges
- **Package Manager:** `npm` (Node >= 22.12.0)

---

## 🚀 Getting Started

### Prerequisites

- [Node.js](https://nodejs.org/) `>= 22.12.0`
- [npm](https://www.npmjs.com/)

### Installation

1. Navigate to the website directory:
   ```bash
   cd website
   ```

2. Install dependencies:
   ```bash
   npm install
   ```

3. Start the local development server:
   ```bash
   npm run dev
   ```
   Open [http://localhost:4321](http://localhost:4321) in your browser.

---

## 🧞 Available Scripts

| Command | Action |
| :--- | :--- |
| `npm run dev` | Starts the Astro development server at `localhost:4321` |
| `npm run build` | Compiles production-ready static assets to `./dist/` |
| `npm run preview` | Spins up a local web server to preview the `./dist/` production build |
| `npm run astro -- --help` | Displays help and available Astro CLI flags |

---

## 📁 Project Structure

```text
website/
├── public/                     # Static public assets (favicons, badges, images)
│   └── favicon.svg
├── src/
│   ├── assets/                 # Brand assets, logos, and vector illustrations
│   ├── components/             # Reusable Astro UI components
│   │   ├── DownloadSection.astro      # Download links & store badges
│   │   ├── EmpiricalMetrics.astro     # Security, privacy & local-first stats
│   │   ├── FeatureGrid.astro          # Interactive feature breakdown
│   │   ├── Footer.astro               # Footer links & licensing
│   │   ├── FutureRoadmap.astro        # Visual product roadmap timeline
│   │   ├── Header.astro               # Navigation bar & theme controls
│   │   ├── Hero.astro                 # Hero banner & call-to-actions
│   │   ├── InteractiveGridPicker.astro# Interactive time picker preview
│   │   └── ui/                        # Reusable atom-level UI primitives
│   ├── layouts/
│   │   └── Layout.astro        # Master HTML layout and meta tags
│   ├── pages/
│   │   └── index.astro         # Main landing page
│   └── styles/                 # Global CSS and Tailwind v4 theme definitions
├── astro.config.mjs            # Astro configuration with Tailwind Vite plugin
├── DESIGN.md                   # Design tokens, color palette, and UI standards
├── package.json
└── tsconfig.json
```

---

## 📄 License

This web portal is part of the **Dosezy** open-source project and is licensed under the **MIT License**. See the root [LICENSE](../LICENSE) file for details.
