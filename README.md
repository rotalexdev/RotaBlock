# RotaBlock

A language-neutral projectional block editor framework for Kotlin Multiplatform.

Text is the only stored representation. Blocks are a live projection of the parsed structure, and
every block edit compiles back into a minimal text edit.

Read [PLAN.md](PLAN.md) for architecture, contracts and governance, and [AGENTS.md](AGENTS.md) for the
working contract.

## Modules

| Module | Role |
| --- | --- |
| `:core:language` | Language-neutral DOM, ranges, document edits, extension points |
| `:core:block` | Block model, block edits, mapping SPI, templates |
| `:core:engine` | Projection engine and edit compiler |
| `:core:testkit` | Conformance suites that keep the contracts testable |
| `:parser:tree-sitter` | Tree-sitter adapter producing the neutral tree |
| `:ui:block` | Compose block surface |
| `:shared` | Composition root and embeddable app surface |
| `:androidApp` | Android entrypoint |
| `:desktopApp` | Desktop entrypoint |

## Build

```bash
powershell.exe .\gradlew.bat build
powershell.exe .\gradlew.bat :desktopApp:run
```
