#[derive(Debug, Clone, PartialEq, Eq)]
pub struct LanguageTag {
    pub language: String,
    pub script: Option<String>,
    pub region: Option<String>,
}

fn alpha(part: &str) -> bool {
    !part.is_empty() && part.bytes().all(|b| b.is_ascii_alphabetic())
}

fn digits(part: &str) -> bool {
    !part.is_empty() && part.bytes().all(|b| b.is_ascii_digit())
}

fn alphanum(part: &str) -> bool {
    !part.is_empty() && part.bytes().all(|b| b.is_ascii_alphanumeric())
}

pub fn parse_language_tag(tag: &str) -> Result<LanguageTag, String> {
    let parts: Vec<&str> = tag.split('-').collect();
    if parts
        .iter()
        .any(|p| p.is_empty() || p.len() > 8 || !alphanum(p))
    {
        return Err(format!("not a valid BCP 47 tag: {tag}"));
    }
    let language = parts[0];
    let language_ok = alpha(language) && matches!(language.len(), 2 | 3 | 5..=8);
    if !language_ok {
        return Err(format!("not a valid BCP 47 tag: {tag}"));
    }
    let mut index = 1;
    let mut script = None;
    if let Some(part) = parts.get(index) {
        if part.len() == 4 && alpha(part) {
            script = Some(titlecase(part));
            index += 1;
        }
    }
    let mut region = None;
    if let Some(part) = parts.get(index) {
        if (part.len() == 2 && alpha(part)) || (part.len() == 3 && digits(part)) {
            region = Some(part.to_ascii_uppercase());
            index += 1;
        }
    }
    while let Some(part) = parts.get(index) {
        let variant = (5..=8).contains(&part.len())
            || (part.len() == 4 && part.as_bytes()[0].is_ascii_digit());
        if !variant {
            break;
        }
        index += 1;
    }
    while index < parts.len() {
        let singleton = parts[index];
        if singleton.len() != 1 {
            return Err(format!("not a valid BCP 47 tag: {tag}"));
        }
        let private_use = singleton.eq_ignore_ascii_case("x");
        index += 1;
        let start = index;
        while index < parts.len() && (private_use || parts[index].len() >= 2) {
            if parts[index].len() == 1 && !private_use {
                break;
            }
            index += 1;
        }
        if index == start {
            return Err(format!("not a valid BCP 47 tag: {tag}"));
        }
        if private_use && index != parts.len() {
            return Err(format!("not a valid BCP 47 tag: {tag}"));
        }
    }
    Ok(LanguageTag {
        language: language.to_ascii_lowercase(),
        script,
        region,
    })
}

fn titlecase(part: &str) -> String {
    let mut out = part.to_ascii_lowercase();
    out[..1].make_ascii_uppercase();
    out
}
