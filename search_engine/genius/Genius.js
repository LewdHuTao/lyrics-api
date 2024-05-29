const cheerio = require("cheerio");
const axios = require("axios");

class Genius {
  constructor() {
    this.geniusURL = "https://api.genius.com/search";
  }

  async getLyrics(title, api_key = null) {
    try {
      const res = await axios.get(`${this.geniusURL}?q=${encodeURIComponent(title)}`, {
        headers: {
          Authorization: `Bearer ${api_key}`,
        },
      });

      if (!res.data.meta.status === 200 || !res.data.response.hits.length) {
        throw new Error("No lyrics results found.");
      }

      const track_name = res.data.response.hits[0].result.title;
      const artist_name = res.data.response.hits[0].result.primary_artist.name;
      const artwork_url = res.data.response.hits[0].result.song_art_image_thumbnail_url || null;
      const search_engine = "Genius";

      const lyricsUrl = res.data.response.hits[0].result.url;
      const lyricsResponse = await axios.get(lyricsUrl);

      if (!lyricsResponse.status === 200) {
        throw new Error("No lyrics result found.");
      }

      const lyricsHtml = lyricsResponse.data;
      const $ = cheerio.load(lyricsHtml);
      let lyrics = $('div[class="lyrics"]').text().trim();

      if (!lyrics) {
        lyrics = "";
        $('div[class^="Lyrics__Container"]').each((i, elem) => {
          if ($(elem).text().length !== 0) {
            let snippet = $(elem)
              .html()
              .replace(/<br>/g, "\n")
              .replace(/<(?!\s*br\s*\/?)[^>]+>/gi, "");
            lyrics += $("<textarea/>").html(snippet).text().trim();
          }
        });
      }

      lyrics = lyrics
        .split("\n")
        .map((line) => line.trim())
        .filter((line) => line !== "");
      lyrics = lyrics.join("\n");

      return { artist_name, track_name, search_engine, artwork_url, lyrics };
    } catch (error) {
      return { message: "No lyrics were found.", response: "404 Not Found" };
    }
  }
}

module.exports = Genius;