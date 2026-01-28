'use client'
import { useEffect, useState } from 'react';
import { portfolioApi } from '@/lib/api/portfolios';

export default function TestPage() {
  const [data, setData] = useState<any>(null);
  const [error, setError] = useState<string>('');

  useEffect(() => {
    // Replace with a known UUID from your local DB
    portfolioApi.getPortfolio('test-user-id')
      .then(setData)
      .catch(err => setError(err.message));
  }, []);

  return (
    <div className="p-10">
      <h1 className="text-2xl font-bold">API Connectivity Test</h1>
      {error && <p className="text-red-500 font-mono">Error: {error}</p>}
      {data ? (
        <pre className="bg-gray-100 p-4 mt-4">{JSON.stringify(data, null, 2)}</pre>
      ) : (
        <p>Loading from Spring Boot...</p>
      )}
    </div>
  );
}