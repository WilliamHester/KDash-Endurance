import { sveltekit } from '@sveltejs/kit/vite';
import { defineConfig } from 'vite';
import commonjs from 'vite-plugin-commonjs';

export default defineConfig({
	plugins: [commonjs(), sveltekit()],
  server: {
    port: 3000,
    strictPort: true
  },
  optimizeDeps: {
    include: [
      'google-protobuf',
      'grpc-web',
      // '@improbable-eng/grpc-web',
    ],
  },
  ssr: {
    noExternal: ['@improbable-eng/grpc-web']
  },
});

