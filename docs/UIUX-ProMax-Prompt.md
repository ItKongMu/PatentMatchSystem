# 专利技术匹配系统 UI/UX 开发提示词

## 使用 UI-UX Pro Max Skill 的完整指南

---

## 1. 项目基础信息

- **项目名称**: PatentMatch - 基于大语言模型的专利技术匹配系统
- **项目类型**: SaaS Dashboard / Enterprise Tool / Research Platform
- **目标用户**: 专利分析师、研发人员、技术转移机构、知识产权从业者
- **技术栈**: Vue 3 + Element Plus + Vite + Pinia + ECharts

---

## 2. 设计系统生成命令

### Step 1: 生成完整设计系统（必须首先执行）

```bash
python3 skills/ui-ux-pro-max/scripts/search.py "enterprise SaaS dashboard data-intensive professional tech analytics" --design-system -p "PatentMatch System"
```

**备选关键词组合**（根据需要选择）:

```bash
# 偏向专业科技感
python3 skills/ui-ux-pro-max/scripts/search.py "tech enterprise dashboard analytics professional minimal" --design-system -p "PatentMatch System"

# 偏向知识产权/法律行业
python3 skills/ui-ux-pro-max/scripts/search.py "legal enterprise professional document management analytics" --design-system -p "PatentMatch System"

# 偏向研究/学术风格
python3 skills/ui-ux-pro-max/scripts/search.py "research platform academic data visualization professional" --design-system -p "PatentMatch System"
```

### Step 2: 持久化设计系统（推荐）

```bash
python3 skills/ui-ux-pro-max/scripts/search.py "enterprise SaaS dashboard data-intensive professional tech analytics" --design-system --persist -p "PatentMatch System"
```

### Step 3: 为各页面创建特定覆盖规则

```bash
# 首页/登录页
python3 skills/ui-ux-pro-max/scripts/search.py "login authentication minimal professional" --design-system --persist -p "PatentMatch System" --page "login"

# Dashboard 仪表盘
python3 skills/ui-ux-pro-max/scripts/search.py "dashboard analytics data visualization metrics" --design-system --persist -p "PatentMatch System" --page "dashboard"

# 专利上传页面
python3 skills/ui-ux-pro-max/scripts/search.py "file upload document management drag-drop progress" --design-system --persist -p "PatentMatch System" --page "patent-upload"

# 专利列表/检索页面
python3 skills/ui-ux-pro-max/scripts/search.py "data table search filter list view pagination" --design-system --persist -p "PatentMatch System" --page "patent-list"

# 专利详情页面
python3 skills/ui-ux-pro-max/scripts/search.py "detail view document reader content hierarchy sidebar" --design-system --persist -p "PatentMatch System" --page "patent-detail"

# 技术匹配页面
python3 skills/ui-ux-pro-max/scripts/search.py "search results comparison cards AI recommendation" --design-system --persist -p "PatentMatch System" --page "patent-match"

# 匹配历史页面
python3 skills/ui-ux-pro-max/scripts/search.py "history timeline records activity log" --design-system --persist -p "PatentMatch System" --page "match-history"
```

---

## 3. 补充详细搜索（按需执行）

### 3.1 图表类型搜索（用于匹配结果可视化）

```bash
# 相似度分布图
python3 skills/ui-ux-pro-max/scripts/search.py "comparison score distribution" --domain chart

# 领域分类饼图
python3 skills/ui-ux-pro-max/scripts/search.py "category distribution pie hierarchy" --domain chart

# 实体关系图
python3 skills/ui-ux-pro-max/scripts/search.py "network relationship connection" --domain chart

# 时间趋势图
python3 skills/ui-ux-pro-max/scripts/search.py "timeline trend history" --domain chart
```

### 3.2 UX 最佳实践搜索

```bash
# 表格交互
python3 skills/ui-ux-pro-max/scripts/search.py "table sorting filtering pagination" --domain ux

# 搜索体验
python3 skills/ui-ux-pro-max/scripts/search.py "search autocomplete suggestion" --domain ux

# 文件上传
python3 skills/ui-ux-pro-max/scripts/search.py "file upload progress validation" --domain ux

# 加载状态
python3 skills/ui-ux-pro-max/scripts/search.py "loading skeleton progress indicator" --domain ux

# 无障碍访问
python3 skills/ui-ux-pro-max/scripts/search.py "accessibility keyboard navigation" --domain ux

# 动画效果
python3 skills/ui-ux-pro-max/scripts/search.py "animation transition micro-interaction" --domain ux
```

### 3.3 样式风格搜索

```bash
# 专业企业风格
python3 skills/ui-ux-pro-max/scripts/search.py "professional enterprise clean minimal" --domain style

# 数据密集型界面
python3 skills/ui-ux-pro-max/scripts/search.py "data-dense dashboard analytics" --domain style

# 深色模式
python3 skills/ui-ux-pro-max/scripts/search.py "dark mode professional tech" --domain style
```

