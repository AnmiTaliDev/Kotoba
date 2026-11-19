use kotoba_input::ShiftState;
use kotoba_layout::{KeyboardLayout, LayoutSource, load};
use kotoba_session::{Command, Context, EnterAction, Event, Needs, Session, SessionError};

fn layouts() -> Vec<KeyboardLayout> {
    let sources: Vec<LayoutSource> = kotoba_data::layouts()
        .iter()
        .map(|(name, text)| LayoutSource {
            origin: (*name).into(),
            text: (*text).into(),
            built_in: true,
        })
        .collect();
    load(&sources).layouts
}

fn session() -> Session {
    Session::new(layouts()).unwrap()
}

fn ctx(now_ms: u64) -> Context {
    Context {
        now_ms,
        ..Context::default()
    }
}

fn text_ctx(before: &str) -> Context {
    Context {
        text_before: before.to_string(),
        ..Context::default()
    }
}

fn commit(text: &str) -> Command {
    Command::Commit {
        text: text.to_string(),
    }
}

#[test]
fn needs_at_least_one_layout() {
    assert_eq!(
        Session::new(Vec::new()).unwrap_err(),
        SessionError::NoLayouts
    );
}

#[test]
fn starts_on_the_first_layout_and_its_default_layer() {
    let session = session();
    let state = session.state();
    assert_eq!(state.layout_id, session.layouts()[0].id);
    assert_eq!(state.layer_id, "letters");
    assert_eq!(state.shift, ShiftState::Off);
    assert_eq!(state.enter_action, EnterAction::Newline);
}

#[test]
fn char_press_commits_text() {
    let mut session = session();
    let outcome = session.press(Event::Char { text: "a".into() }, &ctx(0));
    assert_eq!(outcome.commands, [commit("a")]);
    assert!(!outcome.needs_text_after_edit);
}

#[test]
fn shift_once_is_used_up_by_a_character() {
    let mut session = session();
    session.press(Event::Shift, &ctx(1_000));
    assert_eq!(session.state().shift, ShiftState::Once);
    session.press(Event::Char { text: "A".into() }, &ctx(2_000));
    assert_eq!(session.state().shift, ShiftState::Off);
}

#[test]
fn double_tap_turns_on_caps_and_characters_keep_it() {
    let mut session = session();
    session.press(Event::Shift, &ctx(1_000));
    session.press(Event::Shift, &ctx(1_100));
    assert_eq!(session.state().shift, ShiftState::Caps);
    session.press(Event::Char { text: "A".into() }, &ctx(2_000));
    assert_eq!(session.state().shift, ShiftState::Caps);
    session.press(Event::Shift, &ctx(3_000));
    assert_eq!(session.state().shift, ShiftState::Off);
}

#[test]
fn first_shift_tap_is_never_a_double_tap() {
    let mut session = session();
    session.press(Event::Shift, &ctx(0));
    assert_eq!(session.state().shift, ShiftState::Once);
}

#[test]
fn shift_applies_only_on_shiftable_layers() {
    let mut session = session();
    session.press(Event::Shift, &ctx(1_000));
    assert!(session.state().shift_applies);
    session.press(
        Event::SwitchLayer {
            layer_id: "symbols".into(),
        },
        &ctx(0),
    );
    assert!(!session.state().shift_applies);
    assert_eq!(session.state().shift, ShiftState::Once);
}

#[test]
fn auto_capitalize_asks_for_text_after_edits() {
    let mut session = session();
    session.start_input(EnterAction::Newline, true, false);
    assert!(session.needs_text_after_edit());
    session.after_edit("");
    assert_eq!(session.state().shift, ShiftState::Once);
    let outcome = session.press(Event::Char { text: "H".into() }, &ctx(0));
    assert!(outcome.needs_text_after_edit);
    session.after_edit("H");
    assert_eq!(session.state().shift, ShiftState::Off);
    session.press(Event::Char { text: ".".into() }, &ctx(0));
    session.after_edit("H. ");
    assert_eq!(session.state().shift, ShiftState::Once);
}

