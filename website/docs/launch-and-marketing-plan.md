# Dosezy Launch & Marketing Plan

> **Document Version:** 1.0  
> **Last Updated:** September 2026  
> **Status:** Execution Ready  

---

## 1. Executive Summary & Core Positioning

### Executive Summary
**Dosezy** is a free, open-source, and 100% offline-first medicine tracking application engineered to eliminate the privacy compromises, invasive telemetry, and motor-accessibility barriers found in commercial prescription reminder apps.

Unlike proprietary medication trackers that aggregate or sell prescription routines to ad networks, Dosezy enforces total privacy at the OS manifest level by intentionally omitting `android.permission.INTERNET` from its core client. For families requiring remote monitoring, Dosezy provides optional End-to-End Encrypted (E2EE) synchronization via either a self-hosted Docker node or a managed cloud platform.

### Core Value Proposition & Differentiators
1. **Kernel/Manifest-Enforced Privacy:** Zero telemetry, zero external trackers, local AES-256 Room/SwiftData SQLite databases, and manifest-enforced network isolation.
2. **Accessible 12-Hour Grid Time Picker:** Replaces frustrating circular clock dials with stationary hour and minute touch targets designed for elderly users, presbyopia, and motor tremors.
3. **Offline Emergency ISO Dialer:** Auto-detects emergency services (US 911, EU 112, UK 999, India 112) using local SIM ISO detection without network connectivity.
4. **Contract-Driven & Server Agnostic:** Shared `api/openapi.yaml` contract guarantees complete data portability between self-hosted community nodes (`Docker Compose`) and the official managed cloud platform (`api.dosezy.app`).
5. **Zero Lock-In Pricing Model:** Free client app ($0), free self-hosted backend ($0), and an optional $9.99/mo Family Group Managed Cloud Plan.

---

## 2. Target Audience Personas

| Target Persona | Key Pain Point | Primary Dosezy Value Hook | Primary Channels |
|---|---|---|---|
| **1. Privacy Advocates & FOSS Enthusiasts** | Commercial health apps harvest & sell medication data; forced cloud logins. | `android.permission.INTERNET` omission; F-Droid release; 100% Open Source. | Hacker News, `r/privacy`, `r/androidapps`, F-Droid |
| **2. Self-Hosters & Homelab Owners** | Closed SaaS ecosystems with vendor lock-in & subscription creep. | Docker Compose backend node; private PostgreSQL database; zero telemetry. | `r/selfhosted`, GitHub, Awesome-Selfhosted |
| **3. Family Caregivers & Adult Children** | Worrying about elderly parents missing critical doses while wanting privacy respect. | E2EE Caregiver Web Portal (`app.dosezy.com`); real-time SMS & push alerts. | `r/AgingParents`, `r/CaregiverSupport`, Caregiver Blogs |
| **4. Elderly & Accessibility Users** | Small circular clock dials causing input errors; complex UI latency. | High-contrast 7.8:1 AAA UI; stationary 12-Hour Grid Time Picker; 200% font scaling. | Senior Care Forums, Nextdoor, Accessibility advocates |
| **5. Neurodivergent / ADHD Users** | Executive dysfunction triggered by ad popups, push notifications, and cluttered menus. | Minimalist, zero-ad interface; doze-resilient persistent alarm pipeline. | `r/ADHD`, `r/Neurodiversity`, Productivity blogs |

---

## 3. Go-To-Market (GTM) Multi-Phase Strategy

```
┌─────────────────────────────────────────────────────────────────────────┐
│                      PHASE 1: DEV & PRIVACY BLITZ                       │
│        Hacker News (Show HN) • r/privacy • r/selfhosted • F-Droid       │
└────────────────────────────────────┬────────────────────────────────────┘
                                     │
                                     ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                     PHASE 2: PRODUCT DISCOVERY LAUNCH                   │
│        Product Hunt • AlternativeTo.net • GitHub Awesome Lists          │
└────────────────────────────────────┬────────────────────────────────────┘
                                     │
                                     ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                 PHASE 3: CAREGIVER & ACCESSIBILITY OUTREACH             │
│        r/AgingParents • r/ADHD • Caregiver & Senior Forums • IH         │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 4. Channel Execution Playbook & Post Copy

### Channel 1: Hacker News (*Show HN*)
* **Timing:** Tuesday or Wednesday at 12:00 PM UTC (7:00 AM EST / 4:00 AM PST)
* **Title:** `Show HN: Dosezy – Free, open-source, 100% offline medicine tracker without internet permissions`
* **Post Copy:**
> Hi HN! I built Dosezy, a free and open-source medication tracker designed around local-first privacy and motor accessibility.
> 
> **Why I built it:** Commercial prescription apps frequently track user behavior, require cloud signups, or aggregate routine data. Dosezy intentionally omits `android.permission.INTERNET` from its manifest, ensuring your health records physically cannot leave your device.
> 
> **Key Technical Highlights:**
> - **Local Storage:** SQLite Room (Android) and SwiftData (iOS) with optional local AES-256 encryption.
> - **12-Hour Grid Picker:** Replaces circular clock spinners with stationary grid buttons to prevent input latency and assist users with motor tremors.
> - **SIM ISO Emergency Dialer:** Auto-detects local emergency services (US 911, EU 112, UK 999, India 112) offline.
> - **OpenAPI 3.0 Contract:** Shared schema (`api/openapi.yaml`) supporting optional E2EE sync to either a self-hosted Docker Compose node or managed server.
> 
> Monorepo & Source Code: https://github.com/saad2134/dosezy  
> Website: https://dosezy.app  
> 
> I’d love your feedback on the architecture, accessibility choices, and offline scheduling pipeline!

---

### Channel 2: Reddit Launch Matrix

#### 1. `r/privacy`
* **Title:** `I created a medicine tracker app that intentionally omits internet permissions from its manifest`
* **Focus:** Manifest-enforced offline isolation, zero telemetry, local Room DB, no accounts required.

#### 2. `r/selfhosted`
* **Title:** `Dosezy: Self-host your family's medication adherence sync engine via Docker Compose`
* **Focus:** Docker setup, PostgreSQL backend, private WebSocket stream, Caregiver Web Portal (`Option B`), zero external cloud callbacks.

