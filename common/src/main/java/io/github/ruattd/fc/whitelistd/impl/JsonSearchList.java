package io.github.ruattd.fc.whitelistd.impl;

import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;
import io.github.ruattd.fc.whitelistd.*;
import lombok.NonNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

public class JsonSearchList implements SearchList {
    private SearchMode mode;
    private Path jsonFilePath;

    private final Gson gson = new Gson();
    private SearchListData data = null;

    private void writeFile() throws IOException {
        var writer = new JsonWriter(Files.newBufferedWriter(jsonFilePath));
        gson.toJson(data, SearchListData.class, writer);
        writer.close();
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
                data = gson.fromJson(Files.newBufferedReader(jsonFilePath), SearchListData.class);
                writeFile();
            } catch (IOException e) {
                throw new WhitelistdRuntimeException("Failed to read/write whitelist JSON file", e);
            }
        } else {
            try {
                Files.createFile(jsonFilePath);
                data = new SearchListData();
            } catch (IOException e) {
                throw new WhitelistdRuntimeException("Failed to create whitelist JSON file", e);
            }
        }
    }

    @Override @NonNull
    public AddItemState addItem(@NonNull PlayerInfo player) {
        if (data.players_no_uuid.contains(player.name)) {
            return AddItemState.DUPLICATE;
        } else {
            var name = player.getName();
            var uuid = player.getUuid();
            if (uuid == null) {
                data.players_no_uuid.add(name);
            } else {
                data.players.put(uuid, name);
            }
            try {
                writeFile();
            } catch (IOException e) {
                if (uuid == null) {
                    data.players_no_uuid.remove(name);
                } else {
                    data.players.remove(uuid);
                }
                return AddItemState.IO_ERROR;
            }
            return AddItemState.SUCCESSFUL;
        }
    }

    @Override @NonNull
    public RemoveItemState removeItem(@NonNull PlayerInfo player) {
        var name = player.getName();
        var uuid = player.getUuid();
        boolean nf1 = !data.players_no_uuid.remove(name);
        boolean nf2 = true;
        boolean nf;
        if (uuid == null) {
            nf = nf1;
        } else {
            nf2 = data.players.remove(uuid) == null;
            nf = nf2 && nf1;
        }
        if (nf) {
            return RemoveItemState.NOT_FOUND;
        } else {
            try {
                writeFile();
            } catch (IOException e) {
                if (!nf1) data.players_no_uuid.add(name);
                if (!nf2) data.players.put(uuid, name);
                return RemoveItemState.IO_ERROR;
            }
        }
        return RemoveItemState.SUCCESSFUL;
    }

    @Override @NonNull
    public QueryResult query(@NonNull PlayerInfo player) {
        var name = player.getName();
        var uuid = player.getUuid();
        String r1 = null;
        boolean r2 = false;
        if (mode != SearchMode.PLAYER_UUID) r2 = data.players_no_uuid.contains(name);
        if (player.uuid != null) r1 = data.players.get(uuid);
        boolean found = false;
        switch (mode) {
            case PLAYER_NAME -> {
                if (r2) {
                    found = true;
                } else if (r1 != null) {
                    found = true;
                    name = r1;
                }
            }
            case PLAYER_UUID -> {
                if (r1 != null) {
                    found = true;
                    name = r1;
                }
            }
            case PLAYER_NAME_OR_UUID -> {
                if (r1 == null) {
                    if (r2) found = true;
                } else {
                    found = true;
                    name = r1;
                }
            }
        }
        return new QueryResult(found, name);
    }

    @Override @NonNull
    public ClearState clear() {
        try {
            Files.writeString(jsonFilePath, "");
        } catch (IOException e) {
            return ClearState.IO_ERROR;
        }
        return ClearState.SUCCESSFUL;
    }

    private static class SearchListData {
        final HashMap<UUID, String> players;
        final HashSet<String> players_no_uuid;

        SearchListData() {
            players = new HashMap<>();
            players_no_uuid = new HashSet<>();
        }
    }
}
