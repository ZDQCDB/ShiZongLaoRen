// pages/face-search/face-search.js
const { http } = require('../../utils/request');
const { getElderEmoji } = require('../../utils/helper');

Page({
  data: {
    photoPath: '',    // 本地照片路径
    searching: false, // 识别中
    result: null,     // 识别结果
  },

  onLoad() {
    // 人脸识别无需登录
  },

  /**
   * 拍照（仅相机，不支持相册上传）
   */
  onChoosePhoto() {
    wx.chooseMedia({
      count: 1,
      mediaType: ['image'],
      sourceType: ['camera'],
      camera: 'back',   // 后置摄像头拍走失老人
      success: (res) => {
        const path = res.tempFiles[0].tempFilePath;
        this.setData({ photoPath: path, result: null });
      }
    });
  },

  /** 清除照片 */
  onClearPhoto() {
    this.setData({ photoPath: '', result: null });
  },

  /**
   * 开始人脸识别：先压缩图片（阿里云限制分辨率），再 uploadFile
   */
  onStartSearch() {
    if (!this.data.photoPath) {
      wx.showToast({ title: '请先选择照片', icon: 'none' });
      return;
    }
    if (this.data.searching) return;

    this.setData({ searching: true, result: null });
    wx.showLoading({ title: '识别中，请稍候...', mask: true });

    // 先压缩，再上传（阿里云人脸接口要求图片不超过 2 MB / 4096px）
    wx.compressImage({
      src: this.data.photoPath,
      quality: 80,       // 80% 质量，保留人脸细节同时降低分辨率
      success: (compRes) => {
        this._doUpload(compRes.tempFilePath);
      },
      fail: () => {
        // 压缩失败就直传原图
        this._doUpload(this.data.photoPath);
      }
    });
  },

  /** 实际上传逻辑（内部方法） */
  _doUpload(filePath) {
    const app = getApp();
    const token = app.globalData.token;

    wx.uploadFile({
      url: app.globalData.baseUrl + '/face/search',
      filePath,
      name: 'photo',          // 与后端 @RequestParam("photo") 一致
      header: token ? { 'Authorization': `Bearer ${token}` } : {},
      success: (res) => {
        wx.hideLoading();
        console.log('[faceSearch] statusCode:', res.statusCode);
        console.log('[faceSearch] raw data:', res.data);

        let data;
        try {
          data = JSON.parse(res.data);
        } catch (e) {
          this.setData({ searching: false });
          wx.showToast({ title: '响应解析失败', icon: 'none' });
          return;
        }

        if (data.code !== 200) {
          this.setData({ searching: false });
          wx.showToast({ title: data.message || '识别失败', icon: 'none' });
          return;
        }

        const result = data.data;   // FaceSearchResult 对象
        // 预处理置信度百分比字符串，WXML 不支持 .toFixed() 调用
        if (result && result.confidence != null) {
          result.confidencePercent = (result.confidence * 100).toFixed(1);
        }
        // 根据关系计算 emoji 头像
        if (result && result.elderly) {
          result.elderly.emojiAvatar = getElderEmoji(
            result.elderly.gender, result.elderly.relation
          );
        }
        this.setData({ result, searching: false });

        if (result && result.found) {
          wx.showToast({ title: '识别成功！', icon: 'success' });
        } else {
          wx.showToast({ title: result.message || '未找到匹配信息', icon: 'none', duration: 2500 });
        }
      },
      fail: (err) => {
        wx.hideLoading();
        this.setData({ searching: false });
        console.error('[faceSearch] uploadFile fail:', err);
        wx.showToast({ title: '上传失败，请检查网络', icon: 'none' });
      }
    });
  },

  /** 拨打家属电话 */
  onCallPhone() {
    const phone = this.data.result?.elderly?.contactPhone;
    if (!phone) {
      wx.showToast({ title: '暂无联系电话', icon: 'none' });
      return;
    }
    wx.makePhoneCall({ phoneNumber: phone });
  },

  /** 查看老人完整信息 */
  onViewDetail() {
    if (this.data.result?.elderly?.id) {
      wx.navigateTo({
        url: `/pages/elder-detail/elder-detail?id=${this.data.result.elderly.id}`
      });
    }
  },
});
