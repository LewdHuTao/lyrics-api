interface RateLimitStore {
    [key: string]: { count: number; lastRequest: number };
}

const store: RateLimitStore = {};

const WINDOW_SIZE = 60 * 1000;
const MAX_REQUESTS = 20;

export function rateLimit(ip: string): { allowed: boolean; retryAfter?: number } {
    const now = Date.now();
    const entry = store[ip];

    if (!entry) {
        store[ip] = { count: 1, lastRequest: now };
        return { allowed: true };
    }

    if (now - entry.lastRequest > WINDOW_SIZE) {
        store[ip] = { count: 1, lastRequest: now };
        return { allowed: true };
    }

    if (entry.count >= MAX_REQUESTS) {
        const retryAfter = Math.ceil((WINDOW_SIZE - (now - entry.lastRequest)) / 1000);
        return { allowed: false, retryAfter };
    }

    entry.count += 1;
    entry.lastRequest = now;
    return { allowed: true };
}
