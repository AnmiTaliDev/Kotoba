#[derive(Debug, Clone, uniffi::Record)]
pub struct LayoutSource {
    pub origin: String,
    pub text: String,
    pub built_in: bool,
}

#[derive(Debug, Clone, uniffi::Record)]
pub struct LoadedSource {
    pub layout_id: String,
    pub origin: String,
    pub text: String,
    pub built_in: bool,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, uniffi::Enum)]
pub enum Severity {
    Error,
    Warning,
}

#[derive(Debug, Clone, uniffi::Record)]
pub struct LayoutIssue {
    pub path: String,
    pub message: String,
    pub severity: Severity,
}

#[derive(Debug, Clone, uniffi::Record)]
pub struct LayoutFailure {
    pub origin: String,
    pub issues: Vec<LayoutIssue>,
}

#[derive(Debug, Clone, uniffi::Record)]
pub struct KeyboardLayout {
    pub id: String,
    pub name: String,
    pub locale: String,
    pub default_layer: String,
    pub sentence_end: String,
    pub double_space: String,
    pub auto_capitalize: bool,
    pub order: i32,
    pub number_row: Vec<Key>,
    pub layers: Vec<Layer>,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, uniffi::Enum)]
pub enum LayerKind {
    Keys,
    Emoji,
}

#[derive(Debug, Clone, uniffi::Record)]
pub struct Layer {
    pub id: String,
    pub kind: LayerKind,
    pub shiftable: bool,
    pub number_row: bool,
    pub rows: Vec<Vec<Key>>,
    pub back: Option<LayerLink>,
}

#[derive(Debug, Clone, uniffi::Record)]
pub struct LayerLink {
    pub label: String,
    pub target: String,
}

#[derive(Debug, Clone, uniffi::Enum)]
pub enum Key {
    Char {
        label: String,
        shifted: String,
        weight: f32,
        hold: Vec<String>,
        hold_shifted: Vec<String>,
    },
    Shift {
        weight: f32,
    },
    Delete {
        weight: f32,
    },
    Enter {
        weight: f32,
    },
    Space {
        weight: f32,
    },
    Spacer {
        weight: f32,
    },
    Layer {
        label: String,
        target: String,
        icon: Option<String>,
        weight: f32,
    },
    Language {
        label: String,
        icon: Option<String>,
        weight: f32,
    },
}

#[derive(Debug, Clone, uniffi::Enum)]
pub enum ImportResult {
    Valid {
        layout: KeyboardLayout,
        warnings: Vec<LayoutIssue>,
    },
    Invalid {
        issues: Vec<LayoutIssue>,
    },
}

#[derive(Debug, Clone, uniffi::Record)]
pub struct EmojiCategory {
    pub id: String,
    pub name: String,
    pub icon: String,
    pub emoji: Vec<String>,
    pub recent: bool,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, uniffi::Enum)]
pub enum ShiftState {
    Off,
    Once,
    Caps,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, uniffi::Enum)]
pub enum EnterAction {
    Newline,
    Go,
    Search,
    Send,
    Next,
    Done,
}

#[derive(Debug, Clone, uniffi::Enum)]
pub enum Event {
    Char { text: String },
    Emoji { text: String },
    Space,
    Delete,
    Enter,
    Shift,
    SwitchLayer { layer_id: String },
    SwitchLanguage,
}

#[derive(Debug, Clone, uniffi::Record)]
pub struct Context {
    pub now_ms: i64,
    pub text_before: String,
    pub has_selection: bool,
}

#[derive(Debug, Clone, uniffi::Record)]
pub struct Needs {
    pub text_before: bool,
    pub selection: bool,
}

#[derive(Debug, Clone, uniffi::Enum)]
pub enum Command {
    Commit { text: String },
    DeleteBefore { code_points: i32 },
    BeginBatch,
    EndBatch,
    SendEnterKey,
    PerformEditorAction,
    SwitchLanguage,
}

#[derive(Debug, Clone, uniffi::Record)]
pub struct Outcome {
    pub commands: Vec<Command>,
    pub needs_text_after_edit: bool,
}

#[derive(Debug, Clone, uniffi::Record)]
pub struct SessionState {
    pub layout_id: String,
    pub layer_id: String,
    pub shift: ShiftState,
    pub shift_applies: bool,
    pub enter_action: EnterAction,
}
