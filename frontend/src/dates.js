export const monthNames = ['styczeń', 'luty', 'marzec', 'kwiecień', 'maj', 'czerwiec', 'lipiec', 'sierpień', 'wrzesień', 'październik', 'listopad', 'grudzień'];
export const formatDate = iso => iso ? iso.split('-').reverse().join('-') : '';
export const formatMonth = iso => iso ? iso.split('-').reverse().join('-') : '';

function utcDate(year, month, day) {
  const date = new Date(0);
  date.setUTCFullYear(year, month - 1, day);
  date.setUTCHours(12, 0, 0, 0);
  return date;
}
export function parseDateInput(text, monthOnly = false) {
  const source = text.trim();
  const match = monthOnly
    ? source.match(/^(\d{2})[-./]?(\d{4})$/)
    : source.match(/^(\d{2})[-./]?(\d{2})[-./]?(\d{4})$/);
  if (!match) return null;
  const year = Number(match[monthOnly ? 2 : 3]);
  const month = Number(match[monthOnly ? 1 : 2]);
  const day = monthOnly ? 1 : Number(match[1]);
  if (year < 1 || year > 9999 || month < 1 || month > 12 || day < 1 || day > 31) return null;
  const date = utcDate(year, month, day);
  if (date.getUTCFullYear() !== year || date.getUTCMonth() !== month - 1 || date.getUTCDate() !== day) return null;
  const iso = `${String(year).padStart(4, '0')}-${String(month).padStart(2, '0')}`;
  return monthOnly ? iso : `${iso}-${String(day).padStart(2, '0')}`;
}
export function dateInputError(text, { monthOnly = false, max = '', required = false } = {}) {
  if (!text.trim()) return required ? (monthOnly ? 'Wybierz miesiąc.' : 'Podaj datę.') : '';
  const value = parseDateInput(text, monthOnly);
  if (!value) return `Podaj poprawny ${monthOnly ? 'miesiąc w formacie MM-RRRR' : 'dzień w formacie DD-MM-RRRR'}.`;
  if (max && value > max) return `Wybierz ${monthOnly ? 'miesiąc' : 'datę'} nie późniejsz${monthOnly ? 'y' : 'ą'} niż ${monthOnly ? formatMonth(max) : formatDate(max)}.`;
  return '';
}
export function calendarDays(month) {
  const [year, number] = month.split('-').map(Number);
  const first = utcDate(year, number, 1);
  const offset = (first.getUTCDay() + 6) % 7;
  return Array.from({ length: 42 }, (_, index) => {
    const date = utcDate(year, number, 1 - offset + index);
    return date.getUTCFullYear() < 1 || date.getUTCFullYear() > 9999 ? '' : date.toISOString().slice(0, 10);
  });
}
