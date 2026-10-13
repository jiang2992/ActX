const {createApp, ref, reactive, nextTick, computed, markRaw, watch} = Vue;
const md = markRaw(window.markdownit({
  html: true,      // 允许在 Markdown 中使用 HTML 标签
  linkify: true,   // 自动识别 URL 并转换为可点击的链接
}))

createApp({
  setup() {
    // 响应式状态
    const inputMessage = ref('');
    const currentSessionId = ref(null);
    const currentSessionTitle = ref('');  // 当前会话标题
    const sessions = ref([]);
    const features = ref([
      {id: 'business_discover', name: '业务认知'}
    ]);
    const activeFeature = ref(null);
    const messages = ref([]);
    const isStreaming = ref(false);
    const sidebarVisible = ref(false);
    const messagesAreaRef = ref(null);
    const inputRef = ref(null);
    const fileInputRef = ref(null);
    const sessionListRef = ref(null);
    const attachments = ref([]); // 存储待发送的附件列表 {id, name}
    const isComposing = ref(false); // IME输入法组合输入状态
    const openMenuSessionId = ref(null); // 当前展开更多菜单的会话ID
    const openFeatureMenuId = ref(null); // 当前展开更多菜单的功能ID
    const sessionMenuStyle = ref({}); // 会话更多菜单fixed定位样式
    const featureMenuStyle = ref({}); // 功能更多菜单fixed定位样式
    const sessionMenuRef = ref(null); // 会话更多菜单DOM引用
    const featureMenuRef = ref(null); // 功能更多菜单DOM引用
    let abortController = null;
    const stopRequested = ref(false);
    let userScrolledUp = false;
    let sessionScrollTimer = null;
    let taskOrderMap = {}; // task_order id -> 数据对象映射，用于快速状态更新
    let messageCounter = 0; // 消息唯一计数器，用于生成稳定_uid作为Vue key

    // 是否业务认知模式（该模式无会话列表，使用独立接口）
    const isBusinessDiscover = () => activeFeature.value === 'business_discover';

    // Toast 通知：屏幕顶部显示错误/成功提示，自动消失
    const toastMessage = ref('');
    const toastType = ref('error');
    let toastTimer = null;
    const showToast = (message, type = 'error', duration = 3000) => {
      toastMessage.value = message;
      toastType.value = type;
      clearTimeout(toastTimer);
      toastTimer = setTimeout(() => {
        toastMessage.value = '';
      }, duration);
    };

    // 统一网络请求封装：自动添加公共请求头，解析JSON响应，统一错误处理
    const request = async (url, options = {}) => {
      const fetchOptions = {
        headers: {
          'User-ID': '888',
          ...options.headers
        }
      };
      if (options.method) fetchOptions.method = options.method;
      if (options.body) fetchOptions.body = options.body;

      const response = await fetch(url, fetchOptions);

      if (!response.ok) {
        let errorMsg = `请求失败: ${response.status}`;
        try {
          const errorData = await response.json();
          if (errorData.message) errorMsg = errorData.message;
        } catch (e) {
          // 非JSON响应，使用状态码作为错误信息
        }
        throw new Error(errorMsg);
      }

      const result = await response.json();
      if (result.code !== 0) {
        throw new Error(result.message || '请求失败');
      }
      return result;
    };

    const loadSessions = async () => {
      try {
        const result = await request('/session/list');
        sessions.value = result.data;
      } catch (error) {
        console.error('加载会话列表失败:', error);
        showToast(error.message);
      }
    };

    const scrollToBottom = (force = false) => {
      if (!messagesAreaRef.value) {
        return;
      }

      const container = messagesAreaRef.value;

      // 如果强制滚动，直接滚动到底部
      if (force) {
        userScrolledUp = false;
        nextTick(() => {
          container.scrollTop = container.scrollHeight
        });
        return;
      }

      // 非强制滚动时，只有用户在底部才跟随滚动
      if (!userScrolledUp) {
        nextTick(() => {
          container.scrollTop = container.scrollHeight
        });
      }
    };

    // 通过taskOrderMap映射，用taskId定位任务单，再按code匹配子任务并更新状态
    // 映射查找失败时，兜底遍历所有消息中的task_order
    const updateSubTaskStatus = (taskId, code, status) => {
      const tryUpdate = (taskOrder) => {
        if (!taskOrder || !taskOrder.taskGroups) {
          return false;
        }
        for (const group of taskOrder.taskGroups) {
          if (group.subTasks) {
            for (const subTask of group.subTasks) {
              if (subTask.code === code) {
                console.log(`更新子任务状态: ${code} = ${status}`)
                // 使用Vue的响应式更新确保视图刷新
                subTask.status = status;
                return true;
              }
            }
          }
        }
        return false;
      };

      // 优先通过映射查找
      if (tryUpdate(taskOrderMap[taskId])) {
        // 强制触发响应式更新
        messages.value = [...messages.value];
        return;
      }

      // 兜底：遍历所有消息中的task_order
      for (const msg of messages.value) {
        for (const item of msg.items) {
          if (item.type === 'task_order' && (item.id === taskId || !taskId)) {
            if (tryUpdate(item)) {
              // 强制触发响应式更新
              messages.value = [...messages.value];
              return;
            }
          }
        }
      }
    };

    // 通过taskOrderMap映射直接查找任务单并更新状态
    const updateTaskOrderStatus = (id, status) => {
      const taskOrder = taskOrderMap[id];
      if (taskOrder) {
        taskOrder.status = status;
        // 强制触发响应式更新
        messages.value = [...messages.value];
        // 确保DOM更新
        nextTick(() => {
          // DOM更新后回调
        });
      }
    };

    // 强制更新消息列表
    const forceUpdateMessages = () => {
      messages.value = [...messages.value];
    };

    // 监听用户滚动事件
    const handleScroll = () => {
      if (!messagesAreaRef.value) {
        return;
      }
      const container = messagesAreaRef.value;
      const distanceToBottom = container.scrollHeight - container.scrollTop
          - container.clientHeight;
      // 如果距离底部超过10px，认为用户向上滚动了（降低阈值提高灵敏度）
      userScrolledUp = distanceToBottom > 10;
    };

    const getMarkdownContent = computed(() => {
      return (source) => {
        let html = md.render(source);
        // Wrap code blocks with header (language label + copy button)
        html = html.replace(
            /<pre><code class="language-(\w+)">([\s\S]*?)<\/code><\/pre>/g,
            (_, lang, code) =>
                '<div class="code-block-wrapper">'
                + '<div class="code-block-header"><span class="code-lang">'
                + lang
                + '</span><button class="copy-btn" onclick="window.copyCodeBlock(this)"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"></rect><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path></svg>复制</button></div>'
                + '<pre><code class="language-' + lang + '">' + code
                + '</code></pre></div>'
        );
        html = html.replace(
            /<pre><code>([\s\S]*?)<\/code><\/pre>/g,
            (_, code) =>
                '<div class="code-block-wrapper">'
                + '<div class="code-block-header"><span class="code-lang">code</span><button class="copy-btn" onclick="window.copyCodeBlock(this)"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"></rect><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path></svg>复制</button></div>'
                + '<pre><code>' + code + '</code></pre></div>'
        );
        return html;
      };
    });

    // 探索组折叠：将连续次要文本（thinking/tool_call）分组，默认折叠
    const expandedGroupKeys = reactive({});
    const isExploreGroupExpanded = (key) => !!expandedGroupKeys[key];
    const toggleExploreGroup = (key) => {
      if (expandedGroupKeys[key]) {
        delete expandedGroupKeys[key];
      } else {
        expandedGroupKeys[key] = true;
      }
    };
    const groupedMessages = computed(() => {
      return messages.value.map((msg, msgIndex) => {
        const groups = [];
        let exploreItems = [];
        const flushExplore = () => {
          if (exploreItems.length > 0) {
            const startIdx = msg.items.indexOf(exploreItems[0]);
            // 判断探索组是否是最后一个group：检查最后一个explore item是否在items末尾
            const isLast = msg.items.indexOf(exploreItems[exploreItems.length - 1]) === msg.items.length - 1;
            groups.push({
              type: 'explore_group',
              items: [...exploreItems],
              isStreaming: !msg.done && isLast,
              toolCallCount: exploreItems.filter(i => i.type === 'tool_call').length,
              thinkingCount: exploreItems.filter(i => i.type === 'thinking').length,
              key: `${msgIndex}-${startIdx}`
            });
            exploreItems = [];
          }
        };
        for (const item of msg.items) {
          if (item.type === 'thinking' || item.type === 'tool_call') {
            exploreItems.push(item);
          } else {
            flushExplore();
            groups.push({type: 'item', item: item});
          }
        }
        flushExplore();
        return {...msg, groups};
      });
    });

    // IME 输入法组合输入期间，回车不应发送消息
    const handleEnterKey = (event) => {
      if (isComposing.value) {
        return;
      } // 输入法候选状态下不发送
      sendMessage();
    };

    // textarea 自动变高，上限180px，超出显示滚动条
    const autoResizeTextarea = () => {
      nextTick(() => {
        const el = inputRef.value;
        if (!el) {
          return;
        }
        el.style.height = 'auto';
        el.style.height = Math.min(el.scrollHeight, 180) + 'px';
      });
    };

    // 事件处理
    const toggleSidebar = () => sidebarVisible.value = !sidebarVisible.value;

    const triggerFileUpload = () => {
      if (fileInputRef.value) {
        fileInputRef.value.click();
      }
    };

    const handleFileSelect = async (event) => {
      const files = event.target.files;
      if (!files || files.length === 0) {
        return;
      }

      for (const file of files) {
        try {
          // 上传文件
          const formData = new FormData();
          formData.append('file', file);

          const result = await request('/file', {
            method: 'POST',
            body: formData
          });
          const fileInfo = result.data;

          // 添加到附件列表
          attachments.value.push({
            id: fileInfo.id,
            name: fileInfo.fileName
          });

        } catch (error) {
          console.error('文件上传失败:', error);
          showToast(`文件 "${file.name}" 上传失败: ${error.message}`);
        }
      }

      // 清空input，允许重复选择同一文件
      event.target.value = '';
    };

    const removeAttachment = (index) => {
      attachments.value.splice(index, 1);
    };

    const handlePaste = async (event) => {
      const items = event.clipboardData?.items;
      if (!items) {
        return;
      }

      let hasFile = false;

      for (const item of items) {
        if (item.kind === 'file') {
          hasFile = true;
          const file = item.getAsFile();
          try {
            const formData = new FormData();
            formData.append('file', file);

            const result = await request('/file', {
              method: 'POST',
              body: formData
            });
            const fileInfo = result.data;

            attachments.value.push({
              id: fileInfo.id,
              name: fileInfo.fileName
            });

          } catch (error) {
            console.error('文件粘贴上传失败:', error);
            showToast(`文件 "${file.name}" 粘贴上传失败: ${error.message}`);
          }
        }
      }

      // 纯文本粘贴：手动插入到光标位置
      if (!hasFile) {
        const text = event.clipboardData.getData('text/plain');
        if (text) {
          const el = inputRef.value;
          if (el) {
            const start = el.selectionStart;
            const end = el.selectionEnd;
            const before = inputMessage.value.substring(0, start);
            const after = inputMessage.value.substring(end);
            inputMessage.value = before + text + after;
            nextTick(() => {
              el.selectionStart = el.selectionEnd = start + text.length;
              autoResizeTextarea();
            });
          }
        }
      }
    };

    const handleDrop = async (event) => {
      const files = event.dataTransfer?.files;
      if (!files || files.length === 0) {
        return;
      }

      for (const file of files) {
        try {
          const formData = new FormData();
          formData.append('file', file);

          const result = await request('/file', {
            method: 'POST',
            body: formData
          });
          const fileInfo = result.data;

          attachments.value.push({
            id: fileInfo.id,
            name: fileInfo.fileName
          });

        } catch (error) {
          console.error('文件拖放上传失败:', error);
          showToast(`文件 "${file.name}" 拖放上传失败: ${error.message}`);
        }
      }
    };

    const newChat = () => {
      activeFeature.value = null;
      currentSessionId.value = null;
      currentSessionTitle.value = '';
      messages.value = [];
      taskOrderMap = {};
      for (const key in expandedGroupKeys) delete expandedGroupKeys[key];
      nextTick(() => inputRef.value?.focus());
    };

    // 计算菜单fixed定位样式：右边缘对齐触发按钮，默认在按钮下方，空间不足时向上展开
    const positionMenu = (btn, menu) => {
      const gap = 4;
      const btnRect = btn.getBoundingClientRect();
      const menuHeight = menu.getBoundingClientRect().height;
      const right = `${window.innerWidth - btnRect.right}px`;
      // 下方空间不足且上方空间足够时，向上展开
      if (window.innerHeight - btnRect.bottom < menuHeight + gap
          && btnRect.top > menuHeight + gap) {
        return {
          position: 'fixed',
          right,
          bottom: `${window.innerHeight - btnRect.top + gap}px`
        };
      }
      return {
        position: 'fixed',
        right,
        top: `${btnRect.bottom + gap}px`
      };
    };

    // 展开/收起会话更多菜单
    const toggleSessionMenu = (id) => {
      openMenuSessionId.value = openMenuSessionId.value === id ? null : id;
      openFeatureMenuId.value = null;
      // 菜单挂载完成后根据按钮位置计算fixed定位
      nextTick(() => {
        if (!openMenuSessionId.value) {
          return;
        }
        const btn = document.querySelector(`[data-session-menu-btn="${openMenuSessionId.value}"]`);
        if (btn && sessionMenuRef.value) {
          sessionMenuStyle.value = positionMenu(btn, sessionMenuRef.value);
        }
      });
    };

    const deleteSession = async (id) => {
      if (!confirm('确认要删除该会话吗？删除后无法恢复。')) {
        return;
      }
      try {
        await request(`/session/${id}`, {method: 'DELETE'});
        // 从列表中移除该会话
        sessions.value = sessions.value.filter(s => s.sessionId !== id);
        // 如果删除的是当前会话，清空消息区域
        if (currentSessionId.value === id) {
          currentSessionId.value = null;
          currentSessionTitle.value = '';
          messages.value = [];
          taskOrderMap = {};
          for (const key in expandedGroupKeys) delete expandedGroupKeys[key];
        }
        openMenuSessionId.value = null;
      } catch (error) {
        console.error('删除会话失败:', error);
        showToast(error.message);
      }
    };

    // 展开/收起功能菜单（业务认知）
    const toggleFeatureMenu = (id) => {
      openFeatureMenuId.value = openFeatureMenuId.value === id ? null : id;
      openMenuSessionId.value = null;
      nextTick(() => {
        if (!openFeatureMenuId.value) {
          return;
        }
        const btn = document.querySelector(`[data-feature-menu-btn="${openFeatureMenuId.value}"]`);
        if (btn && featureMenuRef.value) {
          featureMenuStyle.value = positionMenu(btn, featureMenuRef.value);
        }
      });
    };

    // 点击其他区域时关闭所有更多菜单
    const closeMenus = () => {
      openMenuSessionId.value = null;
      openFeatureMenuId.value = null;
    };

    // 重置业务认知会话：清空会话上下文，下次发送消息时不传sessionId，由后端自动创建新会话
    const resetBusinessSession = () => {
      if (!confirm('重置会话仅清空会话上下文，不影响已经保存的业务知识，确认要重置吗')) {
        return;
      }
      currentSessionId.value = null;
      currentSessionTitle.value = '';
      messages.value = [];
      taskOrderMap = {};
      for (const key in expandedGroupKeys) delete expandedGroupKeys[key];
      openFeatureMenuId.value = null;
      nextTick(() => inputRef.value?.focus());
    };

    // 将后端会话消息转换为前端消息格式
    const convertSessionData = (sessionData) => {
      const convertedMessages = [];
      let currentUserMessage = null;

      if (sessionData.messages && Array.isArray(sessionData.messages)) {
        for (const msg of sessionData.messages) {

          if (msg.type === 'user') {
            // 用户消息
            currentUserMessage = {
              _uid: ++messageCounter, // 稳定唯一ID
              question: {
                content: msg.content,
                attachments: msg.attachments || []
              },
              items: [],
              done: true
            };
            convertedMessages.push(currentUserMessage);
          } else if (msg.type === 'answer' && msg.sections && Array.isArray(
              msg.sections)) {
            const items = [];
            for (const section of msg.sections) {
              // 根据 section 的 type 字段判断类型
              switch (section.type) {
                case 'tool_call':
                  items.push({
                    type: 'tool_call',
                    id: section.id,
                    name: section.name,
                    arguments: section.arguments,
                    result: section.result,
                    expanded: false
                  });
                  break;
                case 'thinking':
                  items.push({
                    type: 'thinking',
                    content: section.content
                  });
                  break;
                case 'answer':
                  items.push({
                    type: 'answer',
                    content: section.content
                  });
                  break;
                case 'error':
                  items.push({
                    type: 'error',
                    content: section.content
                  });
                  break;
                case 'tell':
                  items.push({
                    type: 'tell',
                    content: section.content
                  });
                  break;
                case 'stop':
                  items.push({
                    type: 'stop',
                    content: section.content
                  });
                  break;
                case 'sub_task_update':
                  updateSubTaskStatus(section.taskId, section.code,
                      section.status);
                  // 强制更新消息列表以确保状态变更立即显示
                  forceUpdateMessages();
                  break;
                case 'task_update':
                  updateTaskOrderStatus(section.id, section.status);
                  // 强制更新消息列表以确保状态变更立即显示
                  forceUpdateMessages();
                  break;
                case 'task_order':
                  // 为每个任务组添加expanded属性，默认展开
                  const taskGroups = section.taskGroups.map(group => ({
                    ...group,
                    expanded: false // 默认展开任务组
                  }));
                  items.push({
                    type: 'task_order',
                    id: section.id,
                    summary: section.summary,
                    taskGroups: taskGroups,
                    expanded: true // 默认展开任务单
                  });
                  taskOrderMap[section.id] = items[items.length - 1];
                  break;
              }
            }
            // 将助手回复添加到最后一个用户消息
            if (currentUserMessage) {
              currentUserMessage.items = items;
            }
          }
        }
      }
      return convertedMessages;
    };

    // 通用加载会话消息（普通会话与业务认知模式共用）
    const loadSessionMessages = async (url, id) => {
      try {
        const result = await request(`${url}?sessionId=${id}`);
        const sessionData = result.data;
        // 更新会话标题
        if (sessionData.name) {
          currentSessionTitle.value = sessionData.name;
        }
        taskOrderMap = {};
        for (const key in expandedGroupKeys) delete expandedGroupKeys[key];
        messages.value = convertSessionData(sessionData);
        scrollToBottom(true);
      } catch (error) {
        console.error('加载会话消息失败:', error);
        showToast(error.message);
      }
    };

    // 业务认知模式：无会话列表，点击菜单时获取最新单个会话
    const loadLatestBusinessSession = async () => {
      try {
        const result = await request('/business_discover/sessions/latest');
        const session = result.data;
        // 获取到最新会话则加载其消息，否则保持空会话（后续由后端session事件回传会话ID）
        if (session && session.sessionId) {
          currentSessionId.value = session.sessionId;
          // 更新会话标题
          if (session.name) {
            currentSessionTitle.value = session.name;
          }
          await loadSessionMessages('/business_discover/messages', session.sessionId);
        }
      } catch (error) {
        console.error('加载业务认知最新会话失败:', error);
        showToast(error.message);
      }
    };

    const switchFeature = async (id) => {
      activeFeature.value = id;
      currentSessionId.value = null;
      currentSessionTitle.value = '';
      messages.value = [];
      taskOrderMap = {};
      for (const key in expandedGroupKeys) delete expandedGroupKeys[key];
      // 业务认知模式：每次点击菜单时获取最新单个会话
      if (id === 'business_discover') {
        await loadLatestBusinessSession();
      }
      nextTick(() => inputRef.value?.focus());
    };

    const switchSession = async (id) => {
      activeFeature.value = null;
      currentSessionId.value = id;
      // 从会话列表中查找会话标题
      const session = sessions.value.find(s => s.sessionId === id);
      if (session && session.name) {
        currentSessionTitle.value = session.name;
      }
      await loadSessionMessages('/message/list', id);
    };

    // SSE 事件处理 - 流水线模式
    const handleSseEvent = (type, data) => {
      const lastMsg = messages.value[messages.value.length - 1];
      // 标记已收到SSE事件（用于隐藏点点点加载动画）
      lastMsg.hasReceivedResponse = true;
      const items = lastMsg.items;
      let lastItem = items.length > 0 ? items[items.length - 1] : null;
      try {
        switch (type) {
          case 'session':
            // 后端生成的会话信息：解析JSON获取sessionId和标题
            const sessionData = JSON.parse(data);
            const newSessionId = sessionData.sessionId;
            // 更新会话标题
            if (sessionData.name) {
              currentSessionTitle.value = sessionData.name;
            }
            if (!currentSessionId.value) {
              currentSessionId.value = newSessionId;
              // 业务认知模式无会话列表，仅更新当前会话ID
              if (!isBusinessDiscover()) {
                // 避免重复添加，使用后端返回的会话标题
                if (!sessions.value.some(s => s.sessionId === newSessionId)) {
                  sessions.value.unshift({
                    sessionId: newSessionId,
                    name: sessionData.name || '新对话'
                  });
                }
              }
            }
            break;
          case 'message':
            if (!lastItem || lastItem.type !== 'answer') {
              lastItem = {
                type: 'answer',
                content: ''
              };
              items.push(lastItem);
            }
            lastItem.content += data;
            break;
          case 'thinking':
            if (!lastItem || lastItem.type !== 'thinking') {
              lastItem = {
                type: 'thinking',
                content: ''
              };
              items.push(lastItem);
            }
            lastItem.content += data;
            break;
          case 'tell':
            if (!lastItem || lastItem.type !== 'tell') {
              lastItem = {
                type: 'tell',
                content: ''
              };
              items.push(lastItem);
            }
            lastItem.content += data;
            break;
          case 'tool_call':
            const toolCallData = JSON.parse(data);
            lastItem = {
              type: 'tool_call',
              id: toolCallData.id,
              name: toolCallData.name,
              arguments: toolCallData.arguments,
              expanded: false
            };
            items.push(lastItem);
            break;
          case 'tool_result':
            const toolResultData = JSON.parse(data);
            const toolItem = items.find(
                item => item.id === toolResultData.id);
            if (toolItem) {
              toolItem.result = toolResultData.result;
            }
            break;
          case 'error':
            if (!lastItem || lastItem.type !== 'error') {
              lastItem = {
                type: 'error',
                content: ''
              };
              items.push(lastItem);
            }
            lastItem.content += data;
            break;
          case 'stop':
            lastItem = {
              type: 'stop',
              content: data
            };
            items.push(lastItem);
            break;
          case 'done':
            lastMsg.done = true;
            break;
          case 'sub_task_update':
            const subTaskUpdateData = JSON.parse(data);
            updateSubTaskStatus(subTaskUpdateData.taskId,
                subTaskUpdateData.code, subTaskUpdateData.status);
            // 强制更新消息列表以确保状态变更立即显示
            forceUpdateMessages();
            break;
          case 'task_update':
            const taskUpdateData = JSON.parse(data);
            updateTaskOrderStatus(taskUpdateData.id, taskUpdateData.status);
            // 强制更新消息列表以确保状态变更立即显示
            forceUpdateMessages();
            break;
          case 'task_order':
            const taskOrderData = JSON.parse(data);
            // 为每个任务组添加expanded属性，默认展开
            const taskGroups = taskOrderData.taskGroups.map(group => ({
              ...group,
              expanded: false // 默认展开任务组
            }));
            lastItem = {
              type: 'task_order',
              id: taskOrderData.id,
              summary: taskOrderData.summary,
              taskGroups: taskGroups,
              expanded: true // 默认展开任务单
            };
            items.push(lastItem);
            taskOrderMap[taskOrderData.id] = lastItem;
            break;
        }
        // 强制更新消息数组引用，确保groupedMessages computed重新计算
        // （items.push修改了数组内容但computed可能未检测到，需要显式触发）
        forceUpdateMessages();
        // SSE流式输出时使用智能滚动，只在用户已在底部时才滚动
        scrollToBottom(false);
      } catch (e) {
        console.error('SSE事件处理错误:', e, type, data);
      }
    };

    const processSSEStream = async (reader) => {
      const decoder = new TextDecoder();
      let buffer = '';
      let eventType = null;
      let eventData = null;

      while (true) {
        const {done, value} = await reader.read();
        if (done) {
          break;
        }

        const chunk = decoder.decode(value, {stream: true});
        buffer += chunk;

        const lines = buffer.split('\n');
        buffer = lines.pop() || '';

        for (const line of lines) {
          // 空行表示消息结束（规范要求：连续两个换行符）
          if (line.trim() === '') {
            if (eventData !== null) {
              handleSseEvent(eventType || 'message', eventData);
              eventType = null;
              eventData = null;
            }
            continue;
          }

          if (line.startsWith('event:')) {
            eventType = line.slice(6).trim();
          } else if (line.startsWith('data:')) {
            const dataContent = line.slice(5); // 保留原样（不 trim，但规范要求移除前导空格？通常保留）
            if (eventData === null) {
              eventData = dataContent;
            } else {
              eventData += '\n' + dataContent;
            }
          }
          // 忽略其他字段如 id, retry 等
        }
      }

      // 处理最后一条没有收到空行的消息
      if (eventData !== null) {
        handleSseEvent(eventType || 'message', eventData);
      }
    };

    const handleSendClick = () => {
      if (isStreaming.value) {
        stopStreaming();
      } else {
        sendMessage();
      }
    };

    const stopStreaming = async () => {
      if (stopRequested.value) {
        return;
      }
      stopRequested.value = true;

      try {
        await fetch(isBusinessDiscover() ? '/business_discover/chat/stream' : '/chat/stream', {
          method: 'DELETE',
          headers: {
            'Content-Type': 'application/json',
            'User-ID': '888'
          },
          body: JSON.stringify({sessionId: currentSessionId.value})
        });
      } catch (e) {
        console.error('停止请求失败:', e);
      }
    };

    // 发送消息
    const sendMessage = async () => {
      const message = inputMessage.value.trim();
      if (!message || isStreaming.value) {
        return;
      }

      // 新会话时不在前端生成会话ID，直接请求，由后端生成并通过session事件回传

      // 添加用户问题到消息列表
      const lastMsg = {
        _uid: ++messageCounter, // 稳定唯一ID，防止流式输出时Vue key变化导致DOM重建
        question: {
          content: message,
          attachments: [...attachments.value] // 复制当前附件列表
        },
        items: [],
        done: false,
        sendError: false,         // 非SSE错误标记（请求失败时设为true）
        hasReceivedResponse: false // 是否已收到SSE事件（用于控制点点点显示）
      };
      messages.value.push(lastMsg);
      scrollToBottom(true);

      // 清空输入
      inputMessage.value = '';
      if (inputRef.value) {
        inputRef.value.style.height = 'auto';
      }

      isStreaming.value = true;
      stopRequested.value = false;
      abortController = new AbortController();

      try {
        // 构建请求体：没有会话ID时不传sessionId，由后端自动生成
        const requestBody = {
          message
        };
        if (currentSessionId.value) {
          requestBody.sessionId = currentSessionId.value;
        }

        // 如果有附件，添加attachments字段
        if (attachments.value.length > 0) {
          requestBody.attachments = attachments.value.map(att => att.id);
        }

        // 提交时立即清空附件列表（附件已保留在lastMsg.question.attachments中）
        attachments.value = [];

        const response = await fetch(isBusinessDiscover() ? '/business_discover/chat/stream' : '/chat/stream', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'User-ID': '888'
          },
          body: JSON.stringify(requestBody),
          signal: abortController.signal
        });

        if (!response.ok) {
          throw new Error(`请求失败: ${response.status}`);
        }

        // 检查是否返回了JSON错误响应（非SSE流）
        const contentType = response.headers.get('content-type') || '';
        if (contentType.includes('application/json')) {
          const errorData = await response.json();
          showToast(errorData.message || '请求失败');
          // 标记发送失败（保留消息，显示感叹号）
          lastMsg.sendError = true;
          return;
        }

        const reader = response.body.getReader();
        await processSSEStream(reader);

      } catch (error) {
        if (error.name !== 'AbortError') {
          // 标记发送失败（保留消息，显示感叹号）
          lastMsg.sendError = true;
          showToast(error.message);
        }
      } finally {
        isStreaming.value = false;
        abortController = null;
        lastMsg.done = true;
        scrollToBottom(false);
      }
    };

    // 会话列表滚动条：滚动时显示，停止后隐藏
    const bindSessionScroll = () => {
      if (sessionListRef.value) {
        sessionListRef.value.addEventListener('scroll', () => {
          sessionListRef.value.classList.add('scrolling');
          clearTimeout(sessionScrollTimer);
          sessionScrollTimer = setTimeout(() => {
            sessionListRef.value.classList.remove('scrolling');
          }, 600);
        });
      }
    };

    // 业务认知模式无会话列表（v-if移除DOM），切回普通会话时重新绑定滚动监听
    watch(activeFeature, (val) => {
      if (val !== 'business_discover') {
        nextTick(bindSessionScroll);
      }
    });

    // 在组件挂载后加载会话列表
    nextTick(() => {
      loadSessions();

      bindSessionScroll();

      // 点击页面其他区域时关闭更多菜单
      document.addEventListener('click', closeMenus);

      // 滚动或窗口变化时关闭更多菜单（fixed定位的菜单不跟随滚动）
      document.addEventListener('scroll', closeMenus, true);
      window.addEventListener('resize', closeMenus);

      // 添加全局调试函数
      window.debugMessages = () => {
        if (messages.value.length > 0) {
          const lastMsg = messages.value[messages.value.length - 1];
        }
      };
    });

    return {
      toastMessage,
      toastType,
      inputMessage,
      currentSessionId,
      currentSessionTitle,
      getMarkdownContent,
      groupedMessages,
      isExploreGroupExpanded,
      toggleExploreGroup,
      sessions,
      features,
      activeFeature,
      messages,
      isStreaming,
      stopRequested,
      sidebarVisible,
      messagesAreaRef,
      inputRef,
      fileInputRef,
      sessionListRef,
      attachments,
      isComposing,
      openMenuSessionId,
      openFeatureMenuId,
      sessionMenuStyle,
      featureMenuStyle,
      sessionMenuRef,
      featureMenuRef,
      toggleSidebar,
      newChat,
      switchFeature,
      switchSession,
      toggleSessionMenu,
      toggleFeatureMenu,
      deleteSession,
      resetBusinessSession,
      sendMessage,
      handleSendClick,
      stopStreaming,
      handleEnterKey,
      autoResizeTextarea,
      triggerFileUpload,
      handleFileSelect,
      removeAttachment,
      handlePaste,
      handleDrop,
      handleScroll,
    };
  }
}).mount('#app');

