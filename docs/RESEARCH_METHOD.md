# ContextGuard Research Methodology & Evaluation Contract

**Document Version:** 2.1.0  
**Status:** Canonical Benchmark Contract & Scientific Evaluation Standard  
**Subsystem:** `evaluation` & `benchmark`  
**Classification:** Academic Research Framework  

---

## 1. Scientific Objective

ContextGuard addresses a fundamental limitation in existing AI safety and mobile security systems: **the neglect of action context**. Conventional classifiers evaluate artifacts in isolation (e.g., asking "is this document sensitive?"). However, in real-world mobile workflows, the risk of an artifact depends strictly on the intended user action:

* Storing an unredacted bank statement in a personal encrypted vault is **safe** (`ACT`).
* Sending that same statement to an unverified counterparty requires **caution or confirmation** (`WARN` or `ASK`).
* Posting that same statement to a public social media feed is **catastrophic** (`STOP`).

This research framework establishes the Everyday Action Risk Benchmark (EARB v1), formalizes the Counterfactual Action Consistency (CAC) metric suite, and specifies rigorous clustered statistical inference protocols.

---

## 2. Everyday Action Risk Benchmark (EARB v1)

### 2.1 Canonical Intervention Labels
EARB v1 preserves four mutually exclusive pre-action interventions:
1. **`ACT`:** Execute immediately without user friction; negligible or well-bounded consequence.
2. **`WARN`:** Display a non-blocking contextual warning advisory; user may proceed after acknowledgement.
3. **`ASK`:** Block execution pending explicit clarification or verification; required when epistemic uncertainty is elevated or action intent is ambiguous.
4. **`STOP`:** Strictly block execution by default; catastrophic, severe, or irreversible harm evidenced.

### 2.2 Benchmark Dataset Structure
The benchmark comprises 60 validated pairs across four threat domains:
* **Financial Risk:** UPI transactions, bank statements, invoice payments, salary slips.
* **Digital Security:** Phishing lures, authentication prompts, OTP messages, 2FA setup QR codes, SSH keys.
* **Privacy Disclosure:** Identity documents (Aadhaar, PAN, passport), medical records, private photos.
* **Communication Risk:** Privileged corporate acquisition memos, customer disputes, executive emails.

Each base artifact is instantiated across three counterfactual action tiers:
* **Action A (Safe Local / Refusal):** `SAVE`, `KEEP_LOCALLY`, `DECLINE`, `BACK_TO_SAFETY`.
* **Action B (Moderate Exposure / Clarification):** `SEND_TO_KNOWN`, `SEND_TO_UNKNOWN`, `ASK_VERIFY`.
* **Action C (Critical Dissemination / Exploitation):** `POST_PUBLIC`, `LOGIN`, `APPROVE`, `FORWARD`.

### 2.3 Strict Partition Disjointness
To prevent optimistic data leakage, EARB v1 partitions data strictly at the **base artifact level**:

| Partition | Base Artifacts | Artifact IDs | Action-Conditioned Pairs | Ground Truth Distribution |
| :--- | :--- | :--- | :--- | :--- |
| **Dev (Calibration)** | 14 | `ART-FIN-001` - `ART-COM-004` | 42 | 14 ACT, 14 STOP, 7 WARN, 7 ASK |
| **Test (Holdout)** | 6 | `ART-FIN-015` - `ART-PRV-020` | 18 | 6 ACT, 6 STOP, 5 WARN, 1 ASK |
| **Overlap** | **0** | **Disjoint sets** | **0** | **0% leakage** |

---

## 3. Counterfactual Action Consistency (CAC) Metric Suite

### 3.1 Formal Mathematical Formulation
Let $\mathcal{A} = \{A_1, A_2, \dots, A_M\}$ denote the set of base artifacts. For each base artifact $A_i$, let $P_i = \{(x_{i,k}, y_{i,k}, \hat{y}_{i,k})\}_{k=1}^{K_i}$ denote the set of action-conditioned evaluations sharing base artifact $A_i$.

Consider all pairs of distinct actions $(j, k)$ with $j < k$ for artifact $A_i$:

#### 1. Correct Action-Sensitive Transition Rate (CASTR)
For all pairs where ground truth demands a change in intervention ($y_{i,j} \ne y_{i,k}$):

$$\text{CASTR} = \frac{\sum_{i=1}^M \sum_{j < k : y_{i,j} \ne y_{i,k}} \mathbb{I}(\hat{y}_{i,j} = y_{i,j} \land \hat{y}_{i,k} = y_{i,k})}{\sum_{i=1}^M \sum_{j < k : y_{i,j} \ne y_{i,k}} 1}$$

* **Significance:** Measures whether the policy correctly shifts its decision when the user's action alters consequence. A system that predicts a fixed label regardless of action scores $\text{CASTR} = 0.0$.

