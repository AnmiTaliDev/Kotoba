# Keyboard layout format

A layout is a single JSON file. The current format version is `1`.

A machine-readable description is in [layout.schema.json](layout.schema.json).

## Checking a layout

The engine ships a command line checker that applies the same rules as the app:

```
cargo run --manifest-path engine/Cargo.toml -p kotoba-cli -- validate my-layout.json
```

## Installing a layout

1. Put the `.json` file on the device.
2. Open Kotoba, then Settings, then Keyboard layouts, then Import layout, and pick the file.
3. If the file is valid it appears under Imported. If not, the list of problems is shown and nothing is installed.
4. Press Enable layouts and switch the new layout on in the system keyboard settings.
5. Switch layouts with the globe button of the system navigation bar.

Importing a file whose `id` matches an already imported layout replaces it. An `id` of a built-in layout is refused. Imported layouts can be exported and deleted from the same screen. Built-in layouts can be exported and used as a starting point.

## Minimal example

```json
{
  "formatVersion": 1,
  "id": "example",
  "name": "Example",
  "locale": "en",
  "defaultLayer": "letters",
  "punctuation": { "sentenceEnd": ".!?", "doubleSpace": "." },
  "layers": {
    "letters": {
      "shiftable": true,
      "rows": [
        ["q", "w", "e", "r", "t", "y"],
        [
          { "type": "shift", "weight": 1.5 },
          { "type": "space", "weight": 4 },
          { "type": "delete", "weight": 1.5 }
        ]
      ]
    }
  }
}
```

## Top-level fields

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `formatVersion` | integer | no | Format version, default `1`. A file with a newer version than the app supports is rejected. |
| `id` | string | yes | Unique identifier, matches `[a-z0-9_-]{1,32}`. |
| `name` | string | yes | Display name. Shown on the space key and in system keyboard settings. |
| `locale` | string | yes | BCP 47 language tag, for example `en`, `de`, `kk`. Controls letter case conversion. |
| `extends` | string | no | Id of a built-in layout to inherit from. See [Inheritance](#inheritance). |
| `defaultLayer` | string | yes* | Id of the layer shown first. |
| `order` | integer | no | Position in lists, lower first, default `100`. Layouts with equal `order` are sorted by `name`. |
| `autoCapitalize` | boolean | no | Capitalize the first letter of a sentence, default `true`. Set `false` for scripts without letter case. |
| `punctuation` | object | yes* | Sentence handling, see below. |
| `numberRow` | array | no | One row of keys shown above the letter rows when the user enables the number row. |
| `layers` | object | yes* | Map of layer id to layer. |

\* Required unless inherited through `extends`.

### punctuation

| Field | Type | Description |
|-------|------|-------------|
| `sentenceEnd` | string | Characters that end a sentence. Used for automatic capitalization. |
| `doubleSpace` | string | Text that replaces a double space. Use an empty string to disable. |

## Layers

A layer id matches `[a-z0-9_-]{1,32}`. Layers are switched with `layer` keys.

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `kind` | string | `keys` | `keys` for a key grid, `emoji` for the emoji panel. |
| `shiftable` | boolean | `false` | Whether the shift key affects this layer. |
| `numberRow` | boolean | `false` | Whether the optional number row is shown on this layer. |
| `rows` | array | | Rows of keys. Required for `keys` layers. |
| `back` | object | | Required for `emoji` layers: `{ "label": "ABC", "layer": "letters" }`. |

Rows are listed top to bottom. Keys inside a row are listed in the order they are drawn from left to right. The keyboard is always drawn left to right, including for right-to-left languages, so write such layouts in their physical order.

## Keys

A key is either a string or an object.

A string is a character key. The label is the text typed, the shifted form is the label converted to upper case using `locale`.

An object has a `type` and an optional `weight`. Weight is the relative width, default `1`.

| `type` | Extra fields | Description |
|--------|--------------|-------------|
| `char` (default) | `label`, optional `shifted`, `hold`, `holdShifted` | Types `label`. With shift active types `shifted`, or the upper case of `label` when omitted. See [Long press](#long-press). |
| `shift` | | Shift and caps lock. |
| `delete` | | Backspace. Repeats while held. |
| `enter` | | Enter or the editor action. |
| `space` | | Space bar. |
| `spacer` | | Empty gap. |
| `language` | `label`, optional `icon` | Switches to the next enabled layout. Shown only when more than one layout is enabled on the device. `label` is the description, or the text when the icon is unknown. |
| `layer` | `label`, `layer`, optional `icon` | Switches to layer `layer`. `label` is shown, or used as the description when an icon is present. `icon` names a built-in icon and falls back to the label if unknown. |

Use `shifted` when the upper case differs from the language default, for example `{ "label": "ß", "shifted": "ẞ" }`.

## Long press

A `char` key can offer alternative characters that appear in a popup when the key is held.

```json
{ "label": "e", "hold": ["é", "è", "ê", "ë"] }
```

| Field | Type | Description |
|-------|------|-------------|
| `hold` | array of strings | Alternatives in display order, at most 12, each 1 to 8 characters. |
| `holdShifted` | array of strings | Alternatives used while shift is active. Same number of items as `hold`. When omitted, each item of `hold` is converted to upper case using `locale`. |

Behavior:

- a key with `hold` types its character when released, not when pressed, because the press may turn into a long press. Keys without `hold` type on press;
- holding the key shows the popup with the first alternative selected, sliding the finger selects another one, releasing types it;
- sliding out of the key before the long press time cancels the key.

## Inheritance

`extends` names a built-in layout. The new layout starts as a copy of it and then applies its own fields:

- `layers`: each layer in the file replaces the layer with the same id. Other layers are inherited.
- `punctuation`: keys present in the file replace the inherited keys.
- every other field replaces the inherited one.
- `id`, `name`, `locale` and `order` are never inherited.

Only built-in layouts can be extended, and a layout that itself extends another cannot be a base.

## Limits and validation

A file that breaks a rule is rejected as a whole and the other layouts keep working. Every problem is reported with a path such as `$.layers.letters.rows[2][4].type`.

Errors:

- file larger than 256 KB, or not valid JSON;
- missing or malformed required field, wrong value type;
- `id` not matching the pattern, or already used by another layout;
- `locale` not a valid language tag;
- unknown key `type`, unknown layer kind, `layer` or `defaultLayer` pointing to a missing layer;
- empty row, `keys` layer without rows, `emoji` layer without `back`;
- label longer than 8 characters, weight not greater than 0 or above 10;
- `hold` with more than 12 items, an empty or non-string item, or `holdShifted` of a different size;
- more than 200 keys in total;
- default layer without a `space` key.

Warnings, the layout still loads:

- a row with total weight above 14, keys may become too small;
- default layer without a `delete` key.

Unknown fields are ignored.
