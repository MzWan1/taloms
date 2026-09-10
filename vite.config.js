import { defineConfig } from 'vite';
import { resolve } from 'path';

export default defineConfig({
  root: resolve(__dirname, 'src/main/resources/static'),
  publicDir: resolve(__dirname, 'src/main/resources/static'),
  build: {
    outDir: resolve(__dirname, 'src/main/resources/static/dist'),
    assetsDir: '',
    manifest: true,
    rollupOptions: {
      input: {
        main: resolve(__dirname, 'src/main/resources/static/js/taloms.js'),
        offlineDb: resolve(__dirname, 'src/main/resources/static/js/offline-db.js'),
        boundaryMap: resolve(__dirname, 'src/main/resources/static/js/boundary-map.js'),
        formDraft: resolve(__dirname, 'src/main/resources/static/js/form-draft.js'),
      },
      output: {
        entryFileNames: 'js/[name].[hash].js',
        chunkFileNames: 'js/[name].[hash].js',
        assetFileNames: '[name].[hash][extname]'
      }
    }
  },
  server: {
    proxy: {
      '/api': 'http://localhost:8080',
      '/css': 'http://localhost:8080',
      '/js': 'http://localhost:8080'
    }
  }
});
