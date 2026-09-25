# Dosezy: Zenodo Open-Access Preprint & Replication Package

This directory provides the complete, self-contained open-science preprint deposition bundle for:

> **"Dosezy: A Local-First, Accessibility-Centric Android System for Medication Scheduling and Reliable Reminder Delivery"**  
> **Authors:** Saaduddin Mohammad, Md Rahif Uddin Khan, Khwaja Mohammed  
> **Affiliation:** Department of Computer Science and Engineering, Methodist College of Engineering and Technology, Hyderabad, India  
> **Repository:** [https://github.com/saad2134/dosezy](https://github.com/saad2134/dosezy)  
> **License:** Creative Commons Attribution 4.0 International (CC-BY 4.0)

---

## 1. Package Contents

This package bundles all artifacts required for permanent open-access archiving and independent replication:

| File | Size | Description |
| :--- | :---: | :--- |
| **`paper.pdf`** | ~$635\,\text{KB}$ | Camera-ready publication PDF (27 pages, native 2D equations, vector graphics, active hyperlinks). |
| **`paper.docx`** | ~$648\,\text{KB}$ | Formatted Microsoft Word manuscript with native OpenXML Math equations. |
| **`manuscript.md`** | ~$86\,\text{KB}$ | Self-contained preprint source in GitHub-Flavored Markdown. |
| **`main.tex`** | ~$88\,\text{KB}$ | Complete LaTeX article source code. |
| **`references.bib`** | ~$9.5\,\text{KB}$ | 26 verified academic BibTeX citations with DOIs/URLs. |
| **`usability_participants.csv`** | ~$1.8\,\text{KB}$ | De-identified participant-level usability dataset ($N=16$, task times, errors, SEQ, SUS). |
| **`oem_alarm_benchmark.csv`** | ~$9.4\,\text{KB}$ | 100-alarm physical multi-OEM Doze mode latency dataset across 5 hardware vendors. |
| **`reproduce_paper_stats.py`** | ~$10.6\,\text{KB}$ | Single-command Python reproduction script (standard library only, zero external dependencies). |
| **`zenodo.json`** | ~$2.0\,\text{KB}$ | Machine-readable Zenodo deposition metadata schema for automatic field population. |
| **`dosezy_zenodo_bundle.zip`** | ~$1.3\,\text{MB}$ | Compressed all-in-one archive containing all files above for one-click upload. |

---

## 2. How to Upload to Zenodo (Step-by-Step Guide)

### Method A: Web Upload (Recommended — Takes ~2 Minutes)
1. Navigate to **[zenodo.org](https://zenodo.org/)** and log in (or create an account with GitHub / ORCID).
2. Click the **Upload** button in the top navigation bar, then click **New Upload**.
3. **Files:** Drag and drop `dosezy_zenodo_bundle.zip` (or drag in `paper.pdf`, `usability_participants.csv`, `oem_alarm_benchmark.csv`, and `reproduce_paper_stats.py` individually).
4. **Upload Type:** Select **Publication** $\rightarrow$ **Preprint** (or **Working Paper**).
5. **Basic Information:**
   - **Title:** `Dosezy: A Local-First, Accessibility-Centric Android System for Medication Scheduling and Reliable Reminder Delivery`
   - **Authors:**
     - Mohammad, Saaduddin (Methodist College of Engineering and Technology, Hyderabad, India)
     - Khan, Md Rahif Uddin (Methodist College of Engineering and Technology, Hyderabad, India)
     - Mohammed, Khwaja (Methodist College of Engineering and Technology, Hyderabad, India)
   - **Description:** Copy the abstract from `paper.pdf` or use the pre-formatted text in `zenodo.json`.
   - **License:** Select **Creative Commons Attribution 4.0 International (CC-BY 4.0)**.
   - **Keywords:** `Geriatric Accessibility`, `Human-Computer Interaction`, `Local-First Software`, `Medication Adherence`, `Android AlarmManager`, `Mobile Systems Engineering`, `Doze Mode`, `Fitts' Law`, `System Usability Scale`.
6. Click **Save**, then click **Publish**.
7. Zenodo will immediately generate a persistent Digital Object Identifier (DOI), e.g., `10.5281/zenodo.XXXXXXX`.

### Method B: Automated GitHub-Zenodo Integration
If this repository is hosted on GitHub:
1. Connect your GitHub repository to Zenodo at [zenodo.org/account/settings/github/](https://zenodo.org/account/settings/github/).
2. Enable the toggle for `saad2134/dosezy`.
3. Create a GitHub Release (e.g., tag `v1.0.0-preprint`). Zenodo will read `zenodo.json` automatically, archive the repository release, and mint a DOI.

---

## 3. How to Cite This Preprint

Once published, this work can be cited in BibTeX as:

```bibtex
@article{mohammad2026dosezy,
  title     = {Dosezy: A Local-First, Accessibility-Centric Android System for Medication Scheduling and Reliable Reminder Delivery},
  author    = {Mohammad, Saaduddin and Khan, Md Rahif Uddin and Mohammed, Khwaja},
  journal   = {Zenodo},
  year      = {2026},
  doi       = {10.5281/zenodo.XXXXXXX},
  url       = {https://doi.org/10.5281/zenodo.XXXXXXX},
  note      = {Preprint}
}
```

---

## 4. Verification & Computational Reproduction

To independently verify all empirical numbers reported in the preprint:
```bash
python reproduce_paper_stats.py
```
This script executes with Python's standard library (no pip packages needed) and verifies:
- Table 5 participant demographics ($N=16$, age $68.5 \pm 4.8$) and tremor subgroup observations ($n=4$).
- Table 7 usability findings (Total time $19.91 \pm 4.55\,\text{s}$ vs. $35.79 \pm 8.27\,\text{s}$, $t(15)=7.56, p<0.001^*$; errors $0.44 \pm 0.63$ vs. $2.19 \pm 1.28$, $-80.0\%$; SUS $83.44 \pm 9.48$ vs. $57.19 \pm 12.45$, $t(15)=6.99, p<0.001^*$; Holm-Bonferroni correction).
- Table 4 physical OEM Doze alarm benchmark ($99/100$ on-time, Wilson 95% CI $[94.55\%, 99.82\%]$).
