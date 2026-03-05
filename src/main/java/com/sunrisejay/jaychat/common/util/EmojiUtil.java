package com.sunrisejay.jaychat.common.util;

import com.sunrisejay.jaychat.common.constant.EmojiConstants;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 表情工具类
 * 处理表情代码转换
 */
@Component
public class EmojiUtil {

    /**
     * 表情代码的正则表达式
     * 匹配 :code: 格式
     */
    private static final Pattern EMOJI_PATTERN = Pattern.compile(":([a-z_]+):");

    /**
     * 将消息中的表情代码转换为Unicode表情
     * 例如：:smile: -> 😊
     * 
     * @param text 包含表情代码的文本
     * @return 转换后的文本
     */
    public String convertEmojiCodes(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        Matcher matcher = EMOJI_PATTERN.matcher(text);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String code = ":" + matcher.group(1) + ":";
            String emoji = EmojiConstants.EMOJI_MAP.get(code);
            
            if (emoji != null) {
                matcher.appendReplacement(result, Matcher.quoteReplacement(emoji));
            } else {
                // 如果找不到对应的表情，保持原样
                matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group(0)));
            }
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * 检查文本中是否包含表情代码
     */
    public boolean containsEmojiCode(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        return EMOJI_PATTERN.matcher(text).find();
    }
}
