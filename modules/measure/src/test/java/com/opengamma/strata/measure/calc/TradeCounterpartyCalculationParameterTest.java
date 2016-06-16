/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.calc;

import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import java.util.Optional;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.calc.TestingMeasures;
import com.opengamma.strata.calc.runner.CalculationParameter;
import com.opengamma.strata.calc.runner.TestParameter;
import com.opengamma.strata.calc.runner.TestParameter2;
import com.opengamma.strata.product.GenericSecurity;
import com.opengamma.strata.product.GenericSecurityTrade;
import com.opengamma.strata.product.SecurityId;
import com.opengamma.strata.product.SecurityInfo;
import com.opengamma.strata.product.TradeInfo;

/**
 * Test {@link TradeCounterpartyCalculationParameter}.
 */
@Test
public class TradeCounterpartyCalculationParameterTest {

  private static final CalculationParameter PARAM1 = new TestParameter();
  private static final CalculationParameter PARAM2 = new TestParameter();
  private static final CalculationParameter PARAM3 = new TestParameter();
  private static final CalculationParameter PARAM_OTHER = new TestParameter2();

  private static final StandardId ID1 = StandardId.of("test", "cpty1");
  private static final StandardId ID2 = StandardId.of("test", "cpty2");
  private static final StandardId ID3 = StandardId.of("test", "cpty3");

  private static final SecurityInfo SEC_INFO_1 =
      SecurityInfo.of(SecurityId.of("test", "sec1"), 1.0, CurrencyAmount.of(Currency.EUR, 1.0));
  private static final GenericSecurity SEC_1 = GenericSecurity.of(SEC_INFO_1);
  private static final TradeInfo TRADE_INFO_1 = TradeInfo.builder().counterparty(ID1).build();
  private static final GenericSecurityTrade TRADE_1 = GenericSecurityTrade.of(TRADE_INFO_1, SEC_1, 1, 1.0);

  private static final SecurityInfo SEC_INFO_2 =
      SecurityInfo.of(SecurityId.of("test", "sec2"), 2.0, CurrencyAmount.of(Currency.EUR, 2.0));
  private static final GenericSecurity SEC_2 = GenericSecurity.of(SEC_INFO_2);
  private static final TradeInfo TRADE_INFO_2 = TradeInfo.builder().counterparty(ID2).build();
  private static final GenericSecurityTrade TRADE_2 = GenericSecurityTrade.of(TRADE_INFO_2, SEC_2, 2, 2.0);
  private static final TradeInfo TRADE_INFO_3 = TradeInfo.builder().counterparty(ID3).build();
  private static final GenericSecurityTrade TRADE_3 = GenericSecurityTrade.of(TRADE_INFO_3, SEC_2, 2, 2.0);

  //-------------------------------------------------------------------------
  public void test_of() {
    TradeCounterpartyCalculationParameter test = TradeCounterpartyCalculationParameter.of(
        ImmutableMap.of(ID1, PARAM1, ID2, PARAM2), PARAM3);
    assertEquals(test.getQueryType(), TestParameter.class);
    assertEquals(test.getParameters().size(), 2);
    assertEquals(test.getDefaultParameter(), PARAM3);
    assertEquals(test.queryType(), TestParameter.class);
    assertEquals(test.filter(TRADE_1, TestingMeasures.PRESENT_VALUE), Optional.of(PARAM1));
    assertEquals(test.filter(TRADE_2, TestingMeasures.PRESENT_VALUE), Optional.of(PARAM2));
    assertEquals(test.filter(TRADE_3, TestingMeasures.PRESENT_VALUE), Optional.of(PARAM3));
  }

  public void of_empty() {
    assertThrowsIllegalArg(() -> TradeCounterpartyCalculationParameter.of(ImmutableMap.of(), PARAM3));
  }

  public void of_badType() {
    assertThrowsIllegalArg(() -> TradeCounterpartyCalculationParameter.of(ImmutableMap.of(ID1, PARAM_OTHER), PARAM3));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    TradeCounterpartyCalculationParameter test = TradeCounterpartyCalculationParameter.of(
        ImmutableMap.of(ID1, PARAM1, ID2, PARAM2), PARAM3);
    coverImmutableBean(test);
    TradeCounterpartyCalculationParameter test2 = TradeCounterpartyCalculationParameter.of(
        ImmutableMap.of(ID1, PARAM1), PARAM2);
    coverBeanEquals(test, test2);
  }

}