### 3.4 字体排版搜索

```bash
# 专业技术风格
python3 skills/ui-ux-pro-max/scripts/search.py "professional technical modern readable" --domain typography

# 中文友好字体
python3 skills/ui-ux-pro-max/scripts/search.py "multilingual CJK professional" --domain typography
```

### 3.5 颜色方案搜索

```bash
# SaaS 产品配色
python3 skills/ui-ux-pro-max/scripts/search.py "saas professional blue tech" --domain color

# 企业级配色
python3 skills/ui-ux-pro-max/scripts/search.py "enterprise corporate professional" --domain color
```

---

## 4. 技术栈指南获取

```bash
# Vue 3 最佳实践
python3 skills/ui-ux-pro-max/scripts/search.py "composition api state management" --stack vue

# HTML + Tailwind（如果使用）
python3 skills/ui-ux-pro-max/scripts/search.py "responsive layout grid flex" --stack html-tailwind
```

---

## 5. 项目特定的 UI/UX 需求

### 5.1 核心页面结构

| 页面 | 主要功能 | UI 重点 |
|------|----------|---------|
| **登录/注册** | 用户认证 | 简洁、专业、可信感 |
| **Dashboard** | 数据概览 | 卡片式布局、关键指标展示 |
| **专利上传** | PDF上传、文本录入 | 拖拽上传、进度反馈、状态追踪 |
| **专利列表** | 分页、搜索、筛选 | 表格、高级筛选器、快速操作 |
| **专利详情** | 查看专利信息、实体、领域 | 层次化展示、标签、侧边栏 |
| **技术匹配** | 输入查询、查看结果 | 搜索框、结果卡片、相似度可视化 |
| **匹配历史** | 查看历史记录 | 时间线、列表、详情弹窗 |

### 5.2 关键交互场景

| 场景 | UX 要求 |
|------|---------|
| **PDF上传** | 拖拽支持、类型校验、进度条、错误提示 |
| **专利解析进度** | 实时状态更新（PENDING→PARSING→EXTRACTING→VECTORIZING→SUCCESS） |
| **技术匹配搜索** | 输入建议、实体高亮、加载动画 |
| **匹配结果展示** | 相似度分数、匹配原因、实体对比 |
| **领域层次展示** | 树形结构、可展开折叠 |

### 5.3 数据可视化需求

| 图表类型 | 用途 | 推荐库 |
|----------|------|--------|
| **进度条/步骤条** | 专利解析状态 | Element Plus Steps |
| **饼图/环形图** | 领域分布、实体类型分布 | ECharts |
| **柱状图** | 匹配分数对比 | ECharts |
| **标签云** | 关键词展示 | ECharts WordCloud |
| **关系图** | 实体关系可视化（未来） | ECharts Graph |

---

## 6. 推荐的设计规范

### 6.1 色彩建议

```css
/* 主色调 - 专业科技蓝 */
--primary: #2563EB;        /* 主按钮、链接 */
--primary-light: #3B82F6;  /* 悬停状态 */
--primary-dark: #1D4ED8;   /* 按下状态 */

/* 语义色 */
--success: #10B981;        /* 成功状态、匹配度高 */
--warning: #F59E0B;        /* 警告、处理中 */
--danger: #EF4444;         /* 错误、匹配度低 */
--info: #6366F1;           /* 信息提示 */

/* 中性色 */
--bg-primary: #FFFFFF;     /* 主背景 */
--bg-secondary: #F8FAFC;   /* 次级背景 */
--text-primary: #0F172A;   /* 主文字 */
--text-secondary: #64748B; /* 次级文字 */
--border: #E2E8F0;         /* 边框 */

/* 深色模式 */
--dark-bg: #0F172A;
--dark-surface: #1E293B;
--dark-text: #F1F5F9;
```

### 6.2 字体建议

```css
/* 主字体 - 支持中文 */
font-family: 'Inter', 'PingFang SC', 'Microsoft YaHei', sans-serif;

/* 代码/专利号字体 */
font-family: 'JetBrains Mono', 'Fira Code', monospace;
```

### 6.3 间距规范

```css
/* 基础间距单位: 4px */
--space-1: 4px;
--space-2: 8px;
--space-3: 12px;
--space-4: 16px;
--space-5: 20px;
--space-6: 24px;
--space-8: 32px;
--space-10: 40px;
--space-12: 48px;
```

### 6.4 圆角规范

```css
--radius-sm: 4px;    /* 小元素（标签、徽章） */
--radius-md: 8px;    /* 按钮、输入框 */
--radius-lg: 12px;   /* 卡片 */
--radius-xl: 16px;   /* 大型容器 */
```

