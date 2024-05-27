const express = require("express");
const Youtube = require("./Youtube");
const router = express.Router();

const yoututbe = new Youtube();

router.use(express.json());

router.get("/youtube/lyrics", async (req, res) => {
  const { title } = req.query;
  if (!title) {
    return res.status(400).send({ error: "Track Title is needed." });
  }

  try {
    const tracks = await yoututbe.getLyrics(title);
    res.send(tracks);
  } catch (error) {
    res.status(500).send({ error: "Internal Server Error." });
  }
});

module.exports = router;
