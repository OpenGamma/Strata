/*
 * *
 *  * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *  *
 *  * Please see distribution for license.
 *
 *
 */

package com.opengamma.strata.function.credit;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.engine.calculations.DefaultSingleCalculationMarketData;
import com.opengamma.strata.finance.credit.ExpandedCdsTrade;
import com.opengamma.strata.market.sensitivity.CurveCurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.CurveCurrencyParameterSensitivity;

import java.time.LocalDate;

public class CdsPricer {

  public static final CdsPricer DEFAULT = new CdsPricer();

  private final CdsAnalyticsWrapper _wrapper;

  public CdsPricer() {
    _wrapper = new CdsAnalyticsWrapper();
  }

  public MultiCurrencyAmount presentValue(ExpandedCdsTrade trade, DefaultSingleCalculationMarketData provider) {
    LocalDate valuationDate = provider.getValuationDate();
    CurveYieldPlaceholder yieldCurve = Curves.discountCurve();
    CurveCreditPlaceholder creditCurve = Curves.creditCurve();
    double recoveryRate = Curves.recoveryRate();
    return _wrapper.price(valuationDate, trade, yieldCurve, creditCurve, recoveryRate);
  }

  public MultiCurrencyAmount ir01Par(ExpandedCdsTrade trade, DefaultSingleCalculationMarketData provider) {
    LocalDate valuationDate = provider.getValuationDate();
    CurveYieldPlaceholder yieldCurve = Curves.discountCurvePar(0.0001D);
    CurveCreditPlaceholder creditCurve = Curves.creditCurve();
    double recoveryRate = Curves.recoveryRate();
    return _wrapper.price(valuationDate, trade, yieldCurve, creditCurve, recoveryRate)
        .minus(presentValue(trade, provider));
  }

  public CurveCurrencyParameterSensitivities bucketedIr01Par(ExpandedCdsTrade trade, DefaultSingleCalculationMarketData provider) {
    int points = Curves.numOfYieldCurvePoints();
    double[] sensitivities = new double[points];
    for (int i = 0; i < points; i++) {
      LocalDate valuationDate = provider.getValuationDate();
      CurveYieldPlaceholder yieldCurve = Curves.discountCurveParBucket(i, 0.0001D);
      CurveCreditPlaceholder creditCurve = Curves.creditCurve();
      double recoveryRate = Curves.recoveryRate();
      MultiCurrencyAmount sensitivity = _wrapper.price(valuationDate, trade, yieldCurve, creditCurve, recoveryRate)
          .minus(presentValue(trade, provider));
      sensitivities[i] = sensitivity.getAmount(trade.getCurrency()).getAmount();
    }
    return CurveCurrencyParameterSensitivities.of(
        ImmutableList.of(
            CurveCurrencyParameterSensitivity.of(
                "interest curve",
                trade.getCurrency(),
                sensitivities
            )
        )
    );
  }

  public MultiCurrencyAmount cs01Par(ExpandedCdsTrade trade, DefaultSingleCalculationMarketData provider) {
    LocalDate valuationDate = provider.getValuationDate();
    CurveYieldPlaceholder yieldCurve = Curves.discountCurve();
    CurveCreditPlaceholder creditCurve = Curves.creditCurvePar(0.0001D);
    double recoveryRate = Curves.recoveryRate();
    return _wrapper.price(valuationDate, trade, yieldCurve, creditCurve, recoveryRate)
        .minus(presentValue(trade, provider));
  }

  public CurveCurrencyParameterSensitivities bucketedCr01Par(ExpandedCdsTrade trade, DefaultSingleCalculationMarketData provider) {
    int points = Curves.numOfCreditCurvePoints();
    double[] sensitivities = new double[points];
    for (int i = 0; i < points; i++) {
      LocalDate valuationDate = provider.getValuationDate();
      CurveYieldPlaceholder yieldCurve = Curves.discountCurve();
      CurveCreditPlaceholder creditCurve = Curves.creditCurveParBucket(i, 0.0001D);
      double recoveryRate = Curves.recoveryRate();
      MultiCurrencyAmount sensitivity = _wrapper.price(valuationDate, trade, yieldCurve, creditCurve, recoveryRate)
          .minus(presentValue(trade, provider));
      sensitivities[i] = sensitivity.getAmount(trade.getCurrency()).getAmount();
    }
    return CurveCurrencyParameterSensitivities.of(
        ImmutableList.of(
            CurveCurrencyParameterSensitivity.of(
                "credit curve",
                trade.getCurrency(),
                sensitivities
            )
        )
    );
  }

}