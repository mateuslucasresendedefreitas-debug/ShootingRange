# CHROMA STRAIN — Documento de Design & Decisões

*Jogo mobile adaptado do material original **"Codex: Skills, Gadgets, Weapons and Operations"** (Chromanite, três facções, armas, skills, gadgets e operações).*

---

## 1. Análise do material original

O codex entregue descreve um universo completo com sistemas **já numericamente especificados** — o tipo de material que normalmente falta em documentos de estudante. Pontos de maior potencial identificados:

| Elemento do codex | Potencial | Por quê |
|---|---|---|
| **Chromanite** (bio-mineral vivo, doses, withdrawal) | ★★★★★ | Mecânica de identidade pronta: *power-up com custo* (risco/recompensa) e justificativa diegética para tudo no jogo |
| **3 strains com identidade oposta** (Red = força, Green = furtividade, Blue = controle) | ★★★★★ | Sistema de classes completo com fantasia clara — vira 3 estilos de jogo distintos |
| **Growth Conditions** (tabelas de estímulos Bioquímico/Energia/Acústico + regra "pelo menos 2 tipos") | ★★★★★ | O ouro escondido do documento: uma **regra de sistema aprendível** que vira minigame educativo (o jogo ensina o próprio lore-ciência: neuroquímica, energia, acústica) |
| **Armas nomeadas com passivas numéricas** (Embermaw "a cada 4º tiro", Whisperfangs "bleed 5 stacks → Hemorrhage", Glacivore "Chill 3 → Freeze") | ★★★★★ | Especificações que traduzem direto para código — implementadas 1:1 |
| **Operações com mecânicas de boss descritas** (Varrak precisa de taunt, Echo Priestess entre clones, Clothwalker só aparece em movimento…) | ★★★★☆ | Nove lutas com *gimmick* único cada — raro em GDDs iniciais |
| Conteúdo de raid multiplayer (mecânicas para grupo) | ★★☆☆☆ | Inviável em escopo mobile solo — **adaptado** para versões single-player das mesmas ideias |

**Decisão central:** não "usar o tema" do codex — **implementar os sistemas do codex**, com os números originais onde possível (dano 164–202 do Embermaw etc.), adaptando o que era MMO para uma sessão mobile de 3–6 minutos.

## 2. Pesquisa e referências

