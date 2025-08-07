# Lyrics API

A simple lyrics API that fetches lyrics from sources like Musixmatch and YouTube Music.

---

## ❗ V1 API Deprecated

> **The `/v1` endpoints are deprecated and no longer maintained.**  
> While they may still function, they will not receive bug fixes, updates, or support.  
> Please migrate to `/v2` endpoints for improved accuracy, speed, and stability.

---

## Getting Started

### Requirements

- Node.js 18 or higher

### Installation

```bash
git clone https://github.com/lewdhutao/lyrics-api
cd lyrics-api
npm install
```

### Running

```bash
npm start
```

Once running, open: `http://localhost:3000`

---

## API Endpoints

### `/v2/musixmatch/lyrics`

Search for lyrics using Musixmatch.

**Method:** `GET`

**Query Parameters:**
- `title` (required): Song title
- `artist` (optional): Artist name

**Examples:**

```
GET /v2/musixmatch/lyrics?title=back%20to%20friends
GET /v2/musixmatch/lyrics?title=back%20to%20friends&artist=sombr
```

---

### `/v2/youtube/lyrics`

Search for lyrics using YouTube Music.

**Method:** `GET`

**Query Parameters:**
- `title` (required): Song title
- `artist` (optional): Artist name

**Examples:**

```
GET /v2/youtube/lyrics?title=back%20to%20friends
GET /v2/youtube/lyrics?title=back%20to%20friends&artist=sombr
```

---

## Example JSON Response

```json
{
  "data": {
    "artistName": "Sombr",
    "trackName": "Back to Friends",
    "trackId": "dbEY-JVHJWg",
    "searchEngine": "YouTube",
    "artworkUrl": "https://example.com/art.jpg",
    "lyrics": "Touch my body tender\n'Cause the feeling makes me weak..."
  },
  "metadata": {
    "apiVersion": "2.0"
  }
}
```

---

## Deprecated v1 Endpoints

These are no longer maintained and may stop working at any time:

- `/v1/musixmatch/lyrics`
- `/v1/youtube/lyrics`
- `/v1/genius/lyrics` 


---

## Status Codes

| Code | Description            |
|------|------------------------|
| 200  | OK                     |
| 400  | Bad Request            |
| 401  | Unauthorized           |
| 404  | Not Found              |
| 500  | Internal Server Error  |

---

## Supported Sources

- **Musixmatch**: Best for Spotify / Apple Music tracks
- **YouTube Music**: For video-based lyrics

---

## Public API Demo

https://lyrics.lewdhutao.my.eu.org

---

## Deploy to Vercel

[![Deploy with Vercel](https://vercel.com/button)](https://vercel.com/new/clone?repository-url=https://github.com/lewdhutao/lyrics-api)

---

## License
ISC © 2025
