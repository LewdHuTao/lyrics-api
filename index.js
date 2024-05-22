const express = require("express");
const app = express();

const musixmatch = require("./search_engine/musixmatch/router");
const genius = require("./search_engine/genius/router");
app.use(musixmatch);
app.use(genius);

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server is running on port ${PORT}`);
});