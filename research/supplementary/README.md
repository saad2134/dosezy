# Dosezy Supplementary Evaluation Datasets & Reproduction Package

This directory contains the complete, unaggregated empirical datasets and automated statistical reproduction scripts supporting the research article:

> **"Dosezy: A Local-First, Accessibility-Centric Android System for Medication Scheduling and Reliable Reminder Delivery"**  
> Authors: Saaduddin Mohammad, Md Rahif Uddin Khan, Khwaja Mohammed  
> Affiliation: Department of Computer Science and Engineering, Methodist College of Engineering and Technology, Hyderabad, India

---

## 1. Directory Structure

```text
research/supplementary/
├── usability_participants.csv    # N=16 older-adult within-subjects usability trial dataset
├── oem_alarm_benchmark.csv       # 100-alarm physical multi-OEM Doze mode latency dataset
├── reproduce_paper_stats.py      # Independent automated Python reproduction script
└── README.md                     # Data dictionary, methodology codebook, and reproduction guide
```

---

## 2. Dataset Descriptions & Data Dictionaries

### 2.1 `usability_participants.csv` (Within-Subjects Usability Study, N=16)
Records individual participant characteristics, counterbalanced sequence assignments, task completion times, touch input error counts, Single Ease Question (SEQ) ratings, and System Usability Scale (SUS) scores across Condition A (12-Hour Grid) and Condition B (Material 3 Radial Dial).

| Variable / Column | Data Type | Units / Range | Description |
| :--- | :---: | :---: | :--- |
| `Participant_ID` | String | `P01`–`P16` | De-identified participant code. |
| `Age` | Integer | 61–76 years | Participant age at time of testing ($68.5 \pm 4.8$). |
| `Gender` | String | `Female`, `Male` | Participant self-identified gender (9 Female, 7 Male). |
| `Prescription_Glasses` | String | `Yes`, `No` | Use of corrective eyewear during task execution ($12/16 = 75\%$). |
| `Mild_Hand_Tremor` | String | `Yes`, `No` | Clinically observed mild physiological resting/action tremor ($4/16 = 25\%$). |
| `Counterbalance_Sequence`| String | `Grid-First (AB)`, `Radial-First (BA)` | Counterbalanced condition execution sequence ($n=8$ per sequence). |
| `Grid_Task1_Time_sec` | Float | Seconds | Task 1 (8:00 AM) completion time under Condition A. |
| `Grid_Task2_Time_sec` | Float | Seconds | Task 2 (1:30 PM) completion time under Condition A. |
| `Grid_Task3_Time_sec` | Float | Seconds | Task 3 (9:42 PM) completion time under Condition A. |
| `Grid_Total_Time_sec` | Float | Seconds | Total time across all three tasks ($\sum T_i$) under Condition A. |
| `Grid_Errors` | Integer | Count $\ge 0$ | Total algorithmically detected touch-error events under Condition A. |
| `Grid_SEQ_Score` | Integer | 1–7 scale | Single Ease Question rating (1=Very Difficult, 7=Very Easy). |
| `Grid_SUS_Score` | Float | 0–100 scale | Standardized 10-item System Usability Scale composite score (multiples of 2.5). |
| `Radial_Task1_Time_sec` | Float | Seconds | Task 1 (8:00 AM) completion time under Condition B. |
| `Radial_Task2_Time_sec` | Float | Seconds | Task 2 (1:30 PM) completion time under Condition B. |
| `Radial_Task3_Time_sec` | Float | Seconds | Task 3 (9:42 PM) completion time under Condition B. |
| `Radial_Total_Time_sec` | Float | Seconds | Total time across all three tasks ($\sum T_i$) under Condition B. |
| `Radial_Errors` | Integer | Count $\ge 0$ | Total algorithmically detected touch-error events under Condition B. |
| `Radial_SEQ_Score` | Integer | 1–7 scale | Single Ease Question rating under Condition B. |
| `Radial_SUS_Score` | Float | 0–100 scale | Standardized System Usability Scale composite score under Condition B (multiples of 2.5). |

### 2.2 `oem_alarm_benchmark.csv` (Multi-OEM Doze Benchmark, 100 Alarms)
Records physical device alarm delivery timestamps, latency deviations from scheduled wall-clock time, and delivery statuses across five hardware vendors.

| Variable / Column | Data Type | Units / Range | Description |
| :--- | :---: | :---: | :--- |
| `Test_ID` | String | `T001`–`T100` | Chronological trial identifier (20 alarms per device). |
| `Device_Model` | String | Vendor / Model | Physical test hardware (Pixel 7, Galaxy S22, Redmi Note 8T, OnePlus 10 Pro, Moto G Power). |
| `OS_Distribution` | String | Android Build | Operating system version and vendor UI distribution. |
| `Battery_Saver_Mode` | String | `ENABLED` | Hardware battery conservation state during testing. |
| `Scheduled_Time` | String | `HH:MM:SS` | Wall-clock trigger target programmed via `AlarmManager.setAlarmClock`. |
| `Actual_Trigger_Time` | String | `HH:MM:SS.ss` | Hardware broadcast receiver invocation timestamp recorded via logcat. |
| `Latency_Seconds` | Float | Seconds | Temporal delay: $\text{Trigger\_Time} - \text{Scheduled\_Time}$. |
| `Delivery_Status` | String | Categorical | `ON_TIME (<=5s)` or `DELAYED (>5s - OEM powerkeeper throttle)`. |

---

## 3. Automated Reproduction Instructions

### 3.1 Prerequisites
- Python 3.8 or higher
- Standard scientific packages: `pandas`, `numpy`, `scipy`

Install dependencies:
```bash
pip install pandas numpy scipy
```

### 3.2 Executing the Reproduction Script
From within the `research/supplementary/` directory, execute:
```bash
python reproduce_paper_stats.py
```

The script will independently compute and print:
1. Complete participant demographics (Table 5: Mean age $\pm$ SD, gender, vision, tremor status).
2. Exploratory subgroup descriptive statistics for participants with mild hand tremor ($n=4$).
3. **Table 7:** Exact means, sample standard deviations, paired differences, 95% Student's $t$ confidence intervals, paired $t$-statistics ($df=15$), two-tailed $p$-values, effect sizes ($d_z$ and pooled $d_s$), and Holm-Bonferroni step-down significance verification.
4. **Table 4:** Physical OEM alarm benchmark latency distributions, dual reporting for Xiaomi (all 20 vs. 19 on-time alarms), aggregate delivery success, and the exact Wilson score 95% binomial confidence interval ($[94.55\%, 99.82\%]$).

All printed values match the manuscript with zero manual intervention.
