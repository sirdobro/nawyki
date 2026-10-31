import React, { useEffect, useState } from 'react';
import { createRoot } from 'react-dom/client';
import { describeStatus } from './status.js';
import './style.css';

function App() {
  const [status, setStatus] = useState('loading');
  const [release, setRelease] = useState('');
  const [attempt, setAttempt] = useState(0);
  useEffect(() => {
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), 12000);
    setStatus('loading');
    Promise.all(['/api/health/ready', '/api/status'].map(async (path) => {
      const response = await fetch(path, { signal: controller.signal, cache: 'no-store' });
      if (!response.ok) throw new Error('Service unavailable');
      return response.json();
    })).then(([health, info]) => {
      if (health.status !== 'UP' || info.stage !== 'foundation') throw new Error('Unexpected response');
      setRelease(info.release);
      setStatus('ready');
    }).catch(() => setStatus('unavailable')).finally(() => clearTimeout(timeout));
    return () => { clearTimeout(timeout); controller.abort(); };
  }, [attempt]);
  return <div className="shell">
    <header><a className="brand" href="/" aria-label="Nawyki — strona główna"><span className="brand-mark" aria-hidden="true">n.</span>Nawyki</a><span className="edition">Wersja rozwojowa</span></header>
    <main>
      <section className="hero">
        <p className="eyebrow">TWÓJ DZIENNIK REGULARNOŚCI</p>
        <h1>Małe kroki.<br/><span>Więcej dobrych dni.</span></h1>
        <p className="intro">Miejsce na codzienne nawyki, spokojne budowanie regularności i prosty zapis treningów. Bez presji idealnej serii.</p>
        <div className="connection" role="status" aria-live="polite"><span className={`dot ${status}`} aria-hidden="true"/>{describeStatus(status)}</div>
        {status === 'unavailable' && <button onClick={() => setAttempt(value => value + 1)}>Sprawdź ponownie</button>}
      </section>
      <section className="progress-panel" aria-labelledby="progress-title"><div className="panel-title"><span className="eyebrow">PIERWSZY ETAP</span><span className="phase">01 / fundament</span></div><h2 id="progress-title">Dobre nawyki potrzebują dobrego początku.</h2><p>Podstawy aplikacji są już online. Tworzenie prywatnych dzienników będzie dostępne w kolejnym etapie.</p><ol><li><span className="step-symbol" aria-hidden="true">✓</span><div><strong>Połączenie gotowe</strong><span>Bezpieczny adres, aplikacja i baza danych.</span></div></li><li><span className="step-symbol upcoming" aria-hidden="true">2</span><div><strong>Twój prywatny dziennik</strong><span>W przygotowaniu: dostęp kluczem, bez e-maila i hasła.</span></div></li><li><span className="step-symbol upcoming" aria-hidden="true">3</span><div><strong>Codzienny rytm</strong><span>W przygotowaniu: nawyki, historia i postępy.</span></div></li></ol></section>
      <section className="principles" aria-label="Założenia aplikacji"><div><span>01</span><h2>Po swojemu</h2><p>Codziennie, w wybrane dni albo kilka razy w tygodniu.</p></div><div><span>02</span><h2>Bez zbędnych danych</h2><p>Planowane konta bez nazwiska, adresu e-mail i hasła.</p></div><div><span>03</span><h2>Prosto i czytelnie</h2><p>Wykonanie, historia i postęp — w jednym miejscu.</p></div></section>
    </main><footer><span>Nawyki · projekt studencki</span><span>{release && release !== 'development' ? `Wersja ${release.slice(0, 7)}` : 'Środowisko rozwojowe'}</span></footer>
  </div>;
}
createRoot(document.getElementById('root')).render(<App/>);
