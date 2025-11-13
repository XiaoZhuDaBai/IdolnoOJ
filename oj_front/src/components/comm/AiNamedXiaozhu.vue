<template>
  <div class="ai-assistant">
    <div v-if="isOpen"
         class="chat-window"
         :class="{ maximized: isMaximized, dragging: isDragging }"
         :style="{
           left: isMaximized ? '2.5vw' : position.left + 'px',
           top: isMaximized ? '5vh' : position.top + 'px',
           width: isMaximized ? '95vw' : chatWindowSize.width + 'px',
           height: isMaximized ? '90vh' : chatWindowSize.height + 'px',
           display: isMinimized ? 'none' : 'flex'
         }">
      <div class="chat-header" @mousedown="startDrag">
        <div class="header-content">
          <img src="@/icon/ai.jpg" alt="AI" class="header-avatar">
          <div class="header-info">
            <span class="header-title">小助 AI</span>
            <span class="header-status">在线</span>
          </div>
        </div>
        <div class="chat-actions">
          <button class="action-btn" @click="minimizeChat" title="最小化">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <rect x="5" y="11" width="14" height="2" rx="1" fill="currentColor"/>
            </svg>
          </button>
          <button class="action-btn" @click="toggleMaximize" :title="isMaximized ? '还原' : '最大化'">
            <svg v-if="!isMaximized" width="16" height="16" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <rect x="4" y="4" width="16" height="16" rx="2" stroke="currentColor" stroke-width="2" fill="none"/>
            </svg>
            <svg v-else width="16" height="16" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <rect x="7" y="7" width="10" height="10" rx="2" stroke="currentColor" stroke-width="2" fill="none"/>
            </svg>
          </button>
          <button class="action-btn close" @click="closeChat" title="关闭">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M18 6L6 18M6 6l12 12" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
            </svg>
          </button>
        </div>
      </div>
      <div class="messages" ref="messagesContainer">
        <div v-if="messages.length === 0" class="empty-state">
          <img src="@/icon/ai.jpg" alt="AI" class="empty-avatar">
          <p class="empty-title">你好！我是小助 👋</p>
          <p class="empty-subtitle">有什么可以帮到你的吗？</p>
        </div>
        <div v-for="(message, index) in messages"
             :key="index"
             :class="['chat-item', message.type]">
          <img v-if="message.type === 'ai'" src="@/icon/ai.jpg" alt="AI Avatar" class="avatar">
          <div :class="['message-bubble', message.type, { 'streaming': message.isStreaming }]">
            <div v-if="message.type === 'ai'" v-html="marked(message.content)" class="message-content"></div>
            <div v-else class="message-content">{{ message.content }}</div>
            <span v-if="message.isStreaming" class="typing-cursor"></span>
          </div>
        </div>
      </div>
      <div class="input-area">
        <div class="input-wrapper">
          <input
            v-model="inputMessage"
            @keyup.enter="sendMessage"
            placeholder="输入消息..."
            :disabled="isLoading"
          >
          <button
            v-if="!isLoading"
            @click="sendMessage"
            :disabled="!inputMessage.trim()"
            class="send-btn"
          >
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M22 2L11 13M22 2l-7 20-4-9-9-4 20-7z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </button>
          <button
            v-else
            @click="cancelCurrentRequest"
            class="cancel-btn"
          >
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <rect x="6" y="6" width="12" height="12" rx="2" fill="currentColor"/>
            </svg>
          </button>
        </div>
      </div>
      <div class="resize-handle" @mousedown="startResize"></div>
    </div>
    <button v-else-if="isMinimized"
            class="float-btn"
            @click="restoreChat"
            :class="{ 'disabled': !isLoggedIn }">
      <img src="@/icon/ai.jpg" alt="AI Icon" class="ai-icon-img">
      <div v-if="!isLoggedIn" class="login-overlay">
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
          <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z" fill="white"/>
        </svg>
        <span>请先登录</span>
      </div>
    </button>
    <button v-else
            class="float-btn"
            @click="openChat"
            :class="{ 'disabled': !isLoggedIn }">
      <img src="@/icon/ai.jpg" alt="AI Icon" class="ai-icon-img">
      <div v-if="!isLoggedIn" class="login-overlay">
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
          <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z" fill="white"/>
        </svg>
        <span>请先登录</span>
      </div>
      <div v-else class="online-badge"></div>
    </button>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted, onUnmounted, computed } from 'vue'
import { marked } from 'marked'
import { useUserStore } from '@/stores/userStore'

