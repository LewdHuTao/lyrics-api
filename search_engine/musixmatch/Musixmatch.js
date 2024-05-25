class Musixmatch {
  constructor() {
    this.tokenUrl =
      "https://apic-desktop.musixmatch.com/ws/1.1/token.get?app_id=web-desktop-app-v1.0";
    this.searchTermUrl =
      "https://apic-desktop.musixmatch.com/ws/1.1/track.search?app_id=web-desktop-app-v1.0&page_size=5&page=1&s_track_rating=desc&quorum_factor=1.0";
    this.lyricsUrl =
      "https://apic-desktop.musixmatch.com/ws/1.1/track.subtitle.get?app_id=web-desktop-app-v1.0&subtitle_format=lrc";
    this.lyricsAlternative =
      "https://apic-desktop.musixmatch.com/ws/1.1/macro.subtitles.get?format=json&namespace=lyrics_richsynched&subtitle_format=mxm&app_id=web-desktop-app-v1.0";
  }

  async get(url) {
    const response = await fetch(url, {
      method: "GET",
      headers: {
        authority: "apic-desktop.musixmatch.com",
        cookie: "AWSELBCORS=0; AWSELB=0;",
      },
    });
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }
    return response.text();
  }

  async getToken() {
    const result = await this.get(this.tokenUrl);
    const tokenJson = JSON.parse(result);

    if (tokenJson.message.header.status_code !== 200) {
      throw new Error(result);
    }

    return tokenJson.message.body.user_token;
  }

  async getLyrics(trackId, userToken) {
    const formattedUrl = `${this.lyricsUrl}&track_id=${trackId}&usertoken=${userToken}`;
    const result = await this.get(formattedUrl);
    let lyrics = JSON.parse(result).message.body.subtitle.subtitle_body;
    lyrics = lyrics.replace(/\[\d+:\d+\.\d+\]/g, "");
    lyrics = lyrics.trim();
    return lyrics;
  }

  async searchTrack(title = null, userToken) {
    const formattedUrl = `${this.searchTermUrl}&q_track=${title}&usertoken=${userToken}`;

    const result = await this.get(formattedUrl);
    const listResult = JSON.parse(result);

    const data = listResult.message.body.track_list;

    if (data.length === 0) {
      throw new Error("No tracks found for the given query.");
    }

    const lyricsData = data[0].track;
    const lyrics = await this.getLyrics(lyricsData.track_id, userToken);
    const artist_name = lyricsData.artist_name;
    const track_name = lyricsData.track_name;
    const track_id = lyricsData.track_id;
    const artwork_url = lyricsData.album_coverart_350x350 || null;
    const search_engine = "Musixmatch";
    return {
      artist_name,
      track_name,
      track_id,
      search_engine,
      artwork_url,
      lyrics,
    };
  }

  async getLyricsSearch(title, artist = null, userToken) {
    const formattedUrl = `${this.lyricsAlternative}&usertoken=${userToken}&q_album=&q_artist=${artist}&q_artists=&track_spotify_id=&q_track=${title}`;
    const result = await this.get(formattedUrl);
    const lyricsData = JSON.parse(result);
    const data =
      lyricsData.message.body.macro_calls["track.lyrics.get"].message.body
        .lyrics;
    const data2 =
      lyricsData.message.body.macro_calls["matcher.track.get"].message.body
        .track;

    if (data.length === 0) {
      throw new Error("No tracks found for the given query");
    }

    const lyrics = data.lyrics_body;
    const track_id = data2.track_id;
    const track_name = data2.track_name;
    const artist_name = data2.artist_name;
    const artwork_url = data2.album_coverart_350x350 || null;
    const search_engine = "Musixmatch";
    return {
      artist_name,
      track_name,
      track_id,
      search_engine,
      artwork_url,
      lyrics,
    };
  }
}

module.exports = Musixmatch;
