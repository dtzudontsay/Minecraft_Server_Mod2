
#!/usr/bin/env python3
"""
Known World - Java Dependency Inventory
Batch 20.0

Static inventory of project Java classes and explicit imports.

Outputs:
    class_inventory.csv
    internal_dependencies.csv
    external_dependencies.csv
    architecture_summary.json

Limitations:
    This tool analyzes Java source imports.
    It does not resolve runtime dependency injection,
    reflection, resource loading, method calls, or
    dynamically registered callbacks.
"""

from __future__ import annotations

import argparse
import csv
import json
import re

from collections import Counter
from pathlib import Path


ROOT_PACKAGE = "dev.dtzudontsay.knownworld"

PACKAGE_PATTERN = re.compile(
    r"^\s*package\s+([\w.]+)\s*;",
    re.MULTILINE,
)

IMPORT_PATTERN = re.compile(
    r"^\s*import\s+(?:static\s+)?([\w.*]+)\s*;",
    re.MULTILINE,
)

TYPE_PATTERN = re.compile(
    r"\b(?:class|interface|enum|record)\s+(\w+)"
)


def project_root() -> Path:
    return Path(__file__).resolve().parents[2]


def scan_source_file(path: Path, source_root: Path) -> dict:
    source = path.read_text(encoding="utf-8-sig")

    package_match = PACKAGE_PATTERN.search(source)
    if package_match is None:
        raise ValueError(
            f"Missing package declaration: {path}"
        )

    package = package_match.group(1)

    imports = sorted(
        set(IMPORT_PATTERN.findall(source))
    )

    relative_path = path.relative_to(source_root)

    # Java file names are authoritative for top-level
    # public type names. Nested types are not inventoried.
    class_name = path.stem

    qualified_name = f"{package}.{class_name}"

    declared_types = sorted(
        set(TYPE_PATTERN.findall(source))
    )

    return {
        "qualified_name": qualified_name,
        "class_name": class_name,
        "package": package,
        "path": str(relative_path).replace("\\", "/"),
        "declared_types": declared_types,
        "imports": imports,
        "lines": len(source.splitlines()),
    }


def domain_name(package: str) -> str:
    if package == ROOT_PACKAGE:
        return "root"

    prefix = ROOT_PACKAGE + "."

    if not package.startswith(prefix):
        return "external_project_package"

    relative = package[len(prefix):]
    parts = relative.split(".")

    if len(parts) >= 2:
        return ".".join(parts[:2])

    return parts[0]


def write_csv(
    path: Path,
    fieldnames: list[str],
    rows: list[dict],
) -> None:
    with path.open(
        "w",
        encoding="utf-8",
        newline="",
    ) as handle:
        writer = csv.DictWriter(
            handle,
            fieldnames=fieldnames,
        )

        writer.writeheader()
        writer.writerows(rows)


def build_inventory(
    root: Path,
    output: Path,
) -> None:
    source_root = (
        root
        / "src"
        / "main"
        / "java"
    )

    if not source_root.is_dir():
        raise FileNotFoundError(
            f"Java source directory not found: {source_root}"
        )

    java_files = sorted(
        source_root.rglob("*.java")
    )

    classes = [
        scan_source_file(path, source_root)
        for path in java_files
    ]

    known_types = {
        item["qualified_name"]
        for item in classes
    }

    class_rows = []
    internal_rows = []
    external_rows = []

    domain_counts = Counter()
    internal_edges = Counter()
    unresolved_imports = []

    for item in classes:
        source_name = item["qualified_name"]
        source_domain = domain_name(item["package"])

        domain_counts[source_domain] += 1

        class_rows.append({
            "class": source_name,
            "domain": source_domain,
            "package": item["package"],
            "source_path": item["path"],
            "lines": item["lines"],
            "import_count": len(item["imports"]),
        })

        for imported in item["imports"]:
            if imported.startswith(ROOT_PACKAGE + "."):
                target_package = imported.rpartition(".")[0]
                target_domain = domain_name(
                    target_package
                )

                resolved = imported in known_types

                internal_rows.append({
                    "source_class": source_name,
                    "source_domain": source_domain,
                    "target_import": imported,
                    "target_domain": target_domain,
                    "exact_type_resolved": resolved,
                })

                if source_domain != target_domain:
                    internal_edges[
                        (source_domain, target_domain)
                    ] += 1

                if not resolved:
                    # May be a wildcard, nested type,
                    # or static member import.
                    unresolved_imports.append({
                        "source": source_name,
                        "import": imported,
                    })

            else:
                external_rows.append({
                    "source_class": source_name,
                    "source_domain": source_domain,
                    "external_import": imported,
                })

    output.mkdir(
        parents=True,
        exist_ok=True,
    )

    write_csv(
        output / "class_inventory.csv",
        [
            "class",
            "domain",
            "package",
            "source_path",
            "lines",
            "import_count",
        ],
        class_rows,
    )

    write_csv(
        output / "internal_dependencies.csv",
        [
            "source_class",
            "source_domain",
            "target_import",
            "target_domain",
            "exact_type_resolved",
        ],
        internal_rows,
    )

    write_csv(
        output / "external_dependencies.csv",
        [
            "source_class",
            "source_domain",
            "external_import",
        ],
        external_rows,
    )

    summary = {
        "schema_version": 1,
        "analysis_type": "static_java_imports",
        "total_java_files": len(java_files),
        "total_java_source_lines": sum(
            item["lines"] for item in classes
        ),
        "total_internal_imports": len(internal_rows),
        "total_external_imports": len(external_rows),
        "domains": dict(
            sorted(domain_counts.items())
        ),
        "cross_domain_imports": [
            {
                "source_domain": source,
                "target_domain": target,
                "import_count": count,
            }
            for (source, target), count in sorted(
                internal_edges.items()
            )
        ],
        "unresolved_internal_imports": (
            unresolved_imports
        ),
        "limitations": [
            "Imports do not prove runtime usage.",
            "Implicit same-package references are omitted.",
            "Nested types may be unresolved.",
            "Static member imports may be unresolved.",
            "Reflection is not analyzed.",
            "Resource dependencies are not analyzed.",
            "Runtime registration is not analyzed.",
            "No mutation ownership is inferred.",
        ],
    }

    with (
        output / "architecture_summary.json"
    ).open("w", encoding="utf-8") as handle:
        json.dump(
            summary,
            handle,
            indent=2,
            sort_keys=True,
        )
        handle.write("\n")

    print("Known World dependency inventory completed.")
    print(f"Java files: {len(java_files)}")
    print(
        "Java source lines:",
        summary["total_java_source_lines"],
    )
    print(
        "Internal imports:",
        len(internal_rows),
    )
    print(
        "External imports:",
        len(external_rows),
    )
    print(
        "Domains:",
        len(domain_counts),
    )
    print(f"Reports: {output}")


def main() -> None:
    parser = argparse.ArgumentParser(
        description=(
            "Build the Known World Java "
            "dependency inventory."
        )
    )

    parser.add_argument(
        "--root",
        type=Path,
        default=project_root(),
        help="Repository root",
    )

    parser.add_argument(
        "--output",
        type=Path,
        default=None,
        help="Output directory",
    )

    args = parser.parse_args()

    root = args.root.resolve()

    output = (
        args.output.resolve()
        if args.output is not None
        else root
        / "docs"
        / "architecture"
        / "generated"
    )

    build_inventory(root, output)


if __name__ == "__main__":
    main()
