# -*- coding: utf-8 -*-
"""Rename all top-level Java types/files with Dmitriy prefix."""
from __future__ import annotations

import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PREFIX = "Dmitriy"


def unique_java_files(root: Path) -> list[Path]:
    seen: set[str] = set()
    files: list[Path] = []
    for p in root.rglob("*.java"):
        key = str(p.resolve()).lower()
        if key in seen:
            continue
        seen.add(key)
        files.append(p)
    return files


def main() -> None:
    java_files = unique_java_files(ROOT)
    renames: dict[str, str] = {}

    for path in java_files:
        old = path.stem
        if old.startswith(PREFIX):
            continue
        renames[old] = f"{PREFIX}{old}"

    if not renames:
        print("Nothing to rename.")
        return

    ordered = sorted(renames.keys(), key=len, reverse=True)
    pattern_by_old = {
        old: re.compile(rf"\b{re.escape(old)}\b")
        for old in ordered
    }

    contents: dict[Path, str] = {}
    for path in java_files:
        text = path.read_text(encoding="utf-8")
        for old in ordered:
            text = pattern_by_old[old].sub(renames[old], text)
        contents[path] = text

    for path, text in contents.items():
        path.write_text(text, encoding="utf-8", newline="\n")

    for path in java_files:
        old = path.stem
        if old not in renames:
            continue
        new_path = path.with_name(f"{renames[old]}.java")
        if new_path.exists() and new_path != path:
            raise RuntimeError(f"Target already exists: {new_path}")
        path.rename(new_path)

    print(f"Renamed {len(renames)} Java types with prefix '{PREFIX}'.")


if __name__ == "__main__":
    main()
