import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import { resolve } from 'path'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  // Use a relative base so built assets resolve from the current path.
  base: './',
  build: {
    outDir: '../resources/META-INF/resources/roleManager',
    emptyOutDir: true,
    rollupOptions: {
      input: {
        main: resolve(__dirname, 'index.html'),
        app: resolve(__dirname, 'app.html'),
        menu: resolve(__dirname, 'menu.html')
      },
      output: {
        // Ensure consistent file names for web-fragment
        entryFileNames: 'assets/[name].js',
        chunkFileNames: 'assets/[name].js',
        assetFileNames: 'assets/[name].[ext]'
      }
    }
  },
  server: {
    proxy: {
      // Proxy API calls to Spring backend during development
      '/private': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false
      }
    }
  }
})
