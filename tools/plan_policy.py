"""Verifies that the internal references of PLAN.md resolve.

The checks implement the documentation rules of PLAN.md section 10.5: every prefixed identifier,
every section number, every public type named in section 6, and the three module lists.
"""

from __future__ import annotations

import re
import subprocess
import sys
from pathlib import Path

PREFIX = r"(?:P|DEP|I|R|D|Q|M)"
IDENTIFIER_REFERENCE = re.compile(rf"\b{PREFIX}-\d+\b")
IDENTIFIER_DEFINITION_ROW = re.compile(rf"^\|\s*`?\*{{0,2}}{PREFIX}-\d+")
SECTION_HEADING = re.compile(r"^#{2,4}\s+(\d+(?:\.\d+)*)\b", re.MULTILINE)
SECTION_REFERENCE = re.compile(r"§\s*(\d+(?:\.\d+)*)")
MODULE_RUN = re.compile(r":[A-Za-z*:-]+")
MODULE_NAME = re.compile(r"^:[A-Za-z][A-Za-z0-9-]*(?::[A-Za-z][A-Za-z0-9-]*)*$")
BACKTICKED = re.compile(r"`([^`]+)`")
TYPE_NAME = re.compile(r"`([A-Z][a-z][A-Za-z0-9]*)`")
FENCED_BLOCK = re.compile(r"```.*?```", re.DOTALL)
PRODUCTION_SOURCE = re.compile(r"/src/[A-Za-z0-9]+Main/kotlin/")
INCLUDE = re.compile(r"include\s*\(([^)]*)\)")
QUOTED = re.compile(r'"(:[A-Za-z0-9:-]+)"')
STANDARD_LIBRARY_TYPES = frozenset({"Comparable", "String"})


def strip_fences(text: str) -> str:
    """Blanks fenced code blocks while preserving line numbers."""
    return FENCED_BLOCK.sub(lambda match: "\n" * match.group(0).count("\n"), text)


def declarations(name: str) -> re.Pattern[str]:
    """The pattern that declares a named type."""
    return re.compile(rf"\b(?:class|interface|object|typealias)\s+{name}\b")


def check_identifiers(plan: str) -> list[str]:
    """Every prefixed identifier is defined exactly once and every reference resolves."""
    defined: dict[str, int] = {}
    errors: list[str] = []
    for number, line in enumerate(plan.splitlines(), 1):
        match = IDENTIFIER_DEFINITION_ROW.match(line)
        if match is None:
            continue
        identifier = match.group(0).split("|")[1].strip().strip("`").strip("*")
        if identifier in defined:
            errors.append(f"PLAN.md:{number}: {identifier} is defined twice")
        else:
            defined[identifier] = number
    for number, line in enumerate(strip_fences(plan).splitlines(), 1):
        if IDENTIFIER_DEFINITION_ROW.match(line):
            line = line[match_end(line):]
        for reference in IDENTIFIER_REFERENCE.finditer(line):
            if reference.group(0) not in defined:
                errors.append(f"PLAN.md:{number}: {reference.group(0)} has no definition")
    return errors


def match_end(line: str) -> int:
    """The offset after a definition row's leading pipe and identifier cell."""
    return line.index("|", 1)


def check_sections(plan: str) -> list[str]:
    """Every section reference resolves to an existing heading."""
    headings = {match.group(1) for match in SECTION_HEADING.finditer(plan)}
    errors: list[str] = []
    for number, line in enumerate(strip_fences(plan).splitlines(), 1):
        for reference in SECTION_REFERENCE.finditer(line):
            if reference.group(1) not in headings:
                errors.append(f"PLAN.md:{number}: section {reference.group(1)} has no heading")
    return errors


def section(plan: str, heading: str) -> str:
    """The text of one numbered section, up to the next top-level heading."""
    start = re.search(rf"(?m)^###\s+{re.escape(heading)}\s", plan)
    if start is None:
        return ""
    following = re.search(r"(?m)^##\s+\d", plan[start.end():])
    end = start.end() + (following.start() if following else len(plan) - start.end())
    return plan[start.start():end]


