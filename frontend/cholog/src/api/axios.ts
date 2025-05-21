import axios, { AxiosInstance } from "axios";

const api = axios.create({
  baseURL: `${import.meta.env.VITE_API_BASE_URL}/api`,
  withCredentials: true,
  // headers: {
  //   "X-DEV-USER": 1,
  // },
});

export default api;
