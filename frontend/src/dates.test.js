import test from 'node:test';
import assert from 'node:assert/strict';
import { calendarDays, dateInputError, formatDate, formatMonth, parseDateInput } from './dates.js';

test('visible dates and months use Polish order', () => {
  assert.equal(formatDate('2026-11-02'), '02-11-2026');
  assert.equal(formatMonth('2026-11'), '11-2026');
  assert.equal(formatDate(''), ''); assert.equal(formatMonth(''), '');
});
test('manual entry is always day-first, including ambiguous dates', () => {
  for (const text of ['02-11-2026', '02.11.2026', '02/11/2026', '02112026']) assert.equal(parseDateInput(text), '2026-11-02');
  assert.equal(parseDateInput('11/02/2026'), '2026-02-11');
  assert.equal(parseDateInput('2026-11-02'), null);
});
test('invalid and incomplete dates are rejected rather than rolled into next month', () => {
  for (const text of ['', '02-11', '31-04-2026', '00-11-2026', '01-00-2026', '01-13-2026', '01-01-0000']) assert.equal(parseDateInput(text), null);
});
test('leap years and years below 100 are handled without Date constructor shortcuts', () => {
  assert.equal(parseDateInput('29-02-2000'), '2000-02-29');
  assert.equal(parseDateInput('29-02-1900'), null);
  assert.equal(parseDateInput('29-02-2024'), '2024-02-29');
  assert.equal(parseDateInput('29-02-2026'), null);
  assert.equal(parseDateInput('01-01-0099'), '0099-01-01');
});
test('month entry uses month-year and validates bounds', () => {
  assert.equal(parseDateInput('11-2026', true), '2026-11');
  assert.equal(parseDateInput('112026', true), '2026-11');
  assert.equal(parseDateInput('13-2026', true), null);
  assert.equal(parseDateInput('2026-11', true), null);
});
test('required fields and future limits produce Polish errors', () => {
  assert.equal(dateInputError('', { required: true }), 'Podaj datę.');
  assert.equal(dateInputError(''), '');
  assert.equal(dateInputError('03-11-2026', { max: '2026-11-03' }), '');
  assert.match(dateInputError('04-11-2026', { max: '2026-11-03' }), /03-11-2026/);
  assert.match(dateInputError('12-2026', { monthOnly: true, max: '2026-11' }), /11-2026/);
  assert.match(dateInputError('31-02-2026'), /DD-MM-RRRR/);
});
test('calendar starts on Monday and spans month/year boundaries', () => {
  const november = calendarDays('2026-11');
  assert.equal(november.length, 42); assert.equal(november[0], '2026-10-26');
  assert.ok(november.includes('2026-11-30'));
  assert.equal(calendarDays('2026-12').at(-1), '2027-01-10');
  assert.ok(calendarDays('2024-02').includes('2024-02-29'));
  assert.ok(!calendarDays('2026-02').includes('2026-02-29'));
});
