package com.mesadj.web;

import com.mesadj.audio.AudioManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class MesaDjController {
    private final AudioManager manager;

    public MesaDjController(AudioManager manager) {
        this.manager = manager;
    }

    @GetMapping("/state")
    public Map<String, Object> state() {
        return manager.globalSnapshot();
    }

    @GetMapping("/stems")
    public Object stems() {
        return manager.snapshot();
    }

    @PostMapping("/control")
    public ResponseEntity<?> control(@RequestParam String action,
                                     @RequestParam(required = false) String id) {
        try {
            switch (action.toLowerCase()) {
                case "play" -> manager.globalPlay();
                case "pause" -> manager.globalPause();
                case "stop" -> manager.globalStop();
                case "mute" -> requireId(id, manager::toggleMute);
                case "solo" -> requireId(id, manager::toggleSolo);
                case "seek" -> throw new IllegalArgumentException("Use /api/seek?seconds=...");
                default -> throw new IllegalArgumentException("Ação inválida: " + action);
            }
            return ResponseEntity.ok(manager.globalSnapshot());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/seek")
    public ResponseEntity<?> seek(@RequestParam double seconds) {
        if (seconds < 0) return ResponseEntity.badRequest().body(Map.of("error", "seconds não pode ser negativo"));
        manager.seekAll(Math.round(seconds * 1_000_000));
        return ResponseEntity.ok(manager.globalSnapshot());
    }

    private void requireId(String id, java.util.function.Consumer<String> action) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("id é obrigatório");
        action.accept(id);
    }
}
