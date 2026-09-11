"""
Database models for VajraWorld storage engine.
Supports SQLite out-of-the-box and PostgreSQL for enterprise deployments.
"""
from datetime import datetime, timezone
import json
import sqlite3
from typing import Any, Dict, List, Optional

SCHEMA_SQL = """
CREATE TABLE IF NOT EXISTS assets (
    asset_id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    ip_address TEXT NOT NULL,
    subnet TEXT,
    asset_type TEXT DEFAULT 'Host',
    criticality TEXT DEFAULT 'Medium',
    status TEXT DEFAULT 'ACTIVE',
    first_seen TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_seen TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS flows (
    flow_id TEXT PRIMARY KEY,
    timestamp TIMESTAMP NOT NULL,
    src_ip TEXT NOT NULL,
    dst_ip TEXT NOT NULL,
    src_port INTEGER NOT NULL,
    dst_port INTEGER NOT NULL,
    protocol TEXT NOT NULL,
    tcp_flags TEXT,
    bytes_fwd INTEGER DEFAULT 0,
    bytes_bwd INTEGER DEFAULT 0,
    pkts_fwd INTEGER DEFAULT 0,
    pkts_bwd INTEGER DEFAULT 0,
    duration REAL DEFAULT 0.0
);

CREATE TABLE IF NOT EXISTS state_windows (
    window_id TEXT PRIMARY KEY,
    timestamp TIMESTAMP NOT NULL,
    nodes_count INTEGER NOT NULL,
    edges_count INTEGER NOT NULL,
    mean_risk REAL DEFAULT 0.0,
    predicted_stage TEXT DEFAULT 'Benign',
    raw_features_json TEXT
);

CREATE TABLE IF NOT EXISTS forecasts (
    forecast_id TEXT PRIMARY KEY,
    timestamp TIMESTAMP NOT NULL,
    state_id TEXT NOT NULL,
    horizon_steps INTEGER NOT NULL,
    current_risk REAL NOT NULL,
    horizon_risks_json TEXT NOT NULL,
    predicted_stage TEXT NOT NULL,
    stage_probability REAL NOT NULL,
    confidence REAL NOT NULL,
    uncertainty REAL NOT NULL,
    critical_assets_json TEXT,
    drivers_json TEXT,
    model_version TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS incidents (
    incident_id TEXT PRIMARY KEY,
    title TEXT NOT NULL,
    status TEXT DEFAULT 'OPEN',
    severity TEXT DEFAULT 'HIGH',
    risk REAL NOT NULL,
    confidence REAL NOT NULL,
    eta_seconds INTEGER DEFAULT 0,
    predicted_stage TEXT NOT NULL,
    affected_assets_json TEXT,
    evidence_json TEXT,
    recommended_action TEXT,
    acknowledged INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS simulations (
    simulation_id TEXT PRIMARY KEY,
    timestamp TIMESTAMP NOT NULL,
    target_asset TEXT NOT NULL,
    action_type TEXT NOT NULL,
    baseline_risk REAL NOT NULL,
    post_action_risk REAL NOT NULL,
    residual_risk REAL NOT NULL,
    disruption_rating TEXT NOT NULL,
    utility_score REAL NOT NULL,
    details_json TEXT
);

CREATE TABLE IF NOT EXISTS audit_events (
    audit_id TEXT PRIMARY KEY,
    timestamp TIMESTAMP NOT NULL,
    actor TEXT NOT NULL,
    action_type TEXT NOT NULL,
    target TEXT NOT NULL,
    details_json TEXT,
    result TEXT DEFAULT 'SUCCESS'
);

CREATE TABLE IF NOT EXISTS model_versions (
    model_id TEXT PRIMARY KEY,
    version TEXT NOT NULL,
    checksum TEXT NOT NULL,
    is_active INTEGER DEFAULT 1,
    accuracy REAL DEFAULT 0.0,
    brier_score REAL DEFAULT 0.0,
    lead_time_sec REAL DEFAULT 0.0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
"""
