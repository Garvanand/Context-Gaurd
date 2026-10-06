"""
Qwen2.5-VL-3B Multimodal Vision Reasoner implementation using Ollama.

Features:
- Connects to local Ollama runtime (`qwen2.5vl:3b` / `qwen2.5-vl:3b`)
- Strictly enforces structured JSON schema output
- Pre-processes and normalizes in-memory images (JPEG, PNG, WebP)
- Resilient unavailable-model fallback adhering to Model Failure Safety Rule (never silent ACT)
- Real health telemetry and test inference probe
"""

import json
import re
import time
from pathlib import Path
from typing import Dict, Any, Optional
import httpx

from backend.app.core.config import settings
from backend.app.core.logging import logger
from backend.models.base import (
    VisionReasoner,
    StructuredVisionOutput,
    InferredIntentOutput,
    EvidenceItem,
    ImageNormalizer,
)

PROMPTS_DIR = Path("backend/prompts")
DEFAULT_PROMPT_TEMPLATE = PROMPTS_DIR / "reasoning_v1.txt"


class QwenVisionReasoner(VisionReasoner):
    """
    Multimodal Reasoner wrapping Qwen2.5-VL via local Ollama daemon.
    """

    def __init__(
        self,
        ollama_url: Optional[str] = None,
        model_name: Optional[str] = None,
        timeout_seconds: Optional[int] = None,
    ):
        self.ollama_url = (ollama_url or settings.ollama_base_url).rstrip("/")
        # Support both tag variations common in Ollama
        self.model_name = model_name or settings.ollama_model
        self.timeout_seconds = timeout_seconds or settings.vlm_timeout_seconds
        self._prompt_template = self._load_prompt_template()

    def _load_prompt_template(self) -> str:
        if DEFAULT_PROMPT_TEMPLATE.exists():
            return DEFAULT_PROMPT_TEMPLATE.read_text(encoding="utf-8")
        return (
            "You are ContextGuard. Evaluate the artifact with intended action.\n"
            "Output strictly a JSON object with fields: context_summary, intent_assessment, "
            "evidence, risk_type, risk_subtype, severity, reversibility, confidence, "
            "uncertainty_reasons, recommended_action, alternative_action, reason."
        )

    def format_prompt(self, ocr_text: Optional[str], context: Dict[str, Any]) -> str:
        text = ocr_text or "[No OCR text extracted on-device]"
        source_app = context.get("source_app", "Unknown App")
        intended_action = context.get("intended_action", "Unknown Action")
        destination = context.get("destination", "Unknown Destination")
        recipient = context.get("recipient", "Unknown Recipient")

        try:
            return self._prompt_template.format(
                ocr_text=text,
                source_app=source_app,
                intended_action=intended_action,
                destination=destination,
                recipient=recipient,
            )
        except Exception:
            return (
                f"OCR Text: {text}\n"
                f"Source App: {source_app}\n"
                f"Action: {intended_action}\n"
                f"Destination: {destination}\n"
                f"Recipient: {recipient}\n"
                "Provide structured safety assessment JSON."
            )

    async def analyze(
        self,
        image_bytes: Optional[bytes],
        ocr_text: Optional[str],
        context: Dict[str, Any],
    ) -> StructuredVisionOutput:
        """
        Analyze multimodal inputs and return validated StructuredVisionOutput.
        Degrades safely to deterministic context rules if Ollama is unavailable.
        """
        b64_image: Optional[str] = None
        if image_bytes:
            try:
                _, b64_image, _ = ImageNormalizer.normalize_image(image_bytes)
            except Exception as e:
                logger.warning(f"Image normalization failed: {e}. Proceeding with OCR and context.")

        prompt = self.format_prompt(ocr_text, context)

        # Attempt Ollama inference if service is reachable
        try:
            async with httpx.AsyncClient(timeout=float(self.timeout_seconds)) as client:
                payload: Dict[str, Any] = {
                    "model": self.model_name,
                    "messages": [
                        {
                            "role": "user",
                            "content": prompt,
                            **({"images": [b64_image]} if b64_image else {}),
                        }
                    ],
                    "format": "json",
                    "stream": False,
                }

                resp = await client.post(f"{self.ollama_url}/api/chat", json=payload)
                if resp.status_code == 200:
                    raw_content = resp.json().get("message", {}).get("content", "")
                    parsed = self._extract_and_validate_json(raw_content)
                    if parsed:
                        return parsed
                    logger.warning("Ollama response returned malformed JSON. Invoking safety fallback.")
                else:
                    logger.warning(f"Ollama returned HTTP {resp.status_code}. Invoking safety fallback.")
        except Exception as e:
            logger.info(f"Ollama inference unavailable ({type(e).__name__}: {e}). Invoking safe fallback reasoning.")

        # Model Failure Safety Rule: Return structured fallback (never silent ACT on failure)
        return self._generate_safe_fallback(ocr_text, context)

    def _extract_and_validate_json(self, raw_text: str) -> Optional[StructuredVisionOutput]:
        """
        Extract JSON substring and parse into StructuredVisionOutput.
        """
        if not raw_text:
            return None

        # Clean potential markdown fences
        clean_text = raw_text.strip()
        if clean_text.startswith("```json"):
            clean_text = clean_text[7:]
        elif clean_text.startswith("```"):
            clean_text = clean_text[3:]
        if clean_text.endswith("```"):
            clean_text = clean_text[:-3]
        clean_text = clean_text.strip()

        # Regex search for first JSON object if surrounded by noise
        match = re.search(r"\{.*\}", clean_text, re.DOTALL)
        if match:
            clean_text = match.group(0)

        try:
            data = json.loads(clean_text)
            return StructuredVisionOutput(**data)
        except Exception as e:
            logger.warning(f"Failed to parse model JSON output: {e}")
            return None

    def _generate_safe_fallback(
        self,
        ocr_text: Optional[str],
        context: Dict[str, Any]
    ) -> StructuredVisionOutput:
        """
        High-integrity deterministic fallback when Ollama is offline or unparseable.
        Distinguishes artifact risk from action risk based on clear context rules.
        """
        text = (ocr_text or "").lower()
        intended_action = context.get("intended_action", "").lower()
        destination = context.get("destination", "").lower()
        recipient = context.get("recipient", "").lower()

        evidence_items = []
        uncertainty_reasons = ["Local VLM daemon is offline; using deterministic rule-based fallback."]

        # Check for sensitive indicators in OCR
        has_financial = any(k in text for k in ["statement", "account", "balance", "transaction", "bank", "invoice", "credit", "upi", "inr", "merchant", "payment", "salary"])
        has_credentials = any(k in text for k in ["password", "private key", "otp", "token", "auth", "secret", "one-time password"])
        has_personal_id = any(k in text for k in ["passport", "license", "aadhaar", "national id", "dob", "pan verification", "pan"])
        has_phishing_lure = any(k in text for k in ["suspended", "locked", "urgent", "restore access", "kyc update", "verify?id=", ".xyz", ".top", "unauthorized"])

        if has_financial:
            evidence_items.append(EvidenceItem(type="text", description="Detected financial entities or payment tokens", importance=0.85))
        if has_credentials:
            evidence_items.append(EvidenceItem(type="text", description="Detected authentication credentials or OTP markers", importance=0.95))
        if has_personal_id:
            evidence_items.append(EvidenceItem(type="text", description="Detected personal identity or tax identifier markers", importance=0.80))
        if has_phishing_lure:
            evidence_items.append(EvidenceItem(type="text", description="Detected urgency, account suspension, or phishing lure indicators", importance=0.90))

        # Check action exposure and context
        is_phishing_destination = any(k in destination or k in recipient for k in ["phishing", "untrusted external", "unknown sms", "fake"])
        is_public_broadcast = any(k in destination for k in ["public", "twitter", "social", "forum", "broadcast", "reddit"])
        is_untrusted_recipient = any(k in recipient or k in destination for k in ["unknown", "unverified", "stranger", "telegram"])
        is_trusted_personal_or_internal = any(k in destination or k in recipient for k in ["vault", "personal", "encrypted", "local", "self", "drive", "family", "internal", "team", "slack"])

        # Action-Conditioned Decision Rules
        if is_phishing_destination or (has_phishing_lure and is_untrusted_recipient):
            severity = 0.95
            reversibility = 0.90
            confidence = 0.90
            recommended_action = "STOP"
            risk_type = "Digital Security"
            risk_subtype = "Phishing Credential Theft"
            reason = "Deceptive phishing lure and credential submission detected toward untrusted destination. Action blocked."
            alt = "Do not enter credentials or follow unverified links. Access the service directly via official app."
        elif has_credentials and is_untrusted_recipient:
            severity = 0.95
            reversibility = 0.95
            confidence = 0.95
            recommended_action = "STOP"
            risk_type = "Digital Security"
            risk_subtype = "Credential Disclosure"
            reason = "Transmitting OTP or authentication secrets to an unverified recipient enables account takeover. Action blocked."
            alt = "Never share one-time passwords or security codes with third parties."
        elif is_public_broadcast and (has_financial or has_credentials or has_personal_id):
            severity = 0.90
            reversibility = 1.00
            confidence = 0.95
            recommended_action = "STOP"
            risk_type = "Financial" if has_financial else "Digital Security"
            risk_subtype = "Public Data Exposure"
            reason = "Public broadcast of confidential, financial, or identity artifacts causes irreversible disclosure. Action blocked."
            alt = "Save document to personal encrypted vault or redact sensitive details before publishing."
        elif is_untrusted_recipient and (has_financial or has_personal_id):
            severity = 0.50
            reversibility = 0.50
            confidence = 0.65  # Below 0.70 to trigger ASK
            recommended_action = "ASK"
            risk_type = "Financial" if has_financial else "Privacy Disclosure"
            risk_subtype = "Unverified Recipient Transfer"
            uncertainty_reasons.append("Recipient identity cannot be verified through trusted address book.")
            reason = "Transmitting sensitive artifacts or authorizing payment to unverified entities carries high risk. User confirmation required."
            alt = "Verify recipient contact details via independent channel before transferring."
        elif is_trusted_personal_or_internal:
            severity = 0.05
            reversibility = 0.00
            confidence = 0.95
            recommended_action = "ACT"
            risk_type = "Benign" if not (has_financial or has_personal_id) else "Privacy Protected"
            risk_subtype = "Trusted Scoped Action"
            reason = "Action is scoped to trusted internal or personal storage without unauthorized external exposure. Safe to proceed."
            alt = None
        else:
            # Ambiguous default -> ASK (never silent ACT)
            severity = 0.40
            reversibility = 0.50
            confidence = 0.50
            recommended_action = "ASK"
            risk_type = "Privacy Disclosure"
            risk_subtype = "Ambiguous Exposure"
            uncertainty_reasons.append("Insufficient destination context to guarantee safe transmission.")
            reason = "Ambiguous context with unverified recipient. Policy mandates user confirmation."
            alt = "Review destination and recipient before proceeding."

        return StructuredVisionOutput(
            context_summary=f"Artifact analyzed via deterministic fallback rules ({'Financial' if has_financial else ('Credential' if has_credentials else 'General Document')}).",
            intent_assessment=f"Action '{intended_action}' directed to destination '{destination}'.",
            evidence=evidence_items,
            risk_type=risk_type,
            risk_subtype=risk_subtype,
            severity=severity,
            reversibility=reversibility,
            confidence=confidence,
            uncertainty_reasons=uncertainty_reasons,
            recommended_action=recommended_action,
            alternative_action=alt,
            reason=reason,
        )

    async def infer_intent(
        self,
        image_bytes: Optional[bytes],
        ocr_text: Optional[str],
        context: Dict[str, Any],
    ) -> InferredIntentOutput:
        """
        Infer the user's intent using multimodal evidence.
        """
        b64_image: Optional[str] = None
        if image_bytes:
            try:
                _, b64_image, _ = ImageNormalizer.normalize_image(image_bytes)
            except Exception as e:
                logger.warning(f"Image normalization failed for intent inference: {e}")

        text = ocr_text or "[No OCR]"
        source_app = context.get("source_app", "Unknown App")
        destination = context.get("destination", "Unknown Destination")
        recipient = context.get("recipient", "Unknown Recipient")
        candidate_actions = context.get("candidate_actions", ["SEND", "UPLOAD", "POST", "SIGN", "LOGIN", "APPROVE", "SAVE", "OPEN"])

        prompt = (
            "You are an intent inference engine. Predict the intended user action based on the context.\n"
            f"Candidate actions: {candidate_actions}\n\n"
            f"Context:\n"
            f"- OCR Text: {text}\n"
            f"- Source App: {source_app}\n"
            f"- Destination: {destination}\n"
            f"- Recipient: {recipient}\n\n"
            "Output strictly a JSON object with fields:\n"
            "- predicted_action: string\n"
            "- probabilities: dict mapping candidate actions to float probabilities [0, 1]\n"
            "- confidence: float [0, 1]\n"
            "- ambiguity: list of strings (reasons for uncertainty if any)"
        )

        try:
            async with httpx.AsyncClient(timeout=float(self.timeout_seconds)) as client:
                payload: Dict[str, Any] = {
                    "model": self.model_name,
                    "messages": [
                        {
                            "role": "user",
                            "content": prompt,
                            **({"images": [b64_image]} if b64_image else {}),
                        }
                    ],
                    "format": "json",
                    "stream": False,
                }
                resp = await client.post(f"{self.ollama_url}/api/chat", json=payload)
                if resp.status_code == 200:
                    raw_content = resp.json().get("message", {}).get("content", "")
                    
                    # Clean and parse JSON
                    clean_text = raw_content.strip()
                    if clean_text.startswith("```json"): clean_text = clean_text[7:]
                    elif clean_text.startswith("```"): clean_text = clean_text[3:]
                    if clean_text.endswith("```"): clean_text = clean_text[:-3]
                    clean_text = clean_text.strip()
                    match = re.search(r"\{.*\}", clean_text, re.DOTALL)
                    if match: clean_text = match.group(0)

                    try:
                        data = json.loads(clean_text)
                        return InferredIntentOutput(**data)
                    except Exception as e:
                        logger.warning(f"Failed to parse infer_intent JSON: {e}")
        except Exception as e:
            logger.info(f"Ollama infer_intent unavailable: {e}")

        # Fallback to naive heuristics
        dest_lower = destination.lower()
        source_lower = source_app.lower()
        
        inferred = "SEND"
        if any(k in dest_lower for k in ["public", "twitter", "x.com", "social", "forum", "broadcast", "upload", "post", "feed"]):
            inferred = "POST"
        elif any(k in dest_lower for k in ["vault", "drive", "backup", "local", "save"]):
            inferred = "SAVE"
        elif any(k in dest_lower for k in ["login", "portal", "verify"]):
            inferred = "LOGIN"

        return InferredIntentOutput(
            predicted_action=inferred,
            probabilities={a: (0.9 if a == inferred else 0.0) for a in candidate_actions},
            confidence=0.5,
            ambiguity=["Local fallback used due to unavailable VLM."]
        )

    async def check_health(self) -> Dict[str, Any]:
        """
        Verify Ollama service and model status.
        Never fakes health; performs real test inference if service is reachable.
        """
        t0 = time.time()
        ollama_reachable = False
        model_installed = False
        test_inference_status = "service_unavailable"
        startup_latency_ms = 0.0

        try:
            async with httpx.AsyncClient(timeout=2.0) as client:
                tags_resp = await client.get(f"{self.ollama_url}/api/tags")
                startup_latency_ms = round((time.time() - t0) * 1000, 2)
                if tags_resp.status_code == 200:
                    ollama_reachable = True
                    models = tags_resp.json().get("models", [])
                    model_names = [m.get("name", "") for m in models]
                    # Check if requested model exists
                    for m in model_names:
                        if self.model_name in m or "qwen2.5vl" in m or "qwen2.5-vl" in m:
                            model_installed = True
                            break

                    if model_installed:
                        # Attempt lightweight test inference with dummy input
                        try:
                            test_payload = {
                                "model": self.model_name,
                                "messages": [{"role": "user", "content": "ping"}],
                                "stream": False,
                            }
                            test_resp = await client.post(f"{self.ollama_url}/api/chat", json=test_payload, timeout=5.0)
                            if test_resp.status_code == 200:
                                test_inference_status = "passed"
                            else:
                                test_inference_status = f"failed_http_{test_resp.status_code}"
                        except Exception as e:
                            test_inference_status = f"failed_inference_{type(e).__name__}"
                    else:
                        test_inference_status = "model_not_pulled"
        except Exception:
            ollama_reachable = False
            test_inference_status = "service_unavailable"

        return {
            "ollama_reachable": ollama_reachable,
            "model_installed": model_installed,
            "model_name": self.model_name,
            "startup_latency_ms": startup_latency_ms,
            "test_inference_status": test_inference_status,
            "fallback_active": test_inference_status != "passed",
        }
