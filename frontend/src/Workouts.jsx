import React, { useEffect, useRef, useState } from 'react';
import { api } from './api';
import DateField from './DateField';
import { formatDate } from './dates';

const emptySet = () => ({ repetitions: '', weightKg: '' });
const emptyExercise = () => ({ name: '', sets: [emptySet()] });
const emptyWorkout = today => ({ date: today, exercises: [emptyExercise()] });
const readableDate = date => new Intl.DateTimeFormat('pl-PL', { day: 'numeric', month: 'long', year: 'numeric', timeZone: 'UTC' }).format(new Date(`${date}T12:00:00Z`));

export default function Workouts({ today, onDirtyChange, externalRevision }) {
  const [month, setMonth] = useState(today.slice(0, 7));
  const [list, setList] = useState(null);
  const [revision, setRevision] = useState(0);
  const [editor, setEditor] = useState(null);
  const [preview, setPreview] = useState(null);
  const [draft, setDraft] = useState(emptyWorkout(today));
  const [original, setOriginal] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [remoteChanged, setRemoteChanged] = useState(false);
  const errorRef = useRef(null);
  const headingRef = useRef(null);
  const dirty = editor !== null && JSON.stringify(draft) !== original;

  useEffect(() => { onDirtyChange(dirty); return () => onDirtyChange(false); }, [dirty, onDirtyChange]);
  useEffect(() => {
    if (!dirty) return;
    const preventLoss = event => { event.preventDefault(); event.returnValue = ''; };
    window.addEventListener('beforeunload', preventLoss);
    return () => window.removeEventListener('beforeunload', preventLoss);
  }, [dirty]);
  useEffect(() => { if (error) errorRef.current?.focus(); }, [error]);
  useEffect(() => { if (editor !== null) headingRef.current?.focus(); }, [editor]);
  useEffect(() => {
    let active = true;
    if (editor === null) setError('');
    const [year, number] = month.split('-').map(Number);
    const last = new Date(Date.UTC(year, number, 0)).getUTCDate();
    api(`/api/workouts?from=${month}-01&to=${month}-${last}`)
      .then(data => { if (active) setList(data.workouts); })
      .catch(e => { if (active) setError(e.message); });
    return () => { active = false; };
  }, [month, revision, externalRevision]);
  useEffect(() => { setList(null); }, [month]);
  useEffect(() => { if (editor) setRemoteChanged(true); }, [externalRevision]);
  useEffect(() => {
    if (!preview || editor !== null) return;
    let active = true;
    api(`/api/workouts/${preview.id}`).then(value => { if (active) setPreview(value); }).catch(e => {
      if (!active) return;
      if (e.status === 404) { setPreview(null); setMessage('Ten trening został usunięty w innej karcie.'); }
      else setError(e.message);
    });
    return () => { active = false; };
  }, [externalRevision, editor, preview?.id]);

  async function act(action) {
    setBusy(true); setError(''); setMessage('');
    try { await action(); } catch (e) { setError(e.message); } finally { setBusy(false); }
  }
  function open(workout, id = '') {
    if (!id) setPreview(null);
    const value = { date: workout.date, exercises: workout.exercises.map(e => ({ name: e.name, sets: e.sets.map(s => ({ repetitions: String(s.repetitions), weightKg: s.weightKg })) })) };
    setRemoteChanged(false); setDraft(value); setOriginal(JSON.stringify(value)); setEditor(id); setError(''); setMessage('');
  }
  function cancel() {
    if (!dirty || confirm('Porzucić niezapisane zmiany treningu?')) { setEditor(null); setError(''); }
  }
  function exerciseChange(index, transform) {
    setDraft(previous => ({ ...previous, exercises: previous.exercises.map((e, i) => i === index ? transform(e) : e) }));
  }
  function setChange(exercise, index, field, value) {
    exerciseChange(exercise, e => ({ ...e, sets: e.sets.map((s, i) => i === index ? { ...s, [field]: value } : s) }));
  }
  async function save(event) {
    event.preventDefault();
    await act(async () => {
      const saved = await api(editor ? `/api/workouts/${editor}` : '/api/workouts', editor ? 'PUT' : 'POST', draft);
      setPreview(saved);
      setMonth(draft.date.slice(0, 7)); setEditor(null); setRevision(n => n + 1); setMessage('Trening zapisany.');
    });
  }
  async function remove() {
    if (!confirm('Trwale usunąć ten trening wraz z ćwiczeniami i seriami?')) return;
    await act(async () => { await api(`/api/workouts/${editor}`, 'DELETE'); setPreview(null); setEditor(null); setRevision(n => n + 1); setMessage('Trening usunięty.'); });
  }

  return <section aria-label="Dziennik treningów">
    <div className="page-heading"><div><p className="eyebrow">TWÓJ ZAPIS AKTYWNOŚCI</p><h1>Treningi</h1><p className="muted">Ćwiczenia i serie, do których możesz wrócić.</p></div>
      {editor === null && !preview && <button disabled={busy} onClick={() => open(emptyWorkout(today))}>+ Dodaj trening</button>}
    </div>
    {error && <div className="notice error" role="alert" ref={errorRef} tabIndex={-1}>{error}</div>}
    {message && <div className="notice" role="status">{message}</div>}
    {editor === null ? preview ? <WorkoutPreview key={preview.id} workout={preview} busy={busy} onEdit={() => open(preview, preview.id)} onBack={() => { setPreview(null); setError(''); setMessage(''); }} /> : <>
      <DateField className="workout-month" label="Miesiąc" monthOnly required value={month} max={today.slice(0, 7)} onChange={value => { if (value) setMonth(value); }} />
      {!list ? (error ? <button className="secondary" onClick={() => setRevision(n => n + 1)}>Spróbuj ponownie</button> : <p role="status">Wczytujemy treningi…</p>) : list.length === 0 ? <div className="panel empty"><h2>Jeszcze bez wpisów w tym miesiącu</h2><p>Zapisz ćwiczenia i serie z treningu. Zacznij od jednego ćwiczenia.</p><button onClick={() => open(emptyWorkout(today))}>Dodaj pierwszy trening</button></div> : <div className="workout-list">{list.map(w => <article className="habit-row" key={w.id}>
        <div><h2 className="workout-date">{readableDate(w.date)}</h2><p className="workout-summary">{w.exerciseNames.join(' · ')}</p><small>Serie: {w.setCount} · Ćwiczenia: {w.exerciseNames.length}</small></div>
        <button className="secondary" aria-label={`Otwórz trening ${formatDate(w.date)}`} disabled={busy} onClick={() => act(async () => setPreview(await api(`/api/workouts/${w.id}`)))}>Otwórz</button>
      </article>)}</div>}
    </> : <form className="panel workout-editor" onSubmit={save}>
      {remoteChanged && <p className="notice" role="status">Dane na serwerze mogły się zmienić. Twój formularz pozostaje bez zmian. Aby zobaczyć aktualny zapis, anuluj edycję. Zapisanie formularza zastąpi trening Twoją wersją.</p>}
      <h2 tabIndex={-1} ref={headingRef}>{editor ? 'Edytuj trening' : 'Nowy trening'}</h2>
      <fieldset disabled={busy} className="workout-fields"><legend className="sr-only">Dane treningu</legend>
        <DateField className="workout-date-input" label="Data treningu" required max={today} value={draft.date} onChange={date => setDraft(previous => ({ ...previous, date }))} />
        <p className="muted">Zostaw ciężar pusty, jeśli go nie zapisujesz. Każdy wiersz to jedna seria.</p>
        {draft.exercises.map((exercise, i) => <fieldset className="exercise-card" key={i}>
          <legend>Ćwiczenie {i + 1}</legend>
          <label>Nazwa ćwiczenia {i + 1}<input required maxLength={100} value={exercise.name} placeholder="np. Przysiad" onChange={e => exerciseChange(i, old => ({ ...old, name: e.target.value }))} /></label>
          <div className="set-head" aria-hidden="true"><span>Seria</span><span>Powtórzenia</span><span>Ciężar (kg)</span><span /></div>
          {exercise.sets.map((set, j) => <div className="set-row" key={j}>
            <span className="set-number">{j + 1}</span>
            <label><span className="sr-only">Powtórzenia — ćwiczenie {i + 1}, seria {j + 1}</span><input type="number" inputMode="numeric" required min={1} max={10000} step={1} value={set.repetitions} onInput={e => setChange(i, j, 'repetitions', e.currentTarget.value)} onChange={e => setChange(i, j, 'repetitions', e.target.value)} /></label>
            <label><span className="sr-only">Ciężar kg — ćwiczenie {i + 1}, seria {j + 1}, opcjonalny</span><input type="text" inputMode="decimal" maxLength={8} placeholder="—" value={set.weightKg} onChange={e => setChange(i, j, 'weightKg', e.target.value)} /></label>
            <button type="button" className="quiet remove-set" disabled={exercise.sets.length === 1} aria-label={`Usuń serię ${j + 1} z ćwiczenia ${i + 1}`} onClick={() => exerciseChange(i, old => ({ ...old, sets: old.sets.filter((_, n) => n !== j) }))}>×</button>
          </div>)}
          <div className="actions"><button type="button" className="secondary" disabled={exercise.sets.length >= 30} onClick={() => exerciseChange(i, old => ({ ...old, sets: [...old.sets, { ...old.sets[old.sets.length - 1] }] }))}>+ Dodaj serię</button>
            <button type="button" className="quiet" disabled={draft.exercises.length === 1} onClick={() => setDraft({ ...draft, exercises: draft.exercises.filter((_, n) => n !== i) })}>Usuń ćwiczenie {i + 1}</button></div>
        </fieldset>)}
        <button type="button" className="secondary" disabled={draft.exercises.length >= 30} onClick={() => setDraft({ ...draft, exercises: [...draft.exercises, emptyExercise()] })}>+ Dodaj ćwiczenie</button>
      </fieldset>
      <div className="actions workout-save"><button disabled={busy}>{busy ? 'Zapisujemy…' : 'Zapisz trening'}</button><button type="button" className="secondary" disabled={busy} onClick={cancel}>Anuluj</button>{editor && <button type="button" className="quiet danger-text" disabled={busy} onClick={remove}>Usuń trening</button>}</div>
    </form>}
  </section>;
}


