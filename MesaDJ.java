import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class MesaDJ {
    private final Map<String, Instrumento> instrumentos = new ConcurrentHashMap<String, Instrumento>();
    private final Object consoleLock = new Object();

    private volatile boolean mesaIniciada;
    private volatile boolean painelExecutando;
    private volatile boolean limparTela = true;
    private Thread painelThread;

    public MesaDJ() {
        adicionarInstrumento("bateria", 120);
        adicionarInstrumento("baixo", 90);
        adicionarInstrumento("synth", 105);
    }

    public void iniciar() {
        mesaIniciada = true;
        for (Instrumento instrumento : instrumentos.values()) {
            instrumento.iniciar();
        }
        iniciarPainel();
    }

    public boolean adicionarInstrumento(String nomeOriginal, int bpm) {
        String nome = normalizarNome(nomeOriginal);
        if (nome.isEmpty()) {
            throw new IllegalArgumentException("Nome do instrumento nao pode ser vazio.");
        }

        Instrumento novo = new Instrumento(nome, bpm, somPadrao(nome));
        Instrumento existente = instrumentos.putIfAbsent(nome, novo);

        if (existente != null) {
            return false;
        }

        // Se a mesa ja estiver em execucao, o novo instrumento entra tocando imediatamente.
        // Isto independe de o painel automatico estar ligado ou desligado.
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

    public void exibirPainelAgora() {
        List<Instrumento.Status> status = obterStatusOrdenado();

        synchronized (consoleLock) {
            if (limparTela) {
                limparConsoleANSI();
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
            System.out.println("Comandos: pause/pausar, resume/retomar, stop/parar, bpm,");
            System.out.println("          add/adicionar, pauseall, resumeall, status,");
            System.out.println("          panel on|off, clear on|off, help, exit");
            System.out.println("Ex.: add guitarra 140 | pause bateria | bpm synth 180");
            System.out.println("============================================================");
            System.out.print("dj> ");
            System.out.flush();
        }
    }

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

    public String ajuda() {
        return "\nCOMANDOS DISPONIVEIS\n"
                + "  pause <instrumento>      Pausa somente uma faixa.\n"
                + "  resume <instrumento>     Retoma somente uma faixa.\n"
                + "  stop <instrumento>       Encerra definitivamente a thread da faixa.\n"
                + "  bpm <instrumento> <30-300>  Altera o BPM (muda o Thread.sleep).\n"
                + "  add <instrumento> [bpm]  Adiciona uma nova thread durante a execucao.\n"
                + "  pauseall                 Pausa todas as faixas.\n"
                + "  resumeall                Retoma todas as faixas.\n"
                + "  status                   Mostra o painel imediatamente.\n"
                + "  panel on|off             Liga/desliga a thread do painel automatico.\n"
                + "  clear on|off             Liga/desliga a limpeza ANSI do console.\n"
                + "  help                     Mostra esta ajuda.\n"
                + "  exit                     Encerra todas as threads com seguranca.\n";
    }

    private Instrumento buscar(String nome) {
        return instrumentos.get(normalizarNome(nome));
    }

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
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        // Reavalia painelExecutando no proximo ciclo.
                    }
                }
            }
        }, "Painel-Status");
        painelThread.setDaemon(true);
        painelThread.start();
    }

    private synchronized void pararPainel() {
        painelExecutando = false;
        if (painelThread != null) {
            painelThread.interrupt();
        }
    }

    private static void limparConsoleANSI() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

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

    public static String normalizarNome(String texto) {
        if (texto == null) {
            return "";
        }
        String semAcentos = Normalizer.normalize(texto.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return semAcentos.toLowerCase(Locale.ROOT);
    }
}
