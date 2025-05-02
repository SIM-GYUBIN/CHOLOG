import { configureStore } from '@reduxjs/toolkit';
import userReducer from './slices/userSlice';
import projectReducer from './slices/projectSlice';
import logReducer from './slices/logSlice';

const store = configureStore({
  reducer: {
    user: userReducer,
    project: projectReducer,
    log: logReducer,
  },
});

export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;

export default store;
