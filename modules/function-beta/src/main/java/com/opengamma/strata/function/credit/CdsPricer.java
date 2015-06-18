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
import com.google.common.collect.Lists;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.engine.calculations.DefaultSingleCalculationMarketData;
import com.opengamma.strata.finance.credit.ExpandedCds;
import com.opengamma.strata.market.curve.CurveMetadata;
import com.opengamma.strata.market.curve.IsdaCreditCurveParRates;
import com.opengamma.strata.market.curve.IsdaYieldCurveParRates;
import com.opengamma.strata.market.curve.TenorCurveNodeMetadata;
import com.opengamma.strata.market.sensitivity.CurveCurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.CurveCurrencyParameterSensitivity;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;

// TODO move to pricer
public class CdsPricer {

  public static final CdsPricer DEFAULT = new CdsPricer();

  private static final double ONE_BPS = 0.0001D;

  public MultiCurrencyAmount presentValue(
      ExpandedCds product,
      IsdaYieldCurveParRates yieldCurveParRates,
      IsdaCreditCurveParRates creditCurveParRates,
      DefaultSingleCalculationMarketData provider
  ) {
    LocalDate valuationDate = provider.getValuationDate();
    double recoveryRate = creditCurveParRates.getRecoveryRate();
    return CdsAnalyticsWrapper.price(valuationDate, product, yieldCurveParRates, creditCurveParRates, recoveryRate);
  }

  public MultiCurrencyAmount ir01ParallelPar(
      ExpandedCds product,
      IsdaYieldCurveParRates yieldCurveParRates,
      IsdaCreditCurveParRates creditCurveParRates,
      DefaultSingleCalculationMarketData provider
  ) {
    LocalDate valuationDate = provider.getValuationDate();
    double recoveryRate = creditCurveParRates.getRecoveryRate();
    return CdsAnalyticsWrapper
        .price(valuationDate, product, yieldCurveParRates.parallelShiftParRatesinBps(ONE_BPS), creditCurveParRates, recoveryRate)
        .minus(presentValue(product, yieldCurveParRates, creditCurveParRates, provider));
  }

  public CurveCurrencyParameterSensitivities ir01BucketedPar(
      ExpandedCds product,
      IsdaYieldCurveParRates yieldCurveParRates,
      IsdaCreditCurveParRates creditCurveParRates,
      DefaultSingleCalculationMarketData provider
  ) {
    int points = yieldCurveParRates.getNumberOfPoints();
    double[] sensitivities = new double[points];
    List<TenorCurveNodeMetadata> metaData = Lists.newArrayList();
    for (int i = 0; i < points; i++) {
      LocalDate valuationDate = provider.getValuationDate();
      double recoveryRate = creditCurveParRates.getRecoveryRate();
      MultiCurrencyAmount sensitivity = CdsAnalyticsWrapper
          .price(
              valuationDate,
              product,
              yieldCurveParRates.bucketedShiftParRatesinBps(i, ONE_BPS),
              creditCurveParRates,
              recoveryRate)
          .minus(presentValue(product, yieldCurveParRates, creditCurveParRates, provider));
      sensitivities[i] = sensitivity.getAmount(product.getCurrency()).getAmount();
      Period period = yieldCurveParRates.getYieldCurvePoints()[i];
      LocalDate pointDate = valuationDate.plus(period);
      Tenor tenor = Tenor.of(period);
      metaData.add(TenorCurveNodeMetadata.of(pointDate, tenor));
    }
    CurveMetadata curveMetadata = CurveMetadata.of(yieldCurveParRates.getName(), metaData);
    return CurveCurrencyParameterSensitivities.of(
        ImmutableList.of(
            CurveCurrencyParameterSensitivity.of(
                curveMetadata,
                product.getCurrency(),
                sensitivities
            )
        )
    );
  }

  public MultiCurrencyAmount cs01ParallelPar(
      ExpandedCds product,
      IsdaYieldCurveParRates yieldCurveParRates,
      IsdaCreditCurveParRates creditCurveParRates,
      DefaultSingleCalculationMarketData provider
  ) {
    LocalDate valuationDate = provider.getValuationDate();
    double recoveryRate = creditCurveParRates.getRecoveryRate();
    return CdsAnalyticsWrapper
        .price(valuationDate, product, yieldCurveParRates, creditCurveParRates.parallelShiftParRatesinBps(ONE_BPS), recoveryRate)
        .minus(presentValue(product, yieldCurveParRates, creditCurveParRates, provider));
  }

  public CurveCurrencyParameterSensitivities cs01BucketedPar(
      ExpandedCds product,
      IsdaYieldCurveParRates yieldCurveParRates,
      IsdaCreditCurveParRates creditCurveParRates,
      DefaultSingleCalculationMarketData provider
  ) {
    int points = creditCurveParRates.getNumberOfPoints();
    double[] sensitivities = new double[points];
    List<TenorCurveNodeMetadata> metaData = Lists.newArrayList();
    for (int i = 0; i < points; i++) {
      LocalDate valuationDate = provider.getValuationDate();
      double recoveryRate = creditCurveParRates.getRecoveryRate();
      MultiCurrencyAmount sensitivity = CdsAnalyticsWrapper
          .price(valuationDate, product, yieldCurveParRates, creditCurveParRates.bucketedShiftParRatesinBps(i, ONE_BPS), recoveryRate)
          .minus(presentValue(product, yieldCurveParRates, creditCurveParRates, provider));
      sensitivities[i] = sensitivity.getAmount(product.getCurrency()).getAmount();
      Period period = creditCurveParRates.getCreditCurvePoints()[i];
      LocalDate pointDate = valuationDate.plus(period);
      Tenor tenor = Tenor.of(period);
      metaData.add(TenorCurveNodeMetadata.of(pointDate, tenor));
    }
    CurveMetadata curveMetadata = CurveMetadata.of(creditCurveParRates.getName(), metaData);
    return CurveCurrencyParameterSensitivities.of(
        ImmutableList.of(
            CurveCurrencyParameterSensitivity.of(
                curveMetadata,
                product.getCurrency(),
                sensitivities
            )
        )
    );
  }

}