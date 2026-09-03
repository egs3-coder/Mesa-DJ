package com.mesadj.audio;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;
import jakarta.annotation.PreDestroy;

public class AudioManager {
    private final Path folder;
    private final Map<String, AudioTrack> tracks = new LinkedHashMap<>();
    private final Set<String> muted = new HashSet<>();
    private String soloId;
    private Map<String, Boolean> stateBeforeSolo = new HashMap<>();

    public AudioManager(Path folder) { this.folder = folder; }

    public synchronized void loadTracks() throws IOException {
        Files.createDirectories(folder);
        tracks.clear();
        try (Stream<Path> files = Files.list(folder)) {
            files.filter(p -> p.toString().toLowerCase().endsWith(".wav"))
                 .sorted()
                 .forEach(p -> {
                     String name = p.getFileName().toString().replaceFirst("(?i)\\.wav$", "");
                     tracks.put(normalize(name), new AudioTrack(name, p));
                 });
        }
    }

    public synchronized boolean isEmpty() { return tracks.isEmpty(); }
    public Path getFolder() { return folder; }
    public synchronized Collection<AudioTrack> getTracks() { return List.copyOf(tracks.values()); }

    public synchronized void startAll() {
        System.out.println("Criando " + tracks.size() + " threads de áudio...");
        tracks.values().forEach(AudioTrack::startThread);
    }

    public synchronized AudioTrack findTrack(String target) {
        String n = normalize(target);
        if (tracks.containsKey(n)) return tracks.get(n);
        return tracks.entrySet().stream().filter(e -> e.getKey().contains(n)).map(Map.Entry::getValue).findFirst().orElse(null);
    }

    public synchronized String idOf(AudioTrack t) { return normalize(t.getName()); }
    public synchronized String getSoloId() { return soloId; }
    public synchronized boolean isMuted(AudioTrack t) { return muted.contains(idOf(t)); }

    public synchronized void globalPlay() {
        for (AudioTrack t : tracks.values()) if (!isMuted(t) && (soloId == null || idOf(t).equals(soloId))) t.resumeTrack();
    }

    public synchronized void globalPause() { tracks.values().forEach(AudioTrack::pauseTrack); }
    public synchronized void globalStop() { tracks.values().forEach(AudioTrack::stopTrack); }

    /** Move every clip to the same position, preserving the current play/pause state. */
    public synchronized void seekAll(long microseconds) {
        long target = Math.max(0, microseconds);
        for (AudioTrack t : tracks.values()) t.seekMicroseconds(target);
    }

    /** Mute is intentionally implemented as pausing the corresponding audio thread. */
    public synchronized void toggleMute(String target) {
        AudioTrack t = findTrack(target);
        if (t == null) return;
        String id = idOf(t);
        if (muted.remove(id)) {
            if (soloId == null || soloId.equals(id)) t.resumeTrack();
        } else {
            muted.add(id);
            t.pauseTrack();
        }
    }

    /** Solo pauses every other thread and restores the previous state when toggled off. */
    public synchronized void toggleSolo(String target) {
        AudioTrack t = findTrack(target);
        if (t == null) return;
        String id = idOf(t);
        if (id.equals(soloId)) {
            soloId = null;
            for (AudioTrack track : tracks.values()) {
                String tid = idOf(track);
                boolean wasPlaying = stateBeforeSolo.getOrDefault(tid, false);
                if (muted.contains(tid) || !wasPlaying) track.pauseTrack(); else track.resumeTrack();
            }
            stateBeforeSolo.clear();
            return;
        }
        stateBeforeSolo.clear();
        for (AudioTrack track : tracks.values()) stateBeforeSolo.put(idOf(track), track.getState() == TrackState.PLAYING);
        soloId = id;
        for (AudioTrack track : tracks.values()) {
            if (track == t && !muted.contains(id)) track.resumeTrack();
            else track.pauseTrack();
        }
    }

