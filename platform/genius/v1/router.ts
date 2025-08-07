import express, { Request, Response, Router } from 'express';
import log from '../../../utils/logger';
import config from '../../../config';

const router: Router = express.Router();

router.use(express.json());

router.get('/v1/genius/lyrics', async (req: Request, res: Response) => {
  const title = req.query.title as string | undefined;
  const api_key = req.query.api_key as string | undefined;

  if (!title) {
    return res.status(400).send({
      messgae: 'Song Title is needed for this request.',
      response: '400 Bad Request',
    });
  }

  if (!api_key) {
    return res.status(401).send({
      message: 'Genius API Key is needed for this request.',
      response: '401 Unauthorized',
    });
  }

  try {
    const api = `${config.apiUrlV1}/api/v1/lyrics?title=${encodeURIComponent(title)}&api_key=${encodeURIComponent(api_key)}`;
    const response = await fetch(api);
    const data = await response.json();
    
    res.send(data);
  } catch (error) {
    log.error(`Error: ${error as string}`);
    res.status(500).send({
      message: 'An error has occured.',
      response: '500 Internal Server Error',
    });
  }
});

export default router;
