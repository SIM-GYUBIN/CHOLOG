import React from "react";
import ReactDOM from "react-dom/client";
import { Provider } from "react-redux";
import store from "./store/store"; // store 경로 정확히 지정
import App from "./App";
import "./index.css";
import Cholog from "cholog-sdk";

Cholog.init({
  apiKey: "f98c78f3-8bea-4ead-a631-3d79bae44c11",
  environment: "prod",
});

ReactDOM.createRoot(document.getElementById("root") as HTMLElement).render(
  <React.StrictMode>
    <Provider store={store}>
      <App />
    </Provider>
  </React.StrictMode>
);
