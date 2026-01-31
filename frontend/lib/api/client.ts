// lib/api/client.ts
import axios from "axios";
import { createClient } from "../utils/supabase/client";

const API_URL_BASE = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';
const supabase = createClient();

export const apiClient = axios.create({
    baseURL: API_URL_BASE,
    headers: { 'Content-Type': 'application/json' }
});

apiClient.interceptors.request.use(async (config) => {
    const { data: {  session } } = await supabase.auth.getSession();

    if (session?.access_token) {
        config.headers.Authorization = `Bearer ${session.access_token}`;
    }
    return config;
});

// Handling errors global
apiClient.interceptors.response.use(
    (response) => response, (error) => {
        if (error.response?.status === 401) {
            // Only runs on the client side
            if (typeof window !== 'undefined') {
                window.location.href = '/auth/login';
            }
        }
        return Promise.reject(error);
    }
);