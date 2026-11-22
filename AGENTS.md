# AGENTS.md

Operating contract for any agent working in this repository. Read `PLAN.md` first: it is the source of
truth for architecture, contracts and governance. This file is the session-start checklist. When they
disagree, `PLAN.md` wins.

## 1. What this repository is

RotaBlock is a language-neutral projectional block editor framework built with Kotlin Multiplatform.
Text is the only stored representation; blocks are a live projection of the parsed text; every block
edit compiles back into a minimal text edit.

Modules: `:language-api`, `:block-api`, `:block-impl`, `:shared`, `:androidApp`, `:desktopApp`.
Applications are entrypoints only. All product code lives in `:shared` and below.

## 2. Build environment

The host is Windows, driven from WSL. The toolchain lives on the Windows side.

```bash
powershell.exe .\gradlew.bat build
powershell.exe .\gradlew.bat :shared:allTests
powershell.exe .\gradlew.bat :desktopApp:run
```

- Never run `./gradlew`, `gradlew` or a local `gradle` from WSL. There is no Gradle in WSL by design.
- Never install a JDK, Gradle or Kotlin into WSL to make a build work.
- `local.properties` holds `sdk.dir` and is never committed.
- CI runs on Linux and is the only place where `./gradlew` is correct.

## 3. Dependencies

1. Use the published catalog through `rootLibs` first.
2. Add to `gradle/libs.versions.toml` only when the dependency is absent there, and state why next to
   the entry.
3. Never hardcode a version in a `build.gradle.kts` file.

## 4. Non-negotiable rules

### 4.1 Branches

- Work branches branch from `dev` and target `dev`. Never target `master`.
- Only `dev` targets `master`, and only as a release pull request.
- Name work branches `^(feat|fix|ci|docs|refactor|test|chore|perf|build|revert)/[a-z0-9._-]+$`.
- Never push directly to `dev` or `master`.

### 4.2 Commits

- One line: `<type>(<scope>): <subject>`, imperative, English, at most 72 characters.
- No body, no footers, no descriptions, no emoji, no attribution.
- One commit per reviewable work unit, roughly 400 authored changed lines.

### 4.3 Pull requests

- Title follows the commit format exactly.
- Body is empty. No description, no template, no checklist.
- One unit of work per pull request.
- Review feedback goes into the review summary, never as an inline comment.

### 4.4 Comments

- KDoc on public API declarations is required.
- No other comments exist: no `//`, no block comments outside public-API KDoc, no commented-out code,
  no TODO or FIXME markers, no author tags.
- Intent is carried by names, types and tests.

### 4.5 Language

Every artifact is English: code, identifiers, KDoc, tests, commit messages, pull request titles, branch
names, documentation, CI configuration and file names. Conversation language never changes artifacts.

### 4.6 Plan references

Code, tests, commit messages, pull request titles, branch names and any document other than `PLAN.md`
never mention a milestone, a phase or the plan itself. Describe what the change does.

## 5. Ignored by decision

`.atl/`, `odd/`, `.opencode/`, `.gentle-shell/`, `.gentle-ai/`, `.pi/` and any other agent-tooling
directory are gitignored from the first commit and are never committed.

## 6. Task tracking and memory

- ODD task documents live in Engram, which is authoritative.
- When a branch starts, copy its task document from Engram to `odd/tasks/<branch-name>.md` and work
  from that mirror.
- `odd/` stays gitignored. It is never committed and never staged.
- Durable knowledge goes to Engram as a topic, not into repository files.

## 7. Change procedure

1. Read `PLAN.md` and the contracts the change touches.
2. Branch from `dev` with a correctly named branch.
3. Write the failing test first for behavior changes, then make it pass.
4. Keep the change inside one module boundary where possible; respect the dependency rules in
   `PLAN.md` section 2.2.
5. Verify with `powershell.exe .\gradlew.bat build`.
6. Update `PLAN.md` in the same pull request when a contract, invariant, module boundary, build rule or
   governance rule changes.
7. Commit, push, open the pull request against `dev` with an empty body.

## 8. Definition of done

A change is done when its tests assert the full observable effect, the build passes locally and in CI,
public API carries KDoc and nothing else is commented, every artifact is English and free of plan
references, and the pull request targets `dev` from a correctly named branch with a single-line title
and an empty body.

## 9. Hard stops

- Do not add a third Kotlin Multiplatform target without a decision record in `PLAN.md`.
- Do not move product logic into `:androidApp` or `:desktopApp`.
- Do not put JVM-only dependencies in `commonMain`.
- Do not commit build output, caches, IDE state, `local.properties` or anything under the ignored
  directories in section 5.
- Do not commit, push or open a pull request that fails a CI gate.
