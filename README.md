# Lyrics API — v2

**Lyrics API v2** is a Java-based API that fetches and serves song lyrics from multiple sources.

## ✨ Features
- **Multiple Sources**:
    - YouTube
    - Musixmatch
- **Fast & Lightweight**: Fully implemented in Java for speed and stability.
- **Easy Integration**: Simple REST endpoints for quick adoption into any app or service.

## 📦 Installation & Setup
1. **Download the Latest Release**
    - Go to the [Releases](../../releases) page.
    - Download the latest `LyricsApi.jar` file.

2. **Run the API**
   ```bash
   java -jar LyricsApi.jar
   ```

> **Note:** The API currently runs on port `8888` and cannot be changed.

## 📡 Endpoints

### **Get Lyrics**
```http
GET http://localhost:8888/api/v2/lyrics?platform={platform}&title={title}&artist={artist}
```

**Parameters**:

| Name       | Type   | Required | Description |
|------------|--------|----------|-------------|
| `platform` | String | ✅ Yes   | Platform to fetch from (`youtube` or `musixmatch`) |
| `title`    | String | ✅ Yes   | Title of the song |
| `artist`   | String | ❌ No    | Artist name |

**Example**:
```http
GET http://localhost:8888/api/v2/lyrics?platform=youtube&title=back to friends&artist=sombr
```

**Sample Response**:
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
