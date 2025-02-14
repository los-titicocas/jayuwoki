# Jayuwoki Bot

Jayuwoki Bot es un bot de Discord que proporciona varias funcionalidades, incluyendo la gestión de partidas privadas, comandos de música, y más.

## Características

- **Privadita**: Organiza partidas privadas con roles asignados aleatoriamente.
- **Comandos de Música**: Reproduce música desde URLs.
- **Gestión de Jugadores**: Añade, elimina y muestra estadísticas de jugadores.
- **Comandos Diversos**: Incluye comandos como `$rolladie` para lanzar un dado virtual.

## Requisitos

- Java 11 o superior
- Gradle
- Una cuenta de Discord con permisos para crear bots
- Token del bot de Discord
- Archivo de configuración `api.config` con el token del bot
- Archivo de configuración `jayuwokidb-firebase-adminsdk.json` con las credenciales de Firebase'

## Instalación

1. Clona el repositorio:
    ```sh
    git clone https://github.com/tu-usuario/jayuwoki-bot.git
    cd jayuwoki-bot
    ```

2. Configura el archivo `api.config`:
   - Crea un archivo `api.config` en el directorio `src/main/resources/`.
   - Añade tu token de bot de Discord en el archivo:
     ```
     TU_TOKEN_DE_DISCORD
     ```

3. Configura el archivo `jayuwokidb-firebase-adminsdk.json`:
   - Crea un archivo `jayuwokidb-firebase-adminsdk.json` en el directorio `src/main/resources/`.
   - Añade las credenciales de Firebase en el archivo.

4. Compila y ejecuta el bot:
    ```sh
    mvn clean install
    mvn exec:java

## Uso

### Interfaz de Usuario

En esta sección se mostrarán capturas de pantalla de la interfaz de usuario y sus explicaciones.


### Comandos Principales

- **$help**: Muestra una lista de comandos.
  ```sh
  $help

- **$privadita**: Inicia una partida privada con 10 jugadores.
  ```sh
  $privadita jugador1 jugador2 jugador3 jugador4 jugador5 jugador6 jugador7 jugador8 jugador9 jugador10
  
- **$dropPrivadita**: Elimina la partida privada actual.
  ```sh
  $dropPrivadita
  
- **$addPlayer <nombre>**: Añade un jugador a la base de datos.
  ```sh
  $addPlayer jugador1
- **$addPlayers <nombre1> <nombre2> ...**: Añade múltiples jugadores a la base de datos.
  ```sh
  $addPlayers jugador1 jugador2 jugador3
- **$deletePlayer <nombre>**: Elimina un jugador de la base de datos.
  ```sh
  $deletePlayer jugador1
- **$verElo <nombre>**: Muestra el Elo de un jugador.
  ```sh
  $verElo jugador1
- **$verElo**: Muestra el Elo de todos los jugadores.
  ```sh
  $verElo
- **$join**: Une al bot a un canal de voz.
  ```sh
  $join
- **$leave**: Desconecta al bot del canal de voz.
  ```sh
  $leave
- **$play <URL>**: Reproduce un video de YouTube. (No funciona)
  ```sh
  $play https://www.youtube.com/watch?v=dQw4w9WgXcQ
- **$rolladie <número de lados>**: Lanza un dado con el número especificado de lados.
  ```sh
  $rolladie 20
