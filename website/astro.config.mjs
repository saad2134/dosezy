// @ts-check
import { defineConfig } from 'astro/config';
import tailwindcss from '@tailwindcss/vite';
import { socialRedirects } from './src/lib/social-links.ts';

// https://astro.build/config
export default defineConfig({
  redirects: socialRedirects,
  vite: {
    plugins: [tailwindcss()],
  },
});
