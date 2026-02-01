'use client';
import { createClient } from '@/lib/utils/supabase/client';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useState } from 'react';

export default function LoginPage() {
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState('')
    const router = useRouter();

    const supabase = createClient(); // init the supabase 

    const handleLogin = async (e: React.FormEvent) => {
        e.preventDefault();
        const { error } = await supabase.auth.signInWithPassword({ email, password, });

        if (error) {
            setError(error.message);
        }
        else {
            router.push('/portfolio');
            router.refresh();
        }
    };

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

                <div className="relative flex py-5 items-center">
                    <div className="flex-grow border-t border-gray-300"></div>
                    <span className="flex-shrink mx-4 text-gray-400 text-sm">OR</span>
                    <div className="flex-grow border-t border-gray-300"></div>
                </div>

                <div className="text-center">
                    <Link
                        href="./signup"
                        className="text-blue-500 hover:text-blue-700 font-medium transition-colors"
                    >
                        Create Account
                    </Link>
                </div>
            </form>
        </div>
    );
}