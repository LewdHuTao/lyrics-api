import express, { Request, Response, NextFunction } from 'express';
import path from 'path';
import useragent from 'useragent';
import router from './router/router';
import log from "./utils/logger";
import ratelimit from "./utils/ratelimit";
import config from "./config";

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

if (config.ratelimit) {
  app.use('/v2', ratelimit);
}

app.use(logDetails);
app.use(router);


app.get('/', (req, res) => {
  res.setHeader('Cache-Control', 'private, no-cache, no-store, must-revalidate');
  res.setHeader('Expires', '-1');
  res.setHeader('Pragma', 'no-cache');
  res.sendFile(path.join(__dirname, 'documentation', 'index.html'));
});


export const startServer = () => {
  app.listen(PORT, () => {
    log.info(`Lyrics API Server is now running on port: ${PORT}`);
  });
};
