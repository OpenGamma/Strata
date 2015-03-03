package com.opengamma.platform.finance.future;

import static com.opengamma.basics.currency.Currency.GBP;
import static com.opengamma.basics.index.IborIndices.GBP_LIBOR_2M;
import static com.opengamma.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.collect.TestHelper.assertSerialization;
import static com.opengamma.collect.TestHelper.coverBeanEquals;
import static com.opengamma.collect.TestHelper.coverImmutableBean;
import static com.opengamma.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.collect.id.StandardId;
import com.opengamma.platform.finance.SecurityType;

/**
 * Test IborFutureOptionSecurity. 
 */
@Test
public class IborFutureOptionSecurityTest {
  private static final double NOTIONAL_1 = 1_000d;
  private static final double NOTIONAL_2 = 2_000d;
  private static final LocalDate LAST_TRADE_DATE_1 = date(2015, 6, 15);
  private static final LocalDate LAST_TRADE_DATE_2 = date(2015, 9, 16);
  private static final int ROUNDING = 6;
  private static final IborFuture IBOR_FUTURE_1 = IborFuture.builder().roundingDecimalPlaces(ROUNDING).currency(GBP)
      .index(GBP_LIBOR_2M).notional(NOTIONAL_1).lastTradeDate(LAST_TRADE_DATE_1).build();
  private static final IborFuture IBOR_FUTURE_2 = IborFuture.builder().currency(GBP)
      .index(GBP_LIBOR_3M).notional(NOTIONAL_2).lastTradeDate(LAST_TRADE_DATE_2).build();
  private static final LocalDate EXPIRY_DATE = date(2015, 5, 20);
  private static final double STRIKE_PRICE_1 = 1.075;
  private static final double STRIKE_PRICE_2 = 0.975;
  private static final boolean IS_CALL = true;
  private static final IborFutureOption IBOR_FUTURE_OPTION_1 = IborFutureOption.builder().iborFuture(IBOR_FUTURE_1)
      .expirationDate(EXPIRY_DATE).isCall(IS_CALL).strikePrice(STRIKE_PRICE_1).build();
  private static final IborFutureOption IBOR_FUTURE_OPTION_2 = IborFutureOption.builder().iborFuture(IBOR_FUTURE_2)
      .expirationDate(LAST_TRADE_DATE_1).isCall(!IS_CALL).strikePrice(STRIKE_PRICE_2).build();

  private static final StandardId SECURITY_ID = StandardId.of("OG-Ticker", "OG");
  private static final ImmutableMap<String, String> ATTRIBUTES = ImmutableMap.of("a", "b");

  /**
   * Test builder. 
   */
  public void builderTest() {
    IborFutureOptionSecurity optionSecurity = IborFutureOptionSecurity.builder().standardId(SECURITY_ID)
        .attributes(ATTRIBUTES).product(IBOR_FUTURE_OPTION_1).build();
    assertEquals(optionSecurity.getProduct(), IBOR_FUTURE_OPTION_1);
    assertEquals(optionSecurity.getAttributes(), ATTRIBUTES);
    assertEquals(optionSecurity.getStandardId(), SECURITY_ID);
    assertEquals(optionSecurity.getSecurityType(), SecurityType.of("IborFutureOption"));
  }

  /**
   * Coverage test. 
   */
  public void coverageTest() {
    IborFutureOptionSecurity optionSecurity1 = IborFutureOptionSecurity.builder().standardId(SECURITY_ID)
        .attributes(ATTRIBUTES).product(IBOR_FUTURE_OPTION_1).build();
    coverImmutableBean(optionSecurity1);
    IborFutureOptionSecurity optionSecurity2 = IborFutureOptionSecurity.builder().attributes(ImmutableMap.of("c", "d"))
        .standardId(StandardId.of("OG-Ticker", "OG2")).product(IBOR_FUTURE_OPTION_2).build();
    coverBeanEquals(optionSecurity1, optionSecurity2);
  }

  /**
   * Serialization test. 
   */
  public void serializationTest() {
    IborFutureOptionSecurity optionSecurity = IborFutureOptionSecurity.builder().standardId(SECURITY_ID)
        .attributes(ATTRIBUTES).product(IBOR_FUTURE_OPTION_1).build();
    assertSerialization(optionSecurity);
  }

}
