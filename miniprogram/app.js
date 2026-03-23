// app.js - 寻找失踪老人小程序全局逻辑

App({
  globalData: {
    userInfo: null,      // 当前登录用户信息
    token: null,         // JWT 令牌
    baseUrl: 'http://38.207.179.218:8080/api',  // 云服务器公网地址
  },

  onLaunch() {
    // 从本地存储恢复登录状态
    const token = wx.getStorageSync('token');
    const userInfo = wx.getStorageSync('userInfo');

    if (token && userInfo) {
      this.globalData.token = token;
      this.globalData.userInfo = userInfo;
      console.log('已恢复登录状态，userId:', userInfo.userId);
    }

    // 检查微信登录态是否过期
    wx.checkSession({
      fail: () => {
        // session 过期，清除本地存储并跳转登录页
        this.clearLoginState();
      }
    });
  },

  /**
   * 检查是否已登录
   * @returns {boolean}
   */
  isLoggedIn() {
    return !!(this.globalData.token && this.globalData.userInfo);
  },

  /**
   * 保存登录信息到全局和本地存储
   */
  saveLoginState(loginData) {
    this.globalData.token = loginData.token;
    this.globalData.userInfo = loginData;
    wx.setStorageSync('token', loginData.token);
    wx.setStorageSync('userInfo', loginData);
  },

  /**
   * 清除登录状态（退出或过期）
   */
  clearLoginState() {
    this.globalData.token = null;
    this.globalData.userInfo = null;
    wx.removeStorageSync('token');
    wx.removeStorageSync('userInfo');
  },

  /**
   * 跳转到登录页
   */
  navigateToLogin() {
    wx.reLaunch({ url: '/pages/login/login' });
  }
});
