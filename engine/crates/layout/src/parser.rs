use serde_json::{Map, Value};

use crate::case::to_upper;
use crate::issue::{LayoutIssue, Severity};
use crate::locale::parse_language_tag;
use crate::model::{
    CharKey, Key, KeyboardLayout, LanguageKey, Layer, LayerKey, LayerKind, LayerLink,
};

pub const SUPPORTED_FORMAT_VERSION: i64 = 1;
pub const MAX_KEYS: usize = 200;
pub const MAX_LABEL_LENGTH: usize = 8;
pub const MAX_HOLD: usize = 12;
pub const MAX_KEY_WEIGHT: f64 = 10.0;
pub const MAX_ROW_WEIGHT: f64 = 14.0;
pub const DEFAULT_ORDER: i32 = 100;

#[derive(Debug, Clone, PartialEq)]
pub struct ParseResult {
    pub layout: Option<KeyboardLayout>,
    pub issues: Vec<LayoutIssue>,
}

pub fn parse(root: &Value) -> ParseResult {
    let mut reader = Reader::default();
    let layout = match root.as_object() {
        Some(object) => build(object, &mut reader),
        None => {
            reader.error("$", "must be an object");
            None
        }
    };
    let has_errors = reader
        .issues
        .iter()
        .any(|issue| issue.severity == Severity::Error);
    ParseResult {
        layout: if has_errors { None } else { layout },
        issues: reader.issues,
    }
}

fn valid_id(id: &str) -> bool {
    (1..=32).contains(&id.len())
        && id
            .bytes()
            .all(|b| b.is_ascii_lowercase() || b.is_ascii_digit() || b == b'_' || b == b'-')
}

fn build(root: &Map<String, Value>, r: &mut Reader) -> Option<KeyboardLayout> {
    let version = r.opt_int(root, "formatVersion", "$").unwrap_or(1);
    if !(1..=SUPPORTED_FORMAT_VERSION).contains(&version) {
        r.error(
            "$.formatVersion",
            format!("unsupported format version {version}, supported: {SUPPORTED_FORMAT_VERSION}"),
        );
    }
    let id = r.string(root, "id", "$");
    if let Some(id) = &id {
        if !valid_id(id) {
            r.error("$.id", "must match [a-z0-9_-]{1,32}");
        }
    }
    let name = r.string(root, "name", "$");
    if let Some(name) = &name {
        if name.trim().is_empty() {
            r.error("$.name", "must not be blank");
        }
    }
    let locale_tag = r.string(root, "locale", "$");
    let language = locale_tag
        .as_deref()
        .and_then(|tag| match parse_language_tag(tag) {
            Ok(parsed) => Some(parsed.language),
            Err(message) => {
                r.error("$.locale", message);
                None
            }
        });
    let auto_capitalize = r.opt_bool(root, "autoCapitalize", "$").unwrap_or(true);
    let order = match r.opt_int(root, "order", "$") {
        Some(value) => match i32::try_from(value) {
            Ok(order) => order,
            Err(_) => {
                r.error("$.order", "must be an integer");
                DEFAULT_ORDER
            }
        },
        None => DEFAULT_ORDER,
    };

    let punctuation = r.object(root, "punctuation", "$");
    let sentence_end = punctuation.and_then(|p| r.string(p, "sentenceEnd", "$.punctuation"));
    let double_space = punctuation.and_then(|p| r.string(p, "doubleSpace", "$.punctuation"));

    let mut key_count = 0usize;
    let lang = language.as_deref();
    let number_row = r
        .opt_array(root, "numberRow", "$")
        .map(|row| read_row(row, "$.numberRow", lang, r, &mut key_count))
        .unwrap_or_default();

    let layers_json = r.object(root, "layers", "$");
    let mut layers: Vec<Layer> = Vec::new();
    if let Some(layers_json) = layers_json {
        if layers_json.is_empty() {
            r.error("$.layers", "must contain at least one layer");
        }
        for (layer_id, value) in layers_json {
            let path = format!("$.layers.{layer_id}");
            if !valid_id(layer_id) {
                r.error(&path, "layer id must match [a-z0-9_-]{1,32}");
            }
            let Some(layer_json) = value.as_object() else {
                r.error(&path, "must be an object");
                continue;
            };
            if let Some(layer) = read_layer(layer_id, layer_json, &path, lang, r, &mut key_count) {
                layers.push(layer);
            }
        }
    }
    if key_count > MAX_KEYS {
        r.error(
            "$",
            format!("too many keys: {key_count}, maximum {MAX_KEYS}"),
        );
    }

    let default_layer = r.string(root, "defaultLayer", "$");
    if let (Some(default_layer), Some(_)) = (&default_layer, layers_json) {
        if !layers.iter().any(|layer| &layer.id == default_layer) {
            r.error("$.defaultLayer", format!("unknown layer: {default_layer}"));
        }
    }
    resolve_labels(&mut layers, r);
    let known = |target: &str| layers.iter().any(|layer| layer.id == target);
    for layer in &layers {
        if let Some(back) = &layer.back {
            if !known(&back.target) {
                r.error(
                    format!("$.layers.{}.back.layer", layer.id),
                    format!("unknown layer: {}", back.target),
                );
            }
        }
        for (row_index, row) in layer.rows.iter().enumerate() {
            for (key_index, key) in row.iter().enumerate() {
                if let Key::Layer(link) = key {
                    if !known(&link.target) {
                        r.error(
                            format!("$.layers.{}.rows[{row_index}][{key_index}].layer", layer.id),
                            format!("unknown layer: {}", link.target),
                        );
                    }
                }
            }
        }
    }
    if let Some(start) = default_layer
        .as_ref()
        .and_then(|id| layers.iter().find(|l| &l.id == id))
    {
        if start.kind == LayerKind::Keys {
            let keys: Vec<&Key> = start.rows.iter().flatten().collect();
            let path = format!("$.layers.{}", start.id);
            if !keys.iter().any(|key| matches!(key, Key::Space { .. })) {
                r.error(&path, "default layer must contain a space key");
            }
            if !keys.iter().any(|key| matches!(key, Key::Delete { .. })) {
                r.warn(&path, "default layer has no delete key");
            }
        }
    }

    Some(KeyboardLayout {
        id: id?,
        name: name?,
        locale: locale_tag?,
        default_layer: default_layer?,
        sentence_end: sentence_end?,
        double_space: double_space?,
        auto_capitalize,
        order,
        number_row,
        layers,
    })
}

