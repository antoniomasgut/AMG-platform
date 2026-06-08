package com.amg.digitalitzacio.agents.application;

import com.amg.digitalitzacio.shared.sysconfig.application.SystemConfigService;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final Gson GSON = new Gson();

    private final SystemConfigService systemConfig;

    private static final int DIMENSIONS = 1536;

    public float[] embed(String text) {
        var apiKey = resolveApiKey();
        if (apiKey == null) return null;

        try {
            var body = new JsonObject();
            body.addProperty("input", text);
            body.addProperty("model", "text-embedding-3-small");
            body.addProperty("dimensions", DIMENSIONS);

            var request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.openai.com/v1/embeddings"))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(body)))
                .build();

            var response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("Embedding API failed: {} {}", response.statusCode(), response.body());
                return null;
            }

            var json = GSON.fromJson(response.body(), JsonObject.class);
            var data = json.getAsJsonArray("data");
            if (data == null || data.isEmpty()) return null;

            var embedding = data.get(0).getAsJsonObject().getAsJsonArray("embedding");
            var vec = new float[embedding.size()];
            for (int i = 0; i < embedding.size(); i++) {
                vec[i] = embedding.get(i).getAsFloat();
            }
            return vec;
        } catch (Exception e) {
            log.error("Embedding error: {}", e.getMessage());
            return null;
        }
    }

    public List<Float> embedAsList(String text) {
        var vec = embed(text);
        if (vec == null) return null;
        var list = new ArrayList<Float>(vec.length);
        for (var v : vec) list.add(v);
        return list;
    }

    public String embedToJson(String text) {
        var vec = embed(text);
        if (vec == null) return null;
        return GSON.toJson(vec);
    }

    /** Vectoritza múltiples textos amb una sola crida a l'API (batch). */
    public List<String> embedBatchToJson(List<String> texts) {
        var apiKey = resolveApiKey();
        if (apiKey == null || texts == null || texts.isEmpty()) return List.of();
        try {
            var inputArray = new JsonArray();
            texts.forEach(inputArray::add);

            var body = new JsonObject();
            body.add("input", inputArray);
            body.addProperty("model", "text-embedding-3-small");
            body.addProperty("dimensions", DIMENSIONS);

            var request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.openai.com/v1/embeddings"))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(body)))
                .build();

            var response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("Batch embedding API failed: {}", response.statusCode());
                return List.of();
            }

            var data = GSON.fromJson(response.body(), JsonObject.class).getAsJsonArray("data");
            var results = new ArrayList<String>(data.size());
            for (int i = 0; i < data.size(); i++) {
                var embedding = data.get(i).getAsJsonObject().getAsJsonArray("embedding");
                var vec = new float[embedding.size()];
                for (int j = 0; j < embedding.size(); j++) vec[j] = embedding.get(j).getAsFloat();
                results.add(GSON.toJson(vec));
            }
            return results;
        } catch (Exception e) {
            log.error("Batch embedding error: {}", e.getMessage());
            return List.of();
        }
    }

    public double cosineSimilarity(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length) return 0;
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        double denom = Math.sqrt(normA) * Math.sqrt(normB);
        return denom == 0 ? 0 : dot / denom;
    }

    public double cosineSimilarity(String jsonA, String jsonB) {
        if (jsonA == null || jsonB == null) return 0;
        try {
            var arrA = GSON.fromJson(jsonA, float[].class);
            var arrB = GSON.fromJson(jsonB, float[].class);
            return cosineSimilarity(arrA, arrB);
        } catch (Exception e) {
            return 0;
        }
    }

    public float[] parseEmbedding(String json) {
        if (json == null) return null;
        try {
            return GSON.fromJson(json, float[].class);
        } catch (Exception e) {
            return null;
        }
    }

    private String resolveApiKey() {
        var key = systemConfig.get("OPENAI_API_KEY");
        if (key != null && !key.isBlank()) return key;
        log.warn("No OPENAI_API_KEY configured for embeddings — vectorization disabled");
        return null;
    }
}
