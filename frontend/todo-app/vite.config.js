import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  define: {
    global: 'globalThis'
  },
  resolve: {
    alias: {
      process: 'process/browser',
      stream: 'stream-browserify',
    }
  },
  optimizeDeps: {
    include: ['buffer', 'process']
  }
});
