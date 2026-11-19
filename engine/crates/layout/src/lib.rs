#![forbid(unsafe_code)]

mod case;
mod import;
mod issue;
mod loader;
mod locale;
mod model;
mod parser;

pub use case::to_upper;
pub use import::{CANDIDATE_ORIGIN, ImportResult, check_import, too_large_issue, user_origin};
pub use issue::{LayoutIssue, Severity};
pub use loader::{LayoutFailure, LayoutSource, LoadedLayouts, MAX_FILE_BYTES, load};
pub use locale::{LanguageTag, parse_language_tag};
pub use model::{CharKey, Key, KeyboardLayout, Layer, LayerKey, LayerKind, LayerLink};
pub use parser::{
    DEFAULT_ORDER, MAX_HOLD, MAX_KEY_WEIGHT, MAX_KEYS, MAX_LABEL_LENGTH, MAX_ROW_WEIGHT,
    ParseResult, SUPPORTED_FORMAT_VERSION, parse,
};
