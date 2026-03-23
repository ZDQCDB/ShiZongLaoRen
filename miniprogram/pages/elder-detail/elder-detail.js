// pages/elder-detail/elder-detail.js
const { http } = require('../../utils/request');
const { getElderEmoji } = require('../../utils/helper');

Page({
  data: {
    elderly: null,
    loading: true,
    isOwner: false,   // 是否是录入人（可编辑删除）
    elderlyId: null,
  },

  onLoad(options) {
    const id = options.id;
    if (!id) {
      wx.showToast({ title: '参数错误', icon: 'none' });
      wx.navigateBack();
      return;
    }
    this.setData({ elderlyId: id });
    this.loadElderlyDetail(id);
  },

  onShow() {
    // 从编辑页返回时刷新数据
    if (this.data.elderlyId) {
      this.loadElderlyDetail(this.data.elderlyId);
    }
  },

  async loadElderlyDetail(id) {
    this.setData({ loading: true });
    try {
      const elderly = await http.get(`/elderly/${id}`);
      elderly.emojiAvatar = getElderEmoji(elderly.gender, elderly.relation);
      const app = getApp();
      const isOwner = app.isLoggedIn() &&
          app.globalData.userInfo?.userId === elderly.userId;

      wx.setNavigationBarTitle({ title: elderly.name + ' 的信息' });
      this.setData({ elderly, isOwner, loading: false });
    } catch (err) {
      this.setData({ loading: false });
      wx.showToast({ title: '加载失败', icon: 'none' });
      setTimeout(() => wx.navigateBack(), 1500);
    }
  },

  /** 拨打紧急联系电话 */
  onCallPhone() {
    const phone = this.data.elderly?.contactPhone;
    if (!phone) {
      wx.showToast({ title: '暂无联系电话', icon: 'none' });
      return;
    }
    wx.makePhoneCall({ phoneNumber: phone });
  },

  /** 预览照片 */
  onPreviewPhoto() {
    if (this.data.elderly?.photoUrl) {
      wx.previewImage({
        urls: [this.data.elderly.photoUrl],
        current: this.data.elderly.photoUrl,
      });
    }
  },

  /** 跳转编辑页 */
  onEditTap() {
    wx.navigateTo({
      url: `/pages/edit-elder/edit-elder?id=${this.data.elderlyId}`
    });
  },

  /** 删除老人信息 */
  onDeleteTap() {
    wx.showModal({
      title: '确认删除',
      content: `确定要删除"${this.data.elderly?.name}"的信息吗？\n删除后将从人脸库移除，无法恢复。`,
      confirmText: '确定删除',
      confirmColor: '#FF4D4F',
      success: async (res) => {
        if (res.confirm) {
          try {
            await http.delete(`/elderly/${this.data.elderlyId}`);
            wx.showToast({ title: '已删除', icon: 'success' });
            setTimeout(() => wx.navigateBack(), 1200);
          } catch (err) {
            // toast已在request.js中显示
          }
        }
      }
    });
  },
});
