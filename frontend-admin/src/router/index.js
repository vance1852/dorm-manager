import { createRouter, createWebHistory } from "vue-router";

const routes = [
  {
    path: "/login",
    name: "Login",
    component: () => import("../views/Login.vue"),
  },
  {
    path: "/",
    component: () => import("../views/Layout.vue"),
    redirect: "/dashboard",
    children: [
      {
        path: "dashboard",
        name: "Dashboard",
        component: () => import("../views/Dashboard.vue"),
        meta: { title: "首页" },
      },
      {
        path: "users",
        name: "Users",
        component: () => import("../views/Users.vue"),
        meta: { title: "用户管理", roles: [1] },
      },
      {
        path: "buildings",
        name: "Buildings",
        component: () => import("../views/Buildings.vue"),
        meta: { title: "楼栋管理", roles: [1, 2] },
      },
      {
        path: "rooms",
        name: "Rooms",
        component: () => import("../views/Rooms.vue"),
        meta: { title: "房间管理", roles: [1, 2] },
      },
      {
        path: "students",
        name: "Students",
        component: () => import("../views/Students.vue"),
        meta: { title: "学生管理", roles: [1, 2] },
      },
      {
        path: "batch-allocation",
        name: "BatchAllocation",
        component: () => import("../views/BatchAllocation.vue"),
        meta: { title: "批量分配", roles: [1, 2] },
      },
      {
        path: "repairs",
        name: "Repairs",
        component: () => import("../views/Repairs.vue"),
        meta: { title: "维修管理" },
      },
      {
        path: "visitors",
        name: "Visitors",
        component: () => import("../views/Visitors.vue"),
        meta: { title: "访客管理" },
      },
      {
        path: "announcements",
        name: "Announcements",
        component: () => import("../views/Announcements.vue"),
        meta: { title: "公告管理" },
      },
      {
        path: "logs",
        name: "OperationLogs",
        component: () => import("../views/OperationLogs.vue"),
        meta: { title: "操作日志", roles: [1] },
      },
    ],
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem("token");
  if (to.path !== "/login" && !token) {
    next("/login");
  } else {
    next();
  }
});

export default router;
