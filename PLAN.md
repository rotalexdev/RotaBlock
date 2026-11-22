# RotaBlock — Engineering Plan

This document is the source of truth for RotaBlock. It defines what the project is, the contracts
that hold it together, how it is built, how it is reviewed, and how it will grow. Code, tests and
review decisions follow this document; when they disagree, this document wins until a pull request
changes both.

Any change to a contract, invariant, module boundary, build rule or governance rule must update this
file in the same pull request. Milestone labels defined here are internal planning vocabulary and must
never appear in source code, commit messages, pull request titles or branches.

## 1. Product

RotaBlock is a language-neutral, projectional (block-based) editor framework distributed as a Kotlin
Multiplatform library plus two reference entrypoints.

The product invariant is singular and non-negotiable:

> There is one document. Text is the only stored representation. Blocks are a live projection of the
> parsed structure of that text, and every block edit compiles back into a minimal text edit.

There is no parallel block model, no block serialization format, and no view-to-view synchronization.
A keystroke in the text view and a drag in the block view are the same kind of change: a surgical edit
of one document, followed by reparse and re-projection.

### 1.1 What ships

| Artifact | Role |
| --- | --- |
| `:language-api` | Language-neutral DOM, text ranges, document edits, extension points |
| `:block-api` | Block model, block edits, mapping SPI, templates |
| `:block-impl` | Projection engine and edit compiler |
| `:block-ui` | Compose Multiplatform block renderer and gestures |
| `:shared` | Composition root: wires core, UI and language adapters into one embeddable app surface |
| `:androidApp` | Android entrypoint only |
| `:desktopApp` | Desktop (JVM) entrypoint only |

### 1.2 Non-goals

- Not an application product. RotaBlock is a framework consumed by other developers; the two app
  modules exist to prove the framework runs on both targets.
- No block document format, no project file, no workspace persistence in the framework core.
- No non-JVM targets (web, native) until an explicit decision record adds them.
- No editor features outside the block projection model (no terminal emulator, no git client, no
  language server) in the framework modules.

### 1.3 Consumers

Framework consumers embed `:shared` (or the core modules directly) into their own host application,
provide a `LanguageAdapter` for the languages they support, and render the block surface. The API
surface is public and must remain documented, stable and free of JVM-only types in common code.

## 2. Architecture

### 2.1 Layers

```
text (CharSequence + version)          single source of truth
        │  parse
        ▼
neutral DOM  (:language-api)           language-neutral tree, every node carries a TextRange
        │  project
        ▼
block tree   (:block-api, :block-impl) projection of the DOM; blocks anchor to DOM nodes + ranges
        │  render / gesture
        ▼
block UI     (:block-ui)               Compose surface; emits BlockEdit values
        │
        ▼
edit compiler (:block-impl)            BlockEdit -> List<DocumentEdit> (minimal, surgical)
        │  apply
        ▼
text (unchanged document, new version) -> reparse -> re-project -> both views update
```

Each layer only knows the layer below it. The UI never parses, the engine never renders, and the DOM
never knows that blocks exist.

### 2.2 Module graph

```
:androidApp ─┐
             ├─► :shared ─► :block-ui ─► :block-api ─► :language-api
:desktopApp ─┘         └──► :block-impl ─► :block-api
```

Dependency rules, enforced by review and by build configuration:

| Rule | Statement |
| --- | --- |
| DEP-1 | `*-api` modules may depend only on other `*-api` modules. |
| DEP-2 | `*-impl` modules depend on their `api` module and never on a UI module. |
| DEP-3 | Language adapters depend on `:block-api` and on their parser only. |
| DEP-4 | Application modules depend only on `:shared` and contain no product logic. |
| DEP-5 | No module depends on an application module; no cycles between modules. |
| DEP-6 | `commonMain` of every multiplatform module stays free of JVM-only types and dependencies. |

### 2.3 Package and namespace convention

| Concern | Value |
| --- | --- |
| Base package | `dev.rotalex.rotablock` |
| Kotlin packages | Base package plus the module path, for example `dev.rotalex.rotablock.block` and `dev.rotalex.rotablock.language.dom` |
| Android namespaces | Base package plus the module path, for example `dev.rotalex.rotablock.block.api` |
| Android application id | `dev.rotalex.rotablock` |
| Desktop main class | `dev.rotalex.rotablock.desktop.MainKt` |

