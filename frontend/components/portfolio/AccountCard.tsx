// components/portfolio/AccountCard.tsx
import type { Account } from '@/types/portfolio'

interface Props {
    account: Account
}

export function AccountCard({ account }: Props) {
    const totalValue = account.assets.reduce(
        (sum, asset) => sum + (asset?.costBasis || 0),
        0
    )

    return (
        <div className="bg-white rounded-lg shadow p-6">
            <h3 className="text-lg font-semibold mb-2">{account.name}</h3>
            <p className="text-sm text-gray-500 mb-4">{account.accountType}</p>

            <p className="text-2xl font-bold mb-4">
                ${totalValue.toLocaleString()}
            </p>

            <div className="space-y-2">
                {account.assets.map((asset) => (
                    <div key={asset.id} className="flex justify-between">
                        <span>{asset.symbol}</span>
                        <span className="font-semibold">
                            ${asset.costBasis?.toLocaleString()}
                        </span>
                    </div>
                ))}
            </div>
        </div>
    )
}