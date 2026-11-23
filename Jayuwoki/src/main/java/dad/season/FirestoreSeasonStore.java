package dad.season;

import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * Estado de temporada en Firestore, junto a los datos que ya usaba el bot:
 * <pre>
 * {guildId}/Privadita/Players/{nombre}   ← jugadores (ya existía)
 * {guildId}/Season                       ← NUEVO: currentSeason, status (ACTIVE|CLOSING), closingSeason,
 *                                          lastClosedSeason, timezone, updatedAt
 * {guildId}/Season/Archive/{n}           ← NUEVO: histórico, un documento por temporada cerrada con el CSV
 *                                          completo (season, fileName, csv, players, createdAt)
 * </pre>
 * Un documento de Firestore admite hasta 1 MiB: el CSV cabe de sobra (≈15.000 jugadores).
 */
public class FirestoreSeasonStore implements SeasonStore {

    /** Valores iniciales de un jugador (los mismos que usa $addPlayer y $adminResetElo). */
    public static final int DEFAULT_ELO = 1000;

    /**
     * Campos que se reinician al cambiar de temporada. Se usa update() y no set(), así que el resto del
     * documento (name, idPlayer, discriminator, avatarURL, role) se conserva. winRate y winRateFormatted
     * se incluyen porque Firestore los guarda al serializar Player (son getters), y deben quedar coherentes.
     */
    static final Map<String, Object> RESET_FIELDS = Map.of(
            "elo", DEFAULT_ELO,
            "wins", 0,
            "losses", 0,
            "winRate", 0.0,
            "winRateFormatted", "0.00%");

    private static final int BATCH_LIMIT = 400; // Firestore admite 500 escrituras por batch
    /** Margen bajo el límite de 1 MiB por documento. */
    private static final int MAX_CSV_BYTES = 900_000;

    private final Firestore db;

    public FirestoreSeasonStore(Firestore db) {
        this.db = db;
    }

    private DocumentReference stateRef(String guildId) {
        return db.collection(guildId).document("Season");
    }

    private CollectionReference archive(String guildId) {
        return stateRef(guildId).collection("Archive");
    }

    private CollectionReference players(String guildId) {
        return db.collection(guildId).document("Privadita").collection("Players");
    }

    @Override
    public Optional<Stored> read(String guildId) throws Exception {
        DocumentSnapshot snap = stateRef(guildId).get().get();
        return snap.exists() ? Optional.of(parse(snap)) : Optional.empty();
    }

    private static Stored parse(DocumentSnapshot snap) {
        Long current = snap.getLong("currentSeason");
        String status = snap.getString("status");
        Long closing = snap.getLong("closingSeason");
        return new Stored(current == null ? 0 : current,
                "CLOSING".equals(status) ? Status.CLOSING : Status.ACTIVE,
                closing == null ? -1 : closing);
    }

    @Override
    public void initializeIfAbsent(String guildId, long season) throws Exception {
        DocumentReference ref = stateRef(guildId);
        db.runTransaction(tx -> {
            if (!tx.get(ref).get().exists()) {
                Map<String, Object> data = new HashMap<>();
                data.put("currentSeason", season);
                data.put("status", Status.ACTIVE.name());
                data.put("timezone", SeasonCalendar.zone().getId());
                data.put("createdAt", FieldValue.serverTimestamp());
                data.put("updatedAt", FieldValue.serverTimestamp());
                tx.create(ref, data);
            }
            return null;
        }).get();
    }

    @Override
    public boolean beginClosing(String guildId, long season) throws Exception {
        DocumentReference ref = stateRef(guildId);
        DocumentReference archiveRef = archive(guildId).document(String.valueOf(season));
        return db.runTransaction(tx -> {
            // Lecturas (Firestore exige leer todo antes de escribir)
            DocumentSnapshot snap = tx.get(ref).get();
            if (!snap.exists()) {
                return false;
            }
            Stored st = parse(snap);
            if (st.status() != Status.ACTIVE || st.currentSeason() != season) {
                return false;
            }
            boolean archived = tx.get(archiveRef).get().exists();
            List<SeasonCsv.Row> rows = archived ? List.of() : toRows(tx.get(players(guildId)).get());

            // Escrituras: histórico + CLOSING, todo o nada
            if (!archived && (!rows.isEmpty() || season != SeasonCalendar.PRESEASON)) {
                tx.create(archiveRef, archiveData(season, SeasonCsv.build(rows), rows.size()));
            }
            Map<String, Object> upd = new HashMap<>();
            upd.put("status", Status.CLOSING.name());
            upd.put("closingSeason", season);
            upd.put("closingStartedAt", FieldValue.serverTimestamp());
            upd.put("updatedAt", FieldValue.serverTimestamp());
            tx.update(ref, upd);
            return true;
        }).get();
    }

