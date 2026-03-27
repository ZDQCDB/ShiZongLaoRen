// pages/add-elder/add-elder.js
const { http } = require('../../utils/request');
const { checkLogin } = require('../../utils/auth');

const MAX_PHOTOS = 5;   // 每位老人最多注册照片数

Page({
  data: {
    form: {
      name: '',
      gender: '男',
      age: '',
      idCard: '',
      address: '',
      contactPhone: '',
      contactName: '',
      relation: '子女',
      features: '',
      medicalHistory: '',
      additionalInfo: '',
    },
    relations: ['子女', '配偶', '父母', '兄弟姐妹', '社区工作者', '其他'],
    // 已选择的照片本地路径列表（最多5张）
    selectedPhotos: [],
    uploadingPhotos: false,  // 是否正在上传照片
    submitting: false,
    uploadProgress: '',      // 上传进度文字，如"正在上传第2/3张..."
  },

  onLoad() {
    if (!checkLogin()) return;
  },

  // =========================================================
  // 照片选择
  // =========================================================

  /** 点击添加照片 */
  onAddPhoto() {
    const remaining = MAX_PHOTOS - this.data.selectedPhotos.length;
    if (remaining <= 0) {
      wx.showToast({ title: `最多选择${MAX_PHOTOS}张照片`, icon: 'none' });
      return;
    }

    wx.showActionSheet({
      itemList: ['拍照', '从相册选择'],
      success: (res) => {
        const sourceType = res.tapIndex === 0 ? ['camera'] : ['album'];
        wx.chooseMedia({
          count: remaining,           // 最多选剩余数量
          mediaType: ['image'],
          sourceType,
          sizeType: ['compressed'],   // 强制压缩，避免超过阿里云 2MB/4096px 限制
          success: (mediaRes) => {
            const newPaths = mediaRes.tempFiles.map(f => f.tempFilePath);
            const all = [...this.data.selectedPhotos, ...newPaths].slice(0, MAX_PHOTOS);
            this.setData({ selectedPhotos: all });
          }
        });
      }
    });
  },

  /** 删除已选照片 */
  onRemovePhoto(e) {
    const index = e.currentTarget.dataset.index;
    const photos = [...this.data.selectedPhotos];
    photos.splice(index, 1);
    this.setData({ selectedPhotos: photos });
  },

  /** 预览照片 */
  onPreviewPhoto(e) {
    const index = e.currentTarget.dataset.index;
    wx.previewImage({
      current: this.data.selectedPhotos[index],
      urls: this.data.selectedPhotos,
    });
  },

  // =========================================================
  // 表单输入
  // =========================================================

  onInput(e) {
    const field = e.currentTarget.dataset.field;
    this.setData({ [`form.${field}`]: e.detail.value });
  },

  onGenderSelect(e) {
    this.setData({ 'form.gender': e.currentTarget.dataset.gender });
  },

  onRelationSelect(e) {
    this.setData({ 'form.relation': e.currentTarget.dataset.relation });
  },

  // =========================================================
  // 表单验证
  // =========================================================

  validateForm() {
    const { name, gender, address, contactPhone } = this.data.form;
    if (!name.trim()) {
      wx.showToast({ title: '请填写家人姓名', icon: 'none' });
      return false;
    }
    if (!gender) {
      wx.showToast({ title: '请选择性别', icon: 'none' });
      return false;
    }
    if (!address.trim()) {
      wx.showToast({ title: '请填写家庭住址', icon: 'none' });
      return false;
    }
    if (!contactPhone.trim()) {
      wx.showToast({ title: '请填写紧急联系电话', icon: 'none' });
      return false;
    }
    if (!/^1[3-9]\d{9}$/.test(contactPhone.trim())) {
      wx.showToast({ title: '请输入正确的手机号码', icon: 'none' });
      return false;
    }
    return true;
  },

  // =========================================================
  // 提交逻辑：先创建老人，再逐张上传照片
  // =========================================================

  onSubmit() {
    if (!checkLogin()) return;
    if (!this.validateForm()) return;

    // 没有选择照片时给出提示
    if (this.data.selectedPhotos.length === 0) {
      wx.showModal({
        title: '未添加照片',
        content: '未上传照片将无法使用人脸识别功能，建议至少上传1张正面清晰照。确定不上传照片吗？',
        confirmText: '直接保存',
        cancelText: '去添加',
        success: (res) => {
          if (res.confirm) this.doSubmit();
        }
      });
      return;
    }

    this.doSubmit();
  },

  async doSubmit() {
    if (this.data.submitting) return;
    this.setData({ submitting: true, uploadProgress: '' });

    try {
      const form = this.data.form;
      const submitData = {
        name: form.name.trim(),
        gender: form.gender,
        age: form.age ? parseInt(form.age) : null,
        idCard: form.idCard.trim() || null,
        address: form.address.trim(),
        contactPhone: form.contactPhone.trim(),
        contactName: form.contactName.trim() || null,
        relation: form.relation || null,
        features: form.features.trim() || null,
        medicalHistory: form.medicalHistory.trim() || null,
        additionalInfo: form.additionalInfo.trim() || null,
      };

      // ① 创建老人基本信息，获取 elderlyId
      // http.post 已自动解包 resData.data，所以 createRes 就是 ElderlyDTO 对象
      const createRes = await http.post('/elderly', submitData);
      const elderlyId = createRes.id;

      // ② 逐张上传照片并注册人脸
      const photos = this.data.selectedPhotos;
      if (photos.length > 0) {
        this.setData({ uploadingPhotos: true });
        for (let i = 0; i < photos.length; i++) {
          this.setData({ uploadProgress: `正在上传第${i + 1}/${photos.length}张照片...` });
          try {
            await this.uploadOnePhoto(elderlyId, photos[i]);
          } catch (err) {
            // 某张照片上传失败，提示但继续
            console.warn(`第${i + 1}张照片上传失败:`, err);
            wx.showToast({
              title: `第${i + 1}张照片注册失败，已跳过`,
              icon: 'none',
              duration: 2000
            });
          }
        }
      }

      wx.showToast({ title: '保存成功！', icon: 'success', duration: 1500 });
      setTimeout(() => {
        wx.navigateBack();
      }, 1500);

    } catch (err) {
      // toast 已在 request.js 中显示
      console.error('提交失败:', err);
    } finally {
      this.setData({ submitting: false, uploadingPhotos: false, uploadProgress: '' });
    }
  },

  /**
   * 上传单张照片到服务器（POST /elderly/{id}/photo）
   * 先压缩图片，避免阿里云人脸接口报"分辨率超限"
   */
  uploadOnePhoto(elderlyId, filePath) {
    return new Promise((resolve, reject) => {
      // 第一步：压缩图片
      wx.compressImage({
        src: filePath,
        quality: 80,
        success: (compRes) => {
          this._doUploadPhoto(elderlyId, compRes.tempFilePath, resolve, reject);
        },
        fail: () => {
          // 压缩失败则上传原图
          this._doUploadPhoto(elderlyId, filePath, resolve, reject);
        }
      });
    });
  },

  /** 内部：执行实际文件上传 */
  _doUploadPhoto(elderlyId, filePath, resolve, reject) {
    const token = wx.getStorageSync('token');
    const app = getApp();
    const baseUrl = app.globalData.baseUrl;

    wx.uploadFile({
      url: `${baseUrl}/elderly/${elderlyId}/photo`,
      filePath,
      name: 'photo',
      header: { 'Authorization': `Bearer ${token}` },
      success: (res) => {
        try {
          const data = JSON.parse(res.data);
          if (data.code === 200 || data.code === 0) {
            resolve(data);
          } else {
            reject(new Error(data.message || '上传失败'));
          }
        } catch (e) {
          reject(new Error('响应解析失败'));
        }
      },
      fail: (err) => {
        reject(err);
      }
    });
  }
});
