// Eagle-compatible identifiers; independently drawn Lucide/MDI artwork, no Eagle assets.
import fs from 'node:fs/promises';
import path from 'node:path';
const root = path.resolve(import.meta.dirname, '..');
const rows = `
|folder|默认文件夹
library|library|素材库
box|box|盒子
grid|grid-2x2|网格
layer|layers|图层
briefcase|briefcase-business|公文包
photo|image|图片
photos|images|多张图片
video|video|视频
film|film|胶片
film2|clapperboard|场记板
film3|projector|放映机
music|music|音乐
book|book|书籍
book2|book-open|翻开的书
bookshelf|library-big|书架
keynote|presentation|演示
camera|camera|相机
aperture|aperture|光圈
attachment|paperclip|附件
scissors|scissors|剪刀
palette|palette|调色盘
wrench|wrench|扳手
helmet|hard-hat|头盔
life-buoy|life-buoy|救生圈
graph|network|结构图
graph2|chart-network|关系图
tableware|utensils|餐具
cog|settings|设置
bachelor-cap|graduation-cap|学士帽
cones|traffic-cone|路锥
dribbble|dribbble|篮球
email|mail|邮件
business-card|contact|名片
coffee|coffee|咖啡
cart|shopping-cart|购物车
lightbulb|lightbulb|灯泡
inspiration|lightbulb-off|灵感
thumb-up|thumbs-up|赞
thumb-down|thumbs-down|踩
like|heart|喜欢
unlike|heart-crack|不喜欢
star|star|星标
hot|flame|热门
upload|upload|上传
download|download|下载
paid|badge-dollar-sign|付费
free|badge-check|免费
medical|cross|医疗
shield|shield|盾牌
search|search|搜索
shortcuts|zap|快捷方式
recycle|recycle|回收
excalmation|circle-alert|感叹号
question|circle-question-mark|问号
coin1|coins|硬币
coin2|circle-dollar-sign|美元
coin3|japanese-yen|人民币
coin4|circle-pound-sterling|英镑
wallet|wallet|钱包
watch|watch|手表
clock|clock|时钟
calendar-week|calendar-days|周历
calendar-month|calendar|月历
website|globe|网站
phone|smartphone|手机
tablet|tablet|平板
desktop|monitor|电脑
tv|tv|电视
cpu|cpu|处理器
safari|compass|指南针
chrome|chromium|浏览器
pie|chart-pie|饼图
bar-chart|chart-column|柱状图
line-chart|chart-line|折线图
2d|shapes|二维
3d|box|三维
contrast|contrast|对比度
texture|blend|纹理
transition|blend|渐变
animation|gallery-horizontal-end|动画
spectrogram|audio-lines|频谱
font-sans|type|字体
font-sans-serif|case-sensitive|衬线字体
font-handwritten|pen-line|手写
flag|flag|旗帜
earth|earth|地球
pin|map-pin|地点
pin-check|map-pin-check|地点确认
map|map|地图
road|route|道路
motor|bike|两轮车
rocket|rocket|火箭
airplan|plane|飞机
ship|ship|轮船
train|train-front|火车
car|car|汽车
truck|truck|卡车
water-drop|droplet|水滴
sun|sun|太阳
moon|moon|月亮
tree|tree-pine|树木
mountain|mountain|山
cloud|cloud|云
bathtub|bath|浴缸
door|door-open|门
bed|bed|床
cabinet|archive|柜子
sofa|sofa|沙发
home|house|家
store|store|商店
house|building|建筑
game|gamepad-2|游戏
weapon|swords|武器
armor|shield-check|护甲
award2|medal|奖牌
award|award|奖章
award3|badge|荣誉
crown|crown|皇冠
rophy|trophy|奖杯
gift|gift|礼物
facebook|facebook|Facebook
twitter|twitter|Twitter
instagram|instagram|Instagram
eye|eye|眼睛
bear|paw-print|熊
dog|dog|狗
cat|cat|猫
man|person-standing|男性
woman|accessibility|女性
group|users|群组
smile|smile|微笑
meh|meh|平静
frown|frown|难过
die|skull|骷髅
baby|baby|婴儿
kid|person-standing|儿童
angel|sparkles|天使
demon|@mdi|小魔鬼
hand|hand|手
brain|brain|大脑
`.trim().split('\n').map(row => row.split('|'));
const aliases = {heart: 'like', image: 'photo', face: 'smile'};
async function download(url) {
    for (let attempt = 0; ; attempt++) {
        try { const r = await fetch(url); if (!r.ok) throw Error(`${r.status}: ${url}`); return await r.text(); }
        catch (error) { if (attempt === 2) throw error; }
    }
}
const catalog = JSON.parse(await download('https://raw.githubusercontent.com/iconify/icon-sets/master/json/lucide.json'));
function vector(body, filled = false) {
    const paths = [...body.matchAll(/<(path|circle|ellipse|rect|line|polyline|polygon)\b([^>]*)\/?\s*>/g)].map(([, tag, attributes]) => {
        const a = Object.fromEntries([...attributes.matchAll(/([\w-]+)="([^"]*)"/g)].map(m => [m[1], m[2]]));
        let d = a.d;
        if (tag === 'circle' || tag === 'ellipse') { const x=+a.cx,y=+a.cy,rx=+(a.rx??a.r),ry=+(a.ry??a.r); d=`M${x-rx},${y}a${rx},${ry} 0 1,0 ${2*rx},0a${rx},${ry} 0 1,0 ${-2*rx},0`; }
        if (tag === 'line') d=`M${a.x1},${a.y1}L${a.x2},${a.y2}`;
        if (tag === 'polyline' || tag === 'polygon') d=`M${a.points.trim().split(/[ ,]+/).join(',')}${tag==='polygon'?'Z':''}`.replace(/^M([^,]+),([^,]+),/, 'M$1,$2L');
        if (tag === 'rect') { const x=+(a.x??0),y=+(a.y??0),w=+a.width,h=+a.height,r=+(a.rx??0); d=`M${x+r},${y}h${w-2*r}q${r},0 ${r},${r}v${h-2*r}q0,${r} ${-r},${r}h${2*r-w}q${-r},0 ${-r},${-r}v${2*r-h}q0,${-r} ${r},${-r}Z`; }
        if (!d) throw Error(`Unsupported SVG: ${body}`);
        return `    <path android:pathData="${d}" android:fillColor="${filled?'#FF000000':'#00000000'}"${filled?'':' android:strokeColor="#FF000000" android:strokeWidth="1.8" android:strokeLineCap="round" android:strokeLineJoin="round"'}/>`;
    });
    if (!paths.length || /\btransform=/.test(body)) throw Error('Unsupported SVG');
    return `<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">\n${paths.join('\n')}\n</vector>\n`;
}
const demon = await download('https://raw.githubusercontent.com/Templarian/MaterialDesign/master/svg/emoticon-devil-outline.svg');
for (const [id, name] of rows) {
    let icon = catalog.icons[name];
    if (!icon && catalog.aliases?.[name]) icon=catalog.icons[catalog.aliases[name].parent];
    if (name !== '@mdi' && !icon) throw Error(`Unknown Lucide icon: ${name}`);
    await fs.writeFile(path.join(root, 'app/src/main/res/drawable', `ic_category_${id.replaceAll('-', '_')||'folder'}.xml`), vector(name==='@mdi'?demon:icon.body, name==='@mdi'));
}
const kotlin = `package com.tai.oeviewer\n\n// Eagle identifiers map to open-source artwork. See assets/third-party licenses.\ninternal object FolderIcons {\n    data class Choice(val id: String, val label: String, val drawable: Int)\n    val choices = listOf(\n${rows.map(([id,,label])=>`        Choice("${id}", "${label}", R.drawable.ic_category_${id.replaceAll('-', '_')||'folder'})`).join(',\n')}\n    ) + (0..20).map { Choice("number$it", it.toString(), 0) }\n    fun normalize(id: String) = when (id) { ${Object.entries(aliases).map(([a,b])=>`"${a}" -> "${b}"`).join('; ')}; else -> id }\n    fun choice(id: String) = choices.firstOrNull { it.id == normalize(id) }\n}\n`;
await fs.writeFile(path.join(root,'app/src/main/java/com/tai/oeviewer/FolderIcons.kt'),kotlin);
for (const [name,url] of [['lucide-license.txt','https://raw.githubusercontent.com/lucide-icons/lucide/main/LICENSE'],['mdi-license.txt','https://raw.githubusercontent.com/Templarian/MaterialDesign/master/LICENSE']]) await fs.writeFile(path.join(root,'app/src/main/assets/third-party',name), await download(url));
console.log(`Generated ${rows.length + 21} folder icon choices`);
