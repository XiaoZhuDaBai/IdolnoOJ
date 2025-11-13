<script setup>
import { ref, computed } from 'vue'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import { SendFeedback } from '@/api/UserApi.js'

const feedback = ref({
  type: 'bug',
  title: '',
  content: '',
  contact: ''
})

const previewContent = ref('')
const showPreview = ref(false)
const loading = ref(false)
const error = ref(null)
const uploadingImage = ref(false)
const successMessage = ref('')

// 反馈类型配置
const feedbackTypes = [
  { value: 'bug', label: '🐛 问题报告', icon: '🐛', color: '#ef4444', desc: '报告系统错误或异常' },
  { value: 'feature', label: '✨ 功能建议', icon: '✨', color: '#3b82f6', desc: '提出新功能想法' },
  { value: 'improvement', label: '🚀 改进建议', icon: '🚀', color: '#10b981', desc: '优化现有功能体验' },
  { value: 'other', label: '💬 其他反馈', icon: '💬', color: '#8b5cf6', desc: '其他意见或建议' }
]

// 获取当前选中类型的配置
const currentType = computed(() => {
  return feedbackTypes.find(t => t.value === feedback.value.type) || feedbackTypes[0]
})

// 更新预览内容
const updatePreview = () => {
  const rawMarkdown = feedback.value.content
  const htmlContent = marked(rawMarkdown)
  previewContent.value = DOMPurify.sanitize(htmlContent)
}

// 将图片文件转为 Base64
const fileToBase64 = (file) => {
  return new Promise((resolve, reject) => {
    // 验证文件大小（限制 5MB）
    const maxSize = 5 * 1024 * 1024
    if (file.size > maxSize) {
      reject(new Error('图片大小不能超过 5MB'))
      return
    }

    const reader = new FileReader()
    reader.onload = (e) => resolve(e.target.result)
    reader.onerror = (e) => reject(e)
    reader.readAsDataURL(file)
  })
}

// 拖拽图片
const handleDrop = async (e) => {
  e.preventDefault()
  e.stopPropagation()

  const files = e.dataTransfer.files
  if (files && files[0] && files[0].type.startsWith('image/')) {
    await insertImageAsBase64(files[0])
  }
}

// 拖拽悬停效果
const handleDragOver = (e) => {
  e.preventDefault()
  e.stopPropagation()
}

// 粘贴图片
const handlePaste = async (e) => {
  const items = e.clipboardData.items
  for (let item of items) {
    if (item.type.startsWith('image/')) {
      e.preventDefault()
      const file = item.getAsFile()
      if (file) {
        await insertImageAsBase64(file)
      }
      break
    }
  }
}

// 插入 Base64 图片
const insertImageAsBase64 = async (file) => {
  try {
    uploadingImage.value = true
    const base64 = await fileToBase64(file)
    insertImageMarkdown(base64, file.name)
  } catch (err) {
    console.error('图片处理失败:', err)
    alert(err.message || '图片处理失败，请重试')
  } finally {
    uploadingImage.value = false
  }
}

// 选择图片文件
const selectImage = () => {
  const input = document.createElement('input')
  input.type = 'file'
  input.accept = 'image/*'
  input.onchange = async (e) => {
    const file = e.target.files[0]
    if (file) {
      await insertImageAsBase64(file)
    }
  }
  input.click()
}

// 在光标处插入文本（支持撤销）
const insertTextWithUndo = (textarea, text) => {
  if (!textarea) return false

  textarea.focus()

  // 尝试使用现代的 execCommand（支持撤销）
  try {
    document.execCommand('insertText', false, text)
    return true
  } catch (err) {
    // 降级方案：手动插入（不支持撤销）
    console.warn('execCommand not supported, using fallback')
    const start = textarea.selectionStart || 0
    const end = textarea.selectionEnd || 0
    const before = textarea.value.substring(0, start)
    const after = textarea.value.substring(end)

    textarea.value = before + text + after
    const newPos = start + text.length
    textarea.setSelectionRange(newPos, newPos)

    // 触发 input 事件以更新 v-model
    textarea.dispatchEvent(new Event('input', { bubbles: true }))
    return true
  }
}

