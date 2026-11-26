# Block visual language

How a block is drawn, why it is drawn that way, and the arithmetic behind every measurement.
This document is normative for `:ui:block`: a change to a number here is a change to the surface.

## 1. Where the language comes from

Every mainstream block editor is one lineage:

| Editor | Engine | Notes |
| --- | --- | --- |
| StarLogo TNG, App Inventor Classic | MIT OpenBlocks (2007) | Java applet, geometric customisation |
| Blockly | Google, 2012 | Open source, now the Raspberry Pi Foundation |
| App Inventor 2 | Blockly | Replaced OpenBlocks |
| Scratch 3 | `scratch-blocks`, a Blockly fork | The `zelos` renderer is modelled on Scratch 3 |
| Sketchware, Kodular, Niotron, Code.org, MakeCode | Blockly | Same shapes, different palette |

The shapes are therefore not a style choice: they are a converged, decade-tested convention that
users already know. We adopt it instead of inventing a card layout.

Blockly ships three renderers. `geras` is the original (App Inventor, Sketchware look), `thrasos` is
a modernised `geras`, and `zelos` is the Scratch 3 look. **We follow `zelos`**, for one decisive
reason: it is the only renderer whose every constant is derived from a single unit, so the whole
surface scales by changing one number.

## 2. The lattice

One free parameter: the grid unit. All geometry is an integer multiple of it.

```
GRID = 4dp
```

| Measurement | Formula | Value | Source constant |
| --- | --- | --- | --- |
| Corner radius | `1 * GRID` | 4dp | `CORNER_RADIUS` |
| Notch width | `9 * GRID` | 36dp | `NOTCH_WIDTH` |
| Notch height | `2 * GRID` | 8dp | `NOTCH_HEIGHT` |
| Notch left offset | `3 * GRID` | 12dp | `NOTCH_OFFSET_LEFT` |
| Inner corner radius | `1 * GRID` | 4dp | `INSIDE_CORNERS.width` |
| Mouth indent | `offset + inner corner` | 16dp | `NOTCH_OFFSET_LEFT + INSIDE_CORNERS.width` |
| Minimum block height | `12 * GRID` | 48dp | `MIN_BLOCK_HEIGHT` |
| Minimum block width | `2 * GRID` | 8dp | `MIN_BLOCK_WIDTH` |
| Small padding | `1 * GRID` | 4dp | `SMALL_PADDING` |
| Medium padding | `2 * GRID` | 8dp | `MEDIUM_PADDING` |
| Medium-large padding | `3 * GRID` | 12dp | `MEDIUM_LARGE_PADDING` |
| Large padding | `4 * GRID` | 16dp | `LARGE_PADDING` |
| Field height | `8 * GRID` | 32dp | `FIELD_BORDER_RECT_HEIGHT` |
| Field horizontal padding | `2 * GRID` | 8dp | `FIELD_BORDER_RECT_X_PADDING` |
| Field vertical padding | `1.625 * GRID` | 6.5dp | `FIELD_BORDER_RECT_Y_PADDING` |
| Text size | `3 * GRID` | 12pt | `FIELD_TEXT_FONTSIZE` |
| Empty mouth height | `6 * GRID` | 24dp | `EMPTY_STATEMENT_INPUT_HEIGHT` |
| Bottom bar after a mouth, minimum | `6 * GRID` | 24dp | `BOTTOM_ROW_AFTER_STATEMENT_MIN_HEIGHT` |
| Gap between consecutive mouths | `1 * GRID` | 4dp | `BETWEEN_STATEMENT_PADDING_Y` |

Two of those numbers are not arbitrary at `GRID = 4`:

- `MIN_BLOCK_HEIGHT = 48dp` is exactly the minimum touch target of every platform guideline, so the
  smallest block is still a legal hit target.
- `FIELD_BORDER_RECT_HEIGHT = 32dp` is the dense-control height, so an inline value still reads as a
  control rather than as text.

The lattice has a second property worth stating: any two blocks measured on it line up to the
pixel, so a stack never accumulates rounding drift, and a density change (1x, 1.5x, 2x, 3x) is a
scalar, not a redesign.

## 3. The silhouette

A block is a closed path, drawn anticlockwise from its top-left corner:

