const YTMusic = require("ytmusic-api");

class Youtube {
  async getLyrics(title = null) {
    try {
      const ytm = new YTMusic.default();
      await ytm.initialize();

      const song = await ytm.searchSongs(title);
      const data = song[0];
      const artist_name = data.artist.name;
      const track_name = data.name;
      const search_engine = "YouTube";
      const artwork_url = data.thumbnails[1].url;
      const video_id = data.videoId;
      const lyrics_array = await ytm.getLyrics(video_id);
      const lyrics = lyrics_array.join("\n");

      return { artist_name, track_name, search_engine, artwork_url, lyrics };
    } catch (error) {
      return { message: "No lyrics were found." + error , response: "404 Not Found" };
    }
  }
}

module.exports = Youtube;
