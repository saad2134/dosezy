import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { execSync } from 'node:child_process';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const rootDir = path.resolve(__dirname, '..');
const satteriDir = path.join(rootDir, 'node_modules', 'satteri');
const targetFile = path.join(satteriDir, 'satteri_napi.wasi.cjs');

async function main() {
  if (!fs.existsSync(satteriDir)) {
    return;
  }
  if (fs.existsSync(targetFile)) {
    return;
  }

  console.log('[setup-satteri] Setting up WASI fallback for Windows Smart App Control...');
  const tmpDir = path.join(rootDir, 'node_modules', '.tmp-satteri-wasi');
  fs.mkdirSync(tmpDir, { recursive: true });
  const tgzPath = path.join(tmpDir, 'satteri-wasi.tgz');

  try {
    const res = await fetch('https://registry.npmjs.org/@bruits/satteri-wasm32-wasi/-/satteri-wasm32-wasi-0.11.7.tgz');
    if (!res.ok) {
      throw new Error(`Failed to fetch WASI package: ${res.statusText}`);
    }
    const buffer = Buffer.from(await res.arrayBuffer());
    fs.writeFileSync(tgzPath, buffer);

    execSync(`tar -xzf "${tgzPath}" -C "${tmpDir}"`, { stdio: 'pipe' });

    const pkgDir = path.join(tmpDir, 'package');
    const wasiCjs = path.join(pkgDir, 'satteri_napi.wasi.cjs');
    const wasiWasm = path.join(pkgDir, 'satteri_napi.wasm32-wasi.wasm');

    if (fs.existsSync(wasiCjs)) {
      fs.copyFileSync(wasiCjs, path.join(satteriDir, 'satteri_napi.wasi.cjs'));
    }
    if (fs.existsSync(wasiWasm)) {
      fs.copyFileSync(wasiWasm, path.join(satteriDir, 'satteri_napi.wasm32-wasi.wasm'));
    }
    console.log('[setup-satteri] Satteri WASI fallback successfully installed.');
  } catch (err) {
    console.warn('[setup-satteri] Could not auto-install WASI fallback:', err.message);
  } finally {
    try {
      fs.rmSync(tmpDir, { recursive: true, force: true });
    } catch {}
  }
}

main();

