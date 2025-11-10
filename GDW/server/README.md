# Letters API

Express service that guards anonymous letters with a lightweight proof-of-work, Redis-backed rate limiting, and optional on-chain anchoring via ethers.

## Prerequisites
- Node.js 18+
- Docker + Docker Compose v2

## Quick start
1. `cp .env.example .env` and update secrets (RPC key, signer, contract, database URLs).
2. From the repo root run `docker compose up -d` to launch Postgres + Redis.
3. Apply migrations from `server/`: `npm run db:sql`.
4. Development server: `npm run dev` (auto restarts via nodemon).
5. Production run: `npm start`.

## API outline
- `GET /pow` – fetch PoW challenge `{ prefix, nonceSalt, zeros, ts }`.
- `POST /letters` – submit `{ orgId, body, category?, pow }`; verifies challenge, stores record, anchors hash.
- `GET /verify/:id` – returns `{ id, org_id, hash_onchain, tx_hash }` for auditing.

Each request is capped at ~200 kB JSON. Redis rate limiter allows 5 requests/min/IP and blocks violators for 60s.
