# Lyrics API

A simple Lyrics Api that can fetch lyrics from various sources like Musixmatch, Genius, and YouTube.

## Getting Started

### Prerequisites

- [Node.js Version 18+](https://nodejs.org/)

### Installation

1. Clone the repository:

   ```bash
   git clone https://github.com/LewdHuTao/lyrics-api
   ```

2. Install dependencies:

   ```bash
   npm install
   ```

### Running the API

Start the server:

```bash
npm run start

// you can access to the api on: http://localhost:3000
```

## Endpoint:
`/musixmatch/lyrics`

### Method:
`GET`

### Parameters:
`title` (string): The search query (song title).

### Example Request:
`GET /musixmatch/lyrics?title={song_title}`

### Example Response:

```
{
"artist_name": "The Weeknd",
"track_name": "After Hours",
"track_id": 194169155,
"search_engine": "Musixmatch",
"artwork_url": "https://s.mxmcdn.net/images-storage/albums2/7/2/8/7/3/3/48337827_350_350.jpg",
"lyrics": "Thought I almost died in my dream again (baby, almost died) ..."
}
```

## Endpoint:
`/musixmatch/lyrics-search`

### Method:
`GET`

### Parameters:
`title` (string): The search query (song title).

`artist` (string): The artist's name for the song.

### Example Request:
`GET /musixmatch/lyrics-search?title={song_title}&artist={artist_name}`

### Example Response:

```
{
"artist_name": "Rihanna",
"track_name": "Diamonds",
"track_id": 84458341,
"search_engine": "Musixmatch",
"artwork_url": "https://s.mxmcdn.net/images-storage/albums2/1/0/4/7/5/4/40457401_350_350.jpg",
"lyrics": "Shine bright like a diamond ..."
}
```

## Endpoint:
`/genius/lyrics`

### Method:
`GET`

### Parameters:
`title` (string): The search query (song title).

`api_key` (string): Genius API Key.

### Example Request:
`GET /genius/lyrics?title={song_title}&api_key={genius_key}`

### Example Response:

```
{
"artist_name": "Ariana Grande",
"track_name": "yes, and?",
"search_engine": "Genius",
"artwork_url": "https://images.genius.com/c96f76385524a89fea9f1fa731113c6a.300x300x1.png",
"lyrics": "[Verse 1], In case you haven't noticed ..."
}
```

## Endpoint:
`/youtube/lyrics`

### Method:
`GET`

### Parameters:
`title` (string): The search query (song title).

### Example Request:
`GET /youtube/lyrics?title={song_title}`

### Example Response:

```
{
"artist_name": "Rick Astley",
"track_name": "Never Gonna Give You Up",
"search_engine": "YouTube",
"artwork_url": "https://lh3.googleusercontent.com/eC9DfRcYSk4FE-fvDCJSu_4xsKdVMKxwmFTYFZwP8OqB7R4TKxAjKoR-Kp1lXeRi2WddPFYulSte4eW-=w120-h120-l90-rj",
"lyrics": "Never gonna give you up, Never gonna let you down ..."
}
```

# Error Codes
`200`: Success

`400`: Bad Request

`401`: Unauthorized

`404`: Not Found

`500`: Internal Server Error

### Deploy to Vercel

[![Deploy with Vercel](https://vercel.com/button)](https://vercel.com/new/clone?repository-url=https%3A%2F%2Fgithub.com%2FLewdHuTao%2Fllyrics-api)

### Public API

Visit `https://lyrics.lewdhutao.my.eu.org/` to access the public API. Some search engine like Genius might not work as the API is host in Vercel and it seems like Vercel IP is now blocked from accessing Genius API.
