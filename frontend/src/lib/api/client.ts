import axios from "axios";
import { createClient } from "../supabase/client";

const apiClient = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080/",
  headers: {
    "Content-Type" : "application/json",
  }
});

// Attach the Supabase JWT to every request
apiClient.interceptors.request.use(async (config) => {
  const supabase = createClient();
  
  // getUser() is more secure as it fetches the user object 
  // from the Supabase Auth server and validates the JWT.
  const { data: { user }, error } = await supabase.auth.getUser();

  if (user && !error) {
    // We still grab the session to get the token for the header
    const { data: { session } } = await supabase.auth.getSession();
    if (session?.access_token) {
      config.headers.Authorization = `Bearer ${session.access_token}`;
    }
  }
  
  return config;
}, (error) => {
  return Promise.reject(error);
});

// Centralised error handling, extend this as you learn what your backend returns
apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (error.response?.status === 401) {
      // Token expired mid-session, refresh and retry once
      const supabase = createClient();
      const { data: { session } } = await supabase.auth.refreshSession();

      if (session?.access_token) {
        error.config.headers.Authorization = `Bearer ${session.access_token}`;
        return apiClient.request(error.config);
      }

      // Refresh failed, kick them out
      await supabase.auth.signOut();
      window.location.href = "/auth/login";
    }

    return Promise.reject(error);
  }
);

export default apiClient;