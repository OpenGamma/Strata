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
import com.opengamma.strata.market.curve.TenorCurveNodeMetadata;
import com.opengamma.strata.market.sensitivity.CurveCurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.CurveCurrencyParameterSensitivity;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;

public class CdsPricer {

  public static final CdsPricer DEFAULT = new CdsPricer();

  public MultiCurrencyAmount presentValue(ExpandedCds product, DefaultSingleCalculationMarketData provider) {
    LocalDate valuationDate = provider.getValuationDate();
    CurveYieldPlaceholder yieldCurve = Curves.discountCurve();
    CurveCreditPlaceholder creditCurve = Curves.creditCurve();
    double recoveryRate = Curves.recoveryRate();
    return CdsAnalyticsWrapper.price(valuationDate, product, yieldCurve, creditCurve, recoveryRate);
  }

  public MultiCurrencyAmount ir01ParallelPar(ExpandedCds product, DefaultSingleCalculationMarketData provider) {
    LocalDate valuationDate = provider.getValuationDate();
    CurveYieldPlaceholder yieldCurve = Curves.discountCurvePar(0.0001D);
    CurveCreditPlaceholder creditCurve = Curves.creditCurve();
    double recoveryRate = Curves.recoveryRate();
    return CdsAnalyticsWrapper.price(valuationDate, product, yieldCurve, creditCurve, recoveryRate)
        .minus(presentValue(product, provider));
  }

  public CurveCurrencyParameterSensitivities ir01BucketedPar(ExpandedCds product, DefaultSingleCalculationMarketData provider) {
    int points = Curves.numOfYieldCurvePoints();
    double[] sensitivities = new double[points];
    List<TenorCurveNodeMetadata> metaData = Lists.newArrayList();
    for (int i = 0; i < points; i++) {
      LocalDate valuationDate = provider.getValuationDate();
      CurveYieldPlaceholder yieldCurve = Curves.discountCurveParBucket(i, 0.0001D);
      CurveCreditPlaceholder creditCurve = Curves.creditCurve();
      double recoveryRate = Curves.recoveryRate();
      MultiCurrencyAmount sensitivity = CdsAnalyticsWrapper.price(valuationDate, product, yieldCurve, creditCurve, recoveryRate)
          .minus(presentValue(product, provider));
      sensitivities[i] = sensitivity.getAmount(product.getCurrency()).getAmount();
      Period period = yieldCurve.getYieldCurvePoints()[i];
      LocalDate pointDate = valuationDate.plus(period);
      Tenor tenor = Tenor.of(period);
      metaData.add(TenorCurveNodeMetadata.of(pointDate, tenor));
    }
    CurveMetadata curveMetadata = CurveMetadata.of("ir curve", metaData);
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

  public MultiCurrencyAmount cs01ParallelPar(ExpandedCds product, DefaultSingleCalculationMarketData provider) {
    LocalDate valuationDate = provider.getValuationDate();
    CurveYieldPlaceholder yieldCurve = Curves.discountCurve();
    CurveCreditPlaceholder creditCurve = Curves.creditCurvePar(0.0001D);
    double recoveryRate = Curves.recoveryRate();
    return CdsAnalyticsWrapper.price(valuationDate, product, yieldCurve, creditCurve, recoveryRate)
        .minus(presentValue(product, provider));
  }

  public CurveCurrencyParameterSensitivities cs01BucketedPar(ExpandedCds product, DefaultSingleCalculationMarketData provider) {
    int points = Curves.numOfCreditCurvePoints();
    double[] sensitivities = new double[points];
    List<TenorCurveNodeMetadata> metaData = Lists.newArrayList();
    for (int i = 0; i < points; i++) {
      LocalDate valuationDate = provider.getValuationDate();
      CurveYieldPlaceholder yieldCurve = Curves.discountCurve();
      CurveCreditPlaceholder creditCurve = Curves.creditCurveParBucket(i, 0.0001D);
      double recoveryRate = Curves.recoveryRate();
      MultiCurrencyAmount sensitivity = CdsAnalyticsWrapper.price(valuationDate, product, yieldCurve, creditCurve, recoveryRate)
          .minus(presentValue(product, provider));
      sensitivities[i] = sensitivity.getAmount(product.getCurrency()).getAmount();
      Period period = creditCurve.getCreditCurvePoints()[i];
      LocalDate pointDate = valuationDate.plus(period);
      Tenor tenor = Tenor.of(period);
      metaData.add(TenorCurveNodeMetadata.of(pointDate, tenor));
    }
    CurveMetadata curveMetadata = CurveMetadata.of("credit curve", metaData);
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