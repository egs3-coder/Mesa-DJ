import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Controlador da Mesa DJ.
 *
 * Responsabilidades:
 * 1. Guardar os instrumentos.
 * 2. Iniciar/pausar/retomar/parar cada instrumento.
 * 3. Criar novos instrumentos durante a execucao.
 * 4. Manter a Thread extra do painel de status.
 */
public final class MesaDJ {

    /** O enunciado pede atualizacao do status a cada 2 segundos. */
    private static final long INTERVALO_PAINEL_MS = 2000L;

    /*
     * ConcurrentHashMap foi usado porque duas Threads podem acessar a colecao:
     * - Thread principal adiciona/busca instrumentos.
     * - Thread do painel percorre os instrumentos para mostrar o status.
     */
    private final Map<String, Instrumento> instrumentos =
            new ConcurrentHashMap<String, Instrumento>();

    /** Evita que duas Threads imprimam textos Java ao mesmo tempo. */
    private final Object consoleLock = new Object();

    /** volatile torna as mudancas imediatamente visiveis entre Threads. */
    private volatile boolean mesaIniciada;
    private volatile boolean painelExecutando;

    /*
     * IMPORTANTE PARA O SEU PROBLEMA:
     * Nesta V2 a limpeza automatica com ANSI comeca DESLIGADA.
     * Assim o painel de 2 em 2 segundos NAO apaga o comando que voce esta digitando.
     *
     * Se quiser demonstrar o efeito pedido no desafio extra, use:
     *     clear on
     *
     * Para voltar ao modo mais confortavel de digitar:
     *     clear off
     */
    private volatile boolean limparTela = false;

    private Thread painelThread;

    /**
     * Instrumentos iniciais.
     * O BPM continua podendo ser alterado em tempo real pelo comando bpm.
     */
    public MesaDJ() {
        adicionarInstrumento("bateria", 120);
        adicionarInstrumento("baixo", 90);
        adicionarInstrumento("synth", 105);
    }

    /** Inicia todas as Threads dos instrumentos e depois a Thread do painel. */
    public void iniciar() {
        mesaIniciada = true;

        for (Instrumento instrumento : instrumentos.values()) {
            instrumento.iniciar();
        }

        iniciarPainel();
    }

    /**
     * DESAFIO EXTRA - adicionar instrumento enquanto a musica esta tocando.
     * Exemplo: add guitarra 140
     */
    public boolean adicionarInstrumento(String nomeOriginal, int bpm) {
        String nome = normalizarNome(nomeOriginal);

        if (nome.isEmpty()) {
            throw new IllegalArgumentException("Nome do instrumento nao pode ser vazio.");
        }

        Instrumento novo = new Instrumento(nome, bpm, somPadrao(nome));

        /*
         * putIfAbsent e atomico no ConcurrentHashMap:
         * so adiciona se ainda nao existir um instrumento com esse nome.
         */
        Instrumento existente = instrumentos.putIfAbsent(nome, novo);

        if (existente != null) {
            return false;
        }

        // Se a mesa ja estiver funcionando, a nova faixa comeca imediatamente.
        if (mesaIniciada) {
            novo.iniciar();
        }

        return true;
    }

    public boolean pausar(String nome) {
        Instrumento instrumento = buscar(nome);
        return instrumento != null && instrumento.pausar();
    }

    public boolean retomar(String nome) {
        Instrumento instrumento = buscar(nome);
        return instrumento != null && instrumento.retomar();
    }

    public boolean encerrar(String nome) {
        Instrumento instrumento = buscar(nome);
        return instrumento != null && instrumento.encerrar();
    }

    public boolean alterarBpm(String nome, int bpm) {
        Instrumento instrumento = buscar(nome);
        return instrumento != null && instrumento.alterarBpm(bpm);
    }

    public void pausarTodos() {
        for (Instrumento instrumento : instrumentos.values()) {
            instrumento.pausar();
        }
    }

    public void retomarTodos() {
        for (Instrumento instrumento : instrumentos.values()) {
            instrumento.retomar();
        }
    }

    /**
     * Encerramento geral seguro:
     * 1. Para o painel.
     * 2. Sinaliza encerramento para cada instrumento.
     * 3. Usa join() para esperar cada Thread realmente terminar.
     */
    public void encerrarTodos() {
        mesaIniciada = false;
        pararPainel();

        for (Instrumento instrumento : instrumentos.values()) {
            instrumento.encerrar();
        }

        for (Instrumento instrumento : instrumentos.values()) {
            instrumento.aguardarFinalizacao();
        }
    }

    /**
     * Mostra o painel completo.
     *
     * Quando clear on esta ativo, limpa e reescreve a tela,
     * exatamente como o desafio extra pede.
     *
     * Quando clear off esta ativo (padrao desta V2), nao apaga o texto que
     * o usuario esta digitando, facilitando muito o uso do programa.
     */
    public void exibirPainelAgora() {
        List<Instrumento.Status> status = obterStatusOrdenado();

        synchronized (consoleLock) {
            if (limparTela) {
                limparConsoleANSI();
            } else {
                System.out.println();
            }

            System.out.println("============================================================");
            System.out.println("                 MESA DJ - THREADS JAVA");
            System.out.println("============================================================");
            System.out.printf("%-16s %-11s %-7s %-10s %-10s%n",
                    "INSTRUMENTO", "ESTADO", "BPM", "BATIDAS", "SOM");
            System.out.println("------------------------------------------------------------");

            for (Instrumento.Status s : status) {
                System.out.printf("%-16s %-11s %-7d %-10d %-10s%n",
                        s.getNome(), s.getEstado(), s.getBpm(), s.getBatidas(), s.getSom());
            }

            System.out.println("------------------------------------------------------------");
            System.out.println("Comandos principais:");
            System.out.println("  pause bateria       | resume bateria       | stop bateria");
            System.out.println("  bpm bateria 180     | add guitarra 140");
            System.out.println("  pauseall            | resumeall            | status");
            System.out.println("  panel on|off        | clear on|off          | help | exit");
            System.out.println("------------------------------------------------------------");
            System.out.println("DICA: clear OFF evita apagar o comando enquanto voce digita.");
            System.out.println("============================================================");
            System.out.print("dj> ");
            System.out.flush();
        }
    }

