package com.mesadj;

import com.mesadj.audio.AudioManager;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.nio.file.Path;

@SpringBootApplication
public class MesaDjApplication {
    public static void main(String[] args) {
        System.out.println("""
                ╔════════════════════════════════════════════════════╗
                ║                    MESA DJ                         ║
                ║          Terminal player • Spring Boot             ║
                ║              Concurrent Audio Mixer                ║
                ╚════════════════════════════════════════════════════╝
                """);

        SpringApplication.run(MesaDjApplication.class, args);
    }

    @Bean
    AudioManager audioManager() {
        AudioManager manager = new AudioManager(Path.of("music", "APZX_Amalgamize"));
        try {
            manager.loadTracks();
            if (manager.isEmpty()) {
                System.out.println("⚠ Nenhum WAV encontrado em: " + manager.getFolder().toAbsolutePath());
                System.out.println("  Coloque os stems em music/heart-peripheral/ e reinicie.");
            } else {
                manager.startAll();
                System.out.println("o " + manager.getTracks().size() + " threads de áudio iniciadas.");
            }
        } catch (Exception e) {
            throw new IllegalStateException("Não foi possível inicializar o mixer: " + e.getMessage(), e);
        }
        return manager;
    }
}
