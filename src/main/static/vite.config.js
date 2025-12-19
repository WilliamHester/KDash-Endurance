import { sveltekit } from '@sveltejs/kit/vite';
import { defineConfig } from 'vite';
import commonjs from 'vite-plugin-commonjs';
import tailwindcss from "@tailwindcss/vite";

export default defineConfig({
	plugins: [
    commonjs(),
    tailwindcss(),
    sveltekit(),
  ],
  server: {
    port: 3000,
    strictPort: true
  },
  optimizeDeps: {
    include: [
      'google-protobuf',
      'grpc-web',
    ],
  },
  ssr: {
    noExternal: ['@improbable-eng/grpc-web']
  },
});

