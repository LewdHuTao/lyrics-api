package com.example.lyrics_api_v2.service.platform;

import com.example.lyrics_api_v2.model.InternalServerError;
import com.example.lyrics_api_v2.model.Lyrics;
import com.example.lyrics_api_v2.model.LyricsNotFound;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.internal.Internal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class Musixmatch implements PlatformClient {

    private String cachedToken = null;
    private long tokenExpiry = 0;

    private final String tokenUrl = "https://apic-desktop.musixmatch.com/ws/1.1/token.get?app_id=web-desktop-app-v1.0";
    private final String lyricsTrackSearchUrl = "https://apic-desktop.musixmatch.com/ws/1.1/track.search?app_id=web-desktop-app-v1.0&page_size=5&page=1&s_track_rating=desc&quorum_factor=1.0";
    private final String lyricsUrl = "https://apic-desktop.musixmatch.com/ws/1.1/track.subtitle.get?app_id=web-desktop-app-v1.0&subtitle_format=lrc";
    private final String lyricsMacroSubUrl = "https://apic-desktop.musixmatch.com/ws/1.1/macro.subtitles.get?format=json&namespace=lyrics_richsynched&subtitle_format=mxm&app_id=web-desktop-app-v1.0";
    private final String lyricsTranslatedUrl = "https://apic-desktop.musixmatch.com/ws/1.1/crowd.track.translations.get?app_id=web-desktop-app-v1.0";
    private final String lyricsTrackGetUrl = "https://apic-desktop.musixmatch.com/ws/1.1/track.get?app_id=web-desktop-app-v1.0";

    private String get(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setInstanceFollowRedirects(false);
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", "Mozilla/5.0");
        conn.setRequestProperty("cookie", "AWSELBCORS=0; AWSELB=0;");

        int status = conn.getResponseCode();
        if (status == 301 || status == 302) {
            String redirectUrl = conn.getHeaderField("Location");
            if (redirectUrl == null) {
                throw new RuntimeException("Redirected but no Location header found.");
            }
            return get(redirectUrl);
        }

        if (status != HttpURLConnection.HTTP_OK) {
            throw new RuntimeException("Failed to fetch: Status " + status);
        }

        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder content = new StringBuilder();
        String inputLine;
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();
        conn.disconnect();
        return content.toString();
    }

    private synchronized void refreshToken() {
        try {
            String result = get(tokenUrl);
            Pattern pattern = Pattern.compile("\"user_token\":\"(.*?)\"");
            Matcher matcher = pattern.matcher(result);
            if (matcher.find()) {
                cachedToken = matcher.group(1);
                tokenExpiry = System.currentTimeMillis() + (30 * 60 * 1000);
            } else {
                throw new RuntimeException("Failed to extract user token from Musixmatch.");
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to refresh token: " + e.getMessage());
        }
    }

    private String getToken() {
        if (cachedToken == null || System.currentTimeMillis() >= tokenExpiry) {
            refreshToken();
        }
        return cachedToken;
    }

    private String fetchLyricsByTrackId(String trackId, String userToken) throws Exception {
        String formattedUrl = lyricsUrl + "&track_id=" + trackId + "&usertoken=" + userToken;
        String result = get(formattedUrl);

        Pattern pattern = Pattern.compile("\"subtitle_body\":\"(.*?)\"", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(result);

        if (matcher.find()) {
            String raw = matcher.group(1);
            String lyrics = raw
                    .replace("\\n", "\n")
                    .replace("\\r", "")
                    .replace("\\t", "\t")
                    .replace("\\\"", "\"")
                    .replace("\\/", "/")
                    .replace("\\\\", "\\");

            lyrics = lyrics.replaceAll("\\[\\d{2}:\\d{2}\\.\\d{2}\\]", "");
            lyrics = lyrics
                    .replaceAll("(?m)^\\s+", "")
                    .replaceAll("(?m)\\s+$", "")
                    .replaceAll("\\n{2,}", "\n")
                    .trim();

            return lyrics.isEmpty() ? null : lyrics;
        } else {
            return null;
        }
    }

    private String fetchTranslatedLyrics(String trackId, String langCode, String userToken) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String url = lyricsTranslatedUrl +
                "&track_id=" + trackId +
                "&usertoken=" + userToken +
                "&selected_language=" + langCode;

        String translatedResult = get(url);
        JsonNode root = mapper.readTree(translatedResult);

        StringBuilder translatedLyrics = new StringBuilder();
        JsonNode translations = root.path("message").path("body").path("translations_list");

        if (translations.isArray()) {
            for (int i = 0; i < translations.size(); i++) {
                JsonNode description = translations.get(i).path("translation").path("description");
                if (!description.isMissingNode()) {
                    String line = description.asText();

                    line = line
                            .replace("\\n", "\n")
                            .replace("\\r", "")
                            .replace("\\t", "\t")
                            .replace("\\\"", "\"")
                            .replace("\\/", "/")
                            .replace("\\\\", "\\");

                    Matcher unicodeMatcher = Pattern.compile("\\\\u([0-9A-Fa-f]{4})").matcher(line);
                    StringBuffer sb = new StringBuffer();

                    while (unicodeMatcher.find()) {
                        String unicodeChar = String.valueOf((char) Integer.parseInt(unicodeMatcher.group(1), 16));
                        unicodeMatcher.appendReplacement(sb, Matcher.quoteReplacement(unicodeChar));
                    }

                    unicodeMatcher.appendTail(sb);
                    line = sb.toString().trim();

                    if (!line.isEmpty()) {
                        translatedLyrics.append(line).append("\n");
                    }
                }
            }
        }

        if (translatedLyrics.isEmpty()) {
            return null;
        }

        return translatedLyrics.toString();
    }

    private ResponseEntity<?> getLyrics(String trackId, String title, String artist, String langCode) throws Exception {
        String userToken = getToken();

        trackId = (trackId == null) ? "" : trackId.trim();
        title = (title == null) ? "" : title.trim();
        artist = (artist == null) ? "" : artist.trim();
        langCode = (langCode == null) ? "" : langCode.trim();

        if (!trackId.isEmpty() && langCode.isEmpty() && title.isEmpty() && artist.isEmpty()) {
            String trackName = null, artistName = null, albumCoverArt = null;

            String lyrics = fetchLyricsByTrackId(trackId, userToken);

            if (lyrics == null) {
                LyricsNotFound noLyrics = new LyricsNotFound("No lyrics found for trackId " + "'" + trackId + "'.");
                return new ResponseEntity<>(noLyrics, HttpStatus.NOT_FOUND);
            }

            String formattedUrl = lyricsTrackGetUrl + "&track_id=" + trackId + "&usertoken=" + userToken;
            String result = get(formattedUrl);

            Pattern pattern = Pattern.compile("\"track_name\":\"(.*?)\".*?\"artist_name\":\"(.*?)\".*?\"album_coverart_350x350\":\"(.*?)\"", Pattern.DOTALL);
            Matcher detail = pattern.matcher(result);

            if (detail.find()) {
                trackName = detail.group(1);
                artistName = detail.group(2);
                albumCoverArt = detail.group(3);
            }

            Lyrics newLyrics = new Lyrics(artistName, trackName, trackId, "Musixmatch", albumCoverArt, lyrics);
            return new ResponseEntity<>(newLyrics, HttpStatus.OK);
        } else if (trackId.isEmpty() && !title.isEmpty() && artist.isEmpty() && langCode.isEmpty()) {
            String formattedUrl = lyricsTrackSearchUrl + "&q_track=" + URLEncoder.encode(title, StandardCharsets.UTF_8) + "&usertoken=" + userToken;
            String result = get(formattedUrl);

            Pattern pattern = Pattern.compile("\"track_id\":(\\d+).*?\"track_name\":\"(.*?)\".*?\"artist_name\":\"(.*?)\".*?\"album_coverart_350x350\":\"(.*?)\"", Pattern.DOTALL);
            Matcher matcher = pattern.matcher(result);

            if (matcher.find()) {
                String track_Id = matcher.group(1);
                String trackName = matcher.group(2);
                String artistName = matcher.group(3);
                String artworkUrl = matcher.group(4).replace("\\/", "/");
                String lyrics = fetchLyricsByTrackId(track_Id, userToken);

                Lyrics newLyrics = new Lyrics(artistName, trackName, track_Id, "Musixmatch", artworkUrl, lyrics);
                return new ResponseEntity<>(newLyrics, HttpStatus.OK);
            } else {
                LyricsNotFound noLyrics = new LyricsNotFound("No lyrics found for title " + "'" + title + "'.");
                return new ResponseEntity<>(noLyrics, HttpStatus.NOT_FOUND);
            }
        } else if (trackId.isEmpty() && !title.isEmpty() && !artist.isEmpty() && langCode.isEmpty()) {
            String formattedUrl = lyricsMacroSubUrl + "&usertoken=" + userToken
                    + "&q_album=&q_artist=" + URLEncoder.encode(artist, StandardCharsets.UTF_8)
                    + "&q_artists=&track_spotify_id=&q_track=" + URLEncoder.encode(title, StandardCharsets.UTF_8);

            String result = get(formattedUrl);

            Pattern lyricsPattern = Pattern.compile("\"lyrics_body\":\"(.*?)\"");
            Matcher lyricsMatcher = lyricsPattern.matcher(result);

            Pattern trackPattern = Pattern.compile("\"track_id\":(\\d+).*?\"track_name\":\"(.*?)\".*?\"artist_name\":\"(.*?)\".*?\"album_coverart_350x350\":\"(.*?)\"", Pattern.DOTALL);
            Matcher trackMatcher = trackPattern.matcher(result);

            if (lyricsMatcher.find() && trackMatcher.find()) {
                String lyrics = lyricsMatcher.group(1).replace("\\n", "\n").trim();
                String track_Id = trackMatcher.group(1);
                String trackName = trackMatcher.group(2);
                String artistName = trackMatcher.group(3);
                String artworkUrl = trackMatcher.group(4).replace("\\/", "/");

                Lyrics newLyrics = new Lyrics(artistName, trackName, track_Id, "Musixmatch", artworkUrl, lyrics);
                return new ResponseEntity<>(newLyrics, HttpStatus.OK);
            } else {
                LyricsNotFound noLyrics = new LyricsNotFound("No lyrics found for title " + "'" + title + "'" + " and artist " + "'" + artist + "'.");
                return new ResponseEntity<>(noLyrics, HttpStatus.NOT_FOUND);
            }
        } else if (trackId.isEmpty() && !title.isEmpty() && artist.isEmpty() && !langCode.isEmpty()) {
            String track_Id = null, trackName = null, artistName = null, artworkUrl = null;

            String formattedUrl = lyricsTrackGetUrl + "&q_track=" + URLEncoder.encode(title, StandardCharsets.UTF_8) + "&usertoken=" + userToken;

            String result = get(formattedUrl);
            Pattern pattern = Pattern.compile("\"track_id\":(\\d+).*?\"track_name\":\"(.*?)\".*?\"artist_name\":\"(.*?)\".*?\"album_coverart_350x350\":\"(.*?)\"", Pattern.DOTALL);
            Matcher matcher = pattern.matcher(result);

            if (matcher.find()) {
                trackId = matcher.group(1);
                trackName = matcher.group(2);
                artistName = matcher.group(3);
                artworkUrl = matcher.group(4).replace("\\/", "/");
            } else {
                LyricsNotFound noLyrics = new LyricsNotFound("No translated lyrics found.");
                return new ResponseEntity<>(noLyrics, HttpStatus.NOT_FOUND);
            }

            String translatedLyrics = fetchTranslatedLyrics(track_Id, langCode, userToken);

            Lyrics newLyrics = new Lyrics(artistName, trackName, track_Id, "Musixmatch", artworkUrl, translatedLyrics);
            return new ResponseEntity<>(newLyrics, HttpStatus.OK);
        } else if (trackId.isEmpty() && !title.isEmpty() && !artist.isEmpty() && !langCode.isEmpty()) {
            String track_Id = null, trackName = null, artistName = null, artworkUrl = null;

            String formattedUrl = lyricsTrackSearchUrl + "&usertoken=" + userToken
                    + "&q_album=&q_artist=" + URLEncoder.encode(artist, StandardCharsets.UTF_8)
                    + "&q_artists=&track_spotify_id=&q_track=" + URLEncoder.encode(title, StandardCharsets.UTF_8);

            String result = get(formattedUrl);
            Pattern pattern = Pattern.compile("\"track_id\":(\\d+).*?\"track_name\":\"(.*?)\".*?\"artist_name\":\"(.*?)\".*?\"album_coverart_350x350\":\"(.*?)\"", Pattern.DOTALL);
            Matcher trackMatcher = pattern.matcher(result);

            if (trackMatcher.find()) {
                track_Id = trackMatcher.group(1);
                trackName = trackMatcher.group(2);
                artistName = trackMatcher.group(3);
                artworkUrl = trackMatcher.group(4).replace("\\/", "/");
            } else {
                LyricsNotFound noLyrics = new LyricsNotFound("No translated lyrics found.");
                return new ResponseEntity<>(noLyrics, HttpStatus.NOT_FOUND);
            }

            String translatedLyrics = fetchTranslatedLyrics(track_Id, langCode, userToken);

            Lyrics newLyrics = new Lyrics(artistName, trackName, track_Id, "Musixmatch", artworkUrl, translatedLyrics);
            return new ResponseEntity<>(newLyrics, HttpStatus.OK);
        } else if (!trackId.isEmpty() && !langCode.isEmpty() && title.isEmpty() && artist.isEmpty()) {
            String trackName = null, artistName = null, albumCoverArt = null;

            String formattedUrl = lyricsTrackGetUrl + "&track_id=" + trackId + "&usertoken=" + userToken;
            String result = get(formattedUrl);

            Pattern pattern = Pattern.compile("\"track_name\":\"(.*?)\".*?\"artist_name\":\"(.*?)\".*?\"album_coverart_350x350\":\"(.*?)\"", Pattern.DOTALL);
            Matcher detail = pattern.matcher(result);

            if (detail.find()) {
                trackName = detail.group(1);
                artistName = detail.group(2);
                albumCoverArt = detail.group(3);
            } else {
                LyricsNotFound noLyrics = new LyricsNotFound("No translated lyrics found.");
                return new ResponseEntity<>(noLyrics, HttpStatus.NOT_FOUND);
            }

            String translatedLyrics = fetchTranslatedLyrics(trackId, langCode, userToken);

            Lyrics newLyrics = new Lyrics(artistName, trackName, trackId, "Musixmatch", albumCoverArt, translatedLyrics);
            return new ResponseEntity<>(newLyrics, HttpStatus.OK);
        } else {
            InternalServerError badReq = new InternalServerError("Invalid or unsupported parameter combination.");
            return new ResponseEntity<>(badReq, HttpStatus.BAD_REQUEST);
        }
    }

    @Override
    public ResponseEntity<?> fetchLyrics(String trackId, String title, String artist, String translate) throws Exception {
        return getLyrics(trackId, title, artist, translate);
    }
}