No module declares a package outside the base package.

### 2.4 Source-set strategy

Every framework module is Kotlin Multiplatform with exactly two targets:

| Target | Declared as | Purpose |
| --- | --- | --- |
| Android | `com.android.kotlin.multiplatform.library` | Android application consumption |
| Desktop JVM | `jvm("desktop")` | Desktop application consumption |

Both targets are JVM-backed, which is why a JVM-only dependency is legal in a *platform* source set
but never in `commonMain`. When a module needs JVM-only code on both targets it introduces an
intermediate source set (`jvmSharedMain`) that `androidMain` and `desktopMain` depend on, and declares
the JVM-only dependency there once. No framework module currently needs this; a consumer-supplied
language adapter is the only expected case.

### 2.5 Contracts per module

#### `:language-api`

Responsibility: the neutral structural model every language adapter produces and every consumer reads.

Public surface: `DomNode`, `NodeKind`, `TextRange`, `ParsedFile`, `DocumentEdit`, `LanguageId`,
`LanguageAdapter`, `ExtensionPoint`.

Invariants:

- A `DomNode` always carries a `TextRange` in the source it was parsed from.
- Ranges are half-open, `[start, end)`, and never negative.
- `NodeKind` is a value class over a stable string id; neutral kinds are constants, language-specific
  kinds are constructed from the adapter's own naming.
- `DocumentEdit` is the only write primitive: `offset`, `oldLength`, `newText`.
- `LanguageAdapter` is total over malformed input: it returns a `ParsedFile` for any text, marking
  unparsable regions instead of throwing. An editor must never be unable to place a caret.

#### `:block-api`

Responsibility: the block model, the edit vocabulary, and the mapping SPI.

Public surface: `BlockTree`, `BlockNode`, `BlockPart`, `BlockField`, `BlockSlot`, `SlotCategory`,
`ValueKind`, `ValueKindOracle`, `BlockId`, `BlockRef`, `SlotRef`, `BlockEdit` and its variants,
`BlockMapping`, `BlockTemplate`, `ProjectionContext`, `BlockProjectionService`, `BLOCK_MAPPING_EP`.

Invariants:

- A block is an ordered list of parts: chrome fields (read-only syntax), editable fields, and slots.
  The order is render and serialize order.
- `BlockNode.anchor` is the DOM node the block projects; it is null only for synthetic chrome blocks.
- `BlockField.range` is non-null for every editable field; chrome may have no backing text.
- `BlockId` is unique within one projection and is never persisted.
- `SlotCategory` and `ValueKind` are closed sets expressed as enums.
- `BlockEdit` is a closed hierarchy; adding a variant requires a compiler branch and tests.

#### `:block-impl`

Responsibility: project a `ParsedFile` into a `BlockTree` and compile `BlockEdit` into minimal
`DocumentEdit`s.

Public surface: `BlockProjectionEngine` only. Everything else stays `internal`.

Invariants:

- Projection is pure and deterministic: same `ParsedFile` and mapping set produce the same tree.
- A block edit either produces one or more disjoint edits or an empty list; it never throws for a
  stale reference.
- Bytes outside the returned ranges are preserved exactly.

#### `:block-ui`

Responsibility: render a `BlockTree` and turn gestures into `BlockEdit` values. Holds no document
state and performs no parsing.

#### `:shared`

Responsibility: the composition root. Owns the document store, the reparse/re-projection loop, the
language adapter registry, dependency wiring and the single embeddable entry composable. Re-exports
the core modules with `api(...)` so a consumer only depends on `:shared`.

#### `:androidApp` and `:desktopApp`

Responsibility: platform entrypoint only. Create the host, call the `:shared` entry composable, and
declare platform-specific configuration. No domain logic, no parsing, no state machines.

## 3. Contracts

### 3.1 Neutral DOM

| Type | Contract |
| --- | --- |
| `TextRange(start, end)` | Half-open span; `length`, `isEmpty`, `contains(offset)`. |
| `DomNode` | `kind`, `range`, `parent`, `children`, `text()`. Children are in source order. |
| `NodeKind` | Value class over a stable string id; `Comparable` for deterministic ordering. |
| `ParsedFile` | `text()`, `documentVersion`, `root`. The projection input. |
| `DocumentEdit` | `offset`, `oldLength`, `newText`. Applied in descending offset order. |
| `LanguageAdapter` | `language: LanguageId`, `parse(source, version): ParsedFile`. Total over malformed input. |

