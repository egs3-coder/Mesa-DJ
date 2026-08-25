# 🎧 MesaDJ — Threads em Java

> Aplicação em **Java** que simula uma mesa de DJ utilizando **Threads**, onde cada instrumento funciona de forma independente.

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
* ▶️ Iniciar instrumentos com `start()`
* ⏸️ Pausar uma faixa
* ▶️ Retomar uma faixa
* 🛑 Encerrar uma faixa individualmente
* 🔐 Controle de concorrência com `synchronized`
* 💤 Pausa utilizando `wait()`
* 🔔 Retomada utilizando `notifyAll()`
* ⚡ Uso de `interrupt()`
* 🥁 Controle de BPM com `Thread.sleep()`
* 🖥️ Painel de status atualizado a cada **2 segundos**
* ➕ Adição de novos instrumentos durante a execução
* 📦 Armazenamento seguro utilizando `ConcurrentHashMap`
* 🔚 Encerramento organizado utilizando `join()`

---

## 🧠 Como funciona?

Cada instrumento é representado pela classe:

```java
class FaixaInstrumento extends Thread
```

Ao criar um novo instrumento:

```java
instrumento.start();
```

uma nova Thread começa a executar o método:

```java
run()
```

Cada faixa possui seu próprio:

```text
🎵 Nome
▶️ Estado
🥁 BPM
🔢 Quantidade de batidas
```

---

## 🔐 Sincronização

Os estados dos instrumentos são protegidos com:

```java
synchronized
```

Isso evita que duas Threads alterem o mesmo instrumento ao mesmo tempo.

Para controlar pausa e retomada são utilizados:

```java
wait();
notifyAll();
interrupt();
```

---

## 🥁 Controle de BPM

O intervalo entre as batidas é calculado usando:

```java
60000 / BPM
```

Exemplo:

| BPM | Intervalo |
| --: | --------: |
|  60 |   1000 ms |
| 120 |    500 ms |
| 180 |   ~333 ms |

Quanto maior o BPM:

```text
BPM maior
   ↓
Sleep menor
   ↓
Mais batidas
```

---

## 🖥️ Painel de Status

Existe uma Thread exclusiva:

```java
class PainelStatus extends Thread
```

Ela atualiza o painel automaticamente a cada:

```java
Thread.sleep(2000);
```

Exemplo:

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
| `pause bateria`   | ⏸️ Pausa a bateria                      |
| `resume bateria`  | ▶️ Retoma a bateria                     |
| `stop baixo`      | 🛑 Encerra o baixo                      |
| `bpm bateria 180` | 🥁 Altera o BPM                         |
| `add guitarra`    | ➕ Adiciona instrumento com 120 BPM      |
| `add piano 90`    | ➕ Adiciona instrumento com BPM definido |
| `status`          | 🖥️ Atualiza o painel                   |
| `help`            | ❓ Exibe os comandos                     |
| `exit`            | 🚪 Encerra o programa                   |

> ⚠️ Os comandos devem ser digitados **dentro do programa, após aparecer `DJ >`**, e não diretamente no PowerShell.

---

## ➕ Instrumentos durante a execução

Também é possível adicionar novas faixas enquanto o programa já está funcionando:

```text
DJ > add guitarra 150
```

Resultado:

```text
🥁 Bateria   → Thread
🎸 Baixo     → Thread
🎹 Synth     → Thread
🎸 Guitarra  → Nova Thread
```

As outras faixas continuam tocando normalmente.

---

## ▶️ Como executar

### 1. Compile

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

Agora utilize os comandos:

```text
DJ > pause baixo
DJ > resume baixo
DJ > bpm bateria 180
DJ > add guitarra 150
DJ > status
DJ > exit
```

---

## 🧪 Exemplo de teste

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

Essa sequência demonstra praticamente todas as funcionalidades principais do projeto.

---

## ✅ Requisitos atendidos

| Requisito                     | Status |
| ----------------------------- | :----: |
| Threads independentes         |    ✅   |
| Pausar faixa                  |    ✅   |
| Retomar faixa                 |    ✅   |
| Encerrar faixa                |    ✅   |
| `synchronized`                |    ✅   |
| `wait()` / `notifyAll()`      |    ✅   |
| `Thread.sleep()`              |    ✅   |
| Controle de BPM               |    ✅   |
| Painel automático             |    ✅   |
| Atualização a cada 2 segundos |    ✅   |
| Adicionar instrumentos        |    ✅   |
| Encerramento seguro           |    ✅   |

---

## 🛠️ Tecnologias

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

---

## 📚 Conceitos praticados

* Programação concorrente
* Threads
* Sincronização
* Regiões críticas
* Controle de estado
* Pausa e retomada de Threads
* Encerramento seguro
* Coleções concorrentes
