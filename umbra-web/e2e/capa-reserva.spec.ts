import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { expect, test } from '@playwright/test';

const PASTA_DO_DEPLOY = resolve(__dirname, '../../deploy');

test('T4.6c, a capa reserva do compose e o PNG versionado em deploy', () => {
  const compose = readFileSync(resolve(PASTA_DO_DEPLOY, 'compose.yaml'), 'utf8');
  const arte = readFileSync(resolve(PASTA_DO_DEPLOY, 'capa-reserva.png')).toString('base64');

  expect(compose).toMatch(/^ {6}IMGPROXY_FALLBACK_IMAGE_HTTP_CODE: "200"?$/m);
  expect(compose).toMatch(/^ {6}IMGPROXY_FALLBACK_IMAGE_TTL: "86400"?$/m);
  expect(compose.match(/^ {6}IMGPROXY_FALLBACK_IMAGE_DATA: (\S+)$/m)?.[1]).toBe(arte);
});