Parser wrappers are rebuilt on every access in several real parsers, so identity is never used to
match nodes: the engine matches by `TextRange`, never by object identity.

No parser ships in the framework. `:language-api` defines the adapter contract and the neutral DOM; a
host supplies the adapters for the languages it supports. The core therefore carries no parser
dependency, no JVM-only parser library and no language-specific node model.

### 3.2 Block model

| Type | Contract |
| --- | --- |
| `BlockNode` | `id`, `kind`, `anchor`, `range`, `parts`, `label`, `valueKind`; `fields` and `slots` are views over `parts`. |
| `BlockPart` | Sealed: `Field(BlockField)` or `Slot(BlockSlot)`. |
| `BlockField` | `role`, `text`, `editable`, `range`; `BlockField.chrome` for read-only syntax. |
| `BlockSlot` | `category`, `children`, `multiple`, `range`, `valueKind`. |
| `SlotCategory` | Closed grammar: `STATEMENT`, `EXPRESSION`, `TYPE`, `DECLARATION`, `NAME`, `MODIFIER`, `PARAMETER`, `ARGUMENT`, `OPAQUE`. `OPAQUE` accepts anything. |
| `ValueKind` | Closed set shaping the socket: `BOOLEAN`, `NUMBER`, `STRING`, `OBJECT`, `TYPE`, `UNKNOWN`. |
| `BlockTree` | `root`, `version`, `block(id)`, `blockAt(offset)` returning the deepest containing block. |

`SlotCategory` answers "may this go here?" (structural grammar). `ValueKind` answers "what shape is
this?" (rendering affordance). They are orthogonal and never merged.

### 3.3 Identity and references

`BlockId` is a value class, unique within a projection, never persisted. `BlockRef` names a block;
`SlotRef(owner, slotIndex, index)` names a position, where `index == children.size` appends. References
are valid only against the tree they were computed from.

### 3.4 Edits

| Variant | Meaning | Compiled as |
| --- | --- | --- |
| `SetField` | Replace one editable field's text | One replace over the field range |
| `ReplaceWithText` | Replace a slot's content with text to be reparsed | One replace over the slot range |
| `Delete` | Remove a block and syntax that existed only to hold it | One delete over the block range plus owned syntax |
| `InsertTemplate` | Insert a template at a slot position | One zero-length insert at the insertion offset |
| `Wrap` | Wrap a block in a control template | One replace over the block range |
| `Move` | Move a block to another slot position | Two disjoint edits, returned in descending offset order |

Every variant maps to the minimum number of `DocumentEdit`s. An unresolvable reference yields an empty
list: the caller re-projects and retries from a fresh tree.

### 3.5 Mapping SPI

A `BlockMapping` declares the `languages` it serves and the `handles` set of `NodeKind`s it decomposes.
The `handles` set is the decomposition boundary: a handled child becomes its own block, an unhandled
child collapses into a single editable text field. `BlockMapping.project` defaults to
`ProjectionContext.carve`, the generic interleave of source chrome with recursively projected children.
`BlockMapping.serialize` runs only on created or edited nodes and defaults to concatenating parts in
order, never the whole file.

`ProjectionContext` is the seam: `source`, `textOf`, `carve`, `child`, and the `block`/`slot`/`field`
factories that let the engine own id assignment.

### 3.6 Extension points

`ExtensionPoint<T>` is the only registration mechanism. `BLOCK_MAPPING_EP` is the block mapping point;
language adapters and UI contributions get their own points when their modules land. Registration is
explicit and ordered; there is no service loader, no reflection and no global mutable singleton in
`commonMain`.

### 3.7 Invariants