fn resolve_labels(layers: &mut [Layer], r: &mut Reader) {
    let names: std::collections::HashMap<String, String> = layers
        .iter()
        .filter_map(|layer| {
            layer
                .label
                .clone()
                .filter(|label| !label.is_empty())
                .map(|label| (layer.id.clone(), label))
        })
        .collect();
    let ids: std::collections::HashSet<String> =
        layers.iter().map(|layer| layer.id.clone()).collect();
    for layer in layers.iter_mut() {
        let layer_id = layer.id.clone();
        if let Some(back) = &mut layer.back {
            if back.label.is_empty() {
                match names.get(&back.target) {
                    Some(name) => back.label = name.clone(),
                    None if ids.contains(&back.target) => r.error(
                        format!("$.layers.{layer_id}.back.label"),
                        "is required unless the target layer has a label",
                    ),
                    None => {}
                }
            }
        }
        for (row_index, row) in layer.rows.iter_mut().enumerate() {
            for (key_index, key) in row.iter_mut().enumerate() {
                let Key::Layer(link) = key else { continue };
                if !link.label.is_empty() {
                    continue;
                }
                match names.get(&link.target) {
                    Some(name) => link.label = name.clone(),
                    None if ids.contains(&link.target) => r.error(
                        format!("$.layers.{layer_id}.rows[{row_index}][{key_index}].label"),
                        "is required unless the target layer has a label",
                    ),
                    None => {}
                }
            }
        }
    }
}

fn read_layer(
    id: &str,
    json: &Map<String, Value>,
    path: &str,
    lang: Option<&str>,
    r: &mut Reader,
    key_count: &mut usize,
) -> Option<Layer> {
    let kind = match r
        .opt_string(json, "kind", path)
        .as_deref()
        .unwrap_or("keys")
    {
        "keys" => LayerKind::Keys,
        "emoji" => LayerKind::Emoji,
        other => {
            r.error(
                format!("{path}.kind"),
                format!("unknown layer kind: {other}"),
            );
            return None;
        }
    };
    let mut rows: Vec<Vec<Key>> = Vec::new();
    if let Some(rows_json) = r.opt_array(json, "rows", path) {
        for (index, value) in rows_json.iter().enumerate() {
            let row_path = format!("{path}.rows[{index}]");
            let Some(row_json) = value.as_array() else {
                r.error(&row_path, "must be an array");
                continue;
            };
            let row = read_row(row_json, &row_path, lang, r, key_count);
            if row.is_empty() {
                r.error(&row_path, "must not be empty");
                continue;
            }
            let weight: f64 = row.iter().map(|key| f64::from(key.weight())).sum();
            if weight > MAX_ROW_WEIGHT {
                r.warn(
                    &row_path,
                    format!("row is wide: total weight {weight:?}, keys may be too small"),
                );
            }
            rows.push(row);
        }
    }
    let back = r.opt_object(json, "back", path).and_then(|back| {
        let back_path = format!("{path}.back");
        let label = r.opt_string(back, "label", &back_path).unwrap_or_default();
        let target = r.string(back, "layer", &back_path);
        Some(LayerLink {
            label,
            target: target?,
        })
    });
    let label = r.opt_string(json, "label", path);
    if let Some(label) = &label {
        if label.chars().count() > MAX_LABEL_LENGTH {
            r.error(
                format!("{path}.label"),
                format!("is longer than {MAX_LABEL_LENGTH} characters"),
            );
        }
    }
    if kind == LayerKind::Keys && rows.is_empty() {
        r.error(format!("{path}.rows"), "must contain at least one row");
    }
    if kind == LayerKind::Emoji && back.is_none() {
        r.error(format!("{path}.back"), "emoji layer requires back");
    }
    Some(Layer {
        id: id.to_string(),
        label,
        kind,
        shiftable: r.opt_bool(json, "shiftable", path).unwrap_or(false),
        number_row: r.opt_bool(json, "numberRow", path).unwrap_or(false),
        rows,
        back,
    })
}

