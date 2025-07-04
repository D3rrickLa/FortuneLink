package com.laderrco.fortunelink.portfoliomanagement.domain.factories;

import com.laderrco.fortunelink.portfoliomanagement.domain.entities.Transaction;

public class TransactionFactory {
    public static Transaction createAssetBuyTransaction() {
        return null;
    }

    public static Transaction createAssetSellTransaction() {
        return null;
    }
    
    public static Transaction createAssetTransferTransaction() {
        return null;
    }

    /*
     * broad category that handles deposits/withdrawals of cash to/from the portfolio +
     * dividens, interest/fees, and general portfolio fees
     */
    public static Transaction createCashflowTransaction() {
        return null;
    }

    public static Transaction createCorporationActionTransaction() {
        return null;
    }

    /*
     * For createing the initial liability, as a transaction record
     */
    public static Transaction createLiabilityIncurrenceDetails() {
        return null;
    }

    public static Transaction createLiabilityPaymentTransaction() {
        return null;
    }

    /*
     * This is for reversing a user input/voiding an input as we cannot delete things
     * 
     */
    public static Transaction createReversalTransaction() {
        return null;
    }


    /*
     * to handle accural of interest - bonds or charging of interest on a loan before it's actuall paid
     */
    public static Transaction createAccrualTransaction() {
        throw new UnsupportedOperationException("This method has not been implemented.");
    }
}