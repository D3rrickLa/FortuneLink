// lib/api/client.ts
   import axios from 'axios'

   const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080'

   export const apiClient = axios.create({
     baseURL: API_BASE_URL,
     headers: {
       'Content-Type': 'application/json',
     },
   })

   // Add auth token to all requests
   apiClient.interceptors.request.use((config) => {
     const token = localStorage.getItem('auth_token') // Or get from Supabase
     if (token) {
       config.headers.Authorization = `Bearer ${token}`
     }
     return config
   })

   // Handle errors globally
   apiClient.interceptors.response.use(
     (response) => response,
     (error) => {
       if (error.response?.status === 401) {
         // Redirect to login
         window.location.href = '/auth/login'
       }
       return Promise.reject(error)
     }
   )