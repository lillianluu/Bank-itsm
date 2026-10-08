import type {AxiosResponse, InternalAxiosRequestConfig} from 'axios';
import axios from 'axios';

/** 与后端 itsm-common 的 Result<T> 对齐:{"code":0,"message":"ok","data":{},"traceId":"..."} */
export interface Result<T = unknown> {
  code: number;
  message: string;
  data: T;
  traceId?: string;
}

const TOKEN_KEY = 'itsm_access_token';
const REFRESH_TOKEN_KEY = 'itsm_refresh_token';

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY);
}

export function setToken(accessToken: string, refreshToken?: string): void {
  localStorage.setItem(TOKEN_KEY, accessToken);
  if (refreshToken) {
    localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
  }
}

export function clearToken(): void {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(REFRESH_TOKEN_KEY);
}

const http = axios.create({
  baseURL: '/api/v1',
  timeout: 15000
});

// 请求拦截:统一注入 JWT(http 后端要求 Authorization: Bearer <accessToken>)
http.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = getToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

http.interceptors.response.use(
  (res) => {
    const body = res.data as Result;
    if (body.code === 0) {
      // 约定:业务代码通过 http.get<never, T>() 直接拿到 data 部分
      return body.data as unknown as AxiosResponse;
    }
    return Promise.reject(new Error(body.message || '请求失败'));
  },
  (err) => {
    // 401:未登录/凭证失效 → 清理本地凭证并回到登录页
    if (err.response?.status === 401 && !window.location.pathname.startsWith('/login')) {
      clearToken();
      window.location.href = '/login';
      return Promise.reject(new Error('未登录或登录已过期'));
    }
    // 业务错误体也统一取 Result.message,便于界面直接提示
    const body = err.response?.data as Result | undefined;
    return Promise.reject(new Error(body?.message || err.message || '请求失败'));
  }
);

export default http;

