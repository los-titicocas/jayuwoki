# 📊 Sistema de Elo - Jayuwoki Bot

## 🎯 Resumen
El bot Jayuwoki implementa un sistema de clasificación Elo similar al de League of Legends para rankear jugadores en partidas personalizadas (privaditas). El sistema calcula automáticamente cambios en el Elo basándose en el resultado de las partidas y actualiza las estadísticas en Firebase.

---

## 🔄 Flujo Completo del Sistema

### 1️⃣ **Añadir Jugadores a la Base de Datos**

#### Comando: `$addPlayer <nombre>`
Añade un jugador individual con Elo inicial de **1000**.

```
$addPlayer Jorge
```

#### Comando: `$addPlayers <nombre1> <nombre2> ... <nombreN>`
Añade múltiples jugadores de una vez.

```
$addPlayers Jorge Messi Nuriel Nestor Nuha
```

**📍 Ubicación en el código:** `Commands.java` (líneas 118-135)
**💾 Base de datos:** Firebase Firestore
- **Colección:** `{ServerName}/Privadita/Players`
- **Documento:** `{PlayerName}`
- **Datos iniciales:**
  - `name`: String
  - `elo`: 1000 (por defecto)
  - `wins`: 0
  - `losses`: 0

---

### 2️⃣ **Iniciar una Privadita**

#### Comando: `$privadita <10 jugadores>`
Crea equipos aleatorios de 5v5 y asigna roles.

```
$privadita Jorge Messi Nuriel Nestor Nuha Chris Estucaquio Frodo Guamero Jonathan
```

**📍 Ubicación en el código:** `Commands.java` (líneas 62-81)

**Proceso:**
1. Busca los 10 jugadores en Firebase (`DBManager.GetPlayers()`)
2. Valida que todos los jugadores existan
3. Crea una nueva instancia de `Privadita`
4. Mezcla aleatoriamente los jugadores
5. Asigna roles: Top, Jungla, Mid, ADC, Support
6. Divide en **Equipo Azul** (primeros 5) y **Equipo Rojo** (últimos 5)

**Ejemplo de salida:**
```
```
Blue Team
Jorge -> Top
Messi -> Jungla
Nuriel -> Mid
Nestor -> ADC
Nuha -> Support

Red Team
Chris -> Top
Estucaquio -> Jungla
Frodo -> Mid
Guamero -> ADC
Jonathan -> Support
```
```

---

### 3️⃣ **Registrar Resultado de la Partida**

#### Comando: `$resultadoPrivadita <equipo1|equipo2>`
Actualiza el Elo de todos los jugadores según el resultado.

```
$resultadoPrivadita equipo1
```

**📍 Ubicación en el código:**
- `Commands.java` (líneas 89-100)
- `Privadita.java` método `ResultadoPrivadita()` (líneas 98-168)

**Proceso:**

1. **Calcula el Elo promedio de cada equipo**
   ```java
   double averageEloEquipo1 = players.subList(0, 5).stream()
           .mapToInt(Player::getElo)
           .average()
           .orElse(0);
   ```

2. **Para cada jugador:**
   - Si ganó → incrementa `wins` y actualiza Elo contra el promedio enemigo
   - Si perdió → incrementa `losses` y actualiza Elo contra el promedio enemigo

3. **Llama al algoritmo de Elo:**
   ```java
   players.get(i).ActualizarElo(averageEloEquipo2, true); // Ganador
   players.get(i + 5).ActualizarElo(averageEloEquipo1, false); // Perdedor
   ```

4. **Guarda los cambios en Firebase:**
   ```java
   dbManager.updatePlayers(privaditaResultado.getServer(), privaditaResultado.getPlayers());
   ```

**Ejemplo de salida:**
```
Resultado de la partida:

🏆 **Equipo Azul ha ganado!** 🏆

**Cambios de Elo:**
Jorge: 1000 ➝ 1016 (+16)
Chris: 1000 ➝ 984 (-16)
Messi: 1000 ➝ 1016 (+16)
Estucaquio: 1000 ➝ 984 (-16)
...
```

---

## 🧮 Algoritmo de Elo (Similar a LoL)

### Fórmula Matemática

**📍 Ubicación en el código:** `Player.java` método `ActualizarElo()` (líneas 69-108)

#### 1. **Probabilidad Esperada de Victoria**
```
E = 1 / (1 + 10^((EloEnemigo - EloJugador) / 400))
```

**Interpretación:**
- Si tienes **mismo Elo** que el enemigo → E = 0.5 (50% de probabilidad)
- Si tienes **más Elo** que el enemigo → E > 0.5 (mayor probabilidad de ganar)
- Si tienes **menos Elo** que el enemigo → E < 0.5 (menor probabilidad de ganar)

#### 2. **Cambio de Elo**
```
ΔElo = K × (Resultado - Probabilidad Esperada)
```

