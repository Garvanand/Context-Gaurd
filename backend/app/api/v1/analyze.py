"""
Analysis endpoint exposing ContextGuard six-stage reasoning pipeline.
"""

from fastapi import APIRouter, status
from backend.app.models.schemas import AnalysisRequest, AnalysisResponse
from backend.pipeline.pipeline import ContextGuardPipeline

router = APIRouter(tags=["Pre-Action Risk Analysis"])
_pipeline = ContextGuardPipeline()


@router.post(
    "/analyze",
    response_model=AnalysisResponse,
    status_code=status.HTTP_200_OK,
    summary="Action-Conditioned Risk Analysis",
    description=(
        "Executes the full six-stage ContextGuard reasoning pipeline: "
        "Context, Intent, Evidence, Consequence, Uncertainty, Intervention."
    ),
)
async def analyze_artifact(request: AnalysisRequest) -> AnalysisResponse:
    """
    Evaluates Artifact + Context + Intended Action and returns deterministic intervention.
    """
    return await _pipeline.analyze(request)
