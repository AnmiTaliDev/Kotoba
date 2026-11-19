use std::process::ExitCode;

use kotoba_layout::{ImportResult, LayoutIssue, LayoutSource, Severity, check_import, load};

const USAGE: &str = "\
Usage:
  kotoba-cli validate FILE...   check layout files against the built-in layouts
  kotoba-cli list               list the built-in layouts
  kotoba-cli export ID          print a built-in layout as a starting point
";

fn built_in_sources() -> Vec<LayoutSource> {
    kotoba_data::layouts()
        .iter()
        .map(|(name, text)| LayoutSource {
            origin: format!("layouts/{name}"),
            text: (*text).to_string(),
            built_in: true,
        })
        .collect()
}

fn print_issue(issue: &LayoutIssue) {
    let label = match issue.severity {
        Severity::Error => "error",
        Severity::Warning => "warning",
    };
    println!("  {label}: {}: {}", issue.path, issue.message);
}

fn validate(files: &[String]) -> ExitCode {
    if files.is_empty() {
        eprint!("{USAGE}");
        return ExitCode::from(2);
    }
    let built_in = built_in_sources();
    let mut failed = false;
    for file in files {
        let text = match std::fs::read_to_string(file) {
            Ok(text) => text,
            Err(error) => {
                println!("{file}: cannot read: {error}");
                failed = true;
                continue;
            }
        };
        match check_import(&text, &built_in, &[]) {
            ImportResult::Valid { layout, warnings } => {
                println!("{file}: ok ({}, {})", layout.id, layout.name);
                warnings.iter().for_each(print_issue);
            }
            ImportResult::Invalid { issues } => {
                println!("{file}: invalid");
                issues.iter().for_each(print_issue);
                failed = true;
            }
        }
    }
    if failed {
        ExitCode::from(1)
    } else {
        ExitCode::SUCCESS
    }
}

fn list() -> ExitCode {
    let loaded = load(&built_in_sources());
    for layout in &loaded.layouts {
        println!(
            "{}\t{}\t{}\t{}",
            layout.id, layout.locale, layout.order, layout.name
        );
    }
    ExitCode::SUCCESS
}

fn export(id: &str) -> ExitCode {
    let loaded = load(&built_in_sources());
    match loaded.sources.get(id) {
        Some(source) => {
            print!("{}", source.text);
            ExitCode::SUCCESS
        }
        None => {
            eprintln!("no built-in layout with id {id}");
            ExitCode::from(1)
        }
    }
}

fn main() -> ExitCode {
    let args: Vec<String> = std::env::args().skip(1).collect();
    match args.first().map(String::as_str) {
        Some("validate") => validate(&args[1..]),
        Some("list") => list(),
        Some("export") if args.len() == 2 => export(&args[1]),
        _ => {
            eprint!("{USAGE}");
            ExitCode::from(2)
        }
    }
}
