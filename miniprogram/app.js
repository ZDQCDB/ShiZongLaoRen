// app.js - 寻找失踪老人小程序全局逻辑

App({
  globalData: {
    userInfo: null,      // 当前登录用户信息
    token: null,         // JWT 令牌
    baseUrl: 'http://38.207.179.218:8080/api',  // 云服务器公网地址
    silentLoginPromise: null,  // 静默登录的 Promise，防止并发重复调用
  },

  onLaunch() {
    // 从本地存储恢复登录状态（token 存本地，用户永不需要重新登录）
    const token = wx.getStorageSync('token');
    const userInfo = wx.getStorageSync('userInfo');

    if (token && userInfo) {
      this.globalData.token = token;
      this.globalData.userInfo = userInfo;
      console.log('已恢复登录状态，userId:', userInfo.userId);

      // 后台静默刷新 token（不阻塞启动，用户无感知）
      // wx.checkSession 只检查微信session，与我们的JWT是两套机制
      // session 过期时仅在后台刷新 token，不清除登录状态
      wx.checkSession({
        fail: () => {
          console.log('微信session已过期，后台静默刷新token...');
          this.silentRefreshToken();
        }
      });
    }
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
  },

  /**
   * 后台静默刷新 token（用户无感知，不跳转登录页）
   * 利用 wx.login() 无需用户操作即可获取新 code，自动换取新 JWT
   * @returns {Promise<boolean>} - true=刷新成功，false=失败需手动登录
   */
  silentRefreshToken() {
    // 防止并发多次调用
    if (this.globalData.silentLoginPromise) {
      return this.globalData.silentLoginPromise;
    }

    this.globalData.silentLoginPromise = new Promise((resolve) => {
      wx.login({
        success: (res) => {
          if (!res.code) {
            resolve(false);
            return;
          }
          const { http } = require('./utils/request');
          const savedUserInfo = wx.getStorageSync('userInfo');
          http.post('/user/login', {
            code: res.code,
            nickname: savedUserInfo?.nickname || '',
            avatarUrl: savedUserInfo?.avatarUrl || '',
          }).then((loginData) => {
            this.saveLoginState(loginData);
            console.log('静默刷新token成功');
            resolve(true);
          }).catch(() => {
            console.log('静默刷新token失败');
            resolve(false);
          }).finally(() => {
            this.globalData.silentLoginPromise = null;
          });
        },
        fail: () => {
          this.globalData.silentLoginPromise = null;
          resolve(false);
        }
      });
    });

    return this.globalData.silentLoginPromise;
  }
});
