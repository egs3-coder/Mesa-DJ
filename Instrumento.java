public final class Instrumento implements Runnable {
    private final String nome;
    private final String som;

    // Todos os campos mutaveis abaixo sao protegidos pelo monitor do proprio objeto
    // (metodos/blocos synchronized).
    private Thread thread;
    private boolean pausado;
    private boolean encerrado;
    private int bpm;
    private long batidas;

    public Instrumento(String nome, int bpm, String som) {
        if (bpm < 30 || bpm > 300) {
            throw new IllegalArgumentException("BPM deve estar entre 30 e 300.");
        }
        this.nome = nome;
        this.bpm = bpm;
        this.som = som;
    }

    public synchronized void iniciar() {
        if (thread != null) {
            return;
        }
        thread = new Thread(this, "Instrumento-" + nome);
        thread.start();
    }

    public synchronized boolean pausar() {
        if (encerrado || pausado) {
            return false;
        }
        pausado = true;

        // Interrompe apenas o sleep atual para que a thread perceba a pausa logo.
        // Nao mata a thread.
        if (thread != null) {
            thread.interrupt();
        }
        return true;
    }

    public synchronized boolean retomar() {
        if (encerrado || !pausado) {
            return false;
        }
        pausado = false;
        notifyAll();
        return true;
    }

    public synchronized boolean encerrar() {
        if (encerrado) {
            return false;
        }
        encerrado = true;
        pausado = false;
        notifyAll();

        // A interrupcao serve apenas para acordar a thread caso esteja em sleep.
        if (thread != null) {
            thread.interrupt();
        }
        return true;
    }

    public synchronized boolean alterarBpm(int novoBpm) {
        if (encerrado) {
            return false;
        }
        if (novoBpm < 30 || novoBpm > 300) {
            throw new IllegalArgumentException("BPM deve estar entre 30 e 300.");
        }
        bpm = novoBpm;

        // Faz o novo BPM surtir efeito sem esperar o sleep antigo acabar.
        if (thread != null) {
            thread.interrupt();
        }
        return true;
    }

    public synchronized Status obterStatus() {
        String estado;
        if (encerrado) {
            estado = "ENCERRADO";
        } else if (pausado) {
            estado = "PAUSADO";
        } else {
            estado = "TOCANDO";
        }
        return new Status(nome, som, estado, bpm, batidas);
    }

    public void aguardarFinalizacao() {
        Thread copia;
        synchronized (this) {
            copia = thread;
        }

        if (copia != null && copia != Thread.currentThread()) {
            try {
                copia.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public void run() {
        while (true) {
            int bpmAtual;

            synchronized (this) {
                // wait() libera o monitor enquanto a thread esta pausada.
                // Assim, comandos como retomar() e encerrar() continuam conseguindo entrar.
                while (pausado && !encerrado) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        // Acordou para reavaliar as flags de controle.
                    }
                }

                if (encerrado) {
                    break;
                }

                bpmAtual = bpm;
                batidas++;
            }

            long intervaloMs = Math.max(50L, 60_000L / bpmAtual);

            try {
                Thread.sleep(intervaloMs);
            } catch (InterruptedException e) {
                // Pausa, mudanca de BPM ou encerramento: volta ao inicio e reavalia o estado.
            }
        }
    }

    public static final class Status {
        private final String nome;
        private final String som;
        private final String estado;
        private final int bpm;
        private final long batidas;

        public Status(String nome, String som, String estado, int bpm, long batidas) {
            this.nome = nome;
            this.som = som;
            this.estado = estado;
            this.bpm = bpm;
            this.batidas = batidas;
        }

        public String getNome() {
            return nome;
        }

        public String getSom() {
            return som;
        }

        public String getEstado() {
            return estado;
        }

        public int getBpm() {
            return bpm;
        }

        public long getBatidas() {
            return batidas;
        }
    }
}
