use std::collections::{HashMap, HashSet};

use serde_json::{Map, Value};

use crate::issue::LayoutIssue;
use crate::model::KeyboardLayout;
use crate::parser::parse;

pub const MAX_FILE_BYTES: usize = 256 * 1024;

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct LayoutSource {
    pub origin: String,
    pub text: String,
    pub built_in: bool,
}

#[derive(Debug, Clone, PartialEq)]
pub struct LayoutFailure {
    pub origin: String,
    pub issues: Vec<LayoutIssue>,
}

#[derive(Debug, Clone, PartialEq)]
pub struct LoadedLayouts {
    pub layouts: Vec<KeyboardLayout>,
    pub failures: Vec<LayoutFailure>,
    pub warnings: Vec<LayoutFailure>,
    pub sources: HashMap<String, LayoutSource>,
}

fn failure(source: &LayoutSource, path: &str, message: impl Into<String>) -> LayoutFailure {
    LayoutFailure {
        origin: source.origin.clone(),
        issues: vec![LayoutIssue::error(path, message)],
    }
}

pub fn load(sources: &[LayoutSource]) -> LoadedLayouts {
    let mut failures = Vec::new();
    let mut parsed: Vec<(&LayoutSource, Map<String, Value>)> = Vec::new();
    let ordered = sources
        .iter()
        .filter(|s| s.built_in)
        .chain(sources.iter().filter(|s| !s.built_in));
    for source in ordered {
        if source.text.len() > MAX_FILE_BYTES {
            failures.push(failure(
                source,
                "$",
                format!("file is larger than {MAX_FILE_BYTES} bytes"),
            ));
            continue;
        }
        match serde_json::from_str::<Value>(&source.text) {
            Ok(Value::Object(object)) => parsed.push((source, object)),
            Ok(_) => failures.push(failure(
                source,
                "$",
                "malformed JSON: top-level value must be an object",
            )),
            Err(error) => failures.push(failure(source, "$", format!("malformed JSON: {error}"))),
        }
    }

    let mut bases: HashMap<String, &Map<String, Value>> = HashMap::new();
    for (source, object) in &parsed {
        if !source.built_in || object.contains_key("extends") {
            continue;
        }
        if let Some(id) = object
            .get("id")
            .and_then(Value::as_str)
            .filter(|id| !id.is_empty())
        {
            bases.insert(id.to_string(), object);
        }
    }

    let mut layouts = Vec::new();
    let mut warnings = Vec::new();
    let mut loaded_sources = HashMap::new();
    let mut used_ids = HashSet::new();
    for (source, object) in &parsed {
        let document = match resolve(source, object, &bases) {
            Ok(document) => document,
            Err(failed) => {
                failures.push(failed);
                continue;
            }
        };
        let result = parse(&Value::Object(document));
        let Some(layout) = result.layout else {
            failures.push(LayoutFailure {
                origin: source.origin.clone(),
                issues: result.issues,
            });
            continue;
        };
        if !used_ids.insert(layout.id.clone()) {
            let mut issues = result.issues;
            issues.push(LayoutIssue::error(
                "$.id",
                format!("id is already used by another layout: {}", layout.id),
            ));
            failures.push(LayoutFailure {
                origin: source.origin.clone(),
                issues,
            });
            continue;
        }
        loaded_sources.insert(layout.id.clone(), (*source).clone());
        if !result.issues.is_empty() {
            warnings.push(LayoutFailure {
                origin: source.origin.clone(),
                issues: result.issues,
            });
        }
        layouts.push(layout);
    }
    layouts.sort_by(|a, b| a.order.cmp(&b.order).then_with(|| a.name.cmp(&b.name)));
    LoadedLayouts {
        layouts,
        failures,
        warnings,
        sources: loaded_sources,
    }
}

fn resolve(
    source: &LayoutSource,
    object: &Map<String, Value>,
    bases: &HashMap<String, &Map<String, Value>>,
) -> Result<Map<String, Value>, LayoutFailure> {
    let Some(extends) = object.get("extends") else {
        return Ok(object.clone());
    };
    let Some(base_id) = extends.as_str() else {
        return Err(failure(source, "$.extends", "must be a string"));
    };
    let Some(base) = bases.get(base_id) else {
        return Err(failure(
            source,
            "$.extends",
            format!("unknown or non-extendable base layout: {base_id}"),
        ));
    };
    Ok(merge(object, base))
}

fn merge(child: &Map<String, Value>, base: &Map<String, Value>) -> Map<String, Value> {
    let mut merged = base.clone();
    for key in ["id", "name", "locale", "order"] {
        merged.shift_remove(key);
    }
    for (key, value) in child {
        match key.as_str() {
            "extends" => {}
            "layers" | "punctuation" => match (value, merged.get_mut(key)) {
                (Value::Object(own), Some(Value::Object(inherited))) => {
                    for (inner_key, inner_value) in own {
                        inherited.insert(inner_key.clone(), inner_value.clone());
                    }
                }
                _ => {
                    merged.insert(key.clone(), value.clone());
                }
            },
            _ => {
                merged.insert(key.clone(), value.clone());
            }
        }
    }
    merged
}
