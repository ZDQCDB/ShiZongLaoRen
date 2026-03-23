// utils/request.js - 统一网络请求封装

const app = getApp();

/**
 * 封装 wx.request，自动附带 token，统一处理错误
 * @param {Object} options - 请求配置
 * @returns {Promise}
 */
function request(options) {
  return new Promise((resolve, reject) => {
    const { url, method = 'GET', data, header = {}, showLoading = false } = options;

    if (showLoading) {
      wx.showLoading({ title: '请稍候...', mask: true });
    }

    // 自动附加 Authorization 头
    const token = app.globalData.token;
    if (token) {
      header['Authorization'] = `Bearer ${token}`;
    }
    header['Content-Type'] = header['Content-Type'] || 'application/json';

    wx.request({
      url: app.globalData.baseUrl + url,
      method,
      data,
      header,
      timeout: 30000,
      success: (res) => {
        if (showLoading) wx.hideLoading();

        const { statusCode, data: resData } = res;

        if (statusCode === 401) {
          // Token 过期，跳转登录
          app.clearLoginState();
          wx.showToast({ title: '登录已过期，请重新登录', icon: 'none', duration: 2000 });
          setTimeout(() => app.navigateToLogin(), 1500);
          reject(new Error('未授权'));
          return;
        }

        if (statusCode !== 200) {
          const msg = resData?.message || `请求失败(${statusCode})`;
          wx.showToast({ title: msg, icon: 'none' });
          reject(new Error(msg));
          return;
        }

        if (resData.code !== 200) {
          const msg = resData.message || '操作失败';
          wx.showToast({ title: msg, icon: 'none' });
          reject(new Error(msg));
          return;
        }

        resolve(resData.data);
      },
      fail: (err) => {
        if (showLoading) wx.hideLoading();
        console.error('网络请求失败:', err);
        const msg = err.errMsg?.includes('timeout') ? '请求超时，请检查网络' : '网络连接失败';
        wx.showToast({ title: msg, icon: 'none' });
        reject(err);
      }
    });
  });
}

/**
 * 上传文件（照片上传）
 * @param {string} filePath - 本地文件路径
 * @param {string} folder - 服务器存储目录
 * @returns {Promise<string>} - 返回图片URL
 */
function uploadFile(filePath, folder = 'elderly') {
  return new Promise((resolve, reject) => {
    wx.showLoading({ title: '上传中...', mask: true });

    const token = app.globalData.token;

    wx.uploadFile({
      url: app.globalData.baseUrl + '/upload/photo',
      filePath,
      name: 'file',
      formData: { folder },
      header: token ? { 'Authorization': `Bearer ${token}` } : {},
      timeout: 60000,
      success: (res) => {
        wx.hideLoading();
        try {
          const data = JSON.parse(res.data);
          if (data.code === 200 && data.data?.url) {
            resolve(data.data.url);
          } else {
            wx.showToast({ title: data.message || '上传失败', icon: 'none' });
            reject(new Error(data.message || '上传失败'));
          }
        } catch (e) {
          wx.showToast({ title: '上传响应解析失败', icon: 'none' });
          reject(e);
        }
      },
      fail: (err) => {
        wx.hideLoading();
        wx.showToast({ title: '文件上传失败', icon: 'none' });
        reject(err);
      }
    });
  });
}

/**
 * 封装常用请求方法
 */
const http = {
  get:    (url, data, options = {}) => request({ url, method: 'GET',    data, ...options }),
  post:   (url, data, options = {}) => request({ url, method: 'POST',   data, ...options }),
  put:    (url, data, options = {}) => request({ url, method: 'PUT',    data, ...options }),
  delete: (url, data, options = {}) => request({ url, method: 'DELETE', data, ...options }),
  upload: uploadFile,
};

module.exports = { http };
