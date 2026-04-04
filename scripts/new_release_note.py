#!/usr/bin/env python3
"""Generate a release announcement file from RELEASE_TEMPLATE.md."""

from __future__ import annotations

import argparse
from datetime import date
from pathlib import Path
import re
import sys

REPO_ROOT = Path(__file__).resolve().parents[1]
TEMPLATE_PATH = REPO_ROOT / "RELEASE_TEMPLATE.md"

VERSION_RE = re.compile(r"^\d+\.\d+\.\d+$")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Create a release announcement markdown file from template."
    )
    parser.add_argument("--version", required=True, help="Release version, e.g. 2.8.0")
    parser.add_argument(
        "--date",
        dest="release_date",
        default=date.today().isoformat(),
        help="Release date in YYYY-MM-DD format (default: today)",
    )
    parser.add_argument(
        "--output",
        help="Output markdown path (default: RELEASE_ANNOUNCEMENT_<version>.md)",
    )
    parser.add_argument(
        "--force",
        action="store_true",
        help="Overwrite existing output file if it already exists",
    )
    parser.add_argument(
        "--update-changelog",
        action="store_true",
        help="Insert a new release section into CHANGELOG.md",
    )
    parser.add_argument(
        "--changelog-path",
        default="CHANGELOG.md",
        help="Changelog path when using --update-changelog (default: CHANGELOG.md)",
    )
    return parser.parse_args()


def validate_inputs(version: str, release_date: str) -> None:
    if not VERSION_RE.match(version):
        raise ValueError("Invalid --version. Expected format like 2.8.0")
    try:
        date.fromisoformat(release_date)
    except ValueError as exc:
        raise ValueError("Invalid --date. Expected format YYYY-MM-DD") from exc


def build_output_path(version: str, output: str | None) -> Path:
    if output:
        return (REPO_ROOT / output).resolve() if not Path(output).is_absolute() else Path(output)
    safe_version = version.replace(".", "_")
    return REPO_ROOT / f"RELEASE_ANNOUNCEMENT_{safe_version}.md"


def render_template(template: str, version: str, release_date: str) -> str:
    rendered = template.replace("vX.Y.Z", f"v{version}", 1)
    rendered = rendered.replace("(YYYY-MM-DD)", f"({release_date})", 1)
    return rendered


def build_changelog_path(changelog_path: str) -> Path:
    path = Path(changelog_path)
    return (REPO_ROOT / changelog_path).resolve() if not path.is_absolute() else path


def build_changelog_entry(version: str, release_date: str) -> str:
    return (
        f"## [{version}] - {release_date}\n\n"
        "### Highlights\n"
        "- <main highlights>\n\n"
        "### Backend\n"
        "- <backend changes>\n\n"
        "### Frontend\n"
        "- <frontend changes>\n\n"
        "### DevOps / CI\n"
        "- <devops changes>\n\n"
        "### Validation\n"
        "- <verification results>\n\n"
        "---\n\n"
    )


def inject_changelog_section(changelog_text: str, section: str, version: str) -> str:
    if f"## [{version}] -" in changelog_text:
        raise ValueError(f"Version {version} already exists in changelog")

    title = "# Changelog"
    if changelog_text.startswith(title):
        remainder = changelog_text[len(title):]
        if remainder.startswith("\r\n"):
            remainder = remainder[2:]
        elif remainder.startswith("\n"):
            remainder = remainder[1:]
        remainder = remainder.lstrip("\r\n")
        return f"{title}\n\n{section}{remainder}"

    return f"{title}\n\n{section}{changelog_text.lstrip()}"


def main() -> int:
    args = parse_args()

    try:
        validate_inputs(args.version, args.release_date)
    except ValueError as exc:
        print(str(exc), file=sys.stderr)
        return 2

    if not TEMPLATE_PATH.exists():
        print(f"Template not found: {TEMPLATE_PATH}", file=sys.stderr)
        return 1

    template_text = TEMPLATE_PATH.read_text(encoding="utf-8")
    output_path = build_output_path(args.version, args.output)

    if output_path.exists() and not args.force:
        print(f"Output already exists: {output_path}", file=sys.stderr)
        print("Use --force to overwrite or --output to choose another path.", file=sys.stderr)
        return 2

    content = render_template(template_text, args.version, args.release_date)
    output_path.write_text(content, encoding="utf-8")

    if args.update_changelog:
        changelog_path = build_changelog_path(args.changelog_path)
        if not changelog_path.exists():
            print(f"Changelog not found: {changelog_path}", file=sys.stderr)
            return 1

        changelog_text = changelog_path.read_text(encoding="utf-8")
        changelog_section = build_changelog_entry(args.version, args.release_date)
        try:
            updated_changelog = inject_changelog_section(
                changelog_text,
                changelog_section,
                args.version,
            )
        except ValueError as exc:
            print(str(exc), file=sys.stderr)
            return 2
        changelog_path.write_text(updated_changelog, encoding="utf-8")

    relative = output_path.relative_to(REPO_ROOT) if output_path.is_relative_to(REPO_ROOT) else output_path
    print(f"Created release note: {relative}")
    if args.update_changelog:
        relative_changelog = changelog_path.relative_to(REPO_ROOT) if changelog_path.is_relative_to(REPO_ROOT) else changelog_path
        print(f"Updated changelog: {relative_changelog}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