// 在光标处插入图片 markdown
const insertImageMarkdown = (base64, fileName = '图片') => {
  const insert = `![${fileName}](${base64})\n`

  // 使用 nextTick 确保 DOM 更新后再查询
  setTimeout(() => {
    const textarea = document.querySelector('.markdown-editor')

    if (!textarea) {
      console.warn('Textarea not found')
      return
    }

    // 使用支持撤销的插入方法
    const success = insertTextWithUndo(textarea, insert)

    if (!success) {
      // 如果插入失败，追加到末尾
      feedback.value.content += (feedback.value.content ? '\n' : '') + insert
      setTimeout(() => {
        textarea.focus()
        const newPos = feedback.value.content.length
        textarea.setSelectionRange(newPos, newPos)
      }, 0)
    }

    updatePreview()
  }, 0)
}

// 插入 Markdown 语法
const insertMarkdown = (type) => {
  const textarea = document.querySelector('.markdown-editor')

  // 如果 textarea 不存在，直接返回
  if (!textarea) {
    console.warn('Markdown editor not found')
    return
  }

  const start = textarea.selectionStart || 0
  const end = textarea.selectionEnd || 0
  const selectedText = textarea.value.substring(start, end)
  let insert = ''
  let cursorOffset = 0

  switch (type) {
    case 'bold':
      insert = `**${selectedText || '加粗文本'}**`
      cursorOffset = selectedText ? 0 : 2  // 如果有选中文本，光标不移动；否则移动到中间
      break
    case 'italic':
      insert = `*${selectedText || '斜体文本'}*`
      cursorOffset = selectedText ? 0 : 1
      break
    case 'code':
      insert = `\`${selectedText || '代码'}\``
      cursorOffset = selectedText ? 0 : 1
      break
    case 'codeblock':
      insert = `\n\`\`\`\n${selectedText || '// 代码块'}\n\`\`\`\n`
      cursorOffset = selectedText ? 0 : 5
      break
    case 'link':
      insert = `[${selectedText || '链接文本'}](url)`
      cursorOffset = selectedText ? insert.length - 4 : 6
      break
    case 'heading':
      insert = `### ${selectedText || '标题'}`
      cursorOffset = selectedText ? 0 : 4
      break
    case 'list':
      insert = `- ${selectedText || '列表项'}`
      cursorOffset = selectedText ? 0 : 2
      break
    case 'quote':
      insert = `> ${selectedText || '引用文本'}`
      cursorOffset = selectedText ? 0 : 2
      break
  }

  // 使用支持撤销的插入方法
  insertTextWithUndo(textarea, insert)

  // 如果没有选中文本，将光标移动到占位文本中间
  if (!selectedText && cursorOffset > 0) {
    setTimeout(() => {
      const newPos = start + cursorOffset
      textarea.setSelectionRange(newPos, newPos)
    }, 0)
  }

  updatePreview()
}

// 清空表单
const resetForm = () => {
  feedback.value = {
    type: 'bug',
    title: '',
    content: '',
    contact: ''
  }
  previewContent.value = ''
  showPreview.value = false
  error.value = null
  successMessage.value = ''
}

// 表单验证
const isValid = computed(() => {
  return feedback.value.title.trim() !== '' &&
         feedback.value.content.trim() !== '' &&
         feedback.value.content.length >= 10
})

// 字符统计
const contentLength = computed(() => feedback.value.content.length)
const titleLength = computed(() => feedback.value.title.length)