#[test]
fn fields_without_auto_capitalize_never_ask_for_text() {
    let mut session = session();
    session.start_input(EnterAction::Newline, false, false);
    assert!(!session.needs_text_after_edit());
}

#[test]
fn layouts_can_turn_auto_capitalize_off() {
    let mut layouts = layouts();
    layouts[0].auto_capitalize = false;
    let mut session = Session::new(layouts).unwrap();
    session.start_input(EnterAction::Newline, true, false);
    assert!(!session.needs_text_after_edit());
}

#[test]
fn caps_lock_does_not_ask_for_text() {
    let mut session = session();
    session.start_input(EnterAction::Newline, true, false);
    session.press(Event::Shift, &ctx(0));
    session.press(Event::Shift, &ctx(10));
    assert_eq!(session.state().shift, ShiftState::Caps);
    assert!(!session.needs_text_after_edit());
    session.after_edit("x");
    assert_eq!(session.state().shift, ShiftState::Caps);
}

#[test]
fn space_after_a_word_and_a_space_inserts_the_period() {
    let mut session = session();
    let outcome = session.press(Event::Space, &text_ctx("Hello "));
    assert_eq!(
        outcome.commands,
        [
            Command::BeginBatch,
            Command::DeleteBefore { code_points: 1 },
            commit(". "),
            Command::EndBatch
        ]
    );
}

#[test]
fn plain_space_is_a_batch_with_one_commit() {
    let mut session = session();
    let outcome = session.press(Event::Space, &text_ctx("Hello"));
    assert_eq!(
        outcome.commands,
        [Command::BeginBatch, commit(" "), Command::EndBatch]
    );
}

#[test]
fn delete_removes_one_character_without_a_selection() {
    let mut session = session();
    let outcome = session.press(Event::Delete, &Context::default());
    assert_eq!(outcome.commands, [Command::DeleteBefore { code_points: 1 }]);
}

#[test]
fn delete_replaces_a_selection_with_nothing() {
    let mut session = session();
    let outcome = session.press(
        Event::Delete,
        &Context {
            has_selection: true,
            ..Context::default()
        },
    );
    assert_eq!(outcome.commands, [commit("")]);
}

#[test]
fn enter_is_a_newline_or_the_editor_action() {
    let mut session = session();
    assert_eq!(
        session.press(Event::Enter, &ctx(0)).commands,
        [Command::SendEnterKey]
    );
    session.start_input(EnterAction::Send, false, true);
    assert_eq!(
        session.press(Event::Enter, &ctx(0)).commands,
        [Command::PerformEditorAction]
    );
    assert_eq!(session.state().enter_action, EnterAction::Send);
}

#[test]
fn emoji_press_commits_and_remembers_the_emoji() {
    let mut session = session();
    session.press(
        Event::Emoji {
            text: "😀".into()
        },
        &ctx(0),
    );
    let outcome = session.press(
        Event::Emoji {
            text: "👍".into()
        },
        &ctx(0),
    );
    assert_eq!(outcome.commands, [commit("👍")]);
    assert_eq!(session.recents(), ["👍", "😀"]);
    session.press(
        Event::Emoji {
            text: "😀".into()
        },
        &ctx(0),
    );
    assert_eq!(session.recents(), ["😀", "👍"]);
}

#[test]
fn recents_are_limited() {
    let mut session = session();
    for index in 0..40 {
        session.press(
            Event::Emoji {
                text: format!("e{index}"),
            },
            &ctx(0),
        );
    }
    assert_eq!(session.recents().len(), kotoba_emoji::RECENT_LIMIT);
    assert_eq!(session.recents()[0], "e39");
}

#[test]
fn recents_can_be_restored() {
    let mut session = session();
    session.set_recents(vec!["a".into(), "b".into()]);
    assert_eq!(session.recents(), ["a", "b"]);
}

