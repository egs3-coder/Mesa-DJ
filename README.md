# Mesa DJ

Projeto Java com Spring Boot e Maven para controlar uma mesa simples de DJ no navegador.

## Como rodar

No Windows, dentro da pasta do projeto:

```powershell
.\mvnw.cmd spring-boot:run
```

Para rodar apontando para uma pasta de musicas:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--music.dir=C:\Musicas"
```

Exemplo com subpastas:

```text
C:\Musicas
  Rock
    musica-01.mp3
  Eletronica
    set-01.wav
```

Depois abra:

```text
http://localhost:8080
```

Na tela, use a area "Biblioteca" para buscar por nome da musica ou nome da pasta. Se pesquisar `Rock`, o app lista as musicas dentro dessa pasta.

Tambem da para testar direto pelo navegador:

```text
http://localhost:8080/api/library?q=Rock
```

## O que a interface faz

- Play, pause, stop e proxima musica.
- Playlist simples para musicas completas, carregadas somente no inicio.
- Biblioteca local usando a pasta passada no comando `--music.dir`.
- Separacao aproximada da musica em Kick, Caixa, Hats, Surdos, Guitarras, Baixo, Cordas/Piano, Vocais, Vocal principal e Backing Vocals.
- Medidores de dB se mexendo em cada faixa conforme a musica toca.
- Mute e solo por faixa.
- Volume master.
- Equalizador por frequencias: 60 Hz, 170 Hz, 350 Hz, 1 kHz, 3.5 kHz e 10 kHz.

## Observacao importante

O app agora carrega uma musica uma unica vez e cria as faixas automaticamente a partir dela. Essa separacao e uma aproximacao por filtros de frequencia usando Web Audio API, entao ela nao isola perfeitamente vocal, bateria e instrumentos como uma ferramenta de inteligencia artificial faria. Mesmo assim, ela permite demonstrar a ideia da mesa: a musica entra no comeco, passa pela separacao, mostra os dB por faixa, e depois passa pelo equalizador.

Fluxo do audio:

```text
Musica carregada -> Separacao das faixas -> Equalizador -> Master
```
