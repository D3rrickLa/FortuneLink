import Link from "next/link";
import Image from "next/image";

export default function Home() {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center bg-white font-sans dark:bg-black text-black dark:text-white">
      <main className="flex flex-col items-center gap-8 px-6 text-center">
        {/* Logo/Brand Section */}
        <div className="flex flex-col items-center gap-2">
          <div className="bg-blue-600 p-3 rounded-2xl mb-4">
             <span className="text-white text-3xl font-bold">FL</span>
          </div>
          <h1 className="text-5xl font-extrabold tracking-tight sm:text-6xl">
            FortuneLink
          </h1>
          <p className="max-w-[600px] text-lg text-zinc-600 dark:text-zinc-400 sm:text-xl">
            Take control of your financial future. Track your net worth and manage your assets in one secure place.
          </p>
        </div>

        {/* Action Buttons */}
        <div className="flex flex-col gap-4 w-full max-w-sm sm:flex-row sm:max-w-none sm:justify-center">
          <Link
            href="/auth/signup"
            className="flex h-12 items-center justify-center rounded-full bg-blue-600 px-8 text-base font-semibold text-white transition-colors hover:bg-blue-700 sm:w-40"
          >
            Get Started
          </Link>
          <Link
            href="/auth/login"
            className="flex h-12 items-center justify-center rounded-full border border-zinc-200 bg-transparent px-8 text-base font-semibold transition-colors hover:bg-zinc-100 dark:border-zinc-800 dark:hover:bg-zinc-900 sm:w-40"
          >
            Login
          </Link>
        </div>

        {/* Status Check (Optional link to Portfolio) */}
        <Link 
          href="/portfolio" 
          className="text-sm text-zinc-500 hover:text-blue-600 transition-colors underline underline-offset-4"
        >
          Already a member? View your Portfolio →
        </Link>
      </main>

      {/* Simple Footer */}
      <footer className="absolute bottom-8 text-sm text-zinc-400">
        © 2026 FortuneLink Security. Powered by Supabase & Spring Boot.
      </footer>
    </div>
  );
}