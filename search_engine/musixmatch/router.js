const express = require("express");
const router = express.Router();
const Musixmatch = require("./Musixmatch");
const cookieParser = require("cookie-parser");
const path = require("path");

const musixmatch = new Musixmatch();

router.use(express.json());
router.use(cookieParser());

router.get("/musixmatch", async (req, res) => {
  res.sendFile(path.join(__dirname, "index.html"))
}) 

router.get("/musixmatch/lyrics", async (req, res) => {
  const { title } = req.query;
  if (!title) {
    return res.status(400).send({
      messgae: "Song Title is needed for this request.",
      response: "400 Bad Request",
    });
  }
  try {
    let userToken = req.cookies.user_token;
    if (!userToken) {
      userToken = await musixmatch.getToken();
      res.cookie("user_token", userToken);
    }
    const tracks = await musixmatch.searchTrack(title, userToken);
    res.send(tracks);
  } catch (error) {
    res.status(500).send({
      message: "An error has occured.",
      response: "500 Internal Server Error",
    });
  }
});

router.get("/musixmatch/lyrics-search", async (req, res) => {
  const { title, artist } = req.query;
  if (!title || !artist) {
    return res.status(400).send({
      messgae: "Song Title is needed for this request.",
      response: "400 Bad Request",
    });
  }
  try {
    let userToken = req.cookies.user_token;
    if (!userToken) {
      userToken = await musixmatch.getToken();
      res.cookie("user_token", userToken);
    }
    const response = await musixmatch.getLyricsSearch(title, artist, userToken);
    res.send(response);
  } catch (error) {
    res.status(500).send({
      message: "An error has occured.",
      response: "500 Internal Server Error",
    });
  }
});

module.exports = router;
