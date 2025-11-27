import { NextResponse } from "next/server";
import { rateLimit } from "@/utils/ratelimit";
import { fetchJsonWithCache } from "@/utils/cache";

const CACHE_TTL = 30 * 60 * 1000;

export async function GET(req: Request) {
  const url = new URL(req.url);
  const segments = url.pathname.split("/").filter(Boolean);
  const platform = segments[1];

  if (process.env.RATELIMIT && process.env.NODE_ENV == "production") {
    const forwarded = req.headers.get('x-forwarded-for');
    const ip = forwarded ? forwarded.split(',')[0] : 'unknown';
    const { allowed, retryAfter } = rateLimit(ip);

    if (!allowed) {
      return NextResponse.json(
        {
          message: `Too many requests, please try again in ${retryAfter}s`,
          response: "429 Too Many Requests",
        }
      )
    }
  }

  const trackid = url.searchParams.get("trackid") ?? url.searchParams.get("trackId") ?? undefined;
  const title = url.searchParams.get("title") ?? undefined;
  const artist = url.searchParams.get("artist") ?? undefined;
  const langcode = url.searchParams.get("translate") ?? undefined;

  const backendUrl = new URL(`${process.env.API_URL}/api/v2/lyrics`);
  backendUrl.searchParams.append("platform", platform);

  if (trackid) backendUrl.searchParams.append("trackid", trackid);
  if (title) backendUrl.searchParams.append("title", title);
  if (artist) backendUrl.searchParams.append("artist", artist);
  if (langcode) backendUrl.searchParams.append("translate", langcode);

  const { data } = await fetchJsonWithCache(backendUrl.toString(), CACHE_TTL);

  return NextResponse.json(data);
}
