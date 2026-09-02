#!/usr/bin/env node
// Converts the five authoritative OpenAPI contracts in docs/api/*.openapi.yaml into JSON so the
// API console and role inspector can `import` them as ordinary TypeScript modules and get them
// bundled at build time - no runtime fetch, no risk of the portal shipping without them.
//
// Run automatically by `pnpm start`, `pnpm build` and `pnpm test` (see package.json). The output
// in src/app/core/openapi/generated is gitignored on purpose: docs/api is the single source of
// truth, and a checked-in copy would go stale the moment a spec changes without anyone noticing.
import { readFile, writeFile, mkdir } from 'node:fs/promises';
import { existsSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { parse } from 'yaml';

const here = path.dirname(fileURLToPath(import.meta.url));
const webAdminRoot = path.resolve(here, '..');
// Repo root is three levels up from app/frontend/web-admin - the same relative path the backend
// compose file uses to mount docs/api into the Prism containers. Keep the two in sync.
const specsDir = path.resolve(webAdminRoot, '../../../docs/api');
const outDir = path.resolve(webAdminRoot, 'src/app/core/openapi/generated');

const SPECS = [
  { file: 'auth.openapi.yaml', id: 'auth' },
  { file: 'user.openapi.yaml', id: 'user' },
  { file: 'semaphore.openapi.yaml', id: 'semaphore' },
  { file: 'schedule.openapi.yaml', id: 'schedule' },
  { file: 'map.openapi.yaml', id: 'map' },
];

async function main() {
  if (!existsSync(specsDir)) {
    throw new Error(`OpenAPI specs not found at ${specsDir}. Is docs/api still at the repo root?`);
  }
  await mkdir(outDir, { recursive: true });

  for (const spec of SPECS) {
    const sourcePath = path.join(specsDir, spec.file);
    const raw = await readFile(sourcePath, 'utf-8');
    const doc = parse(raw);
    const outPath = path.join(outDir, `${spec.id}.json`);
    await writeFile(outPath, JSON.stringify(doc), 'utf-8');
    console.log(`generated ${path.relative(webAdminRoot, outPath)} from ${spec.file}`);
  }
}

main().catch((err) => {
  console.error('[generate-openapi] failed:', err.message);
  process.exit(1);
});
