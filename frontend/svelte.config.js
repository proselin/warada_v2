import adapter from '@sveltejs/adapter-static';

/** @type {import('@sveltejs/kit').Config} */
const config = {
  kit: {
    adapter: adapter(),
    alias: {
      $components: './src/lib/components',
      $api: './src/lib/api'
    }
  }
};

export default config;
