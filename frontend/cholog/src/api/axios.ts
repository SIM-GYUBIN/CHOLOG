import axios, { AxiosInstance } from "axios";

const api = axios.create({
  baseURL: `${import.meta.env.VITE_API_BASE_URL}/api`,
  headers: {
    "X-DEV-USER": import.meta.env.VITE_API_DEV_USER,
  },
});

export default api;
