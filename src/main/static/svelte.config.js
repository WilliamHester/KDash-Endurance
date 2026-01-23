import adapter from '@sveltejs/adapter-static';

/** @type {import('@sveltejs/kit').Config} */
const config = {
	kit: {
    adapter: adapter({
			pages: 'build',
			assets: 'build',
      // The docs say to avoid index.html because there may be a prerendered
      // page there, but we don't want or care for a prerendered page.
      // index.html is automatically served by caddy when you hit /
			fallback: 'index.html',
			precompress: false,
			strict: true
		})
  }
};

export default config;
