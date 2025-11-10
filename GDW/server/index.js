const crypto = require('node:crypto');
const express = require('express');
const cors = require('cors');
const helmet = require('helmet');
const Redis = require('ioredis');
const { RateLimiterRedis, RateLimiterRes } = require('rate-limiter-flexible');
const { Pool } = require('pg');
const { z } = require('zod');
const { ethers } = require('ethers');
const xss = require('xss');
const dotenv = require('dotenv');

dotenv.config();

const PORT = Number(process.env.PORT || 3000);
const ORG_ID = process.env.ORG_ID || 'kunsan-univ';
const POW_ZEROS = clampZeros(Number(process.env.POW_ZEROS || 18));
const CHALLENGE_TTL_MS = 5 * 60 * 1000;
const redisUrl = process.env.REDIS_URL || 'redis://localhost:6379';
const challengeStore = new Map();

const redisClient = new Redis(redisUrl, { lazyConnect: true });
redisClient.on('error', (err) => {
  console.error('[redis] connection error', err.message);
});

(async () => {
  try {
    await redisClient.connect();
  } catch (err) {
    console.error('[redis] failed to connect, rate limiting disabled', err.message);
  }
})();

const rateLimiter = new RateLimiterRedis({
  storeClient: redisClient,
  points: 5,
  duration: 60,
  blockDuration: 60,
  keyPrefix: 'rl_api'
});

const dbPool = process.env.DATABASE_URL
  ? new Pool({ connectionString: process.env.DATABASE_URL })
  : null;

if (dbPool) {
  dbPool.on('error', (err) => {
    console.error('[db] idle client error', err.message);
  });
} else {
  console.warn('[db] DATABASE_URL not set, persistence disabled');
}

const app = express();
app.use(helmet());
app.use(cors());
app.use(express.json({ limit: '200kb' }));
app.use(rateLimitMiddleware);

const powSchema = z.object({
  prefix: z.string().min(1),
  nonceSalt: z.string().min(1),
  nonce: z.string().min(1),
  ts: z.number().int().positive()
});

const letterSchema = z.object({
  orgId: z.string().min(1),
  body: z.string().min(1).max(5000),
  category: z.string().min(1).max(120).optional(),
  pow: powSchema
});

app.get('/pow', (req, res) => {
  const challenge = createChallenge();
  res.json(challenge);
});

app.post('/letters', async (req, res, next) => {
  try {
    const parsed = letterSchema.parse(req.body);

    if (parsed.orgId !== ORG_ID) {
      return res.status(400).json({ error: 'org mismatch' });
    }

    const sanitizedBody = sanitizeInput(parsed.body);
    const sanitizedCategory = parsed.category ? sanitizeInput(parsed.category) : null;
    const challenge = getChallenge(parsed.pow.nonceSalt);

    if (!challenge) {
      return res.status(400).json({ error: 'challenge expired or unknown' });
    }

    if (challenge.prefix !== parsed.pow.prefix || challenge.ts !== parsed.pow.ts) {
      return res.status(400).json({ error: 'challenge mismatch' });
    }

    const powHash = computePowHash(parsed.pow);
    if (!meetsDifficulty(powHash, POW_ZEROS)) {
      return res.status(400).json({ error: 'invalid proof of work' });
    }

    challengeStore.delete(parsed.pow.nonceSalt);

    const deviceGroup = deriveDeviceGroup(req.get('user-agent') || '');
    const payloadHash = ethers.keccak256(
      ethers.toUtf8Bytes(JSON.stringify({
        orgId: parsed.orgId,
        body: sanitizedBody,
        category: sanitizedCategory
      }))
    );

    const { hashOnChain, txHash } = await anchorLetterHash(payloadHash);

    const record = await persistLetter({
      orgId: parsed.orgId,
      body: sanitizedBody,
      category: sanitizedCategory,
      deviceGroup,
      hashOnChain,
      txHash,
      powOk: true
    });

    return res.status(201).json({
      id: record.id,
      txHash,
      createdAt: record.created_at
    });
  } catch (err) {
    if (err instanceof z.ZodError) {
      return res.status(400).json({ error: err.issues });
    }
    return next(err);
  }
});

app.get('/verify/:id', async (req, res, next) => {
  try {
    if (!dbPool) {
      return res.status(503).json({ error: 'database unavailable' });
    }

    const { rows } = await dbPool.query(
      'select id, org_id, hash_onchain, tx_hash from letters where id = $1',
      [req.params.id]
    );

    if (rows.length === 0) {
      return res.status(404).json({ error: 'letter not found' });
    }

    return res.json(rows[0]);
  } catch (err) {
    return next(err);
  }
});

