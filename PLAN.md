# RotaBlock — Engineering Plan

This document is the source of truth for RotaBlock. It defines what the project is, the contracts that
hold it together, how it is built, how it is reviewed, and how it grows. Code, tests and review
decisions follow this document; when they disagree, this document wins until a pull request changes both.

Two rules govern every edit to this file:

1. A change to a contract, invariant, module boundary, build rule or governance rule updates this file
   in the same pull request as the code it affects (§18, item 6).
2. Every requirement in this document has an identifier — `P-n` principles, `DEP-n` dependency rules,
   `I-n` invariants, `R-n` risks, `D-n` decisions, `Q-n` open questions, `M-n` milestones — and every
   cross-reference uses those identifiers, never prose. Section numbers are referenced as `§n.m`. A
   reference that does not resolve is a documentation defect (§10.6).

Milestone labels defined here are internal planning vocabulary and must never appear in source code,
commit messages, pull request titles or branch names (§13.6).

---

## 1. Purpose and authority

### 1.1 What this document is for

RotaBlock is a framework, not an application. A framework is defined by the contracts it imposes on its
consumers, so this document is written contract-first: every public type has a stated responsibility,
every rule has an identifier, and every decision carries the reason it was taken and the alternative it
rejected.

A reader should be able to answer, from this file alone: what the product is (§2), why it is designed
this way rather than the obvious alternatives (§3, §4, §16), what each module owns (§5, §6), how the
engine behaves at its edges (§7), how a language is added (§8), how a person edits (§9), what "done"
means (§18) and how the repository moves from where it is today to where this document describes (§20).

### 1.2 Precedence

| Order | Artifact | Authority |
| --- | --- | --- |
| 1 | This document | Contracts, invariants, module boundaries, build and governance rules |
| 2 | `AGENTS.md` | Session procedure for agents; it restates this document and never overrides it (§20.4) |
| 3 | Decision records in §16 | The reason a rule exists; they explain rules, they do not replace them |
| 4 | Code and tests | Evidence that a rule holds; a disagreement is a defect in code or tests, not here |

### 1.3 How to read it

- §2 to §4 answer *what* and *why* at the product level.
- §5 answers *where the code lives*.
- §6 answers *what the types promise*.
- §7 and §8 answer *how the engine and the parsers behave*.
- §9 answers *how a human edits*.
- §10 to §13 answer *how it is validated, built and reviewed*.
- §14 to §20 answer *in what order, at what risk, with what decisions, what is still open, what "done"
  means, and how the repository migrates*.

---

## 2. Product

### 2.1 Definition

RotaBlock is a language-neutral, projectional (block-based) editor framework for Kotlin Multiplatform,
distributed as a library plus two reference entrypoints.

The product invariant is singular and non-negotiable:

> There is one document. **Text is the only stored representation.** Blocks are a live projection of the
> parsed structure of that text, and every block edit compiles back into a minimal text edit.

There is no parallel block model, no block serialization format, no view-to-view synchronization and no
tree that owns text. A keystroke in the text surface and a drag in the block surface are the same kind of
change: a surgical edit of one document, followed by reparse and re-projection.

This choice is the single most consequential decision in the project (`D-1` in §16). It removes the
classical failure mode of projectional editors — two sources of truth that drift, and a document that
cannot be edited in any other tool — at the cost of paying for parsing and projection on every change.
The rest of this document is largely about paying that cost honestly (§7.2, §11).

### 2.2 What ships

| Artifact | Role |
| --- | --- |
| `:core:language` | Document, neutral DOM, ranges, document edits, adapter contract, extension points |
| `:core:block` | Block model, block edits, mapping SPI, layout policy, projection service contract |
| `:core:engine` | Projection engine and edit compiler — the only implementation of the two core services |
| `:core:testkit` | Conformance suites every adapter and mapping must pass (§8.6) |
| `:parser:tree-sitter` | Parser adapter over the official Kotlin tree-sitter bindings (§8.1) |
| `:lang:json` | JSON grammar binding and block mapping — the reference language (§8.4) |
| `:ui:block` | Compose Multiplatform block renderer and gestures (§9) |
| `:shared` | Composition root: document store, reparse loop, registry wiring, embeddable surface |
| `:androidApp` | Android entrypoint only |
| `:desktopApp` | Desktop (JVM) entrypoint only |

The module graph, the naming rationale and the migration from the current names are §5.

### 2.3 Non-goals

Framework non-goals, each with the decision that rejects it:

- **No block document format, no project file, no workspace persistence in the core** (`D-1`): the
  document is text, and any text file is already a valid document.
- **No parser inside `:core:*`** (`D-5`, amended by `D-15`): parsing is an extension module, so the core
  keeps no parser dependency and no language-specific node model.
- **No non-JVM targets yet** (`D-2`): Android and desktop are both JVM-backed. The parser substrate is
  multiplatform and does support Native, so adding a target later is a decision record, not a rewrite
  (`Q-1`).
- **No editor features outside the block projection model**: no terminal, no git client, no language
  server, no debugger.
- **No collaborative editing engine in the core** (`D-26`): the document seam is CRDT-ready but the core
  implements no synchronization, and no distributed state enters `commonMain`.

### 2.4 Consumers

A consumer embeds `:shared` (or the `:core:*` modules plus its own choices) into a host application,
supplies or reuses a parser adapter, supplies a `BlockMapping` per language, and renders the block
surface. Consumers therefore depend on contracts (§6), never on engine internals.

The plan is written for three audiences, and the ordering above matters: a consumer integrating the
framework, a contributor adding a language (§8.5), and a maintainer changing the engine (§7).

### 2.5 Supported targets

| Target | Declared as | Status |
| --- | --- | --- |
| Android | `com.android.kotlin.multiplatform.library` | Required (`D-2`) |
| Desktop JVM | `jvm("desktop")` | Required (`D-2`) |
| Kotlin/Native, JS, Wasm | not declared | Rejected for now (`D-2`, `Q-1`) |

---

## 3. Design principles

Principles are the tie-breakers. When two rules in this document conflict, the principle with the lower
number wins, and the conflict is a documentation defect to fix.

| Id | Principle | Consequence |
| --- | --- | --- |
| P-1 | **Text is the truth.** | Every mutation is a `DocumentEdit`; nothing else is authoritative (§6.1). |
| P-2 | **Structure is derived, never stored.** | The block tree is disposable and reproducible from text plus registrations (`D-1`). |
| P-3 | **Every rule is testable through the public surface.** | If a rule cannot be asserted from outside, it is not a contract (§6.10, §10). |
| P-4 | **The core is language-blind.** | `:core:*` never names a language, a grammar or a syntax construct (`DEP-1`). |
| P-5 | **Edits are surgical and minimal.** | An edit changes the smallest text span that expresses the intent; bytes outside it are untouched (`I-3`). |
| P-6 | **Absence of knowledge is a feature, not a crash.** | Unknown kinds, unknown values and unparsable regions degrade to an editable field, never to an exception (`I-5`, §7.6). |
| P-7 | **Incremental behaviour must be observationally identical to full behaviour.** | An optimization that changes any observable result is a defect, not an optimization (`I-18`). |
| P-8 | **Registration is explicit; nothing is discovered.** | No service loader, no reflection, no global mutable singleton in `commonMain` (§6.9, `D-11`). |
| P-9 | **A consumer never needs to read the engine.** | Contracts in `:core:block` are sufficient to render, extend and test a consumer's own language (§2.4). |

`P-4` is what makes `:lang:json` and a future `:lang:kotlin` possible without touching the core, and
`P-7` is what keeps the incremental path from silently becoming a second implementation of the product.

---

## 4. Prior art and what we take from it

A block editor is not a new idea, and the ways it usually fails are documented. This section records
what was studied, what was taken, and what was rejected — it is the evidence behind §16.

### 4.1 Projectional editors: MPS and grammar cells

JetBrains MPS stores an abstract syntax tree as the document and projects notation from it. It proves
projectional editing works at industrial scale, and it also demonstrates the cost: the stored model is
not a text file, so the document escapes the rest of the toolchain, and a textual notation must be
fully specified for every language. Voelter et al. (grammar cells, SLE 2016) add the second lesson:
projectional editors become unusable not because projection is hard, but because the *interaction* is
specified per construct, so consistency across constructs and languages decays.

**Taken**: notation is declarative and per kind, not hand-drawn per screen — here, `BlockMapping` plus
layout policy (§6.6, §6.7). **Rejected**: storing the tree as the document (`D-1`).

### 4.2 Incremental parsing: tree-sitter, Lezer and Roslyn

