use kotoba_emoji::{RECENT_LIMIT, add_recent};
use kotoba_input::{ShiftState, shift_after_edit, space_edit, tap_shift};
use kotoba_layout::{KeyboardLayout, LayerKind};

#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub enum EnterAction {
    #[default]
    Newline,
    Go,
    Search,
    Send,
    Next,
    Done,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum Event {
    Char { text: String },
    Emoji { text: String },
    Space,
    Delete,
    Enter,
    Shift,
    SwitchLayer { layer_id: String },
}

#[derive(Debug, Clone, PartialEq, Eq, Default)]
pub struct Context {
    pub now_ms: u64,
    pub text_before: String,
    pub has_selection: bool,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub struct Needs {
    pub text_before: bool,
    pub selection: bool,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum Command {
    Commit { text: String },
    DeleteBefore { code_points: u32 },
    BeginBatch,
    EndBatch,
    SendEnterKey,
    PerformEditorAction,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Outcome {
    pub commands: Vec<Command>,
    pub needs_text_after_edit: bool,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct SessionState {
    pub layout_id: String,
    pub layer_id: String,
    pub shift: ShiftState,
    pub shift_applies: bool,
    pub enter_action: EnterAction,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum SessionError {
    NoLayouts,
}

impl std::fmt::Display for SessionError {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            SessionError::NoLayouts => f.write_str("a session needs at least one layout"),
        }
    }
}

impl std::error::Error for SessionError {}

#[derive(Debug, Clone)]
pub struct Session {
    layouts: Vec<KeyboardLayout>,
    layout_index: usize,
    layer_id: String,
    shift: ShiftState,
    last_shift_tap_ms: Option<u64>,
    enter_action: EnterAction,
    field_auto_capitalize: bool,
    recents: Vec<String>,
}

impl Session {
    pub fn new(layouts: Vec<KeyboardLayout>) -> Result<Self, SessionError> {
        let first = layouts.first().ok_or(SessionError::NoLayouts)?;
        let layer_id = first.default_layer.clone();
        Ok(Self {
            layouts,
            layout_index: 0,
            layer_id,
            shift: ShiftState::Off,
            last_shift_tap_ms: None,
            enter_action: EnterAction::Newline,
            field_auto_capitalize: false,
            recents: Vec::new(),
        })
    }

    pub fn layouts(&self) -> &[KeyboardLayout] {
        &self.layouts
    }

    pub fn layout(&self) -> &KeyboardLayout {
        &self.layouts[self.layout_index]
    }

    pub fn state(&self) -> SessionState {
        let layout = self.layout();
        let shiftable = layout
            .layer(&self.layer_id)
            .is_some_and(|layer| layer.shiftable);
        SessionState {
            layout_id: layout.id.clone(),
            layer_id: self.layer_id.clone(),
            shift: self.shift,
            shift_applies: shiftable && self.shift != ShiftState::Off,
            enter_action: self.enter_action,
        }
    }

    pub fn recents(&self) -> &[String] {
        &self.recents
    }

    pub fn set_recents(&mut self, recents: Vec<String>) {
        self.recents = recents;
    }

    pub fn replace_layouts(&mut self, layouts: Vec<KeyboardLayout>) -> Result<(), SessionError> {
        if layouts.is_empty() {
            return Err(SessionError::NoLayouts);
        }
        let current = self.layout().id.clone();
        self.layouts = layouts;
        self.layout_index = self
            .layouts
            .iter()
            .position(|l| l.id == current)
            .unwrap_or(0);
        if self.layout().layer(&self.layer_id).is_none() {
            self.layer_id = self.layout().default_layer.clone();
        }
        Ok(())
    }

    pub fn select_layout(&mut self, id: &str) -> bool {
        let Some(index) = self.layouts.iter().position(|l| l.id == id) else {
            return false;
        };
        if index != self.layout_index {
            self.layout_index = index;
            self.layer_id = self.layout().default_layer.clone();
            self.shift = ShiftState::Off;
        }
        true
    }

    pub fn prefer_language(&mut self, language: &str) -> bool {
        let language = language.to_ascii_lowercase();
        let found = self
            .layouts
            .iter()
            .find(|l| l.language() == language)
            .map(|l| l.id.clone());
        found.is_some_and(|id| self.select_layout(&id))
    }

    pub fn start_input(
        &mut self,
        enter_action: EnterAction,
        auto_capitalize: bool,
        restarting: bool,
    ) {
        if !restarting {
            self.layer_id = self.layout().default_layer.clone();
            self.shift = ShiftState::Off;
        }
        self.enter_action = enter_action;
        self.field_auto_capitalize = auto_capitalize;
    }

    fn auto_capitalize_enabled(&self) -> bool {
        self.field_auto_capitalize && self.layout().auto_capitalize
    }

    pub fn needs_text_after_edit(&self) -> bool {
        self.auto_capitalize_enabled() && self.shift != ShiftState::Caps
    }

    pub fn needs(&self, event: &Event) -> Needs {
        match event {
            Event::Space => Needs {
                text_before: true,
                selection: false,
            },
            Event::Delete => Needs {
                text_before: false,
                selection: true,
            },
            _ => Needs::default(),
        }
    }

    pub fn press(&mut self, event: Event, context: &Context) -> Outcome {
        let commands = match event {
            Event::Char { text } => vec![Command::Commit { text }],
            Event::Emoji { text } => {
                self.recents = add_recent(&self.recents, &text, RECENT_LIMIT);
                vec![Command::Commit { text }]
            }
            Event::Delete => {
                if context.has_selection {
                    vec![Command::Commit {
                        text: String::new(),
                    }]
                } else {
                    vec![Command::DeleteBefore { code_points: 1 }]
                }
            }
            Event::Space => {
                let edit = space_edit(&context.text_before, &self.layout().double_space);
                let mut commands = vec![Command::BeginBatch];
                if edit.delete_before > 0 {
                    commands.push(Command::DeleteBefore {
                        code_points: edit.delete_before as u32,
                    });
                }
                commands.push(Command::Commit { text: edit.insert });
                commands.push(Command::EndBatch);
                commands
            }
            Event::Enter => {
                if self.enter_action == EnterAction::Newline {
                    vec![Command::SendEnterKey]
                } else {
                    vec![Command::PerformEditorAction]
                }
            }
            Event::Shift => {
                let since = self
                    .last_shift_tap_ms
                    .map_or(u64::MAX, |last| context.now_ms.saturating_sub(last));
                self.shift = tap_shift(self.shift, since);
                self.last_shift_tap_ms = Some(context.now_ms);
                return Outcome {
                    commands: Vec::new(),
                    needs_text_after_edit: false,
                };
            }
            Event::SwitchLayer { layer_id } => {
                if self.layout().layer(&layer_id).is_some() {
                    self.layer_id = layer_id;
                }
                return Outcome {
                    commands: Vec::new(),
                    needs_text_after_edit: false,
                };
            }
        };
        let needs_text = self.needs_text_after_edit();
        if !needs_text {
            let sentence_end = self.layout().sentence_end.clone();
            self.shift = shift_after_edit(self.shift, "", &sentence_end, false);
        }
        Outcome {
            commands,
            needs_text_after_edit: needs_text,
        }
    }

    pub fn after_edit(&mut self, text_before: &str) {
        let sentence_end = self.layout().sentence_end.clone();
        self.shift = shift_after_edit(
            self.shift,
            text_before,
            &sentence_end,
            self.auto_capitalize_enabled(),
        );
    }

    pub fn is_emoji_layer(&self) -> bool {
        self.layout()
            .layer(&self.layer_id)
            .is_some_and(|layer| layer.kind == LayerKind::Emoji)
    }
}
