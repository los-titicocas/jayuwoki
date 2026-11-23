# 🎮 Comandos de Jayuwoki Bot

## 📊 Sistema de Elo y Privaditas
- `$addPlayer <nombre>` - Añade un jugador con Elo inicial 1000
- `$addPlayers <nombre1> <nombre2> ...` - Añade múltiples jugadores
- `$privadita <10 jugadores>` - Crea equipos 5v5 aleatorios con roles
- `$resultadoPrivadita blue` - Registra victoria del Equipo Azul
- `$resultadoPrivadita red` - Registra victoria del Equipo Rojo
- `$dropPrivadita` - Cancela la privadita actual
- `$verElo` - Muestra ranking completo ordenado por Elo
- `$verElo <nombre>` - Muestra estadísticas de un jugador

## 🎵 Sistema de Música (YouTube, SoundCloud, etc.)
- `$play <URL o búsqueda>` - Reproduce música o añade a la cola
  - **Ejemplos:**
    - `$play https://soundcloud.com/`
    - `$play Never Gonna Give You Up`
- `$pause` - Pausa la reproducción actual
- `$resume` - Reanuda la reproducción
- `$skip` - Salta a la siguiente canción en la cola
- `$stop` - Detiene la reproducción y limpia la cola completa
- `$queue` o `$cola` - Muestra la cola de reproducción
- `$nowplaying` o `$np` - Muestra la canción actual con progreso
- `$join` - Une al bot a tu canal de voz
- `$leave` - Desconecta al bot del canal de voz

## 🔧 Administración (solo admins)
- `$deletePlayer <nombre>` - Elimina un jugador de la base de datos
- `$adminResetElo <nombre>` - Resetea el Elo de un jugador a 1000

## 🎲 Otros Comandos
- `$rolladie <número>` - Lanza un dado (ej: `$rolladie 20`)
- `$help` - Muestra esta lista de comandos

---

📅 Temporadas de 3 meses desde el 23/11/2026: al acabar se guarda un CSV y se reinicia el Elo.

💡 **Tip:** El sistema detecta y corrige automáticamente bugs de Elo (jugadores con Elo anómalo por bugs se resetean a 1000 automáticamente)

🎵 **Fuentes soportadas:** YouTube (no funcha), SoundCloud, Bandcamp, Vimeo, Twitch, archivos HTTP