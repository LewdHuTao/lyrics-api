package com.example.lyrics_api_v2.service.platform;

import com.example.lyrics_api_v2.model.Lyrics;
import com.example.lyrics_api_v2.model.LyricsNotFound;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import okhttp3.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Service
public class YouTube implements PlatformClient {

    private final OkHttpClient httpClient = new OkHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private String apiKey;
    private String clientName;
    private String clientVersion;
    private String artistName;
    private String trackTitle;
    private String artworkUrl;
    private Map<String, Object> context = new HashMap<>();

    @PostConstruct
    public void initialize() throws IOException {
        Document doc = Jsoup.connect("https://music.youtube.com/")
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36")
                .header("Accept-Language", "en-US,en;q=0.9")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .get();
        String homeHtml = doc.html();

        apiKey = extract(homeHtml, "\"INNERTUBE_API_KEY\":\"([^\"]+)\"");
        clientName = extract(homeHtml, "\"INNERTUBE_CLIENT_NAME\":\"([^\"]+)\"");
        clientVersion = extract(homeHtml, "\"INNERTUBE_CLIENT_VERSION\":\"([^\"]+)\"");

        if (apiKey == null || clientName == null || clientVersion == null) {
            throw new RuntimeException("Failed to extract API Key / Client Name / Version!");
        }

        context = Map.of(
                "client", Map.of(
                        "clientName", clientName,
                        "clientVersion", clientVersion
                )
        );
    }

