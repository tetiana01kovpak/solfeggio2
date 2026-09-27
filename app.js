'use strict';

const FREQUENCIES = [
  { hz: 174, label: 'Eases pain & stress' },
  { hz: 285, label: 'Enhances healing & regeneration' },
  { hz: 396, label: 'Releases fear & guilt' },
  { hz: 417, label: 'Facilitates change & letting go' },
  { hz: 528, label: 'Encourages healing & transformation' },
  { hz: 639, label: 'Supports connection & harmony' },
  { hz: 728, label: 'Claimed to destroy parasites in the body', note: 'A sound tone cannot do this.' },
  { hz: 852, label: 'Fosters intuition & awareness' },
];

const FADE_S = 0.08;
const TIMER_FADE_S = 3;

let ctx = null;
let master = null;
let analyser = null;
let current = null;
let timerId = null;
let timerEnd = 0;

function ensureAudio() {
  if (!ctx) {
    const AC = window.AudioContext || window.webkitAudioContext;
    ctx = new AC();
    master = ctx.createGain();
    master.gain.value = volume();
    analyser = ctx.createAnalyser();
    analyser.fftSize = 2048;
    master.connect(analyser);
    analyser.connect(ctx.destination);
    requestAnimationFrame(draw);
  }
  if (ctx.state !== 'running') ctx.resume();
  return ctx;
}

function volume() {
  return Number(document.getElementById('volume').value) / 100;
}

function play(index) {
  ensureAudio();
  stop();
  const now = ctx.currentTime;
  const osc = ctx.createOscillator();
  const gain = ctx.createGain();
  osc.type = 'sine';
  osc.frequency.value = FREQUENCIES[index].hz;
  gain.gain.setValueAtTime(0, now);
  gain.gain.linearRampToValueAtTime(1, now + FADE_S);
  osc.connect(gain).connect(master);
  osc.start(now);
  current = { index, osc, gain };
  startTimer();
  render();
}

function stop(fade = FADE_S) {
  clearTimer();
  if (!current) return;
  const { osc, gain } = current;
  const now = ctx.currentTime;
  gain.gain.cancelScheduledValues(now);
  gain.gain.setValueAtTime(gain.gain.value, now);
  gain.gain.linearRampToValueAtTime(0, now + fade);
  osc.stop(now + fade + 0.02);
  osc.onended = () => gain.disconnect();
  current = null;
  render();
}

function toggle(index) {
  if (current && current.index === index) stop();
  else play(index);
}

function startTimer() {
  const minutes = Number(document.getElementById('timer').value);
  if (!minutes) return;
  timerEnd = Date.now() + minutes * 60000;
  timerId = setInterval(() => {
    if (Date.now() >= timerEnd - TIMER_FADE_S * 1000) stop(TIMER_FADE_S);
    else renderRemaining();
  }, 250);
  renderRemaining();
}

function clearTimer() {
  if (timerId) clearInterval(timerId);
  timerId = null;
  document.getElementById('remaining').textContent = '';
}

function renderRemaining() {
  const s = Math.max(0, Math.ceil((timerEnd - Date.now()) / 1000));
  const mm = Math.floor(s / 60);
  const ss = String(s % 60).padStart(2, '0');
  document.getElementById('remaining').textContent = `· ${mm}:${ss} left`;
}

function buildCards() {
  const root = document.getElementById('cards');
  FREQUENCIES.forEach((f, i) => {
    const card = document.createElement('button');
    card.type = 'button';
    card.className = 'card';
    card.dataset.index = i;
    card.setAttribute('aria-pressed', 'false');
    card.innerHTML =
      `<span class="key">${i + 1}</span>` +
      `<span class="hz">${f.hz} Hz</span>` +
      `<span class="label">${f.note ? '' : 'Traditional association: '}${escapeHtml(f.label)}</span>` +
      (f.note ? `<span class="note">${f.note}</span>` : '');
    card.addEventListener('click', () => toggle(i));
    root.appendChild(card);
  });
}

function escapeHtml(text) {
  return text.replace(/&/g, '&amp;').replace(/</g, '&lt;');
}

function render() {
  document.querySelectorAll('.card').forEach((card) => {
    const on = current && current.index === Number(card.dataset.index);
    card.classList.toggle('playing', !!on);
    card.setAttribute('aria-pressed', on ? 'true' : 'false');
  });
  document.getElementById('readout').textContent =
    current ? `${FREQUENCIES[current.index].hz} Hz sine` : 'Silence';
}

function draw() {
  const canvas = document.getElementById('scope');
  const g = canvas.getContext('2d');
  const data = new Uint8Array(analyser.fftSize);
  const w = canvas.width;
  const h = canvas.height;
  const color = getComputedStyle(document.body).getPropertyValue('--ink').trim() || '#23402b';

  (function frame() {
    analyser.getByteTimeDomainData(data);
    g.clearRect(0, 0, w, h);
    g.lineWidth = 2;
    g.strokeStyle = color;
    g.beginPath();
    const n = data.length / 2;
    for (let i = 0; i < n; i++) {
      const x = (i / (n - 1)) * w;
      const y = (data[i] / 255) * h;
      if (i === 0) g.moveTo(x, y);
      else g.lineTo(x, y);
    }
    g.stroke();
    requestAnimationFrame(frame);
  })();
}

function init() {
  buildCards();

  const vol = document.getElementById('volume');
  vol.addEventListener('input', () => {
    document.getElementById('volume-out').textContent = `${vol.value}%`;
    if (master) master.gain.setTargetAtTime(volume(), ctx.currentTime, 0.02);
  });

  document.getElementById('timer').addEventListener('change', () => {
    if (current) {
      clearTimer();
      startTimer();
    }
  });

  document.addEventListener('keydown', (e) => {
    if (e.metaKey || e.ctrlKey || e.altKey) return;
    if (/^[1-8]$/.test(e.key)) {
      e.preventDefault();
      toggle(Number(e.key) - 1);
    } else if (e.key === ' ') {
      e.preventDefault();
      stop();
    }
  });

  const resume = () => { if (ctx && ctx.state !== 'running') ctx.resume(); };
  document.addEventListener('touchstart', resume, { passive: true });
  document.addEventListener('touchend', resume, { passive: true });
  document.addEventListener('visibilitychange', () => { if (!document.hidden) resume(); });
}

if (typeof document !== 'undefined') init();
if (typeof module !== 'undefined') module.exports = { FREQUENCIES };
