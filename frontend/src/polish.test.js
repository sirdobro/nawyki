import test from 'node:test';
import assert from 'node:assert/strict';
import { quantity, completedDays } from './polish.js';

test('days use dzień only for 1, including numbers ending in 1', () => {
  for (const [count, expected] of [[0, '0 dni'], [1, '1 dzień'], [2, '2 dni'], [3, '3 dni'], [7, '7 dni'], [21, '21 dni'], [101, '101 dni']]) {
    assert.equal(quantity(count, 'days'), expected);
  }
});

test('completion and week counts distinguish singular, few and many', () => {
  for (const [count, completion, week] of [[1, 'wykonanie', 'tydzień'], [2, 'wykonania', 'tygodnie'], [4, 'wykonania', 'tygodnie'], [5, 'wykonań', 'tygodni'], [12, 'wykonań', 'tygodni'], [14, 'wykonań', 'tygodni'], [21, 'wykonań', 'tygodni'], [22, 'wykonania', 'tygodnie'], [101, 'wykonań', 'tygodni'], [102, 'wykonania', 'tygodnie'], [112, 'wykonań', 'tygodni'], [114, 'wykonań', 'tygodni']]) {
    assert.equal(quantity(count, 'completions'), `${count} ${completion}`);
    assert.equal(quantity(count, 'weeks'), `${count} ${week}`);
  }
});

test('workout nouns follow Polish forms, including zero and teens', () => {
  for (const [unit, one, few, many] of [['sets', 'seria', 'serie', 'serii'], ['exercises', 'ćwiczenie', 'ćwiczenia', 'ćwiczeń'], ['workouts', 'trening', 'treningi', 'treningów']]) {
    for (const count of [0, 5, 11, 12, 13, 14, 21, 112]) assert.equal(quantity(count, unit), `${count} ${many}`);
    for (const count of [2, 3, 4, 22, 23, 24, 102]) assert.equal(quantity(count, unit), `${count} ${few}`);
    assert.equal(quantity(1, unit), `1 ${one}`);
  }
});

test('weekly progress uses genitive after z, preserves zero and excess completions', () => {
  assert.equal(completedDays(0, 1), 'Wykonane: 0 z 1 dnia');
  assert.equal(completedDays(1, 1), 'Wykonane: 1 z 1 dnia');
  assert.equal(completedDays(2, 3), 'Wykonane: 2 z 3 dni');
  assert.equal(completedDays(7, 5), 'Wykonane: 7 z 5 dni');
});
