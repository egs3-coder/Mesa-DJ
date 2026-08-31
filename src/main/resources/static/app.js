const state = {
  context: null,
  masterGain: null,
  stemBus: null,
  playlist: [],
  playlistIndex: -1,
  selectedLibraryPath: "",
  songElement: null,
  songSource: null,
  songUrl: "",
  tracks: [],
  eqFilters: [],
  eqGains: [0, 0, 0, 0, 0, 0],
  playing: false,
  raf: null
};

const eqDefaults = [
  { label: "60 Hz", frequency: 60 },
  { label: "170 Hz", frequency: 170 },
  { label: "350 Hz", frequency: 350 },
  { label: "1 kHz", frequency: 1000 },
  { label: "3.5 kHz", frequency: 3500 },
  { label: "10 kHz", frequency: 10000 }
];

const stemProfiles = {
  kick: { type: "lowpass", frequency: 120, q: 1.3, gain: 1.2 },
  caixa: { type: "bandpass", frequency: 220, q: 1.4, gain: 0.9 },
  hats: { type: "highpass", frequency: 6500, q: 0.8, gain: 0.65 },
  surdos: { type: "bandpass", frequency: 95, q: 1.1, gain: 1 },
  "guitarra-b": { type: "bandpass", frequency: 850, q: 0.9, gain: 0.75 },
  "guitarra-s": { type: "bandpass", frequency: 2200, q: 1, gain: 0.75 },
  baixo: { type: "lowpass", frequency: 260, q: 0.9, gain: 0.9 },
  "cordas-piano": { type: "bandpass", frequency: 1400, q: 0.7, gain: 0.7 },
  vocals: { type: "bandpass", frequency: 1600, q: 0.9, gain: 0.85 },
  "lead-vocal": { type: "bandpass", frequency: 2800, q: 1.1, gain: 0.95 },
  "backing-vocals": { type: "bandpass", frequency: 4200, q: 0.9, gain: 0.7 }
};

const nodes = {
  tracks: document.querySelector("#tracks"),
  template: document.querySelector("#trackTemplate"),
  playPause: document.querySelector("#playPause"),
  stop: document.querySelector("#stop"),
  nextSong: document.querySelector("#nextSong"),
  songFiles: document.querySelector("#songFiles"),
  title: document.querySelector("#trackTitle"),
  time: document.querySelector("#timeReadout"),
  masterVolume: document.querySelector("#masterVolume"),
  masterValue: document.querySelector("#masterValue"),
  eqBands: document.querySelector("#eqBands"),
  resetEq: document.querySelector("#resetEq"),
  resetStems: document.querySelector("#resetStems"),
  libraryQuery: document.querySelector("#libraryQuery"),
  searchLibrary: document.querySelector("#searchLibrary"),
  libraryList: document.querySelector("#libraryList")
};

async function boot() {
  const response = await fetch("/api/stems");
  const stems = await response.json();
  renderTracks(stems);
  renderEqualizer();
  bindTransport();
}

function getAudioContext() {
  if (!state.context) {
    state.context = new AudioContext();
    state.masterGain = state.context.createGain();
    state.masterGain.gain.value = Number(nodes.masterVolume.value);
    state.stemBus = state.context.createGain();
    state.stemBus.gain.value = 0.72;
    state.eqFilters = createEqChain(state.context, state.masterGain);
    state.stemBus.connect(state.eqFilters[0]);
    state.masterGain.connect(state.context.destination);
  }
  return state.context;
}

function createEqChain(context, destination) {
  const filters = eqDefaults.map((band, index) => {
    const filter = context.createBiquadFilter();
    filter.type = "peaking";
    filter.frequency.value = band.frequency;
    filter.Q.value = 1;
    filter.gain.value = state.eqGains[index] || 0;
    return filter;
  });
  filters.forEach((filter, index) => filter.connect(filters[index + 1] || destination));
  return filters;
}

function renderTracks(stems) {
  nodes.tracks.innerHTML = "";
  state.tracks = stems.map((stem) => {
    const fragment = nodes.template.content.cloneNode(true);
    const card = fragment.querySelector(".track");
    const group = fragment.querySelector(".group");
    const title = fragment.querySelector("h3");
    const fill = fragment.querySelector(".meter-fill");
    const dbValue = fragment.querySelector(".db-value");
    const mute = fragment.querySelector(".mute");
    const solo = fragment.querySelector(".solo");

    card.dataset.id = stem.id;
    card.classList.toggle("principal", stem.principal);
    group.textContent = stem.groupName;
    title.textContent = stem.displayName;

    const track = {
      ...stem,
      card,
      fill,
      dbValue,
      mute,
      solo,
      muted: false,
      soloed: false
    };

    mute.addEventListener("click", () => {
      track.muted = !track.muted;
      applyAllStemVolumes();
      updateTrackCard(track);
    });

    solo.addEventListener("click", () => {
      track.soloed = !track.soloed;
      applyAllStemVolumes();
      updateAllTrackCards();
    });

    nodes.tracks.appendChild(fragment);
    return track;
  });
}

