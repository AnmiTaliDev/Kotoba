mod convert;
mod types;

use std::sync::{Arc, Mutex, MutexGuard};

use kotoba_layout as layout;
use kotoba_session as session;

pub use types::*;

uniffi::setup_scaffolding!();

#[derive(Debug, uniffi::Error)]
#[uniffi(flat_error)]
pub enum EngineError {
    NoLayouts(String),
}

impl std::fmt::Display for EngineError {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            EngineError::NoLayouts(details) => write!(f, "no valid keyboard layouts: {details}"),
        }
    }
}

impl std::error::Error for EngineError {}

struct Inner {
    session: session::Session,
    loaded: layout::LoadedLayouts,
}

fn describe(loaded: &layout::LoadedLayouts) -> String {
    loaded
        .failures
        .iter()
        .map(|failure| {
            let issues: Vec<String> = failure
                .issues
                .iter()
                .map(|issue| format!("{}: {}", issue.path, issue.message))
                .collect();
            format!("{}: {}", failure.origin, issues.join("; "))
        })
        .collect::<Vec<_>>()
        .join(" | ")
}

fn load_sources(sources: Vec<LayoutSource>) -> layout::LoadedLayouts {
    let sources: Vec<layout::LayoutSource> = sources.into_iter().map(Into::into).collect();
    layout::load(&sources)
}

#[derive(uniffi::Object)]
pub struct Engine {
    inner: Mutex<Inner>,
}

impl Engine {
    fn lock(&self) -> MutexGuard<'_, Inner> {
        self.inner
            .lock()
            .unwrap_or_else(|poisoned| poisoned.into_inner())
    }
}

#[uniffi::export]
impl Engine {
    #[uniffi::constructor]
    pub fn new(sources: Vec<LayoutSource>) -> Result<Arc<Self>, EngineError> {
        let loaded = load_sources(sources);
        let session = session::Session::new(loaded.layouts.clone())
            .map_err(|_| EngineError::NoLayouts(describe(&loaded)))?;
        Ok(Arc::new(Self {
            inner: Mutex::new(Inner { session, loaded }),
        }))
    }

    pub fn reload(&self, sources: Vec<LayoutSource>) -> Result<(), EngineError> {
        let loaded = load_sources(sources);
        let mut inner = self.lock();
        inner
            .session
            .replace_layouts(loaded.layouts.clone())
            .map_err(|_| EngineError::NoLayouts(describe(&loaded)))?;
        inner.loaded = loaded;
        Ok(())
    }

    pub fn layouts(&self) -> Vec<KeyboardLayout> {
        self.lock().loaded.layouts.iter().map(Into::into).collect()
    }

    pub fn failures(&self) -> Vec<LayoutFailure> {
        self.lock().loaded.failures.iter().map(Into::into).collect()
    }

    pub fn warnings(&self) -> Vec<LayoutFailure> {
        self.lock().loaded.warnings.iter().map(Into::into).collect()
    }

    pub fn sources(&self) -> Vec<LoadedSource> {
        self.lock()
            .loaded
            .sources
            .iter()
            .map(|(id, source)| LoadedSource {
                layout_id: id.clone(),
                origin: source.origin.clone(),
                text: source.text.clone(),
                built_in: source.built_in,
            })
            .collect()
    }

    pub fn state(&self) -> SessionState {
        self.lock().session.state().into()
    }

    pub fn select_layout(&self, id: String) -> bool {
        self.lock().session.select_layout(&id)
    }

    pub fn prefer_language(&self, language: String) -> bool {
        self.lock().session.prefer_language(&language)
    }

    pub fn start_input(&self, enter_action: EnterAction, auto_capitalize: bool, restarting: bool) {
        self.lock()
            .session
            .start_input(enter_action.into(), auto_capitalize, restarting);
    }

    pub fn needs(&self, event: Event) -> Needs {
        self.lock().session.needs(&event.into()).into()
    }

    pub fn press(&self, event: Event, context: Context) -> Outcome {
        self.lock()
            .session
            .press(event.into(), &context.into())
            .into()
    }

    pub fn needs_text_after_edit(&self) -> bool {
        self.lock().session.needs_text_after_edit()
    }

    pub fn after_edit(&self, text_before: String) {
        self.lock().session.after_edit(&text_before);
    }

    pub fn recents(&self) -> Vec<String> {
        self.lock().session.recents().to_vec()
    }

    pub fn set_recents(&self, recents: Vec<String>) {
        self.lock().session.set_recents(recents);
    }
}

#[uniffi::export]
pub fn built_in_sources() -> Vec<LayoutSource> {
    kotoba_data::layouts()
        .iter()
        .map(|(name, text)| LayoutSource {
            origin: format!("layouts/{name}"),
            text: (*text).to_string(),
            built_in: true,
        })
        .collect()
}

#[uniffi::export]
pub fn check_import(
    text: String,
    built_in: Vec<LayoutSource>,
    user: Vec<LayoutSource>,
) -> ImportResult {
    let built_in: Vec<layout::LayoutSource> = built_in.into_iter().map(Into::into).collect();
    let user: Vec<layout::LayoutSource> = user.into_iter().map(Into::into).collect();
    layout::check_import(&text, &built_in, &user).into()
}

#[uniffi::export]
pub fn import_too_large_issue() -> LayoutIssue {
    layout::too_large_issue().into()
}

#[uniffi::export]
pub fn max_layout_file_bytes() -> i32 {
    layout::MAX_FILE_BYTES as i32
}

#[uniffi::export]
pub fn user_layout_origin(id: String) -> String {
    layout::user_origin(&id)
}

#[uniffi::export]
pub fn emoji_categories() -> Vec<EmojiCategory> {
    kotoba_emoji::parse_emoji_data(kotoba_data::emoji_json())
        .expect("built-in emoji data is valid")
        .into_iter()
        .map(Into::into)
        .collect()
}

#[uniffi::export]
pub fn encode_recent_emoji(recents: Vec<String>) -> String {
    kotoba_emoji::encode_recents(&recents)
}

#[uniffi::export]
pub fn decode_recent_emoji(stored: String) -> Vec<String> {
    kotoba_emoji::decode_recents(&stored)
}