    private static Map<String, Object> archiveData(long season, String csv, int players) {
        if (csv.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > MAX_CSV_BYTES) {
            throw new IllegalStateException("El CSV de la temporada " + season + " supera el tamaño máximo de un documento de Firestore");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("season", season);
        data.put("fileName", SeasonCalendar.csvFileName(season));
        data.put("csv", csv);
        data.put("players", players);
        data.put("createdAt", FieldValue.serverTimestamp());
        return data;
    }

    @Override
    public List<SeasonCsv.Row> readPlayers(String guildId) throws Exception {
        return toRows(players(guildId).get().get());
    }

    private static List<SeasonCsv.Row> toRows(QuerySnapshot snapshot) {
        List<SeasonCsv.Row> rows = new ArrayList<>();
        for (QueryDocumentSnapshot doc : snapshot.getDocuments()) {
            String name = doc.getString("name");
            rows.add(new SeasonCsv.Row(name != null ? name : doc.getId(),
                    intField(doc, "elo", DEFAULT_ELO), intField(doc, "wins", 0), intField(doc, "losses", 0)));
        }
        return rows;
    }

    private static int intField(DocumentSnapshot doc, String field, int def) {
        Long v = doc.getLong(field);
        return v == null ? def : v.intValue();
    }

    @Override
    public void resetPlayers(String guildId) throws Exception {
        List<QueryDocumentSnapshot> docs = players(guildId).get().get().getDocuments();
        for (int i = 0; i < docs.size(); i += BATCH_LIMIT) {
            WriteBatch batch = db.batch();
            for (QueryDocumentSnapshot doc : docs.subList(i, Math.min(i + BATCH_LIMIT, docs.size()))) {
                batch.update(doc.getReference(), RESET_FIELDS);
            }
            batch.commit().get();
        }
    }

    @Override
    public void finishClosing(String guildId, long closedSeason, long newSeason) throws Exception {
        DocumentReference ref = stateRef(guildId);
        db.runTransaction(tx -> {
            DocumentSnapshot snap = tx.get(ref).get();
            if (!snap.exists()) {
                return null;
            }
            Stored st = parse(snap);
            if (st.status() == Status.CLOSING && st.closingSeason() == closedSeason) {
                Map<String, Object> upd = new HashMap<>();
                upd.put("currentSeason", newSeason);
                upd.put("status", Status.ACTIVE.name());
                upd.put("closingSeason", FieldValue.delete());
                upd.put("lastClosedSeason", closedSeason);
                upd.put("updatedAt", FieldValue.serverTimestamp());
                tx.update(ref, upd);
            }
            return null;
        }).get();
    }

    // ==================== HISTÓRICO DE CSV ====================

    @Override
    public Optional<String> readArchivedCsv(String guildId, long season) throws Exception {
        DocumentSnapshot doc = archive(guildId).document(String.valueOf(season)).get().get();
        return doc.exists() ? Optional.ofNullable(doc.getString("csv")) : Optional.empty();
    }

    @Override
    public void saveArchivedCsvIfAbsent(String guildId, long season, String csv) throws Exception {
        DocumentReference ref = archive(guildId).document(String.valueOf(season));
        db.runTransaction(tx -> {
            if (!tx.get(ref).get().exists()) {
                tx.create(ref, archiveData(season, csv, csv.split("\r\n", -1).length - 2));
            }
            return null;
        }).get();
    }

    @Override
    public OptionalLong latestArchivedSeason(String guildId) throws Exception {
        List<QueryDocumentSnapshot> docs = archive(guildId)
                .orderBy("season", Query.Direction.DESCENDING).limit(1).get().get().getDocuments();
        if (docs.isEmpty() || docs.get(0).getLong("season") == null) {
            return OptionalLong.empty();
        }
        return OptionalLong.of(docs.get(0).getLong("season"));
    }
}
