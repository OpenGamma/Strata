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
import com.opengamma.strata.finance.rate.deposit.IborFixingDepositTrade;
import com.opengamma.strata.finance.rate.deposit.TermDepositTrade;
import com.opengamma.strata.finance.rate.fra.FraTrade;
import com.opengamma.strata.finance.rate.swap.SwapTrade;
import com.opengamma.strata.pricer.rate.datasets.ImmutableRatesProviderSimpleData;
import com.opengamma.strata.pricer.rate.swap.SwapDummyData;

/**
 * Test {@link CalibrationMeasures}.
 */
@Test
public class CalibrationMeasuresTest {

  public void test_DEFAULT() {
    assertThat(CalibrationMeasures.DEFAULT.getTradeTypes()).contains(
        FraTrade.class, SwapTrade.class, IborFixingDepositTrade.class, TermDepositTrade.class);
  }

  //-------------------------------------------------------------------------
  public void test_of_array() {
    CalibrationMeasures test = CalibrationMeasures.of(
        TradeCalibrationMeasure.FRA_PAR_SPREAD, TradeCalibrationMeasure.SWAP_PAR_SPREAD);
    assertThat(test.getTradeTypes()).containsOnly(FraTrade.class, SwapTrade.class);
    assertThat(test.toString()).contains("FraParSpreadDiscounting", "SwapParSpreadDiscounting");
  }

  public void test_of_list() {
    CalibrationMeasures test = CalibrationMeasures.of(
        ImmutableList.of(TradeCalibrationMeasure.FRA_PAR_SPREAD, TradeCalibrationMeasure.SWAP_PAR_SPREAD));
    assertThat(test.getTradeTypes()).containsOnly(FraTrade.class, SwapTrade.class);
    assertThat(test.toString()).contains("FraParSpreadDiscounting", "SwapParSpreadDiscounting");
  }

  public void test_of_duplicate() {
    assertThrowsIllegalArg(() -> CalibrationMeasures.of(
        TradeCalibrationMeasure.FRA_PAR_SPREAD, TradeCalibrationMeasure.FRA_PAR_SPREAD));
    assertThrowsIllegalArg(() -> CalibrationMeasures.of(
        ImmutableList.of(TradeCalibrationMeasure.FRA_PAR_SPREAD, TradeCalibrationMeasure.FRA_PAR_SPREAD)));
  }

  public void test_measureNotKnown() {
    CalibrationMeasures test = CalibrationMeasures.of(TradeCalibrationMeasure.FRA_PAR_SPREAD);
    assertThrowsIllegalArg(() -> test.value(SwapDummyData.SWAP_TRADE, ImmutableRatesProviderSimpleData.IMM_PROV_EUR_FIX));
  }

}
