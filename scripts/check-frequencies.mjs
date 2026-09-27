import { createRequire } from 'node:module';
import assert from 'node:assert/strict';

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

console.log(`ok: ${expected.map(([hz]) => hz).join(', ')} Hz`);
