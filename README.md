# RotaBlock

A language-neutral projectional block editor framework for Kotlin Multiplatform.

Text is the only stored representation. Blocks are a live projection of the parsed structure, and
every block edit compiles back into a minimal text edit.

Read [PLAN.md](PLAN.md) for architecture, contracts and governance, and [AGENTS.md](AGENTS.md) for the
working contract.

## Modules

| Module | Role |
| --- | --- |
| `:language-api` | Language-neutral DOM, ranges, document edits, extension points |
| `:block-api` | Block model, block edits, mapping SPI, templates |
| `:block-impl` | Projection engine and edit compiler |
| `:shared` | Composition root and embeddable app surface |
| `:androidApp` | Android entrypoint |
| `:desktopApp` | Desktop entrypoint |

## Build

```bash
powershell.exe .\gradlew.bat build
powershell.exe .\gradlew.bat :desktopApp:run
```
