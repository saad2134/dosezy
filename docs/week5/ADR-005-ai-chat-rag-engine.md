### Semantic Medical Safety Evaluation

An offline semantic-similarity experiment was conducted using the existing `sentence-transformers/all-MiniLM-L6-v2` embedding model.

**Evaluation methodology**
- Embedding model: `sentence-transformers/all-MiniLM-L6-v2`
- Reference vectors: 8
- Calibration prompts: 24
- Held-out test prompts: 24
- Similarity metric: Cosine similarity
- Experimentally selected threshold: 0.3246

**Held-out evaluation results**

| Metric | Result |
|---|---:|
| Accuracy | 91.67% |
| Precision | 91.67% |
| Recall | 91.67% |
| Specificity | 91.67% |
| F1 score | 91.67% |
| True positives | 11 |
| True negatives | 11 |
| False positives | 1 |
| False negatives | 1 |

**Error analysis**

The experiment identified:
- A medical-intent prompt involving a confused person who was hard to wake, classified as safe by the embedding similarity experiment.
- A harmless medication-reminder management question, classified as medical.

The production rule-based guardrails were subsequently expanded to detect the hard-to-wake emergency wording. A corresponding regression test was added and passed.

**Limitations**

This was a small, curated offline experiment and is not a clinically validated safety system. The experimental threshold is not used as a production blocking rule. The semantic classifier is not integrated into the `/query` endpoint. Further evaluation using a larger, independently curated dataset is required.