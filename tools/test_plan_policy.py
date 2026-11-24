"""Tests the documentation checks of PLAN.md against small synthetic repositories."""

from __future__ import annotations

import importlib.util
import tempfile
import unittest
from pathlib import Path

SPEC = importlib.util.spec_from_file_location("plan_policy", Path(__file__).with_name("plan_policy.py"))
policy = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(policy)

HEADER = "## 1. Purpose\n\n### 1.1 Detail\n\n"


def source(root: Path, relative: str, body: str) -> None:
    """Write a Kotlin source of a production source set."""
    path = root / relative
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(body, encoding="utf-8")


def repository(root: Path, plan: str, agents: str, settings: str) -> None:
    """Write the three documents the checks read."""
    (root / "PLAN.md").write_text(plan, encoding="utf-8")
    (root / "AGENTS.md").write_text(agents, encoding="utf-8")
    (root / "settings.gradle.kts").write_text(settings, encoding="utf-8")


class IdentifierTest(unittest.TestCase):
    """Every prefixed identifier is defined once and every reference resolves."""

    def test_undefined_reference_is_reported(self) -> None:
        self.assertEqual(
            policy.check_identifiers(HEADER + "Breaks `P-2`.\n"),
            ["PLAN.md:5: P-2 has no definition"],
        )

    def test_defined_reference_resolves(self) -> None:
        plan = HEADER + "| `P-1` | **Rule.** | Text. |\n\nBreaks `P-1`.\n"
        self.assertEqual(policy.check_identifiers(plan), [])

    def test_duplicate_definition_is_reported(self) -> None:
        plan = HEADER + "| `P-1` | **One.** | Text. |\n| P-1 | **Two.** | Text. |\n"
        self.assertEqual(policy.check_identifiers(plan), ["PLAN.md:6: P-1 is defined twice"])

    def test_identifier_inside_a_fence_is_ignored(self) -> None:
        plan = HEADER + "| `P-1` | **Rule.** | Text. |\n\n```\nP-9\n```\n"
        self.assertEqual(policy.check_identifiers(plan), [])


class SectionTest(unittest.TestCase):
    """Every section reference resolves to an existing heading."""

    def test_missing_heading_is_reported(self) -> None:
        self.assertEqual(
            policy.check_sections(HEADER + "See §9.9.\n"),
            ["PLAN.md:5: section 9.9 has no heading"],
        )

    def test_existing_heading_resolves(self) -> None:
        self.assertEqual(policy.check_sections(HEADER + "See §1.1 and §1.\n"), [])

    def test_section_inside_a_fence_is_ignored(self) -> None:
        self.assertEqual(policy.check_sections(HEADER + "```\n§9.9\n```\n"), [])


class TypeTest(unittest.TestCase):
    """Every public type named in section 6 resolves."""

    def plan(self, body: str) -> str:
        return HEADER + "## 6. Model\n\n### 6.1 Document and text\n\n" + body + "\n## 7. Next\n"

    def test_missing_declaration_is_reported(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            self.assertEqual(
                policy.check_types(self.plan("`Document` owns text."), root),
                ["PLAN.md:6: Document has no declaration in a production source set"],
            )

    def test_declaration_resolves(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            source(root, "core/language/src/commonMain/kotlin/Document.kt", "class Document\n")
            self.assertEqual(policy.check_types(self.plan("`Document` owns text."), root), [])

    def test_enum_entry_resolves(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            source(
                root,
                "core/block/src/commonMain/kotlin/Style.kt",
                "enum class SeparatorStyle {\n    Comma,\n}\n",
            )
            self.assertEqual(policy.check_types(self.plan("Separator `Comma`."), root), [])

    def test_standard_library_type_resolves(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            self.assertEqual(policy.check_types(self.plan("`String` text."), Path(directory)), [])

    def test_declaration_in_a_test_source_does_not_resolve(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            source(root, "core/language/src/commonTest/kotlin/Document.kt", "class Document\n")
            self.assertEqual(
                policy.check_types(self.plan("`Document` owns text."), root),
                ["PLAN.md:6: Document has no declaration in a production source set"],
            )


GRAPH = "## 5. Modules\n\n### 5.3 Module graph\n\n```\n:core:language\n:tests:*\n```\n\n"


class ModuleTest(unittest.TestCase):
    """Section 5.3, AGENTS.md and the Gradle settings script agree."""

    def documents(self, agents: str, settings: str) -> list[str]:
        return policy.check_modules(GRAPH, agents, settings)

    def test_agreeing_lists_pass(self) -> None:
        self.assertEqual(
            self.documents("Modules: `:core:language`.\n", 'include(":core:language")\n'),
            [],
        )

    def test_missing_agents_module_is_reported(self) -> None:
        self.assertEqual(
            self.documents("Modules: `:shared`.\n", 'include(":core:language")\n'),
            [
                "AGENTS.md: :core:language is missing from the module list",
                "AGENTS.md: :shared is not a module of section 5.3",
            ],
        )

    def test_unbuilt_module_is_reported(self) -> None:
        self.assertEqual(
            self.documents("Modules: `:core:language`.\n", 'include(":core:block")\n'),
            [
                "PLAN.md:5.3: :core:block is missing from the module graph",
                "PLAN.md:5.3: :core:language is not included by the build",
            ],
        )


class MainTest(unittest.TestCase):
    """The command reports through annotations and an exit code."""

    def test_clean_repository_succeeds(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            repository(
                root,
                GRAPH + HEADER + "| `P-1` | **Rule.** | Text. |\n",
                "Modules: `:core:language`.\n",
                'include(":core:language")\n',
            )
            self.assertEqual(policy.main(["plan_policy.py", str(root)]), 0)

    def test_broken_repository_fails(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            repository(root, GRAPH + HEADER + "Breaks `P-2` twice: `P-2`.\n", "", "")
            self.assertEqual(policy.main(["plan_policy.py", str(root)]), 1)


if __name__ == "__main__":
    unittest.main()
