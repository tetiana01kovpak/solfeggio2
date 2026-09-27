import { createRequire } from 'node:module';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const require = createRequire(import.meta.url);
const { FREQUENCIES } = require('../app.js');

const expected = [
  [174, 'Eases pain & stress'],
  [285, 'Enhances healing & regeneration'],
  [396, 'Releases fear & guilt'],
  [417, 'Facilitates change & letting go'],
  [528, 'Encourages healing & transformation'],
  [639, 'Supports connection & harmony'],
  [728, 'Claimed to destroy parasites in the body'],
  [852, 'Fosters intuition & awareness'],
];
assert.deepEqual(FREQUENCIES.map((f) => [f.hz, f.label]), expected);
assert.ok(FREQUENCIES.find((f) => f.hz === 728).note, '728 Hz has no note');

// The Android app keeps its own copy of the labels; it must match the web app.
const kotlin = readFileSync(
  new URL('../android/app/src/main/java/io/github/tetiana01kovpak/solfeggio/Frequencies.kt', import.meta.url),
  'utf8',
);
const android = [...kotlin.matchAll(/Frequency\((\d+), "([^"]+)"/g)].map((m) => [Number(m[1]), m[2]]);
assert.deepEqual(android, expected, 'Android labels differ from the web app');

console.log(`ok: ${expected.map(([hz]) => hz).join(', ')} Hz`);
