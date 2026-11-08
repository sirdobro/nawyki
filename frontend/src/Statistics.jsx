import React, { useEffect, useState } from 'react';
import { api } from './api';
import DateField from './DateField';
import { formatDate } from './dates';

const statuses = { MET: 'Cel osiągnięty', MISSED: 'Cel nieosiągnięty', IN_PROGRESS: 'W toku', PARTIAL: 'Skrócony okres — poza oceną' };
export function Streak({ value }) {
  const unit = value.unit === 'WEEKS' ? 'tyg.' : 'terminów';
  return <p className="streak-line">{value.archived ? 'Seria przy archiwizacji' : 'Bieżąca seria'}: <strong>{value.current} {unit}</strong><span>Najlepsza: <strong>{value.best} {unit}</strong></span></p>;
}

export function HabitStreak({ id, revision }) {
  const [data, setData] = useState(null), [error, setError] = useState(''), [retry, setRetry] = useState(0);
  useEffect(() => {
    let active = true; setData(null); setError('');
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
    let active = true; setData(null); setError('');
    if (valid) api(`/api/reports?period=${period}&date=${date}`).then(value => { if (active) setData(value); }).catch(e => { if (active) setError(e.message); });
    return () => { active = false; };
  }, [period, date, valid, revision, retry]);
  return <section aria-labelledby="statistics-title"><div className="page-heading"><div><p className="eyebrow">TWOJA REGULARNOŚĆ</p><h1 id="statistics-title">Podsumowanie</h1></div></div>
    <div className="report-filters"><label>Zakres<select value={period} onChange={e => setPeriod(e.target.value)}><option value="week">Tydzień</option><option value="month">Miesiąc</option></select></label><DateField key={retry} label="Dzień w wybranym okresie" value={date} onChange={value => { if (value) setDate(value); }} max={today} required onValidityChange={setValid} /><button className="quiet" onClick={() => { setDate(today); setRetry(n => n + 1); }}>Bieżący okres</button></div>
    {!valid ? <p role="status">Wybierz poprawną datę.</p> : error ? <div className="notice error" role="alert">{error} <button className="secondary" onClick={() => setRetry(n => n + 1)}>Spróbuj ponownie</button></div> : !data ? <p role="status">Obliczanie podsumowania…</p> : <>
      <p className="report-range">{formatDate(data.from)} – {formatDate(data.to)}{data.to > data.today && ` · dane do ${formatDate(data.today)}`}</p>
      <div className="report-overview"><div className="panel"><span>Wykonania nawyków w okresie</span><strong>{data.habits.reduce((sum, h) => sum + h.progress.completed, 0)}</strong></div><div className="panel"><span>Treningi w okresie</span><strong>{data.workoutCount}</strong></div></div>
      <p className="muted">Serie obejmują całą historię do dzisiaj. Dni wolne i otwarty okres nie przerywają serii. Dla zarchiwizowanych nawyków zachowujemy wynik z końca ich aktywności.</p>
      {!data.habits.length && <div className="panel empty"><h2>Brak nawyków w tym okresie</h2><p>Wybierz inny okres lub dodaj nawyk w zakładce „Moje nawyki”.</p></div>}
      {data.habits.map(h => <article className="panel report-habit" key={h.id}><h2>{h.name}</h2><Streak value={h.progress.streak} />
        {h.scheduleType === 'WEEKLY_GOAL' ? <><p>Wykonania w wybranym okresie: <strong>{h.progress.completed}</strong>. Cel: <strong>{h.weeklyTarget} różnych dni na 7 dni</strong>.</p><p className="muted">Tygodnie liczymy od daty rozpoczęcia nawyku. Poniżej całe okresy, które przecinają wybrany zakres — ich liczniki mogą obejmować dni spoza niego. Nadwyżka nie zwiększa realizacji powyżej 100%.</p><ul className="weekly-periods">{h.progress.periods.map(p => <li key={p.from}><span>{formatDate(p.from)} – {formatDate(p.to)}</span><strong>{p.completed}/{p.target} · {Math.min(100, Math.round(p.completed / p.target * 100))}%</strong><span>{statuses[p.status]}</span></li>)}</ul></>
          : <><p><strong>{h.progress.completed}/{h.progress.scheduled}</strong> wykonanych terminów do dzisiaj{h.progress.scheduled > 0 && ` · ${Math.round(h.progress.completed / h.progress.scheduled * 100)}%`}</p><p className="muted">{h.progress.periods.some(p => p.status === 'IN_PROGRESS') ? 'Dzisiejszy termin jest jeszcze w toku.' : 'Brak otwartego terminu w tym okresie.'} Przyszłe terminy nie wchodzą do wyniku.</p></>}
      </article>)}
    </>}
  </section>;
}
