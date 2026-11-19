mod common;

use common::*;
use kotoba_layout::{Key, LayoutSource, MAX_FILE_BYTES, load};
use serde_json::json;

fn built_in(origin: &str, text: &str) -> LayoutSource {
    LayoutSource {
        origin: origin.into(),
        text: text.into(),
        built_in: true,
    }
}

fn user(origin: &str, text: &str) -> LayoutSource {
    LayoutSource {
        origin: origin.into(),
        text: text.into(),
        built_in: false,
    }
}

fn child(extra: serde_json::Value) -> String {
    let mut value = json!({"id": "child", "name": "Child", "locale": "de", "extends": "test"});
    for (key, item) in extra.as_object().unwrap() {
        value[key] = item.clone();
    }
    value.to_string()
}

#[test]
fn loads_valid_sources() {
    let loaded = load(&[built_in("a.json", &minimal_text())]);
    assert_eq!(loaded.layouts.len(), 1);
    assert!(loaded.failures.is_empty());
}

#[test]
fn malformed_json_does_not_stop_others() {
    let loaded = load(&[built_in("a.json", &minimal_text()), user("b.json", "{oops")]);
    let ids: Vec<&str> = loaded.layouts.iter().map(|l| l.id.as_str()).collect();
    assert_eq!(ids, ["test"]);
    assert_eq!(loaded.failures.len(), 1);
    assert_eq!(loaded.failures[0].origin, "b.json");
}

#[test]
fn non_object_documents_fail() {
    let loaded = load(&[user("a.json", "[1]")]);
    assert_eq!(loaded.failures.len(), 1);
}

#[test]
fn rejects_oversized_file() {
    let text = format!("{}{}", minimal_text(), " ".repeat(MAX_FILE_BYTES));
    let loaded = load(&[user("big.json", &text)]);
    assert!(loaded.layouts.is_empty());
    assert_eq!(loaded.failures.len(), 1);
}

#[test]
fn extends_inherits_layers_and_punctuation() {
    let loaded = load(&[
        built_in("base.json", &minimal_text()),
        user("child.json", &child(json!({}))),
    ]);
    let layout = loaded.layouts.iter().find(|l| l.id == "child").unwrap();
    assert_eq!(layout.name, "Child");
    assert_eq!(layout.locale, "de");
    assert_eq!(layout.sentence_end, ".");
    assert!(layout.layer("letters").is_some());
}

#[test]
fn extends_overrides_layer_and_punctuation_keys() {
    let extra = json!({
        "punctuation": {"sentenceEnd": "!"},
        "layers": {"letters": {"shiftable": false, "rows": [["z"], [{"type": "space"}, {"type": "delete"}]]}}
    });
    let loaded = load(&[
        built_in("base.json", &minimal_text()),
        user("child.json", &child(extra)),
    ]);
    let layout = loaded.layouts.iter().find(|l| l.id == "child").unwrap();
    assert_eq!(layout.sentence_end, "!");
    assert_eq!(layout.double_space, ".");
    let Key::Char(key) = &layout.layer("letters").unwrap().rows[0][0] else {
        panic!()
    };
    assert_eq!(key.label, "z");
}

#[test]
fn extends_keeps_inherited_layer_positions() {
    let mut base = minimal();
    base["layers"]["symbols"] = base["layers"]["letters"].clone();
    let extra = json!({"layers": {"letters": {"rows": [["z"], [{"type": "space"}]]}, "extra": {"rows": [["q"]]}}});
    let loaded = load(&[
        built_in("base.json", &base.to_string()),
        user("child.json", &child(extra)),
    ]);
    let layout = loaded.layouts.iter().find(|l| l.id == "child").unwrap();
    let ids: Vec<&str> = layout.layers.iter().map(|l| l.id.as_str()).collect();
    assert_eq!(ids, ["letters", "symbols", "extra"]);
}

#[test]
fn unknown_base_fails() {
    let loaded = load(&[user("child.json", &child(json!({})))]);
    assert_eq!(loaded.failures.len(), 1);
    assert_eq!(loaded.failures[0].issues[0].path, "$.extends");
}

