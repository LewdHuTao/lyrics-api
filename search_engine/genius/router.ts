// search_engine/genius/router.ts
import express, { Request, Response, Router } from 'express';
import path from 'path';
import fs from 'fs/promises';
import Genius from './Genius';
import log from '../../utils/logger';

const genius = new Genius();
const router: Router = express.Router();

router.use(express.json());

// Configuration constants
const MAX_RETRIES = 3;
const RETRY_DELAY_MS = 1000; // 1 second delay between retries

// Helper function to sanitize filename
const sanitizeFilename = (filename: string): string => {
    return filename.replace(/[<>:"/\\|?*]/g, '_').replace(/\s+/g, '_');
};

// Helper function to check if file is less than 1 month old
const isFileRecent = async (filePath: string): Promise<boolean> => {
    try {
        const stats = await fs.stat(filePath);
        const oneMonthAgo = new Date();
        oneMonthAgo.setMonth(oneMonthAgo.getMonth() - 1);
        return stats.birthtime > oneMonthAgo;
    } catch (error: unknown) {
        // File doesn't exist or can't be accessed
        return false;
    }
};

// Helper function to ensure directory exists
const ensureDirectoryExists = async (dirPath: string): Promise<void> => {
    try {
        await fs.access(dirPath);
    } catch (error: unknown) {
        try {
            await fs.mkdir(dirPath, { recursive: true });

        } catch (mkdirError: unknown) {
            if (mkdirError instanceof Error) {
                log.error(`Failed to create directory ${dirPath}: ${mkdirError.message}`);
                log.error(`Error stack: ${mkdirError.stack}`);
            } else {
                log.error(`Failed to create directory ${dirPath}: ${JSON.stringify(mkdirError)}`);
            }
            throw mkdirError;
        }
    }
};

// Helper function to read cached lyrics
const readCachedLyrics = async (filePath: string): Promise<any> => {
    try {
        const data = await fs.readFile(filePath, 'utf-8');
        return JSON.parse(data);
    } catch (error: unknown) {
        throw new Error('Failed to read cached lyrics');
    }
};

// Helper function to save lyrics to cache
const saveLyricsToCache = async (filePath: string, lyrics: any): Promise<void> => {
    try {
        const dirPath = path.dirname(filePath);

        await ensureDirectoryExists(dirPath);

        // Check if we can write to the directory
        await fs.access(dirPath, fs.constants.W_OK);

        // Write the file
        await fs.writeFile(filePath, JSON.stringify(lyrics, null, 2), 'utf-8');

        // Verify file was actually created and has content
        const stats = await fs.stat(filePath);

        // Try to read it back to verify
        const testRead = await fs.readFile(filePath, 'utf-8');
    } catch (error: unknown) {
        if (error instanceof Error) {
            log.error(`Failed to save lyrics to cache: ${error.message}`);
            log.error(`Error stack: ${error.stack}`);
        } else {
            log.error(`Failed to save lyrics to cache: ${JSON.stringify(error)}`);
        }
        throw error;
    }
};

// Helper function to add delay between retries
const delay = (ms: number): Promise<void> => {
    return new Promise(resolve => setTimeout(resolve, ms));
};

// Helper function to execute operation with retries
const executeWithRetries = async <T>(
    operation: () => Promise<T>,
    operationName: string,
    maxRetries: number = MAX_RETRIES,
    delayMs: number = RETRY_DELAY_MS
): Promise<T> => {
    let lastError: Error | null = null;

    for (let attempt = 1; attempt <= maxRetries; attempt++) {
        try {
            log.info(`${operationName} - Attempt ${attempt}/${maxRetries}`);
            const result = await operation();

            if (attempt > 1) {
                log.info(`${operationName} succeeded on attempt ${attempt}`);
            }

            return result;
        } catch (error: unknown) {
            lastError = error as Error;

            if (attempt < maxRetries) {
                log.warn(`${operationName} failed on attempt ${attempt}/${maxRetries}: ${lastError.message}. Retrying in ${delayMs}ms...`);
                await delay(delayMs);
            } else {
                log.error(`${operationName} failed on final attempt ${attempt}/${maxRetries}: ${lastError.message}`);
            }
        }
    }

    throw lastError || new Error(`${operationName} failed after ${maxRetries} attempts`);
};

router.get('/genius', async (req: Request, res: Response) => {
    res.sendFile(path.join(__dirname, 'index.html'));
});

router.get('/genius/lyrics', async (req: Request, res: Response) => {
    const title = req.query.title as string | undefined;
    const artist = req.query.artist as string | undefined;
    const api_key = process.env.GENIUS_BEARER;

    if (!title) {
        return res.status(400).send({
            message: 'Song Title is needed for this request.',
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
        // Create cache file path
        const sanitizedTitle = sanitizeFilename(title);
        const sanitizedArtist = artist ? sanitizeFilename(artist) : 'unknown'; // Add this line
        const fileName = `${sanitizedTitle}_${sanitizedArtist}.txt`;
        const cacheDir = path.join(process.cwd(), 'genius_store');
        const filePath = path.join(cacheDir, fileName);

        // Check directory permissions
        try {
            await fs.access(process.cwd(), fs.constants.W_OK);
        } catch (permError: unknown) {
            if (permError instanceof Error) {
                log.error(`No write permission for current directory: ${permError.message}`);
            } else {
                log.error(`No write permission for current directory: ${JSON.stringify(permError)}`);
            }
        }

        // Check if cached file exists and is recent
        if (await isFileRecent(filePath)) {
            try {
                const cachedLyrics = await readCachedLyrics(filePath);
                log.info(`Using cached lyrics from '${filePath}'.`);
                return res.send(cachedLyrics);
            } catch (readError: unknown) {
                if (readError instanceof Error) {
                    log.warn(`Failed to read cached lyrics, fetching new: ${readError.message}`);
                } else {
                    log.warn(`Failed to read cached lyrics, fetching new: ${JSON.stringify(readError)}`);
                }
            }
        }

        // Fetch new lyrics with retry logic
        log.info(`No cache exist.\n Fetching data for title='${title}', artist='${artist}' and save to cache.`);

        const tracks = await executeWithRetries(async () => {
            return await genius.getLyrics(title, api_key, artist); // Add artist parameter
        }, `Genius lyrics search for title: ${title}${artist ? ` by ${artist}` : ''}`);

        // Save to cache - wait for completion to see if there are errors
        try {
            await saveLyricsToCache(filePath, tracks);
        } catch (cacheSaveError: unknown) {
            if (cacheSaveError instanceof Error) {
                log.error(`Failed to cache lyrics: ${cacheSaveError.message}`);
            } else {
                log.error(`Failed to cache lyrics: ${JSON.stringify(cacheSaveError)}`);
            }
        }

        res.send(tracks);
    } catch (error: unknown) {
        if (error instanceof Error) {
            log.error(`Error in /genius/lyrics route after retries: ${error.message}`);
            log.error(`Stack: ${error.stack}`);
            res.status(500).send({
                message: 'An error has occurred after multiple attempts.',
                response: `500 Internal Server Error. Error: ${error.message}`,
            });
        } else {
            log.error(`Unknown error in /genius/lyrics route after retries: ${JSON.stringify(error)}`);
            res.status(500).send({
                message: 'An error has occurred after multiple attempts.',
                response: `500 Internal Server Error. Error: ${JSON.stringify(error)}`,
            });
        }
    }
});

export default router;