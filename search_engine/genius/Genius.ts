// search_engine/genius/Genius.ts
import * as cheerio from 'cheerio';
import log from "../../utils/logger";
import fs from 'fs/promises';
import path from 'path';

interface LyricsResponse {
    artist_name: string;
    track_name: string;
    search_engine: string;
    artwork_url: string | null;
    lyrics: string;
}

interface ErrorResponse {
    message: string;
    response: string;
}

interface ProxyScrapeProxy {
    alive: boolean;
    ip: string;
    port: number;
    protocol: string;
    proxy: string;
    anonymity: string;
    uptime: number;
    average_timeout: number;
}

interface ProxyScrapeResponse {
    shown_records: number;
    total_records: number;
    proxies: ProxyScrapeProxy[];
}

interface ProxyConfig {
    host: string;
    port: number;
    protocol: string;
    uptime?: number;
    timeout?: number;
}

interface ProxyCacheData {
    proxies: ProxyConfig[];
    timestamp: number;
}

class Genius {
    private geniusURL = 'https://api.genius.com/search';
    private proxyScrapeUrl = 'https://api.proxyscrape.com/v4/free-proxy-list/get?request=display_proxies&proxy_format=protocolipport&format=json';

    private proxies: ProxyConfig[] = [];
    private currentProxyIndex = 0;
    private maxRetries = 3;
    private proxyRefreshInterval = 2 * 60 * 60 * 1000; // 2 hours
    private lastProxyFetch = 0;
    private minUptime = 80; // Minimum uptime percentage for proxy selection
    private maxTimeout = 2000; // Maximum acceptable timeout in ms

    // Cache configuration - shared with Musixmatch
    private proxyCacheDir = path.join(process.cwd(), 'proxy_cache');
    private proxyCacheFile = path.join(this.proxyCacheDir, 'proxies.json');

    constructor() {
        // Initialize with cached or fresh proxies
        this.initializeProxies();
    }

    private async initializeProxies(): Promise<void> {
        try {
            // Try to load from cache first
            const cachedProxies = await this.loadProxiesFromCache();
            if (cachedProxies && cachedProxies.length > 0) {
                this.proxies = cachedProxies;
                // log.info(`Loaded ${cachedProxies.length} proxies from cache`);
            } else {
                // If no cache or cache is empty, fetch fresh proxies
                await this.refreshProxies();
            }
        } catch (error) {
            log.error(`Failed to initialize proxies: ${error}`);
            // Continue without proxies - will use direct connection
        }
    }

    private async ensureDirectoryExists(dirPath: string): Promise<void> {
        try {
            await fs.access(dirPath);
        } catch (error: unknown) {
            try {
                await fs.mkdir(dirPath, { recursive: true });
            } catch (mkdirError: unknown) {
                if (mkdirError instanceof Error) {
                    log.error(`Failed to create directory ${dirPath}: ${mkdirError.message}`);
                } else {
                    log.error(`Failed to create directory ${dirPath}: ${JSON.stringify(mkdirError)}`);
                }
                throw mkdirError;
            }
        }
    }

    private async isProxyCacheRecent(): Promise<boolean> {
        try {
            const stats = await fs.stat(this.proxyCacheFile);
            const now = Date.now();
            const cacheAge = now - stats.mtime.getTime();
            return cacheAge < this.proxyRefreshInterval;
        } catch (error: unknown) {
            // File doesn't exist or can't be accessed
            return false;
        }
    }

