<template>
  <div class="login-container">
    <div class="login-card">
      <h1 class="login-title">天天AI超级智能体</h1>
      <p class="login-subtitle">请登录后使用</p>
      <form @submit.prevent="handleLogin" class="login-form">
        <div class="form-group">
          <label>用户名</label>
          <input v-model="username" type="text" placeholder="请输入用户名" required />
        </div>
        <div class="form-group">
          <label>密码</label>
          <input v-model="password" type="password" placeholder="请输入密码" required />
        </div>
        <p v-if="error" class="error-msg">{{ error }}</p>
        <button type="submit" class="login-btn" :disabled="loading">
          {{ loading ? '登录中...' : '登 录' }}
        </button>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'

const router = useRouter()
const username = ref('')
const password = ref('')
const error = ref('')
const loading = ref(false)

const handleLogin = async () => {
  error.value = ''
  loading.value = true
  try {
    const res = await axios.post('http://localhost:8123/api/user/login', {
      username: username.value,
      password: password.value
    })
    localStorage.setItem('access_token', res.data.access_token)
    localStorage.setItem('refresh_token', res.data.refresh_token)
    router.push('/')
  } catch (e) {
    error.value = e.response?.data?.error || '登录失败'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #0a0a12 0%, #111122 100%);
}
.login-card {
  background: rgba(17, 23, 41, 0.9);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  padding: 48px 40px;
  width: 380px;
  box-shadow: 0 8px 32px rgba(0, 240, 255, 0.15), inset 0 0 0 1px rgba(255,255,255,0.1);
}
.login-title {
  font-size: 1.8rem;
  text-align: center;
  color: #fff;
  margin-bottom: 8px;
  text-shadow: 0 0 10px rgba(0, 240, 255, 0.5);
}
.login-subtitle {
  text-align: center;
  color: rgba(255,255,255,0.5);
  margin-bottom: 32px;
  font-size: 0.9rem;
}
.form-group {
  margin-bottom: 20px;
}
.form-group label {
  display: block;
  color: rgba(255,255,255,0.7);
  margin-bottom: 6px;
  font-size: 0.9rem;
}
.form-group input {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid rgba(255,255,255,0.15);
  border-radius: 8px;
  background: rgba(255,255,255,0.05);
  color: #fff;
  font-size: 1rem;
  outline: none;
  box-sizing: border-box;
  transition: border-color 0.3s;
}
.form-group input:focus {
  border-color: #00f0ff;
}
.error-msg {
  color: #ff6b8b;
  text-align: center;
  margin-bottom: 16px;
  font-size: 0.9rem;
}
.login-btn {
  width: 100%;
  padding: 12px;
  border: none;
  border-radius: 8px;
  background: linear-gradient(90deg, #0088ff, #00b2ff);
  color: #fff;
  font-size: 1rem;
  cursor: pointer;
  transition: all 0.3s;
}
.login-btn:hover:not(:disabled) {
  box-shadow: 0 0 15px rgba(0, 178, 255, 0.7);
  transform: scale(1.02);
}
.login-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>
