use kotoba_wasm::{built_in_layouts, built_in_text, check};
use serde_json::Value;

fn parse(text: &str) -> Value {
    serde_json::from_str(text).unwrap()
}

const CHILD: &str = r#"{"id": "mine", "name": "Mine", "locale": "de", "extends": "latin"}"#;

#[test]
fn a_valid_layout_returns_its_preview_model() {
    let result = parse(&check(CHILD));
    assert_eq!(result["ok"], true);
    assert_eq!(result["layout"]["id"], "mine");
    let layers: Vec<&str> = result["layout"]["layers"]
        .as_array()
        .unwrap()
        .iter()
        .map(|layer| layer["id"].as_str().unwrap())
        .collect();
    assert_eq!(layers, ["letters", "emoji", "symbols", "symbols2"]);
}

#[test]
fn preview_keys_carry_resolved_labels() {
    let result = parse(&check(CHILD));
    let letters = &result["layout"]["layers"][0];
    let bottom = letters["rows"]
        .as_array()
        .unwrap()
        .last()
        .unwrap()
        .as_array()
        .unwrap();
    assert_eq!(bottom[0]["type"], "layer");
    assert_eq!(bottom[0]["label"], "?123");
    assert_eq!(bottom[1]["type"], "language");
}

#[test]
fn preview_keys_carry_long_press_alternatives() {
    let text = r#"{"id": "mine", "name": "Mine", "locale": "de", "extends": "latin",
        "layers": {"letters": {"label": "ABC", "rows": [[{"label": "e", "hold": ["é", "è"]}],
        [{"type": "space"}, {"type": "delete"}]]}}}"#;
    let result = parse(&check(text));
    let key = &result["layout"]["layers"][0]["rows"][0][0];
    assert_eq!(key["hold"], serde_json::json!(["é", "è"]));
    assert_eq!(key["holdShifted"], serde_json::json!(["É", "È"]));
}

#[test]
fn an_invalid_layout_returns_issues_with_paths() {
    let result = parse(&check(r#"{"id": "Bad Id"}"#));
    assert_eq!(result["ok"], false);
    assert!(result.get("layout").is_none());
    let paths: Vec<&str> = result["issues"]
        .as_array()
        .unwrap()
        .iter()
        .map(|i| i["path"].as_str().unwrap())
        .collect();
    assert!(paths.contains(&"$.id"));
    assert_eq!(result["issues"][0]["severity"], "error");
}

#[test]
fn malformed_json_is_reported() {
    let result = parse(&check("{oops"));
    assert_eq!(result["ok"], false);
    assert_eq!(result["issues"][0]["path"], "$");
}

#[test]
fn built_in_ids_are_refused() {
    let result = parse(&check(&built_in_text("ru").unwrap()));
    assert_eq!(result["ok"], false);
    assert!(
        result["issues"]
            .as_array()
            .unwrap()
            .iter()
            .any(|i| i["path"] == "$.id")
    );
}

#[test]
fn built_in_layouts_are_listed_in_display_order() {
    let list = parse(&built_in_layouts());
    let ids: Vec<&str> = list
        .as_array()
        .unwrap()
        .iter()
        .map(|l| l["id"].as_str().unwrap())
        .collect();
    assert_eq!(ids[0], "latin");
    assert!(ids.contains(&"ru") && ids.contains(&"kk-latn-2017"));
}

#[test]
fn built_in_text_is_a_loadable_template() {
    let text = built_in_text("tr-q").unwrap();
    let edited = text.replace("\"id\": \"tr-q\"", "\"id\": \"my-tr\"");
    assert_eq!(parse(&check(&edited))["ok"], true);
    assert!(built_in_text("nope").is_none());
}

#[test]
fn warnings_are_returned_with_a_valid_layout() {
    let text = r#"{"id": "mine", "name": "Mine", "locale": "de", "extends": "latin",
        "layers": {"letters": {"label": "ABC", "rows": [[{"type": "space"}]]}}}"#;
    let result = parse(&check(text));
    assert_eq!(result["ok"], true);
    assert_eq!(result["issues"][0]["severity"], "warning");
}
