package com.opengamma.platform.finance.future;

import static com.opengamma.basics.currency.Currency.GBP;
import static com.opengamma.basics.date.Tenor.TENOR_2M;
import static com.opengamma.basics.index.IborIndices.GBP_LIBOR_2M;
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
 * 
 */
@Test
public class IborFutureSecurityTest {
  private static final IborFuture IBOR_FUTURE;
  static {
    double notinal = 1_000d;
    double accFactor = TENOR_2M.getPeriod().toTotalMonths() / 12.0;
    LocalDate lastTradeDate = date(2015, 6, 15);
    int ROUNDING = 4;
    IBOR_FUTURE = IborFuture.builder().currency(GBP).accrualFactor(accFactor).notional(notinal)
        .lastTradeDate(lastTradeDate).index(GBP_LIBOR_2M).roundingDecimalPlaces(ROUNDING).build();
  }
  private static final StandardId SECURITY_ID = StandardId.of("OG-Ticker", "OG");
  private static final ImmutableMap<String, String> ATTRIBUTES = ImmutableMap.of("a", "b");

  /**
   * Test security type
   */
  public void builderTest() {
    IborFutureSecurity iborFutureSecurity = IborFutureSecurity.builder().product(IBOR_FUTURE).standardId(SECURITY_ID)
        .attributes(ATTRIBUTES).build();
    assertEquals(iborFutureSecurity.getSecurityType(), SecurityType.of("IborFuture"));
  }

  /**
   * Coverage test
   */
  public void coverageTest() {
    IborFutureSecurity iborFutureSecurity1 = IborFutureSecurity.builder().product(IBOR_FUTURE).standardId(SECURITY_ID)
        .attributes(ATTRIBUTES).build();
    coverImmutableBean(iborFutureSecurity1);
    IborFuture iborFuture2 = IborFuture.builder().notional(500).lastTradeDate(date(2015, 3, 15)).index(GBP_LIBOR_2M)
        .build();
    IborFutureSecurity iborFutureSecurity2 = IborFutureSecurity.builder().product(iborFuture2)
        .standardId(StandardId.of("OG-Ticker", "OG2")).attributes(ImmutableMap.of("c", "d")).build();
    coverBeanEquals(iborFutureSecurity1, iborFutureSecurity2);
  }

  /**
   * Serialization test
   */
  public void serializationTest() {
    IborFutureSecurity iborFutureSecurity = IborFutureSecurity.builder().product(IBOR_FUTURE).standardId(SECURITY_ID)
        .attributes(ATTRIBUTES).build();
    assertSerialization(iborFutureSecurity);
  }
}
