// pages/login/login.js
const { wxLogin } = require('../../utils/auth');

Page({
  data: {
    loading: false,
  },

  onLoad() {
    const app = getApp();
    // 如果已登录，直接跳到首页
    if (app.isLoggedIn()) {
      wx.switchTab({ url: '/pages/index/index' });
    }
  },

  /**
   * 获取用户信息后执行登录
   */
  async onGetUserInfo(e) {
    if (this.data.loading) return;

    // 微信新版本不再返回用户详细信息，这里兼容处理
    const userProfile = e.detail?.userInfo || null;

    this.setData({ loading: true });
    wx.showLoading({ title: '登录中...', mask: true });

    try {
      await wxLogin(userProfile);
      wx.hideLoading();
      wx.showToast({ title: '登录成功', icon: 'success', duration: 1200 });
      setTimeout(() => {
        wx.switchTab({ url: '/pages/index/index' });
      }, 1000);
    } catch (err) {
      wx.hideLoading();
      wx.showToast({ title: err.message || '登录失败，请重试', icon: 'none' });
    } finally {
      this.setData({ loading: false });
    }
  },

  /**
   * 也可直接跳过登录使用人脸识别（匿名）
   */
  onSkipToSearch() {
    wx.navigateTo({ url: '/pages/face-search/face-search' });
  },

  onTapAgreement() {
    wx.navigateTo({ url: '/pages/user-agreement/user-agreement' });
  },

  onTapPrivacy() {
    wx.navigateTo({ url: '/pages/privacy-policy/privacy-policy' });
  }
});
