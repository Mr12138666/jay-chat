/**
 * 表情工具类
 * 定义常用表情和转换逻辑
 */

// 表情映射表（50个常用表情）
export const EMOJI_MAP: Record<string, string> = {
  // 笑脸类 (10个)
  ':smile:': '😊',
  ':laugh:': '😂',
  ':grin:': '😁',
  ':joy:': '😂',
  ':wink:': '😉',
  ':blush:': '😊',
  ':kiss:': '😘',
  ':heart_eyes:': '😍',
  ':sunglasses:': '😎',
  ':relieved:': '😌',

  // 表情类 (10个)
  ':thinking:': '🤔',
  ':neutral:': '😐',
  ':expressionless:': '😑',
  ':roll_eyes:': '🙄',
  ':confused:': '😕',
  ':worried:': '😟',
  ':sad:': '😢',
  ':cry:': '😭',
  ':angry:': '😠',
  ':rage:': '😡',

  // 手势类 (10个)
  ':thumbsup:': '👍',
  ':thumbsdown:': '👎',
  ':ok_hand:': '👌',
  ':clap:': '👏',
  ':wave:': '👋',
  ':pray:': '🙏',
  ':fist:': '✊',
  ':v:': '✌️',
  ':point_up:': '👆',
  ':point_down:': '👇',

  // 心形类 (9个)
  ':heart:': '❤️',
  ':heart_red:': '❤️',
  ':heart_yellow:': '💛',
  ':heart_green:': '💚',
  ':heart_blue:': '💙',
  ':heart_purple:': '💜',
  ':broken_heart:': '💔',
  ':sparkling_heart:': '💖',
  ':two_hearts:': '💕',

  // 符号类 (10个)
  ':fire:': '🔥',
  ':star:': '⭐',
  ':sparkles:': '✨',
  ':thunder:': '⚡',
  ':check:': '✅',
  ':cross:': '❌',
  ':question:': '❓',
  ':exclamation:': '❗',
  ':zzz:': '💤',
  ':100:': '💯',

  // 其他常用 (1个)
  ':party:': '🎉',
  ':cake:': '🎂',
  ':gift:': '🎁',
  ':tada:': '🎉',
  ':rocket:': '🚀'
}

/**
 * 获取所有表情代码列表
 */
export function getAllEmojiCodes(): string[] {
  return Object.keys(EMOJI_MAP)
}

/**
 * 获取所有表情（用于展示）
 */
export function getAllEmojis(): Array<{ code: string; emoji: string }> {
  return Object.entries(EMOJI_MAP).map(([code, emoji]) => ({ code, emoji }))
}

/**
 * 将表情代码转换为Unicode表情
 */
export function convertEmojiCode(code: string): string {
  return EMOJI_MAP[code] || code
}

/**
 * 在文本中查找表情代码并转换为表情
 */
export function convertEmojiCodesInText(text: string): string {
  if (!text) return text
  
  let result = text
  const pattern = /:([a-z_]+):/g
  
  result = result.replace(pattern, (match) => {
    return EMOJI_MAP[match] || match
  })
  
  return result
}