#### 3. `r/androidapps` & `r/opensource`
* **Title:** `Dosezy: Free, open-source & zero-ad medication reminder app for Android (F-Droid / GitHub)`
* **Focus:** FOSS license, Jetpack Compose UI, Doze-resilient alarm schedule, F-Droid availability.

#### 4. `r/AgingParents` & `r/CaregiverSupport`
* **Title:** `Built a simple, high-contrast medicine reminder app with stationary buttons for elderly parents`
* **Focus:** High contrast AAA ratios, 12-Hour Grid Time Selector, emergency dialer, optional family cloud alerts.

---

### Channel 3: Product Hunt Launch Kit
* **Launch Date:** Tuesday (12:01 AM PST)
* **Product Name:** Dosezy
* **Tagline:** *Free, open-source & 100% offline medicine tracker with caregiver sync*
* **Topics:** Tech, Open Source, Healthcare, User Experience, Privacy
* **Maker Comment:**
> Hey Product Hunt! 👋 
> Most medicine tracking apps today force cloud accounts, serve intrusive ads, or sell prescription telemetry. We built Dosezy to fix this.
> 
> Dosezy is 100% free, open-source, and offline-first. It requires zero network permissions by default, features an accessible 12-Hour Grid Time Picker designed for motor tremor safety, and supports optional end-to-end encrypted caregiver sync for family peace of mind.
> 
> Check out our interactive 2D architecture and let us know what you think! 🚀

---

### Channel 4: AlternativeTo.net Listing
* **Target Competitors:** Medisafe, MyTherapy, Apple Health Medication Reminders, Pill Reminder.
* **Key Tags:** Open Source, Self-Hosted, Offline-First, Privacy-Focused, Zero Ads, F-Droid.
* **Description Summary:** Position Dosezy as the only non-commercial, open-source alternative that doesn't hoard health data or enforce subscription paywalls for basic scheduling.

---

## 5. Four-Week Execution Timeline & Checklist

```
Week -1                  Week 1                  Week 2                  Week 3 & 4
[ Pre-Launch Prep ] ───► [ Hacker News & Reddit ] ───► [ Product Hunt & Directories ] ───► [ Niche Outreach & Iteration ]
```

### Week -1: Pre-Launch Readiness
- [ ] Confirm `npx astro build` passes cleanly across all 17 static site routes.
- [ ] Verify F-Droid submission pipeline and build artifacts on GitHub Releases.
- [ ] Record short 30-second hero preview GIFs of the 12-Hour Grid Picker & Architecture page.
- [ ] Prepare screenshot assets from `public/screenshots/` for Product Hunt gallery.

### Week 1: Technical & FOSS Launch Blitz
- [ ] **Tuesday 12:00 UTC:** Post **Show HN** on Hacker News. Monitor and respond to technical comments for 12 hours.
- [ ] **Wednesday:** Post to `r/privacy` and `r/selfhosted`.
- [ ] **Thursday:** Post to `r/androidapps` and `r/opensource`.
- [ ] Submit PRs to `awesome-selfhosted`, `awesome-android`, and `awesome-privacy` GitHub repositories.

### Week 2: Product Discovery & Directory Distribution
- [ ] **Tuesday 12:01 AM PST:** Launch on **Product Hunt**. Engage supporters and reply to Maker thread.
- [ ] Create official listing on **AlternativeTo.net**.
- [ ] Publish an Indie Hackers article detailing the local-first engineering story.

### Week 3 & 4: Niche Community Outreach & Feedback Integration
- [ ] Share in `r/AgingParents`, `r/CaregiverSupport`, and `r/ADHD`.
- [ ] Monitor GitHub Issues for bug reports, translations, and feature requests.
- [ ] Analyze managed cloud conversion analytics and iterate based on community feedback.

---

## 6. Target Key Performance Indicators (KPIs)

| Metric Category | Target Milestone (30 Days) | Goal |
|---|---|---|
| **GitHub Stars** | 500+ Stars | Establish repository credibility and FOSS visibility |
| **F-Droid / APK Downloads** | 5,000+ Downloads | Build core active user base |
| **Self-Hosted Docker Deployments** | 200+ Nodes | Validate self-hosted server node adoption |
| **Product Hunt Ranking** | Top 5 Product of the Day | Maximum launch awareness |
| **Caregiver Cloud Conversion** | 2% - 3% Conversion | Validate $9.99/mo Family Plan demand |