    /** Imprime uma mensagem de resposta aos comandos do DJ. */
    public void imprimirMensagem(String mensagem) {
        synchronized (consoleLock) {
            System.out.println();
            System.out.println(mensagem);
            System.out.print("dj> ");
            System.out.flush();
        }
    }

    public void setLimparTela(boolean limparTela) {
        this.limparTela = limparTela;
    }

    public boolean isLimparTela() {
        return limparTela;
    }

    public void ligarPainel() {
        if (!painelExecutando) {
            iniciarPainel();
        }
    }

    public void desligarPainel() {
        pararPainel();
    }

    /** Texto mostrado pelo comando help. */
    public String ajuda() {
        return "\nCOMANDOS DISPONIVEIS\n"
                + "  pause <instrumento>          Pausa apenas uma faixa.\n"
                + "  resume <instrumento>         Retoma uma faixa pausada.\n"
                + "  play <instrumento>           Mesmo efeito de resume.\n"
                + "  stop <instrumento>           Encerra definitivamente a Thread da faixa.\n"
                + "  bpm <instrumento> <30-300>   Altera o BPM e, portanto, o Thread.sleep.\n"
                + "  add <instrumento> [bpm]      Cria uma nova Thread durante a execucao.\n"
                + "  pauseall                     Pausa todas as faixas.\n"
                + "  resumeall                    Retoma todas as faixas pausadas.\n"
                + "  status                       Exibe o painel imediatamente.\n"
                + "  panel on|off                 Liga/desliga a Thread do painel de 2 segundos.\n"
                + "  clear on|off                 Liga/desliga a limpeza e reescrita do console.\n"
                + "  help                         Mostra esta ajuda.\n"
                + "  exit                         Encerra tudo com seguranca.\n"
                + "\nEXEMPLOS\n"
                + "  pause bateria\n"
                + "  resume bateria\n"
                + "  bpm synth 180\n"
                + "  add guitarra 140\n"
                + "  clear on\n";
    }

    private Instrumento buscar(String nome) {
        return instrumentos.get(normalizarNome(nome));
    }

    /** Cria uma lista de status e ordena alfabeticamente para o painel. */
    private List<Instrumento.Status> obterStatusOrdenado() {
        List<Instrumento.Status> lista = new ArrayList<Instrumento.Status>();

        for (Instrumento instrumento : instrumentos.values()) {
            lista.add(instrumento.obterStatus());
        }

        Collections.sort(lista, new Comparator<Instrumento.Status>() {
            @Override
            public int compare(Instrumento.Status a, Instrumento.Status b) {
                return a.getNome().compareTo(b.getNome());
            }
        });

        return lista;
    }

    /**
     * DESAFIO EXTRA - THREAD DO PAINEL.
     *
     * Ela e independente das Threads dos instrumentos e da Thread principal.
     * A cada 2 segundos chama exibirPainelAgora().
     */
    private synchronized void iniciarPainel() {
        if (painelExecutando) {
            return;
        }

        painelExecutando = true;

        painelThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (painelExecutando) {
                    exibirPainelAgora();

                    try {
                        Thread.sleep(INTERVALO_PAINEL_MS);
                    } catch (InterruptedException e) {
                        // Acorda para verificar se painelExecutando mudou para false.
                    }
                }
            }
        }, "Painel-Status");

        // Daemon evita que esta Thread sozinha mantenha a JVM aberta no final.
        painelThread.setDaemon(true);
        painelThread.start();
    }

    /** Para a Thread do painel com uma flag + interrupt(). */
    private synchronized void pararPainel() {
        painelExecutando = false;

        if (painelThread != null) {
            painelThread.interrupt();
        }
    }

    /** Sequencia ANSI que limpa o console e posiciona o cursor no inicio. */
    private static void limparConsoleANSI() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    /** Som textual usado apenas para representar o instrumento no painel. */
    private static String somPadrao(String nome) {
        if (nome.contains("bateria")) {
            return "BUM-TSS";
        }
        if (nome.contains("baixo")) {
            return "DUM-DUM";
        }
        if (nome.contains("synth") || nome.contains("teclado")) {
            return "WAAAH";
        }
        if (nome.contains("guitarra")) {
            return "TRIM";
        }
        if (nome.contains("piano")) {
            return "PLIM";
        }
        return "TUM";
    }

    /**
     * Normaliza o nome para aceitar, por exemplo, BATERIA, bateria ou "Báteria".
     */
    public static String normalizarNome(String texto) {
        if (texto == null) {
            return "";
        }

        String semAcentos = Normalizer.normalize(texto.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");

        return semAcentos.toLowerCase(Locale.ROOT);
    }
}
