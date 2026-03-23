// pages/edit-elder/edit-elder.js
const { http } = require('../../utils/request');
const { checkLogin } = require('../../utils/auth');

Page({
  data: {
    elderlyId: null,
    form: {
      name: '',
      gender: '男',
      age: '',
      address: '',
      contactPhone: '',
      contactName: '',
      relation: '子女',
      photoUrl: '',     // 只读，仅用于展示
      photoCount: 0,    // 只读，展示已注册照片数
      features: '',
      medicalHistory: '',
      additionalInfo: '',
    },
    relations: ['子女', '配偶', '父母', '兄弟姐妹', '社区工作者', '其他'],
    submitting: false,
    // 注意：照片不允许修改，已去除 uploading 状态
  },

  onLoad(options) {
    if (!checkLogin()) return;
    const id = options.id;
    if (!id) {
      wx.navigateBack();
      return;
    }
    this.setData({ elderlyId: id });
    this.loadElderlyInfo(id);
  },

  async loadElderlyInfo(id) {
    wx.showLoading({ title: '加载中...', mask: true });
    try {
      const elderly = await http.get(`/elderly/${id}`);
      wx.hideLoading();
      this.setData({
        form: {
          name: elderly.name || '',
          gender: elderly.gender || '男',
          age: elderly.age ? String(elderly.age) : '',
          address: elderly.address || '',
          contactPhone: elderly.contactPhone || '',
          contactName: elderly.contactName || '',
          relation: elderly.relation || '子女',
          photoUrl: elderly.photoUrl || '',     // 只读展示
          photoCount: elderly.photoCount || 0,  // 只读展示
          features: elderly.features || '',
          medicalHistory: elderly.medicalHistory || '',
          additionalInfo: elderly.additionalInfo || '',
        }
      });
    } catch (err) {
      wx.hideLoading();
      wx.navigateBack();
    }
  },

  /** 预览照片（只读，不可修改） */
  onPreviewPhoto() {
    if (this.data.form.photoUrl) {
      wx.previewImage({
        current: this.data.form.photoUrl,
        urls: [this.data.form.photoUrl],
      });
    }
  },

  onInput(e) {
    this.setData({ [`form.${e.currentTarget.dataset.field}`]: e.detail.value });
  },

  onGenderSelect(e) {
    this.setData({ 'form.gender': e.currentTarget.dataset.gender });
  },

  onRelationSelect(e) {
    this.setData({ 'form.relation': e.currentTarget.dataset.relation });
  },

  onSubmit() {
    if (!checkLogin()) return;
    const { name, address, contactPhone } = this.data.form;
    if (!name.trim()) {
      wx.showToast({ title: '请填写老人姓名', icon: 'none' });
      return;
    }
    if (!address.trim()) {
      wx.showToast({ title: '请填写家庭住址', icon: 'none' });
      return;
    }
    if (!contactPhone.trim() || !/^1[3-9]\d{9}$/.test(contactPhone.trim())) {
      wx.showToast({ title: '请输入正确的手机号码', icon: 'none' });
      return;
    }
    this.doSubmit();
  },

  async doSubmit() {
    if (this.data.submitting) return;
    this.setData({ submitting: true });
    try {
      const form = this.data.form;
      // 提交时只发送可修改字段，不含 photoUrl / photoCount / faceEntityId
      await http.put(`/elderly/${this.data.elderlyId}`, {
        name: form.name.trim(),
        gender: form.gender,
        age: form.age ? parseInt(form.age) : null,
        address: form.address.trim(),
        contactPhone: form.contactPhone.trim(),
        contactName: form.contactName.trim() || null,
        relation: form.relation || null,
        features: form.features.trim() || null,
        medicalHistory: form.medicalHistory.trim() || null,
        additionalInfo: form.additionalInfo.trim() || null,
      });
      wx.showToast({ title: '修改成功！', icon: 'success', duration: 1200 });
      setTimeout(() => wx.navigateBack(), 1200);
    } catch (err) {
      // toast 已在 request.js 中显示
    } finally {
      this.setData({ submitting: false });
    }
  }
});
