const rules = new Intl.PluralRules('pl');
const forms = {
  days: { one: 'dzień', few: 'dni', many: 'dni' },
  weeks: { one: 'tydzień', few: 'tygodnie', many: 'tygodni' },
  completions: { one: 'wykonanie', few: 'wykonania', many: 'wykonań' },
  workouts: { one: 'trening', few: 'treningi', many: 'treningów' },
  sets: { one: 'seria', few: 'serie', many: 'serii' },
  exercises: { one: 'ćwiczenie', few: 'ćwiczenia', many: 'ćwiczeń' },
};

// Counts in the interface are nonnegative integers; Polish 12–14 and 21
// do not follow the same forms as 2–4 and 1.
export function quantity(count, unit) {
  return `${count} ${forms[unit][rules.select(count)]}`;
}

// After "z" a single day takes the genitive "dnia", not "dzień".
export function completedDays(count, target) {
  return `Wykonane: ${count} z ${target} ${target === 1 ? 'dnia' : 'dni'}`;
}
