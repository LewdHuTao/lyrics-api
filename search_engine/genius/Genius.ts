import * as cheerio from 'cheerio';
import log from "../../utils/logger";

interface LyricsResponse {
  artist_name: string;
  track_name: string;
  search_engine: string;
  artwork_url: string | null;
  lyrics: string;
}

interface ErrorResponse {
  message: string;
  response: string;
}

class Genius {
  private geniusURL = 'https://api.genius.com/search';

  async getLyrics(title: string, api_key: string | null = null): Promise<LyricsResponse | ErrorResponse> {
    try {
      const res = await fetch(`${this.geniusURL}?q=${encodeURIComponent(title)}`, {
        headers: {
          Authorization: `Bearer ${api_key}`,
        },
      });

      if (!res.ok) {
        throw new Error('Error Code: 404 Not Found.');
      }

      const data = await res.json();
      if (data.meta.status !== 200 || !data.response.hits.length) {
        throw new Error('No lyrics results found.');
      }

      const result = data.response.hits[0].result;
      const track_name = result.title;
      const artist_name = result.primary_artist.name;
      const artwork_url = result.song_art_image_thumbnail_url || null;
      const search_engine = 'Genius';

      const lyricsUrl = result.url;
      const lyricsResponse = await fetch(lyricsUrl);

      if (!lyricsResponse.ok) {
        throw new Error('No lyrics result found.');
      }

      const lyricsHtml = await lyricsResponse.text();
      const $ = cheerio.load(lyricsHtml);
      let lyrics = $('div[class="lyrics"]').text().trim();

      if (!lyrics) {
        lyrics = '';
        $('div[class^="Lyrics__Container"]').each((i, elem) => {
          if (elem) {
            const elementText = $(elem).text();
            if (elementText.length !== 0) {
              const snippet =
                $(elem)
                  .html()
                  ?.replace(/<br>/g, '\n')
                  .replace(/<(?!\s*br\s*\/?)[^>]+>/gi, '') || '';
              lyrics += $('<textarea/>').html(snippet).text().trim();
            }
          }
        });
      }

      lyrics = lyrics
        .split('\n')
        .map((line) => line.trim())
        .filter((line) => line !== '')
        .join('\n');

      return { artist_name, track_name, search_engine, artwork_url, lyrics };
    } catch (error) {
      log.error(`Error: ${error as string}`);
      return { message: 'No lyrics were found.', response: '404 Not Found' };
    }
  }
}

export default Genius;
