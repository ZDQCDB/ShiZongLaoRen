// pages/index/index.js
const { http } = require('../../utils/request');

Page({
  data: {
    greeting: '',
    nickName: '朋友',
    avatarUrl: '',
    elderlyCount: 0,
    safeCount: 0,
    faceCount: 0,
  },

  onLoad() {
    this.setGreeting();
  },

  onShow() {
    const app = getApp();
    if (app.isLoggedIn()) {
      const user = app.globalData.userInfo;
      this.setData({
        nickName: user.nickname || '朋友',
        avatarUrl: user.avatarUrl || '',
      });
      this.loadMyElderlyStats();
    }
  },

  /** 根据时间设置问候语 */
  setGreeting() {
    const hour = new Date().getHours();
    let greeting = '你好';
    if (hour >= 6  && hour < 12) greeting = '早上好';
    else if (hour >= 12 && hour < 14) greeting = '中午好';
    else if (hour >= 14 && hour < 18) greeting = '下午好';
    else if (hour >= 18 && hour < 22) greeting = '晚上好';
    else greeting = '夜深了';
    this.setData({ greeting });
  },

  /** 加载老人统计数量 */
  async loadMyElderlyStats() {
    try {
      const list = await http.get('/elderly/my');
      const totalCount = list ? list.length : 0;
      const faceCount = list ? list.filter(e => e.photoUrl).length : 0;
      this.setData({
        elderlyCount: totalCount,
        safeCount: totalCount,   // 均视为信息完整
        faceCount,
      });
    } catch (e) {
      // 静默失败，不影响首页展示
    }
  },

  /** 跳转人脸识别（tabBar页面必须用 switchTab） */
  onFaceSearchTap() {
    wx.switchTab({ url: '/pages/face-search/face-search' });
  },

  /** 跳转添加老人 */
  onAddElderTap() {
    const app = getApp();
    if (!app.isLoggedIn()) {
      wx.showModal({
        title: '需要登录',
        content: '添加老人信息需要先登录，是否前往登录？',
        confirmText: '去登录',
        success: (res) => {
          if (res.confirm) {
            wx.navigateTo({ url: '/pages/login/login' });
          }
        }
      });
      return;
    }
    wx.navigateTo({ url: '/pages/add-elder/add-elder' });
  },

  /** 跳转我的老人列表 */
  onMyEldersTap() {
    const app = getApp();
    if (!app.isLoggedIn()) {
      wx.showToast({ title: '请先登录', icon: 'none' });
      setTimeout(() => wx.navigateTo({ url: '/pages/login/login' }), 1000);
      return;
    }
    wx.switchTab({ url: '/pages/my-elders/my-elders' });
  },

  /** 用户头像点击 */
  onProfileTap() {
    const app = getApp();
    if (!app.isLoggedIn()) {
      wx.navigateTo({ url: '/pages/login/login' });
    } else {
      wx.showActionSheet({
        itemList: ['退出登录'],
        success: (res) => {
          if (res.tapIndex === 0) {
            wx.showModal({
              title: '退出登录',
              content: '确定要退出登录吗？',
              success: (r) => {
                if (r.confirm) {
                  app.clearLoginState();
                  this.setData({ nickName: '朋友', avatarUrl: '', elderlyCount: 0, faceCount: 0 });
                  wx.showToast({ title: '已退出登录', icon: 'success' });
                }
              }
            });
          }
        }
      });
    }
  },

  /** 使用帮助 */
  onHelpTap() {
    wx.showModal({
      title: '使用说明',
      content: '1. 家属登录后添加老人信息和照片\n2. 遇到走失老人时，拍照或从相册选图\n3. 系统会自动识别并展示老人的家属联系方式\n4. 无需登录即可使用人脸识别功能',
      showCancel: false,
      confirmText: '明白了',
    });
  },
});