    private async loadProxiesFromCache(): Promise<ProxyConfig[] | null> {
        try {
            // Check if cache file exists and is recent
            if (!(await this.isProxyCacheRecent())) {
                // log.info('Proxy cache is stale or doesn\'t exist');
                return null;
            }

            const data = await fs.readFile(this.proxyCacheFile, 'utf-8');
            const cacheData: ProxyCacheData = JSON.parse(data);

            // Verify cache structure
            if (!cacheData.proxies || !Array.isArray(cacheData.proxies)) {
                log.warn('Invalid proxy cache structure');
                return null;
            }

            this.lastProxyFetch = cacheData.timestamp;
            // log.info(`Loaded ${cacheData.proxies.length} proxies from cache (cached at ${new Date(cacheData.timestamp).toISOString()})`);

            return cacheData.proxies;
        } catch (error: unknown) {
            if (error instanceof Error) {
                log.warn(`Failed to load proxies from cache: ${error.message}`);
            } else {
                log.warn(`Failed to load proxies from cache: ${JSON.stringify(error)}`);
            }
            return null;
        }
    }

    private async saveProxiesToCache(proxies: ProxyConfig[]): Promise<void> {
        try {
            await this.ensureDirectoryExists(this.proxyCacheDir);

            const cacheData: ProxyCacheData = {
                proxies,
                timestamp: Date.now()
            };

            await fs.writeFile(this.proxyCacheFile, JSON.stringify(cacheData, null, 2), 'utf-8');

            // Verify file was created successfully
            const stats = await fs.stat(this.proxyCacheFile);
            log.info(`Saved ${proxies.length} proxies to cache (${stats.size} bytes)`);

        } catch (error: unknown) {
            if (error instanceof Error) {
                log.error(`Failed to save proxies to cache: ${error.message}`);
            } else {
                log.error(`Failed to save proxies to cache: ${JSON.stringify(error)}`);
            }
            // Don't throw - caching failure shouldn't break the main functionality
        }
    }

    private async fetchProxiesFromAPI(): Promise<ProxyConfig[]> {
        try {
            // log.info('Fetching fresh proxies from ProxyScrape API...');

            const response = await fetch(this.proxyScrapeUrl, {
                method: 'GET',
                headers: {
                    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'
                }
            });

            if (!response.ok) {
                throw new Error(`Failed to fetch proxies: ${response.status}`);
            }

            const data: ProxyScrapeResponse = await response.json();

            // Filter and sort proxies by quality
            const qualityProxies = data.proxies
                .filter(proxy =>
                    proxy.alive &&
                    proxy.uptime >= this.minUptime &&
                    proxy.average_timeout <= this.maxTimeout &&
                    (proxy.protocol === 'http' || proxy.protocol === 'https')
                )
                .sort((a, b) => {
                    // Sort by uptime (desc) then by timeout (asc)
                    if (a.uptime !== b.uptime) return b.uptime - a.uptime;
                    return a.average_timeout - b.average_timeout;
                })
                // .slice(0, 20) // Take top 20 proxies
                .map(proxy => ({
                    host: proxy.ip,
                    port: proxy.port,
                    protocol: proxy.protocol,
                    uptime: proxy.uptime,
                    timeout: proxy.average_timeout
                }));

            // log.info(`Fetched ${qualityProxies.length} quality proxies from ProxyScrape API`);
            return qualityProxies;

        } catch (error) {
            log.error(`Failed to fetch proxies from API: ${error}`);
            return [];
        }
    }

    private async refreshProxies(): Promise<void> {
        const now = Date.now();

        // Check if we have recent cached proxies
        if (await this.isProxyCacheRecent() && this.proxies.length > 0) {
            // log.info('Using recent cached proxies, skipping refresh');
            return;
        }

        // Check if we recently fetched from API (in-memory check)
        if (now - this.lastProxyFetch < this.proxyRefreshInterval && this.proxies.length > 0) {
            // log.info('Recently fetched proxies, skipping refresh');
            return;
        }

        // log.info('Refreshing proxy list...');
        const newProxies = await this.fetchProxiesFromAPI();

        if (newProxies.length > 0) {
            this.proxies = newProxies;
            this.currentProxyIndex = 0;
            this.lastProxyFetch = now;

            // Save to cache
            await this.saveProxiesToCache(newProxies);

            log.info(`Updated proxy list with ${newProxies.length} proxies`);
        } else {
            log.warn('No quality proxies found, keeping existing list');
        }
    }

