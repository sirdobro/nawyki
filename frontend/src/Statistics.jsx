import React, { useEffect, useState } from 'react';
import { api } from './api';
import DateField from './DateField';
import { quantity, completedDays } from './polish';
import { formatDate } from './dates';

const shiftDate = (date, days) => { const value = new Date(`${date}T12:00:00Z`); value.setUTCDate(value.getUTCDate() + days); return value.toISOString().slice(0, 10); };
const monday = date => shiftDate(date, 1 - (new Date(`${date}T12:00:00Z`).getUTCDay() || 7));
const statuses = { MET: 'Cel osiągnięty', MISSED: 'Cel nieosiągnięty', IN_PROGRESS: 'W toku', PARTIAL: 'Niepełny tydzień — nie wpływa na serię' };
export function Streak({ value }) {
  const unit = value.unit === 'WEEKS' ? 'weeks' : 'completions';
  return <p className="streak-line">{value.archived ? 'Seria w chwili archiwizacji' : 'Bieżąca seria'}: <strong>{quantity(value.current, unit)} z rzędu</strong><span>Najlepsza: <strong>{quantity(value.best, unit)} z rzędu</strong></span></p>;
}

export function HabitStreak({ id, revision }) {
  const [data, setData] = useState(null), [error, setError] = useState(''), [retry, setRetry] = useState(0);
  useEffect(() => {
    let active = true; setError('');
    api(`/api/habits/${id}/progress`).then(value => { if (active) setData(value); }).catch(e => { if (active) setError(e.message); });
    return () => { active = false; };
  }, [id, revision, retry]);
  return error ? <p role="alert">{error} <button className="quiet" onClick={() => setRetry(n => n + 1)}>Ponów</button></p>
    : data ? <Streak value={data.streak} /> : <p role="status">Obliczanie serii…</p>;
}

export default function Statistics({ today, revision }) {
  const [period, setPeriod] = useState('week'), [date, setDate] = useState(today), [valid, setValid] = useState(true);
  const [data, setData] = useState(null), [error, setError] = useState(''), [retry, setRetry] = useState(0);
  useEffect(() => {
    let active = true; setError('');
    if (valid) api(`/api/reports?period=${period}&date=${date}`).then(value => { if (active) setData(value); }).catch(e => { if (active) setError(e.message); });
    return () => { active = false; };
  }, [period, date, valid, revision, retry]);
  useEffect(() => { setData(null); }, [period, date, valid]);
  return <section aria-labelledby="statistics-title"><div className="page-heading"><div><p className="eyebrow">TWOJA REGULARNOŚĆ</p><h1 id="statistics-title">Podsumowanie</h1></div></div>
    <div className="report-filters"><label>Zakres<select value={period} onChange={e => { setPeriod(e.target.value); setValid(true); }}><option value="week">Tydzień</option><option value="month">Miesiąc</option></select></label>
      {period === 'month' ? <DateField key={`month-${retry}`} label="Miesiąc" monthOnly value={date.slice(0, 7)} onChange={value => { if (value) setDate(`${value}-01`); }} max={today.slice(0, 7)} required onValidityChange={setValid} />
        : <div className="report-week"><span id="report-week-label">Tydzień</span><div className="actions" role="group" aria-labelledby="report-week-label"><button className="secondary" aria-label="Poprzedni tydzień" onClick={() => setDate(shiftDate(monday(date), -7))}>←</button><span className="report-week-range">{formatDate(monday(date))} – {formatDate(shiftDate(monday(date), 6))}</span><button className="secondary" aria-label="Następny tydzień" disabled={monday(date) >= monday(today)} onClick={() => setDate(shiftDate(monday(date), 7))}>→</button></div></div>}
      <button className="quiet" onClick={() => { setDate(today); setValid(true); setRetry(n => n + 1); }}>Bieżący okres</button></div>
    {!valid ? <p role="status">Wybierz poprawną datę.</p> : error ? <div className="notice error" role="alert">{error} <button className="secondary" onClick={() => setRetry(n => n + 1)}>Spróbuj ponownie</button></div> : !data ? <p role="status">Obliczanie podsumowania…</p> : <>
      <p className="report-range">{formatDate(data.from)} – {formatDate(data.to)}{data.to > data.today && ` · dane do ${formatDate(data.today)}`}</p>
      <div className="report-overview"><div className="panel"><span>Wykonania nawyków</span><strong>{data.habits.reduce((sum, h) => sum + h.progress.completed, 0)}</strong></div><div className="panel"><span>Treningi</span><strong>{data.workoutCount}</strong></div></div>
      <p className="muted">Serie obejmują całą historię do dzisiaj. Dni bez zaplanowanego nawyku nie przerywają serii. Na wykonanie nawyku masz czas do końca dnia, a na osiągnięcie celu tygodniowego — do końca tygodnia. Dla zarchiwizowanych nawyków zachowujemy wynik z końca ich aktywności.</p>
      {!data.habits.length && <div className="panel empty"><h2>Brak nawyków w tym okresie</h2><p>Wybierz inny okres lub dodaj nawyk w zakładce „Moje nawyki”.</p></div>}
      {data.habits.map(h => <article className="panel report-habit" key={h.id}><h2>{h.name}</h2><Streak value={h.progress.streak} />
        {h.scheduleType === 'WEEKLY_GOAL' ? <><p>Wykonania: <strong>{h.progress.completed}</strong>. Cel: <strong>{quantity(h.weeklyTarget, 'days')} w tygodniu</strong>.</p><p className="muted">Tygodnie liczymy od daty rozpoczęcia nawyku. Każdy tydzień trwa 7 dni. Poniżej pokazujemy pełne tygodnie nawyku, także gdy część dni wypada poza wybranym zakresem. Możesz wykonać nawyk częściej, ale wynik nie przekroczy 100%.</p><ul className="weekly-periods">{h.progress.periods.map(p => <li key={p.from}><span>{formatDate(p.from)} – {formatDate(p.to)}</span><strong>{completedDays(p.completed, p.target)} · {Math.min(100, Math.round(p.completed / p.target * 100))}%</strong><span>{statuses[p.status]}</span></li>)}</ul></>
          : <><p><strong>Wykonane: {h.progress.completed} z {h.progress.scheduled}</strong>{h.progress.scheduled > 0 && ` · ${Math.round(h.progress.completed / h.progress.scheduled * 100)}%`}</p><p className="muted">{h.progress.periods.some(p => p.status === 'IN_PROGRESS') ? 'Możesz jeszcze wykonać nawyk dzisiaj.' : ''} Wynik uwzględnia tylko dni do dzisiaj.</p></>}
      </article>)}
    </>}
  </section>;
}
