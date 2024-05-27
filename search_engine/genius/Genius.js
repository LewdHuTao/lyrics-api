const cheerio = require("cheerio");

class Genius {
  constructor() {
    this.geniusURL = "https://api.genius.com/search";
  }

  async getLyrics(title, api_key = null) {
    const res = await fetch(
      `${this.geniusURL}?q=${encodeURIComponent(title)}`,
      {
        headers: {
          Authorization: `Bearer ${api_key}`,
        },
      }
    );

    if (!res.ok) {
      throw new Error("Error Code: 404 Not Found.");
    }

    const data = await res.json();
    if (data.meta.status !== 200 || !data.response.hits.length) {
      throw new Error("No lyrics results found.");
    }

    const track_name = data.response.hits[0].result.title;
    const artist_name = data.response.hits[0].result.primary_artist.name;
    const artwork_url =
      data.response.hits[0].result.song_art_image_thumbnail_url || null;
    const search_engine = "Genius";

    const lyricsUrl = data.response.hits[0].result.url;
    const lyricsResponse = await fetch(lyricsUrl);

    if (!lyricsResponse.ok) {
      throw new Error("No lyrics result found.");
    }

    const lyricsHtml = await lyricsResponse.text();
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

    return { artist_name, track_name, search_engine, artwork_url, lyrics };
  }
}

module.exports = Genius;