    private getNextProxy(): ProxyConfig | null {
        if (this.proxies.length === 0) return null;

        const proxy = this.proxies[this.currentProxyIndex];
        this.currentProxyIndex = (this.currentProxyIndex + 1) % this.proxies.length;
        return proxy;
    }

    private async get(url: string, headers: Record<string, string> = {}): Promise<string> {
        // Refresh proxies if needed
        await this.refreshProxies();

        let lastError: Error | null = null;

        // If no proxies available, use direct connection
        if (this.proxies.length === 0) {
            log.warn('No proxies available, using direct connection');
            return this.makeDirectRequest(url, headers);
        }

        // Try with different proxies
        for (let attempt = 0; attempt < Math.min(this.maxRetries, this.proxies.length); attempt++) {
            const proxy = this.getNextProxy();
            if (!proxy) break;

            try {
                return await this.makeProxyRequest(url, headers, proxy);
            } catch (error) {
                lastError = error as Error;
                log.warn(`Request failed with proxy ${proxy.host}:${proxy.port} (uptime: ${proxy.uptime}%), trying next...`);

                // Add small delay between retries
                if (attempt < this.maxRetries - 1) {
                    await new Promise(resolve => setTimeout(resolve, 500));
                }
            }
        }

        // If all proxies failed, try direct connection as fallback
        try {
            log.warn('All proxies failed, attempting direct connection...');
            return await this.makeDirectRequest(url, headers);
        } catch (error) {
            throw lastError || error;
        }
    }

    private async makeDirectRequest(url: string, headers: Record<string, string>): Promise<string> {
        const response = await fetch(url, {
            method: 'GET',
            headers: {
                'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
                ...headers
            },
        });

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        return response.text();
    }

    private async makeProxyRequest(url: string, headers: Record<string, string>, proxy: ProxyConfig): Promise<string> {
        // Note: Proxy implementation depends on your environment
        // For Node.js with fetch, you might need a proxy agent library

        const controller = new AbortController();
        const timeoutId = setTimeout(() => controller.abort(), (proxy.timeout || 2000) + 1000);

        try {
            // This is a simplified example - in a real Node.js environment,
            // you'd use a proxy agent library like https-proxy-agent
            const response = await fetch(url, {
                method: 'GET',
                headers: {
                    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
                    ...headers
                },
                signal: controller.signal,
                // Note: In a real implementation, you'd configure the proxy here
                // This might involve using a proxy agent or configuring your HTTP client
            });

            clearTimeout(timeoutId);

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            log.info(`Successfully used proxy ${proxy.host}:${proxy.port}`);
            return response.text();

        } catch (error) {
            clearTimeout(timeoutId);
            throw error;
        }
    }

    // Public methods to configure proxy behavior
    setProxyQualityThresholds(minUptime: number, maxTimeout: number): void {
        this.minUptime = minUptime;
        this.maxTimeout = maxTimeout;
    }

    setMaxRetries(retries: number): void {
        this.maxRetries = retries;
    }

    setProxyRefreshInterval(intervalMs: number): void {
        this.proxyRefreshInterval = intervalMs;
    }

    async forceRefreshProxies(): Promise<void> {
        this.lastProxyFetch = 0; // Force refresh
        await this.refreshProxies();
    }

    async clearProxyCache(): Promise<void> {
        try {
            await fs.unlink(this.proxyCacheFile);
            // log.info('Proxy cache cleared');
        } catch (error: unknown) {
            if (error instanceof Error && 'code' in error && error.code === 'ENOENT') {
                log.info('Proxy cache file does not exist');
            } else {
                log.error(`Failed to clear proxy cache: ${error}`);
            }
        }
    }

