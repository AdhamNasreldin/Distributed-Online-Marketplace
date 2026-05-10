New Era Marketplace Frontend

Environment:
- Node.js 20 or newer
- npm 10 or newer

Run locally:
1. Open a terminal in this folder:
   C:\Users\AboAmer\Desktop\codex AI\NEW ERA\frontend
2. Install dependencies:
   npm.cmd install --cache .npm-cache
3. Start the frontend:
   npm.cmd run dev
4. Open the shown local URL in your browser.

Build for submission:
1. Run:
   npm.cmd run build
2. The production files will be created in the dist folder.

Backend integration:
- By default, the app uses an in-memory mock API for demo purposes.
- To point the frontend to a real backend, create a .env file and set:
  VITE_API_BASE_URL=http://localhost:YOUR_BACKEND_PORT
- The API adapter lives in src/api/marketplaceApi.ts.

Demo verification code:
- 246810

CSV upload columns:
- name, brand, price, quantity, category, description
