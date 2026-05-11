import request from "./request";

export const authApi = {
  login: (data) => request.post("/auth/login", data),
  logout: () => request.post("/auth/logout"),
  getInfo: () => request.get("/auth/info"),
  changePassword: (data) => request.put("/auth/password", data),
};

export const userApi = {
  page: (params) => request.get("/users", { params }),
  create: (data) => request.post("/users", data),
  update: (id, data) => request.put(`/users/${id}`, data),
  delete: (id) => request.delete(`/users/${id}`),
  updateStatus: (id, status) =>
    request.put(`/users/${id}/status`, null, { params: { status } }),
};

export const buildingApi = {
  page: (params) => request.get("/buildings", { params }),
  list: () => request.get("/buildings/list"),
  create: (data) => request.post("/buildings", data),
  update: (id, data) => request.put(`/buildings/${id}`, data),
  delete: (id) => request.delete(`/buildings/${id}`),
  updateStatus: (id, status) =>
    request.put(`/buildings/${id}/status`, null, { params: { status } }),
};

export const roomApi = {
  page: (params) => request.get("/rooms", { params }),
  detail: (id) => request.get(`/rooms/${id}`),
  create: (data) => request.post("/rooms", data),
  update: (id, data) => request.put(`/rooms/${id}`, data),
  delete: (id) => request.delete(`/rooms/${id}`),
  updateStatus: (id, status) =>
    request.put(`/rooms/${id}/status`, null, { params: { status } }),
  getBeds: (id) => request.get(`/rooms/${id}/beds`),
};

export const studentApi = {
  page: (params) => request.get("/students", { params }),
  create: (data) => request.post("/students", data),
  update: (id, data) => request.put(`/students/${id}`, data),
  delete: (id) => request.delete(`/students/${id}`),
  bindBed: (id, bedId) =>
    request.post(`/students/${id}/bindBed`, null, { params: { bedId } }),
  unbindBed: (id) => request.post(`/students/${id}/unbindBed`),
};

export const repairApi = {
  page: (params) => request.get("/repairs", { params }),
  myPage: (params) => request.get("/repairs/my", { params }),
  create: (data) => request.post("/repairs", data),
  handle: (id, data) =>
    request.put(`/repairs/${id}/process`, null, {
      params: { status: data.status, reply: data.reply },
    }),
};

export const visitorApi = {
  page: (params) => request.get("/visitors", { params }),
  myPage: (params) => request.get("/visitors/my", { params }),
  create: (data) => request.post("/visitors", data),
  leave: (id) => request.put(`/visitors/${id}/leave`),
};

export const announcementApi = {
  page: (params) => request.get("/announcements", { params }),
  latest: (limit = 5) =>
    request.get("/announcements/latest", { params: { limit } }),
  create: (data) => request.post("/announcements", data),
  update: (id, data) => request.put(`/announcements/${id}`, data),
  delete: (id) => request.delete(`/announcements/${id}`),
  updateStatus: (id, status) =>
    request.put(`/announcements/${id}/status`, null, { params: { status } }),
};

export const dashboardApi = {
  stats: () => request.get("/dashboard/stats"),
};

export const operationLogApi = {
  page: (params) => request.get("/logs", { params }),
};

export const batchAllocationApi = {
  importStudents: (file) => {
    const formData = new FormData();
    formData.append("file", file);
    return request.post("/batch-allocation/import", formData, {
      headers: { "Content-Type": "multipart/form-data" },
    });
  },
  downloadTemplate: () => {
    window.open("/api/batch-allocation/template", "_blank");
  },
  downloadTestData: () => {
    window.open("/api/batch-allocation/test-data", "_blank");
  },
  startAllocation: (batchNo) =>
    request.post("/batch-allocation/allocate", null, { params: { batchNo } }),
  confirmAllocation: (batchNo, studentIds) =>
    request.post("/batch-allocation/confirm", studentIds, {
      params: { batchNo },
    }),
  clearPending: (batchNo) =>
    request.post("/batch-allocation/clear", null, { params: { batchNo } }),
};
