const express = require('express');
const Musixmatch = require('./Musixmatch'); 
const router = express.Router(); 

const musixmatch = new Musixmatch();

router.use(express.json());

router.get('/musixmatch/lyrics', async (req, res) => {
  const { query } = req.query;
  if (!query) {
    return res.status(400).send({ error: 'Track Title is needed.' });
  }
  try {
    const tracks = await musixmatch.searchTrack(query);
    res.send(tracks);
  } catch (error) {
    res.status(500).send({ error: "Internal Server Error." });
  }
});

router.get('/musixmatch/lyrics-search', async (req, res) => {
  const { title, artist } = req.query;
  if (!title || !artist) {
    return res.status(400).send({ error: 'Title and artist parameters are required' });
  }
  try {
    const response = await musixmatch.getLyricsSearch(title, artist);
    res.send(response);
  } catch (error) {
    res.status(500).send({ error: "Internal Server Error." });
  }
});

module.exports = router;
