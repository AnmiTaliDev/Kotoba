use std::collections::{BTreeSet, HashMap};

use kotoba_layout::{CharKey, Key, KeyboardLayout, LayoutSource, LoadedLayouts, load};

const APOSTROPHE: &str = "\u{2bc}";

fn loaded() -> LoadedLayouts {
    let sources: Vec<LayoutSource> = kotoba_data::layouts()
        .iter()
        .map(|(name, text)| LayoutSource {
            origin: (*name).into(),
            text: (*text).into(),
            built_in: true,
        })
        .collect();
    load(&sources)
}

fn layouts() -> HashMap<String, KeyboardLayout> {
    loaded()
        .layouts
        .into_iter()
        .map(|l| (l.id.clone(), l))
        .collect()
}

fn char_keys(layout: &KeyboardLayout, layer: &str) -> Vec<CharKey> {
    layout
        .layer(layer)
        .unwrap()
        .rows
        .iter()
        .flatten()
        .filter_map(|key| {
            if let Key::Char(key) = key {
                Some(key.clone())
            } else {
                None
            }
        })
        .collect()
}

fn typable(layout: &KeyboardLayout) -> BTreeSet<String> {
    char_keys(layout, &layout.default_layer)
        .into_iter()
        .flat_map(|key| std::iter::once(key.label).chain(key.hold))
        .collect()
}

fn letter_labels(layout: &KeyboardLayout, layer: &str) -> Vec<String> {
    char_keys(layout, layer)
        .into_iter()
        .map(|k| k.label)
        .filter(|l| l.chars().next().is_some_and(char::is_alphabetic))
        .collect()
}

fn alphabets() -> Vec<(&'static str, &'static str)> {
    vec![
        ("latin", "abcdefghijklmnopqrstuvwxyz"),
        ("ru", "абвгдеёжзийклмнопрстуфхцчшщъыьэюя"),
        ("uk", "абвгґдеєжзиіїйклмнопрстуфхцчшщьюяʼ"),
        ("be", "абвгдеёжзійклмнопрстуўфхцчшыьэюя'"),
        ("kk", "абвгғдеёжзийкқлмнңоөпрстуұүфхһцчшщъыіьэюяә"),
        ("kk-compact", "абвгғдеёжзийкқлмнңоөпрстуұүфхһцчшщъыіьэюяә"),
        ("kk-anmitalidev", "аәбгғдежзийкқлмнңоөпрстуұүхһшыі"),
        ("kk-latn-anmitalidev", "aäbgğdejzikqlmnñoöprstuūühşyı"),
        ("kk-latn-2021", "aäbdefgğhıijklmnñoöpqrsştuūüvyz"),
        ("tr-q", "abcçdefgğhıijklmnoöprsştuüvyzqwx"),
        ("tr-f", "abcçdefgğhıijklmnoöprsştuüvyzqwx"),
        ("de", "abcdefghijklmnopqrstuvwxyzäöüß"),
    ]
}

fn token_alphabets() -> Vec<(&'static str, Vec<String>)> {
    let a = APOSTROPHE;
    let tokens = [
        "a", "a'", "b", "d", "e", "f", "g", "g'", "h", "i", "i'", "j", "k", "l", "m", "n", "n'",
        "o", "o'", "p", "q", "r", "s", "s'", "t", "u", "u'", "v", "y", "y'", "z", "c'",
    ];
    vec![(
        "kk-latn-2017",
        tokens.iter().map(|t| t.replace('\'', a)).collect(),
    )]
}

#[test]
fn every_built_in_layout_loads_without_errors_or_warnings() {
    let loaded = loaded();
    assert!(loaded.failures.is_empty(), "{:?}", loaded.failures);
    assert!(loaded.warnings.is_empty(), "{:?}", loaded.warnings);
    assert_eq!(loaded.layouts.len(), kotoba_data::layouts().len());
}

#[test]
fn every_built_in_layout_keeps_shared_layers() {
    for layout in loaded().layouts {
        for id in ["letters", "symbols", "symbols2", "emoji"] {
            assert!(
                layout.layer(id).is_some(),
                "{} misses layer {id}",
                layout.id
            );
        }
    }
}

