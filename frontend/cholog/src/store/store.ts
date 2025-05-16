import { configureStore } from '@reduxjs/toolkit';
import userReducer from './slices/userSlice';
import projectReducer from './slices/projectSlice';
import logReducer from './slices/logSlice';
import extraFeaturesReducer from './slices/extraFeaturesSlice';
import reportReducer from './slices/reportSlice';
import webhookReducer from './slices/webhookSlice';
import jiraReducer from './slices/jiraSlice';

export const store = configureStore({
  reducer: {
    user: userReducer,
    project: projectReducer,
    log: logReducer,
    extraFeatures: extraFeaturesReducer,
    report: reportReducer,
    webhook: webhookReducer,
    jira: jiraReducer,
  },
});

export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;

export default store;
