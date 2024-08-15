import express, { Request, Response, NextFunction } from 'express';
import path from 'path';
import useragent from 'useragent';
import colors from 'colors';

import musixmatch from './search_engine/musixmatch/router';
import genius from './search_engine/genius/router';
import youtube from './search_engine/youtube/router';

const app = express();
const PORT = process.env.PORT || 3000;

function logDetails(req: Request, res: Response, next: NextFunction) {
  const currentTime = new Date().toISOString();
  const agent = useragent.parse(req.headers['user-agent']);
  const browserDetails = `${agent.toAgent()} on ${agent.os}`;
  const ipAddress = req.ip;

  console.log(colors.green(`[${currentTime}] Request: ${req.method} ${req.originalUrl}`));
  console.log(colors.yellow(`[${currentTime}] User-Agent: ${browserDetails} | IP: ${ipAddress}`));
  next();
}

app.use(logDetails);
app.use(musixmatch);
app.use(genius);
app.use(youtube);

app.get('/', async (req, res) => {
  res.sendFile(path.join(__dirname, 'documentation', 'index.html'));
});

app.listen(PORT, () => {
  console.log(colors.blue.bold(`Lyrics API Server is now running on port: ${PORT}`));
});
