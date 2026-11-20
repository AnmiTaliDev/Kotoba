#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
cargo build -p kotoba-wasm --target wasm32-unknown-unknown --release
wasm-bindgen --target web --no-typescript --out-dir web/validator/pkg target/wasm32-unknown-unknown/release/kotoba_wasm.wasm
echo "Serve it with: python3 -m http.server --directory web/validator 8000"
