package com.mesadj.audio;

import javax.sound.sampled.*;
import java.nio.file.Path;

public class AudioTrack implements Runnable {
    /*
     * Muitos drivers/mixers do JavaSound não aceitam PCM de 24 bits.
     * Os stems do Heart Peripheral podem vir em 24-bit, então convertemos
     * para PCM_SIGNED 16-bit antes de abrir o Clip.
     */
    private static final float SAMPLE_RATE = 44100.0f;
    private static final int SAMPLE_SIZE_BITS = 16;

    private final String name;
    private final Path file;
    private Clip clip;
    private Thread thread;
    private TrackState state = TrackState.PAUSED;
    private volatile boolean shutdownRequested = false;

    public AudioTrack(String name, Path file) {
        this.name = name;
        this.file = file;
    }

    public void startThread() {
        thread = new Thread(this, "AudioTrack-" + name);
        thread.start();
    }

    @Override
    public void run() {
        try (AudioInputStream originalStream = AudioSystem.getAudioInputStream(file.toFile())) {
            AudioFormat sourceFormat = originalStream.getFormat();
            AudioFormat playbackFormat = getPlaybackFormat(sourceFormat);

            try (AudioInputStream playbackStream = convertIfNecessary(originalStream, sourceFormat, playbackFormat)) {
                DataLine.Info info = new DataLine.Info(Clip.class, playbackFormat);

                if (!AudioSystem.isLineSupported(info)) {
                    throw new LineUnavailableException(
                            "o dispositivo de áudio não suporta " + formatDescription(playbackFormat)
                    );
                }

                clip = (Clip) AudioSystem.getLine(info);
                clip.open(playbackStream);
                clip.loop(Clip.LOOP_CONTINUOUSLY);

                synchronized (this) {
                    state = TrackState.PLAYING;
                    clip.start();
                    notifyAll();
                }

                while (!shutdownRequested) {
                    synchronized (this) {
                        while (state == TrackState.PAUSED && !shutdownRequested) {
                            wait();
                        }

                        if (shutdownRequested) break;

                        if (state == TrackState.PLAYING && !clip.isRunning()) {
                            clip.start();
                        }
                    }
                    Thread.sleep(20);
                }
            }
        } catch (UnsupportedAudioFileException e) {
            System.err.println("[" + name + "] Arquivo de áudio não suportado: " + e.getMessage());
        } catch (LineUnavailableException e) {
            System.err.println("[" + name + "] Saída de áudio indisponível: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            if (!shutdownRequested) {
                System.err.println("[" + name + "] Erro ao reproduzir: " + e.getMessage());
            }
        } finally {
            if (clip != null) {
                clip.close();
            }

            synchronized (this) {
                state = TrackState.STOPPED;
                notifyAll();
            }

            System.out.println("[" + name + "] thread encerrada.");
        }
    }

    /**
     * Mantém sample rate/canais do WAV, mas usa 16-bit PCM_SIGNED,
     * formato muito mais compatível com os mixers JavaSound do Windows.
     */
    private AudioFormat getPlaybackFormat(AudioFormat source) {
        int channels = source.getChannels();

        return new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                source.getSampleRate() > 0 ? source.getSampleRate() : SAMPLE_RATE,
                SAMPLE_SIZE_BITS,
                channels,
                channels * (SAMPLE_SIZE_BITS / 8),
                source.getSampleRate() > 0 ? source.getSampleRate() : SAMPLE_RATE,
                false
        );
    }

    private AudioInputStream convertIfNecessary(
            AudioInputStream sourceStream,
            AudioFormat source,
            AudioFormat target
    ) throws Exception {
        if (sameFormat(source, target)) {
            return sourceStream;
        }

        if (!AudioSystem.isConversionSupported(target, source)) {
            throw new UnsupportedAudioFileException(
                    "não é possível converter " + formatDescription(source)
                            + " para " + formatDescription(target)
            );
        }

        return AudioSystem.getAudioInputStream(target, sourceStream);
    }

    private boolean sameFormat(AudioFormat a, AudioFormat b) {
        return a.getEncoding().equals(b.getEncoding())
                && a.getSampleRate() == b.getSampleRate()
                && a.getSampleSizeInBits() == b.getSampleSizeInBits()
                && a.getChannels() == b.getChannels()
                && a.isBigEndian() == b.isBigEndian();
    }

    private String formatDescription(AudioFormat f) {
        return String.format(
                "%s %.0f Hz, %d bit, %s, %d bytes/frame, %s-endian",
                f.getEncoding(),
                f.getSampleRate(),
                f.getSampleSizeInBits(),
                f.getChannels() == 1 ? "mono" : "stereo",
                f.getFrameSize(),
                f.isBigEndian() ? "big" : "little"
        );
    }

    public synchronized void pauseTrack() {
        if (clip != null && state == TrackState.PLAYING) {
            clip.stop();
            state = TrackState.PAUSED;
            System.out.println("[" + name + "] PAUSADA em " + time());
        }
    }

    public synchronized void resumeTrack() {
        if (clip != null && !shutdownRequested &&
                (state == TrackState.PAUSED || state == TrackState.STOPPED)) {
            state = TrackState.PLAYING;
            clip.start();
            notifyAll();
            System.out.println("[" + name + "] RETOMADA.");
        }
    }

    public synchronized void stopTrack() {
        if (clip != null) {
            clip.stop();
            clip.setMicrosecondPosition(0);
            state = TrackState.STOPPED;
            System.out.println("[" + name + "] PARADA; posição zerada.");
        }
    }

    public synchronized void shutdown() {
        shutdownRequested = true;
        notifyAll();

        if (clip != null) {
            clip.stop();
        }
    }

    public String getName() {
        return name;
    }

    public synchronized TrackState getState() {
        return state;
    }

    public synchronized long getPositionMicroseconds() {
        return clip == null ? 0 : clip.getMicrosecondPosition();
    }

    public synchronized void seekMicroseconds(long microseconds) {
        if (clip == null) return;
        long max = clip.getMicrosecondLength();
        long target = Math.max(0, Math.min(microseconds, max));
        clip.setMicrosecondPosition(target);
    }

    public synchronized long getDurationMicroseconds() {
        return clip == null ? 0 : clip.getMicrosecondLength();
    }

    public boolean isThreadAlive() {
        return thread != null && thread.isAlive();
    }

    private String time() {
        long s = getPositionMicroseconds() / 1_000_000;
        return String.format("%02d:%02d", s / 60, s % 60);
    }
}
