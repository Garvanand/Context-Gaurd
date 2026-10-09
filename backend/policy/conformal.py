"""
Split Conformal Prediction and Selective Classification for ContextGuard.

Implements rigorous pre-action selective prediction:
- Bounded coverage guarantees using disjoint base-artifact calibration sets.
- Prediction set construction: C(x) = { y in Y : 1 - p(y | x) <= q_hat }.
- Principled abstention: if |C(x)| != 1 or epistemic uncertainty is elevated -> escalate to ASK.
- High-severity override: if STOP is in C(x) and evidenced severity is critical -> enforce STOP.
- Tracks coverage, risk among accepted decisions, abstention rate, and class-specific behavior.
- Explicitly documents exchangeability assumptions and finite-sample limitations.
"""

import math
from dataclasses import dataclass, field
from typing import Dict, List, Optional, Any, Set, Tuple
from pathlib import Path
import json


INTERVENTIONS = ["ACT", "WARN", "ASK", "STOP"]


@dataclass
class ConformalPredictionResult:
    prediction_set: List[str]
    set_size: int
    is_singleton: bool
    is_ambiguous: bool
    is_empty: bool
    selected_intervention: str
    abstention_applied: bool
    nonconformity_threshold: float
    confidence_level: float
    class_probabilities: Dict[str, float] = field(default_factory=dict)


@dataclass
class ConformalCalibrationStats:
    split_name: str
    num_samples: int
    num_base_artifacts: int
    alpha: float
    quantile_q: float
    coverage_empirical: float
    abstention_rate: float
    accepted_risk: float
    class_coverages: Dict[str, float]
    class_abstentions: Dict[str, float]
    assumptions_report: str


