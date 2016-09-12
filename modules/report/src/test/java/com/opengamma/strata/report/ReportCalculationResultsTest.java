/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 * <p>
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
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

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
@Test
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
  public void test_of() {
    ReportCalculationResults test = sut();
    assertEquals(test.getValuationDate(), VAL_DATE);
    assertEquals(test.getTargets(), ImmutableList.of(TRADE));
    assertEquals(test.getColumns(), ImmutableList.of(COLUMN));
    assertEquals(test.getCalculationResults().get(0, 0).getValue(), PV);
    assertEquals(test.getReferenceData(), ReferenceData.standard());
  }

  //-------------------------------------------------------------------------
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
