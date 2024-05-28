const express = require("express");
const Genius = require("./Genius");
const router = express.Router();

const genius = new Genius();

router.use(express.json());

router.get("/genius/lyrics", async (req, res) => {
  const { title, api_key } = req.query;
  if (!title) {
    return res
      .status(400)
      .send({
        messgae: "Song Title is needed for this request.",
        response: "400 Bad Request",
      });
  }
  if (!api_key) {
    return res
      .status(401)
      .send({
        message: "Genius API Key is needed for this request.",
        response: "401 Unauthorized",
      });
  }

  try {
    const tracks = await genius.getLyrics(title, api_key);
    res.send(tracks);
  } catch (error) {
    res
      .status(500)
      .send({
        message: "An error has occured.",
        response: "500 Internal Server Error",
      });
  }
});

module.exports = router;
