// @ts-check
const eslint = require('@eslint/js');
const { defineConfig } = require('eslint/config');
const tseslint = require('typescript-eslint');
const angular = require('angular-eslint');
const boundaries = require('eslint-plugin-boundaries');

const mesmaFeature = { feature: '{{ from.element.captured.feature }}' };

const elementos = [
  { type: 'api', pattern: 'src/app/api' },
  { type: 'core', pattern: 'src/app/core/*', capture: ['area'] },
  { type: 'shared', pattern: 'src/app/shared/*', capture: ['area'] },
  { type: 'feature-pages', pattern: 'src/app/features/*/pages', capture: ['feature'] },
  { type: 'feature-components', pattern: 'src/app/features/*/components', capture: ['feature'] },
  { type: 'feature-data', pattern: 'src/app/features/*/data', capture: ['feature'] },
];

const arquivos = [
  { pattern: 'src/main.ts', category: 'entrada' },
  { pattern: 'src/app/app*.ts', category: 'app' },
  { pattern: 'src/app/features/*/*.routes.ts', category: 'feature-rotas', capture: ['feature'] },
];

const rotasDaMesmaFeature = { feature: '{{ from.file.captured.feature }}' };

const fronteiras = {
  default: 'disallow',
  message:
    'Fronteira de módulo: {{ from.element.types.[0] }}{{ from.file.categories.[0] }} não pode importar {{ to.element.types.[0] }}{{ to.file.categories.[0] }} (ARCHITECTURE-WEB.md §3.2).',
  policies: [
    { from: { file: { categories: 'entrada' } }, allow: { to: { file: { categories: 'app' } } } },
    {
      from: { file: { categories: 'app' } },
      allow: [
        { to: { file: { categories: 'app' } } },
        { to: { element: { types: ['core', 'shared'] } } },
        { to: { file: { categories: 'feature-rotas' } } },
      ],
    },
    { from: { element: { type: 'core' } }, allow: { to: { element: { types: ['core', 'shared', 'api'] } } } },
    { from: { element: { type: 'shared' } }, allow: { to: { element: { type: 'shared' } } } },
    {
      from: { file: { categories: 'feature-rotas' } },
      allow: [
        { to: { element: { types: ['core'] } } },
        { to: { element: { type: 'feature-pages', captured: rotasDaMesmaFeature } } },
        { to: { element: { type: 'feature-data', captured: rotasDaMesmaFeature } } },
      ],
    },
    {
      from: { element: { type: 'feature-pages' } },
      allow: [
        { to: { element: { types: ['core', 'shared'] } } },
        { to: { element: { type: 'feature-pages', captured: mesmaFeature } } },
        { to: { element: { type: 'feature-components', captured: mesmaFeature } } },
        { to: { element: { type: 'feature-data', captured: mesmaFeature } } },
      ],
    },
    {
      from: { element: { type: 'feature-components' } },
      allow: [
        { to: { element: { type: 'shared' } } },
        { to: { element: { type: 'feature-components', captured: mesmaFeature } } },
      ],
    },
    {
      from: { element: { type: 'feature-data' } },
      allow: [
        { to: { element: { types: ['api', 'core', 'shared'] } } },
        { to: { element: { type: 'feature-data', captured: mesmaFeature } } },
      ],
    },
  ],
};

module.exports = defineConfig([
  { ignores: ['src/app/api/**', 'dist/**', '.angular/**', 'coverage/**'] },
  {
    files: ['**/*.ts'],
    extends: [
      eslint.configs.recommended,
      tseslint.configs.recommended,
      tseslint.configs.stylistic,
      angular.configs.tsRecommended,
    ],
    processor: angular.processInlineTemplates,
    rules: {
      '@angular-eslint/directive-selector': ['error', { type: 'attribute', prefix: 'umbra', style: 'camelCase' }],
      '@angular-eslint/component-selector': ['error', { type: 'element', prefix: 'umbra', style: 'kebab-case' }],
      '@angular-eslint/prefer-on-push-component-change-detection': 'error',
      '@angular-eslint/component-max-inline-declarations': ['error', { template: 0, styles: 0, animations: 0 }],
    },
  },
  {
    files: ['src/**/*.ts'],
    ignores: ['src/**/*.spec.ts'],
    plugins: { boundaries },
    settings: {
      'import/resolver': { typescript: { alwaysTryTypes: true } },
      'boundaries/elements': elementos,
      'boundaries/files': arquivos,
    },
    rules: {
      'boundaries/dependencies': ['error', fronteiras],
      'boundaries/no-unknown-files': 'error',
    },
  },
  {
    files: ['src/**/*.spec.ts'],
    rules: { '@angular-eslint/component-max-inline-declarations': 'off' },
  },
  {
    files: ['**/*.html'],
    extends: [angular.configs.templateRecommended, angular.configs.templateAccessibility],
  },
]);
