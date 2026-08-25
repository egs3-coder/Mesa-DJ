import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

public class MesaDJ {

    private final Map<String, FaixaInstrumento> instrumentos = new ConcurrentHashMap<>();

    public void adicionarInstrumento(String nome, int bpm) {
        String chave = nome.toLowerCase();

        if (instrumentos.containsKey(chave)) {
            mensagem("O instrumento \"" + nome + "\" ja existe.");
            return;
        }

        FaixaInstrumento instrumento = new FaixaInstrumento(nome, bpm);
        instrumentos.put(chave, instrumento);
        instrumento.start();

        mensagem("Instrumento \"" + nome + "\" adicionado com sucesso! BPM: " + instrumento.getBpm());
    }

    public void pausarInstrumento(String nome) {
        FaixaInstrumento instrumento = buscarInstrumento(nome);
        if (instrumento != null) {
            instrumento.pausar();
        }
    }

    public void retomarInstrumento(String nome) {
        FaixaInstrumento instrumento = buscarInstrumento(nome);
        if (instrumento != null) {
            instrumento.retomar();
        }
    }

    public void encerrarInstrumento(String nome) {
        FaixaInstrumento instrumento = buscarInstrumento(nome);
        if (instrumento != null) {
            instrumento.encerrar();
        }
    }

    public void alterarBPM(String nome, int novoBpm) {
        FaixaInstrumento instrumento = buscarInstrumento(nome);
        if (instrumento != null) {
            instrumento.setBpm(novoBpm);
        }
    }

    private FaixaInstrumento buscarInstrumento(String nome) {
        FaixaInstrumento instrumento = instrumentos.get(nome.toLowerCase());
        if (instrumento == null) {
            mensagem("Instrumento \"" + nome + "\" nao encontrado.");
        }
        return instrumento;
    }

    public void imprimirPainel() {
        synchronized (System.out) {
            // Limpa o terminal em consoles com suporte ANSI.
            System.out.print("\033[H\033[2J");
            System.out.flush();

            System.out.println("====================================================");
            System.out.println("               MESA DJ - THREADS");
            System.out.println("====================================================");
            System.out.printf("%-18s %-13s %-8s %-10s%n", "INSTRUMENTO", "STATUS", "BPM", "BATIDAS");
            System.out.println("----------------------------------------------------");

            for (FaixaInstrumento instrumento : instrumentos.values()) {
                System.out.printf(
                        "%-18s %-13s %-8d %-10d%n",
                        instrumento.getNomeInstrumento(),
                        instrumento.getStatus(),
                        instrumento.getBpm(),
                        instrumento.getBatidas()
                );
            }

            System.out.println("====================================================");
            System.out.println("Comandos:");
            System.out.println("  pause nome       -> pausar instrumento");
            System.out.println("  resume nome      -> retomar instrumento");
            System.out.println("  stop nome        -> encerrar instrumento");
            System.out.println("  bpm nome valor   -> alterar BPM");
            System.out.println("  add nome         -> adicionar instrumento (120 BPM)");
            System.out.println("  add nome bpm     -> adicionar instrumento com BPM");
            System.out.println("  status           -> atualizar painel");
            System.out.println("  help             -> mostrar ajuda");
            System.out.println("  exit             -> encerrar programa");
            System.out.println("====================================================");
            System.out.print("DJ > ");
            System.out.flush();
        }
    }

    private void mostrarAjuda() {
        synchronized (System.out) {
            System.out.println();
            System.out.println("================= AJUDA =================");
            System.out.println("pause bateria      Pausa a bateria.");
            System.out.println("resume bateria     Retoma a bateria.");
            System.out.println("stop baixo         Encerra somente o baixo.");
            System.out.println("bpm bateria 180    Altera o BPM da bateria.");
            System.out.println("add guitarra       Adiciona guitarra a 120 BPM.");
            System.out.println("add piano 90       Adiciona piano a 90 BPM.");
            System.out.println("status             Exibe o painel agora.");
            System.out.println("exit               Encerra todas as Threads com seguranca.");
            System.out.println("=========================================");
            System.out.print("DJ > ");
            System.out.flush();
        }
    }

