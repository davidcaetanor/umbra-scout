import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { expect, test } from '@playwright/test';
import { LARGURAS_DA_CAPA } from '../src/app/shared/capa/larguras-da-capa';

const RAIZ = resolve(__dirname, '../..');
const larguras = [...LARGURAS_DA_CAPA].map(String);

test.describe('T4.6d, presets da capa iguais no front, no imgproxy e no Caddy', () => {
  test('o compose define um preset por largura, por largura e em WebP 82', () => {
    const compose = readFileSync(resolve(RAIZ, 'deploy/compose.yaml'), 'utf8');
    const presets = compose.match(/^ {6}IMGPROXY_PRESETS: (\S+?)\r?$/m)?.[1].split(',') ?? [];
    const porLargura = presets.flatMap((preset) => {
      const encontrado = /^capa-(\d+)=resize:fit:(\d+):0\/quality:82\/format:webp$/.exec(preset);
      return encontrado && encontrado[1] === encontrado[2] ? [encontrado[1]] : [];
    });

    expect(porLargura).toEqual(larguras);
  });

  test('o Caddy aceita só as larguras da lista e leva cada uma ao seu preset', () => {
    const caddy = readFileSync(resolve(RAIZ, 'umbra-web/docker/Caddyfile'), 'utf8');
    const aceitas = /path_regexp imagem \^\/imagens\/\(\?:\(([\d|]+)\)\//.exec(caddy)?.[1].split('|');
    const mapeadas = [...caddy.matchAll(/^\t\t(\d+) capa-(\d+)\r?$/gm)].filter(([, largura, preset]) => largura === preset);

    expect(aceitas).toEqual(larguras);
    expect(mapeadas.map(([, largura]) => largura)).toEqual(larguras);
  });

  test('o proxy do ng serve aceita as mesmas larguras', () => {
    const proxy = JSON.parse(readFileSync(resolve(RAIZ, 'umbra-web/proxy.conf.json'), 'utf8')) as Record<string, unknown>;

    for (const chave of Object.keys(proxy)) {
      expect(/^\^\/imagens\/\(([\d|]+)\)\//.exec(chave)?.[1].split('|')).toEqual(larguras);
    }
  });
});