// 提交反馈
const submitFeedback = async () => {
  if (!isValid.value) return

  loading.value = true
  error.value = null
  successMessage.value = ''

  try {
    await SendFeedback({
      type: feedback.value.type,
      title: feedback.value.title,
      content: feedback.value.content,
      contact: feedback.value.contact
    })

    successMessage.value = '感谢您的反馈！我们会认真处理您的意见。'

    // 3秒后清空表单
    setTimeout(() => {
      resetForm()
    }, 3000)
  } catch (err) {
    console.error('提交失败:', err)
    error.value = '提交失败，请检查网络连接后重试'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="feedback-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-content">
        <h1 class="page-title">📝 反馈与建议</h1>
        <p class="page-subtitle">您的每一条反馈都是我们前进的动力</p>
      </div>
    </div>

    <!-- 主要内容区 -->
    <div class="feedback-content">
      <!-- 成功提示 -->
      <transition name="fade">
        <div v-if="successMessage" class="success-alert">
          <div class="alert-icon">✅</div>
          <div class="alert-content">
            <h3>提交成功</h3>
            <p>{{ successMessage }}</p>
          </div>
        </div>
      </transition>

      <!-- 错误提示 -->
      <transition name="fade">
        <div v-if="error" class="error-alert">
          <div class="alert-icon">❌</div>
          <div class="alert-content">
            <h3>提交失败</h3>
            <p>{{ error }}</p>
          </div>
        </div>
      </transition>

      <!-- 反馈类型选择 -->
      <div class="feedback-types">
        <div class="section-title">选择反馈类型</div>
        <div class="type-cards">
          <div
            v-for="type in feedbackTypes"
            :key="type.value"
            class="type-card"
            :class="{ active: feedback.type === type.value }"
            @click="feedback.type = type.value"
          >
            <div class="type-icon" :style="{ color: type.color }">{{ type.icon }}</div>
            <div class="type-info">
              <div class="type-label">{{ type.label }}</div>
              <div class="type-desc">{{ type.desc }}</div>
            </div>
            <div class="type-check" v-if="feedback.type === type.value">✓</div>
          </div>
        </div>
      </div>

      <!-- 表单区域 -->
      <div class="feedback-form">
        <!-- 标题输入 -->
        <div class="form-section">
          <div class="section-title">
            标题
            <span class="char-count" :class="{ warning: titleLength > 100 }">
              {{ titleLength }}/100
            </span>
          </div>
        <input
            v-model="feedback.title"
          type="text"
            class="title-input"
            placeholder="简明扼要地描述您的反馈内容..."
            maxlength="100"
          />
        </div>

        <!-- 内容编辑 -->
        <div class="form-section">
          <div class="section-header">
            <div class="section-title">
              详细描述
              <span class="char-count" :class="{ warning: contentLength < 10 }">
                {{ contentLength }} 字符（至少 10 字）
              </span>
            </div>
            <div class="editor-tabs">
              <button
                class="tab-btn"
                :class="{ active: !showPreview }"
                @click="showPreview = false"
              >
                ✍️ 编辑
              </button>
              <button
                class="tab-btn"
                :class="{ active: showPreview }"
                @click="showPreview = true; updatePreview()"
              >
                👁️ 预览
              </button>
            </div>
      </div>

          <!-- 编辑器工具栏 -->
          <div v-if="!showPreview" class="editor-toolbar">
            <div class="toolbar-group">
              <button
                class="toolbar-btn"
                @click="insertMarkdown('bold')"
                title="加粗 (Ctrl+B)"
              >
                <strong>B</strong>
              </button>
              <button
                class="toolbar-btn"
                @click="insertMarkdown('italic')"
                title="斜体 (Ctrl+I)"
              >
                <em>I</em>
              </button>
              <button
                class="toolbar-btn"
                @click="insertMarkdown('code')"
                title="行内代码"
              >
                &lt;/&gt;
              </button>
              <button
                class="toolbar-btn"
                @click="insertMarkdown('codeblock')"
                title="代码块"
              >
                { }
              </button>
            </div>
            <div class="toolbar-divider"></div>
            <div class="toolbar-group">
              <button
                class="toolbar-btn"
                @click="insertMarkdown('heading')"
                title="标题"
              >
                H
              </button>
              <button
                class="toolbar-btn"
                @click="insertMarkdown('list')"
                title="列表"
              >
                ≡
              </button>
              <button
                class="toolbar-btn"
                @click="insertMarkdown('quote')"
                title="引用"
              >
                ❝
              </button>
              <button
                class="toolbar-btn"
                @click="insertMarkdown('link')"
                title="链接"
              >
                🔗
              </button>
            </div>
            <div class="toolbar-divider"></div>
            <div class="toolbar-group">
              <button
                class="toolbar-btn image-btn"
                @click="selectImage"
                :disabled="uploadingImage"
                title="上传图片 (支持拖拽/粘贴)"
              >
                <span v-if="!uploadingImage">🖼️</span>
                <span v-else class="loading-spinner">⏳</span>
              </button>
            </div>
          </div>

          <!-- 编辑区域 -->
          <div class="editor-area">
          <textarea
              v-if="!showPreview"
            v-model="feedback.content"
              class="content-editor markdown-editor"
              placeholder="详细描述您的反馈内容...&#10;&#10;💡 提示：&#10;• 支持 Markdown 格式&#10;• 可以直接粘贴图片（Ctrl+Z 支持撤销）&#10;• 可以拖拽图片到此区域&#10;• 图片会自动转为 Base64 编码"
            @input="updatePreview"
            @drop="handleDrop"
              @dragover="handleDragOver"
            @paste="handlePaste"
          ></textarea>
            <div
              v-else
              class="content-preview markdown-body"
              v-html="previewContent || '<p class=\'empty-preview\'>暂无内容预览</p>'"
            ></div>
        </div>
      </div>

        <!-- 联系方式 -->
        <div class="form-section">
          <div class="section-title">
            联系方式
            <span class="optional-tag">选填</span>
      </div>
        <input
            v-model="feedback.contact"
          type="text"
            class="contact-input"
            placeholder="如需回复，请留下您的邮箱或其他联系方式..."
          />
      </div>

        <!-- 操作按钮 -->
      <div class="form-actions">
          <button
            class="reset-btn"
            @click="resetForm"
            :disabled="loading"
          >
            重置
          </button>
        <button
          class="submit-btn"
          @click="submitFeedback"
            :disabled="!isValid || loading"
          >
            <span v-if="!loading">提交反馈</span>
            <span v-else class="loading-text">
              <span class="loading-spinner">⏳</span>
              提交中...
            </span>
        </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* ==================== 全局样式 ==================== */
.feedback-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 24px;
}

/* ==================== 页面头部 ==================== */
.page-header {
  text-align: center;
  margin-bottom: 32px;
}

.header-content {
  animation: fadeInDown 0.6s ease;
}

.page-title {
  font-size: 36px;
  font-weight: 700;
  color: white;
  margin: 0 0 12px 0;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.page-subtitle {
  font-size: 16px;
  color: rgba(255, 255, 255, 0.9);
  margin: 0;
}

/* ==================== 内容区域 ==================== */
.feedback-content {
  max-width: 900px;
  margin: 0 auto;
  animation: fadeInUp 0.6s ease;
}

/* ==================== 提示框 ==================== */
.success-alert,
.error-alert {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  margin-bottom: 24px;
  border-radius: 12px;
  background: white;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  animation: slideInDown 0.4s ease;
}

.success-alert {
  border-left: 4px solid #10b981;
}

.error-alert {
  border-left: 4px solid #ef4444;
}

.alert-icon {
  font-size: 32px;
  flex-shrink: 0;
}

.alert-content h3 {
  margin: 0 0 8px 0;
  font-size: 18px;
  color: #1f2937;
}

.alert-content p {
  margin: 0;
  font-size: 14px;
  color: #6b7280;
}

/* ==================== 反馈类型卡片 ==================== */
.feedback-types {
  background: white;
  border-radius: 16px;
  padding: 24px;
  margin-bottom: 24px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.type-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 12px;
}

.type-card {
  position: relative;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  border: 2px solid #e5e7eb;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
  background: #fafafa;
}

.type-card:hover {
  border-color: #667eea;
  background: #f8f9ff;
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(102, 126, 234, 0.2);
}

.type-card.active {
  border-color: #667eea;
  background: linear-gradient(135deg, #667eea15, #764ba215);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.25);
}

.type-icon {
  font-size: 28px;
  flex-shrink: 0;
}

.type-info {
  flex: 1;
}

.type-label {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 4px;
}

.type-desc {
  font-size: 12px;
  color: #6b7280;
}

.type-check {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: #667eea;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: bold;
}

/* ==================== 表单区域 ==================== */
.feedback-form {
  background: white;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.form-section {
  margin-bottom: 24px;
}

.form-section:last-of-type {
  margin-bottom: 32px;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.char-count {
  font-size: 12px;
  color: #6b7280;
  font-weight: normal;
  margin-left: 8px;
}

.char-count.warning {
  color: #f59e0b;
}

.optional-tag {
  font-size: 12px;
  font-weight: normal;
  color: #9ca3af;
  background: #f3f4f6;
  padding: 2px 8px;
  border-radius: 4px;
  margin-left: 8px;
}

/* ==================== 输入框 ==================== */
.title-input,
.contact-input {
  width: 100%;
  padding: 12px 16px;
  border: 2px solid #e5e7eb;
  border-radius: 8px;
  font-size: 15px;
  color: #1f2937;
  transition: all 0.3s ease;
  background: #fafafa;
}

.title-input:focus,
.contact-input:focus {
  outline: none;
  border-color: #667eea;
  background: white;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.title-input::placeholder,
.contact-input::placeholder {
  color: #9ca3af;
}

/* ==================== 编辑器标签 ==================== */
.editor-tabs {
  display: flex;
  gap: 8px;
}

.tab-btn {
  padding: 6px 12px;
  border: none;
  background: #f3f4f6;
  color: #6b7280;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.tab-btn:hover {
  background: #e5e7eb;
  color: #1f2937;
}

.tab-btn.active {
  background: #667eea;
  color: white;
}

/* ==================== 编辑器工具栏 ==================== */
.editor-toolbar {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 12px;
  background: #f9fafb;
  border: 2px solid #e5e7eb;
  border-bottom: none;
  border-radius: 8px 8px 0 0;
}

.toolbar-group {
  display: flex;
  gap: 4px;
}

.toolbar-divider {
  width: 1px;
  height: 24px;
  background: #d1d5db;
  margin: 0 8px;
}

.toolbar-btn {
  min-width: 32px;
  height: 32px;
  padding: 0 8px;
  border: 1px solid #d1d5db;
  background: white;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s ease;
  font-size: 14px;
  color: #4b5563;
  display: flex;
  align-items: center;
  justify-content: center;
}

.toolbar-btn:hover:not(:disabled) {
  border-color: #667eea;
  color: #667eea;
  background: #f8f9ff;
}

.toolbar-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.toolbar-btn strong,
.toolbar-btn em {
  font-style: normal;
  font-weight: bold;
}

.loading-spinner {
  display: inline-block;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* ==================== 编辑器区域 ==================== */
.editor-area {
  border: 2px solid #e5e7eb;
  border-radius: 0 0 8px 8px;
  overflow: hidden;
  background: white;
}

.editor-toolbar + .editor-area {
  border-radius: 0 0 8px 8px;
  border-top: none;
}

.content-editor {
  width: 100%;
  min-height: 300px;
  padding: 16px;
  border: none;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 14px;
  line-height: 1.6;
  color: #1f2937;
  resize: vertical;
  background: white;
}

.content-editor:focus {
  outline: none;
}

.content-editor::placeholder {
  color: #9ca3af;
}

.content-preview {
  min-height: 300px;
  padding: 16px;
  overflow-y: auto;
  max-height: 600px;
}

.empty-preview {
  color: #9ca3af;
  text-align: center;
  padding: 40px;
}

/* ==================== 操作按钮 ==================== */
.form-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  padding-top: 8px;
}

.reset-btn,
.submit-btn {
  padding: 12px 32px;
  border: none;
  border-radius: 8px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  gap: 8px;
}

.reset-btn {
  background: #f3f4f6;
  color: #6b7280;
}

.reset-btn:hover:not(:disabled) {
  background: #e5e7eb;
  color: #1f2937;
}

.submit-btn {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.submit-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(102, 126, 234, 0.5);
}

.submit-btn:disabled,
.reset-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
}

.loading-text {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* ==================== Markdown 预览样式 ==================== */
.markdown-body {
  font-size: 15px;
  line-height: 1.7;
  color: #1f2937;
}

.markdown-body h1,
.markdown-body h2,
.markdown-body h3,
.markdown-body h4,
.markdown-body h5,
.markdown-body h6 {
  margin-top: 24px;
  margin-bottom: 12px;
  font-weight: 600;
  line-height: 1.3;
  color: #111827;
}

.markdown-body h1 { font-size: 28px; border-bottom: 2px solid #e5e7eb; padding-bottom: 8px; }
.markdown-body h2 { font-size: 24px; border-bottom: 1px solid #e5e7eb; padding-bottom: 6px; }
.markdown-body h3 { font-size: 20px; }
.markdown-body h4 { font-size: 18px; }

.markdown-body p {
  margin: 12px 0;
}

.markdown-body code {
  padding: 3px 6px;
  margin: 0 2px;
  font-size: 90%;
  background: #f3f4f6;
  border: 1px solid #e5e7eb;
  border-radius: 4px;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  color: #dc2626;
}

.markdown-body pre {
  padding: 16px;
  margin: 16px 0;
  overflow: auto;
  font-size: 13px;
  line-height: 1.5;
  background: #1f2937;
  border-radius: 8px;
  color: #e5e7eb;
}

.markdown-body pre code {
  padding: 0;
  margin: 0;
  background: transparent;
  border: none;
  color: inherit;
}

.markdown-body blockquote {
  padding: 12px 16px;
  margin: 16px 0;
  color: #4b5563;
  background: #f9fafb;
  border-left: 4px solid #667eea;
  border-radius: 0 6px 6px 0;
}

.markdown-body ul,
.markdown-body ol {
  padding-left: 24px;
  margin: 12px 0;
}

.markdown-body li {
  margin: 6px 0;
}

.markdown-body img {
  max-width: 100%;
  height: auto;
  border-radius: 8px;
  margin: 16px 0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.markdown-body a {
  color: #667eea;
  text-decoration: none;
  transition: color 0.2s;
}

.markdown-body a:hover {
  color: #764ba2;
  text-decoration: underline;
}

.markdown-body hr {
  border: none;
  border-top: 2px solid #e5e7eb;
  margin: 24px 0;
}

.markdown-body table {
  border-collapse: collapse;
  width: 100%;
  margin: 16px 0;
}

.markdown-body th,
.markdown-body td {
  padding: 8px 12px;
  border: 1px solid #e5e7eb;
}

.markdown-body th {
  background: #f9fafb;
  font-weight: 600;
}

/* ==================== 动画效果 ==================== */
@keyframes fadeInDown {
  from {
    opacity: 0;
    transform: translateY(-20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes slideInDown {
  from {
    opacity: 0;
    transform: translateY(-30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.fade-enter-active,
.fade-leave-active {
  transition: all 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

/* ==================== 响应式设计 ==================== */
@media (max-width: 768px) {
  .feedback-page {
    padding: 16px;
  }

  .page-title {
    font-size: 28px;
  }

  .type-cards {
    grid-template-columns: 1fr;
  }

  .feedback-form {
    padding: 16px;
  }

  .form-actions {
    flex-direction: column;
  }

  .reset-btn,
  .submit-btn {
    width: 100%;
    justify-content: center;
  }

  .editor-toolbar {
    flex-wrap: wrap;
  }

  .toolbar-divider {
    display: none;
  }
}
</style>
