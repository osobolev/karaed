package karaed.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;

public final class JsonUtil {

    public static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .serializeSpecialFloatingPointValues()
        .create();

    // todo: remove, use only streaming version
    public static <T> T parse(String json, Class<T> cls) {
        return GSON.fromJson(json, cls);
    }

    public static <T> T readFile(Path file, Class<T> cls) throws IOException {
        try (BufferedReader rdr = Files.newBufferedReader(file)) {
            return GSON.fromJson(rdr, cls);
        }
    }

    public static <T> T readFile(Path file, Class<T> cls, Supplier<T> defValue) throws IOException {
        if (!Files.exists(file))
            return defValue.get();
        return readFile(file, cls);
    }

    public static void writeFile(Path file, Object obj) throws IOException {
        try (BufferedWriter wr = Files.newBufferedWriter(file)) {
            GSON.toJson(obj, wr);
        }
    }
}
