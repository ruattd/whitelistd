package io.github.ruattd.fc.whitelistd.impl;

import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;
import io.github.ruattd.fc.whitelistd.*;
import lombok.NonNull;
import org.apache.commons.collections4.MapIterator;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class JsonSearchList implements SearchList {
    private SearchMode mode;
    private Path jsonFilePath;

    private final Gson gson = new Gson();
    private final DualHashBidiMap<UUID, String> players = new DualHashBidiMap<>();
    private final HashSet<String> players_no_uuid = new HashSet<>();

    private final static UUID ZERO_UUID = new UUID(0L, 0L);

    private void write() throws IOException {
        var writer = new JsonWriter(Files.newBufferedWriter(jsonFilePath));
        var list = new ArrayList<PlayerItem>(players.size() + players_no_uuid.size());
        players.forEach((uuid, name) -> list.add(new PlayerItem(name, uuid.toString())));
        players_no_uuid.forEach(name -> list.add(new PlayerItem(name, "")));
        gson.toJson(list.toArray(new PlayerItem[0]), PlayerItem[].class, writer);
        writer.close();
    }

    private void writeEmpty() throws IOException {
        Files.writeString(jsonFilePath, "[]");
    }

    @Override
    public void init(@NonNull SearchMode mode, boolean playerNameCaseSensitive, @NonNull String @NonNull [] args) {
        this.mode = mode;
        var specificPath = Path.of(args[0]);
        if (specificPath.isAbsolute()) {
            jsonFilePath = specificPath;
        } else {
            jsonFilePath = Whitelistd.getInstance().getConfigDir().resolve(specificPath);
        }

        if (Files.exists(jsonFilePath)) {
            try {
                var data = gson.fromJson(Files.newBufferedReader(jsonFilePath), PlayerItem[].class);
                for (var item : data) {
                    var name = item.name();
                    try {
                        var uuid = UUID.fromString(item.uuid());
                        players.put(uuid, name);
                    } catch (IllegalArgumentException ignored) {
                        players_no_uuid.add(name);
                    }
                }
                write();
            } catch (IOException e) {
                throw new WhitelistdRuntimeException("Failed to read/write whitelist JSON file", e);
            }
        } else {
            try {
                Files.createFile(jsonFilePath);
            } catch (IOException e) {
                throw new WhitelistdRuntimeException("Failed to create whitelist JSON file", e);
            }
        }
    }

    @Override @NonNull
    public AddItemState addItem(@NonNull PlayerInfo player) {
        var name = player.getName();
        var uuid = player.getUuid();
        var c1n = players_no_uuid.contains(name);
        var c2u = players.containsKey(uuid);
        var c2n = players.containsValue(name);
        boolean a1 = false, d1 = false;
        boolean a2 = false;
        if (uuid == null) {
            if (c1n || c2n) return AddItemState.DUPLICATE;
            players_no_uuid.add(name); a1 = true;
        } else {
            if (c2u || c2n) {
                return AddItemState.DUPLICATE;
            } else {
                if (c1n) {
                    players_no_uuid.remove(name); d1 = true;
                }
                players.put(uuid, name); a2 = true;
            }
        }
        try {
            write();
        } catch (IOException e) {
            if (a1) players_no_uuid.remove(name);
            if (a2) players.remove(uuid, name);
            if (d1) players_no_uuid.add(name);
            return AddItemState.IO_ERROR;
        }
        return AddItemState.SUCCESSFUL;
    }

    @Override @NonNull
    public RemoveItemState removeItem(@NonNull PlayerInfo player) {
        var name = player.getName();
        var uuid = player.getUuid();
        boolean f1 = false;
        boolean f2;
        boolean f;
        if (uuid == null) {
            f1 = players_no_uuid.remove(name);
            f2 = players.removeValue(name) != null;
            f = f1 || f2;
        } else {
            f2 = players.remove(uuid) != null;
            f = f2;
        }
        if (f) {
            try {
                write();
            } catch (IOException e) {
                if (f1) players_no_uuid.add(name);
                if (f2) players.put(uuid, name);
                return RemoveItemState.IO_ERROR;
            }
            return RemoveItemState.SUCCESSFUL;
        } else {
            return RemoveItemState.NOT_FOUND;
        }
    }

    private UUID find_by_name(@NonNull String name) {
        var r1 = players.getKey(name);
        var r2 = players_no_uuid.contains(name);
        if (r1 != null) return r1;
        if (r2) return ZERO_UUID;
        return null;
    }

    private String find_by_uuid(@NonNull UUID uuid) {
        return players.get(uuid);
    }

    @Override @NonNull
    public QueryResult query(@NonNull PlayerInfo player) {
        var name = player.getName();
        var uuid = player.getUuid();
        boolean found = false;
        switch (mode) {
            case PLAYER_NAME -> {
                var r = find_by_name(name);
                if (r != null) {
                    found = true;
                    if (r != ZERO_UUID) uuid = r;
                }
            }
            case PLAYER_UUID -> {
                var r = find_by_uuid(uuid);
                if (r != null) {
                    found = true;
                    name = r;
                }
            }
            case PLAYER_NAME_OR_UUID -> {
                if (uuid != null) {
                    var r = find_by_uuid(uuid);
                    if (r != null) {
                        found = true;
                        name = r;
                        break;
                    }
                }
                var r = find_by_name(name);
                if (r != null) {
                    found = true;
                    if (r == ZERO_UUID) {
                        uuid = null;
                    } else {
                        uuid = r;
                    }
                }
            }
        }
        return new QueryResult(found, new PlayerInfo(name, uuid));
    }

    @Override @NonNull
    public ClearState clear() {
        try {
            writeEmpty();
            players.clear();
            players_no_uuid.clear();
        } catch (IOException e) {
            return ClearState.IO_ERROR;
        }
        return ClearState.SUCCESSFUL;
    }

    private record PlayerItem(String name, String uuid) {}

    @Override
    public int size() {
        return players.size() + players_no_uuid.size();
    }

    @Override @NonNull
    public Iterable<PlayerInfo> getItems() {
        return () -> new Iterator<>() {
            private final Iterator<String> players_no_uuid = JsonSearchList.this.players_no_uuid.iterator();
            private final MapIterator<UUID, String> players = JsonSearchList.this.players.mapIterator();

            private int currentIterator = 0;

            @Override
            public boolean hasNext() {
                switch (currentIterator) {
                    case 0 -> {
                        var hasNext = players_no_uuid.hasNext();
                        if (hasNext) {
                            return true;
                        } else {
                            currentIterator = 1;
                            return hasNext();
                        }
                    }
                    case 1 -> {
                        return players.hasNext();
                    }
                }
                return false;
            }

            @Override
            public PlayerInfo next() {
                switch (currentIterator) {
                    case 0 -> {
                        var next = players_no_uuid.next();
                        if (next == null) {
                            currentIterator = 1;
                            return next();
                        } else {
                            return new PlayerInfo(next);
                        }
                    }
                    case 1 -> {
                        var next = players.next();
                        if (next == null) {
                            return null;
                        } else {
                            var name = JsonSearchList.this.players.get(next);
                            return new PlayerInfo(name, next);
                        }
                    }
                }
                return null;
            }
        };
    }
}
