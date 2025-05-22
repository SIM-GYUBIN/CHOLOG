import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import api from '../../api/axios';

// 웹훅 조회 요청 인터페이스
interface FetchWebhookRequest {
  projectId: number;
}

// 웹훅 조회 응답 인터페이스
interface WebhookResponse {
  success: boolean;
  data: {
    exists: boolean;
    webhookItem?: {
      id: number;
      mmURL: string;
      keywords: string;
      notificationENV: string;
      isEnabled: boolean;
    };
  };
  error?: {
    code: string;
    message: string;
  };
  timestamp: string;
}

// 웹훅 저장 요청 인터페이스
interface SaveWebhookRequest {
  projectId: number;
  webhookItem: {
    mmURL: string;
    keywords: string;
    notificationENV: string;
    isEnabled: boolean;
  };
}

export const fetchWebhook = createAsyncThunk(
    'webhook/fetchWebhook',
    async (projectId: number, { rejectWithValue }) => {
      try {
        const res = await api.get(`/webhook/${projectId}`);
        return res.data;
      } catch (err: any) {
        return rejectWithValue(err.response?.data || err.message);
      }
    }
  );


// 웹훅 저장 API 요청
export const saveWebhook = createAsyncThunk<
  WebhookResponse,
  SaveWebhookRequest
>('webhook/saveWebhook', async (params, { rejectWithValue }) => {
  try {
    const response = await api.post<WebhookResponse>(
      `/webhook/${params.projectId}`,
      params.webhookItem,
      {
        headers: {
          'Content-Type': 'application/json',
        },
      }
    );
    return response.data;
  } catch (error: any) {
    const status = error.response?.status;
    let errorCode = 'INTERNAL_ERROR';
    
    if (status === 400) errorCode = 'INVALID_REQUEST';
    else if (status === 401) errorCode = 'UNAUTHORIZED';

    return rejectWithValue({
      success: false,
      data: {
        exists: false
      },
      error: {
        code: errorCode,
        message: error.response?.data?.error?.message || '웹훅 설정 저장 중 오류가 발생했습니다.',
      },
      timestamp: new Date().toISOString(),
    } as WebhookResponse);
  }
});

// 웹훅 수정 요청 인터페이스
interface UpdateWebhookRequest {
    projectId: number;
    webhookItem: {
      mmURL: string;
      keywords: string;
      notificationENV: string;
      isEnabled: boolean;
    };
  }
  
  // 웹훅 수정 API 요청
  export const updateWebhook = createAsyncThunk<
    WebhookResponse,
    UpdateWebhookRequest
  >('webhook/updateWebhook', async (params, { rejectWithValue }) => {
    try {
      const response = await api.put<WebhookResponse>(
        `/webhook/${params.projectId}`,
        params.webhookItem,
        {
          headers: {
            'Content-Type': 'application/json',
          },
        }
      );
      return response.data;
    } catch (error: any) {
      const status = error.response?.status;
      let errorCode = 'INTERNAL_ERROR';
      
      if (status === 400) errorCode = 'INVALID_REQUEST';
      else if (status === 401) errorCode = 'UNAUTHORIZED';
  
      return rejectWithValue({
        success: false,
        data: {
          exists: false
        },
        error: {
          code: errorCode,
          message: error.response?.data?.error?.message || '웹훅 설정 수정 중 오류가 발생했습니다.',
        },
        timestamp: new Date().toISOString(),
      } as WebhookResponse);
    }
  });

// 웹훅 슬라이스 상태 인터페이스
interface WebhookState {
  webhookData: {
    exists: boolean;
    webhookItem?: {
      id: number;
      mmURL: string;
      keywords: string;
      notificationENV: string;
      isEnabled: boolean;
    };
  };
  isLoading: boolean;
  error: string | null;
}

// 초기 상태
const initialState: WebhookState = {
  webhookData: {
    exists: false
  },
  isLoading: false,
  error: null
};

// 웹훅 슬라이스 생성
const webhookSlice = createSlice({
  name: 'webhook',
  initialState,
  reducers: {},
  extraReducers: (builder) => {
    // 웹훅 조회 요청 처리
    builder
      .addCase(fetchWebhook.pending, (state) => {
        state.isLoading = true;
        state.error = null;
      })
      .addCase(fetchWebhook.fulfilled, (state, action) => {
        state.isLoading = false;
        state.webhookData = action.payload.data;
      })
      .addCase(fetchWebhook.rejected, (state, action) => {
        state.isLoading = false;
        state.error = (action.payload as WebhookResponse)?.error?.message || '웹훅 설정 조회 중 오류가 발생했습니다.';
      });
    
    // 웹훅 저장 요청 처리
    builder
      .addCase(saveWebhook.pending, (state) => {
        state.isLoading = true;
        state.error = null;
      })
      .addCase(saveWebhook.fulfilled, (state, action) => {
        state.isLoading = false;
        state.webhookData = action.payload.data;
      })
      .addCase(saveWebhook.rejected, (state, action) => {
        state.isLoading = false;
        state.error = (action.payload as WebhookResponse)?.error?.message || '웹훅 설정 저장 중 오류가 발생했습니다.';
      });

      // 웹훅 수정 요청 처리
builder
.addCase(updateWebhook.pending, (state) => {
  state.isLoading = true;
  state.error = null;
})
.addCase(updateWebhook.fulfilled, (state, action) => {
  state.isLoading = false;
  state.webhookData = action.payload.data;
})
.addCase(updateWebhook.rejected, (state, action) => {
  state.isLoading = false;
  state.error = (action.payload as WebhookResponse)?.error?.message || '웹훅 설정 수정 중 오류가 발생했습니다.';
});
  }
});

export default webhookSlice.reducer;