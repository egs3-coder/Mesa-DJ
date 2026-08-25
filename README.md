# 🎧 MesaDJ — Threads em Java

> Aplicação desenvolvida em **Java** que simula uma mesa de DJ utilizando **Threads**, onde cada instrumento funciona de forma independente.

---

## 👥 Integrantes

| Integrante                                 |
| ------------------------------------------ |
| 👨‍💻 **Ewerton Guilherme da Silva**       |
| 👨‍💻 **Pablo Arthur Eustáquio de Lima**   |
| 👨‍💻 **Saulo Eduardo Almeida dos Santos** |
| 👨‍💻 **Lucas Aprigio dos Santos**         |
| 👨‍💻 **João Ricardo Alves de Brito**      |
| 👨‍💻 **Thiago Cardozo da Conceição**      |
| 👨‍💻 **Eloi de Lima Sousa**               |

---

## 🎯 Objetivo

O projeto demonstra conceitos de **programação concorrente em Java**.

Cada instrumento possui sua própria Thread:

```text
🎧 MesaDJ
│
├── 🥁 Bateria
├── 🎸 Baixo
├── 🎹 Synth
├── 🎵 Novos instrumentos
│
└── 🖥️ Painel de Status
```

Assim, é possível **pausar, retomar, alterar o BPM ou encerrar uma faixa sem afetar as demais**.

---

## ⚙️ Funcionalidades

* 🧵 Cada instrumento possui uma **Thread independente**
* ▶️ Inicialização das Threads com `start()`
* ⏸️ Pausar instrumentos individualmente
* ▶️ Retomar instrumentos
* 🛑 Encerrar uma faixa sem interromper as outras
* 🔐 Sincronização utilizando `synchronized`
* 💤 Pausa utilizando `wait()`
* 🔔 Retomada utilizando `notifyAll()`
* ⚡ Controle utilizando `interrupt()`
* 🥁 Simulação de BPM com `Thread.sleep()`
* 🖥️ Painel atualizado automaticamente a cada **2 segundos**
* ➕ Adição de novos instrumentos durante a execução
* 📦 Utilização de `ConcurrentHashMap`
* 🔚 Encerramento organizado das Threads

---

## 🧠 Como funciona?

Cada instrumento é representado pela classe:

```java
class FaixaInstrumento extends Thread
```

Quando um instrumento é iniciado:

```java
instrumento.start();
```

uma nova Thread começa a executar o método:

```java
run();
```

Cada faixa possui seu próprio:

* 🎵 Nome
* ▶️ Estado
* 🥁 BPM
* 🔢 Número de batidas

---

## 🔐 Sincronização

Os estados dos instrumentos são protegidos utilizando:

```java
synchronized
```

Isso evita que diferentes Threads alterem o mesmo estado simultaneamente.

Para controlar pausa e retomada são utilizados:

```java
wait();
notifyAll();
interrupt();
```

---

## 🥁 Controle de BPM

O intervalo entre as batidas é calculado utilizando:

```text
60000 / BPM
```

| BPM | Intervalo aproximado |
| --: | -------------------: |
|  60 |              1000 ms |
| 120 |               500 ms |
| 180 |               333 ms |

Quanto maior o BPM:

```text
BPM maior
    ↓
Intervalo menor
    ↓
Mais batidas
```

O intervalo é aplicado através de:

```java
Thread.sleep(intervalo);
```

---

## 🖥️ Painel de Status

Existe uma Thread exclusiva responsável por mostrar o estado das faixas:

```java
class PainelStatus extends Thread
```

O painel é atualizado automaticamente a cada:

```java
Thread.sleep(2000);
```

### Exemplo

```text
====================================================
               MESA DJ - THREADS
====================================================
INSTRUMENTO        STATUS        BPM      BATIDAS
----------------------------------------------------
Bateria            TOCANDO       120      25
Baixo              TOCANDO        90      18
Synth              PAUSADO        70      10
====================================================
```

---

## 🎛️ Comandos

| Comando           | Função                                  |
| ----------------- | --------------------------------------- |
| `pause bateria`   | ⏸️ Pausa um instrumento                 |
| `resume bateria`  | ▶️ Retoma um instrumento                |
| `stop baixo`      | 🛑 Encerra uma faixa                    |
| `bpm bateria 180` | 🥁 Altera o BPM                         |
| `add guitarra`    | ➕ Adiciona instrumento com 120 BPM      |
| `add piano 90`    | ➕ Adiciona instrumento com BPM definido |
| `status`          | 🖥️ Atualiza o painel                   |
| `help`            | ❓ Exibe os comandos                     |
| `exit`            | 🚪 Encerra o programa                   |

> ⚠️ Os comandos devem ser digitados **dentro do programa depois que aparecer `DJ >`**, e não diretamente no PowerShell.

---

## ➕ Adicionando novos instrumentos

É possível adicionar instrumentos enquanto o programa está funcionando.

Exemplo:

```text
DJ > add guitarra 150
```

Uma nova Thread será criada:

```text
🥁 Bateria   → Thread
🎸 Baixo     → Thread
🎹 Synth     → Thread
🎸 Guitarra  → Nova Thread
```

As outras faixas continuam funcionando normalmente.

---

## ▶️ Como executar

### 1. Compile o projeto

```bash
javac MesaDJ.java
```

### 2. Execute

```bash
java MesaDJ
```

### 3. Aguarde aparecer

```text
DJ >
```

Agora os comandos podem ser utilizados:

```text
DJ > pause baixo
DJ > resume baixo
DJ > bpm bateria 180
DJ > add guitarra 150
DJ > status
DJ > exit
```

---

## 🧪 Teste rápido

Para testar as principais funções:

```text
pause bateria
resume bateria
bpm bateria 200
add guitarra 150
pause guitarra
stop synth
status
exit
```

---

## ✅ Requisitos atendidos

| Requisito                     | Status |
| ----------------------------- | :----: |
| Threads independentes         |    ✅   |
| Pausar faixa                  |    ✅   |
| Retomar faixa                 |    ✅   |
| Encerrar faixa                |    ✅   |
| `synchronized`                |    ✅   |
| `wait()` e `notifyAll()`      |    ✅   |
| `Thread.sleep()`              |    ✅   |
| Controle de BPM               |    ✅   |
| Painel automático             |    ✅   |
| Atualização a cada 2 segundos |    ✅   |
| Adicionar instrumentos        |    ✅   |
| Encerramento seguro           |    ✅   |

---

## 🛠️ Tecnologias e conceitos

![Java](https://img.shields.io/badge/Java-Threads-orange?style=for-the-badge\&logo=openjdk)

```text
☕ Java
🧵 Threads
🔐 synchronized
💤 wait()
🔔 notifyAll()
⚡ interrupt()
⏱️ Thread.sleep()
📦 ConcurrentHashMap
🔗 join()
```
