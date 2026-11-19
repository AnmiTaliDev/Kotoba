#![forbid(unsafe_code)]

use serde_json::Value;

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct EmojiCategory {
    pub id: String,
    pub name: String,
    pub icon: String,
    pub emoji: Vec<String>,
    pub recent: bool,
}

pub const RECENT_LIMIT: usize = 32;

const RECENT_SEPARATOR: char = '\u{1f}';

pub fn parse_emoji_data(json: &str) -> Result<Vec<EmojiCategory>, String> {
    let root: Value =
        serde_json::from_str(json).map_err(|error| format!("malformed JSON: {error}"))?;
    let categories = root
        .get("categories")
        .and_then(Value::as_array)
        .ok_or_else(|| "$.categories: must be an array".to_string())?;
    categories
        .iter()
        .enumerate()
        .map(|(index, category)| parse_category(category, &format!("$.categories[{index}]")))
        .collect()
}

fn parse_category(value: &Value, path: &str) -> Result<EmojiCategory, String> {
    let text = |key: &str| {
        value
            .get(key)
            .and_then(Value::as_str)
            .map(str::to_owned)
            .ok_or_else(|| format!("{path}.{key}: must be a string"))
    };
    let emoji = value
        .get("emoji")
        .and_then(Value::as_array)
        .ok_or_else(|| format!("{path}.emoji: must be an array"))?
        .iter()
        .enumerate()
        .map(|(index, item)| {
            item.as_str()
                .map(str::to_owned)
                .ok_or_else(|| format!("{path}.emoji[{index}]: must be a string"))
        })
        .collect::<Result<Vec<_>, _>>()?;
    Ok(EmojiCategory {
        id: text("id")?,
        name: text("name")?,
        icon: text("icon")?,
        emoji,
        recent: value
            .get("recent")
            .and_then(Value::as_bool)
            .unwrap_or(false),
    })
}

pub fn add_recent(current: &[String], emoji: &str, limit: usize) -> Vec<String> {
    std::iter::once(emoji.to_string())
        .chain(
            current
                .iter()
                .filter(|item| item.as_str() != emoji)
                .cloned(),
        )
        .take(limit)
        .collect()
}

pub fn encode_recents(recents: &[String]) -> String {
    recents.join(&RECENT_SEPARATOR.to_string())
}

pub fn decode_recents(stored: &str) -> Vec<String> {
    stored
        .split(RECENT_SEPARATOR)
        .filter(|item| !item.is_empty())
        .map(str::to_owned)
        .collect()
}
