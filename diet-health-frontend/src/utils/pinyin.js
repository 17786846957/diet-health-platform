// 简易拼音匹配工具 - 支持拼音首字母和全拼搜索
// 基于常见汉字拼音映射，无需第三方库

const pinyinMap = {
  '白':'bai','百':'bai','包':'bao','薄':'bao','北':'bei','本':'ben','比':'bi','扁':'bian',
  '饼':'bing','菠':'bo','菜':'cai','草':'cao','茶':'cha','柴':'chai','橙':'cheng','吃':'chi',
  '赤':'chi','冲':'chong','臭':'chou','醋':'cu','脆':'cui','大':'da','带':'dai','蛋':'dan',
  '刀':'dao','稻':'dao','豆':'dou','炖':'dun','鹅':'e','饭':'fan','肥':'fei','粉':'fen',
  '凤':'feng','腐':'fu','干':'gan','糕':'gao','瓜':'gua','挂':'gua','果':'guo','海':'hai',
  '蚝':'hao','核':'he','黑':'hei','红':'hong','猴':'hou','胡':'hu','花':'hua','黄':'huang',
  '回':'hui','鸡':'ji','煎':'jian','饺':'jiao','酱':'jiang','椒':'jiao','蕉':'jiao',
  '筋':'jin','酒':'ju','橘':'ju','卷':'juan','咖':'ka','烤':'kao','可':'ke','苦':'ku',
  '快':'kuai','辣':'la','兰':'lan','蓝':'lan','梨':'li','栗':'li','莲':'lian','炼':'lian',
  '凉':'liang','龙':'long','卤':'lu','鲈':'lu','绿':'lv','馒':'man','芒':'mang',
  '毛':'mao','玫':'mei','米':'mi','猕':'mi','蜜':'mi','面':'mian','蘑':'mo',
  '奶':'nai','南':'nan','柠':'ning','牛':'niu','浓':'nong','排':'pai','胖':'pang','泡':'pao',
  '烹':'peng','苹':'ping','葡':'pu','七':'qi','芹':'qin','茄':'qie','青':'qing','清':'qing',
  '秋':'qiu','球':'qiu','全':'quan','热':'re','人':'ren','肉':'rou','乳':'ru','软':'ruan',
  '三':'san','桑':'sang','沙':'sha','山':'shan','烧':'shao','蛇':'she','生':'sheng','石':'shi',
  '食':'shi','柿':'shi','薯':'shu','水':'shui','丝':'si','松':'song','酸':'suan','蒜':'suan',
  '笋':'sun','汤':'tang','糖':'tang','桃':'tao','甜':'tian','铁':'tie','土':'tu','豌':'wan',
  '丸':'wan','味':'wei','蜗':'wo','莴':'wo','西':'xi','虾':'xia','鲜':'xian','香':'xiang',
  '箱':'xiang','小':'xiao','蟹':'xie','杏':'xing','雪':'xue','鸭':'ya','羊':'yang','洋':'yang',
  '腰':'yao','药':'yao','椰':'ye','意':'yi','银':'yin','樱':'ying','柚':'you','鱿':'you',
  '鱼':'yu','玉':'yu','芋':'yu','枣':'zao','炸':'zha','芝':'zhi','猪':'zhu',
  '竹':'zhu','煮':'zhu','紫':'zi','粽':'zong','嘴':'zui',

  // 蔬菜
  '西兰花':'xilanhua','西红柿':'xihongshi','番茄':'fanqie','胡萝卜':'huluobo',
  '菠菜':'bocai','白菜':'baicai','韭菜':'jiucai','芹菜':'qincai','生菜':'shengcai',
  '油麦菜':'youmaicai','空心菜':'kongxincai','茼蒿':'tonghao','苦瓜':'kugua',
  '丝瓜':'sigua','南瓜':'nangua','冬瓜':'donggua','洋葱':'yangcong','蒜苗':'suanmiao',
  '莴笋':'wosun','竹笋':'zhusun','莲藕':'lianou','山药':'shanyao','秋葵':'qiukui',
  '香菜':'xiangcai','荠菜':'jicai','苋菜':'xiancai','木耳':'muer','银耳':'yiner',

  // 水果
  '猕猴桃':'mihoutao','芒果':'mangguo','菠萝':'boluo','樱桃':'yingtao',
  '桃子':'taozi','梨':'li','柚子':'youzi','荔枝':'lizhi','龙眼':'longyan',
  '火龙果':'huolongguo','木瓜':'mugua','哈密瓜':'hamigua','山竹':'shanzhu',
  '石榴':'shiliu','百香果':'baixiangguo','柿子':'shizi','桑葚':'sangshen',

  // 肉类
  '鸡胸肉':'jixiongrou','猪排骨':'zhupaigu','猪蹄':'zhuti','猪肝':'zhugan',
  '鸡腿':'jitui','鸡翅':'jichi','鸭肉':'yarou','鹅肉':'erou','羊肉':'yangrou',
  '兔肉':'turou','三文鱼':'sanwenyu','带鱼':'daiyu','鲫鱼':'jiyu','草鱼':'caoyu',
  '螃蟹':'pangxie','鱿鱼':'youyu','海参':'haishen','猪血':'zhuxue',
  '猪肉':'zhurou','牛肉':'niurou','鱼肉':'yurou','虾仁':'xiaren',

  // 主食
  '白米饭':'baimifan','面条':'miantiao','全麦面包':'quanmaimianbao',
  '饺子':'jiaozi','包子':'baozi','油条':'youtiao','花卷':'huajuan',
  '米粉':'mifen','河粉':'hefen','年糕':'niangao','粽子':'zongzi',
  '馄饨':'huntun','窝窝头':'wowotou','燕麦片':'yanmaipian','意大利面':'yidalimian',

  // 蛋奶
  '鸡蛋':'jidan','鸭蛋':'yadan','鹌鹑蛋':'anchundan','牛奶':'niunai',
  '酸奶':'suannai','豆腐':'doufu','豆浆':'doujiang','腐竹':'fuzhu',
  '毛豆':'maodou','黄豆':'huangdou','黑豆':'heidou','红豆':'hongdou','绿豆':'lvdou',

  // 零食
  '花生':'huasheng','核桃':'hetao','薯片':'shupian','杏仁':'xingren',
  '腰果':'yaoguo','开心果':'kaixinguo','瓜子':'guazi','巧克力':'qiaokeli',
  '蛋糕':'dangao','冰淇淋':'bingqilin','海苔':'haitai',

  // 饮品
  '可乐':'kele','绿茶':'lvcha','咖啡':'kafei','奶茶':'naicha',
  '橙汁':'chengzhi','椰汁':'yezhi','啤酒':'pijiu','红酒':'hongjiu',

  // 家常菜
  '西红柿炒鸡蛋':'xihongshichaojidan','宫保鸡丁':'gongbaojiding',
  '鱼香肉丝':'yuxiangrousi','红烧肉':'hongshaorou','糖醋排骨':'tangcupaigu',
  '回锅肉':'huiguorou','麻婆豆腐':'mapodoufu','清炒时蔬':'qingchaoshishu',
  '蒜蓉西兰花':'suanrongxilanhua','干煸豆角':'ganbiandoujiao',
  '地三鲜':'disanxian','青椒炒肉':'qingjiaochaorou','木须肉':'muxurou',
  '酸菜鱼':'suancaiyu','水煮鱼':'shuizhuyu','红烧茄子':'hongshaoqiezi',
  '可乐鸡翅':'kelejichi','蛋炒饭':'danchaofan','炒面':'chaomian',
  '兰州拉面':'lanzhoulamian','担担面':'dandanmian','酸辣粉':'suanlafen',
  '麻辣烫':'malatang','煎饺':'jianjiao','蒸蛋':'zhengdan',
  '凉拌黄瓜':'liangbanhuanggua','凉拌木耳':'liangbanmuer','皮蛋豆腐':'pidandoufu',

  // 调味品
  '老干妈':'laoganma','蚝油':'haoyou','番茄酱':'fanqiejiang','芝麻酱':'zhimajiang',
  '辣椒酱':'lajiaojiang','腐乳':'furu','沙拉酱':'shalajiang',

  // 外卖
  '黄焖鸡米饭':'huangminjimifan','麻辣香锅':'malaxiangguo','烤肉饭':'kaoroufan',
  '炒年糕':'chaoniangao','汉堡':'hanbao','炸鸡':'zhaji','披萨':'pisha',
  '寿司':'shousi','烤串':'kaochuan','砂锅粥':'shaguozhou',
  '肉夹馍':'roujiamo','凉皮':'liangpi','螺蛳粉':'luosifen',
  '煎饼果子':'jianbingguozi','关东煮':'guandongzhu','鸡排':'jipai','牛肉面':'niuroumian'
}

// 获取汉字拼音（取第一个字的拼音）
function getCharPinyin(char) {
  return pinyinMap[char] || char.toLowerCase()
}

// 获取词组拼音（全拼拼接）
function getWordPinyin(word) {
  // 先尝试整词匹配
  if (pinyinMap[word]) return pinyinMap[word]
  // 逐字拼接
  let result = ''
  for (const char of word) {
    result += pinyinMap[char] || char.toLowerCase()
  }
  return result
}

// 拼音匹配函数 - 支持中文、拼音首字母、全拼
export function pinyinMatch(text, keyword) {
  if (!text || !keyword) return false

  const lowerKeyword = keyword.toLowerCase()
  const lowerText = text.toLowerCase()

  // 1. 直接中文匹配
  if (lowerText.includes(lowerKeyword)) return true

  // 2. 拼音全拼匹配
  const fullPinyin = getWordPinyin(text)
  if (fullPinyin.includes(lowerKeyword)) return true

  // 3. 拼音首字母匹配
  let initials = ''
  for (const char of text) {
    const py = pinyinMap[char]
    if (py) {
      initials += py[0]
    } else {
      initials += char.toLowerCase()
    }
  }
  if (initials.includes(lowerKeyword)) return true

  return false
}
