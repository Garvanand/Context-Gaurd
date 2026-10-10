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
from backend.app.api.v1.supervisor import router as supervisor_router
from backend.app.api.v1.relay import router as relay_router


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

# CORS configuration - permit Vite dev servers and local supervisor consoles
app.add_middleware(
    CORSMiddleware,
    allow_origins=[
        "http://localhost:5173",
        "http://127.0.0.1:5173",
        "http://localhost:3000",
        "http://127.0.0.1:3000",
        "*"
    ],
    allow_credentials=False,  # Set False when wildcard origin is present
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


# Include routes at root level (/health, /analyze, /supervisor, /relay)
app.include_router(health_router, tags=["Health"])
app.include_router(analyze_router, tags=["Analyze"])
app.include_router(supervisor_router, tags=["Supervisor"])
app.include_router(relay_router, tags=["Relay"])

# Include routes under /api/v1 prefix (/api/v1/health, /api/v1/analyze, /api/v1/supervisor, /api/v1/relay)
app.include_router(health_router, prefix="/api/v1", tags=["Health v1"])
app.include_router(analyze_router, prefix="/api/v1", tags=["Analyze v1"])
app.include_router(supervisor_router, prefix="/api/v1", tags=["Supervisor v1"])
app.include_router(relay_router, prefix="/api/v1", tags=["Relay v1"])


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
