const DOTTED_I_LANGUAGES: &[&str] = &["tr", "az", "crh"];

pub fn to_upper(text: &str, language: &str) -> String {
    let language = language.to_ascii_lowercase();
    if DOTTED_I_LANGUAGES.contains(&language.as_str()) {
        text.chars()
            .flat_map(|c| {
                if c == 'i' {
                    vec!['\u{130}']
                } else {
                    c.to_uppercase().collect()
                }
            })
            .collect()
    } else {
        text.to_uppercase()
    }
}