| Id | Invariant |
| --- | --- |
| I-1 | Text is the only stored representation. No block tree owns or mutates text. |
| I-2 | Every block anchors to a DOM node and range, or is synthetic chrome with a null anchor. |
| I-3 | A `BlockEdit` compiles to one or more disjoint `DocumentEdit`s; everything outside their ranges is byte-identical. |
| I-4 | Edits are computed against the tree they were derived from; stale references produce an empty list, never an exception. |
| I-5 | An unmapped node kind collapses into an editable text field: no construct is ever a dead end. |
| I-6 | Every editable field carries a range; chrome may not. |
| I-7 | Block ids are stable within one projection and never persisted. |
| I-8 | Projection is pure and deterministic. |
| I-9 | A slot accepts only blocks whose category it accepts, or anything when `OPAQUE`. |
| I-10 | Public API is documented with KDoc; no other comments exist in source. |
| I-11 | `commonMain` never references JVM-only types or dependencies. |
| I-12 | All range arithmetic is half-open and non-negative. |

## 4. Engine semantics

### 4.1 Projection

`carve(node)` interleaves the literal source between consecutive child ranges as read-only chrome
fields with one single-child slot per child, then trailing chrome. A child whose kind is handled is
projected by its mapping; otherwise it collapses to an opaque block. A node with no children becomes a
leaf: one editable field carrying its text.

The expected `ValueKind` of a slot comes from the position when the mapping knows it, and from the
content otherwise. A `ValueKindOracle` is consulted first and may refine any block's produced kind; a
null answer falls back to the syntactic heuristic. Hosts wire a semantic oracle without touching the
engine.

Projection is a full pass in the current design. Incremental projection is an optimization that must
preserve I-8 and the observable results of the full pass; it lands only with a measured budget.

### 4.2 Whitespace policy

Deleting a block also removes the syntax that existed only to hold it: a trailing `;`, then trailing
spaces or tabs, then at most one line break. If a line break is consumed and the removed block was the
only content on its line, the line's indentation is removed too. If no line break is consumed, trailing
spaces are preserved.

Inserting a statement places it on its own line at the indentation of the insertion point. Inserting an
expression splices it in place with no whitespace changes.

These rules are part of the contract: they are asserted by exact-output tests, not by shape checks.

### 4.3 Staleness

`BlockTree.version` mirrors `ParsedFile.documentVersion`. A host compares versions instead of diffing
trees. An edit computed against a stale tree is discarded by the host, not by the engine.

## 5. Build and tooling

| Concern | Decision |
| --- | --- |
| Build tool | Gradle 9.6.1 through the committed wrapper |
| Kotlin | 2.4.0 |
| Android Gradle Plugin | 9.2.1 |
| Compose Multiplatform | 1.11.1 |
| Catalog | Published `io.github.alexanderrotela20.catalog:version-catalog:1.2.7`, imported as `rootLibs` |
| Convention plugins | Published `io.github.alexanderrotela20.convention:*`, version from the catalog |
| Java target | 17 |
| compileSdk / minSdk / targetSdk | 37 / 26 / 37 |
| JDK | Toolchain 21 for the daemon, JVM target 17 |

### 5.1 Catalog policy

1. Use the published catalog through `rootLibs` first.
2. Add an entry to `gradle/libs.versions.toml` only when the dependency is absent from the published
   catalog, and state the reason next to the entry.
3. Never hardcode a version in a `build.gradle.kts` file.

### 5.2 Build commands

The development environment is Windows with WSL; the toolchain lives on the Windows host.

```bash
powershell.exe .\gradlew.bat build
powershell.exe .\gradlew.bat :shared:allTests
powershell.exe .\gradlew.bat :desktopApp:run
```

`./gradlew`, `gradlew` and a locally installed `gradle` are invalid in WSL and must not be used. CI runs
on Linux and is the only place where `./gradlew` is correct.

### 5.3 Local environment

`local.properties` holds `sdk.dir` and is gitignored. `gradle/gradle-daemon-jvm.properties` pins the
daemon JVM and is committed. No build output, cache or IDE state is committed.

## 6. Testing

| Level | Scope | Requirement |
| --- | --- | --- |
| Unit | Pure engine and compiler behavior | Every rule named in this plan has a test asserting its full observable effect |
| Contract | Each public API invariant | One test per invariant that can be violated |
| Golden | Edit compilation | Exact resulting text, character for character |
| Differential | Shared helpers | When shared code changes, run the touched behavior against the previous implementation and compare output |
| UI | Compose surface | Preview-driven visual verification, no golden screenshots |

Rules:

