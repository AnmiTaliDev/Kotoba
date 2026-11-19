#[derive(Debug, Clone, PartialEq, Eq)]
pub struct SpaceEdit {
    pub delete_before: usize,
    pub insert: String,
}

pub fn space_edit(text_before: &str, double_space: &str) -> SpaceEdit {
    let mut tail = text_before.chars().rev();
    let after_word_and_space = !double_space.is_empty()
        && tail.next() == Some(' ')
        && tail.next().is_some_and(char::is_alphanumeric);
    if after_word_and_space {
        SpaceEdit {
            delete_before: 1,
            insert: format!("{double_space} "),
        }
    } else {
        SpaceEdit {
            delete_before: 0,
            insert: " ".to_string(),
        }
    }
}
