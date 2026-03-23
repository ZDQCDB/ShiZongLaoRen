// utils/helper.js - 公共工具函数

/**
 * 根据老人的性别和联系人关系，返回合适的 emoji 头像
 * relation 字段含义：联系人与老人的关系（如"子女"表示联系人是老人的孩子）
 *
 * 对应关系推断：
 *   子女      → 联系人是孩子   → 老人是长辈（父/母 或 爷/奶）→ 👴 👵
 *   配偶      → 联系人是伴侣   → 老人是丈夫/妻子             → 👨 👩
 *   父母      → 联系人是父母   → 老人相对年轻                → 🧑 👦/👧
 *   兄弟姐妹  → 联系人是兄弟姐妹 → 老人是兄/弟/姐/妹         → 👨 👩
 *   社区工作者 → 默认长辈形象   → 👴 👵
 *   其他 / 空  → 默认长辈形象   → 👴 👵
 */
function getElderEmoji(gender, relation) {
  const isFemale = gender === '女';

  switch (relation) {
    case '子女':
    case '社区工作者':
      // 联系人是子女/社区人员，老人通常是祖父母/父母级别
      return isFemale ? '👵' : '👴';

    case '配偶':
      // 联系人是配偶，老人是中老年夫妻形象
      return isFemale ? '👩' : '👨';

    case '父母':
      // 联系人是父母，老人相对年轻，用普通成人 emoji
      return isFemale ? '👩' : '👨';

    case '兄弟姐妹':
      // 联系人是兄弟姐妹
      return isFemale ? '👩' : '👨';

    default:
      // 其他、未填，默认老人形象
      return isFemale ? '👵' : '👴';
  }
}

module.exports = { getElderEmoji };
