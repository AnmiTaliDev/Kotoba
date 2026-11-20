#[derive(Debug, Clone, PartialEq)]
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

impl KeyboardLayout {
    pub fn layer(&self, id: &str) -> Option<&Layer> {
        self.layers.iter().find(|layer| layer.id == id)
    }

    pub fn default_layer(&self) -> &Layer {
        self.layer(&self.default_layer)
            .expect("a parsed layout always has its default layer")
    }

    pub fn language(&self) -> String {
        crate::parse_language_tag(&self.locale)
            .map(|tag| tag.language)
            .unwrap_or_default()
    }
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum LayerKind {
    Keys,
    Emoji,
}

#[derive(Debug, Clone, PartialEq)]
pub struct Layer {
    pub id: String,
    pub kind: LayerKind,
    pub shiftable: bool,
    pub number_row: bool,
    pub rows: Vec<Vec<Key>>,
    pub back: Option<LayerLink>,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct LayerLink {
    pub label: String,
    pub target: String,
}

#[derive(Debug, Clone, PartialEq)]
pub enum Key {
    Char(CharKey),
    Shift { weight: f32 },
    Delete { weight: f32 },
    Enter { weight: f32 },
    Space { weight: f32 },
    Spacer { weight: f32 },
    Layer(LayerKey),
    Language(LanguageKey),
}

impl Key {
    pub fn weight(&self) -> f32 {
        match self {
            Key::Char(key) => key.weight,
            Key::Layer(key) => key.weight,
            Key::Language(key) => key.weight,
            Key::Shift { weight }
            | Key::Delete { weight }
            | Key::Enter { weight }
            | Key::Space { weight }
            | Key::Spacer { weight } => *weight,
        }
    }
}

#[derive(Debug, Clone, PartialEq)]
pub struct CharKey {
    pub label: String,
    pub shifted: String,
    pub weight: f32,
    pub hold: Vec<String>,
    pub hold_shifted: Vec<String>,
}

impl CharKey {
    pub fn text(&self, shifted: bool) -> &str {
        if shifted { &self.shifted } else { &self.label }
    }

    pub fn alternatives(&self, shifted: bool) -> &[String] {
        if shifted {
            &self.hold_shifted
        } else {
            &self.hold
        }
    }
}

#[derive(Debug, Clone, PartialEq)]
pub struct LayerKey {
    pub label: String,
    pub target: String,
    pub icon: Option<String>,
    pub weight: f32,
}

#[derive(Debug, Clone, PartialEq)]
pub struct LanguageKey {
    pub label: String,
    pub icon: Option<String>,
    pub weight: f32,
}