Donde:
- **K = 32** (factor de volatilidad)
- **Resultado** = 1 si ganó, 0 si perdió
- **Probabilidad Esperada** = calculada en el paso anterior

#### 3. **Nuevo Elo**
```
NuevoElo = EloActual + ΔElo
```

Con límite mínimo de **0** (no puede ser negativo).

---

## 📊 Ejemplos de Cálculo

### Escenario 1: Equipos Equilibrados

**Equipo Azul:** 5 jugadores con Elo 1000 → **Promedio: 1000**  
**Equipo Rojo:** 5 jugadores con Elo 1000 → **Promedio: 1000**

**Si gana Equipo Azul:**
```
E = 1 / (1 + 10^((1000 - 1000) / 400)) = 1 / (1 + 1) = 0.5
ΔElo = 32 × (1 - 0.5) = 32 × 0.5 = 16

Ganadores: 1000 + 16 = 1016 (+16)
Perdedores: 1000 + 32 × (0 - 0.5) = 1000 - 16 = 984 (-16)
```

---

### Escenario 2: Equipo Favorito Gana

**Equipo Azul:** Promedio **1200**  
**Equipo Rojo:** Promedio **1000**

**Si gana Equipo Azul (favorito):**
```
Para jugador con Elo 1200:
E = 1 / (1 + 10^((1000 - 1200) / 400)) = 1 / (1 + 10^(-0.5)) ≈ 0.76
ΔElo = 32 × (1 - 0.76) ≈ 8

Ganadores: 1200 + 8 = 1208 (+8) ← Gana poco porque era esperado
Perdedores: 1000 - 24 = 976 (-24) ← Pierde más porque era esperado perder
```

---

### Escenario 3: Sorpresa (Underdog Gana)

**Equipo Azul:** Promedio **1000**  
**Equipo Rojo:** Promedio **1200**

**Si gana Equipo Azul (underdog):**
```
Para jugador con Elo 1000:
E = 1 / (1 + 10^((1200 - 1000) / 400)) ≈ 0.24
ΔElo = 32 × (1 - 0.24) ≈ 24

Ganadores: 1000 + 24 = 1024 (+24) ← Gana mucho por sorpresa
Perdedores: 1200 - 8 = 1192 (-8) ← Pierde poco aunque perdió
```

---

## 🗂️ Estructura de Firebase

```
📁 {ServerName} (ej: "Jayuwoki")
  └── 📁 Privadita
      └── 📁 Players
          ├── 📄 Jorge
          │   ├── name: "Jorge"
          │   ├── elo: 1016
          │   ├── wins: 1
          │   ├── losses: 0
          │   ├── discriminator: ""
          │   ├── avatarURL: ""
          │   └── idPlayer: 0
          ├── 📄 Messi
          │   ├── name: "Messi"
          │   ├── elo: 984
          │   ├── wins: 0
          │   ├── losses: 1
          │   └── ...
          └── ...
```

---

## 🔧 Métodos Clave en el Código

### `Player.java`

```java
public void ActualizarElo(double averageEnemyElo, boolean won)
```
- Implementa el algoritmo de Elo
- Calcula la probabilidad esperada
- Actualiza el Elo del jugador
- Factor K = 32

---

### `DBManager.java`

#### `GetPlayers(String[] nombres, MessageReceivedEvent event)`
- Busca jugadores en Firebase
- Retorna lista de `Player` con datos completos (Elo, wins, losses)

#### `updatePlayers(String server, ObservableList<Player> players)`
- Actualiza todos los jugadores en Firebase
- Usa `WriteBatch` para operaciones atómicas
- Guarda Elo, wins, losses actualizados

#### `AddPlayer(Player newPlayer)` / `AddPlayers(List<Player> newPlayers)`
- Añade nuevos jugadores con Elo inicial de 1000
- Verifica que no existan duplicados

---

### `Privadita.java`

#### `ResultadoPrivadita(String ganador, MessageReceivedEvent event)`
- Calcula Elo promedio de cada equipo
- Actualiza wins/losses
- Llama a `ActualizarElo()` para cada jugador
- Genera mensaje con cambios de Elo

---

## 📋 Comandos Disponibles

| Comando | Descripción | Ejemplo |
|---------|-------------|---------|
| `$addPlayer <nombre>` | Añade un jugador (Elo 1000) | `$addPlayer Jorge` |
| `$addPlayers <nombres>` | Añade múltiples jugadores | `$addPlayers Jorge Messi Nuriel` |
| `$deletePlayer <nombre>` | Elimina un jugador | `$deletePlayer Jorge` |
| `$verElo <nombre>` | Muestra stats de un jugador | `$verElo Jorge` |
| `$verElo` | Muestra todos los jugadores ordenados por Elo | `$verElo` |
| `$privadita <10 nombres>` | Inicia una privadita 5v5 | `$privadita Jorge ...` |
| `$resultadoPrivadita <equipo1\|equipo2>` | Registra resultado y actualiza Elos | `$resultadoPrivadita equipo1` |
| `$dropPrivadita` | Cancela la privadita activa | `$dropPrivadita` |

