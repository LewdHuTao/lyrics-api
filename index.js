const express = require("express");
const path = require("path");
const useragent = require("useragent");
const colors = require("colors");

const musixmatch = require("./search_engine/musixmatch/router");
const genius = require("./search_engine/genius/router");
const youtube = require("./search_engine/youtube/router");

const app = express();
const PORT = process.env.PORT || 3000;

function logDetails(req, res, next) {
  const currentTime = new Date().toISOString();
  const agent = useragent.parse(req.headers["user-agent"]);
  const browserDetails = `${agent.toAgent()} on ${agent.os}`;
  const ipAddress = req.ip;

  console.log(
    colors.green(`[${currentTime}] Request: ${req.method} ${req.originalUrl}`)
  );
  console.log(
    colors.yellow(
      `[${currentTime}] User-Agent: ${browserDetails} | IP: ${ipAddress}`
    )
  );
  next();
}

app.use(logDetails);
app.use(musixmatch);
app.use(genius);
app.use(youtube);

app.get("/", async (req, res) => {
  res.sendFile(path.join(__dirname, "documentation", "index.html"));
});

app.listen(PORT, () => {
  console.log(
    colors.blue.bold(`Lyrics API Server is now running on port: ${PORT}`)
  );
});