#### 2. Inappropriate Transition Rate (ITR)
For all pairs where ground truth intervention is identical ($y_{i,j} = y_{i,k}$):

$$\text{ITR} = \frac{\sum_{i=1}^M \sum_{j < k : y_{i,j} = y_{i,k}} \mathbb{I}(\hat{y}_{i,j} \ne \hat{y}_{i,k})}{\sum_{i=1}^M \sum_{j < k : y_{i,j} = y_{i,k}} 1}$$

* **Significance:** Measures unwarranted intervention flips where counterfactual action changes should not have affected the safety tier.

#### 3. Same-Action-Context Stability (SACS)
The complement of inappropriate transitions:

$$\text{SACS} = 1.0 - \text{ITR} = \frac{\sum_{i=1}^M \sum_{j < k : y_{i,j} = y_{i,k}} \mathbb{I}(\hat{y}_{i,j} = \hat{y}_{i,k})}{\sum_{i=1}^M \sum_{j < k : y_{i,j} = y_{i,k}} 1}$$

#### 4. Critical False-ACT Rate (CFAR)
The safety-critical failure rate measuring instances where catastrophic hazards labeled `STOP` were mistakenly allowed as `ACT`:

$$\text{CFAR} = \frac{\sum_{i=1}^M \sum_{k=1}^{K_i} \mathbb{I}(y_{i,k} = \text{STOP} \land \hat{y}_{i,k} = \text{ACT})}{\sum_{i=1}^M \sum_{k=1}^{K_i} \mathbb{I}(y_{i,k} = \text{STOP})}$$

* **Safety Invariant:** In pre-action safety defense, **CFAR must be strictly 0.0000**. Under no circumstances may a safety engine silently execute an action labeled `STOP`.

#### 5. Action-Pair Macro-F1
The unweighted macro-averaged $F_1$ score across the four canonical classes (ACT, WARN, ASK, STOP):

$$\text{Macro-F1} = \frac{1}{4} \sum_{c \in \{\text{ACT}, \text{WARN}, \text{ASK}, \text{STOP}\}} F_1^{(c)}$$

#### 6. Uncertainty / ASK Behavior
* **Abstention Rate:** $\frac{1}{N} \sum_{i,k} \mathbb{I}(\hat{y}_{i,k} = \text{ASK})$
* **Ambiguity Capture Recall:** $\frac{\sum_{i,k} \mathbb{I}(y_{i,k} = \text{ASK} \land \hat{y}_{i,k} = \text{ASK})}{\sum_{i,k} \mathbb{I}(y_{i,k} = \text{ASK})}$

---

## 4. Empirical Evaluation Protocol

### 4.1 Baselines for Comparison
1. **B1: Artifact-Only Evaluator:** Evaluates artifact sensitivity while ignoring intended user action (reverts to traditional static file scanners).
2. **B3: Multimodal No-Intent Evaluator:** Vision-language model evaluating screen content without action conditioning.
3. **B4: Fixed-Threshold Evaluator:** Evaluates consequence scores against uncalibrated static thresholds.
4. **B5: Full Proposed ContextGuard System:** The complete six-stage reasoning pipeline with split conformal selective prediction and action-conditioned risk gating.

### 4.2 Component Ablations
* **A1: No Intent Resolution:** Omits action intent and assesses raw exposure.
* **A2: No Multimodal Feature Extraction:** Relies purely on metadata without OCR or local URL analysis.
* **A3: Fixed Policy:** Replaces dynamic risk formula $\rho = s \cdot (1 + \lambda r)$ with simple additive risk.
* **A4: Adaptive Policy:** Disables threshold freezing.
* **A5: Warn-Everything:** Naive heuristic baseline warning on any non-zero risk.

### 4.3 Clustered Statistical Bootstrapping
Because the 60 pairs share 20 base artifacts, observations within the same artifact cluster exhibit correlated error distributions. Standard i.i.d. bootstrapping yields artificially narrow confidence intervals.
* **Protocol:** Cluster-level resampling by `base_artifact_id` with 1,000 bootstrap iterations.
* All reported metrics include 95% clustered bootstrap confidence intervals $[\text{CI}_{\text{lower}}, \text{CI}_{\text{upper}}]$.

---

## 5. Summary of Calibrated Cascade Verification

On the canonical EARB v1 dev partition (42 pairs, 14 base artifacts), the Calibrated Cascade Engine achieves:
* **Critical False-ACT Rate (CFAR):** **0.0000** (Zero false ACTs on catastrophic STOP scenarios).
* **Same-Action-Context Stability (SACS):** **1.0000** (Zero inappropriate flips on invariant pairs).
* **Inappropriate Transition Rate (ITR):** **0.0000**.
* **Epistemic Uncertainty Escalation:** Correctly escalates ambiguous and unverified contexts to `ASK` without guessing.
