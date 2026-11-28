
import re
from dataclasses import dataclass


@dataclass
class SafetyDecision:
    allowed: bool
    category: str
    response: str


SAFE_RESPONSE = (
    "I can't provide medical diagnoses, treatment recommendations, "
    "or medication instructions. Please consult your licensed doctor "
    "or pharmacist for guidance. I can help with Dosezy app features "
    "and medication reminder settings."
)

EMERGENCY_RESPONSE = (
    "This may be a medical emergency. Please contact your local "
    "emergency services immediately or seek urgent medical care. "
    "Do not rely on this app for emergency medical advice."
)


def normalize_text(message: str) -> str:
    """Normalize common leetspeak and spelling variations."""
    text = message.lower()

    substitutions = {
        "3": "e",
        "0": "o",
        "1": "i",
        "4": "a",
        "5": "s",
        "7": "t",
    }

    def normalize_word(match):
        word = match.group(0)

        if not word[0].isalpha():
            return word

        for source, target in substitutions.items():
            word = word.replace(source, target)

        return word

    text = re.sub(r"\b[a-z0-9]+\b", normalize_word, text)

    spelling_variants = {
        "difficvlty": "difficulty",
        "brething": "breathing",
        "unconsious": "unconscious",
        "collapsd": "collapsed",
    }

    for wrong, correct in spelling_variants.items():
        text = re.sub(rf"\b{wrong}\b", correct, text)

    return text


# Detect urgent medical situations first.
EMERGENCY_PATTERNS = [
    r"\b(chest pain|chest pressure|chest tightness)\b",
    r"\b(can't breathe|cannot breathe|difficulty breathing|trouble breathing)\b",
    r"\b(dyspnea|shortness of breath|breathlessness)\b",
    r"\b(overdose|overdosed|poisoning|poisoned)\b",
    r"\b(unconscious|not responding|unresponsive|severe bleeding)\b",
    r"\b(seizure|stroke symptoms|suicidal thoughts|suicide attempt)\b",
    r"\b(collapsed|collapse)\b.{0,70}\b(unconscious|unresponsive|not responding)\b",
    r"\b(unconscious|unresponsive|not responding)\b.{0,70}\b(collapsed|collapse)\b",
    r"\b(face drooping|sudden weakness|slurred speech)\b",
    r"\b(cannot wake|can't wake|stopped breathing)\b",
    r"\b(hard to wake|difficult to wake|unable to wake)\b",
]


# Detect diagnosis and symptom interpretation requests.
DIAGNOSIS_PATTERNS = [
    r"\b(diagnose|diagnosis|what disease|which disease)\b",
    r"\b(what is wrong with me|what illness do i have)\b",
    r"\b(symptoms of|cause of my symptoms|interpret my symptoms)\b",
    r"\b(do i have|am i suffering from)\b",
    r"\b(what illness|what condition|what infection)\b",
    r"\b(cephalalgia|tachycardia|bradycardia|hypertension|hypotension)\b",
    r"\b(i have|i am having)\b.{0,50}\b(fever|headache|pain|rash|cough|dizziness)\b.{0,50}\b(what|which|illness|disease|condition)\b",
]


# Detect medication instructions and prescription changes.
MEDICATION_PATTERNS = [
    r"\b(change|increase|decrease|reduce|double|skip|stop|start)\b.{0,60}\b(dose|dosage|medicine|medication|prescription|tablet|pill|insulin)\b",
    r"\b(take|consume|inject)\b.{0,40}\b(extra|more|less|another|double)\b.{0,30}\b(dose|tablet|pill|medicine|medication)\b",
    r"\b(missed|forgot)\b.{0,30}\b(dose|medicine|medication|tablet)\b",
    r"\b(when should i take|how much should i take|what dose should i take)\b",
    r"\b(replace|substitute|switch)\b.{0,30}\b(medicine|medication|drug|prescription)\b",
    r"\b(how much|what amount|what dose)\b.{0,35}\b(insulin|medicine|medication|drug|tablet)\b",
    r"\b(insulin)\b.{0,40}\b(take|inject|dose|dosage|amount)\b",
    r"\b(should i take)\b.{0,40}\b(mg|milligram|tablet|pill|dose)\b",
    r"\b(hypothetically|in theory|for a story|roleplay|pretend)\b.{0,120}\b(dose|dosage|medication|medicine|tablet|pill)\b",
    r"\b(recommend|suggest|change|increase|decrease|double)\b.{0,50}\b(dose|dosage|medication|medicine)\b",
]


# Detect clinical treatment and interaction requests.
CLINICAL_PATTERNS = [
    r"\b(drug interaction|medicine interaction|medication interaction|interactions between)\b",
    r"\b(can i take)\b.{0,50}\b(with|together|both|medicine|medication|drug)\b",
    r"\b(side effects|adverse effects|contraindications)\b",
    r"\b(treatment|cure|therapy)\b.{0,40}\b(for|of|my)\b",
    r"\b(should i take|should i use)\b.{0,40}\b(medicine|medication|drug|antibiotic|painkiller)\b",
    r"\b(recommend|prescribe)\b.{0,30}\b(medicine|medication|drug|antibiotic|painkiller)\b",
    r"\b(prescribe|prescription)\b.{0,40}\b(painkiller|medicine|medication|drug|antibiotic)\b",
    r"\b(what medicines|which medicines|what drugs|which drugs)\b.{0,50}\b(together|combine|mix)\b",
    r"\b(ignore|override|disregard)\b.{0,35}\b(doctor|medical advice|prescription)\b",
]


def matches_any(text: str, patterns: list[str]) -> bool:
    """Return True if any safety pattern matches the supplied text."""
    return any(
        re.search(pattern, text, re.IGNORECASE)
        for pattern in patterns
    )


def check_medical_safety(message: str) -> SafetyDecision:
    """Apply rule-based medical safety checks before documentation retrieval."""

    if not isinstance(message, str) or not message.strip():
        return SafetyDecision(
            allowed=False,
            category="invalid_input",
            response="Please enter a valid message.",
        )

    text = normalize_text(message.strip())

    # Emergency checks have the highest priority.
    if matches_any(text, EMERGENCY_PATTERNS):
        return SafetyDecision(
            allowed=False,
            category="emergency",
            response=EMERGENCY_RESPONSE,
        )

    if matches_any(text, DIAGNOSIS_PATTERNS):
        return SafetyDecision(
            allowed=False,
            category="diagnosis",
            response=SAFE_RESPONSE,
        )

    if matches_any(text, MEDICATION_PATTERNS):
        return SafetyDecision(
            allowed=False,
            category="medication_instruction",
            response=SAFE_RESPONSE,
        )

    if matches_any(text, CLINICAL_PATTERNS):
        return SafetyDecision(
            allowed=False,
            category="clinical_advice",
            response=SAFE_RESPONSE,
        )

    return SafetyDecision(
        allowed=True,
        category="safe",
        response="This request can proceed to the documentation retrieval system.",
    )
