import axios from 'axios';

/** 与后端 itsm-common 的 Result<T> 对齐:{"code":0,"message":"ok","data":{},"traceId":"..."} */
export interface Result<T = unknown> {
  code: number;
  message: string;
  data: T;
  traceId?: string;
}

const http = axios.create({
  baseURL: '/api/v1',
  timeout: 15000
});

http.interceptors.response.use(
  (res) => {
    const body = res.data as Result;
    if (body.code === 0) {
      return body.data;
    }
    return Promise.reject(new Error(body.message || '请求失败'));
  },
  (err) => Promise.reject(err)
);

export default http;