function renderEqualizer() {
  nodes.eqBands.innerHTML = "";
  eqDefaults.forEach((band, index) => {
    const wrapper = document.createElement("label");
    wrapper.className = "eq-band";
    wrapper.innerHTML = `
      <span>${band.label}</span>
      <input type="range" min="-12" max="12" step="1" value="0" data-band="${index}">
      <strong>0 dB</strong>
    `;
    const input = wrapper.querySelector("input");
    const value = wrapper.querySelector("strong");
    input.addEventListener("input", () => {
      value.textContent = `${input.value} dB`;
      applyEqualizer(index, Number(input.value));
    });
    nodes.eqBands.appendChild(wrapper);
  });
}

function bindTransport() {
  nodes.playPause.addEventListener("click", togglePlay);
  nodes.stop.addEventListener("click", stopAll);
  nodes.nextSong.addEventListener("click", nextSong);
  nodes.resetStems.addEventListener("click", resetStems);
  nodes.searchLibrary.addEventListener("click", searchLibrary);
  nodes.libraryQuery.addEventListener("keydown", (event) => {
    if (event.key === "Enter") {
      searchLibrary();
    }
  });

  nodes.masterVolume.addEventListener("input", () => {
    const value = Number(nodes.masterVolume.value);
    nodes.masterValue.textContent = `${Math.round(value * 100)}%`;
    if (state.masterGain) {
      state.masterGain.gain.value = value;
    }
  });

  nodes.songFiles.addEventListener("change", () => {
    state.playlist = Array.from(nodes.songFiles.files).map((file) => ({ type: "local", file, title: file.name }));
    state.playlistIndex = state.playlist.length ? 0 : -1;
    loadCurrentSong();
  });

  nodes.resetEq.addEventListener("click", () => {
    document.querySelectorAll("#eqBands input").forEach((input) => {
      input.value = "0";
      input.parentElement.querySelector("strong").textContent = "0 dB";
    });
    eqDefaults.forEach((_, index) => applyEqualizer(index, 0));
  });
}

async function searchLibrary() {
  const query = encodeURIComponent(nodes.libraryQuery.value.trim());
  const response = await fetch(`/api/library?q=${query}`);
  const songs = await response.json();
  state.playlist = songs.map((song) => ({ type: "library", ...song }));
  state.playlistIndex = state.playlist.length ? 0 : -1;
  renderLibrary(songs);
}

function renderLibrary(songs) {
  nodes.libraryList.innerHTML = "";
  if (!songs.length) {
    const empty = document.createElement("p");
    empty.className = "empty-library";
    empty.textContent = "Nenhuma musica encontrada. Rode o app com --music.dir apontando para sua pasta.";
    nodes.libraryList.appendChild(empty);
    return;
  }

  songs.forEach((song, index) => {
    const button = document.createElement("button");
    button.type = "button";
    button.className = "song-result";
    button.innerHTML = `<span>${song.title}</span><small>${song.folder}</small>`;
    button.addEventListener("click", () => {
      state.playlistIndex = index;
      loadCurrentSong();
    });
    nodes.libraryList.appendChild(button);
  });
}

async function togglePlay() {
  getAudioContext();
  await state.context.resume();
  state.playing ? pauseAll() : playAll();
}

function loadCurrentSong() {
  const context = getAudioContext();
  const song = state.playlist[state.playlistIndex];
  if (!song) {
    return;
  }

  stopAll();
  disconnectCurrentSong();

  state.songUrl = song.type === "library" ? `/api/library/file?path=${encodeURIComponent(song.path)}` : URL.createObjectURL(song.file);
  state.songElement = new Audio(state.songUrl);
  state.songElement.preload = "auto";
  state.songElement.addEventListener("ended", nextSong);
  state.songSource = context.createMediaElementSource(state.songElement);
  connectSeparationGraph();
  nodes.title.textContent = song.title || song.file?.name || "Musica carregada";
  updateTime();
}

