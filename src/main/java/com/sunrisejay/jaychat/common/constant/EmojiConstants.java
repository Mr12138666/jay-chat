package com.sunrisejay.jaychat.common.constant;

import java.util.HashMap;
import java.util.Map;

/**
 * 表情常量
 * 定义常用的50个表情符号
 */
public final class EmojiConstants {

    private EmojiConstants() {
        // 工具类，禁止实例化
    }

    /**
     * 表情代码到Unicode的映射
     * 格式：:code: -> emoji
     */
    public static final Map<String, String> EMOJI_MAP = new HashMap<>();

    static {
        // 笑脸类 (10个)
        EMOJI_MAP.put(":smile:", "😊");
        EMOJI_MAP.put(":laugh:", "😂");
        EMOJI_MAP.put(":grin:", "😁");
        EMOJI_MAP.put(":joy:", "😂");
        EMOJI_MAP.put(":wink:", "😉");
        EMOJI_MAP.put(":blush:", "😊");
        EMOJI_MAP.put(":kiss:", "😘");
        EMOJI_MAP.put(":heart_eyes:", "😍");
        EMOJI_MAP.put(":sunglasses:", "😎");
        EMOJI_MAP.put(":relieved:", "😌");

        // 表情类 (10个)
        EMOJI_MAP.put(":thinking:", "🤔");
        EMOJI_MAP.put(":neutral:", "😐");
        EMOJI_MAP.put(":expressionless:", "😑");
        EMOJI_MAP.put(":roll_eyes:", "🙄");
        EMOJI_MAP.put(":confused:", "😕");
        EMOJI_MAP.put(":worried:", "😟");
        EMOJI_MAP.put(":sad:", "😢");
        EMOJI_MAP.put(":cry:", "😭");
        EMOJI_MAP.put(":angry:", "😠");
        EMOJI_MAP.put(":rage:", "😡");

        // 手势类 (10个)
        EMOJI_MAP.put(":thumbsup:", "👍");
        EMOJI_MAP.put(":thumbsdown:", "👎");
        EMOJI_MAP.put(":ok_hand:", "👌");
        EMOJI_MAP.put(":clap:", "👏");
        EMOJI_MAP.put(":wave:", "👋");
        EMOJI_MAP.put(":pray:", "🙏");
        EMOJI_MAP.put(":fist:", "✊");
        EMOJI_MAP.put(":v:", "✌️");
        EMOJI_MAP.put(":point_up:", "👆");
        EMOJI_MAP.put(":point_down:", "👇");

        // 心形类 (5个)
        EMOJI_MAP.put(":heart:", "❤️");
        EMOJI_MAP.put(":heart_red:", "❤️");
        EMOJI_MAP.put(":heart_yellow:", "💛");
        EMOJI_MAP.put(":heart_green:", "💚");
        EMOJI_MAP.put(":heart_blue:", "💙");
        EMOJI_MAP.put(":heart_purple:", "💜");
        EMOJI_MAP.put(":broken_heart:", "💔");
        EMOJI_MAP.put(":sparkling_heart:", "💖");
        EMOJI_MAP.put(":two_hearts:", "💕");

        // 符号类 (10个)
        EMOJI_MAP.put(":fire:", "🔥");
        EMOJI_MAP.put(":star:", "⭐");
        EMOJI_MAP.put(":sparkles:", "✨");
        EMOJI_MAP.put(":thunder:", "⚡");
        EMOJI_MAP.put(":check:", "✅");
        EMOJI_MAP.put(":cross:", "❌");
        EMOJI_MAP.put(":question:", "❓");
        EMOJI_MAP.put(":exclamation:", "❗");
        EMOJI_MAP.put(":zzz:", "💤");
        EMOJI_MAP.put(":100:", "💯");

        // 其他常用 (5个)
        EMOJI_MAP.put(":party:", "🎉");
        EMOJI_MAP.put(":cake:", "🎂");
        EMOJI_MAP.put(":gift:", "🎁");
        EMOJI_MAP.put(":tada:", "🎉");
        EMOJI_MAP.put(":rocket:", "🚀");
    }

    /**
     * 获取所有表情代码列表（用于前端展示）
     */
    public static String[] getAllEmojiCodes() {
        return EMOJI_MAP.keySet().toArray(new String[0]);
    }
}