    getProxyStats(): {
        total: number;
        current: string | null;
        cacheAge: string | null;
        lastFetch: string | null;
    } {
        const currentProxy = this.proxies[this.currentProxyIndex];

        let cacheAge: string | null = null;
        if (this.lastProxyFetch > 0) {
            const ageMs = Date.now() - this.lastProxyFetch;
            const ageHours = Math.floor(ageMs / (1000 * 60 * 60));
            const ageMinutes = Math.floor((ageMs % (1000 * 60 * 60)) / (1000 * 60));
            cacheAge = `${ageHours}h ${ageMinutes}m`;
        }

        return {
            total: this.proxies.length,
            current: currentProxy ? `${currentProxy.host}:${currentProxy.port}` : null,
            cacheAge,
            lastFetch: this.lastProxyFetch > 0 ? new Date(this.lastProxyFetch).toISOString() : null
        };
    }

    async getLyrics(title: string, api_key: string | null = null, artist?: string): Promise<LyricsResponse | ErrorResponse> {
        try {
            const headers: Record<string, string> = {};
            if (api_key) {
                headers['Authorization'] = `Bearer ${api_key}`;
            }

            // Build search query - include artist if provided
            let searchQuery = title;
            if (artist) {
                searchQuery = `${title} ${artist}`;
            }

            const searchResponse = await this.get(`${this.geniusURL}?q=${encodeURIComponent(searchQuery)}`, headers);

            const data = JSON.parse(searchResponse);
            if (data.meta.status !== 200 || !data.response.hits.length) {
                throw new Error('No lyrics results found.');
            }

            // If artist is provided, try to find a better match
            let bestMatch = data.response.hits[0].result;

            if (artist && data.response.hits.length > 1) {
                // Look for a match that includes the artist name
                const artistLower = artist.toLowerCase();
                for (const hit of data.response.hits) {
                    const resultArtist = hit.result.primary_artist.name.toLowerCase();
                    if (resultArtist.includes(artistLower) || artistLower.includes(resultArtist)) {
                        bestMatch = hit.result;
                        break;
                    }
                }
            }

            const result = bestMatch;
            const track_name = result.title;
            const artist_name = result.primary_artist.name;
            const artwork_url = result.song_art_image_thumbnail_url || null;
            const search_engine = 'Genius';

            const lyricsUrl = result.url;
            const lyricsHtml = await this.get(lyricsUrl);

            const $ = cheerio.load(lyricsHtml);
            let lyrics = '';

            $('div[data-lyrics-container="true"]').each((_, elem) => {
                // Clone the element to avoid modifying the original
                const $clonedElem = $(elem).clone();

                // Remove all child elements with data-exclude-from-selection="true"
                $clonedElem.find('div[data-exclude-from-selection="true"]').remove();

                const snippet = $clonedElem
                    .html()
                    ?.replace(/<br\s*\/?>/g, '\n')
                    .replace(/<(?!br\s*\/?)[^>]+>/gi, '')
                    .trim();
                if (snippet) lyrics += snippet + "\n\n";
                // if (snippet) {
                //     // Split by lines and filter out section labels like [Verse 1], [Chorus], etc.
                //     const filteredLines = snippet
                //         .split('\n')
                //         .filter(line => {
                //             const trimmedLine = line.trim();
                //             // Skip lines that are section labels (start with [ and end with ])
                //             return !(trimmedLine.startsWith('[') && trimmedLine.endsWith(']'));
                //         })
                //         .join('\n')
                //         .trim();

                //     if (filteredLines) {
                //         lyrics += filteredLines + "\n\n";
                //     }
                // }
            });

            if (!lyrics) {
                throw new Error('Lyrics could not be extracted.');
            }

            return { artist_name, track_name, search_engine, artwork_url, lyrics: lyrics.trim() };
        } catch (error) {
            log.error(`Error: ${error instanceof Error ? error.message : String(error)}`);
            return { message: 'No lyrics were found.', response: '404 Not Found' };
        }
    }
}

export default Genius;