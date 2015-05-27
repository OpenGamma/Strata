package com.opengamma.strata.finance.credit.general.reference;

/**
 * Interface representing the underlying of credit default swap
 * (e.g. Single Name Obligation, Index, etc)
 */
public interface ReferenceInformation {

  ReferenceInformationType getType();

  String getMarketDataKey();

}
