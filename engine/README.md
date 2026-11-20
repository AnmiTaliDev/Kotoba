# Kotoba engine

The platform independent part of the Kotoba keyboard: layout format, input rules, emoji data and a keyboard session. It does no file, network or UI work, so it can be used from any platform. The Android app is one client of it.

License: GPL-3.0-only.

## Crates

| Crate | Purpose |
|-------|---------|
| `kotoba-layout` | Layout format v1: model, parser, validation, `extends`, loading from texts, import check, locale aware upper case. |
| `kotoba-input` | Shift state, sentence start detection, double space. No dependencies. |
| `kotoba-emoji` | Emoji panel data format and the recent list. |
| `kotoba-data` | Built-in layouts and emoji data, embedded at build time from `data/`. |
| `kotoba-session` | Layout, layer and shift state; key presses turned into text-editing commands. |
| `kotoba-cli` | `kotoba-cli validate`, `list`, `export`. |
| `kotoba-ffi` | UniFFI bindings (Kotlin and other languages). The only crate with a foreign interface. |
| `kotoba-uniffi-bindgen` | Binding generator used by the Android build. |
| `kotoba-wasm` | WebAssembly build of the layout checker, used by the web page in `web/validator`. |

The first five crates have no `unsafe` code and no platform dependencies. They can be copied out on their own: `kotoba-layout` and `kotoba-input` are independent, `kotoba-session` uses those and `kotoba-emoji`.

The layout format is described in [`../docs/layout-format.md`](../docs/layout-format.md).

## Session protocol

A platform forwards key presses and applies the returned commands. It asks the session what it needs for an event, fills a context, and edits the text field in the order given.

```rust
use kotoba_session::{Context, Event, Session};

let mut session = Session::new(layouts)?;
let event = Event::Space;
let needs = session.needs(&event);              // text before the cursor? selection?
let context = Context { now_ms, text_before, has_selection: false };
let outcome = session.press(event, &context);
// apply outcome.commands to the text field, then:
if outcome.needs_text_after_edit {
    session.after_edit(&text_before_cursor);    // lets the session update shift
}
let state = session.state();                    // layout, layer, shift to draw
```

Offsets and counts are Unicode scalar values (`DeleteBefore`). A platform that counts UTF-16 units must convert.

## Build and test

```
cargo test --workspace
cargo run -p kotoba-cli -- list
```

The Android build runs `cargo ndk` and the binding generator from Gradle (`:app:buildEngineLibraries`, `:app:generateEngineBindings`).

## Web checker

`web/validator` is a static page that checks a layout file with the engine in the browser and previews its keys. Nothing is uploaded.

```
cargo install wasm-bindgen-cli --version 0.2.130
rustup target add wasm32-unknown-unknown
web/validator/build.sh
python3 -m http.server --directory web/validator 8000
```

The `wasm-bindgen-cli` version must match the `wasm-bindgen` crate in `crates/wasm/Cargo.toml`.

## Case mapping

Upper case uses the Unicode mapping with a tailoring for the languages in `kotoba-layout/src/case.rs` (`i` becomes `İ` for Turkish, Azerbaijani and Crimean Tatar). Other locale specific rules are not applied. A layout can set `shifted` explicitly for any key.
