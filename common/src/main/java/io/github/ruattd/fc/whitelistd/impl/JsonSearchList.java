package io.github.ruattd.fc.whitelistd.impl;

import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;
import io.github.ruattd.fc.whitelistd.*;
import lombok.NonNull;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.UUID;

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
        Files.writeString(jsonFilePath, "");
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
        if (players_no_uuid.contains(name) || players.containsKey(uuid) || players.containsValue(name)) {
            return AddItemState.DUPLICATE;
        }
        if (uuid == null) {
            players_no_uuid.add(name);
        } else {
            players.put(uuid, name);
        }
        try {
            write();
        } catch (IOException e) {
            if (uuid == null) {
                players_no_uuid.remove(name);
            } else {
                players.remove(uuid);
            }
            return AddItemState.IO_ERROR;
        }
        return AddItemState.SUCCESSFUL;
    }

    @Override @NonNull
    public RemoveItemState removeItem(@NonNull PlayerInfo player) {
        var name = player.getName();
        var uuid = player.getUuid();
        boolean nf1 = !players_no_uuid.remove(name);
        boolean nf2 = true;
        boolean nf;
        if (uuid == null) {
            nf = nf1;
        } else {
            nf2 = players.remove(uuid) == null;
            nf = nf2 && nf1;
        }
        if (nf) {
            return RemoveItemState.NOT_FOUND;
        } else {
            try {
                write();
            } catch (IOException e) {
                if (!nf1) players_no_uuid.add(name);
                if (!nf2) players.put(uuid, name);
                return RemoveItemState.IO_ERROR;
            }
        }
        return RemoveItemState.SUCCESSFUL;
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
                    }
                } else {
                    var r = find_by_name(name);
                    if (r == ZERO_UUID) {
                        found = true;
                    } else if (r != null) {
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
}
