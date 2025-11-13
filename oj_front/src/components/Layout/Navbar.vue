<template>
  <nav class="navbar">
    <div class="navbar-container">
      <!-- Logo 区域 -->
      <div class="logo" @click="$router.push({ name: 'home' })">
        <img
          :src="logoUrl"
          alt="平台Logo"
          class="logo-image"
          @click.stop="showZoomedLogo = true"
        >
        <div class="logo-text">
          <span class="name">不知道取什么</span>
          <span class="small-name">在线练习平台</span>
        </div>
      </div>

      <!-- Logo 放大预览 -->
      <transition name="fade">
        <div v-if="showZoomedLogo" class="logo-zoom-overlay" @click="showZoomedLogo = false">
          <div class="zoomed-logo-container" @click.stop>
            <img :src="logoUrl" alt="平台Logo" class="zoomed-logo">
            <button class="close-btn" @click="showZoomedLogo = false">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M18 6L6 18M6 6l12 12"/>
              </svg>
            </button>
          </div>
        </div>
      </transition>

      <!-- 导航链接 -->
      <ul class="nav-links">
        <li v-for="(link, index) in navLinks" :key="index">
          <router-link :to="{ name: link.route }" class="nav-link">
            <span class="link-icon">{{ link.icon }}</span>
            <span class="link-text">{{ link.text }}</span>
          </router-link>
        </li>
      </ul>

      <!-- 用户操作区 -->
      <div class="user-actions">
        <template v-if="isLogin">
          <div class="user-menu-wrapper" ref="menuRef">
            <div class="user-info" @click.stop="toggleMenu">
              <img :src="userAvatar" class="user-avatar" alt="用户头像" />
              <span class="user-nickname">{{ userNickname }}</span>
              <svg
                class="dropdown-icon"
                :class="{ open: showMenu }"
                width="16"
                height="16"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
              >
                <path d="M6 9l6 6 6-6"/>
              </svg>
            </div>

            <transition name="dropdown">
              <div v-if="showMenu" class="user-menu">
                <div class="user-menu-header">
                  <img :src="userAvatar" class="menu-avatar" alt="用户头像" />
                  <div class="menu-user-info">
                    <div class="menu-nickname">{{ userNickname }}</div>
                    <div class="menu-email">{{ userStore.user?.email }}</div>
                  </div>
                </div>
                <div class="menu-divider"></div>
                <div class="user-menu-item" @click="goToSettings">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <circle cx="12" cy="12" r="3"/>
                    <path d="M12 1v6m0 6v6M5.64 5.64l4.24 4.24m4.24 4.24l4.24 4.24M1 12h6m6 0h6M5.64 18.36l4.24-4.24m4.24-4.24l4.24-4.24"/>
                  </svg>
                  <span>个人设置</span>
                </div>
                <div class="user-menu-item logout" @click="logout">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4M16 17l5-5-5-5M21 12H9"/>
                  </svg>
                  <span>退出登录</span>
                </div>
              </div>
            </transition>
          </div>
        </template>
        <template v-else>
          <a href="/loginORegister" class="login-btn">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4M10 17l5-5-5-5M21 12H9"/>
            </svg>
            <span>登录</span>
          </a>
        </template>
      </div>
    </div>
  </nav>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import logoImage from '@/icon/logo.jpg'
import { useUserStore } from '@/stores/userStore'
import { useRouter } from 'vue-router'

defineOptions({
  name: 'MainNavbar'
})

const logoUrl = ref(logoImage)
const showZoomedLogo = ref(false)
const userStore = useUserStore()
const router = useRouter()

const navLinks = ref([
  { text: '首页', route: 'home', icon: '🏠' },
  { text: '题库', route: 'problems', icon: '📚' },
  { text: '比赛', route: 'contests', icon: '🏆' },
  { text: '提交记录', route: 'submissions', icon: '📝' },
  { text: '反馈', route: 'feedback', icon: '💬' }
])