// 代码块复制功能
window.copyCodeBlock = function (button) {
  const wrapper = button.closest('.code-block-wrapper');
  if (!wrapper) {
    return;
  }
  const code = wrapper.querySelector('code');
  if (!code) {
    return;
  }
  const text = code.textContent;
  navigator.clipboard.writeText(text).then(() => {
    button.innerHTML = '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="20 6 9 17 4 12"></polyline></svg>已复制';
    button.classList.add('copied');
    setTimeout(() => {
      button.innerHTML = '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"></rect><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path></svg>复制';
      button.classList.remove('copied');
    }, 2000);
  }).catch(err => {
    console.error('复制失败:', err);
  });
};

// 监听并处理所有 a 标签的点击事件
// 获取当前页面的基础信息
const currentProtocol = window.location.protocol;
const currentHost = window.location.host;
const currentBaseUrl = `${currentProtocol}//${currentHost}`;

// 为文档添加事件委托监听器
document.addEventListener('click', function (event) {
  // 查找点击元素或其父元素中的 a 标签
  let targetElement = event.target;
  while (targetElement && targetElement.tagName !== 'A') {
    targetElement = targetElement.parentElement;
  }

  // 如果找到了 a 标签
  if (targetElement && targetElement.tagName === 'A') {
    event.preventDefault(); // 阻止默认跳转行为

    const href = targetElement.getAttribute('href');

    if (!href) {
      return; // 如果没有 href 属性，不做任何处理
    }

    try {
      // 判断是否为完整的 URL（包含协议）
      if (href.startsWith('http://') || href.startsWith('https://')) {
        // 完整链接，直接在新标签页打开
        window.open(href, '_blank');
      } else if (href.startsWith('#/file/')) {
        // 锚链接，且匹配 #/file/* 模式，下载文件
        const filePath = href.substring(7); // 移除 '#/file/' 前缀
        const downloadUrl = `${currentBaseUrl}/file/${filePath}`;

        // 使用隐藏的iframe来触发下载，避免页面跳转
        const iframe = document.createElement('iframe');
        iframe.style.display = 'none';
        iframe.src = downloadUrl;
        document.body.appendChild(iframe);

        // 设置定时器清理iframe
        setTimeout(() => {
          document.body.removeChild(iframe);
        }, 1000); // 延时以确保下载完成
      } else if (href.startsWith('#')) {
        // 其他类型的锚链接，暂时 TODO
        console.log('TODO: 处理其他类型的锚链接:', href);
      } else {
        // 不包含协议与 HOST 部分的相对链接，拼接当前页面的协议和 host
        let fullUrl;
        if (href.startsWith('/')) {
          // 以 / 开头的绝对路径
          fullUrl = `${currentBaseUrl}${href}`;
        } else {
          // 相对路径，基于当前页面路径处理（这里简化处理，直接拼接）
          fullUrl = `${currentBaseUrl}/${href}`;
        }
        window.open(fullUrl, '_blank');
      }
    } catch (error) {
      console.error('处理链接点击事件时出错:', error);
    }
  }
});
