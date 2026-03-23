# -*- coding: utf-8 -*-
# pip install alibabacloud_facebody20191230

import io, json
from alibabacloud_facebody20191230.client import Client
from alibabacloud_facebody20191230.models import SearchFaceAdvanceRequest
from alibabacloud_tea_openapi.models import Config
from alibabacloud_tea_util.models import RuntimeOptions

ACCESS_KEY_ID     = 'LTAI5tNzPYZCGCBUjiYLH7Ko'
ACCESS_KEY_SECRET = 'MHB56uaRcwOGab2tYtHkKRWTtUa1rW'

# 本地图片路径
LOCAL_IMAGE_PATH = r'E:/xiaochengxv/face/face/202311070411.jpg'
DB_NAME          = 'ShiZongLaoRen'

config = Config(
    access_key_id=ACCESS_KEY_ID,
    access_key_secret=ACCESS_KEY_SECRET,
    endpoint='facebody.cn-shanghai.aliyuncs.com',
    region_id='cn-shanghai'
)
client         = Client(config)
runtime_option = RuntimeOptions()

with open(LOCAL_IMAGE_PATH, 'rb') as f:
    IMAGE_BYTES = f.read()

req = SearchFaceAdvanceRequest()
req.image_url_object        = io.BytesIO(IMAGE_BYTES)
req.db_names                = DB_NAME   # ★ 用 db_names（字符串），不用 db_name
req.limit                   = 10
req.max_face_num            = 10
req.quality_score_threshold = 0

try:
    response = client.search_face_advance(req, runtime_option)
    body_dict = response.body.to_map()

    print('========== 格式化响应 ==========')
    print(json.dumps(body_dict, ensure_ascii=False, indent=2))

    print('\n========== 匹配结果 ==========')
    match_list = body_dict.get('Data', {}).get('MatchList', [])
    found = False
    for i, match in enumerate(match_list):
        loc = match.get('Location', {})
        items = match.get('FaceItems', [])
        print(f'\n人脸{i+1}: X={loc.get("X")} Y={loc.get("Y")}  质量={match.get("QualitieScore")}')
        if items:
            found = True
            for item in items:
                print(f'  ✔ 匹配! EntityId={item.get("EntityId")}  '
                      f'Score={item.get("Score")}  '
                      f'ExtraData={item.get("ExtraData")}  '
                      f'FaceId={item.get("FaceId")}')
        else:
            print('  ✘ 未找到匹配')

    print('\n' + '='*40)
    print('✅ 搜索成功！' if found else '❌ 仍未找到匹配')
    print('='*40)

except Exception as error:
    print(f'请求失败: {error}')
    try:
        print(f'错误码: {error.code}')
        print(f'详情: {error.__dict__}')
    except Exception:
        pass
