import express, { Response, Request, Router } from 'express';
import log from '../../../utils/logger';
import config from '../../../config';

const router: Router = express.Router();

router.use(express.json());

router.get('/v1/youtube/lyrics', async (req: Request, res: Response) => {
  const title = req.query.title as string | undefined;

  if (!title) {
    return res.status(400).send({
      message: 'Song Title is needed for this request.',
      response: '400 Bad Request',
    });
  }

  try {
    const api = `${config.apiUrlV1}/api/v1/youtube?title=${encodeURIComponent(title)}`;
    const response = await fetch(api);
    const data = await response.json();
    
    res.send(data);
  } catch (error) {
    log.error(`Error: ${error as string}`);
    res.status(500).send({
      message: 'An error has occurred.',
      response: '500 Internal Server Error',
    });
  }
});

export default router;
