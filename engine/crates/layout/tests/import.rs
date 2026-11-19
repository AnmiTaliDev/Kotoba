mod common;

use common::*;
use kotoba_layout::{ImportResult, LayoutSource, check_import, user_origin};
use serde_json::json;

fn built_in() -> Vec<LayoutSource> {
    vec![LayoutSource {
        origin: "layouts/base.json".into(),
        text: minimal_text(),
        built_in: true,
    }]
}

fn candidate(id: &str, name: &str) -> String {
    minimal_text()
        .replace("\"test\"", &format!("\"{id}\""))
        .replace("\"Test\"", &format!("\"{name}\""))
}

fn valid(result: ImportResult) -> kotoba_layout::KeyboardLayout {
    match result {
        ImportResult::Valid { layout, .. } => layout,
        ImportResult::Invalid { issues } => panic!("expected valid, got {issues:?}"),
    }
}

fn invalid(result: ImportResult) -> Vec<kotoba_layout::LayoutIssue> {
    match result {
        ImportResult::Invalid { issues } => issues,
        ImportResult::Valid { .. } => panic!("expected invalid"),
    }
}

#[test]
fn accepts_valid_layout() {
    assert_eq!(
        valid(check_import(&candidate("mine", "Mine"), &built_in(), &[])).id,
        "mine"
    );
}

#[test]
fn rejects_invalid_layout_with_issues() {
    assert!(!invalid(check_import("{\"id\": \"x\"}", &built_in(), &[])).is_empty());
}

#[test]
fn rejects_malformed_json() {
    assert_eq!(
        invalid(check_import("not json", &built_in(), &[]))[0].path,
        "$"
    );
}

#[test]
fn rejects_id_of_built_in_layout() {
    let issues = invalid(check_import(&candidate("test", "Mine"), &built_in(), &[]));
    assert!(issues.iter().any(|i| i.path == "$.id"));
}

#[test]
fn rejects_id_of_other_user_layout_file() {
    let user = vec![LayoutSource {
        origin: user_origin("other"),
        text: candidate("mine", "Other"),
        built_in: false,
    }];
    let issues = invalid(check_import(&candidate("mine", "Mine"), &built_in(), &user));
    assert!(issues.iter().any(|i| i.path == "$.id"));
}

#[test]
fn allows_replacing_user_layout_with_same_id() {
    let user = vec![LayoutSource {
        origin: user_origin("mine"),
        text: candidate("mine", "Old"),
        built_in: false,
    }];
    assert_eq!(
        valid(check_import(&candidate("mine", "New"), &built_in(), &user)).name,
        "New"
    );
}

#[test]
fn allows_extending_built_in_layout() {
    let text = json!({"id": "mine", "name": "Mine", "locale": "de", "extends": "test"}).to_string();
    assert!(
        valid(check_import(&text, &built_in(), &[]))
            .layer("letters")
            .is_some()
    );
}

#[test]
fn rejects_extending_unknown_base() {
    let text =
        json!({"id": "mine", "name": "Mine", "locale": "de", "extends": "missing"}).to_string();
    assert_eq!(
        invalid(check_import(&text, &built_in(), &[]))[0].path,
        "$.extends"
    );
}

#[test]
fn returns_warnings_of_a_valid_layout() {
    let mut value = minimal();
    value["id"] = json!("mine");
    value["layers"]["letters"]["rows"] = json!([[{"type": "space"}]]);
    match check_import(&value.to_string(), &built_in(), &[]) {
        ImportResult::Valid { warnings, .. } => assert_eq!(warnings.len(), 1),
        other => panic!("{other:?}"),
    }
}
