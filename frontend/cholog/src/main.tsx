import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.tsx'

// if (import.meta.env.MODE === 'development') {
//   import('./mocks/browser').then(({ worker }) => {
//     worker.start();
//   });
// }

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
