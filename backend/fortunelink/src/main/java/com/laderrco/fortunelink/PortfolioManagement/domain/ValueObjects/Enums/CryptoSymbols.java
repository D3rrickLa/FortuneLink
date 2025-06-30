package com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.Enums;

public enum CryptoSymbols {
    BTC, ETH, USDT, XRP, BNB, SOL, USDC, TRX, DOGE, ADA,
    HYPE, BCH, LEO, SUI, LINK, XLM, AVAX, TON, SHIB, LTC,
    USDe, HBAR, DAI, XMR, DOT, BGB, UNI, PI, PEPE, AAVE,
    OKB, TAO, APT, CRO, ICP, NEAR, ETC, USD1, ONDO, MNT,
    GT, POL, TRUMP, KAS, VET, FDUSD, SKY, ATOM, ENA, RENDER,
    FIL, KCS, FET, ALGO, WLD, ARB, SEI, KAIA, FLR, QNT,
    JUP, PYUSD, SPX, BONK, INJ, TIA, FORM, XDC, PAXG, FARTCOIN,
    VIRTUAL, XAUt, OP, STX, IP, S, A, NEXO, CRV, GRT,
    JTO, WIF, IMX, DEXE, AB, CAKE, ZEC, AERO, ENS, FLOKI,
    THETA, BSV, SAND, LDO, BTT, IOTA, GALA, JASMY, PENDLE, ZRO;

    public static Boolean isCrypto(String symbol) {
        for (CryptoSymbols cryptoSymbol : values()) {
            if (cryptoSymbol.name().equalsIgnoreCase(symbol)) {
                return true;
            }
        }

        return false;
    }
}
