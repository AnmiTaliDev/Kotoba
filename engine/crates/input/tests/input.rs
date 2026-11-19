use kotoba_input::{DOUBLE_TAP_MS, ShiftState, shift_after_edit, space_edit, tap_shift};

#[test]
fn first_tap_turns_shift_on_for_one_character() {
    assert_eq!(tap_shift(ShiftState::Off, 10_000), ShiftState::Once);
}

#[test]
fn slow_second_tap_turns_shift_off() {
    assert_eq!(tap_shift(ShiftState::Once, 10_000), ShiftState::Off);
}

#[test]
fn quick_second_tap_turns_caps_on() {
    assert_eq!(
        tap_shift(ShiftState::Once, DOUBLE_TAP_MS - 1),
        ShiftState::Caps
    );
    assert_eq!(tap_shift(ShiftState::Off, 0), ShiftState::Caps);
}

#[test]
fn tap_leaves_caps() {
    assert_eq!(tap_shift(ShiftState::Caps, 0), ShiftState::Off);
    assert_eq!(tap_shift(ShiftState::Caps, 10_000), ShiftState::Off);
}

#[test]
fn double_tap_boundary_is_exclusive() {
    assert_eq!(tap_shift(ShiftState::Off, DOUBLE_TAP_MS), ShiftState::Once);
}

#[test]
fn caps_survives_edits() {
    assert_eq!(
        shift_after_edit(ShiftState::Caps, "abc", ".", true),
        ShiftState::Caps
    );
    assert_eq!(
        shift_after_edit(ShiftState::Caps, "", ".", false),
        ShiftState::Caps
    );
}

#[test]
fn empty_text_starts_a_sentence() {
    assert_eq!(
        shift_after_edit(ShiftState::Off, "", ".!?", true),
        ShiftState::Once
    );
}

#[test]
fn sentence_end_followed_by_space_starts_a_sentence() {
    assert_eq!(
        shift_after_edit(ShiftState::Off, "Hello. ", ".!?", true),
        ShiftState::Once
    );
    assert_eq!(
        shift_after_edit(ShiftState::Off, "What?  ", ".!?", true),
        ShiftState::Once
    );
}

#[test]
fn text_without_trailing_space_does_not_start_a_sentence() {
    assert_eq!(
        shift_after_edit(ShiftState::Off, "Hello.", ".!?", true),
        ShiftState::Off
    );
    assert_eq!(
        shift_after_edit(ShiftState::Once, "Hello", ".!?", true),
        ShiftState::Off
    );
}

#[test]
fn space_after_a_word_does_not_start_a_sentence() {
    assert_eq!(
        shift_after_edit(ShiftState::Off, "Hello ", ".!?", true),
        ShiftState::Off
    );
}

#[test]
fn only_whitespace_before_the_cursor_is_not_after_a_sentence_end() {
    assert_eq!(
        shift_after_edit(ShiftState::Off, "   ", ".!?", true),
        ShiftState::Off
    );
}

#[test]
fn disabled_auto_capitalize_never_turns_shift_on() {
    assert_eq!(
        shift_after_edit(ShiftState::Off, "", ".!?", false),
        ShiftState::Off
    );
    assert_eq!(
        shift_after_edit(ShiftState::Once, "A. ", ".!?", false),
        ShiftState::Off
    );
}

#[test]
fn sentence_end_characters_come_from_the_layout() {
    assert_eq!(
        shift_after_edit(ShiftState::Off, "word\u{61f} ", "\u{61f}", true),
        ShiftState::Once
    );
    assert_eq!(
        shift_after_edit(ShiftState::Off, "word. ", "\u{61f}", true),
        ShiftState::Off
    );
}

#[test]
fn plain_space_inserts_a_space() {
    let edit = space_edit("Hello", ".");
    assert_eq!((edit.delete_before, edit.insert.as_str()), (0, " "));
}

#[test]
fn double_space_after_a_word_inserts_the_period() {
    let edit = space_edit("Hello ", ".");
    assert_eq!((edit.delete_before, edit.insert.as_str()), (1, ". "));
}

#[test]
fn double_space_after_a_digit_inserts_the_period() {
    assert_eq!(space_edit("year 2026 ", ".").insert, ". ");
}

#[test]
fn double_space_needs_a_word_before_the_space() {
    assert_eq!(space_edit("Hello. ", ".").insert, " ");
    assert_eq!(space_edit(" ", ".").insert, " ");
    assert_eq!(space_edit("", ".").insert, " ");
}

#[test]
fn empty_double_space_disables_the_feature() {
    assert_eq!(space_edit("Hello ", "").insert, " ");
}

#[test]
fn double_space_works_with_non_latin_letters() {
    assert_eq!(space_edit("привет ", ".").insert, ". ");
    assert_eq!(space_edit("қазақ ", ".").insert, ". ");
}

#[test]
fn double_space_text_comes_from_the_layout() {
    assert_eq!(space_edit("Hello ", "\u{3002}").insert, "\u{3002} ");
}
