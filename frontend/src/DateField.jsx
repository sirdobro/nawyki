import React, { useEffect, useId, useRef, useState } from 'react';
import { calendarDays, dateInputError, formatDate, formatMonth, monthNames, parseDateInput } from './dates';

// The visible text and picker are independent of browser/OS date ordering.
export default function DateField({ label, value, onChange, max, required = false, disabled = false, monthOnly = false, className = '' }) {
  const id = useId();
  const format = monthOnly ? formatMonth : formatDate;
  const [text, setText] = useState(format(value));
  const [touched, setTouched] = useState(false);
  const [open, setOpen] = useState(false);
  const [pickerMonth, setPickerMonth] = useState((value || max).slice(0, 7));
  const emitted = useRef(value);
  const input = useRef(null);
  const root = useRef(null);
  const trigger = useRef(null);
  const error = dateInputError(text, { monthOnly, max, required });
  useEffect(() => {
    if (value !== emitted.current) { setText(format(value)); emitted.current = value; setTouched(false); }
  }, [value, monthOnly]);
  useEffect(() => { input.current?.setCustomValidity(error); }, [error]);
  useEffect(() => {
    if (!open) return;
    const closeOutside = event => { if (!root.current?.contains(event.target)) setOpen(false); };
    document.addEventListener('pointerdown', closeOutside);
    return () => document.removeEventListener('pointerdown', closeOutside);
  }, [open]);
  function type(next) {
    setText(next);
    const parsed = parseDateInput(next, monthOnly);
    emitted.current = parsed && !dateInputError(next, { monthOnly, max, required }) ? parsed : '';
    onChange(emitted.current);
  }
  function choose(iso) {
    setText(format(iso)); emitted.current = iso; onChange(iso); setTouched(false); setOpen(false); trigger.current?.focus();
  }
  const [year, month] = pickerMonth.split('-').map(Number);
  const lastYear = Number(max.slice(0, 4));
  return <div className={`date-field ${className}`} ref={root} onKeyDown={event => {
    if (event.key === 'Escape' && open) { event.preventDefault(); event.stopPropagation(); setOpen(false); trigger.current?.focus(); }
  }}>
    <label htmlFor={id}>{label}</label>
    <div className="date-controls">
      <input id={id} ref={input} type="text" inputMode="numeric" autoComplete="off" spellCheck="false" maxLength={monthOnly ? 7 : 10}
        value={text} placeholder={monthOnly ? 'MM-RRRR' : 'DD-MM-RRRR'} required={required} disabled={disabled}
        aria-describedby={`${id}-format${touched && error ? ` ${id}-error` : ''}`} aria-invalid={touched && Boolean(error)}
        onChange={event => type(event.target.value)} onBlur={() => { setTouched(true); const parsed = parseDateInput(text, monthOnly); if (parsed && !error) setText(format(parsed)); }} />
      <button ref={trigger} type="button" className="secondary date-trigger" disabled={disabled} aria-label={`${monthOnly ? 'Wybierz miesiąc' : 'Otwórz kalendarz'}: ${label}`}
        aria-expanded={open} aria-controls={`${id}-picker`} aria-haspopup="dialog" onClick={() => { setPickerMonth((value || max).slice(0, 7)); setOpen(!open); }}>▦</button>
    </div>
    <span className="sr-only" id={`${id}-format`}>{monthOnly ? 'Miesiąc i czterocyfrowy rok, MM-RRRR.' : 'Dzień, miesiąc i czterocyfrowy rok, DD-MM-RRRR.'}</span>
    {touched && error && <span className="date-error" id={`${id}-error`} aria-live="polite">{error}</span>}
    {open && <div className="date-picker" id={`${id}-picker`} role="dialog" aria-label={`${monthOnly ? 'Wybór miesiąca' : 'Kalendarz'}: ${label}`}>
      <div className="picker-heading">
        {!monthOnly && <label><span className="sr-only">Miesiąc kalendarza</span><select autoFocus value={month} onChange={event => setPickerMonth(`${String(year).padStart(4, '0')}-${String(event.target.value).padStart(2, '0')}`)}>
          {monthNames.map((name, index) => <option key={name} value={index + 1}>{name}</option>)}
        </select></label>}
        <label><span className={monthOnly ? '' : 'sr-only'}>Rok kalendarza</span><input type="number" min={1} max={lastYear} value={year} autoFocus={monthOnly} onChange={event => {
          const next = Number(event.target.value);
          if (Number.isInteger(next) && next >= 1 && next <= lastYear) setPickerMonth(`${String(next).padStart(4, '0')}-${String(month).padStart(2, '0')}`);
        }} /></label>
      </div>
      {monthOnly ? <div className="picker-months">{monthNames.map((name, index) => {
        const iso = `${String(year).padStart(4, '0')}-${String(index + 1).padStart(2, '0')}`;
        return <button key={name} type="button" className="secondary" disabled={iso > max} aria-pressed={iso === value} onClick={() => choose(iso)}>{name}</button>;
      })}</div> : <>
        <div className="picker-weekdays" aria-hidden="true">{['Pn', 'Wt', 'Śr', 'Cz', 'Pt', 'So', 'Nd'].map(day => <span key={day}>{day}</span>)}</div>
        <div className="picker-days">{calendarDays(pickerMonth).map((iso, index) => <button key={iso || index} type="button" className={`secondary ${iso.slice(0, 7) !== pickerMonth ? 'outside-month' : ''}`}
          disabled={!iso || iso > max} aria-label={iso ? formatDate(iso) : 'Poza zakresem'} aria-pressed={iso === value} onClick={() => choose(iso)}>{iso ? Number(iso.slice(8)) : '·'}</button>)}</div>
      </>}
      <button type="button" className="quiet picker-close" onClick={() => { setOpen(false); trigger.current?.focus(); }}>Zamknij kalendarz</button>
    </div>}
  </div>;
}