function connectSeparationGraph() {
  const context = getAudioContext();
  state.tracks.forEach((track) => {
    const profile = stemProfiles[track.id] || { type: "bandpass", frequency: 1000, q: 1, gain: 0.7 };
    const filter = context.createBiquadFilter();
    const gain = context.createGain();
    const analyser = context.createAnalyser();

    filter.type = profile.type;
    filter.frequency.value = profile.frequency;
    filter.Q.value = profile.q;
    gain.gain.value = profile.gain;
    analyser.fftSize = 1024;
    analyser.smoothingTimeConstant = 0.78;

    state.songSource.connect(filter);
    filter.connect(gain);
    gain.connect(analyser);
    analyser.connect(state.stemBus);

    track.filter = filter;
    track.gain = gain;
    track.analyser = analyser;
    track.samples = new Uint8Array(analyser.fftSize);
    track.profileGain = profile.gain;
    track.dbValue.textContent = "-60 dB";
    track.fill.style.height = "0%";
    updateTrackCard(track);
  });
  applyAllStemVolumes();
}

function disconnectCurrentSong() {
  state.tracks.forEach((track) => {
    track.filter?.disconnect();
    track.gain?.disconnect();
    track.analyser?.disconnect();
    track.filter = null;
    track.gain = null;
    track.analyser = null;
    track.samples = null;
    track.fill.style.height = "0%";
    track.dbValue.textContent = "-60 dB";
  });

  if (state.songSource) {
    state.songSource.disconnect();
    state.songSource = null;
  }
  if (state.songElement) {
    state.songElement.pause();
    state.songElement = null;
  }
  if (state.songUrl && state.songUrl.startsWith("blob:")) {
    URL.revokeObjectURL(state.songUrl);
  }
  state.songUrl = "";
}

function playAll() {
  if (!state.songElement) {
    nodes.title.textContent = "Carregue uma musica no inicio";
    return;
  }
  state.playing = true;
  nodes.playPause.textContent = "Pause";
  state.songElement.play();
  tick();
}

function pauseAll() {
  state.playing = false;
  nodes.playPause.textContent = "Play";
  state.songElement?.pause();
  cancelAnimationFrame(state.raf);
}

function stopAll() {
  state.playing = false;
  nodes.playPause.textContent = "Play";
  if (state.songElement) {
    state.songElement.pause();
    state.songElement.currentTime = 0;
  }
  cancelAnimationFrame(state.raf);
  updateTime();
  updateMeters(true);
}

function nextSong() {
  if (!state.playlist.length) {
    nodes.title.textContent = "Carregue uma ou mais musicas no inicio";
    return;
  }
  state.playlistIndex = (state.playlistIndex + 1) % state.playlist.length;
  const shouldPlay = state.playing;
  loadCurrentSong();
  if (shouldPlay) {
    playAll();
  }
}

function resetStems() {
  state.tracks.forEach((track) => {
    track.muted = false;
    track.soloed = false;
    updateTrackCard(track);
  });
  applyAllStemVolumes();
}

function applyAllStemVolumes() {
  const hasSolo = state.tracks.some((track) => track.soloed);
  state.tracks.forEach((track) => {
    if (!track.gain) {
      return;
    }
    const shouldSilence = track.muted || (hasSolo && !track.soloed);
    track.gain.gain.value = shouldSilence ? 0 : track.profileGain;
  });
}

function applyEqualizer(index, gain) {
  state.eqGains[index] = gain;
  if (state.eqFilters[index]) {
    state.eqFilters[index].gain.value = gain;
  }
}

function updateTrackCard(track) {
  track.card.classList.toggle("muted", track.muted);
  track.card.classList.toggle("soloed", track.soloed);
  track.card.classList.toggle("active", Boolean(track.analyser));
}

function updateAllTrackCards() {
  state.tracks.forEach(updateTrackCard);
}

function tick() {
  updateTime();
  updateMeters(false);
  state.raf = requestAnimationFrame(tick);
}

function updateMeters(reset) {
  state.tracks.forEach((track) => {
    if (reset || !track.analyser || !track.samples) {
      track.fill.style.height = "0%";
      track.dbValue.textContent = "-60 dB";
      return;
    }

    track.analyser.getByteTimeDomainData(track.samples);
    let sum = 0;
    for (const sample of track.samples) {
      const centered = (sample - 128) / 128;
      sum += centered * centered;
    }
    const rms = Math.sqrt(sum / track.samples.length);
    const db = Math.max(-60, 20 * Math.log10(rms || 0.000001));
    const height = Math.min(100, Math.max(0, ((db + 60) / 60) * 100));
    track.fill.style.height = `${height}%`;
    track.dbValue.textContent = `${Math.round(db)} dB`;
  });
}

function updateTime() {
  nodes.time.textContent = `${formatTime(state.songElement?.currentTime || 0)} / ${formatTime(state.songElement?.duration || 0)}`;
}

function formatTime(seconds) {
  const total = Math.max(0, Math.floor(Number.isFinite(seconds) ? seconds : 0));
  const minutes = String(Math.floor(total / 60)).padStart(2, "0");
  const rest = String(total % 60).padStart(2, "0");
  return `${minutes}:${rest}`;
}

boot();