#[test]
fn switching_to_an_unknown_layer_is_ignored() {
    let mut session = session();
    session.press(
        Event::SwitchLayer {
            layer_id: "nope".into(),
        },
        &ctx(0),
    );
    assert_eq!(session.state().layer_id, "letters");
    session.press(
        Event::SwitchLayer {
            layer_id: "emoji".into(),
        },
        &ctx(0),
    );
    assert_eq!(session.state().layer_id, "emoji");
    assert!(session.is_emoji_layer());
}

#[test]
fn selecting_another_layout_resets_layer_and_shift() {
    let mut session = session();
    session.press(
        Event::SwitchLayer {
            layer_id: "symbols".into(),
        },
        &ctx(0),
    );
    session.press(Event::Shift, &ctx(1_000));
    assert!(session.select_layout("ru"));
    let state = session.state();
    assert_eq!(
        (
            state.layout_id.as_str(),
            state.layer_id.as_str(),
            state.shift
        ),
        ("ru", "letters", ShiftState::Off)
    );
}

#[test]
fn selecting_the_current_layout_changes_nothing() {
    let mut session = session();
    session.press(
        Event::SwitchLayer {
            layer_id: "symbols".into(),
        },
        &ctx(0),
    );
    let id = session.state().layout_id;
    assert!(session.select_layout(&id));
    assert_eq!(session.state().layer_id, "symbols");
}

#[test]
fn selecting_an_unknown_layout_fails() {
    assert!(!session().select_layout("nope"));
}

#[test]
fn prefers_a_layout_of_a_language() {
    let mut session = session();
    assert!(session.prefer_language("RU"));
    assert_eq!(session.state().layout_id, "ru");
    assert!(session.prefer_language("tr"));
    assert!(session.state().layout_id.starts_with("tr-"));
    assert!(!session.prefer_language("xx"));
}

#[test]
fn replacing_layouts_keeps_the_current_one() {
    let mut session = session();
    session.select_layout("ru");
    session.press(
        Event::SwitchLayer {
            layer_id: "symbols".into(),
        },
        &ctx(0),
    );
    session
        .replace_layouts(layouts().into_iter().rev().collect())
        .unwrap();
    assert_eq!(session.state().layout_id, "ru");
    assert_eq!(session.state().layer_id, "symbols");
}

#[test]
fn replacing_layouts_falls_back_to_the_first_when_the_current_is_gone() {
    let mut session = session();
    session.select_layout("ru");
    let remaining: Vec<KeyboardLayout> = layouts().into_iter().filter(|l| l.id != "ru").collect();
    let first = remaining[0].id.clone();
    session.replace_layouts(remaining).unwrap();
    assert_eq!(session.state().layout_id, first);
}

#[test]
fn replacing_with_no_layouts_fails_and_keeps_the_old_ones() {
    let mut session = session();
    assert_eq!(
        session.replace_layouts(Vec::new()).unwrap_err(),
        SessionError::NoLayouts
    );
    assert!(!session.layouts().is_empty());
}

#[test]
fn start_input_resets_unless_restarting() {
    let mut session = session();
    session.press(
        Event::SwitchLayer {
            layer_id: "symbols".into(),
        },
        &ctx(0),
    );
    session.press(Event::Shift, &ctx(1_000));
    session.start_input(EnterAction::Search, false, true);
    assert_eq!(session.state().layer_id, "symbols");
    assert_eq!(session.state().shift, ShiftState::Once);
    session.start_input(EnterAction::Search, false, false);
    assert_eq!(session.state().layer_id, "letters");
    assert_eq!(session.state().shift, ShiftState::Off);
}

#[test]
fn needs_tells_what_the_platform_must_supply() {
    let session = session();
    assert_eq!(
        session.needs(&Event::Space),
        Needs {
            text_before: true,
            selection: false
        }
    );
    assert_eq!(
        session.needs(&Event::Delete),
        Needs {
            text_before: false,
            selection: true
        }
    );
    assert_eq!(session.needs(&Event::Shift), Needs::default());
    assert_eq!(
        session.needs(&Event::Char { text: "a".into() }),
        Needs::default()
    );
}
