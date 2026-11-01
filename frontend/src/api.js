let csrf;
export async function api(path, method = 'GET', body, retry = true) {
  const headers = { Accept: 'application/json' };
  if (method !== 'GET') {
    csrf ??= fetch('/api/auth/csrf', { cache: 'no-store' }).then(r => { if (!r.ok) throw Error('Nie udało się połączyć.'); return r.json(); }).catch(e => { csrf = undefined; throw e; });
    const token = await csrf; headers[token.headerName] = token.token;
  }
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  const response = await fetch(path, { method, headers, credentials: 'same-origin', cache: 'no-store', body: body === undefined ? undefined : JSON.stringify(body) });
  const data = await response.json().catch(() => ({}));
  if (response.status === 403 && data.code === 'CSRF_INVALID' && retry) { csrf = undefined; return api(path, method, body, false); }
  if (!response.ok) throw Error(data.errors?.map(e => e.message).join(' ') || data.message || (response.status === 401 ? 'Sesja wygasła lub klucz jest niepoprawny. Zaloguj się ponownie.' : 'Nie udało się wykonać operacji. Spróbuj ponownie.'));
  return data;
}
