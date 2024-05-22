const express = require("express");
const Genius = require("./Genius");
const router = express.Router();

const genius = new Genius();

router.use(express.json());

router.get("/genius/lyrics", async (req, res) => {
  const { title, api_key } = req.query;
  if (!title) {
    return res.status(400).send({ error: "Track Title is needed." });
  }
  if (!api_key) {
    return res.status(400).send({ error: "Api Key is needed." });
  }

  try {
    const tracks = await genius.getLyrics(title, api_key);
    res.send(tracks);
  } catch (error) {
    res.status(500).send({ error: "Internal Server Error." });
  }
});

module.exports = router;
