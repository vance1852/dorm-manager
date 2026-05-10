import axios from "axios";
import { ElMessage } from "element-plus";
import router from "../router";

const request = axios.create({
  baseURL: "/api",
  timeout: 15000,
});

request.interceptors.request.use((config) => {
  const token = localStorage.getItem("token");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// HTTP状态码错误信息映射
const httpErrorMessages = {
  400: "请求参数错误，请检查输入",
  401: "登录已过期，请重新登录",
  403: "没有权限访问该资源",
  404: "请求的资源不存在",
  405: "请求方法不被允许",
  408: "请求超时，请稍后重试",
  500: "服务器内部错误，请稍后重试",
  502: "网关错误，服务暂时不可用",
  503: "服务暂时不可用，请稍后重试",
  504: "网关超时，请稍后重试",
};

request.interceptors.response.use(
  (response) => {
    const res = response.data;
    if (res instanceof Blob) {
      return res;
    }
    if (res.code !== 200) {
      ElMessage.error(res.message || "请求失败");
      if (res.code === 401) {
        localStorage.removeItem("token");
        router.push("/login");
      }
      return Promise.reject(new Error(res.message));
    }
    return res.data;
  },
  (error) => {
    let message = "网络连接异常，请检查网络设置";

    if (error.response) {
      const status = error.response.status;
      message = httpErrorMessages[status] || `请求失败 (${status})`;

      // 401 未授权，跳转登录
      if (status === 401) {
        localStorage.removeItem("token");
        router.push("/login");
      }
    } else if (error.code === "ECONNABORTED") {
      message = "请求超时，请检查网络后重试";
    } else if (error.message?.includes("Network Error")) {
      message = "网络连接失败，请检查网络设置";
    }

    ElMessage.error(message);
    return Promise.reject(error);
  },
);

export default request;
