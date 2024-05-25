const express = require("express");
const router = express.Router();
const Musixmatch = require("./Musixmatch");
const cookieParser = require("cookie-parser");

const musixmatch = new Musixmatch();

router.use(express.json());
router.use(cookieParser());

router.get("/musixmatch/lyrics", async (req, res) => {
  const { title } = req.query;
  if (!title) {
    return res.status(400).send({ error: "Track Title is needed." });
  }
  try {
    let userToken = req.cookies.user_token;
    if (!userToken) {
      userToken = await musixmatch.getToken();
      res.cookie("user_token", userToken); // Set the user token cookie
    }
    const tracks = await musixmatch.searchTrack(title, userToken);
    res.send(tracks);
  } catch (error) {
    res.status(500).send({ error: "Internal Server Error." });
  }
});

router.get("/musixmatch/lyrics-search", async (req, res) => {
  const { title, artist } = req.query;
  if (!title || !artist) {
    return res
      .status(400)
      .send({ error: "Title and artist parameters are required" });
  }
  try {
    let userToken = req.cookies.user_token;
    if (!userToken) {
      userToken = await musixmatch.getToken();
      res.cookie("user_token", userToken); // Set the user token cookie
    }
    const response = await musixmatch.getLyricsSearch(title, artist, userToken);
    res.send(response);
  } catch (error) {
    res.status(500).send({ error: "Internal Server Error." });
  }
});

module.exports = router;
