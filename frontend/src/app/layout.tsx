// src/app/layout.tsx
import { Providers } from "@/components/providers";
import "./globals.css";
import { Geist } from "next/font/google";

const geist = Geist({ subsets: ["latin"] })

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en" className={`h-full antialiased`}>
      <body className={`min-h-full flex flex-col ${geist.className}`}>
        <Providers>{children}</Providers>
      </body>
    </html>
  );
}