    public void encerrarTodos() {
        for (FaixaInstrumento instrumento : instrumentos.values()) {
            instrumento.encerrar();
        }

        // Aguarda brevemente o termino das Threads para uma saida organizada.
        for (FaixaInstrumento instrumento : instrumentos.values()) {
            try {
                instrumento.join(1500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private static void mensagem(String texto) {
        synchronized (System.out) {
            System.out.println("\n" + texto);
            System.out.print("DJ > ");
            System.out.flush();
        }
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        MesaDJ mesa = new MesaDJ();

        // Instrumentos iniciais: cada um inicia sua propria Thread.
        mesa.adicionarInstrumento("Bateria", 120);
        mesa.adicionarInstrumento("Baixo", 90);
        mesa.adicionarInstrumento("Synth", 70);

        // Desafio extra: Thread exclusiva para o painel de status.
        PainelStatus painel = new PainelStatus(mesa);
        painel.start();

        mesa.imprimirPainel();

        boolean programaRodando = true;

        while (programaRodando && scanner.hasNextLine()) {
            String linha = scanner.nextLine().trim();

            if (linha.isEmpty()) {
                System.out.print("DJ > ");
                System.out.flush();
                continue;
            }

            String[] partes = linha.split("\\s+");
            String comando = partes[0].toLowerCase();

            switch (comando) {
                case "pause":
                    if (partes.length < 2) {
                        mensagem("Uso correto: pause nome");
                    } else {
                        mesa.pausarInstrumento(partes[1]);
                    }
                    break;

                case "resume":
                    if (partes.length < 2) {
                        mensagem("Uso correto: resume nome");
                    } else {
                        mesa.retomarInstrumento(partes[1]);
                    }
                    break;

                case "stop":
                    if (partes.length < 2) {
                        mensagem("Uso correto: stop nome");
                    } else {
                        mesa.encerrarInstrumento(partes[1]);
                    }
                    break;

                case "bpm":
                    if (partes.length < 3) {
                        mensagem("Uso correto: bpm nome valor");
                    } else {
                        try {
                            int bpm = Integer.parseInt(partes[2]);
                            mesa.alterarBPM(partes[1], bpm);
                        } catch (NumberFormatException e) {
                            mensagem("O BPM precisa ser um numero inteiro.");
                        }
                    }
                    break;

                case "add":
                    if (partes.length < 2) {
                        mensagem("Uso correto: add nome [bpm]");
                    } else {
                        int bpmInicial = 120;
                        if (partes.length >= 3) {
                            try {
                                bpmInicial = Integer.parseInt(partes[2]);
                            } catch (NumberFormatException e) {
                                mensagem("BPM invalido. Exemplo: add guitarra 150");
                                break;
                            }
                        }
                        mesa.adicionarInstrumento(partes[1], bpmInicial);
                    }
                    break;

                case "status":
                    mesa.imprimirPainel();
                    break;

                case "help":
                    mesa.mostrarAjuda();
                    break;

                case "exit":
                    programaRodando = false;
                    break;

                default:
                    mensagem("Comando desconhecido. Digite help.");
            }
        }

        mensagem("Encerrando Mesa DJ...");
        painel.encerrar();
        mesa.encerrarTodos();

        try {
            painel.join(1500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        scanner.close();

        synchronized (System.out) {
            System.out.println("\nTodas as Threads foram finalizadas.");
            System.out.println("Programa encerrado!");
        }
    }
}

/**
 * Representa uma faixa musical. Cada instancia e uma Thread independente.
 */
class FaixaInstrumento extends Thread {

    private final String nomeInstrumento;

    // Estes estados sao lidos/alterados em secoes sincronizadas.
    private boolean pausado = false;
    private boolean ativo = true;
    private int bpm;
    private long batidas = 0;

    public FaixaInstrumento(String nomeInstrumento, int bpm) {
        this.nomeInstrumento = nomeInstrumento;
        setBpmInicial(bpm);
        setName("Thread-" + nomeInstrumento);
    }

    @Override
    public void run() {
        while (true) {
            try {
                int bpmAtual;

                synchronized (this) {
                    // Se estiver pausada, a Thread espera sem ficar consumindo CPU.
                    while (pausado && ativo) {
                        wait();
                    }

                    // Encerramento seguro: a propria Thread percebe o estado e termina.
                    if (!ativo) {
                        break;
                    }

                    bpmAtual = bpm;
                    batidas++;
                }

                synchronized (System.out) {
                    System.out.println("\n♪ [" + nomeInstrumento + "] tocando...");
                    System.out.print("DJ > ");
                    System.out.flush();
                }

                // Desafio extra: BPM simulado pelo intervalo de sleep.
                // 60 BPM = 1000 ms; 120 BPM = 500 ms; 180 BPM ~= 333 ms.
                long intervalo = 60000L / bpmAtual;
                Thread.sleep(intervalo);

            } catch (InterruptedException e) {
                // interrupt() apenas acorda a Thread de sleep()/wait().
                // O loop volta e verifica novamente pausado/ativo/BPM.
            }
        }

        synchronized (System.out) {
            System.out.println("\n■ [" + nomeInstrumento + "] Thread encerrada.");
            System.out.print("DJ > ");
            System.out.flush();
        }
    }

    public synchronized void pausar() {
        if (!ativo) {
            System.out.println("\n" + nomeInstrumento + " ja foi encerrado.");
            return;
        }

        if (pausado) {
            System.out.println("\n" + nomeInstrumento + " ja esta pausado.");
            return;
        }

        pausado = true;
        interrupt();
        System.out.println("\n[PAUSE] " + nomeInstrumento + " pausado.");
    }

    public synchronized void retomar() {
        if (!ativo) {
            System.out.println("\n" + nomeInstrumento + " ja foi encerrado.");
            return;
        }

        if (!pausado) {
            System.out.println("\n" + nomeInstrumento + " ja esta tocando.");
            return;
        }

        pausado = false;
        notifyAll();
        System.out.println("\n[PLAY] " + nomeInstrumento + " retomado.");
    }

    public synchronized void encerrar() {
        if (!ativo) {
            return;
        }

        // Nao usamos Thread.stop(), que e inseguro e obsoleto.
        ativo = false;
        pausado = false;
        notifyAll();
        interrupt();
    }

    public synchronized void setBpm(int novoBpm) {
        if (!ativo) {
            System.out.println("\nNao e possivel alterar o BPM de " + nomeInstrumento + " porque a faixa foi encerrada.");
            return;
        }

        if (novoBpm < 30 || novoBpm > 300) {
            System.out.println("\nBPM deve ficar entre 30 e 300.");
            return;
        }

        bpm = novoBpm;
        interrupt();
        System.out.println("\nBPM de " + nomeInstrumento + " alterado para " + bpm + ".");
    }

    private void setBpmInicial(int bpm) {
        if (bpm >= 30 && bpm <= 300) {
            this.bpm = bpm;
        } else {
            this.bpm = 120;
        }
    }

    public synchronized int getBpm() {
        return bpm;
    }

    public synchronized long getBatidas() {
        return batidas;
    }

    public String getNomeInstrumento() {
        return nomeInstrumento;
    }

    public synchronized String getStatus() {
        if (!ativo) {
            return "ENCERRADO";
        }
        if (pausado) {
            return "PAUSADO";
        }
        return "TOCANDO";
    }
}

/**
 * Thread independente que mostra o status de todas as faixas a cada 2 segundos.
 */
class PainelStatus extends Thread {

    private final MesaDJ mesa;
    private boolean ativo = true;

    public PainelStatus(MesaDJ mesa) {
        this.mesa = mesa;
        setName("Thread-Painel-Status");
    }

    @Override
    public void run() {
        while (estaAtivo()) {
            try {
                Thread.sleep(2000);
                if (estaAtivo()) {
                    mesa.imprimirPainel();
                }
            } catch (InterruptedException e) {
                // Permite sair rapidamente do sleep quando o programa e encerrado.
            }
        }
    }

    private synchronized boolean estaAtivo() {
        return ativo;
    }

    public synchronized void encerrar() {
        ativo = false;
        interrupt();
    }
}