const isLogin = computed(() => userStore.isLogin && userStore.user)
const userAvatar = computed(() => userStore.user?.avatar || '/default-avatar.png')
const userNickname = computed(() => userStore.user?.nickname || userStore.user?.email || '用户')

const showMenu = ref(false)
const menuRef = ref(null)
const toggleMenu = () => {
  showMenu.value = !showMenu.value
}
const goToSettings = () => {
  showMenu.value = false
  router.push('/settings') // 你可以根据实际路由调整
}
const logout = () => {
  showMenu.value = false
  userStore.logout()
  router.push('/loginORegister')
}

const handleClickOutside = (event) => {
  if (menuRef.value && !menuRef.value.contains(event.target)) {
    showMenu.value = false
  }
}
onMounted(() => {
  document.addEventListener('click', handleClickOutside)
})
onBeforeUnmount(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<style scoped>
/* ==================== 导航栏主体 ==================== */
.navbar {
  position: sticky;
  top: 0;
  z-index: 999;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  box-shadow: 0 2px 16px rgba(0, 0, 0, 0.08);
  border-bottom: 1px solid rgba(99, 102, 241, 0.1);
}

.navbar-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 12px 32px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
}

/* ==================== Logo 区域 ==================== */
.logo {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  transition: transform 0.3s ease;
  user-select: none;
}

.logo:hover {
  transform: translateY(-2px);
}

.logo-image {
  height: 48px;
  width: 48px;
  object-fit: contain;
  border-radius: 12px;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(99, 102, 241, 0.2);
}

.logo-image:hover {
  transform: scale(1.1) rotate(5deg);
  box-shadow: 0 4px 16px rgba(99, 102, 241, 0.4);
}

.logo-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.logo .name {
  font-size: 18px;
  font-weight: 700;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  letter-spacing: -0.5px;
}

.logo .small-name {
  font-size: 11px;
  font-weight: 500;
  color: #6b7280;
  letter-spacing: 0.5px;
}

/* ==================== Logo 放大预览 ==================== */
.logo-zoom-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.85);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 9999;
  cursor: pointer;
  backdrop-filter: blur(4px);
}

.zoomed-logo-container {
  position: relative;
  max-width: 90vw;
  max-height: 90vh;
  padding: 32px;
  background-color: white;
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  cursor: default;
  animation: zoomIn 0.3s ease;
}

@keyframes zoomIn {
  from {
    opacity: 0;
    transform: scale(0.8);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

.zoomed-logo {
  max-width: 100%;
  max-height: 80vh;
  object-fit: contain;
  border-radius: 8px;
}

.close-btn {
  position: absolute;
  top: 16px;
  right: 16px;
  width: 36px;
  height: 36px;
  border: none;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}

.close-btn:hover {
  transform: rotate(90deg) scale(1.1);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
}

/* ==================== 导航链接 ==================== */
.nav-links {
  display: flex;
  list-style: none;
  gap: 8px;
  margin: 0;
  padding: 0;
  flex: 1;
  justify-content: center;
}

.nav-links li {
  margin: 0;
}

.nav-link {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 16px;
  color: #4b5563;
  text-decoration: none;
  font-size: 15px;
  font-weight: 500;
  border-radius: 10px;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}

.nav-link::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, #667eea15, #764ba215);
  opacity: 0;
  transition: opacity 0.3s ease;
  border-radius: 10px;
}

.nav-link:hover::before {
  opacity: 1;
}

.nav-link:hover {
  color: #667eea;
  transform: translateY(-2px);
}

.nav-link.router-link-active {
  color: white;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.3);
}

.nav-link.router-link-active::before {
  opacity: 0;
}

.link-icon {
  font-size: 16px;
  transition: transform 0.3s ease;
}

.nav-link:hover .link-icon {
  transform: scale(1.2);
}

.link-text {
  position: relative;
  z-index: 1;
}

/* ==================== 用户操作区 ==================== */
.user-actions {
  display: flex;
  align-items: center;
}

.login-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  text-decoration: none;
  font-size: 15px;
  font-weight: 600;
  border-radius: 10px;
  transition: all 0.3s ease;
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.3);
}