tree-sitter is a parser generator that produces a concrete syntax tree, reuses unchanged subtrees on
edit (`InputEdit`), and recovers from syntax errors instead of failing — its stated goals are exactly
"fast enough to parse on every keystroke" and "robust in the presence of syntax errors". CodeMirror's
Lezer reaches the same place differently, reusing `TreeFragment` slices tagged with an offset delta.
Roslyn (C#) keeps an immutable *green* tree with structural sharing and a *red* tree with parents and
positions, so an editor can hold many versions cheaply.

**Taken**: tree-sitter as the substrate and its incremental reuse model (§8.1); immutable projection
nodes with structural sharing so a version is cheap to retain (§7.2); error recovery as a first-class
input rather than an exception path (`I-5`, §7.6). **Rejected**: position-carrying red trees, because
ranges in this design are derived from the parse and carried on blocks, so a second positional layer
would duplicate state (`D-19`).

### 4.3 Structured editing without losing free editing

Tratt (2024) argues that the historical objection to structured editing — that it must reject
ill-formed intermediate states — is largely solvable with incremental parsing, because the editor can
always accept text and re-derive structure. Beckmann et al. (CHI 2023) demonstrate structured editors
derived from tree-sitter grammars with an explicit *input reconciliation* step: what the user types is
matched against the tokens the grammar expects at that position, and only then applied.

**Taken**: text is always accepted; structure is reconciled afterwards (§9.5, `D-20`); the grammar, not
the mapping author, decides which tokens are legal in a hole, wherever the grammar can answer (§8.4).
**Rejected**: modal restriction of input to legal tokens only, because it makes typing unreliable in
exactly the cases a user needs to repair syntax.

### 4.4 Editor architecture patterns

| Pattern | Where it applies here | Why |
| --- | --- | --- |
| Ports and adapters (hexagonal) | `:core:*` are ports; `:parser:*`, `:lang:*`, `:ui:*` are adapters | Keeps the core language-blind (`P-4`, `DEP-1..DEP-4`) |
| Microkernel with an explicit registry | `ExtensionPoint` as the only registration mechanism (§6.9) | No discovery, no reflection (`P-8`) |
| Command | `BlockEdit` variants are commands with a compiler (§6.5) | Each intent has one place that turns it into text |
| Composite | `BlockNode` over `BlockPart` (§6.3) | Render, serialize and traverse share one shape |
| Interpreter / visitor over the parse | `BlockMapping.project` (§6.6) | A language describes decomposition without owning traversal |
| Memento / transaction log | `Document` history of inverse edits (§7.5) | Undo is a text operation, not a tree snapshot |
| Flyweight / structural sharing | Immutable nodes reused across versions (§7.2) | Retaining a version must not cost a full copy |
| Unidirectional data flow (MVI) | `:ui:block` renders state and emits edits (§9.1) | The UI holds no document state |
| Rope / piece table | Deferred behind the `Document` seam (§6.1, `Q-2`) | The seam exists now; the structure is an optimization |
| CRDT (Fugue/Yjs family) | Explicitly out of the core (`D-26`, §7.5) | Collaboration must not shape the core's contracts |

### 4.5 What the prior art does not answer

Three things have no settled answer in the sources above, and this plan decides them itself, with the
reason recorded:

1. **Comment ownership** (`D-22`, §6.8). No tree-sitter grammar attaches trivia; the mapping has to.
2. **Layout** (`D-21`, §6.7). Incremental parsers do not format; a language-blind whitespace rule
   cannot serve JSON, Kotlin and Python at once (§4.6).
3. **Interactive latency budget on mobile** (`D-25`, §11). Published editors target desktop; the
   numbers here are set for a mid-range Android device.

### 4.6 Why one global whitespace rule was rejected

The first version of this plan specified a single engine-wide whitespace policy: strip a trailing
semicolon, then trailing spaces, then at most one line break. It is correct for exactly one family of
languages and wrong for the rest:

| Language | What breaks under a global rule |
| --- | --- |
| JSON | Comma separators must be added or removed with the element; a trailing comma is invalid |
| Python, YAML | Indentation is syntax; removing it changes meaning |
| Kotlin, Java | Braces and blank lines are conventions, comments may sit between the element and its separator |
| Markdown | A line break is meaningful; a list item owns its following indented block |

So layout becomes a per-language policy supplied with the mapping (`D-21`, §6.7), and the engine only
defines when it is consulted.

---

## 5. Architecture

### 5.1 The loop

```
        text (Document: immutable text + version)          §6.1
                 │  parse (incremental)
                 ▼
        parse tree (tree-sitter, byte ranges)               §8.1
                 │  adapt (byte → UTF-16 ranges)
                 ▼
        neutral DOM (:core:language)                        §6.2
                 │  project
                 ▼
        block tree (:core:block contracts, :core:engine)    §6.3, §7.1
                 │  render / gesture
                 ▼
        block surface (:ui:block)  ── emits BlockEdit ──┐   §9
                 │                                      │
                 ▼                                      ▼
        text surface (free editing)          edit compiler (:core:engine)   §7.3
                 │                                      │
                 └──────────► DocumentEdit(s) ◄─────────┘
                                   │ apply
                                   ▼
                     new text + version → reparse → re-project
```

Both surfaces write to the same document, and neither reads the other. The text surface changes text
directly; the block surface changes text through the compiler. After either, the loop restarts from
text.

### 5.2 Layers and allowed knowledge

| Layer | Knows about | Must never know |
| --- | --- | --- |
| Document (`:core:language`) | Text, ranges, edits, versions | Blocks, parsers, UI |
| Neutral DOM (`:core:language`) | Kinds, ranges, children | Blocks, which language produced it |
| Block contracts (`:core:block`) | DOM, block model, intents, mappings | Parsers, Compose, any concrete language |
| Engine (`:core:engine`) | The two contracts it implements | Compose, any concrete language |
| Parser adapter (`:parser:*`) | tree-sitter and the DOM it produces | Blocks, UI |
| Language (`:lang:*`) | Its grammar and its mapping | Other languages, UI |
| UI (`:ui:*`) | Block tree and intents | Text internals, parsing, document mutation |
| Host (`:shared`) | Everything, as wiring only | Product rules that belong in core |

### 5.3 Module graph

```
:androidApp ─┐
             ├─► :shared ─┬─► :ui:block ──────────────► :core:block ─► :core:language
:desktopApp ─┘             ├─► :core:engine ───────────► :core:block
                           ├─► :lang:json ─────────────► :parser:tree-sitter ─► :core:language
                           │                        └─► :core:block
                           └─► :core:language

test only:   every module ─► :core:testkit
build only:  build-logic ─► the plugin that compiles native code in :lang:* modules
```

The graph is a DAG. The engine is not on the language path: a language module supplies data
(grammar + mapping) and the engine consumes it, so adding a language never touches the engine.

### 5.4 Naming rationale

The previous names `:block-api` and `:block-impl` were rejected for three reasons:

1. **`api`/`impl` describes a build relationship, not a responsibility.** They say how a module is
   consumed, not what it owns, and they invite the belief that the pair is a contract plus its private
   implementation — which is false here: the engine is a public service others may substitute.
2. **Grouping by kind of artifact (`api`, `impl`, `ui`) hides the layering.** A reader must infer the
   graph from names instead of reading it: `:core:engine` depends on `:core:block` because an engine
   consumes blocks, which the name now says.
3. **A parent module makes the boundary enforceable.** `:core:*` as a prefix is a rule a build can
   check and a reviewer can see at a glance: anything under `:core` is language-blind (`P-4`).

| Current | Target | Reason |
| --- | --- | --- |
| `:language-api` | `:core:language` | Owns the document and the neutral structural model |
| `:block-api` | `:core:block` | Owns the block contracts; not "the API half of a pair" |
| `:block-impl` | `:core:engine` | It *is* the engine: projection and compilation |
| — | `:core:testkit` | New: conformance suites, so contracts are testable by consumers (§8.6) |
| — | `:parser:tree-sitter` | New: the adapter over the parser substrate (§8.1) |
| — | `:lang:json` | New: reference language (§8.4) |
| `:block-ui` | `:ui:block` | Grouped with the other view modules; UI is not core |
| `:shared` | `:shared` | Unchanged by request: the composition root *is* an application concern |
| `:androidApp`, `:desktopApp` | unchanged | Application entrypoints, not framework |

### 5.5 Dependency rules

| Id | Rule |
| --- | --- |
| DEP-1 | `:core:*` depends only on `:core:*`. No core module depends on a parser, a language, a UI module or an application. |
| DEP-2 | `:core:engine` depends on `:core:block` (and transitively `:core:language`). |
| DEP-3 | `:parser:*` depends on `:core:language` and on its parser library, and on nothing else. |
| DEP-4 | `:lang:*` depends on `:parser:tree-sitter`, `:core:block` and its grammar artifact, never on another `:lang:*`. |
| DEP-5 | `:ui:*` depends on `:core:block` only: a renderer needs blocks and intents, not the engine. |
| DEP-6 | `:shared` may depend on anything; nothing depends on `:shared`. |
| DEP-7 | Application modules depend only on `:shared` and contain no product logic. |
| DEP-8 | `:core:testkit` is consumed only as a test dependency (`testImplementation`), never as `implementation`. |
| DEP-9 | No module depends on an application module, and the module graph has no cycles. |
| DEP-10 | `commonMain` of every multiplatform module is free of JVM-only types and dependencies. A multiplatform library is not a JVM-only dependency, so it is legal in `commonMain` (`D-15`). |
| DEP-11 | A module never reaches into another module's `internal` declarations. |

`DEP-5` has a practical consequence worth stating: the UI cannot call `project`. It receives a
`BlockTree` and emits `BlockEdit`; the host applies them through `BlockProjectionService`. That is what
keeps rendering pure (§9.1).

### 5.6 Package and namespace convention

| Concern | Value |
| --- | --- |
| Base package | `dev.rotalex.rotablock` |
| Kotlin packages | Base package plus the module path without the group prefix: `dev.rotalex.rotablock.core.language`, `dev.rotalex.rotablock.core.block`, `dev.rotalex.rotablock.core.engine`, `dev.rotalex.rotablock.parser.treesitter`, `dev.rotalex.rotablock.lang.json`, `dev.rotalex.rotablock.ui.block` |
| Android namespaces | Base package plus the module path with dots: `dev.rotalex.rotablock.core.block` |
| Android application id | `dev.rotalex.rotablock` |
| Desktop main class | `dev.rotalex.rotablock.desktop.MainKt` |

No module declares a package outside the base package, and a package never spans two modules.

### 5.7 Source-set strategy

Every framework module is Kotlin Multiplatform with exactly the targets in §2.5.

| Source set | Holds | Rule |
| --- | --- | --- |
| `commonMain` | Everything language- and platform-neutral | No JVM-only type or dependency (`DEP-10`) |
| `androidMain`, `desktopMain` | Platform actuals, platform configuration | No duplicated product logic |
| `jvmSharedMain` | Code both JVM targets share but `commonMain` must not see | Introduced only when needed |
| `commonTest` | Contract, conformance and golden tests | The default place for tests (§10.3) |
| `<platform>Test` | Tests requiring a platform API or a real device budget | Must state why `commonTest` cannot host them |

The parser adapter is where this matters: the tree-sitter binding is multiplatform, so the adapter lives
in `commonMain` and no JVM-only parser dependency is introduced (`D-15`, `R-7`).

### 5.8 Module migration

The rename is mechanical but touches every file. §20 holds the ordered migration with the exact steps,
the compatibility decisions and the review slicing. Until that migration lands, `AGENTS.md` describes
the current names and this section describes the target: that is a documented, temporary divergence,
not a contradiction, because §20.1 assigns it an owner and an order.

---

## 6. Contracts

Every type below is public API of its module, carries KDoc (`I-10`) and has contract tests in
`:core:testkit` or the module's `commonTest`.

### 6.1 Document and text

`Document` is the only owner of text and the only mutator of it (`P-1`, `I-17`). It did not exist in the
first version of this plan, and its absence was the largest structural gap: edits compiled into nothing,
and there was no place to define range units, versions, undo or byte conversion.

| Member | Contract |
| --- | --- |
| `text: CharSequence` | The current content. Truth (`I-1`). |
| `version: Long` | Monotonic; increments by exactly one per applied transaction (§7.5). |
| `length: Int` | Number of UTF-16 code units. |
| `edit(edits: List<DocumentEdit>): Document` | Applies a batch atomically and returns a new `Document` with `version + 1`. Rejects overlapping or out-of-bounds edits instead of guessing (§6.5). |
| `slice(range: TextRange): String` | The text of a range. |
| `lineOf(offset: Int): Int`, `offsetOf(line: Int, column: Int): Int` | O(log n) line indexing. |
| `byteOffset(charOffset: Int): Int`, `charOffset(byteOffset: Int): Int` | O(log n) conversion across the parser boundary (§8.2). |
| `utf8: ByteArray` | The text as UTF-8 bytes, memoized per version, for the parser. |

Rules:

- **Range unit is the UTF-16 code unit**, because that is Kotlin's `String` index and every consumer
  surface (Compose text layout, caret, selection) is specified in it (`D-13`). Parser bytes are converted
  at the seam (§8.2), never leaked into block or UI contracts.
- `Document` is immutable; every mutation returns a new instance. Retaining the previous version must be
  cheap, which is why the text structure is behind the seam and may later become a rope or piece table
  without touching `Document` consumers (`Q-2`).
- A batch is atomic: either every edit in `edit(...)` applies or none does (§6.5).
- `Document` carries no blocks, no parse tree and no selection. Those are derived or host-owned (`P-2`).

### 6.2 Neutral DOM

| Type | Contract |
| --- | --- |
| `TextRange(start, end)` | Half-open `[start, end)` in UTF-16 code units; `length`, `isEmpty`, `contains(offset)`. Negative or inverted ranges are unconstructible (`I-12`). |
| `NodeKind(id)` | Value class over a stable string id, `Comparable`, namespaced by the adapter that produces it (§6.9, `D-23`). Neutral constants are `ROOT` and `ERROR` only. |
| `DomNode(kind, range, source, children)` | Children are in source order, non-overlapping, and contained by the parent range. `text()` returns its source slice. `parent` is set on construction. |
| `ParsedFile(source, documentVersion, root)` | The adapter's output: a whole document parsed at one version. |
| `DocumentEdit(offset, oldLength, newText)` | The only write primitive (§6.5). |
| `LanguageAdapter(language, parse(source, version): ParsedFile)` | Total over malformed input: it returns a `ParsedFile` for any text and marks unparsable regions with `ERROR` (`I-5`, §7.6). |

Constraints that the DOM must keep, because §7 depends on them:

- **Children do not overlap.** `carve` (§7.1) interleaves the literal gap between consecutive children,
  which is only well-defined for a non-overlapping sequence.
- **Ranges are exact.** A node's range covers exactly its own text, including the syntax it owns, so that
  a block edit can address it without re-parsing.
- **Identity is never used to match nodes.** Parsers rebuild nodes on every edit; matching is by range and
  by kind (`D-24`).

### 6.3 Block model

| Type | Contract |
| --- | --- |
| `BlockNode` | `id`, `kind`, `category`, `anchor`, `range`, `parts`, `label`, `valueKind`. `fields` and `slots` are computed views over `parts`. |
| `BlockPart` | Sealed: `Field(BlockField)` or `Slot(BlockSlot)`; order is render and serialize order. |
| `BlockField` | `role`, `text`, `editable`, `range`. An editable field always carries a range (`I-6`); chrome may carry none. |
| `BlockSlot` | `category`, `children`, `multiple`, `range`, `valueKind`. |
| `SlotCategory` | Closed set: `STATEMENT`, `EXPRESSION`, `TYPE`, `DECLARATION`, `NAME`, `MODIFIER`, `PARAMETER`, `ARGUMENT`, `OPAQUE`. `OPAQUE` accepts anything. |
| `ValueKind` | Closed set: `BOOLEAN`, `NUMBER`, `STRING`, `OBJECT`, `ARRAY`, `TYPE`, `NULL`, `UNKNOWN`. |
| `BlockTree` | `root`, `version`, `language`, `block(id)`, `blockAt(offset)`, `enclosing(range)`. The language travels with the tree because compilation asks that language's layout rules, and a tree is what an intent is compiled against. |

Two corrections to the first version of this plan, both forced by making `I-9` enforceable:

- **`BlockNode` gains `category`** (`D-17`). Without it a slot cannot answer "does this slot accept this
  block" from the model, so `I-9` was unenforceable and the engine silently accepted everything. A block's
  category is what its mapping declares for that kind, and `opaque` blocks are `OPAQUE`.
- **`ValueKind` gains `ARRAY` and `NULL`** (`D-17`). JSON forced this: an array is not an object, and
  `null` is a value, so collapsing both into `OBJECT` and `UNKNOWN` would make the socket affordance lie.

`SlotCategory` answers "may this go here?" — structural grammar. `ValueKind` answers "what shape is
this?" — rendering affordance. They stay orthogonal and are never merged (§4.1).

### 6.4 Identity and references

| Type | Contract |
| --- | --- |
| `BlockId` | Value class over a non-blank string. Unique within one projection, never persisted (`I-7`). |
| `BlockRef(id)` | Names a block; what an edit carries. |
| `SlotRef(owner, slotIndex, index)` | Names a position in a slot; `index == children.size` appends. |

Id derivation changed from the first version (`D-19`): an id is **derived from the parse position of the
node it projects**, not from a traversal counter. A counter makes every id invalid after any edit, which
destroys focus, selection, drag state and incremental projection. The derivation is the node's path from
the root (kind plus index within parent), which is stable for unchanged structure and cheap to compute
during projection. It is not persisted and carries no meaning outside the projection that produced it.

References are valid only against the tree they were computed from. A reference that no longer resolves
produces an empty edit list, never an exception (`I-4`).

### 6.5 Edits

`DocumentEdit` is the only write primitive and every block intent compiles to a list of them.

| Variant | Meaning | Compiled as |
| --- | --- | --- |
| `SetField(target, fieldIndex, text)` | Replace one editable field's text | One replace over the field range |
| `ReplaceWithText(slot, text)` | Replace the content at a slot position with text to be reparsed | One replace over the addressed span |
| `Delete(block)` | Remove a block and the syntax that existed only to hold it | One delete over the block range plus owned syntax, per layout policy (§6.7) |
| `InsertTemplate(slot, template)` | Insert a template at a slot position | One zero-length insert at the insertion offset, plus the separators layout policy requires |
| `Wrap(block, template)` | Wrap a block in a control template | One replace over the block range |
| `Move(block, target)` | Move a block to another slot position | Two disjoint edits, returned in descending offset order |

Normative examples, because "minimum number of edits" alone is not testable (JSON, §8.4):

| Input | Intent | Resulting text |
| --- | --- | --- |
| `{"a": 1, "b": 2}` | `Delete` the pair `"a": 1` | `{"b": 2}` — the element **and** its trailing separator |
| `{"a": 1, "b": 2}` | `Delete` the pair `"b": 2` | `{"a": 1}` — the preceding separator, because a trailing comma is invalid |
| `{}` | `InsertTemplate` a pair | `{"key": null}` — the template's text, no separator, since the object was empty |
| `{"a": 1}` | `InsertTemplate` a pair | `{"a": 1, "key": null}` — separator inserted before the element |
| `[1, 2]` | `Move` `1` after `2` | `[2, 1]` — two disjoint edits, descending offset |

Rules:

- Edits in a returned batch are **disjoint** and are applied in **descending offset order**, so no offset
  is invalidated by a previous edit (`I-3`, `I-16`).
- Bytes outside the returned ranges are byte-identical (`I-3`).
- A batch is atomic at the `Document` level (§6.1): a partially applicable batch is a compiler defect.
- Every variant is deterministic: the same edit against the same tree yields the same
  `List<DocumentEdit>` (`I-15`).
- Adding a variant requires a compiler branch, golden tests and an entry in this table (§18, item 1).

### 6.6 Mapping SPI

A `BlockMapping` describes one language, or one host dialect, to the engine.

| Member | Contract |
| --- | --- |
| `languages: Set<LanguageId>` | The languages this mapping serves. |
| `handles: Set<NodeKind>` | The decomposition boundary (`I-5`): a handled child becomes its own block, an unhandled one collapses to a single editable field. |
| `categories: Map<NodeKind, SlotCategory>` | The category a produced block declares, which is what makes `I-9` decidable (§6.3). |
| `layout: LayoutPolicy` | The whitespace and separator rules of this language (§6.7). |
| `project(node, context): BlockNode` | Defaults to `ProjectionContext.carve`: the generic interleave of chrome and projected children. |
| `serialize(block): String` | Defaults to concatenating parts in order, recursing into slots. Runs only on created or edited blocks, never on the whole file. |
| `notation(kind): KindNotation?` | Optional declarative description of how a kind is presented and what may be typed into its holes (§9.5, `D-20`). |

`ProjectionContext` is the seam between a mapping and the engine: `source`, `textOf`, `carve`, `child`,
`node`, and the `block`/`slot`/`field` factories that let the engine own id assignment and category
resolution.

A mapping is a **pure description**. It never mutates, never owns a document, never decides ids and never
touches text. That is what lets one engine serve every language (`P-4`) and what makes a language module a
data module (`DEP-4`).

### 6.7 Layout policy

Layout is the difference between "the edit is correct" and "the edit is acceptable" (§4.6). It is a
per-language value, not an engine rule (`D-21`).

| Member | Contract |
| --- | --- |
| `separator: SeparatorStyle` | `None`, `LineBreak`, `Comma`, or `Custom(leading: String, trailing: String)`. |
| `trailingSeparator: Boolean` | Whether the last element in a list carries a separator. |
| `indent: IndentStyle` | `None`, or `Spaces(unit)` / `Tabs`, with the rule for nested statements. |
| `onDelete(elementRange, context): List<DocumentEdit>` | The language's own answer for which owned syntax disappears with an element. |
| `onInsert(offset, elementText, context): List<DocumentEdit>` | The language's own answer for which separators appear. |

The engine never computes whitespace itself; it asks the mapping's policy and validates the result
(`I-3`, `I-16`). A language that has nothing to say uses `LayoutPolicy.NONE`, which produces the naive
result and is only correct for a language where whitespace is insignificant.

This is where Python, YAML, Markdown and JSON diverge, and why the first version's global rule was
rejected rather than patched (`§4.6`).

### 6.8 Trivia and comments

Grammars do not attach comments; a CST reports them as nodes somewhere between the constructs they
comment. If the framework does not decide ownership, `Delete` and `Move` will silently drop or relocate
comments, which destroys a user's trust faster than any rendering bug (`D-22`).

Rules:

- **A comment belongs to the nearest following named sibling** at the same nesting level. If there is
  none, it belongs to the nearest preceding sibling.
- A comment inside a block's range but outside any child belongs to that block's **trailing** trivia.
- `Delete(block)` removes the block's own leading trivia only when that trivia is separated from the
  previous sibling by nothing but whitespace; otherwise the trivia survives, attached to its new
  neighbour (`I-14`).
- `Move(block, target)` moves the block's leading trivia with it and leaves trailing trivia in place;
  a trailing comment that named the moved block is not the framework's to rewrite.
- A blank line between two elements is trivia of the second element's leading region, which is what makes
  the "remove at most one line break" instinct mostly right for line-separated languages and wrong for
  comma-separated ones.
- Editing inside a comment is `SetField` on a comment block: comments project as opaque editable blocks,
  never as chrome.

### 6.9 Extension points and registry

`ExtensionPoint<T>` is the only registration mechanism (`P-8`, `D-11`): `register(value)` appends in call
order, `extensions()` returns an ordered immutable snapshot. There is no service loader, no reflection and
no global mutable singleton in `commonMain`.

Points defined by the core:

| Point | Registers | Declared in |
| --- | --- | --- |
| `LANGUAGE_ADAPTER_EP` | `LanguageAdapter` | `:core:language` |
| `BLOCK_MAPPING_EP` | `BlockMapping` | `:core:block` |

Registry rules (`D-18`):

- A registry is resolved **once per document version**, not per block, and the resolved set is passed to
  the engine.
- Two adapters claiming the same `LanguageId`, or two mappings claiming the same language and the same
  kind, are a **startup error**: the host reports it and refuses the language (`I-20`). The first version
  silently took the first registration, which turns a packaging mistake into a mysterious projection.
- Resolution order is registration order, and it is stable across runs (§7.2 determinism).

### 6.10 Invariants

| Id | Invariant | Enforced by |
| --- | --- | --- |
| `I-1` | Text is the only stored representation; no block tree owns or mutates text. | `Document` (§6.1) |
| `I-2` | Every block anchors to a DOM node and range, or is synthetic chrome with a null anchor. | Model (§6.3) |
| `I-3` | A block edit compiles to one or more disjoint document edits; everything outside their ranges is byte-identical. | Compiler (§7.3) |
| `I-4` | Edits are computed against the tree they were derived from; stale references produce an empty list, never an exception. | Compiler (§7.3) |
| `I-5` | An unmapped or unparsable node collapses into an editable text field: no construct is ever a dead end. | Engine (§7.1, §7.6) |
| `I-6` | Every editable field carries a range; chrome may not. | Model constructor |
| `I-7` | Block ids are unique within one projection, derived from parse position and never persisted. | Engine (§6.4) |
| `I-8` | Projection is pure and deterministic: same document, same registrations, same tree. | Engine (§7.1) |
| `I-9` | A slot accepts only blocks whose category it accepts, or anything when it is `OPAQUE`. | Model plus engine (§6.3) |
| `I-10` | Public API is documented with KDoc; no other comments exist in source. | Review and lint (§13.5) |
| `I-11` | Every range is half-open, non-negative and within its source. | Constructors |
| `I-12` | `commonMain` never references a JVM-only type or dependency. | Build plus review (`DEP-10`) |
| `I-13` | **Round trip**: for an untouched document, `parse → project → serialize(whole)` reproduces the text byte for byte. | Golden tests (§10.2) |
| `I-14` | **No silent trivia loss**: no edit drops or relocates a comment except under the rules of §6.8. | Golden tests (§10.2) |
| `I-15` | **Deterministic compilation**: the same edit against the same tree yields the same edit list. | Unit tests (§10.1) |
| `I-16` | Returned edits are disjoint and ordered by descending offset. | Compiler (§7.3) |
| `I-17` | All text mutation goes through `Document`; no module concatenates text to build a document. | Review plus `DEP-1` |
| `I-18` | **Incremental equals full**: incremental projection is observably identical to a full projection of the same document. | Differential tests (§10.5) |
| `I-19` | A parse tree and its block tree never outlive the version that produced them. | Host (§7.4) |
| `I-20` | A registry with two claimants for the same language or kind is rejected at startup, never resolved by chance. | Registry (§6.9) |

`I-13` is the invariant the first version of this plan lacked entirely, and it is the first one to
implement: without it nothing prevents the framework from corrupting a document it also renders.

---

## 7. Engine semantics

### 7.1 Projection

`carve(node)` produces the ordered parts of a block: for each child, the literal source between the
previous child's end and this child's start becomes one read-only chrome field, followed by one
single-child slot holding the projected child; after the last child, the remaining source becomes
trailing chrome. A node with no children becomes a leaf: one editable field carrying its text.

| Node shape | Resulting block |
| --- | --- |
| Handled kind, has children | Chrome and slots by `carve`, category from `categories[kind]` |
| Unhandled kind, has children | One editable field over the whole node range (`I-5`) |
| Any kind, no children | One editable field over its text (`I-5`) |
| `ERROR` node from the parser | Opaque editable block, marked as error, never chrome (§7.6) |
| Node with zero-width range | Synthetic chrome block with a null anchor and no field |

Projection is a full pass over the DOM, then refined by §7.2. It is pure (`I-8`) and must not depend on
mutable state outside the projection context.

### 7.2 Incremental projection

Incremental projection is not an optional refinement in this design; the loop of §5.1 runs on every
keystroke and the parser hands back a partially reused tree.

Contract:

1. A projection is keyed by `(Document.version, resolved registrations)`.
2. Given the previous tree and the set of ranges that changed, the engine re-projects only blocks whose
   range intersects a changed range or whose ancestors' chrome changed.
3. The result must be **observably identical** to a full projection (`I-18`), including ids, part order
   and value kinds. Divergence is a defect in the incremental path, never an acceptable difference.
4. If the engine cannot decide which blocks are affected — an edit that changes structure outside the
   changed ranges, a registry change, a version gap — it falls back to a full projection. Correctness
   first, and the fallback is measured (§11).
5. Nodes are immutable and shared between versions where unchanged, so retaining the previous tree is
   cheap (`D-19`).

### 7.3 Edit compilation

Compilation turns one `BlockEdit` into text edits against the tree it was derived from.

Steps for every variant:

1. **Resolve** the reference against the tree. Failure yields an empty list (`I-4`).
2. **Derive** the spans: the target element, the syntax it owns, and the separators the layout policy
   requires (§6.7).
3. **Emit** the minimal list, ordered by descending offset and provably disjoint (`I-3`, `I-16`).
4. **Verify** that applying the list to the document reproduces the intended text and changes nothing
   else. This check is part of the engine, not only of the tests.

### 7.4 Staleness and versions

`BlockTree.version` mirrors `Document.version`. A host compares versions instead of diffing trees. An
edit computed against a stale tree is discarded by the host, not by the engine, because only the host
knows what the user meant by the action. A parse tree and its block tree never outlive the version that
produced them (`I-19`).

### 7.5 Transactions and undo

An **interaction**, not a keystroke, is the unit of undo.

- The host opens a transaction, accumulates the applied `DocumentEdit` batches and closes it.
- `Document` records, per transaction, the inverse edits that restore the previous text.
- Undo applies the inverse batch as a new transaction, so undo is itself observable and re-projectable.
- A transaction that empties a document region has no special case: text is the state.
- Collaborative editing is out of scope (`D-26`), but `Document` is the seam where an operation log would
  attach, which is why no tree state ever needs to be transferred.

### 7.6 Error handling

Parsers are total and recover; the framework must not have a second failure mode.

| Situation | Behaviour |
| --- | --- |
| Parser reports `ERROR` node | Project as an opaque editable block, flagged as error, with the error text editable as a whole |
| Document is empty | Root block with a single empty, editable slot; never a tree with no blocks |
| Kind has no mapping | Opaque collapse (`I-5`) |
| Reference no longer resolves | Empty edit list (`I-4`) |
| Registry conflict | Startup error, language refused (`I-20`) |
| Edit would produce text the parser rejects | Applied as text, then projected with an error block: the framework never refuses a user's keystroke (`§4.3`) |

### 7.7 Value kinds

The expected `ValueKind` of a slot comes from the position when the mapping declares it, and from the
content otherwise. A `ValueKindOracle` is consulted first and may refine any block's produced kind; a
null answer falls back to the syntactic heuristic, which is `UNKNOWN` and claims nothing. Hosts wire a
semantic oracle (a compiler, a type checker) without touching the engine, and the oracle must be pure and
version-keyed.

---

## 8. Language support

### 8.1 Parser substrate

Language support is built on **tree-sitter** through its official Kotlin Multiplatform bindings
(`io.github.tree-sitter:ktreesitter`), which publish JVM, Android and Native artifacts and expose
`Language`, `Parser`, `Tree`, `Node`, `TreeCursor` and `InputEdit` (`D-15`).

Why this substrate, rather than a hand-written parser or a JVM-only one:

| Requirement | How tree-sitter meets it | What the alternative costs |
| --- | --- | --- |
| Incremental reparse on every keystroke | `InputEdit` plus subtree reuse is the library's stated purpose | A hand-written parser needs its own incremental machinery, or full reparse per key |
| Robustness on incomplete input | Error recovery produces a usable tree for malformed text | A strict parser throws, and an editor cannot have a "cannot place a caret" state (`I-5`) |
| Multiplatform, no JVM-only core | Official Kotlin bindings target JVM, Android and Native | PSI or JavaParser are JVM-only, which violates `DEP-10` |
| One adapter for many languages | Grammars are data; the adapter is written once | One adapter implementation per language |
| Language additions without engine changes | A grammar binding plus a mapping is a data module (`DEP-4`) | Core changes per language |

Constraints accepted with this choice: no JS/Wasm target from the bindings, a native binary per platform
(`R-7`), and grammar/binding version coupling (`R-8`).

The adapter (`:parser:tree-sitter`) is written **once** in `commonMain` and implements
`LanguageAdapter` for any grammar: it selects named children, skips anonymous tokens that carry no
meaning, maps `ERROR` nodes to the neutral `ERROR` kind, namespaces kinds by grammar (`D-23`) and converts
byte offsets to UTF-16 ranges (§8.2).

### 8.2 Ranges across the byte/char boundary

tree-sitter addresses text in **bytes** (start byte, end byte, row/column point); Kotlin addresses it in
**UTF-16 code units**; `Document` ranges are UTF-16 (§6.1). The boundary is real and is the most likely
place for a silent corruption bug (`R-9`).

Rules:

- The parser is fed **UTF-8 bytes** (`Document.utf8`), because grammars are authored and tested against
  UTF-8 and their lexers assume it; feeding UTF-16 would make identifiers with non-ASCII characters
  grammar-dependent.
- Conversion is O(log n) through an index maintained per document version, never a scan per node.
- `TextRange` is always UTF-16 and always within `document.length` (`I-11`).
- The conformance suite includes text with a two-byte character, a three-byte character and a character
  outside the BMP, and asserts that every projected block range round-trips to the same source slice
  (§8.6).
- An adapter never exposes byte offsets. If a byte offset escapes into `:core:block` or `:ui:*`, the
  design is broken.

### 8.3 Grammar packaging and the language module

A `:lang:*` module contains exactly three things:

1. The grammar binding (`TreeSitterJson.language()`): the grammar's own C sources, never hand-edited, and
   the small Kotlin object that loads the compiled library. The upstream generator targets the classic
   Android plugin and its own resource layout, so the binding is written against the layout of the JNI
   build plugin in `build-logic` instead. The grammar arrives as a git submodule pinned to a release tag,
   and that tag is the version the catalog records for it (`R-8`, §12.1).
2. The `BlockMapping` for that language: handled kinds, categories, layout policy, notation.
3. Conformance and golden tests, using `:core:testkit` (`DEP-8`).

No language module contains projection logic, id assignment, traversal or text mutation. If a language
needs behaviour the SPI cannot express, the SPI is extended in `:core:block` with its own decision record
— that is the signal that the abstraction is wrong, not that the module should bypass it (`P-4`).

### 8.4 The JSON reference language

JSON is the first supported language (`D-16`): it is small, unambiguous, has no comments, no indentation
significance and a strict grammar with excellent error recovery, so it exercises every contract without
the noise of a large language.

Node kinds and their projection:

| Grammar kind | Block | Category | ValueKind | Parts |
| --- | --- | --- | --- | --- |
| `document` | Root | `OPAQUE` | `UNKNOWN` | One multiple `OPAQUE` slot for the top-level value plus surrounding chrome |
| `object` | Object | `EXPRESSION` | `OBJECT` | Chrome `{`, one multiple slot of pairs, chrome `}` |
| `pair` | Pair | `DECLARATION` | `UNKNOWN` | Key field (editable string content), chrome `:`, one single `EXPRESSION` slot for the value |
| `array` | Array | `EXPRESSION` | `ARRAY` | Chrome `[`, one multiple `EXPRESSION` slot, chrome `]` |
| `string` | String literal | `EXPRESSION` | `STRING` | One editable field over the quoted text |
| `number` | Number literal | `EXPRESSION` | `NUMBER` | One editable field |
| `true`, `false` | Boolean literal | `EXPRESSION` | `BOOLEAN` | One editable field |
| `null` | Null literal | `EXPRESSION` | `NULL` | One editable field |
| `ERROR` | Opaque block | `OPAQUE` | `UNKNOWN` | One editable field, flagged as error (§7.6) |

Layout policy for JSON: separator `Custom(leading = "", trailing = ", ")`, which is the comma and the space
the examples below require (`Comma` alone cannot express the space), `trailingSeparator = false`, indent
`None` (JSON layout is the user's; the framework does not reformat), and the `onDelete`/`onInsert` rules
that the examples in §6.5 spell out: removal takes the following separator, or the preceding one when the
element is last, and insertion places a separator only when the element joins an existing one.

Acceptance criteria for the reference language, all asserted by golden tests:

1. `I-13`: pretty-printed, minified and deliberately malformed documents round-trip byte for byte.
2. Every edit in §6.5 produces exactly the text in its example.
3. Deleting every element of an object or array in turn never leaves an invalid document, including the
   last element and the only element.
4. A document that is entirely an error projects as one editable block that, when fixed, projects
   normally without restarting the session.
5. Editing inside a string literal never escapes the quoting.

### 8.5 Adding a language

The checklist a contributor follows, in order. If any step requires touching `:core:*`, stop and open a
design change instead (`§8.3`):

1. Add the grammar binding to a new `:lang:<name>` module (`DEP-4`).
2. Declare `languages`, `handles`, `categories`, `layout` and `notation`.
3. Decide the block decomposition for each handled kind and write it down as a table, as in §8.4.
4. Write the layout policy: separators, trailing separator, indentation, deletion and insertion rules.
5. Add conformance tests from `:core:testkit` (`§8.6`) and golden tests for every intent the language
   supports.
6. Add the language to the host's registry assertion test so `I-20` covers it.

The second language is the real test of the abstraction: a language whose layout differs structurally
from JSON (Kotlin with line-separated statements, or YAML with significant indentation) must be added
without touching `:core:*` (`M-7`).

### 8.6 Conformance suite

`:core:testkit` publishes abstract suites that a consumer's adapter or mapping extends:

| Suite | Asserts |
| --- | --- |
| `AdapterConformance` | Totality on malformed input, range validity, non-overlapping children, containment, UTF-16 conversion including astral characters, determinism, `ERROR` marking |
| `MappingConformance` | Declared kinds are actually produced, categories resolve, layout rules hold, round trip `I-13`, no trivia loss `I-14` |
| `EditCompilerConformance` | Disjointness, descending order, byte identity outside ranges, staleness yielding empty lists `I-4`, determinism `I-15` |

A language is supported when its module passes all three. This is what makes "support a language" a
checkable statement rather than a claim.

---

## 9. Interaction model

A projectional editor is not a renderer plus a parser. Voelter's evidence (`§4.1`) is that editors of this
family fail on interaction inconsistency, not on projection, so the interaction model is a contract, not
a UI detail.

### 9.1 Unidirectional flow

`:ui:*` is a pure function of state plus an emitter of intents.

- Input: a `BlockTree`, a selection, and a focus description.
- Output: `BlockEdit` values, never document mutations (`DEP-5`).
- The renderer holds no document, no parser and no version. It cannot create state that the document does
  not justify.
- Recomposition is driven by a new `BlockTree`; nothing in the UI caches a block across versions.

### 9.2 Focus model

| Focus | Reached by | What it means |
| --- | --- | --- |
| Block | Clicking a block's chrome, arrow keys at a field boundary | The block as a whole: selectable, draggable, deletable |
| Field | Clicking or tabbing into an editable field | Text input into that field, `SetField` on commit |
| Slot | Clicking an empty slot, or tabbing to it | A hole: the next insertion target |

Focus is identified by a `BlockId` plus a field index or slot index, and it is **re-anchored after every
projection** by matching the parse path of §6.4, falling back to the nearest surviving ancestor. Focus
never survives an edit by object identity (`D-19`).

### 9.3 Empty slots and ghost placeholders

An empty slot is rendered as a **ghost**: a placeholder that names what the grammar expects there, using
the mapping's notation (§6.6). This is the single most important affordance of a projectional editor — an
empty slot that renders as nothing is indistinguishable from a bug, and it is where users get stuck.

- A ghost is never text and never a field; it is a rendering of `SlotCategory` plus notation.
- A ghost disappears when the slot has a child and reappears when the child is deleted.
- An empty root document shows one ghost, which is what makes "no content" editable at all (`§7.6`).

### 9.4 Keyboard

| Key | In a field | On a block | On an empty slot |
| --- | --- | --- | --- |
| `Tab` / `Shift+Tab` | Commit and move to the next or previous hole | Focus the first hole | Focus the first field |
| `Enter` | Commit; a layout rule may split the element | Insert a statement after, per `InsertTemplate` | Insert the default template |
| `Backspace` at field start | Delete the preceding element if the field is empty | `Delete` | Nothing |
| `Escape` | Revert the field to its committed text | Clear selection | Clear focus |
| Arrow keys | Move within the field; leave it at a boundary | Move between blocks | Move between holes |

The table is normative: the same key has the same meaning in every language, and a language cannot
override it (that is a variation the plan refuses on purpose, `§4.1`).

### 9.5 Input reconciliation

Typing into a field is free text; typing into a hole is structure. The two are reconciled, not
restricted (`§4.3`, `D-20`):

1. Text typed into a slot is first accepted as text and parsed incrementally.
2. If the fragment parses to exactly one node of a kind the slot accepts, the text is replaced by that
   node's canonical text, and focus moves into it.
3. If it parses to several nodes, the slot accepts them all when `multiple`, otherwise the first is kept
   and the rest become siblings.
4. If it does not parse, it stays an opaque block with the literal text, visibly marked, and re-parses on
   the next keystroke. Nothing is refused and nothing is lost (`I-5`).
5. Autocompletion, where the mapping provides notation, offers the tokens the grammar expects at the
   current position; it never blocks a keystroke.

### 9.6 IME and composition

Composition (CJK, dead keys, dictation) produces text that is temporarily not a word. Because projection
can split a field — the parser may see an identifier as two nodes mid-composition — the rule is:

- While a composition region is active, the projection of the block that contains the caret is
  **suspended**: the field renders the composing text directly and no `BlockEdit` is emitted.
- On composition commit or cancel, one `SetField` is emitted for the final text and projection resumes.
- The composition region is never allowed to span two fields. If it would, the projection is what yields,
  not the text.

### 9.7 Selection and caret across reprojection

Caret and selection are **document offsets**, never block-relative positions, which is the practical
benefit of text being the truth (`P-1`): an offset survives any re-projection, any id change and any
incremental path. Block-level selection is a `TextRange` plus the set of blocks whose ranges it covers.

### 9.8 Drag and drop

- A drag source is a block; the drop target is a slot position.
- While dragging, the target slot shows whether it accepts the dragged block's category (`I-9`), and an
  incompatible target is visibly refused rather than silently ignored.
- A drop emits one `Move`. A drop that the layout policy cannot express as two disjoint edits is refused
  in the UI, never approximated.

### 9.9 Accessibility

- Every block exposes its role and its category to the accessibility tree, and every field exposes its
  role name from the mapping.
- The ghost of an empty slot is exposed as a labelled control, not as decoration.
- Keyboard-only operation is complete: every action in §9.4 has a key, and no action requires a gesture.

---

## 10. Testing and quality

### 10.1 Levels

| Level | Scope | Requirement |
| --- | --- | --- |
| Unit | One rule of §6 or §7 | Asserts the rule's full observable effect |
| Contract | One invariant of §6.10 | One test per invariant that can be violated |
| Conformance | Adapters and mappings | The suites of §8.6, extended by the module |
| Golden | Edit compilation and round trip | Exact resulting text, character for character |
| Differential | Shared helpers and the incremental path | Compared against a reference implementation (§10.5) |
| Property | Round trip and range algebra | Generated documents and edits with fixed seeds |
| UI | Compose surface | Preview-driven visual verification, no golden screenshots |

Rules: behavior changes start with a failing test; a test asserts every observable effect, including that
a rejected input left the document unchanged; tests exercise the public surface, never internal storage;
tests live in `commonTest` unless a platform API is required (§5.7); a flaky, order-dependent or
time-dependent test is a defect.

### 10.2 Golden tests for text

Every rule in §6.7 and §7.3 is asserted as exact text, not as a property of it. A golden test carries the
input document, the intent, and the exact expected document. A rule without a golden test is a rule that
is not enforced.

`I-13` is asserted twice: as a golden test over a corpus of documents (including malformed ones) and as a
property test over generated ones (§10.4).

### 10.3 Property-based tests

Without adding a test framework dependency, property tests use `kotlin.random.Random` with a fixed seed
list recorded in the test, so a failure is reproducible:

- **Round trip** (`I-13`): generated valid and invalid documents project and serialize back to the same
  bytes.
- **Edit algebra** (`I-3`, `I-16`): generated disjoint edits applied in descending order produce the same
  text as applying them in ascending order to a copy with adjusted offsets.
- **Range algebra** (`I-11`): generated ranges satisfy the half-open, non-negative and containment rules.

### 10.4 Differential checks

Two differential checks are mandatory, because both compare an optimization against the definition it
must not change:

- **Incremental versus full** (`I-18`): for a generated edit sequence, the incremental tree equals the full
  tree structurally, including ids and part order.
- **Compiler versus intent**: applying the emitted edits to the document must produce exactly the text the
  intent describes, and the surrounding bytes must be unchanged (`I-3`).

When shared code changes, the touched behavior is also run against the previous implementation, comparing
exit-observable output.

### 10.5 Documentation checks

`PLAN.md` is checked by `tools/plan_policy.py` in CI, because a reference that does not resolve is a
defect and not a style issue (`§1`):

- every `P-`, `DEP-`, `I-`, `R-`, `D-`, `Q-`, `M-` reference resolves to a definition row of this file,
  and no identifier is defined twice;
- every `§n.m` reference resolves to a heading with that number, and a fenced block is neither a
  definition nor a reference;
- every public type named in §6 resolves to a declaration in a production source set of a module, to an
  enum entry, or to a Kotlin standard-library type;
- §5.3, the module list of `AGENTS.md` and the `include` calls of `settings.gradle.kts` name the same
  modules.

### 10.6 Flaky tests

A flaky test is quarantined by deletion or by making it deterministic, never by retrying. Retries hide
the only signal that an incremental path has a race.

---

## 11. Performance budgets

Budgets are contracts, measured on the slowest reference device (a mid-range Android phone), because
desktop performance flatters this design (`D-25`). A budget miss is a defect that blocks the milestone
that introduced it.

| Operation | Budget | Measured by |
| --- | --- | --- |
| Keystroke to re-projected tree, 2,000-line JSON | p50 ≤ 16 ms, p95 ≤ 33 ms | Benchmark in `:core:engine` |
| Incremental parse of a one-character edit, 2,000 lines | ≤ 2 ms | Benchmark in `:parser:tree-sitter` |
| Full projection, 20,000 nodes | ≤ 25 ms | Benchmark in `:core:engine` |
| Incremental projection of a one-character edit | ≤ 4 ms | Differential benchmark (§10.4) |
| Byte/char conversion index build | ≤ 3 ms for 2,000 lines | Benchmark in `:core:language` |
| Scroll with 500 blocks at 60 fps | No frame over 16 ms | Compose benchmark in `:ui:block` |
| Cold start to first projection, 1,000 lines | ≤ 300 ms | Instrumented test in `:androidApp` |
| Block tree memory | ≤ 8× document text | Allocation test |
| Retained versions | At most one parse tree and one block tree per document | Code review plus leak test |

Two rules keep the numbers honest: a budget is measured on the reference device, never on the developer's
machine, and a benchmark that cannot run in CI is recorded with its manual procedure rather than asserted.

---

## 12. Build and tooling

| Concern | Decision |
| --- | --- |
| Build tool | Gradle 9.6.1 through the committed wrapper |
| Kotlin | 2.4.0 |
| Android Gradle Plugin | 9.2.1 |
| Compose Multiplatform | 1.11.1 |
| Parser | `io.github.tree-sitter:ktreesitter`, pinned through the catalog (`D-15`) |
| Catalog | Published `io.github.alexanderrotela20.catalog:version-catalog:1.2.7`, imported as `rootLibs` |
| Convention plugins | Published `io.github.alexanderrotela20.convention:*`, version from the catalog |
| Native build | Local plugin `dev.rotalex.rotablock.jni` in `build-logic` |
| Java target | 17 |
| compileSdk / minSdk / targetSdk | 37 / 26 / 37 |
| JDK | Toolchain 21 for the daemon, JVM target 17 |

### 12.1 Catalog policy

1. Use the published catalog through `rootLibs` first.
2. Add an entry to `gradle/libs.versions.toml` only when the dependency is absent from the published
   catalog, and state the reason next to the entry. The tree-sitter binding and each grammar are such
   entries.
3. Never hardcode a version in a `build.gradle.kts` file.
4. A grammar's version is pinned together with the binding version that generated it (`R-8`).

### 12.2 Build commands

The development environment is Windows with WSL; the toolchain lives on the Windows host.

```bash
powershell.exe .\gradlew.bat build
powershell.exe .\gradlew.bat :core:engine:allTests
powershell.exe .\gradlew.bat :lang:json:allTests
powershell.exe .\gradlew.bat :desktopApp:run
```

`./gradlew`, `gradlew` and a locally installed `gradle` are invalid in WSL and must not be used. CI runs
on Linux and is the only place where `./gradlew` is correct.

### 12.3 Local environment

`local.properties` holds `sdk.dir` and is gitignored. `gradle/gradle-daemon-jvm.properties` pins the daemon
JVM and is committed. No build output, cache, IDE state or generated grammar source is committed unless
the generator cannot run in CI, and then it is committed with its generation command recorded.

### 12.4 Native and packaging

The tree-sitter binding ships native binaries per platform (`R-7`): the Android artifact size is measured
per release, and a language module that adds a grammar adds its own binary. An ABI split or a debug-only
grammar set is a packaging decision recorded here before it is implemented.

A language module that carries C sources applies `dev.rotalex.rotablock.jni`, from the `build-logic`
included build. The plugin compiles the desktop library for the host through CMake and cross-compiles every
Android ABI through the NDK, then packages the result where each target loads it: Android jniLibs through
the variant API, desktop under `jni/<classifier>` resources. It is the only native build path in the
repository, and it exists because the AGP 9 multiplatform library plugin exposes no `externalNativeBuild`
DSL. `build-logic` is build logic, not a product module: it is on no product module's compile classpath.

---

## 13. Repository governance

### 13.1 Branch model

| Branch | Receives | Purpose |
| --- | --- | --- |
| `master` | Pull requests from `dev` only | Release line; every commit here is a release |
| `dev` | Pull requests from work branches | Integration line; the default working base |
| `feat/*`, `fix/*`, `ci/*`, `docs/*`, `refactor/*`, `test/*`, `chore/*`, `perf/*`, `build/*`, `revert/*` | Direct pushes by their author | One unit of work each |

Rules: a work branch always branches from `dev` and targets `dev`; only `dev` may target `master`;
`master` is updated exclusively through a release pull request and then tagged; direct pushes to `dev` and
`master` are forbidden; branch names match `^(feat|fix|ci|docs|refactor|test|chore|perf|build|revert)/[a-z0-9._-]+$`.

### 13.2 Commits

- One line only: `<type>(<scope>): <subject>`, scope optional, imperative, English, no trailing period, at
  most 72 characters.
- No body, no footers, no emoji, no attribution.
- One commit per reviewable work unit, roughly 400 authored changed lines, with tests and documentation in
  the same unit as the behavior they describe.

### 13.3 Pull requests

- Title follows the commit format exactly.
- Body is empty. No description, no template, no checklist (`D-28`).
- One pull request carries one unit of work and one reviewable diff.
- Reviews leave no inline comments; feedback goes into the review summary.

A design change large enough to need prose is expressed as a decision record in §16 or as a change to
§6, not as pull request body text: that keeps the reasoning in the artifact that outlives the pull
request.

### 13.4 Language

Every artifact is English: code, identifiers, KDoc, tests, fixtures, commit messages, pull request titles,
branch names, documentation, CI configuration and file names. Conversation language never changes
artifacts.

### 13.5 Comments

- KDoc (`/** ... */`) is allowed and required on public API declarations, and only there (`I-10`).
- No line comments, no other block comments, no commented-out code, no TODO or FIXME markers, no author
  tags.
- Intent is expressed through names, types and tests.

### 13.6 Plan references

Source code, tests, commit messages, pull request titles, branch names and documentation other than this
file never mention a milestone, this plan, or a phase of it. Work is described by what it does.

### 13.7 Work tracking and memory

- ODD task documents live in Engram, which is authoritative.
- `odd/tasks/<branch-name>.md` is a per-branch mirror created when the branch starts.
- `odd/` and the agent tooling directories (`.atl/`, `.opencode/`, `.gentle-shell/`, `.gentle-ai/`, `.pi/`)
  are gitignored and never committed.
- Durable knowledge goes to Engram as a topic, not into repository files.
- A task table carries an explicit status column, never an implicit one.

### 13.8 CI gates

| Gate | Enforces |
| --- | --- |
| `pr-policy` | Target branch; `master` only from `dev`; branch naming; empty pull request body; title format |
| `commit-policy` | Single-line conventional subjects, at most 72 characters, no body, no plan references |
| `comment-policy` | No line comments and no non-KDoc block comments in Kotlin sources |
| `plan-policy` | The documentation checks of §10.5 |
| `build` | `./gradlew build` on Linux with the Android SDK and a pinned JDK |

A pull request that fails any gate is not reviewable. Branch protection on `master` and `dev` requires
these checks before merging.

### 13.9 Bootstrap exception

A repository cannot be governed before it exists. Exactly one bootstrap commit landed directly on
`master`, containing only repository governance. From that point on, `master` receives only `dev` pull
requests and `dev` receives only work-branch pull requests.

---

## 14. Milestones

Milestones are exit criteria, not calendar. A milestone closes when merged pull requests demonstrate its
criteria. The order changed from the first version of this plan: the engine and its edit compiler are
proven against JSON before any UI work begins, because the UI cannot be designed against contracts whose
text behaviour is unverified.

| Id | Scope | Exit criteria |
| --- | --- | --- |
| `M-1` | Contracts | `:core:language`, `:core:block` and `:core:engine` public surfaces with KDoc, contract tests per invariant of §6.10, and `:core:testkit` suites published |
| `M-2` | Document and adapter | `:core:language` `Document`, `:parser:tree-sitter`, and `AdapterConformance` passing on a real grammar, including the astral-character cases of §8.2 |
| `M-3` | Projection | Full and incremental projection of JSON, `I-18` differential tests green, performance budgets of §11 met for projection |
| `M-4` | Edit compilation | Every variant of §6.5 with the golden examples of §6.5 and §8.4, `I-13` round trip green over the JSON corpus, budgets for keystroke latency met |
| `M-5` | Interaction and UI | `:ui:block` renders the JSON tree with ghosts, focus, keyboard and drag and drop per §9, verified by previews |
| `M-6` | Host | `:shared` document store, reparse loop, registry with `I-20` conflict reporting, undo transactions per §7.5 |
| `M-7` | Second language | A language whose layout differs structurally from JSON added by data only, with no change to `:core:*` (§8.5) |
| `M-8` | Entrypoints | Both applications open, edit and save a JSON document end to end |

---

## 15. Risks

| Id | Risk | Mitigation |
| --- | --- | --- |
| `R-1` | Projection correctness on malformed input | Total parse contract, opaque collapse, golden corpus of broken documents (§7.6) |
| `R-2` | AGP 9 KMP Android library plugin maturity | Versions pinned in the catalog, one module shape reused, upgrades as their own pull request |
| `R-3` | Compose Multiplatform and Kotlin version compatibility | Pinned by the published catalog, upgrades gated by a green build on both targets |
| `R-4` | Review workload | Work units of roughly 400 authored lines (§13.2) |
| `R-5` | Silent regressions in shared helpers | Differential checks (§10.4) |
| `R-6` | Full-pass projection cost on large files | Numeric budgets (§11) and the incremental contract (§7.2) |
| `R-7` | Native binary size from the parser on Android | Per-release size measurement (§12.4), grammar only in language modules |
| `R-8` | Grammar and binding version skew | Grammar and binding pinned together, conformance suite per language (§8.6, §12.1) |
| `R-9` | Byte/char conversion defects corrupting ranges | Single conversion point, O(log n) index, astral-character conformance cases (§8.2) |
| `R-10` | Incremental projection silently diverging from full | `I-18` differential tests, fallback to full projection when affected ranges are unclear (§7.2) |
| `R-11` | Layout policy becoming per-language engine code | Layout is data on the mapping, validated by the engine, never an engine branch (§6.7) |
| `R-12` | IME composition fighting reprojection | Suspension rule of §9.6 |
| `R-13` | Module rename churn hiding real changes | Rename is its own pull request with no behavior change, tests unchanged (§20.2) |
| `R-14` | Interaction inconsistency across languages | Normative keyboard table, shared notation vocabulary, conformance tests (§9.4, §4.1) |

---

## 16. Decision records

| Id | Decision | Rationale | Rejected alternative |
| --- | --- | --- | --- |
| `D-1` | Text is the single source of truth; blocks are a projection | Eliminates dual-model drift, keeps documents editable by any tool, makes every block operation a reviewable text edit | Storing the abstract syntax tree as the document, as MPS does (§4.1) |
| `D-2` | Exactly Android and desktop JVM targets | Both are JVM-backed; one shared core serves both products | Adding Native or Web now, which doubles the toolchain for no consumer |
| `D-3` | Applications are entrypoints only | Product logic stays testable in common code and cannot fork per platform | Logic in each app module |
| `D-4` | Published catalog first, local entry only with a stated reason | One version authority across the Rotalex ecosystem | Per-module versions |
| `D-5` | The core ships no parser (amended by `D-15`) | Keeps `:core:*` language-blind and free of parser dependencies | A bundled parser in core |
| `D-6` | Modules are split by responsibility, not by `api`/`impl` | A name states what a module owns; `api`/`impl` states only how it is consumed (§5.4) | Splitting every concern into a contract and a private implementation module |
| `D-7` | KDoc on public API only | Names, types and tests carry intent; comments rot | Comment-heavy code |
| `D-8` | One bootstrap commit on `master`, then release-only | Governance needs a first commit | Rewriting history to fake a governed start |
| `D-9` | Engram is authoritative for task documents, `odd/` is a mirror | Task state survives sessions without polluting the repository | Task documents committed to the repository |
| `D-10` | Merge commits and single-line subjects | History stays traceable to pull requests | Squash-only, which loses the unit boundary |
| `D-11` | CI is the single automated enforcement point | A rule that is not checked is not a rule | Review-only enforcement |
| `D-12` | Publishing coordinates deferred | No consumer exists yet | Guessing coordinates now |
| `D-13` | Ranges are UTF-16 code units and `Document` is the only text owner | Matches Kotlin strings and every UI surface; keeps parser bytes at the seam (§6.1, §8.2) | Byte-based ranges in the public model, which forces every consumer to convert |
| `D-14` | `Document` is immutable and hides its text structure | Retaining versions must be cheap; a rope or piece table becomes an internal change (§6.1) | Mutable buffer with observers |
| `D-15` | tree-sitter through the official Kotlin Multiplatform bindings | Incremental, robust, multiplatform, one adapter for many languages (§8.1) | Hand-written parsers per language; JVM-only PSI, which violates `I-12` |
| `D-16` | JSON is the first language | Small, strict, comment-free, excellent error recovery; exercises every contract without noise (§8.4) | Starting with Kotlin or Java, whose size hides engine defects |
| `D-17` | `BlockNode` carries `category`; `ValueKind` gains `ARRAY` and `NULL` | Makes `I-9` decidable from the model and stops JSON sockets from lying (§6.3) | Inferring categories from kinds at render time |
| `D-18` | Registries resolve once per version and reject conflicting claims at startup | A packaging mistake must be a startup error, not a mysterious projection (§6.9) | First-registration-wins, silently |
| `D-19` | Ids derive from parse path; nodes are immutable and shared | Keeps focus, selection and incremental reuse alive across edits (§6.4, §7.2) | Traversal counters, which invalidate every id per edit |
| `D-20` | Notation is declarative and input is reconciled, never restricted | Interaction consistency across languages; typing always works (§9.5, §4.3) | Modal, token-restricted input |
| `D-21` | Layout is a per-language policy | One global whitespace rule cannot serve JSON, Kotlin and Python (§4.6) | Engine-wide whitespace rules |
| `D-22` | Comment ownership is fixed by rule | Otherwise edits silently move or drop comments and users lose trust (§6.8) | Leaving trivia to each grammar |
| `D-23` | Node kinds are namespaced by the adapter | Two grammars can produce the same kind name (§6.2) | Global kind vocabulary |
| `D-24` | Nodes are matched by range and kind, never by identity | Parsers rebuild nodes on every edit (§6.2) | Object identity matching |
| `D-25` | Performance budgets are numeric and measured on a mid-range Android device | Budgets without numbers decide nothing, and desktop flatters the design (§11) | "Fast enough" as a judgement |
| `D-26` | Collaboration is out of the core; `Document` is the seam | Synchronization must not shape core contracts (§2.3, §7.5) | A CRDT in the core, as some editors do (§4.4) |
| `D-27` | Modules regroup under `:core`, `:parser`, `:lang`, `:ui`; applications stay top level | The prefix is a checkable statement about the boundary (§5.4) | Flat module list with responsibility encoded in suffixes |
| `D-28` | Pull request bodies stay empty; reasoning lives in this document | Keeps the argument in the artifact that outlives the pull request (§13.3) | Descriptive bodies, which disappear with the review |

---

## 17. Open questions

| Id | Question | Blocks | Current leaning |
| --- | --- | --- | --- |
| `Q-1` | Is a third target (Native or Web) worth a second toolchain? | Nothing yet | No: the parser substrate would allow it, but no consumer needs it (`D-2`) |
| `Q-2` | When does `Document` need a rope or piece table instead of an immutable string? | The budget of §11 for large documents | Only if the full-projection budget fails on text above ~5,000 lines |
| `Q-3` | Should a semantic oracle be shipped for JSON, or only the SPI? | `M-4` | Ship the SPI and one oracle that infers kinds from content, no schema engine |
| `Q-4` | Does the framework own formatting, or delegate to an external formatter? | `M-5` | Delegate: formatting is a separate intent, never automatic |
| `Q-5` | How is a document's language detected when the host supports several? | `M-6` | A host policy over file name and content sniffing; never inside the engine |
| `Q-6` | Should inline rename of a slot's kind be supported, with reparse? | `M-5` | Yes as `ReplaceWithText` plus reconciliation, but after `M-4` |

---

## 18. Definition of done

A change is done when all of the following hold.

1. **Behavior.** It is covered by tests asserting the full observable effect, and every new
   `BlockEdit` variant, mapping rule or layout rule has a golden test (§10.2).
2. **Contracts.** If it adds or changes a public type, the type appears in §6 with its contract, and the
   invariants it touches are listed in §6.10.
3. **Build.** `powershell.exe .\gradlew.bat build` passes locally and every CI gate of §13.8 passes on the
   pull request.
4. **Docs.** Public API carries KDoc and no other comment exists (`I-10`); no artifact mentions this plan
   or a milestone (§13.6); the documentation checks of §10.5 pass.
5. **Process.** The pull request targets `dev` from a correctly named branch, with a single-line title and
   an empty body.
6. **Plan.** This document was updated in the same pull request if a contract, invariant, module
   boundary, build rule or governance rule changed (§1.2).

---

## 19. Glossary

| Term | Meaning |
| --- | --- |
| Document | The text buffer and its version; the only stored representation (§6.1) |
| Neutral DOM | Language-neutral tree with UTF-16 ranges, produced by an adapter (§6.2) |
| Parse tree | The parser's own tree (tree-sitter), in bytes; internal to the adapter (§8.1) |
| Block tree | Projection of the neutral DOM into blocks, fields and slots (§6.3) |
| Chrome | Read-only syntax text around children, rendered but not editable |
| Field | Inline editable token inside a block |
| Slot | Child position inside a block, constrained by a category |
| Ghost | The rendering of an empty slot that names what the grammar expects (§9.3) |
| Carve | Generic projection interleaving chrome with child slots (§7.1) |
| Opaque collapse | Unmapped or unparsable node rendered as a single editable text field (`I-5`) |
| Edit compiler | Component turning a block edit into minimal document edits (§7.3) |
| Layout policy | Per-language rules for separators, indentation and owned syntax (§6.7) |
| Input reconciliation | Matching typed text against the grammar and adopting the parse when unique (§9.5) |
| Conformance suite | Abstract tests every adapter and mapping must pass (§8.6) |
| Walking skeleton | Thinnest end-to-end path that builds and runs on both targets |

---

## 20. Migration of the current repository

### 20.1 Order and ownership

The repository today has `:language-api`, `:block-api`, `:block-impl`, `:block-ui`, `:shared`,
`:androidApp` and `:desktopApp`, with 129 passing tests across the three contract modules. This document
describes the target. The gap is closed by four ordered pull requests, each its own review unit:

| Step | Pull request | Contains | Behavior change |
| --- | --- | --- | --- |
| 1 | `refactor: regroup modules under the core prefix` | Directory moves, `settings.gradle.kts`, module build files, package renames, `AGENTS.md` module list | None; every test unchanged and green (`R-13`) |
| 2 | `feat: add the document and the parser adapter` | `Document` in `:core:language`, `:parser:tree-sitter`, `:core:testkit` `AdapterConformance` | New capability, nothing removed |
| 3 | `feat: complete the block model` | `BlockNode.category`, `ValueKind.ARRAY` and `NULL`, id derivation from parse path (`D-17`, `D-19`) | Model grows; existing mappings must declare categories |
| 4 | `feat: compile edits with a layout policy` | `LayoutPolicy` on the mapping, the compiler of §7.3, `BlockProjectionService` implementation, JSON module (`D-16`, `D-21`) | The engine finally applies edits |

Until step 1 lands, `AGENTS.md` names the current modules and this section names the target: the
divergence is ordered, owned and temporary, which is why it is not a contradiction of §1.2.

### 20.2 Step 1 in detail

1. Create the `:core`, `:parser`, `:lang` and `:ui` container directories and move each module directory
   into place. Gradle has no notion of a parent module unless one is declared, so the parent is expressed
   by directory and by project path only, never by a container project with sources.
2. Update `settings.gradle.kts` to the new paths; no publication or consumer exists, so no compatibility
   shim is needed (`D-12`).
3. Rename packages to the paths of §5.6, and Android namespaces with them.
4. Update `AGENTS.md` in the same pull request, because it is the contract agents read first (§20.4).
5. Do not change any behavior, test or KDoc in this step. The diff is moves and renames; the review reads
   the directory tree, not the code.

### 20.3 Step 3 compatibility note

Adding `category` to `BlockNode` is a constructor change, so the migration updates the 10 block types'
tests and the projection engine in the same pull request, and the projection context gains a
`category(kind)` resolution that reads the mapping's `categories`. A mapping that declares no category
for a handled kind fails conformance rather than guessing (`§8.6`).

### 20.4 Documents that must move with the code

| Document | What changes when |
| --- | --- |
| `PLAN.md` | Authority for every rule; changed in the pull request that changes a rule (§1.2) |
| `AGENTS.md` | Module list, build commands and the session checklist; changed in step 1 (§20.2) |
| `.github/workflows/ci.yml` | Checks out the grammar submodules and installs the NDK version the JNI plugin resolves (§8.3) |
| `.github/workflows/pr-policy.yml` | Adds the `plan-policy` job of §13.8 |
| `gradle/libs.versions.toml` | Adds the tree-sitter binding and each grammar, with the reason recorded (§12.1) |
