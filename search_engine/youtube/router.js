const express = require("express");
const Youtube = require("./Youtube");
const router = express.Router();

const yoututbe = new Youtube();

router.use(express.json());

router.get("/youtube/lyrics", async (req, res) => {
  const { title } = req.query;
  if (!title) {
    return res.status(400).send({
      messgae: "Song Title is needed for this request.",
      response: "400 Bad Request",
    });
  }

  try {
    const tracks = await yoututbe.getLyrics(title);
    res.send(tracks);
  } catch (error) {
    res.status(500).send({
      message: "An error has occured.",
      response: "500 Internal Server Error",
    });
  }
});

module.exports = router;
