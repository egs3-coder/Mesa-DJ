# 🎚️ Mesa DJ — Heart Peripheral

Projeto de **concorrência em Java** baseado no desafio de simular uma mesa de DJ onde cada instrumento/faixa toca de forma independente.

A música escolhida é **“Heart Peripheral” — AM Contra**, da biblioteca gratuita Cambridge-MT. A página lista o *Edited Excerpt* com 24 tracks (~40 MB) e o multitrack completo com 32 tracks (~275 MB).

Fonte:
https://www.cambridge-mt.com/ms3/mtk/

## 🎯 O foco

Esta versão **não usa IA nem separador de stems**. Os arquivos já vêm separados.

O objetivo é demonstrar:

- `Thread`
- `Runnable`
- `synchronized`
- `wait()`
- `notifyAll()`
- `volatile`
- estados independentes
- execução concorrente
- pausa/retomada
- encerramento seguro

Arquitetura:

```text
                  MESA DJ
                     |
          +----------+----------+
          |          |          |
       Thread     Thread      Thread
       BASS       DRUMS       SYNTH
          |          |          |
       bass.wav   drums.wav   synth.wav
```

Cada WAV vira um `AudioTrack`, e cada `AudioTrack` possui sua própria `Thread` e `Clip`.

## 📁 Instalação dos stems

Baixe o **Edited Excerpt** de “Heart Peripheral” no Cambridge-MT.

Coloque os WAVs em:

```text
music/heart-peripheral/
```

Exemplo:

```text
music/
└── heart-peripheral/
    ├── 01 Kick.wav
    ├── 02 Bass.wav
    ├── 03 Synth.wav
    └── ...
```

O programa detecta automaticamente todos os `.wav`, portanto os nomes reais dos arquivos não precisam ser iguais aos exemplos.

### Por que WAV?

Use os WAV originais do multitrack. O projeto usa `javax.sound.sampled`, da própria plataforma Java, para reprodução PCM/WAV. Não é necessário converter para MP3.

## ▶️ Executar

Requisito:

- Java 17+
- Maven opcional
- saída de áudio disponível

Pela IDE, execute:

```text
src/main/java/com/mesadj/MesaDjApplication.java
```

Ou compile:

```powershell
mvn compile
```

e execute a classe `com.mesadj.MesaDjApplication` pela IDE.

## 🎛️ Comandos

```text
DJ > HELP
DJ > LIST
DJ > STATUS
DJ > PAUSE bass
DJ > RESUME bass
DJ > STOP bass
DJ > PAUSE ALL
DJ > RESUME ALL
DJ > STOP ALL
DJ > EXIT
```

O programa aceita também comandos parciais. Se existir uma faixa chamada `02 Deep Bass Synth.wav`, por exemplo:

```text
PAUSE bass
```

pode encontrá-la.

## 🧵 Como a Thread funciona

Cada `AudioTrack` implementa:

```java
Runnable
```

e recebe uma thread:

```java
Thread thread = new Thread(this, "AudioTrack-" + name);
thread.start();
```

A thread mantém o seu próprio `Clip`.

Enquanto estiver tocando:

```text
PLAYING
   ↓
clip.start()
```

Ao pausar:

```text
clip.stop()
state = PAUSED
```

A posição do `Clip` é preservada, então `RESUME` continua daquele ponto.

## 🔐 Sincronização

Os métodos que alteram o estado são:

```java
public synchronized void pauseTrack()
public synchronized void resumeTrack()
public synchronized void stopTrack()
```

Isso protege o estado da faixa contra alterações concorrentes.

Quando pausada, a thread entra em:

```java
wait();
```

Ela não fica gastando CPU em um loop inútil.

Quando recebe `RESUME`:

```java
notifyAll();
```

A thread acorda e continua.

## 🛑 Encerramento seguro

Não usamos:

```java
Thread.stop();
```

Em vez disso existe:

```java
private volatile boolean shutdownRequested;
```

Ao executar:

```text
EXIT
```

a aplicação sinaliza o encerramento, acorda threads que estejam esperando e fecha os `Clip`s.

## 🔁 Loop contínuo

Cada faixa utiliza:

```java
clip.loop(Clip.LOOP_CONTINUOUSLY);
```

Isso permite que cada instrumento continue tocando continuamente enquanto o DJ controla as outras faixas.

## 🧪 Demonstração para o professor

1. Rode o programa.
2. Execute `STATUS`.
3. Mostre que existem várias `THREAD`s.
4. Execute:

```text
PAUSE bass
```

5. Mostre que somente o baixo mudou para `PAUSED`.
6. Execute:

```text
RESUME bass
```

7. Pause duas faixas:

```text
PAUSE bass
PAUSE synth
```

8. Retome apenas uma:

```text
RESUME synth
```

9. Finalize:

```text
EXIT
```

e mostre as mensagens de encerramento das threads.

## 🧠 O que o projeto prova

A aplicação transforma uma música multitrack em um pequeno sistema concorrente:

```text
N arquivos WAV
      ↓
N AudioTracks
      ↓
N Threads
      ↓
N estados independentes
      ↓
comandos do DJ
      ↓
synchronized + wait/notify + volatile
```

O Spring Boot foi retirado desta versão de propósito. Para **este desafio específico**, Java puro deixa os conceitos pedidos muito mais evidentes. Se depois houver necessidade de uma interface, Spring Boot pode entrar como camada de apresentação/API sem mudar o núcleo de concorrência.

## ⚠️ Observação

`Clip` carrega o áudio em memória. Por isso, para a demonstração, o **Edited Excerpt** de aproximadamente 40 MB é mais indicado que o multitrack completo de aproximadamente 275 MB.

A biblioteca Cambridge-MT é a fonte dos arquivos; respeite as condições de uso e atribuição indicadas pelo próprio site.
