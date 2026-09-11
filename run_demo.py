"""
Single-command demonstration runner for VajraWorld.
Starts the Edge Intelligence REST API, initializes the storage database,
and replays the competition demo scenario.
"""
import sys
import time
import threading
import uvicorn
import requests
from edge.api.server import app
from edge.demo_replay import run_scenario

def start_server():
    uvicorn.run(app, host="127.0.0.1", port=8000, log_level="warning")

def main():
    guardian_mode = "--guardian" in sys.argv
    test_mode = "--test-mode" in sys.argv
    print("=" * 65)
    print("VAJRAWORLD GUARDIAN -- PREDICTIVE CYBER DEFENCE WORLD MODEL")
    print("Single-Command Demonstration & Test Launcher")
    print("=" * 65)

    # 1. Start Edge REST API server in background thread
    server_thread = threading.Thread(target=start_server, daemon=True)
    server_thread.start()

    print("[+] Starting Edge REST API on http://127.0.0.1:8000/v1...")
    
    # Wait for server to become ready
    ready = False
    for attempt in range(20):
        try:
            r = requests.get("http://127.0.0.1:8000/", timeout=1.0)
            if r.status_code == 200 and r.json().get("status") == "ONLINE":
                ready = True
                break
        except Exception:
            time.sleep(0.5)

    if not ready:
        print("[!] Error: Edge REST API server failed to start within timeout.")
        sys.exit(1)

    print("[+] Edge REST API is ONLINE!")
    print("[+] Android Defender Plane endpoint available at: http://10.0.2.2:8000/ (Emulator) or http://127.0.0.1:8000/")
    print("[+] API Documentation at: http://127.0.0.1:8000/docs\n")

    # 2. Run the Demo Scenario
    try:
        if guardian_mode:
            from edge.demo_guardian_replay import run_guardian_scenario
            run_guardian_scenario(api_url="http://127.0.0.1:8000/v1")
        else:
            run_scenario(api_url="http://127.0.0.1:8000/v1", fast_mode=True)
    except Exception as e:
        print(f"[!] Demo scenario failed: {e}")
        sys.exit(1)

    if test_mode:
        print("\n[+] Test Mode verification passed successfully!")
        sys.exit(0)
    else:
        print("\n[+] Edge service running. Press Ctrl+C to terminate.")
        try:
            while True:
                time.sleep(1)
        except KeyboardInterrupt:
            print("\n[+] Shutting down VajraWorld.")

if __name__ == "__main__":
    main()
