import express, { Request, Response, NextFunction } from 'express';
import path from 'path';
import useragent from 'useragent';

// v1 API
import musixmatchV1 from './platform/musixmatch/v1/router';
import geniusV1 from './platform/genius/v1/router';
import youtubeV1 from './platform/youtube/v1/router';

// v2 API
import musixmatchV2 from './platform/musixmatch/v2/router';
import youtubeV2 from './platform/youtube/v2/router';

import log from "./utils/logger";

const app = express();
const PORT = process.env.PORT || 3000;

function logDetails(req: Request, res: Response, next: NextFunction) {
  const currentTime = new Date().toISOString();
  const agent = useragent.parse(req.headers['user-agent']);
  const browserDetails = `${agent.toAgent()} on ${agent.os}`;
  const ipAddress = req.ip;

  log.success(`[${currentTime}] Request: ${req.method} ${req.originalUrl}`);
  log.warn(`[${currentTime}] User-Agent: ${browserDetails} | IP: ${ipAddress}`);
  next();
}

app.use(logDetails);
app.use(musixmatchV1);
app.use(geniusV1);
app.use(youtubeV1);
app.use(musixmatchV2);
app.use(youtubeV2);

app.get('/', async (req, res) => {
  res.sendFile(path.join(__dirname, 'documentation', 'index.html'));
});

export const startServer = () => {
  app.listen(PORT, () => {
    log.info(`Lyrics API Server is now running on port: ${PORT}`);
  });
};
