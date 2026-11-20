use kotoba_emoji as emoji;
use kotoba_input as input;
use kotoba_layout as layout;
use kotoba_session as session;

use crate::types::*;

impl From<LayoutSource> for layout::LayoutSource {
    fn from(source: LayoutSource) -> Self {
        Self {
            origin: source.origin,
            text: source.text,
            built_in: source.built_in,
        }
    }
}

impl From<layout::Severity> for Severity {
    fn from(severity: layout::Severity) -> Self {
        match severity {
            layout::Severity::Error => Severity::Error,
            layout::Severity::Warning => Severity::Warning,
        }
    }
}

impl From<layout::LayoutIssue> for LayoutIssue {
    fn from(issue: layout::LayoutIssue) -> Self {
        Self {
            path: issue.path,
            message: issue.message,
            severity: issue.severity.into(),
        }
    }
}

impl From<&layout::LayoutFailure> for LayoutFailure {
    fn from(failure: &layout::LayoutFailure) -> Self {
        Self {
            origin: failure.origin.clone(),
            issues: failure.issues.iter().cloned().map(Into::into).collect(),
        }
    }
}

impl From<&layout::Key> for Key {
    fn from(key: &layout::Key) -> Self {
        match key {
            layout::Key::Char(key) => Key::Char {
                label: key.label.clone(),
                shifted: key.shifted.clone(),
                weight: key.weight,
                hold: key.hold.clone(),
                hold_shifted: key.hold_shifted.clone(),
            },
            layout::Key::Shift { weight } => Key::Shift { weight: *weight },
            layout::Key::Delete { weight } => Key::Delete { weight: *weight },
            layout::Key::Enter { weight } => Key::Enter { weight: *weight },
            layout::Key::Space { weight } => Key::Space { weight: *weight },
            layout::Key::Spacer { weight } => Key::Spacer { weight: *weight },
            layout::Key::Language(key) => Key::Language {
                label: key.label.clone(),
                icon: key.icon.clone(),
                weight: key.weight,
            },
            layout::Key::Layer(key) => Key::Layer {
                label: key.label.clone(),
                target: key.target.clone(),
                icon: key.icon.clone(),
                weight: key.weight,
            },
        }
    }
}

impl From<&layout::Layer> for Layer {
    fn from(layer: &layout::Layer) -> Self {
        Self {
            id: layer.id.clone(),
            label: layer.label.clone(),
            kind: match layer.kind {
                layout::LayerKind::Keys => LayerKind::Keys,
                layout::LayerKind::Emoji => LayerKind::Emoji,
            },
            shiftable: layer.shiftable,
            number_row: layer.number_row,
            rows: layer
                .rows
                .iter()
                .map(|row| row.iter().map(Into::into).collect())
                .collect(),
            back: layer.back.as_ref().map(|back| LayerLink {
                label: back.label.clone(),
                target: back.target.clone(),
            }),
        }
    }
}

impl From<&layout::KeyboardLayout> for KeyboardLayout {
    fn from(layout: &layout::KeyboardLayout) -> Self {
        Self {
            id: layout.id.clone(),
            name: layout.name.clone(),
            locale: layout.locale.clone(),
            default_layer: layout.default_layer.clone(),
            sentence_end: layout.sentence_end.clone(),
            double_space: layout.double_space.clone(),
            auto_capitalize: layout.auto_capitalize,
            order: layout.order,
            number_row: layout.number_row.iter().map(Into::into).collect(),
            layers: layout.layers.iter().map(Into::into).collect(),
        }
    }
}

impl From<layout::ImportResult> for ImportResult {
    fn from(result: layout::ImportResult) -> Self {
        match result {
            layout::ImportResult::Valid { layout, warnings } => ImportResult::Valid {
                layout: (&layout).into(),
                warnings: warnings.into_iter().map(Into::into).collect(),
            },
            layout::ImportResult::Invalid { issues } => ImportResult::Invalid {
                issues: issues.into_iter().map(Into::into).collect(),
            },
        }
    }
}