1. **Top edge.** A block that nothing can sit above (the document's root) gets a **hat**: a cubic
   Bézier from `(0, 0)` to `(w, 0)` with control points at `(0.26w, -22)` and `(0.74w, -22)`. The
   visible height of such a curve is `3/4 * 22 = 16.5dp`, because the extreme of a cubic Bézier with
   both control points at height `h` reaches `0.75h`. Any other block gets a **notch**: a concave
   cut, 36dp wide starting 12dp from the left edge, 8dp deep.
2. **Right edge** down to the bottom of the block's opening bar, then, if it has a mouth, left to
   the mouth indent and down the mouth's inner edge.
3. **Bottom edge.** With a mouth, the block turns right at the mouth's bottom into a **closing bar**
   of its own minimum height; without one, the bottom edge carries the **tab** — the convex twin of
   the notch — when another block follows.
4. **Left edge** back up. If the block has a mouth, the left edge is a spine `16dp` wide running the
   whole height of the mouth: that spine is what makes the shape a C.

The notch and the tab are the same cut seen from both sides, so consecutive blocks abut with **zero
gap** and their silhouettes fuse into one continuous piece. That is the whole point: adjacency is
drawn by the shape, not by whitespace, so a stack reads as one construct while each member is still
a separate block. Blocks in a mouth are indented by exactly `16dp`, which is the notch offset plus
the inner corner: the child's notch therefore lands inside the parent's mouth, not under its spine.

The notch path itself, with `inner = NOTCH_WIDTH / 3` and `curve = inner / 3`, is three short
segments joined by quarter curves of height `NOTCH_HEIGHT / 4`; at `GRID = 4` that is a 12dp drop
over 4dp, twice. The exact Bézier control points are in the source and need not be guessed: they are
`(curve/2, 0)`, `(3curve/4, quarter/2)`, `(curve, quarter)` for the entry and the mirror of that for
the exit.

## 4. What we keep and what we drop

The family encodes two different relationships. Blockly uses the notch to mean *sequence* (previous
and next statement) and the C to mean *containment* (a statement list). RotaBlock projects text, and
text has containment but no statement list, so:

| Element | Decision | Reason |
| --- | --- | --- |
| Lattice | Keep | Published, verified, one scale parameter. |
| C-shape for a container | Keep | A `BlockSlot` is containment; the mouth is its exact visual form. |
| Inner corner and 16dp indent | Keep | Derived from the same unit; aligns child notches. |
| Hat on the root | Keep | The root genuinely has nothing above it. |
| Notch and tab on stacked members | Keep | Members of a container are a sequence; the tab is also the future drop affordance of the drag contract. |
| Zero gap in a stack | Keep | The notch is the separator; whitespace would read as a gap in the structure. |
| Hexagonal reporters for booleans | Keep | The family gives every value a *socket shape*, not only a colour: hexagon for a boolean, round for any other value. A JSON `true` is a boolean like `"a"` is a string, and the projected value keeps its shape so a reader never has to parse the word. |
| Rounded (round-ended) shape for non-boolean values | Defer | It needs an inline socket to plug into; until a bar can hold a nested value expression, the chip is what marks a value. See §9. |
| Coloured per-category random hue | Drop | Categories are few and semantic; see §6. |
| Notch on a leaf pair | Keep | It is the only thing that says "another member may follow". |

Deliberate consequences, stated so they are not mistaken for bugs: the surface is wider than a card
list, a mouth shows the workspace background between its children, and a container narrower than its
content does not exist because a block's width is the width of its widest row including the mouth.

## 5. Values

A value is a **chip**: a rounded rectangle of height `8 * GRID` with `2 * GRID` horizontal padding,
seated on its owner's line. The chip's fill encodes the value kind and its text is the value; a
value that is itself a container is not a chip, it is a nested block.

A member whose only content is one value is drawn on **one line**: its key, its separator and its
value chip share a bar, because a two-line block for `"path": "/greeting"` would double the height of
every document without adding information.

A boolean is still a chip, but its bar ends in a point on both sides: the end cut is `2 * GRID`
(`notchHeight`), so the shape is derived from the same unit as every other measurement and the bar
grows by one cut per side. The chip never touches the point because the bar's content padding is
`4 * GRID` against a `2 * GRID` cut.

A container value hoists: `"body": {` is a single opening bar carrying the member's key and the
container's brace, followed by the container's mouth and its own closing bar. Chained hoists
(`"a": { "b": {`) keep hoisting until a bar holds two children.

## 6. Colour

Blockly derives three tones from one colour: `primary` (fill), `secondary` (15% darker, the raised
edge) and `tertiary` (25% darker, the outline). We use the same rule for the same reason: a block
outlined in its own darkened colour reads as a solid object at any size, and the outline also
separates two blocks of the same category.

```
fill    = primary
stroke  = blend(black, primary, 0.25)   // tertiary, 1dp
text    = white when the fill is dark enough, otherwise the same tertiary
```

Category colours are chosen for mutual discriminability, not decoration, and every category has
exactly one. A nested block does **not** get a darker fill: category identity outranks depth, and the
mouth already shows the background between levels.

## 7. Ghosts

An empty slot is a dashed outline at chip height, in the slot's category colour at reduced opacity,
carrying the label the slot accepts. It is never text and never a field. When the slot has a child
the ghost is not drawn at all, so the block simply grows.

A ghost whose slot accepts a boolean is drawn as the same hexagon the value itself would take, so the
hollow shape and the filled shape are the same shape. That is the whole point of a socket shape: the
reader recognises what fits before reading the label.

## 8. Acceptance

A render is accepted when all of the following hold, checked on the preview rather than in source:

1. Two consecutive members abut with no visible gap, and the notch of the second wraps the tab of
   the first.
2. A mouth's interior shows the workspace background, and its closing bar sits below the last child
   with `24dp` of height, never overlapping a child.
3. A block's width equals its widest row, and no chip is clipped by an outline.
4. Every measurement is an integer multiple of `4dp`; nothing is eyeballed.
5. A category is identifiable by fill alone, with the outline adding separation, not information.

## 9. Open gap: horizontal composition

The surface composes **vertically**: a container stacks its children, and a value joins its owner's
bar only when it is a single leaf. That is enough for JSON and not enough for a programming language,
where an expression is a value block plugged into a socket inside another block's bar
(`a + b`, `not x`, `f(g(y))`).

Closing it needs a different measurement rule, not a different palette: a bar holds a sequence of
tokens *and* nested value blocks, so a bar's width becomes the sum of its own tokens plus the measured
widths of the values it holds, and the notch/tab vocabulary gains the round and hexagonal plug ends.
Until that exists, the round-ended value shape is deliberately not drawn: a shape that cannot plug
into anything would promise an affordance the surface does not have.

## 10. Sources

- Blockly `ConstantProvider`, fetched from `packages/blockly/core/renderers/common/constants.ts`:
  `NOTCH_WIDTH 15`, `NOTCH_HEIGHT 4`, `CORNER_RADIUS 8`, `MIN_BLOCK_HEIGHT 24`,
  `STATEMENT_INPUT_PADDING_LEFT 20`, `FIELD_BORDER_RECT_HEIGHT 16`.
- Blockly block contract, `packages/blockly/core/block.ts`: `setOutputShape` and `getOutputShape`
  make the output shape a per-block property, serialised with the block. The repository moved to
  `RaspberryPiFoundation/blockly`; the enum value that maps a boolean type to the hexagon was not read
  in this pass, so the boolean rule is recorded as family convention rather than as a quoted constant.
- Blockly `zelos` `ConstantProvider`, `packages/blockly/core/renderers/zelos/constants.ts`:
  `GRID_UNIT 4` with `CORNER_RADIUS = 1 * GRID`, `NOTCH_WIDTH = 9 * GRID`,
  `NOTCH_HEIGHT = 2 * GRID`, `NOTCH_OFFSET_LEFT = 3 * GRID`, `MIN_BLOCK_HEIGHT = 12 * GRID`,
  `FIELD_BORDER_RECT_HEIGHT = 8 * GRID`, `START_HAT_HEIGHT 22` with the `3/4` visible-height note.
- Blockly renderer overview and constant-provider guide: `geras`, `thrasos` and `zelos` are the
  three built-in renderers, and `zelos` is the Scratch 3 design.
- Blockly block-colour guide: additional tones are derived from the main colour, and border colours
  are a darker version of it.
- Scratch Wiki, *Blocks*: hat, stack, boolean, reporter, C and cap shapes, and what each means.
- Scratch Addons, *Customizable block shape*: presets confirm Scratch 3 padding, corner and notch as
  the 100% baseline, with Scratch 2 as `70%` padding and `75%` notch.
- PenguinMod, *Custom notch shape*: the notch is 36 wide by 8 high, and may not pass the centre line.
- Wikipedia, *MIT App Inventor*: App Inventor 2 replaced OpenBlocks with Blockly.
- Bau et al., *Learnable Programming: Blocks and Beyond*: OpenBlocks, Blockly and Droplet as the
  three blocks toolkits, with Blockly behind App Inventor, Code.org and Scratch's future.
