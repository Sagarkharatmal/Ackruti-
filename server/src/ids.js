import { randomBytes, randomInt } from 'node:crypto';

// No 0/O, 1/I/L so codes are easy to read out loud on the street.
const CODE_ALPHABET = 'ABCDEFGHJKMNPQRSTUVWXYZ23456789';

export function makeGameCode(length = 6) {
  let code = '';
  for (let i = 0; i < length; i++) code += CODE_ALPHABET[randomInt(CODE_ALPHABET.length)];
  return code;
}

export function makeId() {
  return randomBytes(8).toString('hex');
}

export function makeToken() {
  return randomBytes(24).toString('base64url');
}

export function normaliseCode(raw) {
  return String(raw ?? '')
    .toUpperCase()
    .replace(/[^A-Z0-9]/g, '')
    .slice(0, 6);
}
