use kotoba_emoji::{RECENT_LIMIT, add_recent, decode_recents, encode_recents, parse_emoji_data};

fn list(items: &[&str]) -> Vec<String> {
    items.iter().map(|s| s.to_string()).collect()
}

#[test]
fn new_emoji_goes_first() {
    assert_eq!(
        add_recent(&list(&["a", "b"]), "c", 10),
        list(&["c", "a", "b"])
    );
}

#[test]
fn repeated_emoji_moves_to_the_front_without_duplicates() {
    assert_eq!(
        add_recent(&list(&["a", "b", "c"]), "c", 10),
        list(&["c", "a", "b"])
    );
}

#[test]
fn list_is_cut_to_the_limit() {
    assert_eq!(
        add_recent(&list(&["a", "b", "c"]), "d", 3),
        list(&["d", "a", "b"])
    );
}

#[test]
fn default_limit_is_thirty_two() {
    let full: Vec<String> = (0..RECENT_LIMIT).map(|i| i.to_string()).collect();
    assert_eq!(add_recent(&full, "x", RECENT_LIMIT).len(), RECENT_LIMIT);
}

#[test]
fn recents_round_trip_through_storage() {
    let recents = list(&["😀", "❤️", "👍"]);
    assert_eq!(decode_recents(&encode_recents(&recents)), recents);
    assert!(decode_recents("").is_empty());
}

#[test]
fn parses_categories() {
    let json = r#"{"categories":[
        {"id":"recent","name":"Recent","icon":"i1","recent":true,"emoji":[]},
        {"id":"smileys","name":"Smileys","icon":"i2","emoji":["😀","😁"]}
    ]}"#;
    let categories = parse_emoji_data(json).unwrap();
    assert_eq!(categories.len(), 2);
    assert!(categories[0].recent && categories[0].emoji.is_empty());
    assert!(!categories[1].recent);
    assert_eq!(categories[1].emoji, list(&["😀", "😁"]));
}

#[test]
fn rejects_malformed_data_with_a_path() {
    assert!(parse_emoji_data("not json").is_err());
    let error =
        parse_emoji_data(r#"{"categories":[{"id":"x","name":"X","icon":"i"}]}"#).unwrap_err();
    assert!(error.contains("$.categories[0].emoji"), "{error}");
}