- **Gênero/controles:** *Archero, Soul Knight, Nuclear Throne mobile ports* — twin-stick virtual com stick dinâmico (âncora onde o dedo pousa) é o padrão que melhor equilibra precisão e conforto em touch; botões de habilidade em arco no polegar direito (alcance de polegar, alvos ≥ 88px físicos).
- **Sessão mobile:** runs curtas com meta-progressão persistente (modelo *roguelite de operações*) — encaixa o formato "Field Deployments" do codex.
- **Juice:** hit-stop, screen shake por trauma (curva quadrática), damage numbers, haptics — padrão Vlambeer/*Downwell* para leitura de impacto sem custo de arte.
- **Educacional:** *quiz embutido na economia* (não em tela separada): a regra de cultivo do codex ("pelo menos dois TIPOS de estímulo") virou o minigame do Lab, e a resposta está no Codex in-game — loop ler → aplicar → recompensa (modelo *Duolingo-loop* aplicado a lore).

## 3. Decisões de design

### Gênero: **Twin-stick action roguelite por operações**
Por quê: mostra as 21 mecânicas do codex (armas/skills/gadgets/doses/status) em gameplay contínuo; controles nativos de touch; sessões curtas; escopo viável e polível.

### Estilo de arte: **"Bioluminescência cristalina"** — vetorial procedural neon sobre void escuro
- Tudo é desenhado por código (shards/cristais + glow radial): consistência total, APK de 4MB, 60fps.
- A cor É a linguagem: facção, status (burn vermelho, bleed verde, chill azul), perigo (telegraphs), recompensa.
- Justificativa diegética: tudo no mundo é Chromanite — inimigos são "husks" cristalinos da strain local.

### Narrativa: **"Free Runner"** — mercenário sem facção
Você testa as três strains (desbloqueio das 3 desde o início = escolha de estilo, não punição). Lore entregue via Codex in-game (17 entradas fiéis ao documento) com incentivo econômico de leitura ("Field Study": +10 shards por entrada lida). Sem cutscenes: banners de missão + hints de boss derrotado.

### UX mobile
- Landscape imersivo, safe-area de polegares, sticks dinâmicos, auto-mira ao mover sem mirar.
- HUD por prioridade: HP/dose (canto sup. esq.), boss bar (topo centro), habilidades com cooldown radial (inf. dir.).
- Tudo tolerante a erro: tap-vs-drag no codex, botões grandes, pausa a qualquer momento, save automático.

## 4. Estrutura do jogo

```
TITLE → HUB (Operações · Lab · Codex · Upgrades, com troca de facção)
          └─ RUN (ondas → boss → resultado) → recompensas → HUB
```

### As três facções (fiéis ao codex)

| | 🔴 CRIMSON DEN (Vanguard) | 🟢 VERDANT HERD (Phantom) | 🔵 NAVY COLONY (Savant) |
|---|---|---|---|
| Arma | **EMBERMAW** — semi-auto; *Heat Vent Cycle*: 4º tiro incendiário perfurante | **NEEDLEWRAITH** — burst SMG; *Neurofracture*: 3º burst aplica Neural Disrupt | **GLACIVORE** — sniper cryo; *Ice Latch*: 3 Chill → Freeze + vulnerabilidade a crítico |
| Melee | **FURYBRAND** — *Bloodfire Memory*: + dano com HP baixo, ignição | **WHISPERFANGS** — dash-strike; *Bloodthread*: crit → Bleed ×5 → **Hemorrhage** (+25% dano recebido) | **EVOCLASM** — combo ×3; *System Overclock*: +3%/buff, finisher atordoa |
| Skill | **Seismic Fist** — slam em área (+25% dentro de fogo; bônus vs. queimando) | **Phantom Vein** — stealth 4s, 1º golpe crítico, +50% velocidade ao sair | **Overcharge Wave** — interrompe, desorienta, enfraquece; devolve cooldown de gadget |
| Gadget | **Red Solvent Flask** — zona de fogo 6s | **Pulse Decoy** — decoy que taunta e explode em flash lento | **Cognitive Loop Trap** — campo que zapeia/atordoa |
| Dose | **Adrenaline Pump** — +40% dano, lifesteal melee; *withdrawal*: lento | **Bloom Rush** — todo hit crita, +25% vel.; *withdrawal*: cadência menor | **Cortex Split** — **bullet-time** (mundo a 45%); *withdrawal*: skills travadas |
| Passiva | **Pain Echo** — <30% HP: choque retaliatório | **Spinal Bloom** — 3s sem dano: +vel., próximo hit amortecido | **Data Charge** — kills empilham +3% dano |

### As nove operações (todas com o gimmick do codex)

| Op | Modificador | Boss e mecânica |
|---|---|---|
| Redhold Arena (Trials) | Berserker Mode — inimigos enfurecem com o tempo | **Varrak** ignora dano à distância: taunt no corpo-a-corpo abre a guarda |
| The Bleeding Depths (Raid) | Pressure Surge — anéis de choque da arena | **Matron of the Maw** — ondas de eco concêntricas com brecha para atravessar |
| Nuka, Orca's Wound (Hunt) | Rampage Cycles | **Nuka** alterna fúria (investidas + slams) e colapso exausto (+50% dano) |
| Gravenight Spire (Trials) | Flickering Reality — padrões mudam por onda | **Thyral** — fase 2 "Mirrored Realm": ataca do seu ponto espelhado; Mirror Thorns refletem balas até marcados por melee |
| Temple of the Hollow Sign (Raid) | Hollow Sign — inimigos distantes somem | **Echo Priestess** — 4 clones; matar o errado a EMPODERA; o real tem núcleo brilhante |
| Clothwalker (Hunt) | Haunted Whispers | **Clothwalker** só é sólido enquanto você se MOVE; parado, ele some e se cura |
| Synapse Sector 09 (Trials) | Overclock Surge — tudo acelera; score multiplica | **Echo-Byte** fica +forte a cada segundo; janela de "sync" ×2 dano após dash |
| Hive Interface Theta (Raid) | Hive Logic — zonas de vírus | **Queen Processor** blindada enquanto houver vírus maduro: purgue ficando na zona |
| Scion of the Code Swarm (Hunt) | Mirror Protocol | **Scion** deploya "ecos" que atiram COM A SUA ARMA em você + varredura de feixe |

### Loop educativo (adaptação do conteúdo)
1. **Codex** (17 entradas com o texto do documento) paga shards por leitura.
2. **Lab**: gaste *raw nodes* (colhidos nas runs) escolhendo **2 estímulos** que fazem a strain florescer — a regra do codex ("pelo menos 2 dos 3 TIPOS") é o critério; distratores vêm das listas das outras strains; erro gera explicação ("ambos são ENERGIA — uma strain precisa de dois tipos…").
3. Acerto refina **Stabilized Dose** → você inicia a próxima operação já dosado. Conhecimento = poder, literalmente.

### Economia
- **Shards** (moeda): clear de op, score, drops, leitura de codex → 4 upgrades ×5 níveis (Vitality/Power/Reflex/Systems).
- **Raw Nodes** (material): drops + bônus de vitória → tentativas do Lab.
- **Doses estabilizadas**: consumível por facção (meter cheio no início da run).
- Progressão por facção: TRIALS → RAID → HUNT (desbloqueio sequencial).

## 5. Arquitetura técnica

- **Android nativo em Java puro** (zero dependências — sem AndroidX): `SurfaceView` + thread própria, timestep fixo 60Hz, render em canvas de hardware.
- Resolução virtual (altura 720u) → idêntico em qualquer aspect ratio.
- Partículas pooladas, projéteis poolados, glow via gradiente radial (sem blur), câmera com trauma/kick.
- Áudio 100% sintetizado (`scripts/gen_audio.py`, numpy): 33 SFX + 5 loops ambientes seamless; `SoundPool` + `MediaPlayer` (assets *stored* p/ `openFd`).
- Ícones procedurais (`scripts/gen_icons.py`, Pillow): launcher legado + adaptive icon.
- Save: `SharedPreferences`.
- **Dois pipelines de build**:
  - `Gradle/AGP 8` (Android Studio ou CI) — padrão;
  - `scripts/build_apk.py` — pipeline **sem Android SDK** (aapt2 do Apktool + android-all do Robolectric + dx + uber-apk-signer), usado para gerar o `dist/ChromaStrain-debug.apk` deste repositório em ambiente sem acesso ao SDK.

## 6. Validação e balanceamento por simulação

Um harness headless (`tests/headless` + `scripts/run_sim.sh`) roda o jogo real na JVM
com stubs no lugar das classes Android e um bot jogando:

- **Varredura god-mode**: o bot vence as **27 combinações** (3 facções × 9 operações),
  levando **todos os 9 scripts de boss do spawn à morte** — fases, clones da Priestess,
  Mirror Thorns, zonas de vírus, echo turrets, beam do Scion, ciclos de dose (incluindo
  o bullet-time azul). Resultado: **zero crashes**.
- **Telemetria mortal**: o mesmo bot, mortal e sem desviar de nada, morre nas waves 2–4 —
  piso de dificuldade saudável (um humano desvia, usa stealth reativo e upgrades).
- **Caminho de derrota**: bot passivo morre na wave 1 como esperado.

O tuning da v1 saiu desses dados: dano inimigo do tier TRIALS reduzido (×0.9),
volatile mais suave, *anti-stall* nos spitters (param de kitar após 55s de wave),
dose carrega mais rápido (+12/kill), kit verde com +6% de velocidade base e
Spinal Bloom absorvendo 40%.

## 7. O que ficou de fora (escopo consciente)

- Skills não implementadas viraram lore no codex (Shockspike/Thermal/Mist/Scentbreaker/Focus Shard/Dissonance/Neural Lock citados nas entradas) — slots de expansão natural.
- Multiplayer/raids em grupo → bosses single-player com as mesmas mecânicas.
- Sets de equipamento (Ashblood Forge etc.) → candidato a v1.1 (sistema de loot).

## 8. Roadmap sugerido

1. **v1.1** — sistema de loot com os Set Bonuses do codex; 2ª skill por facção (Synapse Surge, Neural Lock); daily ops.
2. **v1.2** — modo endless com leaderboard local; conquistas ("Field Researcher": ler todo o codex).
3. **v2** — os 21 bosses do codex (3 por raid), eventos de cultivo em tempo real.