#[test]
fn user_layout_cannot_be_a_base() {
    let user_base = minimal_text().replace("\"test\"", "\"ubase\"");
    let text = child(json!({})).replace("\"test\"", "\"ubase\"");
    let loaded = load(&[user("a.json", &user_base), user("b.json", &text)]);
    let ids: Vec<&str> = loaded.layouts.iter().map(|l| l.id.as_str()).collect();
    assert_eq!(ids, ["ubase"]);
    assert_eq!(loaded.failures.len(), 1);
}

#[test]
fn extending_an_extended_layout_fails() {
    let second = child(json!({}))
        .replace("\"child\"", "\"grandchild\"")
        .replace("\"test\"", "\"child\"");
    let loaded = load(&[
        built_in("a.json", &minimal_text()),
        built_in("b.json", &child(json!({}))),
        built_in("c.json", &second),
    ]);
    let mut ids: Vec<&str> = loaded.layouts.iter().map(|l| l.id.as_str()).collect();
    ids.sort_unstable();
    assert_eq!(ids, ["child", "test"]);
    assert_eq!(loaded.failures.len(), 1);
}

#[test]
fn duplicate_id_is_rejected() {
    let loaded = load(&[
        built_in("a.json", &minimal_text()),
        user("b.json", &minimal_text()),
    ]);
    assert_eq!(loaded.layouts.len(), 1);
    assert_eq!(loaded.failures[0].origin, "b.json");
}

#[test]
fn built_in_layouts_load_before_user_layouts() {
    let user_text = minimal_text().replace("\"test\"", "\"zuser\"");
    let loaded = load(&[
        user("a.json", &user_text),
        built_in("z.json", &minimal_text()),
    ]);
    let ids: Vec<&str> = loaded.layouts.iter().map(|l| l.id.as_str()).collect();
    assert_eq!(ids, ["test", "zuser"]);
}

#[test]
fn invalid_extends_type_is_reported() {
    let mut value: serde_json::Value = serde_json::from_str(&child(json!({}))).unwrap();
    value["extends"] = json!(5);
    let loaded = load(&[
        built_in("a.json", &minimal_text()),
        user("b.json", &value.to_string()),
    ]);
    assert_eq!(loaded.failures[0].issues[0].path, "$.extends");
}

#[test]
fn order_is_not_inherited() {
    let mut base = minimal();
    base["order"] = json!(0);
    let loaded = load(&[
        built_in("base.json", &base.to_string()),
        user("child.json", &child(json!({}))),
    ]);
    let layout = loaded.layouts.iter().find(|l| l.id == "child").unwrap();
    assert_eq!(layout.order, 100);
}

#[test]
fn layouts_are_sorted_by_order_then_name() {
    let a = minimal()
        .to_string()
        .replace("\"test\"", "\"a\"")
        .replace("\"Test\"", "\"Beta\"");
    let b = minimal()
        .to_string()
        .replace("\"test\"", "\"b\"")
        .replace("\"Test\"", "\"Alpha\"");
    let mut first = minimal();
    first["id"] = json!("c");
    first["name"] = json!("Zed");
    first["order"] = json!(1);
    let loaded = load(&[
        built_in("a", &a),
        built_in("b", &b),
        built_in("c", &first.to_string()),
    ]);
    let ids: Vec<&str> = loaded.layouts.iter().map(|l| l.id.as_str()).collect();
    assert_eq!(ids, ["c", "b", "a"]);
}

#[test]
fn warnings_are_reported_for_loaded_layouts() {
    let mut value = minimal();
    value["layers"]["letters"]["rows"] = json!([[{"type": "space"}]]);
    let loaded = load(&[built_in("a.json", &value.to_string())]);
    assert_eq!(loaded.layouts.len(), 1);
    assert_eq!(loaded.warnings.len(), 1);
}

#[test]
fn loaded_layouts_remember_their_source() {
    let loaded = load(&[built_in("a.json", &minimal_text())]);
    assert_eq!(loaded.sources["test"].origin, "a.json");
    assert!(loaded.sources["test"].built_in);
}
