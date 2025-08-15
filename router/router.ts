import express, { Response, Request, Router } from 'express';
import cookieParser from 'cookie-parser';
import log from '../utils/logger';
import config from '../config';

const router: Router = express.Router();

router.use(express.json());
router.use(cookieParser());

type UrlBuilder = (platform: string, title: string, artist?: string, api_key?: string) => string;

interface ApiVersionConfig {
    requiredParams: (platform: string) => string[];
    buildUrl: UrlBuilder;
}

const API_CONFIG: Record<string, ApiVersionConfig> = {
    v1: {
        requiredParams: (platform) => {
            if (platform === 'genius') return ['title', 'api_key'];
            return ['title'];
        },
        buildUrl: (platform, title, artist, api_key) => {
            const params = new URLSearchParams({ title });
            if (artist) params.append('artist', artist);
            if (api_key) params.append('api_key', api_key);
            return `${config.apiUrlV1}/api/v1/${encodeURIComponent(platform)}?${params.toString()}`;
        }
    },
    v2: {
        requiredParams: () => ['title'],
        buildUrl: (platform, title, artist) => {
            const params = new URLSearchParams({ platform, title });
            if (artist) params.append('artist', artist);
            return `${config.apiUrlV2}/api/v2/lyrics?${params.toString()}`;
        }
    }
};

router.get('/:apiVersion/:platform/lyrics', async (req: Request, res: Response) => {
    const { apiVersion, platform } = req.params;
    const title = req.query.title as string | undefined;
    const artist = req.query.artist as string | undefined;
    const api_key = req.query.api_key as string | undefined;

    const configEntry = API_CONFIG[apiVersion];
    if (!configEntry) {
        return res.status(400).json({ message: `Unsupported API version: ${apiVersion}`, response: '400 Bad Request' });
    }

    for (const param of configEntry.requiredParams(platform)) {
        if (!req.query[param]) {
            return res.status(400).json({ message: `Missing required parameter: ${param}`, response: '400 Bad Request' });
        }
    }

    try {
        const apiUrl = configEntry.buildUrl(platform, title!, artist, api_key);
        const response = await fetch(apiUrl);
        const data = await response.json();
        res.send(data);
    } catch (error) {
        log.error(`Error: ${error}`);
        res.status(500).json({ message: 'An error has occurred', response: '500 Internal Server Error' });
    }
});

export default router;
