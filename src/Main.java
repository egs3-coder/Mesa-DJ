import java.util.Scanner;

/**
 * Classe principal.
 *
 * Esta e a Thread que representa o DJ/usuario.
 * Ela fica lendo comandos do teclado enquanto as outras Threads
 * (bateria, baixo, synth, painel...) continuam executando em paralelo.
 */
public final class Main {

    public static void main(String[] args) {
        // PASSO 1 - Criar a mesa.
        MesaDJ mesa = new MesaDJ();

        // PASSO 2 - Scanner sera usado para ler os comandos digitados.
        Scanner scanner = new Scanner(System.in);

        // PASSO 3 - Iniciar as Threads dos instrumentos e do painel.
        mesa.iniciar();
        mesa.imprimirMensagem(
                "Mesa DJ iniciada. Digite 'help' para ver os comandos. "
                        + "Nesta V2, clear esta OFF para nao apagar sua digitacao.");

        boolean continuar = true;

        try {
            /*
             * Enquanto o programa estiver ativo, a Thread principal fica aqui
             * esperando comandos. As outras Threads continuam funcionando.
             */
            while (continuar && scanner.hasNextLine()) {
                String linha = scanner.nextLine().trim();

                if (linha.isEmpty()) {
                    mesa.imprimirMensagem("Digite um comando. Use 'help' para ajuda.");
                    continue;
                }

                continuar = processarComando(mesa, linha);
            }

        } finally {
            /*
             * Mesmo se ocorrer algum problema, o finally garante que tentaremos
             * encerrar todas as Threads de forma controlada.
             */
            mesa.encerrarTodos();
            scanner.close();

            System.out.println(
                    "\nMesa DJ encerrada com seguranca. Todas as threads finalizaram.");
        }
    }

    /**
     * Interpreta o texto digitado pelo usuario.
     */
    private static boolean processarComando(MesaDJ mesa, String linha) {
        String[] partes = linha.split("\\s+");
        String comando = partes[0].toLowerCase();

        try {
            // pause bateria
            if (comando.equals("pause") || comando.equals("pausar")) {
                exigirArgumentos(partes, 2, "pause <instrumento>");

                boolean ok = mesa.pausar(partes[1]);

                mesa.imprimirMensagem(ok
                        ? "Faixa '" + partes[1] + "' pausada. As demais continuam tocando."
                        : "Nao foi possivel pausar '" + partes[1]
                                + "'. Verifique se existe e se esta tocando.");

            // resume bateria / play bateria
            } else if (comando.equals("resume")
                    || comando.equals("retomar")
                    || comando.equals("play")) {

                exigirArgumentos(partes, 2, "resume <instrumento>");

                boolean ok = mesa.retomar(partes[1]);

                mesa.imprimirMensagem(ok
                        ? "Faixa '" + partes[1] + "' retomada."
                        : "Nao foi possivel retomar '" + partes[1] + "'.");

            // stop synth
            } else if (comando.equals("stop") || comando.equals("parar")) {
                exigirArgumentos(partes, 2, "stop <instrumento>");

                boolean ok = mesa.encerrar(partes[1]);

                mesa.imprimirMensagem(ok
                        ? "Thread da faixa '" + partes[1]
                                + "' recebeu sinal de encerramento."
                        : "Nao foi possivel encerrar '" + partes[1] + "'.");

            // bpm bateria 180
            } else if (comando.equals("bpm")) {
                exigirArgumentos(partes, 3, "bpm <instrumento> <30-300>");

                int bpm = Integer.parseInt(partes[2]);
                boolean ok = mesa.alterarBpm(partes[1], bpm);

                mesa.imprimirMensagem(ok
                        ? "BPM de '" + partes[1] + "' alterado para " + bpm + "."
                        : "Instrumento '" + partes[1]
                                + "' nao encontrado ou ja encerrado.");

            // add guitarra 140
            } else if (comando.equals("add") || comando.equals("adicionar")) {
                exigirArgumentos(partes, 2, "add <instrumento> [bpm]");

                // Se o usuario nao informar BPM, usamos 120.
                int bpm = partes.length >= 3
                        ? Integer.parseInt(partes[2])
                        : 120;

                boolean ok = mesa.adicionarInstrumento(partes[1], bpm);

                mesa.imprimirMensagem(ok
                        ? "Instrumento '" + partes[1]
                                + "' adicionado e sua Thread foi iniciada."
                        : "Ja existe um instrumento chamado '" + partes[1] + "'.");

            // pauseall
            } else if (comando.equals("pauseall")) {
                mesa.pausarTodos();
                mesa.imprimirMensagem("Todas as faixas foram pausadas.");

            // resumeall
            } else if (comando.equals("resumeall")) {
                mesa.retomarTodos();
                mesa.imprimirMensagem("Todas as faixas pausadas foram retomadas.");

            // status
            } else if (comando.equals("status")) {
                mesa.exibirPainelAgora();

            // panel on / panel off
            } else if (comando.equals("panel")) {
                exigirArgumentos(partes, 2, "panel on|off");

                if (partes[1].equalsIgnoreCase("on")) {
                    mesa.ligarPainel();
                    mesa.imprimirMensagem(
                            "Painel automatico ligado: atualiza a cada 2 segundos.");

                } else if (partes[1].equalsIgnoreCase("off")) {
                    mesa.desligarPainel();
                    mesa.imprimirMensagem("Painel automatico desligado.");

                } else {
                    mesa.imprimirMensagem("Use: panel on ou panel off");
                }

            // clear on / clear off
            } else if (comando.equals("clear")) {
                exigirArgumentos(partes, 2, "clear on|off");

                if (partes[1].equalsIgnoreCase("on")) {
                    mesa.setLimparTela(true);
                    mesa.imprimirMensagem(
                            "Limpeza ANSI ligada. O painel sera limpo e reescrito a cada 2 segundos.");

                } else if (partes[1].equalsIgnoreCase("off")) {
                    mesa.setLimparTela(false);
                    mesa.imprimirMensagem(
                            "Limpeza ANSI desligada. Agora sua digitacao nao sera apagada pelo painel.");

                } else {
                    mesa.imprimirMensagem("Use: clear on ou clear off");
                }

            // help
            } else if (comando.equals("help") || comando.equals("ajuda")) {
                mesa.imprimirMensagem(mesa.ajuda());

            // exit
            } else if (comando.equals("exit") || comando.equals("sair")) {
                return false;

            } else {
                mesa.imprimirMensagem(
                        "Comando desconhecido: '" + comando + "'. Use 'help'.");
            }

        } catch (NumberFormatException e) {
            mesa.imprimirMensagem(
                    "Erro: o BPM precisa ser um numero inteiro. Ex.: bpm bateria 120");

        } catch (IllegalArgumentException e) {
            mesa.imprimirMensagem("Erro: " + e.getMessage());
        }

        return true;
    }

    /** Verifica se o usuario digitou a quantidade minima de argumentos. */
    private static void exigirArgumentos(String[] partes, int minimo, String uso) {
        if (partes.length < minimo) {
            throw new IllegalArgumentException("Uso correto: " + uso);
        }
    }
}