function WorkoutPreview({ workout, busy, onEdit, onBack }) {
  const heading = useRef(null);
  useEffect(() => { heading.current?.focus(); }, []);
  return <section className="panel workout-preview" aria-labelledby="workout-preview-title">
    <div className="page-heading"><div><p className="eyebrow">ZAPISANY TRENING</p><h2 id="workout-preview-title" ref={heading} tabIndex={-1}>{readableDate(workout.date)}</h2></div><div className="actions"><button className="secondary" disabled={busy} onClick={onBack}>Wróć do listy</button><button disabled={busy} onClick={onEdit}>Edytuj</button></div></div>
    {workout.exercises.map((exercise, i) => <section className="workout-preview-exercise" key={i}>
      <h3>{exercise.name}</h3>
      <table><caption className="sr-only">Serie ćwiczenia {i + 1}: {exercise.name}</caption><thead><tr><th scope="col">Seria</th><th scope="col">Powtórzenia</th><th scope="col">Ciężar (kg)</th></tr></thead><tbody>
        {exercise.sets.map((set, j) => <tr key={j}><th scope="row">{j + 1}</th><td>{set.repetitions}</td><td>{set.weightKg === '' ? <span className="muted">Nie zapisano</span> : String(set.weightKg).replace('.', ',')}</td></tr>)}
      </tbody></table>
    </section>)}
  </section>;
}
