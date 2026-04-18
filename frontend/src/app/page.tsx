// app/page.tsx
import { createClient } from "@/lib/utils/supabase/server";
import { redirect } from "next/navigation";
import Link from "next/link";

export default async function Home() {
  const supabase = await createClient();
  const { data: { user } } = await supabase.auth.getUser();

  if (user) redirect("/dashboard");

  return (
    <main className="flex min-h-screen flex-col items-center justify-center gap-4">
      <h1 className="text-4xl font-bold">FortuneLink</h1>
      <p className="text-muted-foreground">Take control of your financial journey</p>
      <div className="flex gap-4">
        <Link href="/auth/login" className="btn">Login</Link>
        <Link href="/auth/register" className="btn">Register</Link>
      </div>
    </main>
  );
}