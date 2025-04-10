package karaed.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class JsonUtil {

    private static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .serializeSpecialFloatingPointValues()
        .create();

    public static <T> T readFile(Path file, Class<T> cls) throws IOException {
        try (BufferedReader rdr = Files.newBufferedReader(file)) {
            return GSON.fromJson(rdr, cls);
        }
    }

    public static void writeFile(Path file, Object obj) throws IOException {
        try (BufferedWriter wr = Files.newBufferedWriter(file)) {
            GSON.toJson(obj, wr);
        }
    }
}
