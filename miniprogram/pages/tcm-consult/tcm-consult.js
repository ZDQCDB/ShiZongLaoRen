// pages/tcm-consult/tcm-consult.js
const { http } = require('../../utils/request');
const { checkLogin } = require('../../utils/auth');

// 常见症状快捷标签（老人 / 小孩分开）
const QUICK_TAGS = {
  elder: ['失眠多梦', '腰膝酸软', '气短乏力', '消化不良', '关节疼痛', '头晕耳鸣', '便秘', '高血压辅助'],
  child: ['反复咳嗽', '积食厌食', '夜间哭闹', '发热', '腹泻', '盗汗', '鼻塞流涕', '免疫力低下']
};

Page({
  data: {
    userType: 'elder',         // elder / child
    messages: [],              // 聊天消息列表
    inputValue: '',            // 输入框内容
    loading: false,            // AI 思考中
    quickTags: QUICK_TAGS.elder,
    showHistory: false,        // 是否显示历史面板
    historyList: [],
    scrollTop: 9999,           // 控制聊天区自动滚到底部
  },

  onLoad() {
    if (!checkLogin()) return;
    // 展示欢迎语
    this.addAiMessage('您好！我是中医健康助手 🌿\n\n请选择适用人群（老人/小孩），然后描述症状，我将为您提供中医调理建议。\n\n⚠️ 建议仅供科普参考，症状严重请及时就医。');
  },

  /** 切换老人 / 小孩 */
  onSwitchUserType(e) {
    const type = e.currentTarget.dataset.type;
    if (type === this.data.userType) return;
    this.setData({
      userType: type,
      quickTags: QUICK_TAGS[type]
    });
  },

  /** 点击快捷标签 */
  onTapTag(e) {
    const tag = e.currentTarget.dataset.tag;
    this.setData({ inputValue: tag });
  },

  /** 输入框变化 */
  onInput(e) {
    this.setData({ inputValue: e.detail.value });
  },

  /** 发送问诊请求（流式输出，逐字显示） */
  onSend() {
    const symptom = this.data.inputValue.trim();
    if (!symptom) {
      wx.showToast({ title: '请描述症状', icon: 'none' });
      return;
    }
    if (this.data.loading) return;

    const app = getApp();
    this.addUserMessage(symptom);
    this.setData({ inputValue: '', loading: true });

    // 添加空的 AI 气泡，后续逐字填入
    const aiMsgId = Date.now();
    this.addAiMessage('', aiMsgId);

    let buffer = '';   // 用于缓存跨 chunk 的不完整 SSE 行
    const self = this;

    const task = wx.request({
      url: app.globalData.baseUrl + '/tcm/stream',
      method: 'POST',
      data: JSON.stringify({
        userType: this.data.userType,
        symptom: symptom
      }),
      header: {
        'Content-Type': 'application/json',
        'Authorization': app.globalData.token ? ('Bearer ' + app.globalData.token) : ''
      },
      enableChunked: true,   // 关键：开启分块接收
      success() {
        self.setData({ loading: false });
      },
      fail() {
        self.appendToMessage(aiMsgId, '\n\n⚠️ 网络连接失败，请检查网络后重试');
        self.setData({ loading: false });
      }
    });

    // 每次收到新的数据块时触发
    task.onChunkReceived((res) => {
      try {
        // 将 ArrayBuffer 解码为字符串
        const text = self.decodeBuffer(res.data);
        buffer += text;

        // 按换行切分，最后一段可能不完整，留在 buffer 里
        const lines = buffer.split('\n');
        buffer = lines.pop();

        for (const line of lines) {
          const trimmed = line.trim();
          if (!trimmed.startsWith('data:')) continue;

          const data = trimmed.slice(5).trim();

          if (data === '[DONE]') {
            self.setData({ loading: false });
            return;
          }

          // 服务端发送的就是内容片段，直接拼接显示
          if (data && data !== '') {
            self.appendToMessage(aiMsgId, data);
          }
        }
      } catch (e) {
        console.error('解析流式数据失败', e);
      }
    });
  },

  /** 解码 ArrayBuffer 为 UTF-8 字符串 */
  decodeBuffer(buffer) {
    try {
      // TextDecoder 在微信基础库 2.19.3+ 可用
      return new TextDecoder('utf-8').decode(new Uint8Array(buffer));
    } catch (e) {
      // 降级兼容方案
      const bytes = new Uint8Array(buffer);
      let str = '';
      for (let i = 0; i < bytes.length; i++) {
        str += String.fromCharCode(bytes[i]);
      }
      try { return decodeURIComponent(escape(str)); } catch (_) { return str; }
    }
  },

  /** 向指定 id 的消息末尾追加内容（流式逐字拼接） */
  appendToMessage(id, text) {
    const messages = this.data.messages.map(m =>
      m.id === id ? { ...m, text: m.text + text } : m
    );
    this.setData({ messages, scrollTop: messages.length * 9999 });
  },

  /** 添加用户消息到列表 */
  addUserMessage(text) {
    const messages = this.data.messages;
    messages.push({ id: Date.now(), role: 'user', text });
    this.setData({ messages, scrollTop: messages.length * 9999 });
  },

  /** 添加 AI 消息到列表 */
  addAiMessage(text, id = Date.now()) {
    const messages = this.data.messages;
    messages.push({ id, role: 'ai', text });
    this.setData({ messages, scrollTop: messages.length * 9999 });
  },

  /** 替换指定 id 的消息内容 */
  replaceMessage(id, text) {
    const messages = this.data.messages.map(m =>
      m.id === id ? { ...m, text } : m
    );
    this.setData({ messages, scrollTop: messages.length * 9999 });
  },

  /** 查看历史记录 */
  async onShowHistory() {
    try {
      wx.showLoading({ title: '加载中...', mask: true });
      const list = await http.get('/tcm/history');
      wx.hideLoading();
      this.setData({ historyList: list || [], showHistory: true });
    } catch (e) {
      wx.hideLoading();
    }
  },

  /** 关闭历史面板 */
  onHideHistory() {
    this.setData({ showHistory: false });
  },

  /** 点击历史记录，填入当次问诊内容到聊天 */
  onTapHistory(e) {
    const item = e.currentTarget.dataset.item;
    this.setData({ showHistory: false });
    this.addUserMessage(item.symptom);
    this.addAiMessage(item.advice);
  },

  /** 清空对话 */
  onClearChat() {
    wx.showModal({
      title: '清空对话',
      content: '确定要清空当前对话吗？',
      success: (r) => {
        if (r.confirm) {
          this.setData({ messages: [] });
          this.addAiMessage('对话已清空 🌿\n\n请重新描述症状，我为您提供中医建议。');
        }
      }
    });
  }
});
