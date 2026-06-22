#!/usr/bin/env node
//
// i18n lint - fails the build on hardcoded SME-facing strings.
//
// Rule: JSX text nodes in feature components must come from the language dictionary
// (useLang().t.*), never literal sentences. Scans .tsx files under src, excluding the
// shadcn primitives (src/components/ui) and the dictionaries themselves (src/lang).
//
// A literal is flagged when it contains >=2 alphabetic words and at least one lowercase
// letter (skips ALLCAPS acronyms like "URGENT"). Add a line containing "i18n-ignore"
// to suppress a deliberate exception.
//
// Key safety (no missing/typo keys) is handled by TypeScript: sw.ts is typed as a
// DeepPartial<Dictionary>, so the compiler catches bad keys and the provider falls back
// to English at runtime.
//
import { readdirSync, readFileSync, statSync } from "node:fs";
import { join, relative } from "node:path";

const ROOT = new URL("..", import.meta.url).pathname;
const SRC = join(ROOT, "src");
const EXCLUDE_DIRS = [join(SRC, "components", "ui"), join(SRC, "lang")];

function walk(dir) {
  const out = [];
  for (const entry of readdirSync(dir)) {
    const full = join(dir, entry);
    if (statSync(full).isDirectory()) {
      if (EXCLUDE_DIRS.some((ex) => full.startsWith(ex))) continue;
      out.push(...walk(full));
    } else if (full.endsWith(".tsx")) {
      out.push(full);
    }
  }
  return out;
}

// JSX text between a closing '>' and an opening '<' that is not an expression.
const JSX_TEXT = />([^<>{}]+)</g;
const HAS_TWO_WORDS = /[A-Za-z]{2,}\s+[A-Za-z]{2,}/;
const HAS_LOWERCASE = /[a-z]/;

const violations = [];

for (const file of walk(SRC)) {
  const lines = readFileSync(file, "utf8").split("\n");
  lines.forEach((line, i) => {
    if (line.includes("i18n-ignore")) return;
    let m;
    JSX_TEXT.lastIndex = 0;
    while ((m = JSX_TEXT.exec(line)) !== null) {
      const text = m[1].trim();
      if (HAS_TWO_WORDS.test(text) && HAS_LOWERCASE.test(text)) {
        violations.push(`${relative(ROOT, file)}:${i + 1}  "${text}"`);
      }
    }
  });
}

if (violations.length > 0) {
  console.error("i18n lint failed - hardcoded SME-facing strings found:\n");
  for (const v of violations) console.error("  " + v);
  console.error(`\n${violations.length} violation(s). Move copy into src/lang/en.ts and use useLang().t.`);
  process.exit(1);
}

console.log("i18n lint passed - no hardcoded SME-facing strings.");
