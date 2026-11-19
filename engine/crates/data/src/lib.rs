#![forbid(unsafe_code)]

mod generated {
    include!(concat!(env!("OUT_DIR"), "/layouts.rs"));
}

pub fn layouts() -> &'static [(&'static str, &'static str)] {
    generated::LAYOUTS
}

pub fn emoji_json() -> &'static str {
    include_str!("../../../data/emoji/emoji.json")
}
