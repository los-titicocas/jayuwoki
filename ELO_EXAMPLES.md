# 🧪 Ejemplos Prácticos del Sistema Elo

## 📊 Simulación de Cambios de Elo

### Escenario 1: Partida Equilibrada
**Situación:** Todos los jugadores tienen 1000 de Elo (primera partida)

| Equipo Azul | Elo Inicial | Equipo Rojo | Elo Inicial |
|-------------|-------------|-------------|-------------|
| Jorge       | 1000        | Chris       | 1000        |
| Messi       | 1000        | Estucaquio  | 1000        |
| Nuriel      | 1000        | Frodo       | 1000        |
| Nestor      | 1000        | Guamero     | 1000        |
| Nuha        | 1000        | Jonathan    | 1000        |
| **Promedio**| **1000**    | **Promedio**| **1000**    |

**Resultado: Gana Equipo Azul**

Cálculo para cada jugador:
```
Probabilidad esperada: E = 1 / (1 + 10^((1000-1000)/400)) = 0.5
Cambio ganador: ΔElo = 32 × (1 - 0.5) = +16
Cambio perdedor: ΔElo = 32 × (0 - 0.5) = -16
```

| Jugador    | Antes | Después | Cambio |
|------------|-------|---------|--------|
| Jorge      | 1000  | 1016    | +16    |
| Messi      | 1000  | 1016    | +16    |
| Nuriel     | 1000  | 1016    | +16    |
| Nestor     | 1000  | 1016    | +16    |
| Nuha       | 1000  | 1016    | +16    |
| Chris      | 1000  | 984     | -16    |
| Estucaquio | 1000  | 984     | -16    |
| Frodo      | 1000  | 984     | -16    |
| Guamero    | 1000  | 984     | -16    |
| Jonathan   | 1000  | 984     | -16    |

---

### Escenario 2: El Favorito Gana
**Situación:** Equipo Azul tiene jugadores más experimentados

| Equipo Azul | Elo Inicial | Equipo Rojo | Elo Inicial |
|-------------|-------------|-------------|-------------|
| Jorge       | 1200        | Chris       | 900         |
| Messi       | 1250        | Estucaquio  | 950         |
| Nuriel      | 1180        | Frodo       | 880         |
| Nestor      | 1220        | Guamero     | 920         |
| Nuha        | 1150        | Jonathan    | 850         |
| **Promedio**| **1200**    | **Promedio**| **900**     |

**Resultado: Gana Equipo Azul (lo esperado)**

Cálculo para Jorge (Elo 1200 vs promedio 900):
```
E = 1 / (1 + 10^((900-1200)/400)) = 1 / (1 + 10^(-0.75)) ≈ 0.85
ΔElo ganador = 32 × (1 - 0.85) ≈ +5
ΔElo perdedor = 32 × (0 - 0.15) ≈ -5
```

| Jugador    | Antes | Después | Cambio | Comentario |
|------------|-------|---------|--------|------------|
| Jorge      | 1200  | 1205    | +5     | Gana poco (era favorito) |
| Messi      | 1250  | 1255    | +5     | Gana poco (era favorito) |
| Nuriel     | 1180  | 1185    | +5     | Gana poco (era favorito) |
| Nestor     | 1220  | 1225    | +5     | Gana poco (era favorito) |
| Nuha       | 1150  | 1155    | +5     | Gana poco (era favorito) |
| Chris      | 900   | 895     | -5     | Pierde poco (era underdog) |
| Estucaquio | 950   | 945     | -5     | Pierde poco (era underdog) |
| Frodo      | 880   | 875     | -5     | Pierde poco (era underdog) |
| Guamero    | 920   | 915     | -5     | Pierde poco (era underdog) |
| Jonathan   | 850   | 845     | -5     | Pierde poco (era underdog) |

---

### Escenario 3: La Gran Sorpresa! 🎉
**Situación:** El underdog gana

| Equipo Azul | Elo Inicial | Equipo Rojo | Elo Inicial |
|-------------|-------------|-------------|-------------|
| Jorge       | 900         | Chris       | 1200        |
| Messi       | 950         | Estucaquio  | 1250        |
| Nuriel      | 880         | Frodo       | 1180        |
| Nestor      | 920         | Guamero     | 1220        |
| Nuha        | 850         | Jonathan    | 1150        |
| **Promedio**| **900**     | **Promedio**| **1200**    |

**Resultado: Gana Equipo Azul (¡sorpresa!)**

Cálculo para Jorge (Elo 900 vs promedio 1200):
```
E = 1 / (1 + 10^((1200-900)/400)) = 1 / (1 + 10^(0.75)) ≈ 0.15
ΔElo ganador = 32 × (1 - 0.15) ≈ +27
ΔElo perdedor = 32 × (0 - 0.85) ≈ -27
```

