/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.trade;

import static org.testng.AssertJUnit.fail;

import java.math.BigDecimal;

import org.testng.annotations.Test;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalTime;
import org.threeten.bp.OffsetTime;
import org.threeten.bp.ZoneId;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.ZonedDateTime;

import com.opengamma.core.position.Counterparty;
import com.opengamma.core.position.impl.SimpleCounterparty;
import com.opengamma.core.position.impl.SimpleTrade;
import com.opengamma.financial.security.future.InterestRateFutureSecurity;
import com.opengamma.id.ExternalId;
import com.opengamma.util.money.Currency;
import com.opengamma.util.test.TestGroup;
import com.opengamma.util.time.Expiry;

/**
 * Test for {@link TradeWrapper}.
 */
@Test(groups = TestGroup.UNIT)
public class TradeWrapperTest {

  /**
   * Tests if the validation check for the security type is working correctly.
   */
  public void testInstanceOf() {
    Expiry expiry = new Expiry(ZonedDateTime.of(LocalDate.of(2014, 6, 18), LocalTime.of(0, 0), ZoneId.systemDefault()));
    String tradingExchange = "";
    String settlementExchange = "";
    Currency currency = Currency.USD;
    double unitAmount = 1000;
    ExternalId underlyingId = ExternalId.of("first", "second");
    String category = "";
    InterestRateFutureSecurity irFuture = new InterestRateFutureSecurity(expiry, tradingExchange, settlementExchange, currency, unitAmount, underlyingId, category);
    Counterparty counterparty = new SimpleCounterparty(ExternalId.of(Counterparty.DEFAULT_SCHEME, "COUNTERPARTY"));
    BigDecimal tradeQuantity = BigDecimal.valueOf(10);
    LocalDate tradeDate = LocalDate.of(2000, 1, 1);
    OffsetTime tradeTime = OffsetTime.of(LocalTime.of(0, 0), ZoneOffset.UTC);
    SimpleTrade trade = new SimpleTrade(irFuture, tradeQuantity, counterparty, tradeDate, tradeTime);
    
    new InterestRateFutureTrade(trade);
    
    try {
      new BondFutureTrade(trade);
      fail("TradeWrapper instanceof validation is not working");
    } catch(Exception e) {
      // expected
    }
  }
}