// 获取用户状态
const userStore = useUserStore()
const isLoggedIn = computed(() => userStore.isLogin)
const currentUser = computed(() => userStore.user)

const isOpen = ref(false)
const isMaximized = ref(false)
const isMinimized = ref(false)
const isDragging = ref(false)
const isLoading = ref(false)
const currentAiMessage = ref('')
const inputMessage = ref('')
const messages = ref([])
const messagesContainer = ref(null)
const chatWindowSize = ref({ width: 350, height: 500 })
const lastSize = ref({ width: 350, height: 500 })
const position = ref({ left: window.innerWidth - 370, top: window.innerHeight - 520 })
const dragState = ref({ startX: 0, startY: 0, offsetX: 0, offsetY: 0 })
const resizeState = ref({ resizing: false, startX: 0, startY: 0, startWidth: 0, startHeight: 0 })

const openChat = () => {
  // 检查用户是否已登录
  if (!isLoggedIn.value) {
    // 显示登录提示
    alert('请先登录后再使用AI助手')
    return
  }
  isOpen.value = true
  isMinimized.value = false
}
const closeChat = () => {
  isOpen.value = false
}
const minimizeChat = () => {
  isMinimized.value = true
  isOpen.value = false
}
const restoreChat = () => {
  // 检查用户是否已登录
  if (!isLoggedIn.value) {
    alert('请先登录后再使用AI助手')
    return
  }
  isMinimized.value = false
  isOpen.value = true
}
const toggleMaximize = () => {
  if (!isMaximized.value) {
    lastSize.value = { ...chatWindowSize.value }
    isMaximized.value = true
  } else {
    isMaximized.value = false
    chatWindowSize.value = { ...lastSize.value }
  }
}
const startDrag = (e) => {
  if (isMaximized.value) return
  isDragging.value = true
  dragState.value = {
    startX: e.clientX,
    startY: e.clientY,
    offsetX: e.clientX - position.value.left,
    offsetY: e.clientY - position.value.top
  }
  document.body.style.userSelect = 'none'
}
const handleMouseMove = (e) => {
  if (isDragging.value) {
    position.value.left = Math.max(0, Math.min(e.clientX - dragState.value.offsetX, window.innerWidth - chatWindowSize.value.width))
    position.value.top = Math.max(0, Math.min(e.clientY - dragState.value.offsetY, window.innerHeight - chatWindowSize.value.height))
  }
  if (resizeState.value.resizing) {
    const dx = e.clientX - resizeState.value.startX
    const dy = e.clientY - resizeState.value.startY
    chatWindowSize.value.width = Math.max(300, resizeState.value.startWidth + dx)
    chatWindowSize.value.height = Math.max(350, resizeState.value.startHeight + dy)
  }
}
const handleMouseUp = () => {
  isDragging.value = false
  resizeState.value.resizing = false
  document.body.style.userSelect = ''
}
const startResize = (e) => {
  if (isMaximized.value) return
  resizeState.value = {
    resizing: true,
    startX: e.clientX,
    startY: e.clientY,
    startWidth: chatWindowSize.value.width,
    startHeight: chatWindowSize.value.height
  }
  e.stopPropagation()
  document.body.style.userSelect = 'none'
}

const scrollToBottom = async () => {
  await nextTick()
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

// 使用 AbortController 来管理请求取消
const abortController = ref(null)

// 处理单个 SSE 数据块
const processSSEChunk = (chunk, lastMessage) => {
  try {
    const jsonStr = chunk.slice(5).trim() // 移除 'data:' 前缀
    if (!jsonStr) return false

    const data = JSON.parse(jsonStr)
    if (!data.output) return false

    // 处理完成信号
    if (data.output.finish_reason === 'stop') {
      return 'finished'
    }

    // 处理文本内容
    if (data.output.text) {
      currentAiMessage.value += data.output.text
      if (lastMessage) {
        lastMessage.content = currentAiMessage.value
      }
      return true
    }

    return false
  } catch (e) {
    console.error('解析 SSE 数据失败:', e, '原始数据:', chunk)
    return false
  }
}

// 使用 ReadableStream 处理流式响应
const handleStreamResponse = async (reader, lastMessage) => {
  try {
    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()

      if (done) {
        // 处理缓冲区中剩余的数据
        if (buffer.trim()) {
          const lines = buffer.split('\n')
          for (const line of lines) {
            if (line.startsWith('data:')) {
              processSSEChunk(line, lastMessage)
            }
          }
        }
        break
      }

      // 将新数据添加到缓冲区
      const chunk = new TextDecoder().decode(value)
      buffer += chunk

      // 按行处理数据
      const lines = buffer.split('\n')

      // 保留最后一行（可能不完整）
      buffer = lines.pop() || ''

      // 处理完整的行
      let isFinished = false
      for (const line of lines) {
        if (line.startsWith('data:')) {
          const result = processSSEChunk(line, lastMessage)
          if (result === 'finished') {
            isFinished = true
            break
          }
        }
      }

      if (isFinished) break

      // 滚动到底部
      await scrollToBottom()
    }

    // 完成处理
    if (lastMessage) {
      lastMessage.isStreaming = false
    }
    isLoading.value = false
    currentAiMessage.value = ''
    await scrollToBottom()

  } catch (error) {
    console.error('处理流数据时出错:', error)
    if (lastMessage) {
      lastMessage.content = currentAiMessage.value || '抱歉，处理响应时出错。'
      lastMessage.isStreaming = false
    }
    isLoading.value = false
  }
}