- Behavior changes start with a failing test: observe red, then green, then refactor.
- A test asserts every observable effect of the rule it covers: returned edits, resulting text,
  preserved bytes outside the range, and that rejected input leaves the document unchanged.
- Tests exercise the public interface, never internal storage.
- Tests live in `commonTest` unless they need a JVM-only dependency.
- Flaky, order-dependent or time-dependent tests are defects, not tolerated costs.

## 7. Repository governance

### 7.1 Branch model

| Branch | Receives | Purpose |
| --- | --- | --- |
| `master` | Pull requests from `dev` only | Release line; every commit here is a release |
| `dev` | Pull requests from work branches | Integration line; the default working base |
| `feat/*`, `fix/*`, `ci/*`, `docs/*`, `refactor/*`, `test/*`, `chore/*`, `perf/*`, `build/*`, `revert/*` | Direct pushes by their author | One unit of work each |

Rules:

- A work branch always branches from `dev` and always targets `dev`.
- Only `dev` may target `master`. Nothing else ever reaches `master`.
- `master` is updated exclusively through a release pull request, then tagged.
- Direct pushes to `dev` and `master` are forbidden.
- Branch names match `^(feat|fix|ci|docs|refactor|test|chore|perf|build|revert)/[a-z0-9._-]+$`.

### 7.2 Commits

- One line only: `<type>(<scope>): <subject>` where `<scope>` is optional.
- Imperative mood, English, no trailing period, at most 72 characters.
- No body, no footers, no descriptions, no emoji, no attribution lines.
- One commit per reviewable work unit, roughly 400 authored changed lines, tests and documentation
  included in the same unit as the behavior they describe.

### 7.3 Pull requests

- Title follows the commit format exactly.
- Body is empty. No description, no template, no checklist.
- A pull request carries one unit of work and one reviewable diff.
- Reviews leave no inline comments. Feedback goes into the review summary or into the code itself.

### 7.4 Language

Every artifact is written in English: code, identifiers, KDoc, tests, fixtures, commit messages, pull
request titles, branch names, documentation, CI configuration and file names. Conversation language
does not change artifacts.

### 7.5 Comments

- KDoc (`/** ... */`) is allowed and required on public API declarations, and only there.
- No line comments (`//`) anywhere.
- No block comments other than public-API KDoc.
- No commented-out code, no TODO/FIXME markers, no author tags, no changelog notes in source.
- Intent is expressed through names, types and tests.

### 7.6 Plan references

Source code, tests, commit messages, pull request titles, branch names and documentation other than
this file never mention a milestone, a phase or this plan. Work is described by what it does, not by
where it sits in a roadmap.

### 7.7 Work tracking and memory

- ODD task documents are stored in Engram. Engram is the authoritative store.
- `odd/tasks/<branch-name>.md` is a per-branch working mirror created from Engram when a branch starts.
- `odd/` is gitignored and never committed, by decision.
- `.atl/`, `.opencode/`, `.gentle-shell/`, `.gentle-ai/`, `.pi/` and any other agent-tooling directory
  are gitignored from the first commit.
- Session knowledge worth keeping goes to Engram as a durable topic, not into repository files.

### 7.8 CI gates

| Gate | Enforces |
| --- | --- |
| `pr-policy` | Target branch is `dev` or `master`; `master` only accepts `dev`; work branches match the naming pattern; pull request body is empty; title follows the commit format |
| `commit-policy` | Every commit in the pull request has a single-line conventional subject, at most 72 characters, no body, no plan references |
| `comment-policy` | No `//` line comments and no non-KDoc block comments in Kotlin sources |
| `build` | `./gradlew build` on Linux with the Android SDK and a pinned JDK |

A pull request that fails any gate is not reviewable. Branch protection on `master` and `dev` requires
these checks to pass before merging.

### 7.9 Bootstrap exception

A repository cannot be governed before it exists. Exactly one bootstrap commit lands directly on
`master`, containing only repository governance: `.gitignore`, `README.md` and the CI workflows. From
that point on, `master` receives only `dev` pull requests and `dev` receives only work-branch pull
requests.

## 8. Milestones

Milestones are exit criteria, not calendar. A milestone closes when its criteria are demonstrated by
merged pull requests.

