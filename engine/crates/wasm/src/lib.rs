use kotoba_layout::{
    ImportResult, Key, KeyboardLayout, Layer, LayerKind, LayoutIssue, LayoutSource, Severity,
    check_import, load,
};
use serde_json::{Value, json};
use wasm_bindgen::prelude::*;

fn built_in_sources() -> Vec<LayoutSource> {
    kotoba_data::layouts()
        .iter()
        .map(|(name, text)| LayoutSource {
            origin: format!("layouts/{name}"),
            text: (*text).to_string(),
            built_in: true,
        })
        .collect()
}

fn issues_json(issues: &[LayoutIssue]) -> Value {
    issues
        .iter()
        .map(|issue| {
            json!({
                "path": issue.path,
                "message": issue.message,
                "severity": match issue.severity {
                    Severity::Error => "error",
                    Severity::Warning => "warning",
                },
            })
        })
        .collect()
}

fn key_json(key: &Key) -> Value {
    match key {
        Key::Char(key) => json!({
            "type": "char",
            "label": key.label,
            "shifted": key.shifted,
            "weight": key.weight,
            "hold": key.hold,
            "holdShifted": key.hold_shifted,
        }),
        Key::Shift { weight } => json!({"type": "shift", "weight": weight}),
        Key::Delete { weight } => json!({"type": "delete", "weight": weight}),
        Key::Enter { weight } => json!({"type": "enter", "weight": weight}),
        Key::Space { weight } => json!({"type": "space", "weight": weight}),
        Key::Spacer { weight } => json!({"type": "spacer", "weight": weight}),
        Key::Layer(key) => {
            json!({"type": "layer", "label": key.label, "target": key.target, "weight": key.weight})
        }
        Key::Language(key) => json!({"type": "language", "label": key.label, "weight": key.weight}),
    }
}

fn layer_json(layer: &Layer) -> Value {
    json!({
        "id": layer.id,
        "label": layer.label,
        "kind": match layer.kind {
            LayerKind::Keys => "keys",
            LayerKind::Emoji => "emoji",
        },
        "shiftable": layer.shiftable,
        "numberRow": layer.number_row,
        "rows": layer.rows.iter().map(|row| row.iter().map(key_json).collect::<Vec<_>>()).collect::<Vec<_>>(),
        "back": layer.back.as_ref().map(|back| json!({"label": back.label, "target": back.target})),
    })
}

fn layout_json(layout: &KeyboardLayout) -> Value {
    json!({
        "id": layout.id,
        "name": layout.name,
        "locale": layout.locale,
        "order": layout.order,
        "defaultLayer": layout.default_layer,
        "numberRow": layout.number_row.iter().map(key_json).collect::<Vec<_>>(),
        "layers": layout.layers.iter().map(layer_json).collect::<Vec<_>>(),
    })
}

/// Checks a layout text the way the app does when importing it. Returns JSON:
/// `{"ok": bool, "issues": [...], "layout": {...}}` (`layout` only when `ok`).
#[wasm_bindgen]
pub fn check(text: &str) -> String {
    let result = match check_import(text, &built_in_sources(), &[]) {
        ImportResult::Valid { layout, warnings } => {
            json!({"ok": true, "issues": issues_json(&warnings), "layout": layout_json(&layout)})
        }
        ImportResult::Invalid { issues } => json!({"ok": false, "issues": issues_json(&issues)}),
    };
    result.to_string()
}

/// The built-in layouts as JSON `[{"id", "name", "locale", "order"}]`, in display order.
#[wasm_bindgen]
pub fn built_in_layouts() -> String {
    let loaded = load(&built_in_sources());
    loaded
        .layouts
        .iter()
        .map(|layout| json!({"id": layout.id, "name": layout.name, "locale": layout.locale, "order": layout.order}))
        .collect::<Vec<_>>()
        .into_iter()
        .collect::<Value>()
        .to_string()
}

/// The source text of a built-in layout, to start a new layout from.
#[wasm_bindgen]
pub fn built_in_text(id: &str) -> Option<String> {
    load(&built_in_sources())
        .sources
        .get(id)
        .map(|source| source.text.clone())
}
