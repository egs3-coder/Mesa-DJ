/**
 * Representa UMA faixa/instrumento da mesa de DJ.
 *
 * Cada objeto Instrumento possui sua propria Thread.
 * Por isso bateria, baixo, synth, guitarra etc. conseguem executar
 * ao mesmo tempo e de forma independente.
 */
public final class Instrumento implements Runnable {

    // Limites aceitos para o BPM informado pelo usuario.
    private static final int BPM_MINIMO = 30;
    private static final int BPM_MAXIMO = 300;

    /**
     * Fator usado apenas para deixar a demonstracao mais lenta e facil de acompanhar.
     *
     * Exemplo:
     * 120 BPM, em tempo real, daria 500 ms entre batidas.
     * Com fator 4, usamos 2000 ms entre batidas.
     *
     * A relacao continua correta para a atividade:
     * BPM maior -> sleep menor -> mais batidas.
     */
    private static final long FATOR_TEMPO_DEMONSTRACAO = 4L;

    // Dados que nao mudam depois que o instrumento e criado.
    private final String nome;
    private final String som;

    /*
     * DADOS COMPARTILHADOS ENTRE THREADS
     * ---------------------------------
     * A Thread do instrumento le esses valores.
     * A Thread principal (comandos do DJ) tambem pode altera-los.
     *
     * Por isso o acesso e protegido com synchronized.
     */
    private Thread thread;
    private boolean pausado;
    private boolean encerrado;
    private int bpm;
    private long batidas;

    /**
     * Construtor do instrumento.
     */
    public Instrumento(String nome, int bpm, String som) {
        validarBpm(bpm);
        this.nome = nome;
        this.bpm = bpm;
        this.som = som;
    }

    /**
     * PASSO 1 - INICIAR A THREAD
     *
     * new Thread(this, ...) cria uma Thread que executara o metodo run().
     * start() pede ao Java para iniciar essa nova Thread.
     */
    public synchronized void iniciar() {
        if (thread != null) {
            return; // Evita iniciar a mesma Thread duas vezes.
        }

        thread = new Thread(this, "Instrumento-" + nome);
        thread.start();
    }

    /**
     * PASSO 2 - PAUSAR SOMENTE ESTE INSTRUMENTO
     *
     * synchronized garante que apenas uma Thread por vez altere
     * o estado pausado/encerrado deste instrumento.
     */
    public synchronized boolean pausar() {
        if (encerrado || pausado) {
            return false;
        }

        pausado = true;

        /*
         * interrupt() NAO mata a Thread.
         * Aqui ele serve apenas para interromper o Thread.sleep atual,
         * fazendo a Thread voltar rapidamente ao inicio do loop e perceber
         * que pausado == true.
         */
        if (thread != null) {
            thread.interrupt();
        }

        return true;
    }

    /**
     * PASSO 3 - RETOMAR O INSTRUMENTO
     *
     * notifyAll() acorda a Thread caso ela esteja parada em wait().
     */
    public synchronized boolean retomar() {
        if (encerrado || !pausado) {
            return false;
        }

        pausado = false;
        notifyAll();
        return true;
    }

    /**
     * PASSO 4 - ENCERRAR A THREAD COM SEGURANCA
     *
     * Nao usamos Thread.stop().
     * Em vez disso, sinalizamos encerrado = true e acordamos a Thread.
     * Ela propria percebe a flag e sai naturalmente do metodo run().
     */
    public synchronized boolean encerrar() {
        if (encerrado) {
            return false;
        }

        encerrado = true;
        pausado = false;
        notifyAll();

        // Se estiver dormindo em sleep(), acorda para verificar encerrado.
        if (thread != null) {
            thread.interrupt();
        }

        return true;
    }

    /**
     * DESAFIO EXTRA - ALTERAR BPM DURANTE A EXECUCAO.
     */
    public synchronized boolean alterarBpm(int novoBpm) {
        if (encerrado) {
            return false;
        }

        validarBpm(novoBpm);
        bpm = novoBpm;

        // Interrompe o sleep antigo para o BPM novo valer imediatamente.
        if (thread != null) {
            thread.interrupt();
        }

        return true;
    }

    /**
     * Cria uma fotografia segura do estado atual para o painel.
     */
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

    /**
     * join() faz a Thread principal esperar esta Thread realmente terminar.
     * E usado no encerramento final do programa.
     */
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

    /**
     * CORACAO DA THREAD.
     *
     * Cada instrumento executa este loop de forma independente.
     */
    @Override
    public void run() {
        while (true) {
            int bpmAtual;

            /*
             * REGIAO CRITICA
             * Apenas uma Thread por vez entra neste bloco para ler/alterar
             * pausado, encerrado, bpm e batidas.
             */
            synchronized (this) {

                /*
                 * Se estiver pausado, wait() faz a Thread realmente esperar.
                 * Diferente de um while vazio, wait() nao fica gastando CPU.
                 *
                 * Outra Thread podera executar retomar() e chamar notifyAll().
                 */
                while (pausado && !encerrado) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        // Apenas acorda e volta a verificar as flags.
                    }
                }

                // Encerramento cooperativo e seguro.
                if (encerrado) {
                    break;
                }

                // Copiamos o BPM atual e registramos uma batida.
                bpmAtual = bpm;
                batidas++;
            }

            /*
             * DESAFIO EXTRA - THREAD.SLEEP CONTROLADO PELO BPM
             *
             * Formula base:
             *     60000 / BPM
             *
             * Para a demonstracao ficar mais lenta, multiplicamos por 4.
             * Assim da tempo de digitar os comandos e acompanhar o painel.
             *
             * Exemplos nesta versao:
             *  60 BPM  -> 4000 ms
             * 120 BPM  -> 2000 ms
             * 180 BPM  -> aproximadamente 1332 ms
             */
            long intervaloBaseMs = 60_000L / bpmAtual;
            long intervaloDemonstracaoMs = intervaloBaseMs * FATOR_TEMPO_DEMONSTRACAO;

            try {
                Thread.sleep(intervaloDemonstracaoMs);
            } catch (InterruptedException e) {
                /*
                 * pause, resume, mudanca de BPM ou encerramento podem acordar
                 * a Thread. Voltamos ao inicio do while e reavaliamos o estado.
                 */
            }
        }
    }

    private static void validarBpm(int bpm) {
        if (bpm < BPM_MINIMO || bpm > BPM_MAXIMO) {
            throw new IllegalArgumentException(
                    "BPM deve estar entre " + BPM_MINIMO + " e " + BPM_MAXIMO + ".");
        }
    }

    /**
     * Objeto simples e imutavel usado pelo painel para exibir o estado.
     */
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