fn read_row(
    json: &[Value],
    path: &str,
    lang: Option<&str>,
    r: &mut Reader,
    key_count: &mut usize,
) -> Vec<Key> {
    let mut keys = Vec::with_capacity(json.len());
    for (index, value) in json.iter().enumerate() {
        if let Some(key) = read_key(value, &format!("{path}[{index}]"), lang, r) {
            keys.push(key);
            *key_count += 1;
        }
    }
    keys
}

fn read_key(raw: &Value, path: &str, lang: Option<&str>, r: &mut Reader) -> Option<Key> {
    if let Some(label) = raw.as_str() {
        return char_key(label, None, 1.0, path, lang, r, None);
    }
    let Some(object) = raw.as_object() else {
        r.error(path, "must be a string or an object");
        return None;
    };
    let weight = r.opt_number(object, "weight", path).unwrap_or(1.0);
    if weight <= 0.0 || weight > MAX_KEY_WEIGHT {
        r.error(
            format!("{path}.weight"),
            format!("must be greater than 0 and at most {MAX_KEY_WEIGHT:?}"),
        );
        return None;
    }
    let weight = weight as f32;
    let kind = r
        .opt_string(object, "type", path)
        .unwrap_or_else(|| "char".to_string());
    match kind.as_str() {
        "char" => {
            let label = r.string(object, "label", path)?;
            let shifted = r.opt_string(object, "shifted", path);
            char_key(&label, shifted, weight, path, lang, r, Some(object))
        }
        "shift" => Some(Key::Shift { weight }),
        "delete" => Some(Key::Delete { weight }),
        "enter" => Some(Key::Enter { weight }),
        "space" => Some(Key::Space { weight }),
        "spacer" => Some(Key::Spacer { weight }),
        "layer" => {
            let label = r.opt_string(object, "label", path).unwrap_or_default();
            let target = r.string(object, "layer", path)?;
            let icon = r
                .opt_string(object, "icon", path)
                .filter(|icon| !icon.is_empty());
            Some(Key::Layer(LayerKey {
                label,
                target,
                icon,
                weight,
            }))
        }
        "language" => {
            let label = r.string(object, "label", path)?;
            let icon = r
                .opt_string(object, "icon", path)
                .filter(|icon| !icon.is_empty());
            Some(Key::Language(LanguageKey {
                label,
                icon,
                weight,
            }))
        }
        other => {
            r.error(format!("{path}.type"), format!("unknown key type: {other}"));
            None
        }
    }
}

fn char_key(
    label: &str,
    shifted: Option<String>,
    weight: f32,
    path: &str,
    lang: Option<&str>,
    r: &mut Reader,
    json: Option<&Map<String, Value>>,
) -> Option<Key> {
    if label.is_empty() {
        r.error(path, "label must not be empty");
        return None;
    }
    if label.chars().count() > MAX_LABEL_LENGTH {
        r.error(
            path,
            format!("label is longer than {MAX_LABEL_LENGTH} characters"),
        );
        return None;
    }
    if let Some(shifted) = &shifted {
        if shifted.chars().count() > MAX_LABEL_LENGTH {
            r.error(
                format!("{path}.shifted"),
                format!("is longer than {MAX_LABEL_LENGTH} characters"),
            );
            return None;
        }
    }
    let language = lang.unwrap_or("");
    let upper = shifted.unwrap_or_else(|| to_upper(label, language));
    let hold = json
        .and_then(|j| read_alternatives(j, "hold", path, r))
        .unwrap_or_default();
    let explicit = json.and_then(|j| read_alternatives(j, "holdShifted", path, r));
    if let Some(explicit) = &explicit {
        if explicit.len() != hold.len() {
            r.error(
                format!("{path}.holdShifted"),
                "must have the same number of items as hold",
            );
            return None;
        }
    }
    let hold_shifted =
        explicit.unwrap_or_else(|| hold.iter().map(|item| to_upper(item, language)).collect());
    Some(Key::Char(CharKey {
        label: label.to_string(),
        shifted: upper,
        weight,
        hold,
        hold_shifted,
    }))
}

