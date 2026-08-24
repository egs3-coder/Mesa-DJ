import java.util.Scanner;

public final class Main {
    public static void main(String[] args) {
        MesaDJ mesa = new MesaDJ();
        Scanner scanner = new Scanner(System.in);

        mesa.iniciar();
        mesa.imprimirMensagem("Mesa DJ iniciada. Digite 'help' para ver os comandos.");

        boolean continuar = true;

        try {
            while (continuar && scanner.hasNextLine()) {
                String linha = scanner.nextLine().trim();
                if (linha.isEmpty()) {
                    mesa.imprimirMensagem("Digite um comando. Use 'help' para ajuda.");
                    continue;
                }

                continuar = processarComando(mesa, linha);
            }
        } finally {
            mesa.encerrarTodos();
            scanner.close();
            System.out.println("\nMesa DJ encerrada com seguranca. Todas as threads finalizaram.");
        }
    }

    private static boolean processarComando(MesaDJ mesa, String linha) {
        String[] partes = linha.split("\\s+");
        String comando = partes[0].toLowerCase();

        try {
            if (comando.equals("pause") || comando.equals("pausar")) {
                exigirArgumentos(partes, 2, "pause <instrumento>");
                boolean ok = mesa.pausar(partes[1]);
                mesa.imprimirMensagem(ok
                        ? "Faixa '" + partes[1] + "' pausada. As demais continuam tocando."
                        : "Nao foi possivel pausar '" + partes[1] + "'. Verifique se existe e se esta tocando.");

            } else if (comando.equals("resume") || comando.equals("retomar") || comando.equals("play")) {
                exigirArgumentos(partes, 2, "resume <instrumento>");
                boolean ok = mesa.retomar(partes[1]);
                mesa.imprimirMensagem(ok
                        ? "Faixa '" + partes[1] + "' retomada."
                        : "Nao foi possivel retomar '" + partes[1] + "'.");

            } else if (comando.equals("stop") || comando.equals("parar")) {
                exigirArgumentos(partes, 2, "stop <instrumento>");
                boolean ok = mesa.encerrar(partes[1]);
                mesa.imprimirMensagem(ok
                        ? "Thread da faixa '" + partes[1] + "' recebeu sinal de encerramento."
                        : "Nao foi possivel encerrar '" + partes[1] + "'.");

            } else if (comando.equals("bpm")) {
                exigirArgumentos(partes, 3, "bpm <instrumento> <30-300>");
                int bpm = Integer.parseInt(partes[2]);
                boolean ok = mesa.alterarBpm(partes[1], bpm);
                mesa.imprimirMensagem(ok
                        ? "BPM de '" + partes[1] + "' alterado para " + bpm + "."
                        : "Instrumento '" + partes[1] + "' nao encontrado ou ja encerrado.");

            } else if (comando.equals("add") || comando.equals("adicionar")) {
                exigirArgumentos(partes, 2, "add <instrumento> [bpm]");
                int bpm = partes.length >= 3 ? Integer.parseInt(partes[2]) : 120;
                boolean ok = mesa.adicionarInstrumento(partes[1], bpm);
                mesa.imprimirMensagem(ok
                        ? "Instrumento '" + partes[1] + "' adicionado e sua thread foi iniciada."
                        : "Ja existe um instrumento chamado '" + partes[1] + "'.");

            } else if (comando.equals("pauseall")) {
                mesa.pausarTodos();
                mesa.imprimirMensagem("Todas as faixas foram pausadas.");

            } else if (comando.equals("resumeall")) {
                mesa.retomarTodos();
                mesa.imprimirMensagem("Todas as faixas pausadas foram retomadas.");

            } else if (comando.equals("status")) {
                mesa.exibirPainelAgora();

            } else if (comando.equals("panel")) {
                exigirArgumentos(partes, 2, "panel on|off");
                if (partes[1].equalsIgnoreCase("on")) {
                    mesa.ligarPainel();
                    mesa.imprimirMensagem("Painel automatico ligado (atualiza a cada 2 segundos).");
                } else if (partes[1].equalsIgnoreCase("off")) {
                    mesa.desligarPainel();
                    mesa.imprimirMensagem("Painel automatico desligado.");
                } else {
                    mesa.imprimirMensagem("Use: panel on ou panel off");
                }

            } else if (comando.equals("clear")) {
                exigirArgumentos(partes, 2, "clear on|off");
                if (partes[1].equalsIgnoreCase("on")) {
                    mesa.setLimparTela(true);
                    mesa.imprimirMensagem("Limpeza ANSI ligada.");
                } else if (partes[1].equalsIgnoreCase("off")) {
                    mesa.setLimparTela(false);
                    mesa.imprimirMensagem("Limpeza ANSI desligada (util em consoles de IDE)." );
                } else {
                    mesa.imprimirMensagem("Use: clear on ou clear off");
                }

            } else if (comando.equals("help") || comando.equals("ajuda")) {
                mesa.imprimirMensagem(mesa.ajuda());

            } else if (comando.equals("exit") || comando.equals("sair")) {
                return false;

            } else {
                mesa.imprimirMensagem("Comando desconhecido: '" + comando + "'. Use 'help'.");
            }

        } catch (IllegalArgumentException e) {
            mesa.imprimirMensagem("Erro: " + e.getMessage());
        }

        return true;
    }

    private static void exigirArgumentos(String[] partes, int minimo, String uso) {
        if (partes.length < minimo) {
            throw new IllegalArgumentException("Uso correto: " + uso);
        }
    }
}
