"use client";
import { useState } from "react";
import { createClient } from "@/lib/utils/supabase/client";
import { useRouter } from "next/navigation";
import { Button } from "@/components/ui/button";

export function LoginForm() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const router = useRouter();

  async function handleLogin() {
    setLoading(true);
    setError(null);
    const supabase = createClient();

    const { error } = await supabase.auth.signInWithPassword({ email, password });
    if (error) {
      setError(error.message);
      setLoading(false);
      return;
    }
    router.replace("/dashboard"); // replace, not push — no going back to login
    router.refresh();
  }

  return (
    <div className="flex flex-col gap-4 w-full max-w-sm">
      <h2 className="text-2xl font-semibold">Welcome back</h2>
      {error && <p className="text-sm text-destructive">{error}</p>}
      <input
        type="email"
        placeholder="Email"
        value={email}
        onChange={e => setEmail(e.target.value)}
        className="border rounded px-3 py-2 bg-background text-foreground"
        disabled={loading}
      />
      <input
        type="password"
        placeholder="Password"
        value={password}
        onChange={e => setPassword(e.target.value)}
        className="border rounded px-3 py-2 bg-background text-foreground"
        disabled={loading}
      />
      <Button onClick={handleLogin} disabled={loading}>
        {loading ? "Signing in..." : "Sign in"}
      </Button>
      <p className="text-sm text-center">
        No account? <a href="/auth/register" className="underline">Register</a>
      </p>
    </div>
  );
}