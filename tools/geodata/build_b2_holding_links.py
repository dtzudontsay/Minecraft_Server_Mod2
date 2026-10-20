
#!/usr/bin/env python3
"""B2.2: reconcile existing map-observation names with political holding IDs.

Run from any directory:
    py tools/geodata/build_b2_holding_links.py

No new coordinates or place identities are invented. ALL output X/Z pairs
are unapproved observations, never safe settlement-generation locations.
The tool does not modify the original research CSVs or existing world saves.
"""
from __future__ import annotations

import argparse
import csv
import json
import re
import unicodedata
from collections import Counter, defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
RESEARCH = ROOT / "tools/geodata"
ACTIVE = RESEARCH / "output/authored_places/reconciled_2026-10-17/marker_catalog_active_253.csv"
INDEXES = (
    RESEARCH / "research_marker_name_index.csv",
    RESEARCH / "sw_marker_name_crosswalk.csv",
)
HOLDINGS = ROOT / "src/main/resources/data/knownworld/scenarios/agot_298_ac/landed_holdings.json"
REFERENCES = (
    ROOT / "src/main/resources/data/knownworld/reference/locations.json",
    ROOT / "src/main/resources/data/knownworld/reference/locations_holdings.json",
)
RUNTIME = ROOT / "src/main/resources/data/knownworld/scenarios/agot_298_ac/holding_geo_candidates.json"
AUDIT = RESEARCH / "output/b2_holding_links"
NON_SITE = {"REGION", "TERRITORY"}


def read_csv(path: Path) -> list[dict[str, str]]:
    with path.open("r", encoding="utf-8-sig", newline="") as f:
        result = list(csv.DictReader(f))
    if not result:
        raise ValueError(f"Empty source: {path}")
    return result


def slug(text: str) -> str:
    text = unicodedata.normalize("NFKD", text.casefold())
    text = "".join(c for c in text if not unicodedata.combining(c))
    text = text.replace("’", "").replace("'", "")
    return re.sub(r"_+", "_", re.sub(r"[^a-z0-9]+", "_", text)).strip("_")


def add_name(index: dict[str, set[str]], name: str, place_id: str) -> None:
    if name and place_id:
        index[slug(name)].add(place_id)