    public synchronized Map<String,Object> snapshot(AudioTrack t, boolean principal) {
        Map<String,Object> m = new LinkedHashMap<>();
        String id = idOf(t);
        m.put("id", id);
        m.put("name", t.getName());
        m.put("state", t.getState().name());
        m.put("muted", muted.contains(id));
        m.put("soloed", id.equals(soloId));
        m.put("principal", principal);
        m.put("positionMicros", t.getPositionMicroseconds());
        m.put("durationMicros", t.getDurationMicroseconds());
        m.put("threadAlive", t.isThreadAlive());
        return m;
    }

    public synchronized List<Map<String,Object>> snapshot() {
        List<Map<String,Object>> result = new ArrayList<>();
        int i = 0;
        for (AudioTrack t : tracks.values()) result.add(snapshot(t, i++ == 0));
        return result;
    }

    public synchronized Map<String,Object> globalSnapshot() {
        List<Map<String,Object>> list = snapshot();
        long position = 0, duration = 0;
        for (Map<String,Object> m : list) {
            if (Boolean.TRUE.equals(m.get("principal"))) {
                position = ((Number)m.get("positionMicros")).longValue();
                duration = ((Number)m.get("durationMicros")).longValue();
            }
        }
        boolean playing = list.stream().anyMatch(m -> "PLAYING".equals(m.get("state")) && Boolean.TRUE.equals(m.get("threadAlive")));
        Map<String,Object> out = new LinkedHashMap<>();
        out.put("playing", playing);
        out.put("soloId", soloId);
        out.put("positionMicros", position);
        out.put("durationMicros", duration);
        out.put("tracks", list);
        return out;
    }

    public synchronized boolean executeCommand(String input) {
        String[] p = input.trim().split("\\s+");
        if (p.length == 0 || p[0].isBlank()) return true;
        switch (p[0].toUpperCase()) {
            case "HELP" -> help();
            case "LIST" -> list();
            case "STATUS" -> status();
            case "PAUSE" -> { if (p.length > 1 && p[1].equalsIgnoreCase("ALL")) globalPause(); else action(p, "pause"); }
            case "RESUME", "PLAY" -> { if (p.length > 1 && p[1].equalsIgnoreCase("ALL")) globalPlay(); else action(p, "resume"); }
            case "STOP" -> { if (p.length > 1 && p[1].equalsIgnoreCase("ALL")) globalStop(); else action(p, "stop"); }
            case "EXIT", "QUIT", "SAIR" -> { return false; }
            default -> System.out.println("Comando desconhecido. Digite HELP.");
        }
        return true;
    }

    private void action(String[] p, String action) {
        if (p.length < 2) { System.out.println("Uso: " + action.toUpperCase() + " <faixa|ALL>"); return; }
        AudioTrack t = findTrack(p[1]);
        if (t == null) System.out.println("Faixa '" + p[1] + "' não encontrada. Use LIST.");
        else switch (action) { case "pause" -> t.pauseTrack(); case "resume" -> t.resumeTrack(); case "stop" -> t.stopTrack(); }
    }

    private void list() { System.out.println("\\nFAIXAS:"); tracks.values().forEach(t -> System.out.println(" - " + t.getName() + " [" + normalize(t.getName()) + "]")); }
    private void status() { System.out.println("\\nSTATUS:"); tracks.values().forEach(t -> System.out.printf("%-28s %-8s %s%n", t.getName(), t.getState(), time(t.getPositionMicroseconds()))); }
    private void help() { System.out.println("LIST | STATUS | PAUSE <faixa> | RESUME <faixa> | STOP <faixa> | PAUSE ALL | RESUME ALL | STOP ALL | EXIT"); }
    private String time(long us) { long s = us / 1_000_000; return String.format("%02d:%02d", s/60, s%60); }

    @PreDestroy
    public synchronized void shutdown() {
        tracks.values().forEach(AudioTrack::shutdown);
        for (AudioTrack t : tracks.values()) while (t.isThreadAlive()) try { Thread.sleep(20); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
    }
    private String normalize(String s) { return s.toLowerCase(Locale.ROOT).replace(".wav", "").replaceAll("[^a-z0-9]+", " ").trim().replaceAll("\\s+", " "); }
}
