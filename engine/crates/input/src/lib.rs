#![forbid(unsafe_code)]

mod shift;
mod space;

pub use shift::{DOUBLE_TAP_MS, ShiftState, shift_after_edit, tap_shift};
pub use space::{SpaceEdit, space_edit};
