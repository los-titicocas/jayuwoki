package dad.season;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.TreeMap;

/** SeasonStore en memoria para tests. synchronized = equivalente a las transacciones de Firestore. */
class InMemorySeasonStore implements SeasonStore {

    final Map<String, Stored> states = new LinkedHashMap<>();
    final Map<String, Map<String, SeasonCsv.Row>> players = new LinkedHashMap<>();
    /** Equivalente a {guildId}/Season/Archive/{n}. */
    final Map<String, TreeMap<Long, String>> archives = new LinkedHashMap<>();
    int resetCalls = 0;
    /** Si > 0, las próximas N llamadas a resetPlayers fallan tras reiniciar solo al primer jugador. */
    int failNextResets = 0;

    synchronized void putPlayer(String guild, String name, int elo, int wins, int losses) {
        players.computeIfAbsent(guild, g -> new LinkedHashMap<>()).put(name, new SeasonCsv.Row(name, elo, wins, losses));
    }

    synchronized SeasonCsv.Row player(String guild, String name) {
        return players.get(guild).get(name);
    }

    @Override
    public synchronized Optional<Stored> read(String guildId) {
        return Optional.ofNullable(states.get(guildId));
    }

    @Override
    public synchronized void initializeIfAbsent(String guildId, long season) {
        states.putIfAbsent(guildId, new Stored(season, Status.ACTIVE, -1));
    }

    @Override
    public synchronized boolean beginClosing(String guildId, long season) {
        Stored st = states.get(guildId);
        if (st == null || st.status() != Status.ACTIVE || st.currentSeason() != season) {
            return false;
        }
        List<SeasonCsv.Row> rows = readPlayers(guildId);
        if (!rows.isEmpty() || season != SeasonCalendar.PRESEASON) {
            saveArchivedCsvIfAbsent(guildId, season, SeasonCsv.build(rows));
        }
        states.put(guildId, new Stored(season, Status.CLOSING, season));
        return true;
    }

    @Override
    public synchronized List<SeasonCsv.Row> readPlayers(String guildId) {
        return new ArrayList<>(players.getOrDefault(guildId, Map.of()).values());
    }

    @Override
    public synchronized void resetPlayers(String guildId) {
        resetCalls++;
        Map<String, SeasonCsv.Row> map = players.getOrDefault(guildId, new LinkedHashMap<>());
        for (String name : new ArrayList<>(map.keySet())) {
            map.put(name, new SeasonCsv.Row(name, 1000, 0, 0));
            if (failNextResets > 0) {
                failNextResets--;
                throw new RuntimeException("Simulación: el bot se cae a mitad del reinicio");
            }
        }
    }

    @Override
    public synchronized void finishClosing(String guildId, long closedSeason, long newSeason) {
        Stored st = states.get(guildId);
        if (st != null && st.status() == Status.CLOSING && st.closingSeason() == closedSeason) {
            states.put(guildId, new Stored(newSeason, Status.ACTIVE, -1));
        }
    }

    @Override
    public synchronized Optional<String> readArchivedCsv(String guildId, long season) {
        return Optional.ofNullable(archives.getOrDefault(guildId, new TreeMap<>()).get(season));
    }

    @Override
    public synchronized void saveArchivedCsvIfAbsent(String guildId, long season, String csv) {
        archives.computeIfAbsent(guildId, g -> new TreeMap<>()).putIfAbsent(season, csv);
    }

    @Override
    public synchronized OptionalLong latestArchivedSeason(String guildId) {
        TreeMap<Long, String> m = archives.get(guildId);
        return m == null || m.isEmpty() ? OptionalLong.empty() : OptionalLong.of(m.lastKey());
    }
}
