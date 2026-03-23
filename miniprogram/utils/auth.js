// utils/auth.js - 登录授权工具类

const { http } = require('./request');
const app = getApp();

/**
 * 微信登录流程：
 * 1. wx.login() 获取临时 code
 * 2. 调用后端 /user/login 接口换取 token
 * 3. 保存登录状态
 *
 * @param {Object} userProfile - 可选，微信用户信息（昵称/头像）
 * @returns {Promise<Object>} - 返回登录信息
 */
async function wxLogin(userProfile = null) {
  return new Promise((resolve, reject) => {
    wx.login({
      success: async (loginRes) => {
        if (!loginRes.code) {
          reject(new Error('获取微信登录code失败'));
          return;
        }

        try {
          const loginData = await http.post('/user/login', {
            code: loginRes.code,
            nickname: userProfile?.nickName || '',
            avatarUrl: userProfile?.avatarUrl || '',
          });

          // 保存登录状态
          app.saveLoginState(loginData);
          resolve(loginData);
        } catch (err) {
          reject(err);
        }
      },
      fail: (err) => {
        reject(new Error('微信登录失败：' + err.errMsg));
      }
    });
  });
}

/**
 * 检查登录状态，未登录则跳转登录页
 * @returns {boolean} - true 已登录
 */
function checkLogin() {
  if (!app.isLoggedIn()) {
    wx.showToast({
      title: '请先登录',
      icon: 'none',
      duration: 1500
    });
    setTimeout(() => app.navigateToLogin(), 1000);
    return false;
  }
  return true;
}

/**
 * 获取当前用户信息
 */
function getCurrentUser() {
  return app.globalData.userInfo;
}

/**
 * 退出登录
 */
function logout() {
  app.clearLoginState();
  app.navigateToLogin();
}

module.exports = { wxLogin, checkLogin, getCurrentUser, logout };
