/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.calibration;

import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.pricer.datasets.ImmutableRatesProviderSimpleData;
import com.opengamma.strata.pricer.swap.SwapDummyData;
import com.opengamma.strata.product.deposit.IborFixingDepositTrade;
import com.opengamma.strata.product.deposit.TermDepositTrade;
import com.opengamma.strata.product.fra.FraTrade;
import com.opengamma.strata.product.fx.FxSwapTrade;
import com.opengamma.strata.product.index.IborFutureTrade;
import com.opengamma.strata.product.swap.SwapTrade;

/**
 * Test {@link CalibrationMeasures}.
 */
@Test
public class CalibrationMeasuresTest {

  //-------------------------------------------------------------------------
  public void test_PAR_SPREAD() {
    assertThat(CalibrationMeasures.PAR_SPREAD.getName()).isEqualTo("ParSpread");
    assertThat(CalibrationMeasures.PAR_SPREAD.getTradeTypes()).contains(
        FraTrade.class, 
        FxSwapTrade.class,
        IborFixingDepositTrade.class, 
        IborFutureTrade.class, 
        SwapTrade.class, 
        TermDepositTrade.class);
  }

  public void test_MARKET_QUOTE() {
    assertThat(CalibrationMeasures.MARKET_QUOTE.getName()).isEqualTo("MarketQuote");
    assertThat(CalibrationMeasures.MARKET_QUOTE.getTradeTypes()).contains(
        FraTrade.class,
        IborFixingDepositTrade.class,
        IborFutureTrade.class,
        SwapTrade.class,
        TermDepositTrade.class);
  }

  //-------------------------------------------------------------------------
  public void test_of_array() {
    CalibrationMeasures test = CalibrationMeasures.of(
        "Test",
        TradeCalibrationMeasure.FRA_PAR_SPREAD,
        TradeCalibrationMeasure.SWAP_PAR_SPREAD);
    assertThat(test.getName()).isEqualTo("Test");
    assertThat(test.getTradeTypes()).containsOnly(FraTrade.class, SwapTrade.class);
    assertThat(test.toString()).isEqualTo("Test");
  }

  public void test_of_list() {
    CalibrationMeasures test = CalibrationMeasures.of(
        "Test",
        ImmutableList.of(TradeCalibrationMeasure.FRA_PAR_SPREAD, TradeCalibrationMeasure.SWAP_PAR_SPREAD));
    assertThat(test.getName()).isEqualTo("Test");
    assertThat(test.getTradeTypes()).containsOnly(FraTrade.class, SwapTrade.class);
    assertThat(test.toString()).isEqualTo("Test");
  }

  public void test_of_duplicate() {
    assertThrowsIllegalArg(() -> CalibrationMeasures.of(
        "Test", TradeCalibrationMeasure.FRA_PAR_SPREAD, TradeCalibrationMeasure.FRA_PAR_SPREAD));
    assertThrowsIllegalArg(() -> CalibrationMeasures.of(
        "Test", ImmutableList.of(TradeCalibrationMeasure.FRA_PAR_SPREAD, TradeCalibrationMeasure.FRA_PAR_SPREAD)));
  }

  public void test_measureNotKnown() {
    CalibrationMeasures test = CalibrationMeasures.of("Test", TradeCalibrationMeasure.FRA_PAR_SPREAD);
    assertThrowsIllegalArg(() -> test.value(SwapDummyData.SWAP_TRADE, ImmutableRatesProviderSimpleData.IMM_PROV_EUR_FIX));
  }

}
