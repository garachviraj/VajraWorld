"""
FastAPI application entry point for VajraWorld Edge Plane.
"""
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from edge.api.routes import router

app = FastAPI(
    title="VajraWorld Edge Intelligence Engine",
    description="Predictive Cyber Defence World Model REST API & Telemetry Interface",
    version="0.8.0"
)

# Enable CORS for Android client and web dashboards
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(router)

@app.get("/")
def health_check():
    return {
        "service": "VajraWorld Edge Intelligence Plane",
        "status": "ONLINE",
        "version": "0.8.0",
        "docs_url": "/docs"
    }

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("edge.api.server:app", host="0.0.0.0", port=8000, reload=True)
