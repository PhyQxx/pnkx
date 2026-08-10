package com.pnkx.common.utils.random;

import com.pnkx.common.constant.WebsiteAddressConstants;

import java.util.Random;

/**
 * NameUtils
 *
 * @author 裴浩宇
 * @version 1.0
 * @date 2023/8/18 13:48
 * @description 随机名字
 */
public class NameAndHeader {

    /**
     * 随机名字
     */
    private static final String[] nameArray = {"木英山下", "良月秋一", "不二樱尼", "千宫月梨", "岁纳京子", "加赖友香", "早间一鱼", "杉何鸦丏", "川奈樱璇", "山野原子",
            "小栗由奈", "凉宫雪月", "桐崎千棘", "川成霖子", "芥川鹤一", "樱下春树", "石原里希", "樱乃穗子", "五十岚宁宁", "桥本理绘", "蜡笔小埋", "日奈叶子", "樱野千寻",
            "川藤奈子", "村上雪荟", "上杉夏香", "一生所爱の赫萝", "雪代巴", "最上京子", "灯千代明", "朽木白哉", "奈落", "川澄舞", "四季映姬", "边里唯世", "春野樱", "安塔利亚",
            "寒蝉鸣泣之时", "蕾玥瑷雅", "江户川柯北", "公主公主", "三千院凪", "远矢莉磨", "君焰い", "五十音夏式", "支葵千里", "学园天堂", "大崎娜娜", "夏娜", "牧野留姬", "佐藤圣",
            "夕日红", "琉璃舞", "天野信之", "杀生丸", "藤纲春绯", "泉此方", "遥远时空中", "田井中律", "空の白", "幽幻紫银", "逢坂大河", "樱语冰凌", "枣真夜", "伊卡洛斯", "伊集院翼",
            "皇昂流", "金色琴弦", "夏尔", "世末歌者", "基拉", "铃木空", "辉夜姬", "神谷熏", "娜娜莉冰见", "白井", "流川枫", "水银灯", "零之使魔", "吸血鬼骑士", "希羽岚梦", "知世",
            "灰原哀", "高石川", "西行寺え幽幽子", "桔梗", "草摩由希", "本城莲", "夏目不二子", "掌中萌虎", "卡嘉莉", "蜂蜜和四叶草", "彩云国物语", "飞影", "冰雪殇璃陌梦", "血叶洛莉兰",
            "恋离飞翼", "花样男子", "洛丽塔い", "姬月的项圈", "凤天音", "希洛梦", "拾晨雨", "静木枫", "空空姬", "由贵瑛里", "恋爱情结", "翼の年代记", "两仪式", "伍晚晴", "海猫鸣泣之时",
            "小泉理沙", "雏莓", "松本乱菊", "乙坂有宇", "小松奈奈天使禁猎区", "雏森桃", "辰伶", "筱残", "折笠佐目", "夜神月", "幼姬", "七曜の贤者", "玖兰枢", "我爱罗", "阎魔爱", "千本樱",
            "旋风管家", "斩魂の庭师", "火乃香", "颜鸢", "风璃殇", "宫藤透", "羽灵", "伊文思", "绯色月下", "佐仓蜜柑"};

    /**
     * 获取随机名字
     *
     * @return 名字
     */
    public static String randomName() {
        Random random = new Random();
        return nameArray[random.nextInt(nameArray.length)];
    }

    /**
     * 获取随机头像
     *
     * @return 头像
     */
    public static String randomHeader() {
        Random random = new Random();
        return WebsiteAddressConstants.FTP_SITE_ADDRESS + "/ftp/pnkx/header/" + random.nextInt(54) + ".png";
    }

    public static void main(String[] args) {
        System.out.println(randomHeader());
    }
}
