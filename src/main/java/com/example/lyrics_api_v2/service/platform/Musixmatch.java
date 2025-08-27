package com.example.lyrics_api_v2.service.platform;

import com.example.lyrics_api_v2.model.Lyrics;
import com.example.lyrics_api_v2.model.MusixmatchToken;
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
                tokenExpiry = System.currentTimeMillis() + (30 * 60 * 1000); // 30 mins expiry
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
            lyrics = lyrics.replace("\\n", "\n").replaceAll("\\[\\d+:\\d+\\.\\d+\\]", "").trim();
            return lyrics;
        } else {
            return null;
        }
    }

    private Lyrics searchTrackByTitle(String title) {
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
                String artworkUrl = matcher.group(4);

                String lyrics = fetchLyricsByTrackId(trackId, userToken);

                return new Lyrics(artistName, trackName, trackId, "Musixmatch", artworkUrl, lyrics);
            } else {
                return null;
            }

        } catch (Exception e) {
            throw new RuntimeException("Error fetching Musixmatch lyrics: " + e.getMessage());
        }
    }

    private Lyrics searchTrackByTitleAndArtist(String title, String artist) {
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
                String artworkUrl = trackMatcher.group(4);

                return new Lyrics(artistName, trackName, trackId, "Musixmatch", artworkUrl, lyrics);
            } else {
                return null;
            }

        } catch (Exception e) {
            throw new RuntimeException("Error fetching Musixmatch lyrics: " + e.getMessage());
        }
    }

    @Override
    public Lyrics fetchLyrics(String title, String artist) {
        if (artist != null && !artist.isEmpty()) {
            return searchTrackByTitleAndArtist(title, artist);
        } else {
            return searchTrackByTitle(title);
        }
    }
}