.login-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(99, 102, 241, 0.4);
}

/* ==================== 用户菜单 ==================== */
.user-menu-wrapper {
  position: relative;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 16px;
  cursor: pointer;
  border-radius: 12px;
  transition: all 0.3s ease;
  background: #f9fafb;
  border: 2px solid transparent;
}

.user-info:hover {
  background: linear-gradient(135deg, #667eea10, #764ba210);
  border-color: rgba(99, 102, 241, 0.2);
}

.user-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
}

.user-info:hover .user-avatar {
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.3);
  transform: scale(1.05);
}

.user-nickname {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dropdown-icon {
  color: #6b7280;
  transition: transform 0.3s ease;
}

.dropdown-icon.open {
  transform: rotate(180deg);
}

/* ==================== 下拉菜单 ==================== */
.user-menu {
  position: absolute;
  right: 0;
  top: calc(100% + 8px);
  min-width: 280px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.12);
  padding: 8px;
  z-index: 1000;
  border: 1px solid rgba(0, 0, 0, 0.06);
}

.user-menu-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: linear-gradient(135deg, #667eea08, #764ba208);
  border-radius: 8px;
  margin-bottom: 4px;
}

.menu-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.menu-user-info {
  flex: 1;
  overflow: hidden;
}

.menu-nickname {
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.menu-email {
  font-size: 12px;
  color: #6b7280;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.menu-divider {
  height: 1px;
  background: linear-gradient(to right, transparent, #e5e7eb, transparent);
  margin: 8px 0;
}

.user-menu-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  cursor: pointer;
  color: #4b5563;
  font-size: 14px;
  font-weight: 500;
  border-radius: 8px;
  transition: all 0.2s ease;
}

.user-menu-item:hover {
  background: #f3f4f6;
  color: #667eea;
  transform: translateX(4px);
}

.user-menu-item.logout {
  color: #ef4444;
}

.user-menu-item.logout:hover {
  background: #fef2f2;
  color: #dc2626;
}

.user-menu-item svg {
  flex-shrink: 0;
}

/* ==================== 动画效果 ==================== */
.fade-enter-active,
.fade-leave-active {
  transition: all 0.3s ease;
}

.fade-enter-from {
  opacity: 0;
}

.fade-leave-to {
  opacity: 0;
}

.dropdown-enter-active {
  animation: dropdownIn 0.3s ease;
}

.dropdown-leave-active {
  animation: dropdownOut 0.2s ease;
}

@keyframes dropdownIn {
  from {
    opacity: 0;
    transform: translateY(-10px) scale(0.95);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

@keyframes dropdownOut {
  from {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
  to {
    opacity: 0;
    transform: translateY(-10px) scale(0.95);
  }
}

/* ==================== 响应式设计 ==================== */
@media (max-width: 1024px) {
  .navbar-container {
    padding: 12px 20px;
  }

  .nav-links {
    gap: 4px;
  }

  .nav-link {
    padding: 8px 12px;
    font-size: 14px;
  }

  .link-text {
    display: none;
  }

  .link-icon {
    font-size: 18px;
  }

  .user-nickname {
    display: none;
  }
}

@media (max-width: 768px) {
  .navbar-container {
    flex-wrap: wrap;
    gap: 16px;
  }

  .logo {
    order: 1;
  }

  .user-actions {
    order: 2;
  }

  .nav-links {
    order: 3;
    width: 100%;
    justify-content: space-around;
    padding: 8px 0;
    border-top: 1px solid rgba(99, 102, 241, 0.1);
  }

  .user-menu {
    right: auto;
    left: 50%;
    transform: translateX(-50%);
  }
}

@media (max-width: 480px) {
  .navbar-container {
    padding: 12px 16px;
  }

  .logo .name {
    font-size: 16px;
  }

  .logo .small-name {
    font-size: 10px;
  }

  .nav-link {
    padding: 8px;
  }

  .link-icon {
    font-size: 20px;
  }
}
</style>
