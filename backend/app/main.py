"""
ContextGuard FastAPI Application Entry Point.

Action-Conditioned Pre-Action Digital Safety Backend.
"""

from contextlib import asynccontextmanager
import time
from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse

from backend.app.core.config import settings
from backend.app.core.logging import logger
from backend.app.api.v1.health import router as health_router
from backend.app.api.v1.analyze import router as analyze_router


@asynccontextmanager
async def lifespan(app: FastAPI):
    # Startup sequence
    logger.info(
        f"Starting {settings.app_name} v{settings.app_version} [{settings.environment}] "
        f"on {settings.host}:{settings.port}"
    )
    yield
    # Shutdown sequence
    logger.info(f"Shutting down {settings.app_name} cleanly")


app = FastAPI(
    title="ContextGuard Backend",
    description="Multimodal AI System for Pre-Action Risk Detection in Everyday Digital Tasks",
    version=settings.app_version,
    docs_url="/docs" if settings.debug else None,
    redoc_url="/redoc" if settings.debug else None,
    lifespan=lifespan,
)

# CORS configuration
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.allowed_origins,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.middleware("http")
async def log_requests(request: Request, call_next):
    start_time = time.time()
    response = await call_next(request)
    duration_ms = round((time.time() - start_time) * 1000, 2)
    logger.info(
        f"{request.method} {request.url.path} -> {response.status_code} ({duration_ms}ms)"
    )
    response.headers["X-Process-Time-Ms"] = str(duration_ms)
    return response


# Include routes at root level (/health, /analyze)
app.include_router(health_router, tags=["Health"])
app.include_router(analyze_router, tags=["Analyze"])

# Include routes under /api/v1 prefix (/api/v1/health, /api/v1/analyze)
app.include_router(health_router, prefix="/api/v1", tags=["Health v1"])
app.include_router(analyze_router, prefix="/api/v1", tags=["Analyze v1"])


@app.get("/", summary="Root Welcome")
async def root():
    return {
        "system": settings.app_name,
        "version": settings.app_version,
        "description": "Action-Conditioned Pre-Action Digital Safety Engine",
        "docs": "/docs" if settings.debug else "disabled",
        "status": "operational",
    }


@app.exception_handler(Exception)
async def global_exception_handler(request: Request, exc: Exception):
    logger.error(f"Unhandled exception on {request.method} {request.url.path}: {exc}", exc_info=True)
    return JSONResponse(
        status_code=500,
        content={
            "detail": "Internal server error occurred.",
            "error_type": type(exc).__name__,
        },
    )


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        "backend.app.main:app",
        host=settings.host,
        port=settings.port,
        reload=settings.debug,
    )