| Jugador    | Antes | Después | Cambio | Comentario |
|------------|-------|---------|--------|------------|
| Jorge      | 900   | 927     | +27    | ¡Gran victoria! |
| Messi      | 950   | 977     | +27    | ¡Gran victoria! |
| Nuriel     | 880   | 907     | +27    | ¡Gran victoria! |
| Nestor     | 920   | 947     | +27    | ¡Gran victoria! |
| Nuha       | 850   | 877     | +27    | ¡Gran victoria! |
| Chris      | 1200  | 1173    | -27    | Derrota inesperada |
| Estucaquio | 1250  | 1223    | -27    | Derrota inesperada |
| Frodo      | 1180  | 1153    | -27    | Derrota inesperada |
| Guamero    | 1220  | 1193    | -27    | Derrota inesperada |
| Jonathan   | 1150  | 1123    | -27    | Derrota inesperada |

---

### Escenario 4: Progresión a lo Largo de 10 Partidas
**Seguimiento de Jorge a través de múltiples partidas**

| Partida | Elo Jorge | Elo Promedio Enemigo | Resultado | Prob. Esperada | Cambio | Nuevo Elo |
|---------|-----------|----------------------|-----------|----------------|--------|-----------|
| 1       | 1000      | 1000                 | Victoria  | 0.50           | +16    | 1016      |
| 2       | 1016      | 1020                 | Derrota   | 0.49           | -16    | 1000      |
| 3       | 1000      | 980                  | Victoria  | 0.53           | +15    | 1015      |
| 4       | 1015      | 1100                 | Victoria  | 0.38           | +20    | 1035      |
| 5       | 1035      | 1050                 | Derrota   | 0.48           | -17    | 1018      |
| 6       | 1018      | 950                  | Victoria  | 0.60           | +13    | 1031      |
| 7       | 1031      | 1031                 | Victoria  | 0.50           | +16    | 1047      |
| 8       | 1047      | 1200                 | Derrota   | 0.31           | -10    | 1037      |
| 9       | 1037      | 1000                 | Victoria  | 0.55           | +14    | 1051      |
| 10      | 1051      | 1100                 | Victoria  | 0.43           | +18    | 1069      |

**Resumen después de 10 partidas:**
- Elo inicial: 1000
- Elo final: 1069
- Cambio total: +69
- Victorias: 7
- Derrotas: 3
- Win rate: 70%

---

## 🎯 Interpretación de Cambios de Elo

### Ganando puntos:
- **+5 a +10:** Ganaste contra un equipo mucho más débil (lo normal)
- **+15 a +18:** Ganaste contra equipo equilibrado (bien hecho)
- **+25 a +32:** ¡Ganaste contra un equipo muy superior! (excelente)

### Perdiendo puntos:
- **-5 a -10:** Perdiste contra un equipo superior (no es grave)
- **-15 a -18:** Perdiste contra equipo equilibrado (normal)
- **-25 a -32:** Perdiste contra equipo inferior (mal resultado)

---

## 📈 Distribución Esperada de Elo

Después de muchas partidas, el sistema tiende a estabilizarse:

```
Bronce      │ ▓▓▓▓░░░░░░░░░░░░░░░░ │  0 -  899 │  20% jugadores
Plata       │ ▓▓▓▓▓▓▓▓░░░░░░░░░░░░ │ 900 - 1199 │  40% jugadores
Oro         │ ▓▓▓▓▓▓░░░░░░░░░░░░░░ │ 1200- 1499 │  25% jugadores
Platino     │ ▓▓▓░░░░░░░░░░░░░░░░░ │ 1500- 1799 │  10% jugadores
Diamante    │ ▓░░░░░░░░░░░░░░░░░░░ │ 1800+      │   5% jugadores
```

---

## 🧮 Fórmulas de Referencia Rápida

### Probabilidad Esperada
```
E = 1 / (1 + 10^((EloEnemigo - EloPropio) / 400))
```

### Cambio de Elo
```
ΔElo = K × (S - E)

Donde:
K = 32 (constante)
S = 1 si gana, 0 si pierde
E = probabilidad esperada
```

### Ejemplos de Probabilidad
| Diferencia de Elo | Probabilidad de Victoria | Cambio si Gana | Cambio si Pierde |
|-------------------|--------------------------|----------------|------------------|
| -400              | 10%                      | +29            | -3               |
| -300              | 15%                      | +27            | -5               |
| -200              | 24%                      | +24            | -8               |
| -100              | 36%                      | +20            | -12              |
| 0                 | 50%                      | +16            | -16              |
| +100              | 64%                      | +12            | -20              |
| +200              | 76%                      | +8             | -24              |
| +300              | 85%                      | +5             | -27              |
| +400              | 90%                      | +3             | -29              |

---
