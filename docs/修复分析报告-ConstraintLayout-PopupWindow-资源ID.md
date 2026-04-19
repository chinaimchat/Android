# 问题修复分析报告

**日期**: 2026-03-10  
**范围**: ConstraintLayout 弃用、No package ID 6b 资源错误、PopupWindow Hidden API 反射

---

## 一、修复概览

| 问题 | 状态 | 说明 |
|-----|------|------|
| 1. ConstraintLayout `layout_constraintWidth_default="wrap"` 弃用 | ✅ 已修复 | 2 处布局已改为 `wrap_content` + `layout_constrainedWidth="true"` |
| 2. No package ID 6b found for resource ID 0x6b0b0013 | ⚠️ 未在代码中定位 | 建议见下文「资源 ID 问题」 |
| 3. PopupWindow 反射访问 Hidden API `mOnScrollChangedListener` | ✅ 已修复 | 2 个类改为使用公开 API 自建滚动监听并 `update()` |

---

## 二、已完成的修改

### 2.1 ConstraintLayout 弃用（问题 1）

- **原因**: `layout_constraintWidth_default="wrap"` 为旧写法，新版本推荐用 `layout_width="WRAP_CONTENT"` + `layout_constrainedWidth="true"`。
- **修改文件**:
  - `wkuikit/src/main/res/layout/item_chat_conv_layout.xml`  
    - 控件: `nameTv`（会话列表项名称）
  - `wkuikit/src/main/res/layout/chat_title_layout.xml`  
    - 控件: `titleCenterTv`（标题栏居中文字）
- **具体改动**: 将上述 TextView 的 `layout_width="0dp"` + `layout_constraintWidth_default="wrap"` 改为 `layout_width="wrap_content"` + `layout_constrainedWidth="true"`，约束与链式布局保持不变。
- **效果**: 消除弃用告警，行为与原来「受约束的 wrap 宽度」一致。

### 2.2 PopupWindow Hidden API 反射（问题 3）

- **原因**: 通过反射访问 `PopupWindow.mOnScrollChangedListener` 在 Android 9+ 被限制，导致「hiddenapi: ... denied」。
- **思路**: 不再依赖系统内部监听器，改为在 **anchor 的 ViewTreeObserver** 上注册自己的 `OnScrollChangedListener`，在回调里调用 `PopupWindow.update(anchor, xoff, yoff, width, height)` 实现跟随滚动。
- **修改文件与要点**:

  1. **`wkbase/.../ActionBarPopupWindow.java`**
     - 删除对 `PopupWindow.class.getDeclaredField("mOnScrollChangedListener")` 的反射及 `Field` 使用。
     - 新增成员: `mScrollAnchor`、`mScrollXoff`、`mScrollYoff`，以及 `mOurScrollListener`（在回调中调用 `update(mScrollAnchor, mScrollXoff, mScrollYoff, getWidth(), getHeight())`）。
     - `showAsDropDown(anchor, xoff, yoff)` / `update(anchor, xoff, yoff, w, h)` / `update(anchor, w, h)` 中设置 anchor 与偏移并调用 `registerListener(anchor)`；`registerListener` 仅在 anchor 的 VTO 上添加/移除 `mOurScrollListener`。
     - `unregisterListener()` 中移除监听并清空 `mScrollAnchor`。
     - 移除 `@SuppressLint("SoonBlockedPrivateApi")` 及相关反射代码。

  2. **`wkbase/.../EmojiColorPickerWindow.java`**
     - 同样去掉对 `mOnScrollChangedListener` 的反射与静态 `Field`。
     - 采用与上面相同的「自建 mOurScrollListener + 记录 anchor/xoff/yoff + update()」实现。
     - 在 `showAsDropDown`、两个 `update` 重载中设置 anchor/偏移并调用 `registerListener`；`dismiss` 时调用 `unregisterListener()`。
     - 删除未使用的 `SuppressLint`、`Field` 引用。

- **效果**: 不再访问 Hidden API，弹窗在 anchor 滚动时仍能通过 `update()` 正确跟随位置，兼容新系统。

---

## 三、资源 ID 问题（问题 2）— 未在代码中定位

- **现象**: 运行时报错 `No package ID 6b found for resource ID 0x6b0b0013`。  
  Android 资源 ID 高字节为 package ID；当前 app 主包一般为 `0x7f`，`0x6b` 通常来自其他模块或依赖，说明某处在解析/使用一个「属于其他 package」的资源 ID。
- **已做排查**: 在工程内搜索 `0x6b0b0013`、`6b0b0013`、`package ID 6b` 及常见 `getIdentifier`/资源加载方式，**未在源码中找到直接引用**，因此无法通过改一两处引用完成修复。
- **可能来源**:
  - 依赖库或子模块在运行时用到了错误的资源 ID 或错误的 Context/Resources。
  - 多模块合并后资源 ID 分配与预期不一致（例如某库持有的是编译时另一包 ID 下的 ID）。
  - 动态布局/主题或插件化场景下使用了未正确注册的 package。
- **建议后续步骤**:
  1. **抓完整堆栈**: 在 Logcat 中对该错误或包含 “No package ID” 的异常打 breakpoint 或过滤，确认首次抛出时的调用栈，定位是哪个类、哪一行在解析 0x6b0b0013。
  2. **核对依赖与模块**: 检查 `app`、`wkbase`、`wkuikit` 等模块的 `build.gradle` 与 `applicationId`/`namespace`，确认没有重复或错误引用；查看是否有依赖带自己的资源且未正确 merge。
  3. **清洁构建**: 执行 `./gradlew clean` 后全量重新构建并再跑一遍，排除陈旧 R/资源缓存导致的 ID 错位。
  4. **缩小范围**: 若堆栈指向某个 Activity/Fragment 或具体功能，可暂时注释该入口或布局，确认是否与该资源 ID 强相关，再针对该界面或依赖做资源与包名检查。

当前报告中**未对问题 2 做代码级修改**，待能稳定复现并拿到堆栈后再做针对性修复。

---

## 四、涉及文件清单

| 文件 | 变更类型 |
|------|----------|
| `wkuikit/src/main/res/layout/item_chat_conv_layout.xml` | 布局：ConstraintLayout 弃用属性替换 |
| `wkuikit/src/main/res/layout/chat_title_layout.xml` | 布局：ConstraintLayout 弃用属性替换 |
| `wkbase/.../ActionBarPopupWindow.java` | 移除反射，改用公开 API 实现滚动跟随 |
| `wkbase/.../EmojiColorPickerWindow.java` | 同上 |

---

## 五、建议验证

1. **ConstraintLayout**: 打开会话列表与带标题栏的聊天/群聊界面，确认名称与标题文字显示正常、无截断或约束异常。
2. **PopupWindow**: 在列表或可滚动区域打开 ActionBar 弹窗、表情颜色选择器等，滚动页面时弹窗应随 anchor 移动，且不再出现 “hiddenapi … denied” 日志。
3. **资源 ID**: 若仍出现 “No package ID 6b” 错误，请按第三节步骤抓取堆栈并据此进一步排查。

---

*报告结束*