impl From<emoji::EmojiCategory> for EmojiCategory {
    fn from(category: emoji::EmojiCategory) -> Self {
        Self {
            id: category.id,
            name: category.name,
            icon: category.icon,
            emoji: category.emoji,
            recent: category.recent,
        }
    }
}

impl From<input::ShiftState> for ShiftState {
    fn from(shift: input::ShiftState) -> Self {
        match shift {
            input::ShiftState::Off => ShiftState::Off,
            input::ShiftState::Once => ShiftState::Once,
            input::ShiftState::Caps => ShiftState::Caps,
        }
    }
}

impl From<session::EnterAction> for EnterAction {
    fn from(action: session::EnterAction) -> Self {
        match action {
            session::EnterAction::Newline => EnterAction::Newline,
            session::EnterAction::Go => EnterAction::Go,
            session::EnterAction::Search => EnterAction::Search,
            session::EnterAction::Send => EnterAction::Send,
            session::EnterAction::Next => EnterAction::Next,
            session::EnterAction::Done => EnterAction::Done,
        }
    }
}

impl From<EnterAction> for session::EnterAction {
    fn from(action: EnterAction) -> Self {
        match action {
            EnterAction::Newline => session::EnterAction::Newline,
            EnterAction::Go => session::EnterAction::Go,
            EnterAction::Search => session::EnterAction::Search,
            EnterAction::Send => session::EnterAction::Send,
            EnterAction::Next => session::EnterAction::Next,
            EnterAction::Done => session::EnterAction::Done,
        }
    }
}

impl From<Event> for session::Event {
    fn from(event: Event) -> Self {
        match event {
            Event::Char { text } => session::Event::Char { text },
            Event::Emoji { text } => session::Event::Emoji { text },
            Event::Space => session::Event::Space,
            Event::Delete => session::Event::Delete,
            Event::Enter => session::Event::Enter,
            Event::Shift => session::Event::Shift,
            Event::SwitchLayer { layer_id } => session::Event::SwitchLayer { layer_id },
            Event::SwitchLanguage => session::Event::SwitchLanguage,
        }
    }
}

impl From<Context> for session::Context {
    fn from(context: Context) -> Self {
        Self {
            now_ms: u64::try_from(context.now_ms).unwrap_or(0),
            text_before: context.text_before,
            has_selection: context.has_selection,
        }
    }
}

impl From<session::Needs> for Needs {
    fn from(needs: session::Needs) -> Self {
        Self {
            text_before: needs.text_before,
            selection: needs.selection,
        }
    }
}

impl From<session::Command> for Command {
    fn from(command: session::Command) -> Self {
        match command {
            session::Command::Commit { text } => Command::Commit { text },
            session::Command::DeleteBefore { code_points } => Command::DeleteBefore {
                code_points: i32::try_from(code_points).unwrap_or(i32::MAX),
            },
            session::Command::BeginBatch => Command::BeginBatch,
            session::Command::EndBatch => Command::EndBatch,
            session::Command::SendEnterKey => Command::SendEnterKey,
            session::Command::PerformEditorAction => Command::PerformEditorAction,
            session::Command::SwitchLanguage => Command::SwitchLanguage,
        }
    }
}

impl From<session::Outcome> for Outcome {
    fn from(outcome: session::Outcome) -> Self {
        Self {
            commands: outcome.commands.into_iter().map(Into::into).collect(),
            needs_text_after_edit: outcome.needs_text_after_edit,
        }
    }
}

impl From<session::SessionState> for SessionState {
    fn from(state: session::SessionState) -> Self {
        Self {
            layout_id: state.layout_id,
            layer_id: state.layer_id,
            shift: state.shift.into(),
            shift_applies: state.shift_applies,
            enter_action: state.enter_action.into(),
        }
    }
}
