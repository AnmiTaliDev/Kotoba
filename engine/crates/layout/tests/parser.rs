mod common;

use common::*;
use kotoba_layout::{CharKey, Key, MAX_HOLD, MAX_KEYS};
use serde_json::{Value, json};

#[test]
fn parses_minimal_layout() {
    let result = parse_value(&minimal());
    assert!(errors(&result).is_empty(), "{:?}", result.issues);
    let layout = result.layout.unwrap();
    assert_eq!(layout.id, "test");
    assert!(layout.auto_capitalize);
    assert_eq!(layout.order, 100);
    let Key::Char(CharKey { shifted, .. }) = &layout.layer("letters").unwrap().rows[0][0] else {
        panic!()
    };
    assert_eq!(shifted, "A");
}

#[test]
fn uppercase_follows_locale() {
    let mut value = minimal();
    value["locale"] = json!("tr");
    let value = with_first_row(value, json!(["i", "ı"]));
    let layout = parse_value(&value).layout.unwrap();
    let shifted: Vec<&str> = layout.layer("letters").unwrap().rows[0]
        .iter()
        .map(|key| match key {
            Key::Char(key) => key.shifted.as_str(),
            _ => "",
        })
        .collect();
    assert_eq!(shifted, ["İ", "I"]);
}

#[test]
fn explicit_shifted_overrides_uppercase() {
    let key = first_char_key(&with_first_row(
        minimal(),
        json!([{"label": "ß", "shifted": "ẞ"}]),
    ));
    assert_eq!(key.shifted, "ẞ");
}

#[test]
fn auto_capitalize_can_be_disabled() {
    let mut value = minimal();
    value["autoCapitalize"] = json!(false);
    assert!(!parse_value(&value).layout.unwrap().auto_capitalize);
}

#[test]
fn order_is_read() {
    let mut value = minimal();
    value["order"] = json!(7);
    assert_eq!(parse_value(&value).layout.unwrap().order, 7);
}

#[test]
fn rejects_missing_id() {
    let mut value = minimal();
    value.as_object_mut().unwrap().remove("id");
    assert_error(&value, "$.id");
}

#[test]
fn rejects_bad_id() {
    let mut value = minimal();
    value["id"] = json!("Bad Id");
    assert_error(&value, "$.id");
}

#[test]
fn rejects_bad_locale() {
    let mut value = minimal();
    value["locale"] = json!("not a locale");
    assert_error(&value, "$.locale");
}

#[test]
fn accepts_locales_with_a_script() {
    let mut value = minimal();
    value["locale"] = json!("kk-Latn");
    assert_eq!(parse_value(&value).layout.unwrap().locale, "kk-Latn");
}

#[test]
fn rejects_newer_format_version() {
    let mut value = minimal();
    value["formatVersion"] = json!(99);
    assert_error(&value, "$.formatVersion");
}

#[test]
fn rejects_non_integer_format_version() {
    let mut value = minimal();
    value["formatVersion"] = json!(1.5);
    assert_error(&value, "$.formatVersion");
}

#[test]
fn rejects_unknown_key_type() {
    let value = with_first_row(minimal(), json!([{"type": "teleport"}]));
    assert_error(&value, "$.layers.letters.rows[0][0].type");
}

#[test]
fn rejects_unknown_layer_target() {
    let value = with_first_row(
        minimal(),
        json!([{"type": "layer", "label": "x", "layer": "nope"}]),
    );
    assert_error(&value, "$.layers.letters.rows[0][0].layer");
}

#[test]
fn rejects_unknown_default_layer() {
    let mut value = minimal();
    value["defaultLayer"] = json!("nope");
    assert_error(&value, "$.defaultLayer");
}

#[test]
fn rejects_default_layer_without_space() {
    let mut value = minimal();
    value["layers"]["letters"]["rows"] = json!([["a"]]);
    assert_error(&value, "$.layers.letters");
}

#[test]
fn warns_about_missing_delete() {
    let mut value = minimal();
    value["layers"]["letters"]["rows"] = json!([[{"type": "space"}]]);
    let result = parse_value(&value);
    assert!(result.layout.is_some());
    assert!(
        warnings(&result)
            .iter()
            .any(|i| i.path == "$.layers.letters")
    );
}

#[test]
fn rejects_empty_row() {
    let mut value = minimal();
    letters_rows(&mut value).push(json!([]));
    assert_error(&value, "$.layers.letters.rows[2]");
}

#[test]
fn rejects_long_label() {
    assert_error(
        &with_first_row(minimal(), json!(["123456789"])),
        "$.layers.letters.rows[0][0]",
    );
}

#[test]
fn rejects_non_positive_weight() {
    let value = with_first_row(minimal(), json!([{"label": "a", "weight": 0}]));
    assert_error(&value, "$.layers.letters.rows[0][0].weight");
}

#[test]
fn warns_about_wide_row() {
    let mut value = minimal();
    letters_rows(&mut value).push(json!(vec!["a"; 15]));
    let result = parse_value(&value);
    assert!(result.layout.is_some());
    assert!(
        warnings(&result)
            .iter()
            .any(|i| i.path == "$.layers.letters.rows[2]")
    );
}

