<script setup lang="ts">
import { ref, watch, nextTick } from 'vue'
import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js'
import 'highlight.js/styles/github.css'

const props = defineProps<{
  content: string
}>()

const renderedHtml = ref('')

const md = new MarkdownIt({
  html: false,
  xhtmlOut: false,
  linkify: true,
  typographer: true,
  breaks: false,
  langPrefix: 'language-',
  highlight: (str: string, lang: string) => {
    const language = (lang || '').trim().toLowerCase()
    if (language && hljs.getLanguage(language)) {
      try {
        return hljs.highlight(str, { language }).value
      } catch {
        return md.utils.escapeHtml(str)
      }
    }

    try {
      return hljs.highlightAuto(str).value
    } catch {
      return md.utils.escapeHtml(str)
    }
  }
})

const defaultFence = md.renderer.rules.fence || ((tokens: any, idx: any, options: any, _env: any, renderer: any) => {
  return renderer.renderToken(tokens, idx, options)
})

md.renderer.rules.fence = (tokens: any, idx: any, options: any, env: any, renderer: any) => {
  const token = tokens[idx]
  const info = token.info ? md.utils.unescapeAll(token.info).trim() : ''
  const langName = info ? info.split(/\s+/g)[0].toLowerCase() : ''
  const codeId = `code-${Math.random().toString(36).slice(2, 11)}`
  const codeContent = encodeURIComponent(token.content)
  const renderedCode = defaultFence(tokens, idx, options, env, renderer)

  const langHeader = langName ? `<span class="code-language-label">${langName}</span>` : '<span></span>'

  return `<div class="code-block-wrapper">
    <div class="code-header">
      ${langHeader}
      <button class="copy-code-btn" onclick="copyCode('${codeId}')" type="button">复制</button>
    </div>
    <div class="code-content" id="${codeId}" data-code="${codeContent}">
      ${renderedCode}
    </div>
  </div>`
}

const renderMarkdown = (content: string) => {
  if (!content) return ''
  try {
    // 减少连续换行（3个以上换行改为2个）
    const normalizedContent = content.replace(/\n{3,}/g, '\n\n')
    return md.render(normalizedContent)
  } catch (error) {
    console.error('Markdown render error:', error)
    return md.utils.escapeHtml(content).replace(/\n/g, '<br>')
  }
}

const setupCopyFunction = () => {
  const win = window as Window & {
    copyCode?: (codeId: string) => Promise<void>
  }

  if (!win.copyCode) {
    win.copyCode = async (codeId: string) => {
      try {
        const codeElement = document.getElementById(codeId)
        if (!codeElement) return

        const rawCode = codeElement.getAttribute('data-code') || ''
        const codeContent = decodeURIComponent(rawCode)
        await navigator.clipboard.writeText(codeContent)

        const button = codeElement.parentElement?.querySelector('.copy-code-btn') as HTMLButtonElement | null
        if (!button) return

        const previousText = button.textContent
        button.textContent = '已复制'
        button.classList.add('copied')
        setTimeout(() => {
          button.textContent = previousText
          button.classList.remove('copied')
        }, 1000)
      } catch (error) {
        console.error('复制失败:', error)
      }
    }
  }
}

watch(
  () => props.content,
  (newContent) => {
    renderedHtml.value = renderMarkdown(newContent || '')
    nextTick(() => setupCopyFunction())
  },
  { immediate: true }
)
</script>

<template>
  <div class="markdown-content" v-html="renderedHtml"></div>
</template>

<style scoped>
.markdown-content {
  word-wrap: break-word;
  line-height: 1.5;
  min-width: 100px;
}

.markdown-content :deep(p) {
  margin: 4px 0;
  color: white;
}

.markdown-content :deep(h1),
.markdown-content :deep(h2),
.markdown-content :deep(h3) {
  margin: 12px 0 8px;
  font-weight: 600;
  color: white;
}

.markdown-content :deep(ul),
.markdown-content :deep(ol) {
  margin: 8px 0;
  padding-left: 20px;
  color: white;
}

.markdown-content :deep(blockquote) {
  border-left: 3px solid rgba(255, 255, 255, 0.3);
  padding-left: 12px;
  margin: 8px 0;
  color: rgba(255, 255, 255, 0.85);
}

.markdown-content :deep(a) {
  color: #a6c8ff;
  text-decoration: none;
}

.markdown-content :deep(a:hover) {
  text-decoration: underline;
}

.markdown-content :deep(:not(pre) > code) {
  background: rgba(255, 255, 255, 0.15);
  padding: 2px 6px;
  border-radius: 4px;
  font-family: 'Courier New', monospace;
  font-size: 0.9em;
  color: #ffe083;
}

.markdown-content :deep(.code-block-wrapper) {
  margin: 10px 0;
  border-radius: 10px;
  overflow: hidden;
  background: #ffffff;
  border: 1px solid #e5e7eb;
  display: flex;
  flex-direction: column;
}

.markdown-content :deep(.code-header) {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background: #f6f8fa;
  border-bottom: 1px solid #e5e7eb;
  order: 0;
  width: 100%;
}

.markdown-content :deep(.code-language-label) {
  font-size: 12px;
  color: #374151;
}

.markdown-content :deep(.copy-code-btn) {
  border: 1px solid #d1d5db;
  border-radius: 6px;
  padding: 4px 8px;
  font-size: 12px;
  color: #111827;
  background: #ffffff;
  cursor: pointer;
  transition: all 0.2s ease;
}

.markdown-content :deep(.copy-code-btn:hover) {
  background: #f3f4f6;
}

.markdown-content :deep(.copy-code-btn.copied) {
  background: #dcfce7;
  border-color: #86efac;
}

.markdown-content :deep(.code-content) {
  padding: 12px;
  overflow-x: auto;
  background: #ffffff;
  order: 1;
  width: 100%;
}

.markdown-content :deep(pre) {
  margin: 0;
  padding: 0;
  background: transparent;
}

.markdown-content :deep(pre code) {
  display: block;
  white-space: pre;
  font-family: 'Courier New', monospace;
  font-size: 13px;
  color: #111827;
}

.markdown-content :deep(.hljs) {
  background: transparent !important;
  color: #111827;
}

.markdown-content :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 8px 0;
}

.markdown-content :deep(th),
.markdown-content :deep(td) {
  border: 1px solid rgba(255, 255, 255, 0.2);
  padding: 6px 10px;
  text-align: left;
}

.markdown-content :deep(th) {
  background: rgba(255, 255, 255, 0.1);
}
</style>
