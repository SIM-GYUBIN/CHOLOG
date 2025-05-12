import { configureStore } from '@reduxjs/toolkit';
import userReducer from './slices/userSlice';
import projectReducer from './slices/projectSlice';
import logReducer from './slices/logSlice';
import extraFeaturesReducer from './slices/extraFeaturesSlice';
import reportReducer from './slices/reportSlice';

const store = configureStore({
  reducer: {
    user: userReducer,
    project: projectReducer,
    log: logReducer,
    extraFeatures: extraFeaturesReducer,
    report: reportReducer,
  },
});

export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;

export default store;
