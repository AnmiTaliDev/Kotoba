#![allow(dead_code)]

use kotoba_layout::{CharKey, Key, LayoutIssue, ParseResult, Severity, parse};
use serde_json::{Value, json};

pub fn minimal() -> Value {
    json!({
        "id": "test",
        "name": "Test",
        "locale": "en",
        "defaultLayer": "letters",
        "punctuation": {"sentenceEnd": ".", "doubleSpace": "."},
        "layers": {
            "letters": {
                "shiftable": true,
                "rows": [
                    ["a", "b"],
                    [{"type": "shift"}, {"type": "space", "weight": 4}, {"type": "delete"}]
                ]
            }
        }
    })
}

pub fn minimal_text() -> String {
    minimal().to_string()
}

pub fn parse_value(value: &Value) -> ParseResult {
    parse(value)
}

pub fn errors(result: &ParseResult) -> Vec<&LayoutIssue> {
    result
        .issues
        .iter()
        .filter(|i| i.severity == Severity::Error)
        .collect()
}

pub fn warnings(result: &ParseResult) -> Vec<&LayoutIssue> {
    result
        .issues
        .iter()
        .filter(|i| i.severity == Severity::Warning)
        .collect()
}

pub fn assert_error(value: &Value, path: &str) {
    let result = parse_value(value);
    assert!(
        result.layout.is_none(),
        "expected a failure at {path}, got a layout"
    );
    assert!(
        errors(&result).iter().any(|i| i.path == path),
        "expected an error at {path}, got {:?}",
        result.issues
    );
}

pub fn letters_rows(value: &mut Value) -> &mut Vec<Value> {
    value["layers"]["letters"]["rows"].as_array_mut().unwrap()
}

pub fn with_first_row(mut value: Value, row: Value) -> Value {
    letters_rows(&mut value)[0] = row;
    value
}

pub fn first_char_key(value: &Value) -> CharKey {
    let result = parse_value(value);
    let layout = result
        .layout
        .unwrap_or_else(|| panic!("layout failed: {:?}", result.issues));
    match &layout.layer("letters").unwrap().rows[0][0] {
        Key::Char(key) => key.clone(),
        other => panic!("not a char key: {other:?}"),
    }
}
