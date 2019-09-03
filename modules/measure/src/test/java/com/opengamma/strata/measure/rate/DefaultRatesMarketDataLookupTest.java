/*
 * Copyright (C) 2019 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.rate;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.calc.runner.FxRateLookup;
import com.opengamma.strata.data.ObservableSource;
import com.opengamma.strata.market.curve.CurveGroupName;
import com.opengamma.strata.market.curve.CurveId;
import com.opengamma.strata.market.curve.CurveName;

public class DefaultRatesMarketDataLookupTest {

  /**
   * Validates the observable source in the curve IDs match the observable source passed in.
   */
  @Test
  public void validateObservableSourceDiscountCurves() {
    ObservableSource observableSource = ObservableSource.of("source");
    CurveId curveId = CurveId.of(CurveGroupName.of("group"), CurveName.of("curves"), observableSource);
    Map<Currency, CurveId> curveMap = ImmutableMap.of(Currency.USD, curveId);
    // This should complete successfully
    DefaultRatesMarketDataLookup.of(curveMap, ImmutableMap.of(), observableSource, FxRateLookup.ofRates());
    // This should blow up because the source in the IDs doesn't match the source passed to the method
    assertThatIllegalArgumentException()
        .isThrownBy(
            () -> DefaultRatesMarketDataLookup.of(curveMap, ImmutableMap.of(), ObservableSource.NONE, FxRateLookup.ofRates()))
        .withMessageContaining("must match the observable source in all curve IDs");
  }

  /**
   * Validates the observable source in the curve IDs match the observable source passed in.
   */
  @Test
  public void validateObservableSourceForwardCurves() {
    ObservableSource observableSource = ObservableSource.of("source");
    CurveId curveId = CurveId.of(CurveGroupName.of("group"), CurveName.of("curves"), observableSource);
    Map<IborIndex, CurveId> curveMap = ImmutableMap.of(IborIndices.AUD_BBSW_1M, curveId);
    // This should complete successfully
    DefaultRatesMarketDataLookup.of(ImmutableMap.of(), curveMap, observableSource, FxRateLookup.ofRates());
    // This should blow up because the source in the IDs doesn't match the source passed to the method
    assertThatIllegalArgumentException()
        .isThrownBy(
            () -> DefaultRatesMarketDataLookup.of(ImmutableMap.of(), curveMap, ObservableSource.NONE, FxRateLookup.ofRates()))
        .withMessageContaining("must match the observable source in all curve IDs");
  }
}
