import rateLimit from "express-rate-limit";

const ratelimit = rateLimit({
    windowMs: 1 * 60 * 1000,
    max: 20,
    message: {
        message: "Too many requests, please try again later.",
        response: "429 Too Many Requests"
    },
});

export default ratelimit;
