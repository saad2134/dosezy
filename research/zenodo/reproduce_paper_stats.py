#!/usr/bin/env python3
"""
Dosezy Evaluation Statistics Reproduction Script
Paper: "Dosezy: A Local-First, Accessibility-Centric Android System for Medication Scheduling and Reliable Reminder Delivery"

This script independently reproduces all empirical results, inferential statistics, 
confidence intervals, and effect sizes reported in Tables 4 and 5 of the manuscript.

Requirements:
    python >= 3.8
    pandas >= 1.5.0
    numpy >= 1.20.0
    scipy >= 1.9.0

Usage:
    python reproduce_paper_stats.py
"""

import os
import sys
import numpy as np
import pandas as pd
from scipy import stats

def run_reproduction():
    if hasattr(sys.stdout, 'reconfigure'):
        sys.stdout.reconfigure(encoding='utf-8')

    script_dir = os.path.dirname(os.path.abspath(__file__))
    u_path = os.path.join(script_dir, "usability_participants.csv")
    o_path = os.path.join(script_dir, "oem_alarm_benchmark.csv")

    if not os.path.exists(u_path) or not os.path.exists(o_path):
        print(f"ERROR: Supplementary CSV files not found in {script_dir}")
        sys.exit(1)

    print("=" * 80)
    print("DOSEZY PREPRINT EMPIRICAL EVALUATION REPRODUCTION SUITE")
    print("=" * 80)

    # ---------------------------------------------------------
    # 1. TABLE 5: PARTICIPANT COHORT DEMOGRAPHICS (N=16 Older Adults)
    # ---------------------------------------------------------
    df_u = pd.read_csv(u_path)
    assert len(df_u) == 16, f"Expected 16 participant records, got {len(df_u)}"

    # Strict domain invariant assertions
    assert all(60 <= x <= 100 for x in df_u['Age']), "Age out of range"
    assert all(0 <= x <= 100 for x in df_u['Grid_SUS_Score']), "Grid SUS out of bounds [0, 100]"
    assert all(0 <= x <= 100 for x in df_u['Radial_SUS_Score']), "Radial SUS out of bounds [0, 100]"
    assert all(x % 2.5 == 0 for x in df_u['Grid_SUS_Score']), "Grid SUS not multiple of 2.5"
    assert all(x % 2.5 == 0 for x in df_u['Radial_SUS_Score']), "Radial SUS not multiple of 2.5"
    assert np.allclose(df_u['Grid_Total_Time_sec'], df_u['Grid_Task1_Time_sec'] + df_u['Grid_Task2_Time_sec'] + df_u['Grid_Task3_Time_sec'])
    assert np.allclose(df_u['Radial_Total_Time_sec'], df_u['Radial_Task1_Time_sec'] + df_u['Radial_Task2_Time_sec'] + df_u['Radial_Task3_Time_sec'])

    age_mean = df_u['Age'].mean()
    age_sd = df_u['Age'].std(ddof=1)
    age_min = df_u['Age'].min()
    age_max = df_u['Age'].max()

    n_female = int((df_u['Gender'] == 'Female').sum())
    n_male = int((df_u['Gender'] == 'Male').sum())
    n_glasses = int((df_u['Prescription_Glasses'] == 'Yes').sum())
    n_tremor = int((df_u['Mild_Hand_Tremor'] == 'Yes').sum())
    n_seq_ab = int((df_u['Counterbalance_Sequence'] == 'Grid-First (AB)').sum())
    n_seq_ba = int((df_u['Counterbalance_Sequence'] == 'Radial-First (BA)').sum())

    print("\n[TABLE 5: PARTICIPANT COHORT DEMOGRAPHICS]")
    print(f"  Sample size (N):          {len(df_u)}")
    print(f"  Age (years):              {age_mean:.1f} +- {age_sd:.1f} (range: {age_min}-{age_max})")
    print(f"  Gender:                   Female: {n_female} ({n_female/16*100:.1f}%), Male: {n_male} ({n_male/16*100:.1f}%)")
    print(f"  Prescription Glasses:     Yes: {n_glasses} ({n_glasses/16*100:.1f}%), No: {16-n_glasses} ({(16-n_glasses)/16*100:.1f}%)")
    print(f"  Mild Hand Tremor:         Yes: {n_tremor} ({n_tremor/16*100:.1f}%), No: {16-n_tremor} ({(16-n_tremor)/16*100:.1f}%)")
    print(f"  Counterbalance Sequence:  Grid-First (AB): {n_seq_ab}, Radial-First (BA): {n_seq_ba}")

    # Subgroup descriptive breakdown
    df_tremor = df_u[df_u['Mild_Hand_Tremor'] == 'Yes']
    df_notremor = df_u[df_u['Mild_Hand_Tremor'] == 'No']
    print(f"\n[EXPLORATORY SUBGROUP DESCRIPTIVE OBSERVATIONS (Tremor n=4 vs No-Tremor n=12)]")
    print(f"  Tremor (n=4):    Grid Time = {df_tremor['Grid_Total_Time_sec'].mean():.2f}s, Radial Time = {df_tremor['Radial_Total_Time_sec'].mean():.2f}s | Grid Errors = {df_tremor['Grid_Errors'].mean():.2f}, Radial Errors = {df_tremor['Radial_Errors'].mean():.2f}")
    print(f"  No Tremor (n=12): Grid Time = {df_notremor['Grid_Total_Time_sec'].mean():.2f}s, Radial Time = {df_notremor['Radial_Total_Time_sec'].mean():.2f}s | Grid Errors = {df_notremor['Grid_Errors'].mean():.2f}, Radial Errors = {df_notremor['Radial_Errors'].mean():.2f}")

    # ---------------------------------------------------------
    # 2. TABLE 7: USABILITY STUDY STATISTICAL TESTS (N=16)
    # ---------------------------------------------------------
    print("\n" + "=" * 80)
    print("TABLE 7: COMPARATIVE USABILITY FINDINGS (N=16 Within-Subjects)")
    print("=" * 80)

    metrics = [
        ("Task 1: 8:00 AM (s)", "Grid_Task1_Time_sec", "Radial_Task1_Time_sec", "r-g"),
        ("Task 2: 1:30 PM (s)", "Grid_Task2_Time_sec", "Radial_Task2_Time_sec", "r-g"),
        ("Task 3: 9:42 PM (s)", "Grid_Task3_Time_sec", "Radial_Task3_Time_sec", "r-g"),
        ("Total Time (s)", "Grid_Total_Time_sec", "Radial_Total_Time_sec", "r-g"),
        ("Touch Errors", "Grid_Errors", "Radial_Errors", "r-g"),
        ("SEQ Score (1-7)", "Grid_SEQ_Score", "Radial_SEQ_Score", "g-r"),
        ("SUS Score (0-100)", "Grid_SUS_Score", "Radial_SUS_Score", "g-r"),
    ]

    header_fmt = "{:<22} | {:<12} | {:<12} | {:<18} | {:<15} | {:<6} | {:<6}"
    row_fmt    = "{:<22} | {:<12} | {:<12} | {:<18} | {:<15} | {:<6} | {:<6}"
    print(header_fmt.format("Metric", "12-Hour Grid", "Radial Dial", "Mean Diff [95% CI]", "Paired t-Test", "d_z", "d_s"))
    print("-" * 105)

    p_values = []
    table5_results = {}

    for name, g_col, r_col, direction in metrics:
        g = df_u[g_col].values
        r = df_u[r_col].values
        diff = (r - g) if direction == "r-g" else (g - r)

        mg, sg = np.mean(g), np.std(g, ddof=1)
        mr, sr = np.mean(r), np.std(r, ddof=1)
        md, sd = np.mean(diff), np.std(diff, ddof=1)

        t_stat, p_val = stats.ttest_rel(r, g) if direction == "r-g" else stats.ttest_rel(g, r)
        p_values.append(p_val)

        d_z = md / sd
        d_s = md / np.sqrt((sg**2 + sr**2) / 2.0)
        ci = stats.t.interval(0.95, len(diff) - 1, loc=md, scale=stats.sem(diff))

        grid_str = f"{mg:.2f} +- {sg:.2f}"
        radial_str = f"{mr:.2f} +- {sr:.2f}"
        diff_str = f"{md:.2f} [{ci[0]:.2f}, {ci[1]:.2f}]"
        t_str = f"t(15)={t_stat:.2f}, p<0.001*" if p_val < 0.001 else f"t(15)={t_stat:.2f}, p={p_val:.4f}"

        print(row_fmt.format(name, grid_str, radial_str, diff_str, t_str, f"{d_z:.2f}", f"{d_s:.2f}"))
        table5_results[name] = {
            'grid_mean': mg, 'grid_sd': sg,
            'radial_mean': mr, 'radial_sd': sr,
            'diff_mean': md, 'ci_lower': ci[0], 'ci_upper': ci[1],
            't_stat': t_stat, 'p_val': p_val,
            'd_z': d_z, 'd_s': d_s
        }

    # Holm-Bonferroni correction verification
    p_sorted_indices = np.argsort(p_values)
    m = len(p_values)
    print("\n[HOLM-BONFERRONI STEP-DOWN CORRECTION VERIFICATION]")
    for rank, idx in enumerate(p_sorted_indices):
        alpha_corrected = 0.05 / (m - rank)
        is_sig = p_values[idx] < alpha_corrected
        print(f"  Rank {rank+1}: p = {p_values[idx]:.2e} < alpha_adj = {alpha_corrected:.4f} -> Significant: {is_sig}")

    # ---------------------------------------------------------
    # 3. TABLE 4: MULTI-OEM ALARM BENCHMARK (100 Alarms)
    # ---------------------------------------------------------
    print("\n" + "=" * 80)
    print("TABLE 4: MULTI-OEM DOZE MODE ALARM BENCHMARK (100 Alarms Across 5 Devices)")
    print("=" * 80)

    df_o = pd.read_csv(o_path)
    assert len(df_o) == 100, f"Expected 100 alarm trials, got {len(df_o)}"

    devices = [
        "Google Pixel 7",
        "Samsung Galaxy S22",
        "Xiaomi Redmi Note 8T",
        "OnePlus 10 Pro",
        "Motorola Moto G Power"
    ]

    header_fmt4 = "{:<25} | {:<12} | {:<15} | {:<12} | {:<12}"
    row_fmt4    = "{:<25} | {:<12} | {:<15} | {:<12} | {:<12}"
    print(header_fmt4.format("Device Model", "Alarms Tested", "On-Time (<=5s)", "Mean Latency (s)", "Max Latency (s)"))
    print("-" * 85)

    all_latencies = df_o['Latency_Seconds'].values

    for dev in devices:
        sub = df_o[df_o['Device_Model'] == dev]['Latency_Seconds'].values
        assert len(sub) == 20, f"{dev}: expected 20 alarms, got {len(sub)}"
        m_dev = np.mean(sub)
        sd_dev = np.std(sub, ddof=1)
        max_dev = np.max(sub)
        ontime_dev = int(np.sum(sub <= 5.0))

        if dev == "Xiaomi Redmi Note 8T":
            # Dual reporting: all 20 alarms vs 19 on-time alarms
            sub_ontime = sub[sub <= 5.0]
            m_ontime = np.mean(sub_ontime)
            sd_ontime = np.std(sub_ontime, ddof=1)
            print(row_fmt4.format(f"{dev} (All 20)", "20", f"{ontime_dev}/20 ({ontime_dev/20*100:.1f}%)", f"{m_dev:.2f} +- {sd_dev:.2f}", f"{max_dev:.2f}"))
            print(row_fmt4.format("  |-- On-Time Only (19)", "19", "19/19 (100.0%)", f"{m_ontime:.2f} +- {sd_ontime:.2f}", f"{np.max(sub_ontime):.2f}"))
        else:
            print(row_fmt4.format(dev, "20", f"{ontime_dev}/20 ({ontime_dev/20*100:.1f}%)", f"{m_dev:.2f} +- {sd_dev:.2f}", f"{max_dev:.2f}"))

    # Overall Aggregate
    ontime_all = int(np.sum(all_latencies <= 5.0))
    m_all = np.mean(all_latencies)
    sd_all = np.std(all_latencies, ddof=1)
    max_all = np.max(all_latencies)

    # Excl. Outlier
    ontime_latencies = all_latencies[all_latencies <= 5.0]
    m_excl = np.mean(ontime_latencies)
    sd_excl = np.std(ontime_latencies, ddof=1)

    print("-" * 85)
    print(row_fmt4.format("Aggregate All 100 Alarms", "100", f"{ontime_all}/100 ({ontime_all/100*100:.1f}%)", f"{m_all:.2f} +- {sd_all:.2f}", f"{max_all:.2f}"))
    print(row_fmt4.format("  |-- Excl. Outlier (99)", "99", "99/99 (100.0%)", f"{m_excl:.2f} +- {sd_excl:.2f}", f"{np.max(ontime_latencies):.2f}"))

    # Wilson Score 95% Confidence Interval for 99/100
    p = ontime_all / 100.0
    n = 100.0
    z = 1.95996  # 95% CI
    wilson_center = (p + (z**2) / (2 * n)) / (1 + (z**2) / n)
    wilson_margin = (z / (1 + (z**2) / n)) * np.sqrt((p * (1 - p) / n) + ((z**2) / (4 * (n**2))))
    wilson_lower = (wilson_center - wilson_margin) * 100.0
    wilson_upper = (wilson_center + wilson_margin) * 100.0

    print(f"\n[WILSON SCORE 95% BINOMIAL CONFIDENCE INTERVAL]")
    print(f"  Success rate: {ontime_all}/100 = {p*100:.1f}%")
    print(f"  Wilson 95% CI: [{wilson_lower:.2f}%, {wilson_upper:.2f}%]")

    print("\n" + "=" * 80)
    print(">>> ALL REPRODUCED METRICS CALCULATED DIRECTLY FROM SUPPLEMENTARY CSVs! <<<")
    print("=" * 80)

if __name__ == "__main__":
    run_reproduction()
