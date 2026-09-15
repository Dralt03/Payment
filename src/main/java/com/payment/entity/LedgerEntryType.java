package com.payment.entity;

/**
 * Whether a ledger entry represents money going OUT or money coming IN.
 *
 * Double-entry accounting rule:
 *   For every payment of amount X from Account A to Account B:
 *     DEBIT  Account A  X   (money leaves A)
 *     CREDIT Account B  X   (money arrives at B)
 *
 *   Sum of all DEBITs must equal sum of all CREDITs across the whole ledger.
 *   This is your internal consistency check.
 */
public enum LedgerEntryType {
    DEBIT,
    CREDIT
}
