import { rateLimit } from "@/utils/ratelimit";
import { NextResponse } from "next/server";
import { fetchJsonWithCache } from "@/utils/cache";

const CACHE_TTL = 5 * 60 * 1000;

export async function GET(req: Request) {
  try {
    const { searchParams } = new URL(req.url);
    const title = searchParams.get("title");
    const backendUrl = `${process.env.API_URL}/api/v2/metadata?platform=musixmatch&title=${title}`;

    if (process.env.RATELIMIT && process.env.NODE_ENV == "production") {
      const forwarded = req.headers.get('x-forwarded-for') ?? undefined;
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

    const { data } = await fetchJsonWithCache(backendUrl, CACHE_TTL);

    return NextResponse.json(data);
  } catch (err: any) {
    return NextResponse.json({ error: err.message }, { status: 500 });
  }
}
