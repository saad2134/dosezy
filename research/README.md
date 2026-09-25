# Dosezy Scientific Research & Preprint Repository

This directory contains the complete scientific manuscript, experimental datasets, reproduction scripts, and publication-ready packages for the research project:

> **"Dosezy: A Local-First, Accessibility-Centric Android System for Medication Scheduling and Reliable Reminder Delivery"**  
> **Authors:** Saaduddin Mohammad, Md Rahif Uddin Khan, Khwaja Mohammed  
> **Affiliation:** Department of Computer Science and Engineering, Methodist College of Engineering and Technology, Hyderabad, India  
> **Repository:** [https://github.com/saad2134/dosezy](https://github.com/saad2134/dosezy)

---

## 1. Directory Structure & File Map

The `research/` directory is organized into synchronized publication packages:

```text
research/
├── arxiv_preprint/              # Primary Camera-Ready Publication Package
│   ├── paper.pdf                # Camera-ready publication PDF (27 pages, native 2D equations)
│   ├── paper.docx               # Formatted Word manuscript with native OpenXML Math
│   ├── manuscript.md            # Self-contained preprint source in GitHub-Flavored Markdown
│   ├── main.tex                 # Academic LaTeX article source code
│   ├── references.bib           # 26 verified BibTeX citations with DOIs/URLs
│   └── figures/                 # High-resolution vector and PNG architectural diagrams
│
├── zenodo/                      # Dedicated Zenodo Open-Science Deposition Package
│   ├── zenodo.json              # Machine-readable Zenodo deposition metadata schema
│   ├── dosezy_zenodo_bundle.zip # Standalone all-in-one archive ready for one-click upload
│   ├── paper.pdf                # Synchronized camera-ready PDF
│   ├── paper.docx               # Synchronized Word manuscript
│   ├── manuscript.md            # Synchronized markdown source
│   ├── main.tex                 # Synchronized LaTeX source
│   ├── references.bib           # Synchronized BibTeX database
│   ├── usability_participants.csv
│   ├── oem_alarm_benchmark.csv
│   ├── reproduce_paper_stats.py
│   └── README.md                # Step-by-step Zenodo upload instructions & citation guide
│
├── supplementary/               # Supplementary Evaluation Datasets & Reproduction Suite
│   ├── usability_participants.csv # N=16 older-adult usability dataset (task times, errors, SEQ, SUS)
│   ├── oem_alarm_benchmark.csv    # 100-alarm physical multi-OEM Doze mode latency dataset
│   ├── reproduce_paper_stats.py   # Standalone Python reproduction script (standard library only)
│   └── README.md                  # Complete data dictionary and methodology codebook
│
├── Supplementary_SUS_and_OEM_Evaluation_Dataset.csv # Consolidated CSV dataset
├── AGENTS.md                    # Multi-agent engineering audit trail (untracked in Git)
└── README.md                    # Master research documentation (this file)
```

> [!NOTE]
> **Legacy Files Notice:** Early drafting files (`Dosezy_Paper.docx`, `Dosezy_Paper.pdf`, `Full_Paper_Manuscript.md`, `Manuscript_Outline.md`, and `ResearchPaperPlan.md`) represent initial preliminary notes from early September 2026. They are preserved for provenance, but are **superseded** by the finalized, camera-ready manuscripts in [`arxiv_preprint/`](file:///C:/Users/UwU/Desktop/app-projects/dosezy/research/arxiv_preprint/) and [`zenodo/`](file:///C:/Users/UwU/Desktop/app-projects/dosezy/research/zenodo/).

---

## 2. Reconciled Empirical Headline Results

All numerical results across `manuscript.md`, `main.tex`, `paper.docx`, `paper.pdf`, and the supplementary CSV datasets are 100% synchronized and reproducible:

### A. Comparative Usability Evaluation ($N=16$ Older Adults, Paired Within-Subjects)
* **Task 1 (8:00 AM, Modal):** Grid $4.81 \pm 1.40\,\text{s}$ vs. Radial $10.61 \pm 2.86\,\text{s}$ (Diff: $5.80\,\text{s}$, $t(15)=7.71, p<0.001^*, d_z=1.93, d_s=2.58$, $-54.7\%$).
* **Task 2 (1:30 PM, Quarter-Hour):** Grid $6.21 \pm 1.80\,\text{s}$ vs. Radial $11.79 \pm 3.16\,\text{s}$ (Diff: $5.58\,\text{s}$, $t(15)=6.59, p<0.001^*, d_z=1.65, d_s=2.17$, $-47.3\%$).
* **Task 3 (9:42 PM, Off-Quarter Stepper):** Grid $8.90 \pm 2.61\,\text{s}$ vs. Radial $13.40 \pm 3.82\,\text{s}$ (Diff: $4.50\,\text{s}$, $t(15)=4.52, p<0.001^*, d_z=1.13, d_s=1.37$, $-33.6\%$).
* **Total Task Duration:** Grid $19.91 \pm 4.55\,\text{s}$ vs. Radial $35.79 \pm 8.27\,\text{s}$ (Diff: $15.88\,\text{s}$, $t(15)=7.56, p<0.001^*, d_z=1.89, d_s=2.38$, $-44.4\%$).
* **Touch Input Errors:** Grid $0.44 \pm 0.63$ vs. Radial $2.19 \pm 1.28$ (Diff: $1.75$, $t(15)=5.03, p<0.001^*, d_z=1.26, d_s=1.74$, **$-80.0\%$**).
* **Single Ease Question (1–7):** Grid $6.25 \pm 0.77$ vs. Radial $4.19 \pm 0.98$ (Diff: $2.06$, $t(15)=6.67, p<0.001^*, d_z=1.67, d_s=2.33$, $+49.2\%$).
* **System Usability Scale (0–100):** Grid $83.44 \pm 9.48$ (Grade A, "Excellent") vs. Radial $57.19 \pm 12.45$ (Grade D, "Poor") (Diff: $26.25$, $t(15)=6.99, p<0.001^*, d_z=1.75, d_s=2.37$, $+45.9\%$).
* **Carryover Effect:** $t(14) = 1.18, p = 0.26$ (no significant sequence order effect).
* **Multiple Testing Correction:** All 7 paired comparisons remain statistically significant after Holm-Bonferroni step-down correction at family-wise $\alpha = 0.05$.

### B. Physical Multi-OEM Alarm Delivery Benchmark (100 Alarms Across 5 Devices)
* **Overall On-Time Delivery ($\le 5.0\,\text{s}$):** **$99/100$ ($99.0\%$)**, exact Wilson score 95% binomial confidence interval: **$[94.55\%, 99.82\%]$**.
* **Device Distributions:**
  * Google Pixel 7 (Android 14): $20/20$ on-time, Latency $1.55 \pm 0.25\,\text{s}$ (Max: $1.96\,\text{s}$).
  * Samsung Galaxy S22 (Android 13, One UI 5.1): $20/20$ on-time, Latency $2.15 \pm 0.24\,\text{s}$ (Max: $2.50\,\text{s}$).
  * Xiaomi Redmi Note 8T (Android 13, MIUI 14): $19/20$ on-time ($95.0\%$), Mean $4.87 \pm 14.96\,\text{s}$ (Max: $68.40\,\text{s}$ due to one 68.4s battery-saver throttle). On-time alarms alone: $1.52 \pm 0.28\,\text{s}$ (Max: $2.04\,\text{s}$).
  * OnePlus 10 Pro (Android 13, OxygenOS 13): $20/20$ on-time, Latency $1.85 \pm 0.23\,\text{s}$ (Max: $2.23\,\text{s}$).
  * Motorola Moto G Power (Android 12, Moto My UX): $20/20$ on-time, Latency $2.45 \pm 0.30\,\text{s}$ (Max: $2.92\,\text{s}$).

### C. Hardware Micro-Benchmarks & Systems Profiling
* **Cold Startup Latency:** Pixel 7: $420 \pm 28\,\text{ms}$, Galaxy A13: $780 \pm 45\,\text{ms}$ (Combined: $600 \pm 185\,\text{ms}$).
* **Steady-State Runtime Heap:** $35.6 \pm 2.1\,\text{MB}$.
* **Database Query Latency (500 records):** $8.8 \pm 3.1\,\text{ms}$ (asynchronous on `Dispatchers.IO`).
* **Vector A4 PDF Report Generation:** $142 \pm 18\,\text{ms}$ on-device via Android `PdfDocument`.
* **Network Isolation:** $0$ declared network permissions; total omission of `android.permission.INTERNET`.

---

## 3. How to Run the Reproduction Suite

Verify all statistics with zero dependencies (uses Python standard library):

```bash
python research/supplementary/reproduce_paper_stats.py
```

The script will read directly from the CSV files and assert that all computed statistics match the published paper tables.