#[test]
fn layout_ids_are_unique_and_ordered() {
    let loaded = loaded();
    let ids: Vec<&str> = loaded.layouts.iter().map(|l| l.id.as_str()).collect();
    let unique: BTreeSet<&str> = ids.iter().copied().collect();
    assert_eq!(ids.len(), unique.len());
    let mut sorted = loaded.layouts.clone();
    sorted.sort_by(|a, b| a.order.cmp(&b.order).then_with(|| a.name.cmp(&b.name)));
    assert_eq!(
        ids,
        sorted.iter().map(|l| l.id.as_str()).collect::<Vec<_>>()
    );
}

#[test]
fn letter_keys_are_not_duplicated() {
    for layout in loaded().layouts {
        let labels: Vec<String> = char_keys(&layout, &layout.default_layer)
            .into_iter()
            .map(|k| k.label)
            .collect();
        let unique: BTreeSet<&String> = labels.iter().collect();
        assert_eq!(
            labels.len(),
            unique.len(),
            "{} has duplicated keys: {labels:?}",
            layout.id
        );
    }
}

#[test]
fn layouts_cover_their_alphabets() {
    let layouts = layouts();
    for (id, alphabet) in alphabets() {
        let layout = layouts
            .get(id)
            .unwrap_or_else(|| panic!("layout {id} is missing"));
        let typable = typable(layout);
        let missing: Vec<String> = alphabet
            .chars()
            .map(String::from)
            .filter(|letter| !typable.contains(letter))
            .collect();
        assert!(missing.is_empty(), "{id} cannot type: {missing:?}");
    }
}

#[test]
fn every_built_in_layout_has_an_alphabet_check() {
    let mut known: BTreeSet<String> = alphabets().iter().map(|(id, _)| id.to_string()).collect();
    known.extend(token_alphabets().iter().map(|(id, _)| id.to_string()));
    let ids: BTreeSet<String> = loaded().layouts.into_iter().map(|l| l.id).collect();
    assert_eq!(ids, known);
}

#[test]
fn kazakh_full_layout_uppercases_its_extra_row() {
    let layouts = layouts();
    let row: Vec<String> = char_keys(&layouts["kk"], "letters")[..9]
        .iter()
        .map(|k| k.shifted.clone())
        .collect();
    assert_eq!(row, ["Ә", "І", "Ң", "Ғ", "Ү", "Ұ", "Қ", "Ө", "Һ"]);
}

#[test]
fn cyrillic_layouts_contain_no_latin_lookalikes() {
    let layouts = layouts();
    for id in ["ru", "uk", "be", "kk", "kk-compact", "kk-anmitalidev"] {
        let foreign: Vec<char> = typable(&layouts[id])
            .iter()
            .flat_map(|s| s.chars())
            .filter(|c| c.is_alphabetic() && *c != 'ʼ' && !('\u{400}'..='\u{52f}').contains(c))
            .collect();
        assert!(
            foreign.is_empty(),
            "{id} contains non-Cyrillic letters: {foreign:?}"
        );
    }
}

#[test]
fn compact_kazakh_layout_keeps_four_rows_and_uses_long_press() {
    let layouts = layouts();
    let layout = &layouts["kk-compact"];
    assert_eq!(layout.layer("letters").unwrap().rows.len(), 4);
    let plain: Vec<String> = char_keys(layout, "letters")
        .into_iter()
        .map(|k| k.label)
        .collect();
    for letter in "әғқңөүұһі".chars() {
        assert!(
            !plain.contains(&letter.to_string()),
            "{letter} must not be a plain key"
        );
    }
}

#[test]
fn reduced_kazakh_layout_has_exactly_its_alphabet() {
    let layouts = layouts();
    let layout = &layouts["kk-anmitalidev"];
    let keys = char_keys(layout, "letters");
    let letters = letter_labels(layout, "letters");
    assert_eq!(letters.len(), 31);
    assert_eq!(letters.iter().collect::<BTreeSet<_>>().len(), 31);
    assert!(keys.iter().all(|k| k.hold.is_empty()));
    for letter in "вёцчщъьэюяф".chars() {
        assert!(
            !letters.contains(&letter.to_string()),
            "{letter} must not be typable"
        );
    }
}