---

## 7. 组件设计要点

### 7.1 专利卡片组件

```
┌─────────────────────────────────────────────────┐
│ ┌──────────────────────────────────────────────┐│
│ │ CN123456789A              [状态徽章: 已完成] ││
│ │ 一种基于深度学习的医疗图像诊断方法           ││
│ ├──────────────────────────────────────────────┤│
│ │ 申请人: 某医疗科技公司                       ││
│ │ 申请日: 2024-01-15                           ││
│ ├──────────────────────────────────────────────┤│
│ │ 领域: [G06T] [G06N] [A61B]                   ││
│ │ 实体: 深度学习 · CNN · CT图像 · 病变检测    ││
│ ├──────────────────────────────────────────────┤│
│ │ [查看详情]  [发起匹配]  [下载PDF]            ││
│ └──────────────────────────────────────────────┘│
└─────────────────────────────────────────────────┘
```

### 7.2 匹配结果卡片

```
┌─────────────────────────────────────────────────┐
│ 相似度: ████████████░░░░ 87.5%                  │
├─────────────────────────────────────────────────┤
│ CN987654321B                                    │
│ 基于卷积神经网络的X光影像分析系统              │
├─────────────────────────────────────────────────┤
│ 匹配原因:                                       │
│ • 核心技术相同：均使用深度学习进行医学影像分析 │
│ • 实体匹配：深度学习(✓) CNN(✓) 医学影像(✓)    │
│ • 领域匹配：G06T图像处理                        │
├─────────────────────────────────────────────────┤
│ [查看详情]  [对比分析]                          │
└─────────────────────────────────────────────────┘
```

### 7.3 解析状态指示器

```
PENDING ──○── PARSING ──○── EXTRACTING ──●── VECTORIZING ──○── SUCCESS
              ▲
              └── 当前状态: 正在提取实体和领域信息...
```

---

## 8. 前端开发检查清单

### 8.1 视觉质量
- [ ] 不使用 emoji 作为图标（使用 Heroicons/Element Plus 图标）
- [ ] 所有图标来自统一图标库
- [ ] 悬停状态不会导致布局偏移
- [ ] 使用主题色变量而非硬编码颜色

### 8.2 交互体验
- [ ] 所有可点击元素添加 `cursor-pointer`
- [ ] 悬停状态有清晰视觉反馈
- [ ] 过渡动画平滑 (150-300ms)
- [ ] 键盘导航可用（focus 状态可见）

### 8.3 亮色/暗色模式
- [ ] 亮色模式文字对比度足够 (4.5:1)
- [ ] 玻璃/透明元素在亮色模式下可见
- [ ] 边框在两种模式下都可见
- [ ] 交付前两种模式都已测试

### 8.4 布局
- [ ] 浮动元素有适当边距
- [ ] 内容不被固定导航栏遮挡
- [ ] 响应式适配: 375px, 768px, 1024px, 1440px
- [ ] 移动端无水平滚动

### 8.5 无障碍
- [ ] 所有图片有 alt 文本
- [ ] 表单输入有 label
- [ ] 颜色不是唯一的状态指示
- [ ] 尊重 `prefers-reduced-motion`

---

## 9. 示例工作流

### 场景：开发专利匹配结果页面

**Step 1: 生成设计系统**
```bash
python3 skills/ui-ux-pro-max/scripts/search.py "search results comparison cards AI recommendation" --design-system --persist -p "PatentMatch System" --page "patent-match"
```

**Step 2: 获取图表建议**
```bash
python3 skills/ui-ux-pro-max/scripts/search.py "comparison score bar horizontal" --domain chart
```

**Step 3: 获取 UX 指南**
```bash
python3 skills/ui-ux-pro-max/scripts/search.py "search results loading infinite scroll" --domain ux
```

**Step 4: 获取 Vue 实现建议**
```bash
python3 skills/ui-ux-pro-max/scripts/search.py "list rendering virtual scroll" --stack vue
```

**Step 5: 根据设计系统实现页面**

---

## 10. 常见问题解决

| 问题 | 搜索命令 |
|------|----------|
| 表格数据太密集 | `python3 scripts/search.py "data-dense table readability" --domain ux` |
| 加载时间长 | `python3 scripts/search.py "skeleton loading placeholder" --domain ux` |
| 移动端适配 | `python3 scripts/search.py "responsive mobile-first" --stack html-tailwind` |
| 颜色对比度不足 | `python3 scripts/search.py "accessibility contrast" --domain ux` |
| 状态反馈不明显 | `python3 scripts/search.py "feedback toast notification" --domain ux` |

---

**文档版本**: v1.0  
**创建日期**: 2026年2月3日  
**适用项目**: PatentMatch - 基于大语言模型的专利技术匹配系统
