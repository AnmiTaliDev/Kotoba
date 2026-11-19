use std::env;
use std::fs;
use std::path::PathBuf;

fn main() {
    let manifest = PathBuf::from(env::var("CARGO_MANIFEST_DIR").unwrap());
    let layouts = manifest.join("../../data/layouts");
    println!("cargo:rerun-if-changed={}", layouts.display());
    let mut names: Vec<String> = fs::read_dir(&layouts)
        .expect("data/layouts must exist")
        .filter_map(|entry| entry.ok())
        .map(|entry| entry.file_name().to_string_lossy().into_owned())
        .filter(|name| name.ends_with(".json"))
        .collect();
    names.sort();
    let mut out = String::from("pub const LAYOUTS: &[(&str, &str)] = &[\n");
    for name in &names {
        let path = layouts.join(name).canonicalize().unwrap();
        out.push_str(&format!(
            "    ({:?}, include_str!({:?})),\n",
            name,
            path.display().to_string()
        ));
    }
    out.push_str("];\n");
    let target = PathBuf::from(env::var("OUT_DIR").unwrap()).join("layouts.rs");
    fs::write(target, out).unwrap();
}