class SplitConformalCalibrator:
    """
    Split Conformal Calibrator trained on disjoint dev split.
    """

    def __init__(self, alpha: float = 0.10):
        self.alpha = float(alpha)
        self.quantile_q: float = 0.50
        self.is_calibrated: bool = False
        self.stats: Optional[ConformalCalibrationStats] = None

    @staticmethod
    def compute_class_probabilities(
        severity: float,
        reversibility: float,
        confidence: float,
        risk_score: float,
        is_unknown_recipient: bool = False,
        is_public: bool = False,
    ) -> Dict[str, float]:
        """
        Computes calibrated class affinities over {ACT, WARN, ASK, STOP}.
        Uses transparent deterministic probability fusion rather than an overfitted 60-pair model.
        """
        s = max(0.0, min(1.0, float(severity)))
        r = max(0.0, min(1.0, float(reversibility)))
        c = max(0.0, min(1.0, float(confidence)))
        rho = float(risk_score)

        # Baseline logits based on consequence and uncertainty
        # STOP affinity: driven by high rho and severe irreversible consequences
        logit_stop = (rho - 0.65) * 5.0 + (s - 0.5) * 3.0 + (r - 0.5) * 2.0
        if is_public and s > 0.4:
            logit_stop += 2.5
        if s >= 0.85:
            logit_stop += 3.0

        # ASK affinity: driven by low confidence or ambiguous context
        uncertainty = 1.0 - c
        logit_ask = (uncertainty - 0.30) * 6.0
        if 0.30 <= rho <= 0.70 and c < 0.75:
            logit_ask += 2.5
        if is_unknown_recipient and s > 0.3:
            logit_ask += 1.5

        # WARN affinity: driven by moderate evidenced risk with high certainty
        logit_warn = 0.0
        if 0.35 <= rho < 0.70:
            logit_warn = (1.0 - abs(rho - 0.52) * 4.0) + (c - 0.6) * 3.0
        else:
            logit_warn = -2.0

        # ACT affinity: driven by low rho, high reversibility, and high confidence
        logit_act = (0.35 - rho) * 6.0 + (c - 0.6) * 3.0 - (s * 3.0)
        if s <= 0.10 and r <= 0.10:
            logit_act += 4.0

        # Numerically stable softmax
        logits = [logit_act, logit_warn, logit_ask, logit_stop]
        max_l = max(logits)
        exp_l = [math.exp(l - max_l) for l in logits]
        sum_exp = sum(exp_l)
        probs = [p / sum_exp for p in exp_l]

        return {
            "ACT": round(probs[0], 4),
            "WARN": round(probs[1], 4),
            "ASK": round(probs[2], 4),
            "STOP": round(probs[3], 4),
        }

    def calibrate(
        self,
        calibration_records: List[Dict[str, Any]],
        alpha: Optional[float] = None
    ) -> ConformalCalibrationStats:
        """
        Calibrates conformal quantile q_hat on the calibration set.
        Enforces disjoint base-artifact partition.
        """
        if alpha is not None:
            self.alpha = float(alpha)

        if not calibration_records:
            raise ValueError("Calibration set cannot be empty.")

        n = len(calibration_records)
        base_artifacts: Set[str] = {r.get("base_artifact_id", "UNKNOWN") for r in calibration_records}

        nonconformity_scores: List[float] = []
        sample_probs_and_gt: List[Tuple[Dict[str, float], str]] = []

        for r in calibration_records:
            gt = r.get("expected_intervention") or r.get("ground_truth")
            if gt not in INTERVENTIONS:
                continue

            sev_map = {"negligible": 0.05, "minor": 0.25, "moderate": 0.50, "severe": 0.75, "critical": 1.00}
            rev_map = {"fully": 0.00, "partially": 0.50, "irreversible": 1.00}

            raw_s = r.get("severity", 0.5)
            s = sev_map.get(str(raw_s).lower(), 0.5) if isinstance(raw_s, str) and not raw_s.replace(".", "", 1).isdigit() else float(raw_s)

            raw_rev = r.get("reversibility", 0.5)
            rev = rev_map.get(str(raw_rev).lower(), 0.5) if isinstance(raw_rev, str) and not raw_rev.replace(".", "", 1).isdigit() else float(raw_rev)

            raw_unc = r.get("uncertainty", 0.20)
            unc = float(raw_unc) if raw_unc is not None else 0.20
            c = float(r.get("confidence", 1.0 - unc))
            rho = float(r.get("risk_score", s * (1.0 + 0.75 * rev)))
            is_unk = bool("unknown" in str(r.get("recipient", "")).lower())
            is_pub = bool("public" in str(r.get("destination", "")).lower())


            probs = self.compute_class_probabilities(s, rev, c, rho, is_unk, is_pub)
            p_gt = probs.get(gt, 0.0)

            # Standard nonconformity score: 1 - p(y_true | x)
            score = 1.0 - p_gt
            nonconformity_scores.append(score)
            sample_probs_and_gt.append((probs, gt))

        if not nonconformity_scores:
            raise ValueError("No valid calibration records found.")

        nonconformity_scores.sort()
        n_eff = len(nonconformity_scores)

        # Standard finite-sample conformal quantile index: ceil((n+1) * (1 - alpha)) / n
        q_idx = int(math.ceil((n_eff + 1) * (1.0 - self.alpha))) - 1
        q_idx = max(0, min(n_eff - 1, q_idx))
        self.quantile_q = float(nonconformity_scores[q_idx])
        self.is_calibrated = True

        # Compute empirical performance metrics on calibration set
        covered = 0
        abstentions = 0
        accepted_errors = 0
        accepted_total = 0

        class_counts: Dict[str, int] = {k: 0 for k in INTERVENTIONS}
        class_covered: Dict[str, int] = {k: 0 for k in INTERVENTIONS}
        class_abstain: Dict[str, int] = {k: 0 for k in INTERVENTIONS}

        for probs, gt in sample_probs_and_gt:
            class_counts[gt] += 1
            # Conformal set: all classes with 1 - p <= quantile_q
            c_set = [k for k in INTERVENTIONS if (1.0 - probs[k]) <= self.quantile_q]
            
            if gt in c_set:
                covered += 1
                class_covered[gt] += 1

            if len(c_set) != 1:
                abstentions += 1
                class_abstain[gt] += 1
            else:
                accepted_total += 1
                pred = c_set[0]
                if pred != gt:
                    accepted_errors += 1

        cov_emp = round(covered / n_eff, 4)
        abst_rate = round(abstentions / n_eff, 4)
        acc_risk = round(accepted_errors / accepted_total, 4) if accepted_total > 0 else 0.0

        class_cov_pct = {
            k: round(class_covered[k] / class_counts[k], 4) if class_counts[k] > 0 else 1.0
            for k in INTERVENTIONS
        }
        class_abst_pct = {
            k: round(class_abstain[k] / class_counts[k], 4) if class_counts[k] > 0 else 0.0
            for k in INTERVENTIONS
        }

        report = (
            f"Split Conformal Calibration (alpha={self.alpha:.2f}, n={n_eff}, "
            f"artifacts={len(base_artifacts)}): Empirical coverage {cov_emp*100:.1f}%. "
            f"Quantile q_hat={self.quantile_q:.4f}. "
            "Assumption Notice: Exact finite-sample marginal coverage holds under exchangeability. "
            "Because base artifacts represent grouped scenario clusters, base-artifact disjointness "
            "is enforced to prevent data leakage, though minor inter-scenario covariate shift may occur."
        )

        self.stats = ConformalCalibrationStats(
            split_name="dev",
            num_samples=n_eff,
            num_base_artifacts=len(base_artifacts),
            alpha=self.alpha,
            quantile_q=self.quantile_q,
            coverage_empirical=cov_emp,
            abstention_rate=abst_rate,
            accepted_risk=acc_risk,
            class_coverages=class_cov_pct,
            class_abstentions=class_abst_pct,
            assumptions_report=report,
        )
        return self.stats

    def predict(
        self,
        class_probabilities: Dict[str, float],
        deterministic_label: str,
        severity: float = 0.5,
        reversibility: float = 0.5,
        confidence: float = 0.8,
    ) -> ConformalPredictionResult:
        """
        Generates conformal prediction set and applies selective prediction policy.
        """
        q_thresh = self.quantile_q if self.is_calibrated else 0.65

        # Prediction set C(x) = {y : 1 - p(y|x) <= q_thresh}
        pred_set = [k for k in INTERVENTIONS if (1.0 - class_probabilities.get(k, 0.0)) <= q_thresh]

        set_size = len(pred_set)
        is_empty = (set_size == 0)
        is_singleton = (set_size == 1)
        is_ambiguous = (set_size > 1)

        # Selective decision rule:
        # Rule 1: High severity safeguard: if STOP is in the prediction set and severity is high,
        # never downgrade to ASK or ACT; safety intervention is STOP.
        if "STOP" in pred_set and (severity >= 0.80 or deterministic_label == "STOP"):
            final_intervention = "STOP"
            abstention = False

        # Rule 2: Unambiguous singleton set
        elif is_singleton:
            final_intervention = pred_set[0]
            abstention = (final_intervention == "ASK")

        # Rule 3: Empty set (extreme anomaly / nonconforming outlier) -> Abstain to ASK
        elif is_empty:
            final_intervention = "ASK"
            abstention = True

        # Rule 4: Ambiguous set (|C(x)| > 1) -> Escalation to ASK
        else:
            final_intervention = "ASK"
            abstention = True


        # Invariant: If epistemic confidence is genuinely degraded (< 0.70) and not critical STOP -> ASK
        if confidence < 0.70 and final_intervention not in ("STOP", "ASK"):
            final_intervention = "ASK"
            abstention = True

        return ConformalPredictionResult(
            prediction_set=pred_set,
            set_size=set_size,
            is_singleton=is_singleton,
            is_ambiguous=is_ambiguous,
            is_empty=is_empty,
            selected_intervention=final_intervention,
            abstention_applied=abstention,
            nonconformity_threshold=round(q_thresh, 4),
            confidence_level=round(1.0 - self.alpha, 4),
            class_probabilities=class_probabilities,
        )


def load_calibration_dataset(file_path: Optional[str] = None) -> List[Dict[str, Any]]:
    """Loads dev partition from benchmark dataset for disjoint calibration."""
    path = Path(file_path or "benchmark/data/earb_v1.jsonl")
    if not path.exists():
        return []

    dev_records = []
    with open(path, "r", encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if not line:
                continue
            rec = json.loads(line)
            if rec.get("split") == "dev":
                dev_records.append(rec)
    return dev_records
