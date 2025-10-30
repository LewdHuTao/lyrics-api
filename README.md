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
   **Run the API with custom port**
    ```bash
   java -jar LyricsApi.jar --server.port={port}
   ```

**Parameters**:

| Name        | Type   | Required | Description                                        |
|-------------|--------|----------|----------------------------------------------------|
| `platform`  | String | ✅ Yes   | Platform to fetch from (`youtube` or `musixmatch`) |
| `title`     | String | ✅ Yes   | Title of the song                                  |
| `artist`    | String | ❌ No    | Artist name                                        |
| `translate` | String | ❌ No    | Language Code (Only for Musixmatch)                |

### **Get Lyrics**
```http
GET http://localhost:8888/api/v2/lyrics?platform={platform}&title={title}&artist={artist}
```

### **Get Translated Lyrics**

**⚠️ Note: Translation features are currently available only on Musixmatch.**
**Support for other platforms may be added in future updates.**

```http
GET http://localhost:8888/api/v2/lyrics?platform=musixmatch&title={title}&artist={artist}&translate={lang_code}
```
**Available language codes**

<details>
<summary>Click to expand full list</summary>

| Language Name    | ISO-1 | ISO-3 |
|------------------|----|-----|
| Afar             | aa | aar |
| Abkhazian        | ab | abk |
| Afrikaans        | af | afr |
| Akan             | ak | aka |
| Albanian         | sq | sqi |
| Amharic          | am | amh |
| Arabic           | ar | ara |
| Aragonese        | an | arg |
| Armenian         | hy | hye |
| Assamese         | as | asm |
| Asturian         | a3 | ast |
| Avaric           | av | ava |
| Avestan          | ae | ave |
| Aymara           | ay | aym |
| Azerbaijani      | az | aze |
| Bashkir          | ba | bak |
| Bambara          | bm | bam |
| Basque           | eu | eus |
| Belarusian       | be | bel |
| Bengali          | bn | ben |
| Bihari Languages | bh | bh0 |
| Bislama          | bi | bis |
| Bosnian          | bs | bos |
| Breton           | br | bre |
| Bulgarian        | bg | bul |
| Burmese          | my | mya |
| Catalan          | ca | cat |
| Cebuano          | c2 | ceb |
| Chamorro         | ch | cha |
| Chechen          | ce | che |
| Chinese (Simplified) | zh | zho |
| Church Slavic    | cu | chu |
| Chuvash          | cv | chv |
| Cornish          | kw | cor |
| Corsican         | co | cos |
| Czech            | cs | ces |
| Danish           | da | dan |
| Divehi           | dv | div |
| Dutch            | nl | nld |
| Dzongkha         | dz | dzo |
| English          | en | eng |
| Esperanto        | eo | epo |
| Estonian         | et | est |
| Fijian           | fj | fij |
| Filipino         | f1 | fil |
| Finnish          | fi | fin |
| French           | fr | fra |
| German           | de | deu |
| Greek            | el | ell |
| Gujarati         | gu | guj |
| Hebrew           | he | heb |
| Hindi            | hi | hin |
| Hungarian        | hu | hun |
| Icelandic        | is | isl |
| Indonesian       | id | ind |
| Italian          | it | ita |
| Japanese         | ja | jpn |
| Japanese (Romanized) | rj | rja |
| Korean**         | ko | kor |
| Korean (Romanized) | rk | rkr |
| Khmer, Central   | km | khm |
| Lao              | lo | lao |
| Latin            | la | lat |
| Lithuanian       | lt | lit |
| Macedonian       | mk | mkd |
| Malay            | ms | msa |
| Malayalam        | ml | mal |
| Malayalam (Romanized) | m8 | ma8 |
| Marathi          | mr | mar |
| Marathi (Romanized) | m9 | ma9 |
| Mongolian        | mn | mon |
| Nepali           | ne | nep |
| Nepali (Romanized) | n5 | ne5 |
| Norwegian        | no | nor |
| Oriya            | or | ori |
| Oriya (Romanized) | o1 | or1 |
| Panjabi          | pa | pan |
| Panjabi (Romanized) | p5 | pa5 |
| Persian          | fa | fas |
| Polish           | pl | pol |
| Portuguese       | pt | por |
| Romanian         | ro | ron |
| Russian       | ru | rus |
| Russian (Romanized) | r2 | ru2 |
| Sanskrit         | sa | san |
| Sanskrit (Romanized) | s4 | sa4 |
| Serbian          | sr | srp |
| Sinhala          | si | sin |
| Slovak           | sk | slk |
| Slovenian        | sl | slv |
| Spanish          | es | spa |
| Swahili          | sw | swa |
| Swedish          | sv | swe |
| Tamil            | ta | tam |
| Tamil (Romanized) | t2 | ta2 |
| Telugu           | te | tel |
| Telugu (Romanized) | t3 | te3 |
| Thai             | th | tha |
| Thai (Romanized) | t4 | tr1 |
| Turkish          | tr | tur |
| Ukrainian        | uk | ukr |
| Urdu             | ur | urd |
| Urdu (Romanized) | u1 | ur1 |
| Uzbek            | uz | uzb |
| Vietnamese       | vi | vie |
| Welsh            | cy | cym |
| Xhosa            | xh | xho |
| Yoruba           | yo | yor |
| Zulu             | zu | zul |

</details>

## **Sample Response**:
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
