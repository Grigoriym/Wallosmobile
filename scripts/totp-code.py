#!/usr/bin/env python3
"""Print the current TOTP code for the throwaway Wallos instance (docs/local-info.txt).

Wallos verifies with a 30-second period and a wide window, so a code stays good for a while —
but generate a fresh one per attempt anyway, since a *used* code is recorded in `last_totp_used`.

    python3 scripts/totp-code.py            # the instance's published secret
    python3 scripts/totp-code.py <SECRET>   # any other Base32 secret
"""
import base64
import hashlib
import hmac
import struct
import sys
import time

DEFAULT_SECRET = "JBSWY3DPEHPK3PXP"


def totp(secret: str, when: float | None = None) -> str:
    key = base64.b32decode(secret.upper())
    counter = int(when if when is not None else time.time()) // 30
    digest = hmac.new(key, struct.pack(">Q", counter), hashlib.sha1).digest()
    offset = digest[-1] & 0x0F
    code = (struct.unpack(">I", digest[offset:offset + 4])[0] & 0x7FFFFFFF) % 1_000_000
    return f"{code:06d}"


if __name__ == "__main__":
    print(totp(sys.argv[1] if len(sys.argv) > 1 else DEFAULT_SECRET))
