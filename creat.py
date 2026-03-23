# -*- coding: utf-8 -*-
# 引入依赖包
# pip install alibabacloud_facebody20191230

import os
from alibabacloud_facebody20191230.models import CreateFaceDbRequest
from alibabacloud_tea_openapi.models import Config
from alibabacloud_facebody20191230.client import Client
from alibabacloud_tea_util.models import RuntimeOptions

# -*- coding: utf-8 -*-
import json
from alibabacloud_facebody20191230.client import Client
from alibabacloud_facebody20191230.models import SearchFaceAdvanceRequest
from alibabacloud_tea_openapi.models import Config
from alibabacloud_tea_util.models import RuntimeOptions

ACCESS_KEY_ID     = 'LTAI5tNzPYZCGCBUjiYLH7Ko'
ACCESS_KEY_SECRET = 'MHB56uaRcwOGab2tYtHkKRWTtUa1rW'


config = Config(
    access_key_id=ACCESS_KEY_ID,
    access_key_secret=ACCESS_KEY_SECRET,
    endpoint='facebody.cn-shanghai.aliyuncs.com',
    region_id='cn-shanghai'
)
runtime_option = RuntimeOptions()
create_face_db_request = CreateFaceDbRequest(
            name='ShiZongLaoRen'
        )
try:
  # 初始化Client
  client = Client(config)
  response = client.create_face_db_with_options(create_face_db_request, runtime_option)
  # 获取整体结果
  print(response.body)
except Exception as error:
  # 获取整体报错信息
  print(error)
  # 获取单个字段
  print(error.code)
  # tips: 可通过error.__dict__查看属性名称