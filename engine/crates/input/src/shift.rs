#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash, Default)]
pub enum ShiftState {
    #[default]
    Off,
    Once,
    Caps,
}

pub const DOUBLE_TAP_MS: u64 = 350;

pub fn tap_shift(current: ShiftState, since_previous_tap_ms: u64) -> ShiftState {
    if current == ShiftState::Caps {
        ShiftState::Off
    } else if since_previous_tap_ms < DOUBLE_TAP_MS {
        ShiftState::Caps
    } else if current == ShiftState::Off {
        ShiftState::Once
    } else {
        ShiftState::Off
    }
}

pub fn shift_after_edit(
    current: ShiftState,
    text_before: &str,
    sentence_end: &str,
    auto_capitalize: bool,
) -> ShiftState {
    if current == ShiftState::Caps {
        ShiftState::Caps
    } else if auto_capitalize && starts_sentence(text_before, sentence_end) {
        ShiftState::Once
    } else {
        ShiftState::Off
    }
}

fn starts_sentence(text_before: &str, sentence_end: &str) -> bool {
    let Some(last) = text_before.chars().last() else {
        return true;
    };
    if !last.is_whitespace() {
        return false;
    }
    text_before
        .trim_end()
        .chars()
        .last()
        .is_some_and(|c| sentence_end.contains(c))
}
