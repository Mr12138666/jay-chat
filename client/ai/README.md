# AI 前端独立访问说明

## 独立访问地址

AI 前端已配置为独立运行，使用端口 **5174**，与主前端（默认 5173）互不干扰。

## 启动方式

### 方式一：在 AI 目录下直接启动

```bash
cd client/ai
npm install  # 首次运行需要安装依赖
npm run dev
```

### 方式二：从项目根目录启动

```bash
cd client/ai
npm run dev
```

## 访问地址

启动后访问：**http://localhost:5174**

## 同时运行两个前端

可以同时运行主前端和 AI 前端：

1. **终端 1** - 启动主前端（聊天室）：
   ```bash
   cd client
   npm run dev
   ```
   访问：http://localhost:5173

2. **终端 2** - 启动 AI 前端：
   ```bash
   cd client/ai
   npm run dev
   ```
   访问：http://localhost:5174

## 后端 API 配置

当前 AI 前端直接调用后端 API：`http://localhost:8080/v6/ai/generateStream`

如需修改 API 地址，可以：
1. 修改 `src/views/Index.vue` 中的 API 地址
2. 或使用环境变量（需要配置 Vite 环境变量）

## 构建生产版本

```bash
npm run build
```

构建产物在 `dist` 目录。
