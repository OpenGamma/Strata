/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.graph;

import static org.testng.AssertJUnit.assertEquals;

import java.util.Collections;

import org.testng.annotations.Test;

import com.opengamma.core.position.PositionOrTrade;
import com.opengamma.core.position.impl.SimpleTrade;
import com.opengamma.core.security.impl.SimpleSecurityLink;
import com.opengamma.financial.security.equity.EquitySecurity;
import com.opengamma.id.ExternalId;
import com.opengamma.sesame.function.OutputFunction;
import com.opengamma.util.money.Currency;
import com.opengamma.util.test.TestGroup;

@Test(groups = TestGroup.UNIT)
public class SecurityFunctionDecoratorTest {

  @Test
  public void decorateRoot() {
    FunctionTree<Fn> functionTree = FunctionTree.forFunction(Fn.class);
    FunctionTree<?> decoratedTree = SecurityFunctionDecorator.decorateRoot(functionTree);
    Object fn = decoratedTree.build(Collections.<Class<?>, Object>emptyMap());
    EquitySecurity security = new EquitySecurity("exc", "exc", "compName", Currency.AUD);
    SimpleTrade trade = new SimpleTrade();
    SimpleSecurityLink securityLink = new SimpleSecurityLink(ExternalId.of("abc", "123"));
    securityLink.setTarget(security);
    trade.setSecurityLink(securityLink);
    @SuppressWarnings("unchecked")
    Currency ccy = ((OutputFunction<PositionOrTrade, Currency>) fn).execute(trade);
    assertEquals(Currency.AUD, ccy);
  }

  public static class Fn implements OutputFunction<EquitySecurity, Currency> {

    @Override
    public Currency execute(EquitySecurity equitySecurity) {
      return equitySecurity.getCurrency();
    }
  }
}
