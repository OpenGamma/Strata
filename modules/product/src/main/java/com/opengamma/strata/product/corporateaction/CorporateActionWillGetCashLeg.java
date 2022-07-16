package com.opengamma.strata.product.corporateaction;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.product.common.PayReceive;

import java.time.LocalDate;

public interface CorporateActionWillGetCashLeg extends CorporateActionWillGetLeg {

  public abstract PayReceive getPayReceive();

  public abstract LocalDate getPaymentDate();

  public abstract CurrencyAmount getCurrencyAmount();

}