---

## 🎮 Flujo de Uso Completo

### 1. Preparación Inicial
```bash
# Añadir jugadores (solo se hace una vez)
$addPlayers Jorge Messi Nuriel Nestor Nuha Chris Estucaquio Frodo Guamero Jonathan
```

### 2. Jugar una Partida
```bash
# Iniciar privadita
$privadita Jorge Messi Nuriel Nestor Nuha Chris Estucaquio Frodo Guamero Jonathan

# El bot muestra los equipos aleatorios
# ... juegan la partida en LoL ...

# Registrar resultado (equipo1 = Azul, equipo2 = Rojo)
$resultadoPrivadita equipo1
```

### 3. Ver Clasificación
```bash
# Ver Elo de todos los jugadores ordenados
$verElo

# Ver Elo de un jugador específico
$verElo Jorge
```

---

## 🔥 Características Avanzadas

### ✅ Ventajas del Sistema

1. **Justicia en Rankings**
   - Ganar contra equipo fuerte → muchos puntos
   - Ganar contra equipo débil → pocos puntos
   - Perder contra equipo fuerte → pierdes poco
   - Perder contra equipo débil → pierdes mucho

2. **Equipos Balanceados**
   - Usa Elo promedio de cada equipo
   - Considera la fuerza global, no individual

3. **Persistencia en Firebase**
   - Todos los cambios se guardan automáticamente
   - Historial de wins/losses
   - No se pierde información

4. **Protección contra Errores**
   - Elo mínimo de 0 (no negativo)
   - Validación de jugadores duplicados
   - Verificación de permisos

### 🎯 Factor K Explicado

El valor K = 32 es un **balance** entre:
- **K alto (40-50):** Cambios drásticos, ideal para nuevos jugadores
- **K medio (25-35):** Balance estándar usado en este bot
- **K bajo (15-24):** Cambios graduales para jugadores establecidos

League of Legends usa un sistema dinámico donde K varía según:
- Número de partidas jugadas
- Rango actual del jugador
- MMR (Match Making Rating)

---

## 🐛 Solución de Problemas

### Error: "Jugador no encontrado"
- Verificar que el jugador esté en la base de datos con `$verElo`
- Añadir el jugador con `$addPlayer <nombre>`

### Error: "Ya hay una privadita activa"
- Terminar la privadita actual con `$resultadoPrivadita equipo1/equipo2`
- O cancelarla con `$dropPrivadita`

### Elo no se actualiza
- Verificar logs en consola (`System.out.println` en `updatePlayers`)
- Comprobar conexión a Firebase
- Verificar permisos del usuario (requiere admin o `massPermissionCheck=true`)

---

## 🚀 Mejoras Futuras Sugeridas

1. **Sistema de Rangos Visuales**
   - Bronce: 0-999
   - Plata: 1000-1299
   - Oro: 1300-1599
   - Platino: 1600-1899
   - Diamante: 1900+

2. **Factor K Dinámico**
   - K más alto para jugadores con < 20 partidas
   - K más bajo para jugadores con > 100 partidas

3. **Estadísticas Avanzadas**
   - Win rate por rol
   - Elo promedio por equipo en privaditas
   - Gráficos de progresión de Elo

4. **Leaderboards**
   - Top 10 jugadores
   - Rankings por servidor
   - Historial de partidas

---

## 🔧 Detección y Corrección Automática de Bugs

### Sistema Inteligente de Detección

El bot distingue automáticamente entre:
- ✅ **Elo legítimo** (jugador malo que ha perdido muchas partidas)
- 🐛 **Elo anómalo** (causado por bugs del sistema)

### Casos que se Corrigen Automáticamente

| Caso | Elo | Partidas | Acción |
|------|-----|----------|--------|
| Sin inicializar | 0-799 | 0 | ✅ Resetea a 1000 |
| Bug matemático | 0-799 | 1-3 | ✅ Resetea a 1000 |
| Jugador malo | 0-799 | 4+ | ❌ NO resetea (legítimo) |

**Ejemplo:**
```
marco: Elo=29, W:1, L:1 (2 partidas)
→ ⚠️ BUG DETECTADO: Imposible matemáticamente tener 29 con 2 partidas
→ ✅ Resetea a 1000

NoobMaster: Elo=450, W:5, L:45 (50 partidas)
→ ℹ️ INFO: Elo legítimo, NO se resetea
```

### Comando Manual para Admins

Si necesitas resetear manualmente a un jugador (ej: segunda oportunidad):

```discord
$adminResetElo <nombre>
```

**Ejemplo:**
```discord
$adminResetElo NoobMaster
```

**Salida:**
```
✅ **NoobMaster** ha sido reseteado manualmente por un admin:
```
Antes:  Elo: 450 | W:5 L:45
Ahora:  Elo: 1000 | W:0 L:0
```
````
