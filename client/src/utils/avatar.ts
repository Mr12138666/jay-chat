export const MAX_AVATAR_FILE_SIZE = 5 * 1024 * 1024
export const MIN_AVATAR_SIZE = 128

/**
 * Validate avatar file type, size and resolution before upload.
 */
export const validateAvatarFile = async (
  file: File,
  options?: {
    minSize?: number
    maxFileSize?: number
  }
): Promise<void> => {
  const minSize = options?.minSize ?? MIN_AVATAR_SIZE
  const maxFileSize = options?.maxFileSize ?? MAX_AVATAR_FILE_SIZE

  if (!file.type.startsWith('image/')) {
    throw new Error('只能上传图片文件')
  }

  if (file.size > maxFileSize) {
    throw new Error('图片大小不能超过5MB')
  }

  const objectUrl = URL.createObjectURL(file)
  try {
    const size = await new Promise<{ width: number; height: number }>((resolve, reject) => {
      const img = new Image()
      img.onload = () => resolve({ width: img.naturalWidth, height: img.naturalHeight })
      img.onerror = () => reject(new Error('图片读取失败，请重新选择'))
      img.src = objectUrl
    })

    if (size.width < minSize || size.height < minSize) {
      throw new Error(`头像分辨率过低，至少需要 ${minSize}x${minSize}`)
    }
  } finally {
    URL.revokeObjectURL(objectUrl)
  }
}

export const withCacheBuster = (url: string): string => {
  const separator = url.includes('?') ? '&' : '?'
  return `${url}${separator}t=${Date.now()}`
}