| Id | Scope | Exit criteria |
| --- | --- | --- |
| M0 | Foundation | Repository, build, CI gates, governance documents and a green build of a walking skeleton on both targets |
| M1 | Language contracts | `:language-api` public surface with KDoc and contract tests for every invariant |
| M2 | Block contracts | `:block-api` public surface with KDoc and contract tests for every invariant |
| M3 | Engine | Projection and edit compilation with golden tests for every rule in section 4 |
| M4 | Block UI | Compose renderer and gestures emitting `BlockEdit`s, verified through previews |
| M5 | Host | `:shared` document store, reparse loop, adapter registry and embeddable app surface |
| M6 | Entrypoints | Both applications open a sample document and edit it end to end |

## 9. Risks

| Id | Risk | Mitigation |
| --- | --- | --- |
| R-1 | Projection correctness on malformed input | Tolerant parse contract, golden tests over incomplete fragments, no throwing paths in projection |
| R-2 | AGP 9 KMP Android library plugin maturity | Versions pinned through the catalog; single module shape reused everywhere; upgrades are their own pull request |
| R-3 | Compose Multiplatform and Kotlin version compatibility | Both pinned by the published catalog; upgrades gated by a green build on both targets |
| R-4 | Review workload | Work units of roughly 400 authored lines, one behavior per pull request |
| R-5 | Silent regressions in shared edit helpers | Differential checks against the previous implementation when shared code changes |
| R-6 | Full-pass projection cost on large files | Measured budget per file size; incremental projection only with a passing budget |

## 10. Decision records

| Id | Decision | Rationale |
| --- | --- | --- |
| D-1 | Text is the single source of truth; blocks are a projection | Eliminates dual-model drift and makes every block operation a reviewable text edit |
| D-2 | Kotlin Multiplatform with exactly `android` and `desktop` targets | Both are JVM-backed; one shared core serves both products without a third toolchain |
| D-3 | Applications are entrypoints only; product code lives in `:shared` and below | Keeps product logic testable in common code and prevents platform forks |
| D-4 | Published catalog first, local catalog only as a documented fallback | One version authority for the whole Rotalex ecosystem |
| D-5 | No parser adapter ships in the framework core | Language support is a consumer concern: `:language-api` defines the adapter contract, the core carries no parser dependency, and the neutral DOM stays the only structural model |
| D-6 | Contracts split into `api` and `impl` modules per concern | Consumers depend on stable contracts; implementation can evolve without breaking them |
| D-7 | KDoc on public API only, no other comments | Names, types and tests carry intent; comments rot and duplicate |
| D-8 | One bootstrap commit on `master`, then release-only | Governance needs a first commit; every later change is reviewable |
| D-9 | Engram is authoritative for task documents, `odd/` is a gitignored mirror | Task state survives sessions without polluting the repository |
| D-10 | Merge commits, single-line commit subjects | History stays traceable to pull requests without verbose messages |
| D-11 | CI is the single automated enforcement point for repository policy | Rules that are not checked are not rules |
| D-12 | Publishing coordinates are deferred | No consumer exists yet; the coordinates are decided when the first publication happens |

## 11. Definition of done

A change is done when all of the following hold:

1. The behavior it adds or changes is covered by tests that assert the full observable effect.
2. `powershell.exe .\gradlew.bat build` passes locally and the CI gates pass on the pull request.
3. Public API additions carry KDoc; no other comments were added.
4. Every artifact is in English and free of plan references.
5. The pull request targets `dev` from a correctly named work branch, with a single-line title and an
   empty body.
6. This document was updated if a contract, invariant, module boundary, build rule or governance rule
   changed.

## 12. Glossary

| Term | Meaning |
| --- | --- |
| Document | The text buffer; the only stored representation |
| Neutral DOM | Language-neutral tree with ranges, produced by a language adapter |
| Block tree | Projection of the neutral DOM into blocks, fields and slots |
| Chrome | Read-only syntax text around children, rendered but not editable |
| Field | Inline editable token inside a block |
| Slot | Child position inside a block, constrained by a slot category |
| Carve | Generic projection that interleaves chrome with child slots |
| Opaque collapse | Unmapped node rendered as a single editable text field |
| Edit compiler | Component turning a block edit into minimal document edits |
| Walking skeleton | Thinnest end-to-end path that builds and runs on both targets |
