
#!/usr/bin/env python3
"""
Known World - Save File Auditor
Batch 20.2

Read-only inspection of Known World save data.

Features:
- File inventory
- File sizes
- SHA-256 checksums
- Basic known-format header checks
- JSON summary output

This script does not modify save files.

It does not prove relational consistency between
separate persistence domains.
"""

from __future__ import annotations

import argparse
import hashlib
import json
from datetime import datetime, timezone
from pathlib import Path


KNOWN_HEADERS = {
    "npcs.tsv": "KNOWNWORLD_NPCS\t",
    "campaign_calendar.tsv":
        "KNOWNWORLD_CAMPAIGN_CALENDAR\t1",
}


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()

    with path.open("rb") as handle:
        while True:
            chunk = handle.read(1024 * 1024)

            if not chunk:
                break

            digest.update(chunk)

    return digest.hexdigest()


def header_status(path: Path) -> str:
    expected = KNOWN_HEADERS.get(path.name)

    if expected is None:
        return "not_checked"

    with path.open("rb") as handle:
        first_line = handle.readline(4096)

    if len(first_line) == 4096 and not first_line.endswith(
        (b"\n", b"\r")
    ):
        return "header_too_long"

    decoded = first_line.decode(
        "utf-8",
        errors="replace",
    ).strip("\r\n")

    if decoded.startswith(expected):
        return "recognized"

    return "unexpected_header"


def audit(directory: Path) -> dict:
    if not directory.is_dir():
        raise FileNotFoundError(
            f"Save directory does not exist: {directory}"
        )

    records = []

    for path in sorted(directory.rglob("*")):
        if not path.is_file():
            continue

        relative = path.relative_to(directory)

        records.append({
            "path": relative.as_posix(),
            "bytes": path.stat().st_size,
            "sha256": sha256_file(path),
            "header_status": header_status(path),
        })

    warnings = []

    for record in records:
        if record["header_status"] in (
            "unexpected_header",
            "header_too_long",
        ):
            warnings.append(
                f"Check file header: {record['path']}"
            )

    temporary = [
        record["path"]
        for record in records
        if record["path"].endswith(".tmp")
    ]

    if temporary:
        warnings.append(
            "Temporary save files exist: "
            + ", ".join(temporary)
        )

    return {
        "schema_version": 1,
        "audit_time_utc": datetime.now(
            timezone.utc
        ).isoformat(),
        "save_directory": str(directory.resolve()),
        "file_count": len(records),
        "total_bytes": sum(
            record["bytes"] for record in records
        ),
        "files": records,
        "warnings": warnings,
        "limitations": [
            "This is a read-only inventory.",
            "Only selected known file headers are checked.",
            "Checksums cannot prove that save data is valid.",
            "Cross-file references are not validated.",
            "Concurrent game saving can produce mixed snapshots.",
            "Historical consistency is not verified.",
        ],
    }


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Audit Known World save files."
    )

    parser.add_argument(
        "--save-dir",
        type=Path,
        required=True,
        help=(
            "Path to the Known World NPC save directory, "
            "normally world/knownworld/npc"
        ),
    )

    parser.add_argument(
        "--output",
        type=Path,
        default=None,
        help="Optional path for the JSON audit report",
    )

    args = parser.parse_args()

    result = audit(args.save_dir.resolve())

    print(
        json.dumps(
            result,
            indent=2,
        )
    )

    if args.output is not None:
        output = args.output.resolve()

        output.parent.mkdir(
            parents=True,
            exist_ok=True,
        )

        output.write_text(
            json.dumps(result, indent=2) + "\n",
            encoding="utf-8",
        )

    if result["warnings"]:
        print(
            "\nAudit completed with warnings."
        )
    else:
        print(
            "\nAudit completed without detected warnings."
        )


if __name__ == "__main__":
    main()
