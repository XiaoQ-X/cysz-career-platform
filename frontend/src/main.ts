import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './app/router'
import { installAuthHttpBinding, useAuthStore } from './features/auth/auth.store'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
installAuthHttpBinding({
  navigateToLogin: (redirect) => {
    void router.push({ path: '/login', query: redirect === '/' ? {} : { redirect } })
  },
})
void useAuthStore().restore().catch(() => undefined)
app.use(router)

app.mount('#app')
