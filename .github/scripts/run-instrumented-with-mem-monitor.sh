#!/usr/bin/env bash
# TEMPORARY diagnostics helper — logs runner host RAM (free -h) and emulator guest RAM
# (/proc/meminfo) every 15s alongside the instrumented test run, to confirm or rule out
# memory exhaustion as the cause of the ColorBuffer/ClassNotFoundException failures seen
# on the ui CI shard. Remove once the root cause is confirmed and a real fix lands.
set -uo pipefail

(
  while true; do
    echo "=== [mem-monitor] $(date -u +%T) host ==="
    free -h
    echo "=== [mem-monitor] $(date -u +%T) guest ==="
    adb shell cat /proc/meminfo 2>/dev/null | head -5
    sleep 15
  done
) &
monitor_pid=$!

./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class="$1"
exit_code=$?

kill "$monitor_pid" 2>/dev/null || true
exit "$exit_code"
