import express, { Response, Request, Router } from 'express';
import cookieParser from 'cookie-parser';
import log from '../../../utils/logger';
import config from '../../../config';

const router: Router = express.Router();

router.use(express.json());
router.use(cookieParser());

router.get('/v2/musixmatch/lyrics', async (req: Request, res: Response) => {
  const title = req.query.title as string | undefined;
  const artist = req.query.artist as string | undefined;

  if (!title) {
    return res.status(400).send({
      message: 'Song Title is needed for this request.',
      response: '400 Bad Request',
    });
  }

  try {
    let api;

    if (artist) {
      api = `${config.apiUrlV2}/api/v2/lyrics?platform=musixmatch&title=${encodeURIComponent(title)}&artist=${encodeURIComponent(artist)}`;
    } else {
      api = `${config.apiUrlV2}/api/v2/lyrics?platform=musixmatch&title=${encodeURIComponent(title)}`;
    }

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