    private String extract(String html, String pattern) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(pattern).matcher(html);
        return matcher.find() ? matcher.group(1) : null;
    }

    @Override
    public ResponseEntity<?> fetchLyrics(String title, String artist, String langCode){
        try {
            String query = (artist != null && !artist.isBlank()) ? title + " " + artist : title;
            List<String> songVideoIds = searchSongVideoIds(query);

            if (songVideoIds.isEmpty()) {
                String message;
                if (artist != null && !artist.isEmpty()) {
                    message = "No lyrics found for title '" + title + "' and artist '" + artist + "'.";
                } else {
                    message = "No lyrics found for title '" + title + "'.";
                }
                LyricsNotFound noLyrics = new LyricsNotFound(message);
                return new ResponseEntity<>(noLyrics, HttpStatus.NOT_FOUND);
            }

            for (int i = 0; i < songVideoIds.size(); i++) {
                String videoId = songVideoIds.get(i);
                String browseId = getLyricsBrowseId(videoId);
                if (browseId == null) continue;
                fetchSongContent(videoId);

                String lyricsContent = fetchLyricsContent(browseId);
                if (lyricsContent != null && !lyricsContent.isBlank()) {
                    Lyrics newLyrics = new Lyrics(this.artistName, this.trackTitle, videoId, "YouTube", this.artworkUrl, lyricsContent);
                    return new ResponseEntity<>(newLyrics, HttpStatus.OK);
                }
            }

            String message;
            if (artist != null && !artist.isEmpty()) {
                message = "No lyrics found for title '" + title + "' and artist '" + artist + "'.";
            } else {
                message = "No lyrics found for title '" + title + "'.";
            }
            LyricsNotFound noLyrics = new LyricsNotFound(message);
            return new ResponseEntity<>(noLyrics, HttpStatus.NOT_FOUND);

        } catch (Exception e) {
            throw new RuntimeException("Error fetching YouTube lyrics: " + e.getMessage());
        }
    }

    private List<String> searchSongVideoIds(String query) throws IOException {
        String url = "https://music.youtube.com/youtubei/v1/search?key=" + apiKey;

        Map<String, Object> payload = new HashMap<>();
        payload.put("context", context);
        payload.put("query", query);
        payload.put("params", "EgWKAQIIAWoKEAMQBRAKEAkQBA==");

        RequestBody body = RequestBody.create(objectMapper.writeValueAsString(payload), MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("X-YouTube-Client-Name", clientName)
                .addHeader("X-YouTube-Client-Version", clientVersion)
                .addHeader("X-Origin", "https://music.youtube.com")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) return Collections.emptyList();

            JsonNode root = objectMapper.readTree(response.body().string());
            return extractAllVideoIds(root);
        }
    }

    private List<String> extractAllVideoIds(JsonNode node) {
        List<String> ids = new ArrayList<>();
        if (node == null || node.isMissingNode()) return ids;
        if (
                node.has("musicResponsiveListItemFlexColumnRenderer") &&
                        node.get("musicResponsiveListItemFlexColumnRenderer").has("text")
        ) {
            JsonNode runs = node.get("musicResponsiveListItemFlexColumnRenderer").get("text").get("runs");
            if (runs != null && runs.isArray()) {
                for (int i = 0; i < runs.size(); i++) {
                    JsonNode run = runs.get(i);
                    JsonNode videoIdNode = run.at("/navigationEndpoint/watchEndpoint/videoId");
                    if (!videoIdNode.isMissingNode()) {
                        ids.add(videoIdNode.asText());
                    }
                }
            }
        }

        if (node.isObject()) {
            node.fields().forEachRemaining(entry -> ids.addAll(extractAllVideoIds(entry.getValue())));
        } else if (node.isArray()) {
            for (int i = 0; i < node.size(); i++) {
                JsonNode item = node.get(i);
                ids.addAll(extractAllVideoIds(item));
            }
        }
        return ids;
    }

    private String getLyricsBrowseId(String videoId) throws IOException {
        String url = "https://music.youtube.com/youtubei/v1/next?key=" + apiKey;

        Map<String, Object> payload = Map.of(
                "context", context,
                "videoId", videoId
        );

        RequestBody body = RequestBody.create(objectMapper.writeValueAsString(payload), MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("X-YouTube-Client-Name", clientName)
                .addHeader("X-YouTube-Client-Version", clientVersion)
                .addHeader("X-Origin", "https://music.youtube.com")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) return null;

            JsonNode nextJson = objectMapper.readTree(response.body().string());

            JsonNode tabs = nextJson.at("/contents/singleColumnMusicWatchNextResultsRenderer/tabbedRenderer/tabs");
            if (!tabs.isArray() || tabs.size() == 0) {
                tabs = nextJson.at("/contents/singleColumnMusicWatchNextResultsRenderer/tabbedRenderer/watchNextTabbedResultsRenderer/tabs");
            }
            if (!tabs.isArray() || tabs.size() == 0) return null;

            for (int i = 0; i < tabs.size(); i++) {
                JsonNode tab = tabs.get(i);
                JsonNode tabRenderer = tab.get("tabRenderer");
                if (tabRenderer != null) {
                    String title = tabRenderer.at("/title").asText();
                    if (title == null || title.isBlank()) {
                        title = tabRenderer.at("/title/runs/0/text").asText();
                    }
                    if ("Lyrics".equalsIgnoreCase(title)) {
                        return tabRenderer.at("/endpoint/browseEndpoint/browseId").asText();
                    }
                }
            }
        }
        return null;
    }

    private String fetchSongContent(String videoId) throws IOException {
        String url = "https://music.youtube.com/youtubei/v1/next?key=" + apiKey;

        Map<String, Object> payload = Map.of(
                "context", context,
                "videoId", videoId
        );

        RequestBody body = RequestBody.create(objectMapper.writeValueAsString(payload), MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("X-YouTube-Client-Name", clientName)
                .addHeader("X-YouTube-Client-Version", clientVersion)
                .addHeader("X-Origin", "https://music.youtube.com")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) return null;

            JsonNode root = objectMapper.readTree(response.body().string());
            JsonNode playlistNode = root
                    .at("/contents/singleColumnMusicWatchNextResultsRenderer/tabbedRenderer/watchNextTabbedResultsRenderer/tabs/0/tabRenderer/content/musicQueueRenderer/content/playlistPanelRenderer/contents/0/playlistPanelVideoRenderer");

            String title = playlistNode.at("/title/runs/0/text").asText(null);
            String artist = playlistNode.at("/longBylineText/runs/0/text").asText(null);
            String thumbnailUrl = playlistNode.at("/thumbnail/thumbnails/1/url").asText(null);

            this.trackTitle = title;
            this.artistName = artist;
            this.artworkUrl = thumbnailUrl;
        }
        return  null;
    }

    private String fetchLyricsContent(String browseId) throws IOException {
        String url = "https://music.youtube.com/youtubei/v1/browse?key=" + apiKey;

        Map<String, Object> payload = Map.of(
                "context", context,
                "browseId", browseId
        );

        RequestBody body = RequestBody.create(objectMapper.writeValueAsString(payload), MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("X-YouTube-Client-Name", clientName)
                .addHeader("X-YouTube-Client-Version", clientVersion)
                .addHeader("X-Origin", "https://music.youtube.com")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) return null;

            JsonNode browseJson = objectMapper.readTree(response.body().string());

            JsonNode runs = browseJson.at("/contents/sectionListRenderer/contents/0/musicDescriptionShelfRenderer/description/runs");
            if (!runs.isArray() || runs.size() == 0) {
                runs = browseJson.at("/contents/musicDescriptionShelfRenderer/description/runs");
            }
            if (!runs.isArray() || runs.size() == 0) return null;

            StringBuilder lyrics = new StringBuilder();
            for (int i = 0; i < runs.size(); i++) {
                JsonNode run = runs.get(i);
                lyrics.append(run.get("text").asText());
            }
            return lyrics.toString().trim();
        }
    }
}