fn read_alternatives(
    json: &Map<String, Value>,
    key: &str,
    path: &str,
    r: &mut Reader,
) -> Option<Vec<String>> {
    let array = r.opt_array(json, key, path)?;
    if array.len() > MAX_HOLD {
        r.error(
            format!("{path}.{key}"),
            format!("has more than {MAX_HOLD} items"),
        );
        return None;
    }
    let mut items = Vec::with_capacity(array.len());
    for (index, value) in array.iter().enumerate() {
        match value.as_str() {
            Some(text) if !text.is_empty() && text.chars().count() <= MAX_LABEL_LENGTH => {
                items.push(text.to_string());
            }
            _ => {
                r.error(
                    format!("{path}.{key}[{index}]"),
                    format!("must be a string of 1 to {MAX_LABEL_LENGTH} characters"),
                );
                return None;
            }
        }
    }
    Some(items)
}

#[derive(Default)]
struct Reader {
    issues: Vec<LayoutIssue>,
}

impl Reader {
    fn error(&mut self, path: impl Into<String>, message: impl Into<String>) {
        self.issues.push(LayoutIssue::error(path, message));
    }

    fn warn(&mut self, path: impl Into<String>, message: impl Into<String>) {
        self.issues.push(LayoutIssue::warning(path, message));
    }

    fn present<'a>(object: &'a Map<String, Value>, key: &str) -> Option<&'a Value> {
        object.get(key).filter(|value| !value.is_null())
    }

    fn string(&mut self, object: &Map<String, Value>, key: &str, path: &str) -> Option<String> {
        match Self::present(object, key) {
            None => {
                self.error(format!("{path}.{key}"), "is required");
                None
            }
            Some(Value::String(text)) => Some(text.clone()),
            Some(_) => {
                self.error(format!("{path}.{key}"), "must be a string");
                None
            }
        }
    }

    fn opt_string(&mut self, object: &Map<String, Value>, key: &str, path: &str) -> Option<String> {
        match Self::present(object, key)? {
            Value::String(text) => Some(text.clone()),
            _ => {
                self.error(format!("{path}.{key}"), "must be a string");
                None
            }
        }
    }

    fn opt_bool(&mut self, object: &Map<String, Value>, key: &str, path: &str) -> Option<bool> {
        match Self::present(object, key)? {
            Value::Bool(value) => Some(*value),
            _ => {
                self.error(format!("{path}.{key}"), "must be true or false");
                None
            }
        }
    }

    fn opt_int(&mut self, object: &Map<String, Value>, key: &str, path: &str) -> Option<i64> {
        match Self::present(object, key)? {
            Value::Number(number) if number.is_i64() || number.is_u64() => {
                number.as_i64().or_else(|| {
                    self.error(format!("{path}.{key}"), "must be an integer");
                    None
                })
            }
            _ => {
                self.error(format!("{path}.{key}"), "must be an integer");
                None
            }
        }
    }

    fn opt_number(&mut self, object: &Map<String, Value>, key: &str, path: &str) -> Option<f64> {
        match Self::present(object, key)? {
            Value::Number(number) => number.as_f64(),
            _ => {
                self.error(format!("{path}.{key}"), "must be a number");
                None
            }
        }
    }

    fn object<'a>(
        &mut self,
        object: &'a Map<String, Value>,
        key: &str,
        path: &str,
    ) -> Option<&'a Map<String, Value>> {
        match Self::present(object, key) {
            None => {
                self.error(format!("{path}.{key}"), "is required");
                None
            }
            Some(Value::Object(inner)) => Some(inner),
            Some(_) => {
                self.error(format!("{path}.{key}"), "must be an object");
                None
            }
        }
    }

    fn opt_object<'a>(
        &mut self,
        object: &'a Map<String, Value>,
        key: &str,
        path: &str,
    ) -> Option<&'a Map<String, Value>> {
        match Self::present(object, key)? {
            Value::Object(inner) => Some(inner),
            _ => {
                self.error(format!("{path}.{key}"), "must be an object");
                None
            }
        }
    }

    fn opt_array<'a>(
        &mut self,
        object: &'a Map<String, Value>,
        key: &str,
        path: &str,
    ) -> Option<&'a Vec<Value>> {
        match Self::present(object, key)? {
            Value::Array(inner) => Some(inner),
            _ => {
                self.error(format!("{path}.{key}"), "must be an array");
                None
            }
        }
    }
}
