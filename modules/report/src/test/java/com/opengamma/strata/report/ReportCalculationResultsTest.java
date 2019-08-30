/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.report;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.product.common.BuySell.BUY;
import static com.opengamma.strata.product.common.BuySell.SELL;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.AdjustableDate;
import com.opengamma.strata.calc.Column;
import com.opengamma.strata.calc.Results;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.measure.Measures;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.fra.Fra;
import com.opengamma.strata.product.fra.FraTrade;

/**
 * Test {@link ReportCalculationResults}.
 */
public class ReportCalculationResultsTest {

  private static final LocalDate VAL_DATE = date(2016, 6, 30);
  private static final LocalDate VAL_DATE2 = date(2016, 7, 1);
  private static final FraTrade TRADE = FraTrade.of(TradeInfo.empty(), Fra.builder()
      .buySell(BUY)
      .notional(1_000_000)
      .startDate(date(2015, 8, 5))
      .endDate(date(2015, 11, 5))
      .paymentDate(AdjustableDate.of(date(2015, 8, 7)))
      .fixedRate(0.25d)
      .index(GBP_LIBOR_3M)
      .build());
  private static final FraTrade TRADE2 = FraTrade.of(TradeInfo.empty(), Fra.builder()
      .buySell(SELL)
      .notional(1_000_000)
      .startDate(date(2015, 8, 5))
      .endDate(date(2015, 11, 5))
      .paymentDate(AdjustableDate.of(date(2015, 8, 7)))
      .fixedRate(0.25d)
      .index(GBP_LIBOR_3M)
      .build());
  private static final Column COLUMN = Column.of(Measures.PRESENT_VALUE);
  private static final Column COLUMN2 = Column.of(Measures.PAR_RATE);
  private static final CurrencyAmount PV = CurrencyAmount.of(GBP, 12);

  //-------------------------------------------------------------------------
  @Test
  public void test_of() {
    ReportCalculationResults test = sut();
    assertThat(test.getValuationDate()).isEqualTo(VAL_DATE);
    assertThat(test.getTargets()).containsExactly(TRADE);
    assertThat(test.getColumns()).containsExactly(COLUMN);
    assertThat(test.getCalculationResults().get(0, 0).getValue()).isEqualTo(PV);
    assertThat(test.getReferenceData()).isEqualTo(ReferenceData.standard());
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverImmutableBean(sut());
    coverBeanEquals(sut(), sut2());
  }

  //-------------------------------------------------------------------------
  static ReportCalculationResults sut() {
    Results results = Results.of(ImmutableList.of(COLUMN.toHeader()), ImmutableList.of(Result.success(PV)));
    return ReportCalculationResults.of(VAL_DATE, ImmutableList.of(TRADE), ImmutableList.of(COLUMN), results);
  }

  static ReportCalculationResults sut2() {
    Results results = Results.of(ImmutableList.of(COLUMN.toHeader()), ImmutableList.of(Result.success(Double.valueOf(25))));
    return ReportCalculationResults.of(VAL_DATE2, ImmutableList.of(TRADE2), ImmutableList.of(COLUMN2), results);
  }

}
