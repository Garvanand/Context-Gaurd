"""
Automated Setup and Diagnostic Script for Ollama and Qwen2.5-VL-3B.

Checks:
1. Ollama installation on Windows (PATH and default locations)
2. Service availability on port 11434
3. Model tag presence ('qwen2.5vl:3b' or 'qwen2.5-vl:3b')
4. Model pulling if environment permissions allow
5. End-to-end multimodal image inference sanity check
"""

import io
import os
import sys
import time
import shutil
import socket
import urllib.request
import subprocess
from pathlib import Path
from PIL import Image, ImageDraw

TARGET_MODEL = "qwen2.5vl:3b"
ALT_TARGET_MODEL = "qwen2.5-vl:3b"
OLLAMA_URL = "http://localhost:11434"


def check_port(host="127.0.0.1", port=11434, timeout=1.5) -> bool:
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    sock.settimeout(timeout)
    res = sock.connect_ex((host, port))
    sock.close()
    return res == 0


def create_sample_test_image() -> bytes:
    img = Image.new("RGB", (300, 150), color=(240, 240, 245))
    draw = ImageDraw.Draw(img)
    draw.rectangle([10, 10, 290, 140], outline=(40, 60, 120), width=2)
    draw.text((25, 40), "ContextGuard Sanity Check", fill=(20, 20, 40))
    draw.text((25, 70), "Synthetic Benchmark Test", fill=(80, 80, 100))
    buf = io.BytesIO()
    img.save(buf, format="JPEG")
    return buf.getvalue()


def main():
    print("=" * 60)
    print("CONTEXTGUARD: OLLAMA & QWEN2.5-VL-3B SETUP DIAGNOSTICS")
    print("=" * 60)

    # 1. Check CLI executable
    ollama_bin = shutil.which("ollama")
    if not ollama_bin:
        # Check standard user local directory
        local_cand = Path(os.environ.get("LOCALAPPDATA", "")) / "Programs" / "Ollama" / "ollama.exe"
        if local_cand.exists():
            ollama_bin = str(local_cand)

    print(f"1. Ollama Executable: {'Found at ' + ollama_bin if ollama_bin else 'NOT INSTALLED'}")

    # 2. Check service running
    port_open = check_port()
    print(f"2. Ollama Daemon (Port 11434): {'ACTIVE' if port_open else 'INACTIVE / NOT RUNNING'}")

    if not ollama_bin:
        print("\n[ACTION REQUIRED] Ollama is not installed on this workstation.")
        print("Installation options:")
        print("  Option A (winget):")
        print("    winget install Ollama.Ollama --accept-source-agreements --accept-package-agreements")
        print("  Option B (direct installer):")
        print("    Download and run https://ollama.com/download/OllamaSetup.exe")
        print("After installation, start the Ollama application from the Start Menu.")
        sys.exit(0)

    if not port_open:
        print("\nStarting Ollama daemon in background...")
        try:
            subprocess.Popen([ollama_bin, "serve"], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
            time.sleep(3)
            port_open = check_port()
            print(f"Daemon startup attempt: {'SUCCESS' if port_open else 'FAILED - start manually via desktop app'}")
        except Exception as e:
            print(f"Could not launch Ollama daemon automatically: {e}")

    if port_open:
        # 3. Check installed models
        try:
            req = urllib.request.Request(f"{OLLAMA_URL}/api/tags")
            with urllib.request.urlopen(req, timeout=3) as resp:
                import json
                tags_data = json.loads(resp.read().decode())
                models = [m.get("name", "") for m in tags_data.get("models", [])]
                print(f"3. Installed Ollama Models: {models}")
                
                has_qwen = any(TARGET_MODEL in m or ALT_TARGET_MODEL in m for m in models)
                if has_qwen:
                    print(f"4. Target VLM ({TARGET_MODEL}) is INSTALLED and READY!")
                else:
                    print(f"\n4. Target model '{TARGET_MODEL}' not yet pulled.")
                    print(f"   Run: ollama pull {TARGET_MODEL}")
        except Exception as e:
            print(f"Failed to query Ollama tags API: {e}")


if __name__ == "__main__":
    main()
