(() => {
  'use strict';

  const SERVICES = [
    { key: 'hotels', name: 'hotel-management', url: '/api/v1/hotels' },
    { key: 'users', name: 'user-service', url: '/api/v1/users' },
    { key: 'bookings', name: 'booking-service', url: '/api/v1/bookings' },
  ];
  const POLL_MS = 3000;
  const TIMEOUT_MS = 4000;
  const MAX_TICKS = 30;

  const state = { hotels: [], users: [], bookings: [] };
  const snapshot = { hotels: '', users: '', bookings: '' };
  const beats = { hotels: [], users: [], bookings: [] };
  const ui = { version: '…', pod: '…', ok: null };

  const $ = (selector, root = document) => root.querySelector(selector);

  function el(tag, props = {}, ...children) {
    const node = document.createElement(tag);
    for (const [key, value] of Object.entries(props)) {
      if (key === 'class') node.className = value;
      else if (key === 'text') node.textContent = value;
      else node.setAttribute(key, value);
    }
    node.append(...children);
    return node;
  }

  /* ---------- Мережа ---------- */

  async function timedFetch(url, options = {}) {
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), TIMEOUT_MS);
    try {
      return await fetch(url, { cache: 'no-store', ...options, signal: controller.signal });
    } finally {
      clearTimeout(timer);
    }
  }

  function record(key, ok, ms) {
    const list = beats[key];
    list.push({ ok, ms: Math.round(ms) });
    if (list.length > MAX_TICKS) list.shift();
  }

  async function pollService(service) {
    const started = performance.now();
    try {
      const response = await timedFetch(service.url);
      if (!response.ok) throw new Error('HTTP ' + response.status);
      const data = await response.json();
      record(service.key, true, performance.now() - started);
      const serialized = JSON.stringify(data);
      if (serialized !== snapshot[service.key]) {
        snapshot[service.key] = serialized;
        state[service.key] = data;
      }
    } catch (error) {
      record(service.key, false, performance.now() - started);
    }
  }

  async function pollUi() {
    try {
      const response = await timedFetch('/version.json');
      if (!response.ok) throw new Error('HTTP ' + response.status);
      const data = await response.json();
      ui.version = data.version;
      ui.pod = response.headers.get('X-Pod') || '—';
      ui.ok = true;
    } catch (error) {
      ui.ok = false;
    }
  }

  async function pollAll() {
    await Promise.all([...SERVICES.map(pollService), pollUi()]);
    renderTape();
    renderData();
  }

  /* ---------- Стрічка пульсу ---------- */

  function tapeRow(name, entries, metaText, okNow) {
    const row = el('div', { class: 'tape-row' });
    row.append(el('span', { class: 'dot ' + (okNow === null ? '' : okNow ? 'ok' : 'fail') }));
    const label = el('span', { class: 'tape-name', text: name });
    if (metaText) label.append(el('span', { class: 'tape-meta', text: metaText }));
    row.append(label);
    const ticks = el('span', { class: 'ticks', 'aria-hidden': 'true' });
    for (const entry of entries) ticks.append(el('span', { class: 'tick ' + (entry.ok ? 'ok' : 'fail') }));
    row.append(ticks);
    return row;
  }

  function renderTape() {
    const tape = $('#tape');
    const rows = SERVICES.map((service) => {
      const list = beats[service.key];
      const last = list[list.length - 1];
      return tapeRow(service.name, list, last ? last.ms + ' мс' : '', last ? last.ok : null);
    });
    const uiRow = tapeRow('ui', [], 'v' + ui.version, ui.ok);
    const info = el('div', { class: 'tape-info', text: 'Под UI: ' + ui.pod });
    uiRow.append(info);
    tape.replaceChildren(...rows, uiRow);
  }

  /* ---------- Таблиці ---------- */

  function stars(rating) {
    const value = Math.max(0, Math.min(5, Number(rating) || 0));
    return el('span', { class: 'stars', 'aria-label': 'Рейтинг ' + value + ' з 5', text: '★'.repeat(value) + '☆'.repeat(5 - value) });
  }

  function fillTable(key, rowsData, buildRow) {
    $('#rows-' + key).replaceChildren(...rowsData.map(buildRow));
    $('#empty-' + key).hidden = rowsData.length > 0;
    $('#count-' + key).textContent = String(rowsData.length);
  }

  function nights(from, to) {
    const ms = new Date(to) - new Date(from);
    return Number.isFinite(ms) ? Math.round(ms / 86400000) : '';
  }

  function renderData() {
    fillTable('hotels', state.hotels, (hotel) => el('tr', {},
      el('td', { class: 'num', text: String(hotel.id) }),
      el('td', { text: hotel.name }),
      el('td', { text: hotel.address }),
      el('td', {}, stars(hotel.rating))));

    fillTable('users', state.users, (user) => el('tr', {},
      el('td', { class: 'num', text: String(user.id) }),
      el('td', { text: user.fullName }),
      el('td', { text: user.email })));

    const userName = (id) => (state.users.find((u) => u.id === id) || {}).fullName || 'користувач #' + id;
    const hotelName = (id) => (state.hotels.find((h) => h.id === id) || {}).name || 'готель #' + id;
    fillTable('bookings', state.bookings, (booking) => el('tr', {},
      el('td', { class: 'num', text: String(booking.id) }),
      el('td', { text: userName(booking.userId) }),
      el('td', { text: hotelName(booking.hotelId) }),
      el('td', { text: booking.checkIn }),
      el('td', { text: booking.checkOut }),
      el('td', { class: 'num', text: String(nights(booking.checkIn, booking.checkOut)) })));

    fillSelect($('#select-users'), state.users, (u) => u.id + ' · ' + u.fullName);
    fillSelect($('#select-hotels'), state.hotels, (h) => h.id + ' · ' + h.name);
    const missing = state.users.length === 0 || state.hotels.length === 0;
    $('#hint-bookings').hidden = !missing;
    $('#submit-bookings').disabled = missing;
  }

  function fillSelect(select, items, label) {
    const previous = select.value;
    select.replaceChildren(...items.map((item) => el('option', { value: String(item.id), text: label(item) })));
    if (previous && items.some((item) => String(item.id) === previous)) select.value = previous;
  }

  /* ---------- Вкладки ---------- */

  function showView(name) {
    for (const tab of document.querySelectorAll('.tabs [role="tab"]')) {
      const active = tab.dataset.view === name;
      tab.setAttribute('aria-selected', String(active));
      tab.tabIndex = active ? 0 : -1;
      $('#view-' + tab.dataset.view).hidden = !active;
    }
    history.replaceState(null, '', '#' + name);
  }

  function initTabs() {
    const tabs = [...document.querySelectorAll('.tabs [role="tab"]')];
    tabs.forEach((tab, index) => {
      tab.addEventListener('click', () => showView(tab.dataset.view));
      tab.addEventListener('keydown', (event) => {
        if (event.key !== 'ArrowRight' && event.key !== 'ArrowLeft') return;
        const next = tabs[(index + (event.key === 'ArrowRight' ? 1 : tabs.length - 1)) % tabs.length];
        next.focus();
        showView(next.dataset.view);
      });
    });
    const initial = location.hash.slice(1);
    if (SERVICES.some((s) => s.key === initial)) showView(initial);
  }

  /* ---------- Форми ---------- */

  let toastTimer;
  function toast(text) {
    const node = $('#toast');
    node.textContent = text;
    node.hidden = false;
    node.classList.remove('show');
    void node.offsetWidth;
    node.classList.add('show');
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => { node.hidden = true; }, 2600);
  }

  function showMessage(form, text) {
    const box = $('.msg', form);
    box.textContent = text || '';
    box.hidden = !text;
  }

  function todayIso() {
    const now = new Date();
    return new Date(now.getTime() - now.getTimezoneOffset() * 60000).toISOString().slice(0, 10);
  }

  const FORMS = {
    hotels: {
      url: '/api/v1/hotels',
      done: 'Готель додано',
      read(form) {
        const data = new FormData(form);
        return { name: data.get('name').trim(), address: data.get('address').trim(), rating: Number(data.get('rating')) };
      },
      check(v) {
        if (!v.name) return 'Вкажіть назву готелю.';
        if (!v.address) return 'Вкажіть адресу готелю.';
        if (!(v.rating >= 1 && v.rating <= 5)) return 'Рейтинг має бути від 1 до 5.';
        return '';
      },
    },
    users: {
      url: '/api/v1/users',
      done: 'Користувача додано',
      read(form) {
        const data = new FormData(form);
        return { fullName: data.get('fullName').trim(), email: data.get('email').trim() };
      },
      check(v) {
        if (v.fullName.length < 2 || v.fullName.length > 50) return "Ім'я має бути від 2 до 50 символів.";
        if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v.email)) return 'Вкажіть коректний email, наприклад ivan@example.com.';
        return '';
      },
    },
    bookings: {
      url: '/api/v1/bookings',
      done: 'Бронювання створено',
      read(form) {
        const data = new FormData(form);
        return {
          userId: Number(data.get('userId')),
          hotelId: Number(data.get('hotelId')),
          checkIn: data.get('checkIn'),
          checkOut: data.get('checkOut'),
        };
      },
      check(v) {
        if (!v.userId || !v.hotelId) return 'Оберіть користувача і готель.';
        if (!v.checkIn || !v.checkOut) return 'Вкажіть дати заїзду й виїзду.';
        if (v.checkIn < todayIso()) return 'Дата заїзду не може бути в минулому.';
        if (v.checkOut <= v.checkIn) return 'Дата виїзду має бути пізніше за дату заїзду.';
        return '';
      },
    },
  };

  function explain(status, key) {
    if (status === 400) return 'Сервер відхилив дані. Перевірте значення полів.';
    if (status === 502 || status === 503 || status === 504) return 'Сервіс тимчасово недоступний. Спробуйте ще раз за кілька секунд.';
    if (key === 'bookings') return 'Не вдалося створити бронювання. Переконайтеся, що користувач і готель існують, і повторіть спробу.';
    return 'Не вдалося зберегти запис (помилка ' + status + '). Повторіть спробу.';
  }

  function initForms() {
    for (const [key, config] of Object.entries(FORMS)) {
      const form = $('#form-' + key);
      form.addEventListener('submit', async (event) => {
        event.preventDefault();
        showMessage(form, '');
        const values = config.read(form);
        const problem = config.check(values);
        if (problem) return showMessage(form, problem);

        const button = $('button[type="submit"]', form);
        button.disabled = true;
        try {
          const response = await timedFetch(config.url, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(values),
          });
          if (!response.ok) return showMessage(form, explain(response.status, key));
          if (key !== 'bookings') form.reset();
          toast(config.done);
          await pollAll();
        } catch (error) {
          showMessage(form, 'Немає зв\u2019язку із сервером. Перевірте підключення і повторіть спробу.');
        } finally {
          button.disabled = key === 'bookings' && (state.users.length === 0 || state.hotels.length === 0);
        }
      });
    }
    const checkIn = $('#form-bookings [name="checkIn"]');
    const checkOut = $('#form-bookings [name="checkOut"]');
    checkIn.min = todayIso();
    checkOut.min = todayIso();
    checkIn.addEventListener('change', () => { if (checkIn.value) checkOut.min = checkIn.value; });
  }

  /* ---------- Старт ---------- */

  initTabs();
  initForms();
  renderTape();
  pollAll();
  setInterval(pollAll, POLL_MS);
})();
