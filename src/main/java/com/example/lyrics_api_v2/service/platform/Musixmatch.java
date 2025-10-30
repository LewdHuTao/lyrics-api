package com.example.lyrics_api_v2.service.platform;

import com.example.lyrics_api_v2.model.Lyrics;
import com.example.lyrics_api_v2.model.LyricsNotFound;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final String searchTermUrl = "https://apic-desktop.musixmatch.com/ws/1.1/track.search?app_id=web-desktop-app-v1.0&page_size=5&page=1&s_track_rating=desc&quorum_factor=1.0";
    private final String lyricsUrl = "https://apic-desktop.musixmatch.com/ws/1.1/track.subtitle.get?app_id=web-desktop-app-v1.0&subtitle_format=lrc";
    private final String lyricsAlternative = "https://apic-desktop.musixmatch.com/ws/1.1/macro.subtitles.get?format=json&namespace=lyrics_richsynched&subtitle_format=mxm&app_id=web-desktop-app-v1.0";
    private final String translatedLyricsUrl = "https://apic-desktop.musixmatch.com/ws/1.1/crowd.track.translations.get?app_id=web-desktop-app-v1.0";

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
                new MusixmatchToken("Failed to extract user token from Musixmatch", "500 - Internal Server Error", 500);
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

        Pattern pattern = Pattern.compile("\"subtitle_body\":\"(.*?)\"");
        Matcher matcher = pattern.matcher(result);

        if (matcher.find()) {
            String lyrics = matcher.group(1);
            ObjectMapper mapper = new ObjectMapper();
            lyrics = mapper.readValue("\"" + lyrics.replace("\"", "\\\"") + "\"", String.class)
                    .replaceAll("\\[\\d+:\\d+\\.\\d+\\]", "")
                    .trim();
            return lyrics;
        } else {
            return null;
        }
    }

    private ResponseEntity<?> getTranslatedLyrics(String title, String artist, String langCode) {
        String userToken = getToken();
        String trackId = null, lyrics = null, trackName = null, artistName = null, artworkUrl = null, formattedUrl = null;

        try {
            if (artist != null) {
                formattedUrl = lyricsAlternative + "&usertoken=" + userToken
                        + "&q_album=&q_artist=" + URLEncoder.encode(artist, StandardCharsets.UTF_8)
                        + "&q_artists=&track_spotify_id=&q_track=" + URLEncoder.encode(title, StandardCharsets.UTF_8);

                String result = get(formattedUrl);
                Pattern lyricsPattern = Pattern.compile("\"lyrics_body\":\"(.*?)\"");
                Matcher matcher = lyricsPattern.matcher(result);
                Pattern pattern = Pattern.compile("\"track_id\":(\\d+).*?\"track_name\":\"(.*?)\".*?\"artist_name\":\"(.*?)\".*?\"album_coverart_350x350\":\"(.*?)\"", Pattern.DOTALL);
                Matcher trackMatcher = pattern.matcher(result);

                if (matcher.find() && trackMatcher.find()) {
                    trackId = trackMatcher.group(1);
                    trackName = trackMatcher.group(2);
                    artistName = trackMatcher.group(3);
                    artworkUrl = trackMatcher.group(4).replace("\\/", "/");
                } else {
                    LyricsNotFound noLyrics = new LyricsNotFound("No lyrics found for title " + "'" + title + "'" + " and artist " + "'" + artist + "'.");
                    return new ResponseEntity<>(noLyrics, HttpStatus.NOT_FOUND);
                }
            } else {
                formattedUrl = searchTermUrl + "&q_track=" + URLEncoder.encode(title, StandardCharsets.UTF_8) + "&usertoken=" + userToken;

                String result = get(formattedUrl);
                Pattern pattern = Pattern.compile("\"track_id\":(\\d+).*?\"track_name\":\"(.*?)\".*?\"artist_name\":\"(.*?)\".*?\"album_coverart_350x350\":\"(.*?)\"", Pattern.DOTALL);
                Matcher matcher = pattern.matcher(result);

                if (matcher.find()) {
                    trackId = matcher.group(1);
                    trackName = matcher.group(2);
                    artistName = matcher.group(3);
                    artworkUrl = matcher.group(4).replace("\\/", "/");
                } else {
                    LyricsNotFound noLyrics = new LyricsNotFound("No lyrics found for title " + "'" + title + "'.");
                    return new ResponseEntity<>(noLyrics, HttpStatus.NOT_FOUND);
                }
            }

            ObjectMapper mapper = new ObjectMapper();
            String url = translatedLyricsUrl +
                    "&track_id=" + trackId +
                    "&usertoken=" + userToken +
                    "&selected_language=" + langCode;

            String translatedResult = get(url);
            Pattern translatedPattern = Pattern.compile("\"description\":\"(.*?)\".*?\"selected_language\":\"" + Pattern.quote(langCode) + "\"", Pattern.DOTALL);
            Matcher translatedMatcher = translatedPattern.matcher(translatedResult);

            StringBuilder translatedLyrics = new StringBuilder();
            while (translatedMatcher.find()) {
                String line = translatedMatcher.group(1);
                line = mapper.readValue("\"" + line.replace("\"", "\\\"") + "\"", String.class)
                        .replaceAll("\\[\\d+:\\d+\\.\\d+\\]", "")
                        .trim();
                if (!line.isEmpty()) {
                    translatedLyrics.append(line).append("\n");
                }
            }

            if (translatedLyrics.isEmpty()) {
                LyricsNotFound noTranslatedLyrics = new LyricsNotFound("No translated lyrics available.");
                return new ResponseEntity<>(noTranslatedLyrics, HttpStatus.NOT_FOUND);
            }

            Lyrics newLyrics = new Lyrics(artistName, trackName, trackId, "Musixmatch", artworkUrl, translatedLyrics.toString());
            return new ResponseEntity<>(newLyrics, HttpStatus.OK);
        } catch (Exception e) {
            throw new RuntimeException("Error fetching Musixmatch lyrics: " + e.getMessage());
        }
    }

    private ResponseEntity<?> searchTrackByTitle(String title) {
        String userToken = getToken();
        try {
            String query = URLEncoder.encode(title, StandardCharsets.UTF_8);
            String formattedUrl = searchTermUrl + "&q_track=" + query + "&usertoken=" + userToken;
            String result = get(formattedUrl);

            Pattern pattern = Pattern.compile("\"track_id\":(\\d+).*?\"track_name\":\"(.*?)\".*?\"artist_name\":\"(.*?)\".*?\"album_coverart_350x350\":\"(.*?)\"", Pattern.DOTALL);
            Matcher matcher = pattern.matcher(result);

            if (matcher.find()) {
                String trackId = matcher.group(1);
                String trackName = matcher.group(2);
                String artistName = matcher.group(3);
                String artworkUrl = matcher.group(4).replace("\\/", "/");
                String lyrics = fetchLyricsByTrackId(trackId, userToken);

                Lyrics newLyrics = new Lyrics(artistName, trackName, trackId, "Musixmatch", artworkUrl, lyrics);
                return new ResponseEntity<>(newLyrics, HttpStatus.OK);
            } else {
                LyricsNotFound noLyrics = new LyricsNotFound("No lyrics found for title " + "'" + title + "'.");
                return new ResponseEntity<>(noLyrics, HttpStatus.NOT_FOUND);
            }

        } catch (Exception e) {
            throw new RuntimeException("Error fetching Musixmatch lyrics: " + e.getMessage());
        }
    }

    private ResponseEntity<?> searchTrackByTitleAndArtist(String title, String artist) {
        String userToken = getToken();
        try {
            String formattedUrl = lyricsAlternative + "&usertoken=" + userToken
                    + "&q_album=&q_artist=" + URLEncoder.encode(artist, StandardCharsets.UTF_8)
                    + "&q_artists=&track_spotify_id=&q_track=" + URLEncoder.encode(title, StandardCharsets.UTF_8);

            String result = get(formattedUrl);

            Pattern lyricsPattern = Pattern.compile("\"lyrics_body\":\"(.*?)\"");
            Matcher lyricsMatcher = lyricsPattern.matcher(result);

            Pattern trackPattern = Pattern.compile("\"track_id\":(\\d+).*?\"track_name\":\"(.*?)\".*?\"artist_name\":\"(.*?)\".*?\"album_coverart_350x350\":\"(.*?)\"", Pattern.DOTALL);
            Matcher trackMatcher = trackPattern.matcher(result);

            if (lyricsMatcher.find() && trackMatcher.find()) {
                String lyrics = lyricsMatcher.group(1).replace("\\n", "\n").trim();
                String trackId = trackMatcher.group(1);
                String trackName = trackMatcher.group(2);
                String artistName = trackMatcher.group(3);
                String artworkUrl = trackMatcher.group(4).replace("\\/", "/");

                Lyrics newLyrics = new Lyrics(artistName, trackName, trackId, "Musixmatch", artworkUrl, lyrics);
                return new ResponseEntity<>(newLyrics, HttpStatus.OK);
            } else {
                LyricsNotFound noLyrics = new LyricsNotFound("No lyrics found for title " + "'" + title + "'" + " and artist " + "'" + artist + "'.");
                return new ResponseEntity<>(noLyrics, HttpStatus.NOT_FOUND);
            }

        } catch (Exception e) {
            throw new RuntimeException("Error fetching Musixmatch lyrics: " + e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> fetchLyrics(String title, String artist, String translate) {
        if (artist != null && !artist.isEmpty() && translate == null) {
            return searchTrackByTitleAndArtist(title, artist);
        } else if (artist == null && translate == null) {
            return searchTrackByTitle(title);
        } else if (translate != null && !translate.isEmpty()) {
            return getTranslatedLyrics(title, artist, translate);
        } else {
            return null;
        }
    }
}