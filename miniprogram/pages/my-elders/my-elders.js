// pages/my-elders/my-elders.js
const { http } = require('../../utils/request');
const { checkLogin } = require('../../utils/auth');
const { getElderEmoji } = require('../../utils/helper');

Page({
  data: {
    list: [],
    loading: false,
  },

  onLoad() {
    if (!checkLogin()) return;
  },

  onShow() {
    // 每次进入页面刷新列表（从编辑/添加页返回后更新）
    if (getApp().isLoggedIn()) {
      this.loadElderlyList();
    }
  },

  onPullDownRefresh() {
    this.loadElderlyList().finally(() => {
      wx.stopPullDownRefresh();
    });
  },

  async loadElderlyList() {
    this.setData({ loading: true });
    try {
      const rawList = await http.get('/elderly/my');
      const list = (rawList || []).map(item => ({
        ...item,
        emojiAvatar: getElderEmoji(item.gender, item.relation),
      }));
      this.setData({ list, loading: false });
    } catch (err) {
      this.setData({ loading: false });
    }
  },

  /** 点击老人卡片 */
  onElderTap(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({ url: `/pages/elder-detail/elder-detail?id=${id}` });
  },

  /** 添加新老人 */
  onAddElder() {
    wx.navigateTo({ url: '/pages/add-elder/add-elder' });
  },
});
