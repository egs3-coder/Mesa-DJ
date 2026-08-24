# Mesa DJ com Threads - Java

Projeto da atividade **Threads I - Mesa DJ**, implementado em Java com threads independentes, sincronizacao, pausa/retomada segura, encerramento controlado e desafios extras.

## Integrantes

- Eloi de Lima Sousa
- Thiago Cardozo da Conceicao
- Joao Ricardo Alves de Brito
- Ewerton Guilherme da Silva
- Saulo Eduardo Almeida dos Santos
- Lucas Aprigio dos Santos
- Pablo Arthur Eustaquio de Lima

## Requisitos atendidos

1. Cada instrumento e uma `Thread` independente.
2. Bateria, baixo e synth iniciam simultaneamente.
3. Controle individual por texto: `pause`, `resume`, `stop`.
4. Estado interno protegido com `synchronized`.
5. Pausa eficiente com `wait()` e retomada com `notifyAll()`.
6. Encerramento seguro por flag + `interrupt()` + `join()`; nao usa `Thread.stop()`.
7. BPM altera o tempo de `Thread.sleep()` (`60000 / BPM`).
8. Thread extra `Painel-Status` atualiza o painel a cada 2 segundos.
9. `add guitarra` adiciona um instrumento durante a execucao.
10. `ConcurrentHashMap` permite que o painel percorra os instrumentos enquanto novos instrumentos sao adicionados.

## Estrutura

```text
MesaDJ_Threads_Java/
  src/
    Main.java
    MesaDJ.java
    Instrumento.java
  RELATORIO.md
  ROTEIRO_APRESENTACAO.md
  executar_windows.bat
  executar_linux_mac.sh
```

## Como executar

### Windows (Prompt/PowerShell)

```powershell
javac -encoding UTF-8 -d out src\*.java
java -cp out Main
```

Ou execute `executar_windows.bat`.

### Linux/macOS

```bash
chmod +x executar_linux_mac.sh
./executar_linux_mac.sh
```

## Comandos

```text
pause bateria
resume bateria
stop synth
bpm baixo 150
add guitarra
add piano 100
pauseall
resumeall
status
panel on
panel off
clear on
clear off
help
exit
```

> Observacao: o painel usa sequencias ANSI para limpar a tela. Se o console da IDE mostrar caracteres estranhos, use `clear off`.