app.use((err, req, res, _next) => {
  console.error(err);
  res.status(500).json({ error: 'internal server error' });
});

app.listen(PORT, () => {
  console.log(`api ready on port ${PORT}`);
});

function rateLimitMiddleware(req, res, next) {
  if (!redisClient.status || redisClient.status !== 'ready') {
    return next();
  }

  rateLimiter
    .consume(req.ip)
    .then(() => next())
    .catch((err) => {
      if (err instanceof RateLimiterRes) {
        return res.status(429).json({
          error: 'rate limit exceeded',
          retryAfter: err.msBeforeNext / 1000
        });
      }
      return next(err);
    });
}

function createChallenge() {
  const prefix = crypto.randomBytes(8).toString('hex');
  const nonceSalt = crypto.randomBytes(8).toString('hex');
  const ts = Date.now();
  const expiresAt = ts + CHALLENGE_TTL_MS;

  challengeStore.set(nonceSalt, { prefix, ts, expiresAt });
  return { prefix, nonceSalt, zeros: POW_ZEROS, ts };
}

function getChallenge(nonceSalt) {
  const entry = challengeStore.get(nonceSalt);
  if (!entry) return null;
  if (entry.expiresAt < Date.now()) {
    challengeStore.delete(nonceSalt);
    return null;
  }
  return entry;
}

setInterval(() => {
  const now = Date.now();
  for (const [salt, data] of challengeStore.entries()) {
    if (data.expiresAt < now) {
      challengeStore.delete(salt);
    }
  }
}, CHALLENGE_TTL_MS).unref();

function computePowHash(powPayload) {
  const payload = `${powPayload.prefix}:${powPayload.nonceSalt}:${powPayload.ts}:${powPayload.nonce}`;
  return crypto.createHash('sha256').update(payload).digest('hex');
}

function meetsDifficulty(hashHex, zeros) {
  if (zeros <= 0) return true;
  const hashValue = BigInt(`0x${hashHex}`);
  const threshold = 256n - BigInt(Math.min(zeros, 256));
  return (hashValue >> threshold) === 0n;
}

function clampZeros(value) {
  if (!Number.isFinite(value) || value <= 0) return 1;
  if (value > 256) return 256;
  return Math.floor(value);
}

function sanitizeInput(input) {
  return xss(input, {
    whiteList: {},
    stripIgnoreTag: true,
    stripIgnoreTagBody: ['script']
  }).trim();
}

function deriveDeviceGroup(userAgent) {
  const ua = userAgent.toLowerCase();
  if (ua.includes('android')) return 'android';
  if (ua.includes('iphone') || ua.includes('ipad')) return 'ios';
  if (ua.includes('mac os') || ua.includes('windows') || ua.includes('linux')) return 'desktop';
  return 'unknown';
}

async function anchorLetterHash(payloadHash) {
  const rpcUrl = process.env.RPC_URL;
  const signerKey = process.env.SIGNER_KEY;
  const contractAddr = process.env.CONTRACT_ADDR;

  if (!rpcUrl || !signerKey || !contractAddr) {
    return { hashOnChain: payloadHash, txHash: null };
  }

  try {
    const provider = new ethers.JsonRpcProvider(rpcUrl);
    const wallet = new ethers.Wallet(signerKey, provider);
    const iface = new ethers.Interface(['function anchor(bytes32 payloadHash) returns (bool)']);
    const data = iface.encodeFunctionData('anchor', [payloadHash]);
    const txResponse = await wallet.sendTransaction({
      to: contractAddr,
      data,
      value: 0
    });
    const receipt = await txResponse.wait(1);
    return { hashOnChain: payloadHash, txHash: receipt.hash };
  } catch (err) {
    console.error('[anchor] failed to submit tx', err.message);
    return { hashOnChain: payloadHash, txHash: null };
  }
}

async function persistLetter({ orgId, body, category, deviceGroup, hashOnChain, txHash, powOk }) {
  if (!dbPool) {
    return {
      id: crypto.randomUUID(),
      created_at: new Date().toISOString()
    };
  }

  const insert = `
    insert into letters (org_id, body, category, pow_ok, device_group, hash_onchain, tx_hash, status)
    values ($1, $2, $3, $4, $5, $6, $7, $8)
    returning id, created_at
  `;

  const status = txHash ? 'anchored' : 'pending';
  const result = await dbPool.query(insert, [
    orgId,
    body,
    category,
    powOk,
    deviceGroup,
    hashOnChain,
    txHash,
    status
  ]);

  return result.rows[0];
}
