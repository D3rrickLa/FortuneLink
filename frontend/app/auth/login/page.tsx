// app/auth/login/page.tsx
   'use client'
   
   import { useState } from 'react'
   import { createClient } from '@/lib/supabase/client'
   import { useRouter } from 'next/navigation'

   export default function LoginPage() {
     const [email, setEmail] = useState('')
     const [password, setPassword] = useState('')
     const [error, setError] = useState('')
     const router = useRouter()

     const handleLogin = async (e: React.FormEvent) => {
       e.preventDefault()
       const { error } = await createClient.auth.signInWithPassword({
         email,
         password,
       })
       
       if (error) {
         setError(error.message)
       } else {
         router.push('/portfolio')
       }
     }

     return (
       <div className="min-h-screen flex items-center justify-center">
         <form onSubmit={handleLogin} className="w-full max-w-md space-y-4">
           <h1 className="text-2xl font-bold">Login to FortuneLink</h1>
           
           {error && <p className="text-red-500">{error}</p>}
           
           <input
             type="email"
             placeholder="Email"
             value={email}
             onChange={(e) => setEmail(e.target.value)}
             className="w-full px-4 py-2 border rounded"
           />
           
           <input
             type="password"
             placeholder="Password"
             value={password}
             onChange={(e) => setPassword(e.target.value)}
             className="w-full px-4 py-2 border rounded"
           />
           
           <button
             type="submit"
             className="w-full bg-blue-500 text-white py-2 rounded hover:bg-blue-600"
           >
             Login
           </button>
         </form>
       </div>
     )
   }