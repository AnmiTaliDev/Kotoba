use serde_json::Value;

use crate::issue::LayoutIssue;
use crate::loader::{LayoutSource, MAX_FILE_BYTES, load};
use crate::model::KeyboardLayout;

pub const CANDIDATE_ORIGIN: &str = "import";

#[derive(Debug, Clone, PartialEq)]
pub enum ImportResult {
    Valid {
        layout: KeyboardLayout,
        warnings: Vec<LayoutIssue>,
    },
    Invalid {
        issues: Vec<LayoutIssue>,
    },
}

pub fn user_origin(id: &str) -> String {
    format!("user/{id}.json")
}

pub fn too_large_issue() -> LayoutIssue {
    LayoutIssue::error("$", format!("file is larger than {MAX_FILE_BYTES} bytes"))
}

pub fn check_import(text: &str, built_in: &[LayoutSource], user: &[LayoutSource]) -> ImportResult {
    let candidate_id = serde_json::from_str::<Value>(text)
        .ok()
        .and_then(|value| value.get("id").and_then(Value::as_str).map(str::to_owned))
        .unwrap_or_default();
    let replaced = user_origin(&candidate_id);
    let mut sources: Vec<LayoutSource> = built_in.to_vec();
    sources.extend(
        user.iter()
            .filter(|source| source.origin != replaced)
            .cloned(),
    );
    sources.push(LayoutSource {
        origin: CANDIDATE_ORIGIN.to_string(),
        text: text.to_string(),
        built_in: false,
    });

    let loaded = load(&sources);
    if let Some(failed) = loaded
        .failures
        .iter()
        .find(|f| f.origin == CANDIDATE_ORIGIN)
    {
        return ImportResult::Invalid {
            issues: failed.issues.clone(),
        };
    }
    let layout = loaded
        .layouts
        .iter()
        .find(|layout| {
            loaded
                .sources
                .get(&layout.id)
                .is_some_and(|s| s.origin == CANDIDATE_ORIGIN)
        })
        .cloned();
    let Some(layout) = layout else {
        return ImportResult::Invalid {
            issues: vec![LayoutIssue::error("$", "layout was not loaded")],
        };
    };
    let warnings = loaded
        .warnings
        .iter()
        .find(|w| w.origin == CANDIDATE_ORIGIN)
        .map(|w| w.issues.clone())
        .unwrap_or_default();
    ImportResult::Valid { layout, warnings }
}
