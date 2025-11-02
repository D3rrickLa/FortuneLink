"use client";
import Image from "next/image";
import { useState, FormEvent } from "react";

export default function Home() {
  const [formData, setFormData] = useState({
    userId: "f8db6ee5-561e-4631-ba62-c4944a9ff983",
    creationDateTime: "",
    type: "BUY",
    ticker: "",
    quantity: "",
    amount: ""
  });

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    
    try {
      const response = await fetch('http://localhost:8080/api/transactions', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          userId: formData.userId,
          date: formData.creationDateTime,
          type: formData.type,
          ticker: formData.ticker,
          quantity: parseFloat(formData.quantity),
          amount: parseFloat(formData.amount)
        })
      });
      
      const data = await response.json();
      console.log('Success:', data);
      alert('Transaction added!');
      
    } catch (error) {
      console.error('Error:', error);
      alert('Failed to add transaction');
    }
  };

   return (
    <div className="p-8">
      <h1 className="text-2xl font-bold mb-4">Add Transaction</h1>
      
      <form onSubmit={handleSubmit} className="space-y-4 max-w-md">
        
        <div>
          <label className="block mb-1">Date</label>
          <input
            type="datetime-local"
            value={formData.creationDateTime}
            onChange={(e) => setFormData({...formData, creationDateTime: e.target.value})}
            className="w-full border p-2 rounded"
            required
          />
        </div>

        <div>
          <label className="block mb-1">Type</label>
          <select
            value={formData.type}
            onChange={(e) => setFormData({...formData, type: e.target.value})}
            className="w-full border p-2 rounded"
          >
            <option value="BUY">BUY</option>
            <option value="SELL">SELL</option>
            <option value="DEPOSIT">DEPOSIT</option>
          </select>
        </div>

        <div>
          <label className="block mb-1">Ticker</label>
          <input
            type="text"
            value={formData.ticker}
            onChange={(e) => setFormData({...formData, ticker: e.target.value})}
            className="w-full border p-2 rounded"
            placeholder="AAPL"
          />
        </div>

        <div>
          <label className="block mb-1">Quantity</label>
          <input
            type="number"
            step="0.01"
            value={formData.quantity}
            onChange={(e) => setFormData({...formData, quantity: e.target.value})}
            className="w-full border p-2 rounded"
          />
        </div>

        <div>
          <label className="block mb-1">Amount ($)</label>
          <input
            type="number"
            step="0.01"
            value={formData.amount}
            onChange={(e) => setFormData({...formData, amount: e.target.value})}
            className="w-full border p-2 rounded"
            required
          />
        </div>

        <button 
          type="submit"
          className="w-full bg-blue-600 text-white p-2 rounded hover:bg-blue-700"
        >
          Add Transaction
        </button>
      </form>
    </div>
  );

}
