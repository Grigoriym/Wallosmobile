# Frictions

Tooling friction hit during work, newest last. One line each. Promoted or fixed entries get
deleted — see `/finalize`.

- `pip install perfetto` failed with a PEP 668 "externally managed environment" error on this
  machine's Python — a venv (`python3 -m venv`) was needed, `--break-system-packages` was not tried.
- Backgrounding `adb shell perfetto -o ... -t 8s ... &` inside a `(... &)` subshell and pulling the
  trace after the script's own `sleep`s summed to ~8s grabbed a 0-byte file — the on-device capture
  was still running past the subshell's own exit; re-pulling once `adb shell ps -A | grep perfetto`
  showed it had actually finished got the real (1.2MB) file.
- Timing an `adb shell input tap X Y &` plus a fixed `sleep` to screenshot an in-flight loading state
  repeatedly missed the window — real server latency for the same endpoint varied ~10ms to ~700ms
  run to run, and `adb`'s own shell-dispatch overhead added further unaccounted jitter on top; cutting
  connectivity before the tap (`airplane-mode enable` + `svc wifi disable`) forced a deterministic,
  wide window instead of racing a moving target.
- `docker exec wallos grep -rn ... --include=*.php` failed with "unrecognized option" — the
  container's `grep` is BusyBox's, which has no `--include`; `grep -rln <pattern> <path>` (no flag,
  just a bare recursive search) is what works there.
