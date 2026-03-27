// utils/request.js - 统一网络请求封装

const app = getApp();

/**
 * 发送底层请求（不含401重试逻辑）
 */
function rawRequest(options, token) {
  return new Promise((resolve, reject) => {
    const { url, method = 'GET', data, header = {}, showLoading = false } = options;

    if (showLoading) {
      wx.showLoading({ title: '请稍候...', mask: true });
    }

    const reqHeader = { ...header };
    if (token) {
      reqHeader['Authorization'] = `Bearer ${token}`;
    }
    reqHeader['Content-Type'] = reqHeader['Content-Type'] || 'application/json';

    wx.request({
      url: app.globalData.baseUrl + url,
      method,
      data,
      header: reqHeader,
      timeout: 30000,
      success: (res) => {
        if (showLoading) wx.hideLoading();
        resolve(res);
      },
      fail: (err) => {
        if (showLoading) wx.hideLoading();
        reject(err);
      }
    });
  });
}

/**
 * 封装 wx.request，自动附带 token，统一处理错误
 * 遇到 401 时先尝试静默刷新 token，刷新成功则重试请求，失败才跳登录页
 * @param {Object} options - 请求配置
 * @param {boolean} isRetry - 是否是重试请求（防止死循环）
 * @returns {Promise}
 */
async function request(options, isRetry = false) {
  const token = app.globalData.token;

  let res;
  try {
    res = await rawRequest(options, token);
  } catch (err) {
    console.error('网络请求失败:', err);
    const msg = err.errMsg?.includes('timeout') ? '请求超时，请检查网络' : '网络连接失败';
    wx.showToast({ title: msg, icon: 'none' });
    throw err;
  }

  const { statusCode, data: resData } = res;

  if (statusCode === 401 && !isRetry) {
    // Token 过期，先尝试静默刷新（用户无感知）
    const refreshed = await app.silentRefreshToken();
    if (refreshed) {
      // 刷新成功，用新 token 重试请求
      return request(options, true);
    }
    // 静默刷新失败（彻底无法登录），才清除状态并跳转
    app.clearLoginState();
    wx.showToast({ title: '登录信息已失效，请重新登录', icon: 'none', duration: 2000 });
    setTimeout(() => app.navigateToLogin(), 1500);
    throw new Error('未授权');
  }

  if (statusCode === 401 && isRetry) {
    app.clearLoginState();
    wx.showToast({ title: '登录信息已失效，请重新登录', icon: 'none', duration: 2000 });
    setTimeout(() => app.navigateToLogin(), 1500);
    throw new Error('未授权');
  }

  if (statusCode !== 200) {
    const msg = resData?.message || `请求失败(${statusCode})`;
    wx.showToast({ title: msg, icon: 'none' });
    throw new Error(msg);
  }

  if (resData.code !== 200) {
    const msg = resData.message || '操作失败';
    wx.showToast({ title: msg, icon: 'none' });
    throw new Error(msg);
  }

  return resData.data;
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
