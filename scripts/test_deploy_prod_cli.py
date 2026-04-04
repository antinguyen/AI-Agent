#!/usr/bin/env python3
"""Lightweight regression checks for scripts/deploy_prod.py CLI behavior."""

from __future__ import annotations

import json
import subprocess
import sys
from pathlib import Path


SCRIPT_PATH = Path(__file__).resolve().parent / "deploy_prod.py"


def run_cmd(*args: str) -> subprocess.CompletedProcess[str]:
    return subprocess.run(
        [sys.executable, str(SCRIPT_PATH), *args],
        capture_output=True,
        text=True,
        check=False,
    )


def parse_first_json_line(output: str) -> dict:
    for line in output.splitlines():
        line = line.strip()
        if line.startswith("{") and line.endswith("}"):
            return json.loads(line)
    raise AssertionError("No JSON payload found in command output")


def test_valid_health_timeout_json_mode() -> None:
    result = run_cmd("--dry-run", "--print-json-only", "--health-timeout-sec", "45")
    assert result.returncode == 0, result.stderr or result.stdout
    payload = json.loads(result.stdout.strip())
    assert payload["status"] == "success"
    assert payload["options"]["healthTimeoutSec"] == 45
    assert payload["options"]["healthRetryAttempts"] == 9
    assert payload["options"]["healthRetrySleepSec"] == 5


def test_invalid_health_timeout_non_integer() -> None:
    result = run_cmd("--dry-run", "--print-json-only", "--health-timeout-sec", "abc")
    assert result.returncode != 0
    payload = parse_first_json_line(result.stdout)
    assert payload["exitCode"] == 2
    assert payload["error"]["code"] == "invalid_option_value"


def test_invalid_health_timeout_too_low() -> None:
    result = run_cmd("--dry-run", "--print-json-only", "--health-timeout-sec", "0")
    assert result.returncode != 0
    payload = parse_first_json_line(result.stdout)
    assert payload["exitCode"] == 2
    assert payload["error"]["code"] == "invalid_option_value"


def test_invalid_option_combo_print_json_only_without_dry_run() -> None:
    result = run_cmd("--print-json-only")
    assert result.returncode != 0
    payload = parse_first_json_line(result.stdout)
    assert payload["exitCode"] == 2
    assert payload["error"]["code"] == "invalid_option_combo"


def test_health_log_message_is_dynamic() -> None:
    source = SCRIPT_PATH.read_text(encoding="utf-8")
    assert "wait up to {HEALTH_TIMEOUT_SEC}s for Spring Boot" in source


def main() -> None:
    tests = [
        test_valid_health_timeout_json_mode,
        test_invalid_health_timeout_non_integer,
        test_invalid_health_timeout_too_low,
        test_invalid_option_combo_print_json_only_without_dry_run,
        test_health_log_message_is_dynamic,
    ]

    failures = []
    for test in tests:
        try:
            test()
            print(f"PASS: {test.__name__}")
        except Exception as exc:  # noqa: BLE001
            failures.append((test.__name__, exc))
            print(f"FAIL: {test.__name__}: {exc}")

    if failures:
        print(f"\n{len(failures)} test(s) failed")
        raise SystemExit(1)

    print(f"\nAll {len(tests)} tests passed")


if __name__ == "__main__":
    main()