def production_sources(root: Path) -> list[Path]:
    """Every tracked Kotlin source of a production source set."""
    listed = tracked_sources(root)
    paths = [root / name for name in listed] if listed is not None else list(
        root.glob("**/src/*Main/kotlin/**/*.kt")
    )
    return [path for path in paths if PRODUCTION_SOURCE.search(path.as_posix())]


def tracked_sources(root: Path) -> list[str] | None:
    """The Kotlin sources git reports, when the directory is a checkout."""
    try:
        completed = subprocess.run(
            ["git", "-C", str(root), "ls-files", "*.kt"],
            capture_output=True,
            text=True,
            check=False,
        )
    except OSError:
        return None
    if completed.returncode != 0:
        return None
    return [line for line in completed.stdout.splitlines() if line]


def check_types(plan: str, root: Path) -> list[str]:
    """Every public type named in section 6 resolves to a declaration or a known type."""
    text = strip_fences(section(plan, "6.1"))
    names = sorted(set(TYPE_NAME.findall(text)))
    sources = production_sources(root)
    bodies = [path.read_text(encoding="utf-8", errors="ignore") for path in sources]
    errors: list[str] = []
    for name in names:
        if name in STANDARD_LIBRARY_TYPES:
            continue
        declared = any(declarations(name).search(body) for body in bodies)
        entry = any(
            "enum class" in body and re.search(rf"(?m)^\s*{name}\s*[,;(]?\s*$", body)
            for body in bodies
        )
        if not declared and not entry:
            errors.append(f"PLAN.md:6: {name} has no declaration in a production source set")
    return errors


def modules_in(text: str) -> set[str]:
    """Every module path in a text."""
    return {token for token in MODULE_RUN.findall(text) if MODULE_NAME.match(token)}


def module_graph(plan: str) -> set[str]:
    """The module graph of section 5.3."""
    start = re.search(r"(?m)^###\s+5\.3\s", plan)
    if start is None:
        return set()
    block = FENCED_BLOCK.search(plan, start.end())
    return modules_in(block.group(0)) if block else set()


def agents_modules(agents: str) -> set[str]:
    """The module list of AGENTS.md's session checklist paragraph."""
    lines = agents.splitlines()
    for index, line in enumerate(lines):
        if not line.lstrip().startswith("Modules:"):
            continue
        paragraph = [line]
        for following in lines[index + 1:]:
            if not following.strip():
                break
            paragraph.append(following)
        return {
            token
            for token in BACKTICKED.findall(" ".join(paragraph))
            if MODULE_NAME.match(token)
        }
    return set()


def settings_modules(settings: str) -> set[str]:
    """Every module a Gradle settings script includes."""
    modules: set[str] = set()
    for arguments in INCLUDE.findall(settings):
        modules.update(QUOTED.findall(arguments))
    return modules


def check_modules(plan: str, agents: str, settings: str) -> list[str]:
    """Section 5.3, AGENTS.md and the Gradle settings script name the same modules."""
    planned = module_graph(plan)
    documented = agents_modules(agents)
    built = settings_modules(settings)
    errors = [f"AGENTS.md: {module} is missing from the module list" for module in sorted(planned - documented)]
    errors += [f"AGENTS.md: {module} is not a module of section 5.3" for module in sorted(documented - planned)]
    errors += [f"PLAN.md:5.3: {module} is missing from the module graph" for module in sorted(built - planned)]
    errors += [f"PLAN.md:5.3: {module} is not included by the build" for module in sorted(planned - built)]
    return errors


def main(argv: list[str]) -> int:
    """Run every documentation check over a repository root."""
    root = Path(argv[1]) if len(argv) > 1 else Path(".")
    plan = (root / "PLAN.md").read_text(encoding="utf-8")
    agents = (root / "AGENTS.md").read_text(encoding="utf-8")
    settings = (root / "settings.gradle.kts").read_text(encoding="utf-8")
    errors = [
        *check_identifiers(plan),
        *check_sections(plan),
        *check_types(plan, root),
        *check_modules(plan, agents, settings),
    ]
    for error in errors:
        print(f"::error::{error}")
    if errors:
        print(f"{len(errors)} documentation reference(s) do not resolve")
        return 1
    print("PLAN.md references resolve")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