#[test]
fn official_latin_2017_covers_its_letters_with_apostrophes() {
    let layouts = layouts();
    let layout = &layouts["kk-latn-2017"];
    let typable = typable(layout);
    for (id, tokens) in token_alphabets() {
        assert_eq!(id, "kk-latn-2017");
        let missing: Vec<&String> = tokens.iter().filter(|t| !typable.contains(*t)).collect();
        assert!(missing.is_empty(), "2017 cannot type: {missing:?}");
    }
    let labels: Vec<String> = char_keys(layout, "letters")
        .into_iter()
        .map(|k| k.label)
        .collect();
    for absent in ["w", "x", "c"] {
        assert!(
            !labels.contains(&absent.to_string()),
            "{absent} is not in the 2017 alphabet"
        );
    }
}

#[test]
fn official_latin_2021_drops_letters_outside_its_alphabet() {
    let layouts = layouts();
    let labels: Vec<String> = char_keys(&layouts["kk-latn-2021"], "letters")
        .into_iter()
        .map(|k| k.label)
        .collect();
    for absent in ["w", "x", "c", "ç"] {
        assert!(
            !labels.contains(&absent.to_string()),
            "{absent} is not in the 2021 alphabet"
        );
    }
}

#[test]
fn kazakh_latin_layouts_keep_dotted_and_dotless_i_casing() {
    let layouts = layouts();
    for id in ["kk-latn-anmitalidev", "kk-latn-2021"] {
        let keys: HashMap<String, CharKey> = char_keys(&layouts[id], "letters")
            .into_iter()
            .map(|k| (k.label.clone(), k))
            .collect();
        assert_eq!(keys["i"].shifted, "İ", "{id}: i");
        assert_eq!(keys["ı"].shifted, "I", "{id}: ı");
    }
}

#[test]
fn kazakh_latin_layouts_contain_no_cyrillic() {
    for layout in loaded().layouts.iter().filter(|l| l.locale == "kk-Latn") {
        let cyrillic = typable(layout)
            .iter()
            .flat_map(|s| s.chars().collect::<Vec<_>>())
            .any(|c| ('\u{400}'..='\u{52f}').contains(&c));
        assert!(!cyrillic, "{} has Cyrillic letters", layout.id);
    }
}

#[test]
fn apostrophe_alphabet_types_letters_with_apostrophe_on_long_press() {
    let layouts = layouts();
    let key = char_keys(&layouts["kk-latn-2017"], "letters")
        .into_iter()
        .find(|k| k.label == "a")
        .unwrap();
    assert_eq!(key.hold, [format!("a{APOSTROPHE}")]);
    assert_eq!(key.hold_shifted, [format!("A{APOSTROPHE}")]);
}

#[test]
fn turkish_layouts_use_turkish_casing_for_dotted_and_dotless_i() {
    let layouts = layouts();
    for id in ["tr-q", "tr-f"] {
        let keys: HashMap<String, CharKey> = char_keys(&layouts[id], "letters")
            .into_iter()
            .map(|k| (k.label.clone(), k))
            .collect();
        assert_eq!(keys["i"].shifted, "İ", "{id}: i");
        assert_eq!(keys["ı"].shifted, "I", "{id}: ı");
    }
}

#[test]
fn turkish_layouts_have_thirty_two_letters_and_currency_on_long_press() {
    let layouts = layouts();
    for id in ["tr-q", "tr-f"] {
        let keys = char_keys(&layouts[id], "letters");
        let letters = letter_labels(&layouts[id], "letters");
        assert_eq!(letters.len(), 32, "{id} letter keys");
        assert_eq!(keys.iter().find(|k| k.label == "e").unwrap().hold, ["€"]);
        assert_eq!(keys.iter().find(|k| k.label == "t").unwrap().hold, ["₺"]);
    }
}

#[test]
fn emoji_data_parses_and_has_a_recent_tab_first() {
    let categories = kotoba_emoji::parse_emoji_data(kotoba_data::emoji_json()).unwrap();
    assert!(categories[0].recent);
    assert!(
        categories
            .iter()
            .skip(1)
            .all(|c| !c.recent && !c.emoji.is_empty())
    );
}

#[test]
fn every_built_in_layout_has_a_language_key_on_its_letters_layer() {
    for layout in loaded().layouts {
        let has = layout
            .layer(&layout.default_layer)
            .unwrap()
            .rows
            .iter()
            .flatten()
            .any(|key| matches!(key, Key::Language(_)));
        assert!(has, "{} has no language key", layout.id);
    }
}
