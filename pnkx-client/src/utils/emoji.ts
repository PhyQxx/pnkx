const raw = 'https://ftp.pnkx.top:8/ftp/pnkx/static/'

const faceList = ['tv_小电视', '小黄脸']

const emojiList = [
  {
    '[tv_白眼]': `${raw}/emoji/tv/baiyan.gif`,
    '[tv_doge]': `${raw}/emoji/tv/doge.gif`,
    '[tv_坏笑]': `${raw}/emoji/tv/huaixiao.gif`,
    '[tv_难过]': `${raw}/emoji/tv/nanguo.gif`,
    '[tv_生气]': `${raw}/emoji/tv/shengqi.gif`,
    '[tv_委屈]': `${raw}/emoji/tv/weiqu.gif`,
    '[tv_斜眼笑]': `${raw}/emoji/tv/xieyanxiao.gif`,
    '[tv_呆]': `${raw}/emoji/tv/dai.gif`,
    '[tv_发怒]': `${raw}/emoji/tv/fanu.gif`,
    '[tv_惊吓]': `${raw}/emoji/tv/jingxia.gif`,
    '[tv_呕吐]': `${raw}/emoji/tv/outu.gif`,
    '[tv_思考]': `${raw}/emoji/tv/sikao.gif`,
    '[tv_微笑]': `${raw}/emoji/tv/weixiao.gif`,
    '[tv_疑问]': `${raw}/emoji/tv/yiwen.gif`,
    '[tv_大哭]': `${raw}/emoji/tv/daku.gif`,
    '[tv_鼓掌]': `${raw}/emoji/tv/guzhang.gif`,
    '[tv_抠鼻]': `${raw}/emoji/tv/koubi.gif`,
    '[tv_亲亲]': `${raw}/emoji/tv/qinqin.gif`,
    '[tv_调皮]': `${raw}/emoji/tv/tiaopi.gif`,
    '[tv_笑哭]': `${raw}/emoji/tv/xiaoku.gif`,
    '[tv_晕]': `${raw}/emoji/tv/yun.gif`,
    '[tv_点赞]': `${raw}/emoji/tv/dianzan.gif`,
    '[tv_害羞]': `${raw}/emoji/tv/haixiu.gif`,
    '[tv_睡着]': `${raw}/emoji/tv/shuizhe.gif`,
    '[tv_色]': `${raw}/emoji/tv/se.gif`,
    '[tv_吐血]': `${raw}/emoji/tv/tuxue.gif`,
    '[tv_无奈]': `${raw}/emoji/tv/wunai.gif`,
    '[tv_再见]': `${raw}/emoji/tv/zaijian.gif`,
    '[tv_流汗]': `${raw}/emoji/tv/liuhan.gif`,
    '[tv_偷笑]': `${raw}/emoji/tv/touxiao.gif`,
    '[tv_抓狂]': `${raw}/emoji/tv/zhuakuang.gif`,
    '[tv_黑人问号]': `${raw}/emoji/tv/wenhao.gif`,
    '[tv_困]': `${raw}/emoji/tv/kun.gif`,
    '[tv_打脸]': `${raw}/emoji/tv/dalian.gif`,
    '[tv_闭嘴]': `${raw}/emoji/tv/bizui.gif`,
    '[tv_鄙视]': `${raw}/emoji/tv/bishi.gif`,
    '[tv_腼腆]': `${raw}/emoji/tv/miantian.gif`,
    '[tv_馋]': `${raw}/emoji/tv/chan.gif`,
    '[tv_可爱]': `${raw}/emoji/tv/keai.gif`,
    '[tv_发财]': `${raw}/emoji/tv/facai.gif`,
    '[tv_生病]': `${raw}/emoji/tv/shengbing.gif`,
    '[tv_流鼻血]': `${raw}/emoji/tv/liubixue.gif`,
    '[tv_尴尬]': `${raw}/emoji/tv/ganga.gif`,
    '[tv_大佬]': `${raw}/emoji/tv/dalao.gif`
  },
  {
    '[衰]': `${raw}/emoji/01/shai.gif`,
    '[鄙视]': `${raw}/emoji/01/bishi.gif`,
    '[再见]': `${raw}/emoji/01/zaijian.gif`,
    '[捂嘴]': `${raw}/emoji/01/wuzui.gif`,
    '[摸鱼]': `${raw}/emoji/01/moyu.gif`,
    '[奋斗]': `${raw}/emoji/01/fendou.gif`,
    '[白眼]': `${raw}/emoji/01/baiyan.gif`,
    '[可怜]': `${raw}/emoji/01/kelian.gif`,
    '[皱眉]': `${raw}/emoji/01/zhoumei.gif`,
    '[鼓掌]': `${raw}/emoji/01/guzhang.gif`,
    '[烦恼]': `${raw}/emoji/01/fannao.gif`,
    '[吐舌]': `${raw}/emoji/01/tushe.gif`,
    '[挖鼻]': `${raw}/emoji/01/wabi.gif`,
    '[委屈]': `${raw}/emoji/01/weiqu.gif`,
    '[滑稽]': `${raw}/emoji/01/huaji.gif`,
    '[啊这]': `${raw}/emoji/01/azhe.gif`,
    '[生气]': `${raw}/emoji/01/shengqi.gif`,
    '[害羞]': `${raw}/emoji/01/haixiu.gif`,
    '[晕]': `${raw}/emoji/01/yun.gif`,
    '[好色]': `${raw}/emoji/01/haose.gif`,
    '[流泪]': `${raw}/emoji/01/liulei.gif`,
    '[吐血]': `${raw}/emoji/01/tuxie.gif`,
    '[微笑]': `${raw}/emoji/01/weixiao.gif`,
    '[酷]': `${raw}/emoji/01/ku.gif`,
    '[坏笑]': `${raw}/emoji/01/huaixiao.gif`,
    '[吓]': `${raw}/emoji/01/xia.gif`,
    '[大兵]': `${raw}/emoji/01/dabing.gif`,
    '[哭笑]': `${raw}/emoji/01/kuxiao.gif`,
    '[困]': `${raw}/emoji/01/kun.gif`,
    '[呲牙]': `${raw}/emoji/01/ciya.gif`
  }
]

// 把emojiList数组中的每一个对象，放到emojiMap中方便取用
function allEmoji(): {} {
  let emojiMap = {}
  emojiList.map((emojis) => (emojiMap = { ...emojiMap, ...emojis }))
  return emojiMap
}

export default {
  faceList,
  emojiList,
  allEmoji: allEmoji()
}