const sendMessage = async () => {
  if (!inputMessage.value.trim()) return

  // 检查用户是否已登录
  if (!isLoggedIn.value) {
    alert('请先登录后再使用AI助手')
    return
  }

  // 如果有正在进行的请求，取消它
  if (abortController.value) {
    abortController.value.abort()
  }

  // 创建新的 AbortController
  abortController.value = new AbortController()

  messages.value.push({
    type: 'user',
    content: inputMessage.value
  })

  const userMessage = inputMessage.value
  inputMessage.value = ''
  isLoading.value = true
  currentAiMessage.value = ''

  // 添加 AI 消息占位符
  messages.value.push({
    type: 'ai',
    content: '',
    isStreaming: true
  })

  await scrollToBottom()

  try {
    // 获取用户UUID
    const uuid = currentUser.value?.uuid

    if (!uuid) {
      throw new Error('用户信息不完整，无法发送请求')
    }

    // 使用 fetch API 直接处理流式响应
    const response = await fetch('/api/ai/chat', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        prompt: userMessage,
        uuid: uuid  // 添加用户UUID
      }),
      signal: abortController.value.signal
    })

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`)
    }

    const reader = response.body.getReader()
    const lastMessage = messages.value[messages.value.length - 1]

    // 处理流式响应
    await handleStreamResponse(reader, lastMessage)

  } catch (error) {
    // 如果是取消请求导致的错误，不做处理
    if (error.name === 'AbortError') {
      console.log('请求已被取消')
      return
    }

    console.error('AI回复失败:', error)
    const lastMessage = messages.value[messages.value.length - 1]
    if (lastMessage && lastMessage.type === 'ai') {
      lastMessage.content = '抱歉，我现在无法回答这个问题。请稍后再试。'
      lastMessage.isStreaming = false
    }
    isLoading.value = false
    currentAiMessage.value = ''
    await scrollToBottom()
  } finally {
    abortController.value = null
  }
}

// 取消当前请求的方法
const cancelCurrentRequest = () => {
  if (abortController.value) {
    abortController.value.abort()
    abortController.value = null
    isLoading.value = false

    // 更新最后一条消息
    const lastMessage = messages.value[messages.value.length - 1]
    if (lastMessage && lastMessage.type === 'ai' && lastMessage.isStreaming) {
      lastMessage.content += '\n\n[用户已取消请求]'
      lastMessage.isStreaming = false
    }
  }
}

onMounted(() => {
  window.addEventListener('mousemove', handleMouseMove)
  window.addEventListener('mouseup', handleMouseUp)

  // 如果组件挂载时已经打开，但用户未登录，则关闭聊天窗口
  if (isOpen.value && !isLoggedIn.value) {
    isOpen.value = false
  }

  // 不再自动添加欢迎消息，改为空状态显示
})
onUnmounted(() => {
  window.removeEventListener('mousemove', handleMouseMove)
  window.removeEventListener('mouseup', handleMouseUp)
})
</script>

<style scoped>
.ai-assistant {
  position: fixed;
  bottom: 24px;
  right: 24px;
  z-index: 1000;
}

/* 浮动按钮 */
.float-btn {
  position: fixed;
  bottom: 24px;
  right: 24px;
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--color-primary) 0%, #5b7fc9 100%);
  border: none;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(51, 65, 85, 0.15), 0 2px 4px rgba(51, 65, 85, 0.1);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: visible;
  padding: 0;
}

.float-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(51, 65, 85, 0.2), 0 4px 8px rgba(51, 65, 85, 0.12);
}

.float-btn:active {
  transform: translateY(0);
}

.float-btn.disabled {
  opacity: 0.6;
  cursor: not-allowed;
  filter: grayscale(0.3);
}

.float-btn.disabled:hover {
  transform: none;
  box-shadow: 0 4px 12px rgba(51, 65, 85, 0.15);
}

.ai-icon-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 50%;
}

.online-badge {
  position: absolute;
  bottom: 4px;
  right: 4px;
  width: 14px;
  height: 14px;
  background-color: var(--color-success);
  border: 2px solid white;
  border-radius: 50%;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

.login-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.75);
  backdrop-filter: blur(4px);
  border-radius: 50%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  gap: 4px;
  color: white;
  font-size: 11px;
  font-weight: 500;
  text-align: center;
}

/* 聊天窗口 */
.chat-window {
  position: fixed;
  z-index: 1001;
  min-width: 360px;
  min-height: 500px;
  max-width: 90vw;
  max-height: 90vh;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.12), 0 8px 16px rgba(0, 0, 0, 0.08);
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid var(--color-border);
}

.chat-window.maximized {
  width: 95vw !important;
  height: 90vh !important;
  left: 2.5vw !important;
  top: 5vh !important;
  border-radius: var(--radius-md);
}

.chat-header {
  padding: 16px 20px;
  background: linear-gradient(135deg, var(--color-primary) 0%, #5b7fc9 100%);
  color: white;
  display: flex;
  justify-content: space-between;
  align-items: center;
  cursor: move;
  user-select: none;
  flex-shrink: 0;
}

.header-content {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid rgba(255, 255, 255, 0.3);
}

.header-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.header-title {
  font-size: 15px;
  font-weight: 600;
  letter-spacing: -0.2px;
}

.header-status {
  font-size: 11px;
  opacity: 0.9;
  font-weight: 400;
}

.chat-actions {
  display: flex;
  gap: 4px;
}

.action-btn {
  width: 32px;
  height: 32px;
  border-radius: var(--radius-sm);
  background: rgba(255, 255, 255, 0.15);
  border: none;
  color: white;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
  flex-shrink: 0;
}

.action-btn:hover {
  background: rgba(255, 255, 255, 0.25);
}

.action-btn.close:hover {
  background: rgba(239, 68, 68, 0.9);
}

.resize-handle {
  position: absolute;
  width: 20px;
  height: 20px;
  right: 0;
  bottom: 0;
  cursor: se-resize;
  z-index: 10;
  opacity: 0;
  transition: opacity 0.2s;
}

.chat-window:hover .resize-handle {
  opacity: 1;
}

.resize-handle::after {
  content: '';
  position: absolute;
  right: 4px;
  bottom: 4px;
  width: 12px;
  height: 12px;
  border-right: 2px solid var(--color-border);
  border-bottom: 2px solid var(--color-border);
}
/* 消息区域 */
.messages {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 16px;
  background: var(--color-bg);
  scroll-behavior: smooth;
}

.messages::-webkit-scrollbar {
  width: 6px;
}

.messages::-webkit-scrollbar-track {
  background: transparent;
}

.messages::-webkit-scrollbar-thumb {
  background: var(--color-border);
  border-radius: 3px;
}

.messages::-webkit-scrollbar-thumb:hover {
  background: var(--color-text-tertiary);
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  gap: 12px;
  opacity: 0.8;
}

.empty-avatar {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  object-fit: cover;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.empty-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--color-text);
  margin: 0;
}

.empty-subtitle {
  font-size: 14px;
  color: var(--color-text-secondary);
  margin: 0;
}

/* 聊天项 */
.chat-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  width: 100%;
  animation: slideIn 0.3s ease;
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.chat-item.user {
  flex-direction: row-reverse;
  justify-content: flex-start;
}

.chat-item.ai {
  flex-direction: row;
  justify-content: flex-start;
}

.avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

/* 消息气泡 */
.message-bubble {
  max-width: 75%;
  padding: 12px 16px;
  border-radius: var(--radius-lg);
  word-break: break-word;
  position: relative;
  font-size: 14px;
  line-height: 1.6;
}

.message-bubble.user {
  background: var(--color-primary);
  color: white;
  border-bottom-right-radius: 4px;
  box-shadow: 0 2px 8px rgba(51, 65, 85, 0.15);
}

.message-bubble.ai {
  background: white;
  color: var(--color-text);
  border-bottom-left-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  border: 1px solid var(--color-border);
}

.message-bubble.streaming {
  background: white;
  border: 1px solid var(--color-primary);
  border-style: dashed;
}

.message-content {
  white-space: pre-wrap;
  overflow-wrap: break-word;
}

.typing-cursor {
  display: inline-block;
  width: 2px;
  height: 1em;
  background-color: var(--color-primary);
  margin-left: 2px;
  animation: blink 1s infinite;
  vertical-align: middle;
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}

/* 输入区域 */
.input-area {
  padding: 16px 20px;
  border-top: 1px solid var(--color-border);
  background: var(--color-surface);
  flex-shrink: 0;
}

.input-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
  background: white;
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 4px 4px 4px 16px;
  transition: all 0.2s ease;
}

.input-wrapper:focus-within {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(51, 65, 85, 0.1);
}

.input-area input {
  flex: 1;
  padding: 10px 0;
  border: none;
  outline: none;
  font-size: 14px;
  color: var(--color-text);
  background: transparent;
}

.input-area input::placeholder {
  color: var(--color-text-tertiary);
}

.input-area input:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.send-btn,
.cancel-btn {
  width: 40px;
  height: 40px;
  border-radius: var(--radius-md);
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: all 0.2s ease;
}

.send-btn {
  background: var(--color-primary);
  color: white;
}

.send-btn:hover:not(:disabled) {
  background: #2d3f5f;
  transform: scale(1.05);
}

.send-btn:disabled {
  background: var(--color-border);
  cursor: not-allowed;
  opacity: 0.5;
}

.cancel-btn {
  background: var(--color-error);
  color: white;
}

.cancel-btn:hover {
  background: #dc2626;
  transform: scale(1.05);
}

/* Markdown 样式 */
.message-bubble.ai :deep(h1) {
  font-size: 1.4em;
  margin: 0.8em 0 0.4em;
  font-weight: 600;
  color: var(--color-text);
}

.message-bubble.ai :deep(h2) {
  font-size: 1.25em;
  margin: 0.7em 0 0.4em;
  font-weight: 600;
  color: var(--color-text);
}

.message-bubble.ai :deep(h3) {
  font-size: 1.1em;
  margin: 0.6em 0 0.3em;
  font-weight: 600;
  color: var(--color-text);
}

.message-bubble.ai :deep(p) {
  margin: 0.5em 0;
  line-height: 1.7;
}

.message-bubble.ai :deep(p:first-child) {
  margin-top: 0;
}

.message-bubble.ai :deep(p:last-child) {
  margin-bottom: 0;
}

.message-bubble.ai :deep(ul),
.message-bubble.ai :deep(ol) {
  margin: 0.5em 0;
  padding-left: 1.5em;
}

.message-bubble.ai :deep(li) {
  margin: 0.3em 0;
  line-height: 1.6;
}

.message-bubble.ai :deep(code) {
  background-color: var(--color-bg);
  color: var(--color-primary);
  padding: 0.2em 0.5em;
  border-radius: 4px;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 0.9em;
  border: 1px solid var(--color-border);
}

.message-bubble.ai :deep(pre) {
  background-color: #1e293b;
  color: #e2e8f0;
  padding: 1em;
  border-radius: var(--radius-md);
  overflow-x: auto;
  margin: 0.8em 0;
  border: 1px solid var(--color-border);
}

.message-bubble.ai :deep(pre code) {
  background: none;
  color: inherit;
  padding: 0;
  border: none;
  font-size: 0.9em;
}

.message-bubble.ai :deep(blockquote) {
  border-left: 3px solid var(--color-primary);
  margin: 0.8em 0;
  padding-left: 1em;
  color: var(--color-text-secondary);
  font-style: italic;
}

.message-bubble.ai :deep(hr) {
  border: none;
  border-top: 1px solid var(--color-border);
  margin: 1em 0;
}

.message-bubble.ai :deep(strong) {
  font-weight: 600;
  color: var(--color-text);
}

.message-bubble.ai :deep(em) {
  font-style: italic;
}

.message-bubble.ai :deep(a) {
  color: var(--color-primary);
  text-decoration: none;
  border-bottom: 1px solid transparent;
  transition: border-color 0.2s;
}

.message-bubble.ai :deep(a:hover) {
  border-bottom-color: var(--color-primary);
}

.message-bubble.ai :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 0.8em 0;
  font-size: 0.9em;
}

.message-bubble.ai :deep(th),
.message-bubble.ai :deep(td) {
  border: 1px solid var(--color-border);
  padding: 0.5em;
  text-align: left;
}

.message-bubble.ai :deep(th) {
  background-color: var(--color-bg);
  font-weight: 600;
}
</style>