#[test]
fn rejects_too_many_keys() {
    let mut value = minimal();
    for _ in 0..(MAX_KEYS / 10 + 1) {
        letters_rows(&mut value).push(json!(vec!["a"; 10]));
    }
    assert_error(&value, "$");
}

#[test]
fn rejects_emoji_layer_without_back() {
    let mut value = minimal();
    value["layers"]["emoji"] = json!({"kind": "emoji"});
    assert_error(&value, "$.layers.emoji.back");
}

#[test]
fn rejects_wrong_types() {
    let mut value = minimal();
    value["name"] = json!(5);
    assert_error(&value, "$.name");
    let mut value = minimal();
    value["punctuation"] = json!("x");
    assert_error(&value, "$.punctuation");
}

#[test]
fn rejects_non_object_documents() {
    let result = parse_value(&json!([1, 2]));
    assert!(result.layout.is_none());
    assert_eq!(result.issues[0].path, "$");
}

#[test]
fn collects_several_issues_at_once() {
    let mut value = minimal();
    value["id"] = json!("Bad");
    value["locale"] = json!("???");
    let result = parse_value(&value);
    let paths: Vec<&str> = errors(&result).iter().map(|i| i.path.as_str()).collect();
    assert!(
        paths.contains(&"$.id") && paths.contains(&"$.locale"),
        "{paths:?}"
    );
}

#[test]
fn null_counts_as_absent() {
    let mut value = minimal();
    value["autoCapitalize"] = Value::Null;
    assert!(parse_value(&value).layout.unwrap().auto_capitalize);
}

fn hold_key(extra: Value) -> Value {
    let mut key = json!({"label": "e"});
    for (name, item) in extra.as_object().unwrap() {
        key[name] = item.clone();
    }
    with_first_row(minimal(), json!([key]))
}

#[test]
fn reads_hold_alternatives() {
    let key = first_char_key(&hold_key(json!({"hold": ["é", "è"]})));
    assert_eq!(key.hold, ["é", "è"]);
    assert_eq!(key.hold_shifted, ["É", "È"]);
}

#[test]
fn explicit_hold_shifted_overrides_uppercase() {
    let key = first_char_key(&with_first_row(
        minimal(),
        json!([{"label": "s", "hold": ["ß"], "holdShifted": ["ẞ"]}]),
    ));
    assert_eq!(key.hold_shifted, ["ẞ"]);
}

#[test]
fn hold_defaults_to_empty() {
    assert!(first_char_key(&hold_key(json!({}))).hold.is_empty());
}

#[test]
fn rejects_hold_shifted_with_different_size() {
    let value = hold_key(json!({"hold": ["é", "è"], "holdShifted": ["É"]}));
    assert_error(&value, "$.layers.letters.rows[0][0].holdShifted");
}

#[test]
fn rejects_too_many_alternatives() {
    let value = hold_key(json!({"hold": vec!["a"; MAX_HOLD + 1]}));
    assert_error(&value, "$.layers.letters.rows[0][0].hold");
}

#[test]
fn rejects_bad_alternative_items() {
    assert_error(
        &hold_key(json!({"hold": [""]})),
        "$.layers.letters.rows[0][0].hold[0]",
    );
    assert_error(
        &hold_key(json!({"hold": [5]})),
        "$.layers.letters.rows[0][0].hold[0]",
    );
}

#[test]
fn rejects_non_array_hold() {
    assert_error(
        &hold_key(json!({"hold": "é"})),
        "$.layers.letters.rows[0][0].hold",
    );
}

#[test]
fn layers_keep_file_order() {
    let mut value = minimal();
    value["layers"]["symbols"] = value["layers"]["letters"].clone();
    value["layers"]["alt"] = value["layers"]["letters"].clone();
    let layout = parse_value(&value).layout.unwrap();
    let ids: Vec<&str> = layout.layers.iter().map(|l| l.id.as_str()).collect();
    assert_eq!(ids, ["letters", "symbols", "alt"]);
}

#[test]
fn counts_labels_in_characters_not_bytes() {
    assert!(
        parse_value(&with_first_row(minimal(), json!(["ыыыыыыыы"])))
            .layout
            .is_some()
    );
    assert_error(
        &with_first_row(minimal(), json!(["ыыыыыыыыы"])),
        "$.layers.letters.rows[0][0]",
    );
}

#[test]
fn reads_language_key() {
    let value = with_first_row(
        minimal(),
        json!([{"type": "language", "label": "Language", "icon": "ic_key_language", "weight": 1.5}]),
    );
    let layout = parse_value(&value).layout.unwrap();
    let Key::Language(key) = &layout.layer("letters").unwrap().rows[0][0] else {
        panic!()
    };
    assert_eq!(key.label, "Language");
    assert_eq!(key.icon.as_deref(), Some("ic_key_language"));
    assert_eq!(key.weight, 1.5);
}

#[test]
fn language_key_icon_is_optional() {
    let value = with_first_row(
        minimal(),
        json!([{"type": "language", "label": "Language"}]),
    );
    let layout = parse_value(&value).layout.unwrap();
    let Key::Language(key) = &layout.layer("letters").unwrap().rows[0][0] else {
        panic!()
    };
    assert_eq!(key.icon, None);
}

#[test]
fn language_key_needs_a_label() {
    assert_error(
        &with_first_row(minimal(), json!([{"type": "language"}])),
        "$.layers.letters.rows[0][0].label",
    );
}
