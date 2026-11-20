import init, { check, built_in_layouts, built_in_text } from "./pkg/kotoba_wasm.js";

const STORAGE_KEY = "kotoba-layout-checker";
const $ = (id) => document.getElementById(id);
const editor = $("editor");
const status = $("status");
const issues = $("issues");
const preview = $("preview");
const layerSelect = $("layer");
const shift = $("shift");
const numbers = $("numbers");
const templates = $("templates");

let current = null;
let timer = 0;

function remember(text) {
  try { localStorage.setItem(STORAGE_KEY, text); } catch (error) { void error; }
}

function recall() {
  try { return localStorage.getItem(STORAGE_KEY) || ""; } catch (error) { return ""; }
}

function setStatus(text, kind) {
  status.textContent = text;
  status.className = "chip" + (kind ? " " + kind : "");
}

function renderIssues(list) {
  issues.replaceChildren();
  if (list.length === 0) {
    const item = document.createElement("li");
    item.className = "empty";
    item.textContent = editor.value.trim() ? "No problems found." : "Nothing to check yet.";
    issues.append(item);
    return;
  }
  for (const issue of list) {
    const item = document.createElement("li");
    const badge = document.createElement("span");
    badge.className = "badge " + issue.severity;
    badge.textContent = issue.severity;
    const path = document.createElement("span");
    path.className = "path";
    path.textContent = issue.path;
    const message = document.createElement("span");
    message.className = "message";
    message.textContent = issue.message;
    item.append(badge, path, message);
    issues.append(item);
  }
}

function keyText(key, shifted, layout) {
  switch (key.type) {
    case "char": return shifted ? key.shifted : key.label;
    case "layer": return key.label;
    case "language": return "Lang";
    case "shift": return "⇧";
    case "delete": return "⌫";
    case "enter": return "↵";
    case "space": return layout.name;
    default: return "";
  }
}

function renderKey(key, shifted, layout) {
  const node = document.createElement("div");
  node.className = "key " + key.type;
  if (key.type !== "char" && key.type !== "space" && key.type !== "spacer") node.classList.add("tonal");
  node.style.flexGrow = String(key.weight);
  node.textContent = keyText(key, shifted, layout);
  if (key.type === "char") {
    const alternatives = shifted ? key.holdShifted : key.hold;
    if (alternatives.length > 0) {
      node.title = "Long press: " + alternatives.join(" ");
      const hint = document.createElement("span");
      hint.className = "hint";
      hint.textContent = alternatives[0];
      node.append(hint);
    }
  }
  return node;
}

function renderPreview() {
  preview.replaceChildren();
  if (!current) return;
  const layer = current.layers.find((l) => l.id === layerSelect.value) || current.layers[0];
  if (!layer) return;
  if (layer.kind === "emoji") {
    const note = document.createElement("div");
    note.className = "emoji-note";
    note.textContent = "Emoji panel. The back key shows “" + (layer.back ? layer.back.label : "") + "”.";
    preview.append(note);
    return;
  }
  const shifted = shift.checked && layer.shiftable;
  const rows = [];
  if (numbers.checked && layer.numberRow && current.numberRow.length > 0) rows.push(current.numberRow);
  rows.push(...layer.rows);
  for (const keys of rows) {
    const row = document.createElement("div");
    row.className = "row";
    for (const key of keys) row.append(renderKey(key, shifted, current));
    preview.append(row);
  }
}

function renderLayers(layout) {
  const previous = layerSelect.value;
  layerSelect.replaceChildren();
  for (const layer of layout.layers) {
    const option = document.createElement("option");
    option.value = layer.id;
    option.textContent = layer.label ? layer.id + " (" + layer.label + ")" : layer.id;
    layerSelect.append(option);
  }
  layerSelect.value = layout.layers.some((l) => l.id === previous) ? previous : layout.defaultLayer;
}

function validate() {
  const text = editor.value;
  remember(text);
  if (!text.trim()) {
    current = null;
    setStatus("Empty");
    renderIssues([]);
    layerSelect.replaceChildren();
    preview.replaceChildren();
    return;
  }
  const result = JSON.parse(check(text));
  const errors = result.issues.filter((i) => i.severity === "error").length;
  const warnings = result.issues.length - errors;
  renderIssues(result.issues);
  if (result.ok) {
    current = result.layout;
    setStatus(warnings > 0 ? "Valid, " + warnings + (warnings === 1 ? " warning" : " warnings") : "Valid", warnings > 0 ? "warn" : "ok");
    renderLayers(current);
    renderPreview();
  } else {
    current = null;
    setStatus(errors + (errors === 1 ? " error" : " errors"), "bad");
    layerSelect.replaceChildren();
    preview.replaceChildren();
  }
}

function schedule() {
  clearTimeout(timer);
  timer = setTimeout(validate, 150);
}

async function readFile(file) {
  editor.value = await file.text();
  validate();
}

await init();

for (const layout of JSON.parse(built_in_layouts())) {
  const option = document.createElement("option");
  option.value = layout.id;
  option.textContent = layout.name + " (" + layout.id + ")";
  templates.append(option);
}

templates.addEventListener("change", () => {
  if (!templates.value) return;
  const text = built_in_text(templates.value) || "";
  editor.value = text.replace(/"id"\s*:\s*"[^"]*"/, '"id": "my-' + templates.value + '"');
  templates.value = "";
  validate();
});
editor.addEventListener("input", schedule);
$("file").addEventListener("change", (event) => {
  const [file] = event.target.files;
  if (file) readFile(file);
  event.target.value = "";
});
$("clear").addEventListener("click", () => {
  editor.value = "";
  validate();
  editor.focus();
});
layerSelect.addEventListener("change", renderPreview);
shift.addEventListener("change", renderPreview);
numbers.addEventListener("change", renderPreview);
editor.addEventListener("dragover", (event) => {
  event.preventDefault();
  editor.classList.add("dragging");
});
editor.addEventListener("dragleave", () => editor.classList.remove("dragging"));
editor.addEventListener("drop", (event) => {
  event.preventDefault();
  editor.classList.remove("dragging");
  const [file] = event.dataTransfer.files;
  if (file) readFile(file);
});

editor.value = recall();
validate();
