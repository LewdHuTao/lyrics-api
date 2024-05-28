const express = require("express");
const app = express();
const path = require("path");

const musixmatch = require("./search_engine/musixmatch/router");
const genius = require("./search_engine/genius/router");
const youtube = require("./search_engine/youtube/router");

app.use(musixmatch);
app.use(genius);
app.use(youtube);

const PORT = process.env.PORT || 3000;

app.get("/", async (req, res) => {
  res.sendFile(path.join(__dirname, "documentation", "index.html"));
});

app.listen(PORT, () => {
  console.log(`Server is running on port ${PORT}`);
});