def collect(repo_root: Path = ROOT) -> tuple[dict, list[dict], list[dict]]:
    """Build a validated runtime candidate manifest and two audit tables."""
    base = repo_root / "tools/geodata"
    markers = read_csv(base / "output/authored_places/reconciled_2026-10-17/marker_catalog_active_253.csv")
    expected_cols = {"id", "minecraft_x", "minecraft_z", "canonical_place_id", "name", "placement_status"}
    if not expected_cols.issubset(markers[0]):
        raise ValueError("Active marker CSV columns changed; refuse to guess")
    marker_ids = [m["id"] for m in markers]
    if len(marker_ids) != len(set(marker_ids)):
        raise ValueError("Duplicate marker IDs in active observations")

    holdings_path = repo_root / "src/main/resources/data/knownworld/scenarios/agot_298_ac/landed_holdings.json"
    holdings = json.loads(holdings_path.read_text(encoding="utf-8"))["holdings"]
    holding_ids = [h["id"] for h in holdings]
    if len(holding_ids) != len(set(holding_ids)):
        raise ValueError("Duplicate authored holding IDs")
    holdings_by_location: dict[str, list[dict]] = defaultdict(list)
    for h in holdings:
        holdings_by_location[h["worldLocationId"]].append(h)

    # Combine both reference files without choosing a 'winning' duplicate.
    references: dict[str, list[dict]] = defaultdict(list)
    names: dict[str, set[str]] = defaultdict(set)
    for relative in ("locations.json", "locations_holdings.json"):
        p = repo_root / "src/main/resources/data/knownworld/reference" / relative
        for item in json.loads(p.read_text(encoding="utf-8")):
            references[item["id"]].append(item)
            add_name(names, item.get("displayName", ""), item["id"])
            add_name(names, item["id"], item["id"])
    for h in holdings:
        add_name(names, h["name"], h["worldLocationId"])
        add_name(names, h["worldLocationId"], h["worldLocationId"])

    crosswalk: dict[str, list[tuple[str, str]]] = defaultdict(list)
    for relative in ("research_marker_name_index.csv", "sw_marker_name_crosswalk.csv"):
        p = base / relative
        for row in read_csv(p):
            if row["name"].strip():
                crosswalk[row["id"]].append((row["name"].strip(), relative))

    marker_to_location: dict[str, str] = {}
    marker_detail: dict[str, dict] = {}
    audit_markers = []
    for marker in sorted(markers, key=lambda m: m["id"]):
        marker_id = marker["id"]
        published = marker.get("canonical_place_id", "").strip()
        evidence = [(marker.get("name", "").strip(), "marker_catalog")]
        evidence += crosswalk.get(marker_id, [])
        matched: set[str] = set()
        for label, _source in evidence:
            # Slash-separated alternatives describe uncertain identity,
            # not confirmation that either alternative occupies this point.
            if not label or "/" in label:
                continue
            matched.update(loc for loc in names.get(slug(label), ()) if loc in holdings_by_location)
        if published in holdings_by_location:
            matched.add(published)
        # If a marker explicitly identifies a *different* known location,
        # do not override it by a coincidentally matching label.
        conflicting_identity = bool(
            published and published in references and published not in matched
            and matched
        )
        if conflicting_identity or len(matched) > 1:
            state = "CONFLICTING_IDENTITIES"
            selected = None
        elif len(matched) == 1:
            selected = next(iter(matched))
            state = "CANDIDATE_ID_MATCH" if selected == published else "CANDIDATE_EXACT_NAME"
        else:
            selected = None
            state = "NOT_LINKED_TO_HOLDING"

        x_text, z_text = marker["minecraft_x"].strip(), marker["minecraft_z"].strip()
        if not x_text or not z_text:
            raise ValueError(f"Marker {marker_id} has no X/Z")
        x, z = int(round(float(x_text))), int(round(float(z_text)))
        if selected:
            marker_to_location[marker_id] = selected
        detail = {
            "markerId": marker_id,
            "candidateX": x,
            "candidateZ": z,
            "identityEvidence": state,
            "sourceNameStatus": marker.get("source_name_status", ""),
            "reviewStatus": marker.get("review_status", ""),
            "landMaskStatus": marker.get("land_mask_status", ""),
            "physicalPlacementApproved": False,
        }
        marker_detail[marker_id] = detail
        audit_markers.append({
            "marker_id": marker_id,
            "candidate_location_id": selected or "",
            "link_status": state,
            "other_possible_location_ids": ";".join(sorted(matched)) if not selected else "",
            "source_canonical_id": published,
            "names_in_existing_sources": " | ".join(dict.fromkeys(v for v, _ in evidence if v)),
            "candidate_x": x,
            "candidate_z": z,
            "land_mask_status": detail["landMaskStatus"],
            "placement_approved": "NO",
        })

    by_location: dict[str, list[dict]] = defaultdict(list)
    for marker_id, location in marker_to_location.items():
        by_location[location].append(marker_detail[marker_id])

    records, audit_holdings = [], []
    for h in sorted(holdings, key=lambda h: h["id"]):
        location = h["worldLocationId"]
        candidates = sorted(by_location.get(location, []), key=lambda m: m["markerId"])
        domain = h["type"] in NON_SITE
        if domain:
            state = "DOMAIN_NOT_SETTLEMENT"
        elif len(candidates) > 1:
            state = "MULTIPLE_CANDIDATE_MARKERS_REVIEW"
        elif candidates:
            state = "CANDIDATE_NOT_APPROVED"
        else:
            state = "NO_MATCHED_MARKER"
        records.append({
            "holdingId": h["id"], "worldLocationId": location,
            "holdingType": h["type"], "ownerDynastyId": h.get("ownerDynastyId"),
            "holderNpcId": h.get("holderNpcId"), "isDomain": domain,
            "locationReferenceExists": location in references,
            "status": state, "candidates": candidates,
        })
        audit_holdings.append({
            "holding_id": h["id"], "holding_name": h["name"],
            "holding_type": h["type"], "world_location_id": location,
            "owner_dynasty_id": h.get("ownerDynastyId", ""),
            "holder_npc_id": h.get("holderNpcId", ""),
            "location_ref_exists": "YES" if location in references else "NO",
            "status": state, "marker_ids": ";".join(m["markerId"] for m in candidates),
            "approved_build_coordinates": "NO",
        })

    counts = Counter(row["status"] for row in records)
    marker_counts = Counter(row["link_status"] for row in audit_markers)
    result = {
        "schemaVersion": 1,
        "purpose": "Non-authoritative source observations for B2/B3, not settlement generation",
        "physicalPlacementApproval": "NONE",
        "sourceFiles": [
            "tools/geodata/output/authored_places/reconciled_2026-10-17/marker_catalog_active_253.csv",
            "tools/geodata/research_marker_name_index.csv",
            "tools/geodata/sw_marker_name_crosswalk.csv",
            "src/main/resources/data/knownworld/scenarios/agot_298_ac/landed_holdings.json",
            "src/main/resources/data/knownworld/reference/locations.json",
            "src/main/resources/data/knownworld/reference/locations_holdings.json",
        ],
        "summary": {
            "holdingCount": len(holdings), "activeMarkerCount": len(markers),
            "distinctReferencedLocations": len(holdings_by_location),
            "holdingStates": dict(sorted(counts.items())),
            "markerLinkStates": dict(sorted(marker_counts.items())),
        },
        "holdings": records,
    }
    # Critical constraints, including no silently approved site or synthetic Y.
    assert len(result["holdings"]) == len(holdings)
    assert all(not m["physicalPlacementApproved"] for h in records for m in h["candidates"])
    assert all("candidateY" not in m for h in records for m in h["candidates"])
    return result, audit_holdings, audit_markers


def write_csv(path: Path, rows: list[dict]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="") as f:
        writer = csv.DictWriter(f, fieldnames=list(rows[0]))
        writer.writeheader()
        writer.writerows(rows)


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--dry-run", action="store_true", help="Inspect coverage, don't write files")
    parser.add_argument("--check", action="store_true", help="Verify existing generated JSON matches sources")
    args = parser.parse_args()
    result, holdings, markers = collect()
    payload = json.dumps(result, ensure_ascii=False, indent=2) + "\n"
    print(json.dumps(result["summary"], indent=2))
    if args.check:
        if not RUNTIME.exists() or RUNTIME.read_text(encoding="utf-8") != payload:
            raise SystemExit("B2 runtime evidence is stale/missing. Rerun without --check.")
    elif not args.dry_run:
        RUNTIME.parent.mkdir(parents=True, exist_ok=True)
        RUNTIME.write_text(payload, encoding="utf-8")
        write_csv(AUDIT / "holdings_259_audit.csv", holdings)
        write_csv(AUDIT / "markers_253_audit.csv", markers)
        print(f"Wrote {RUNTIME.relative_to(ROOT)}")
        print(f"Wrote {AUDIT.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
