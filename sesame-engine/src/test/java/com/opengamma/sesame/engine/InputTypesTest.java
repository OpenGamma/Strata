/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.engine;

import static com.opengamma.sesame.config.ConfigBuilder.column;
import static com.opengamma.sesame.config.ConfigBuilder.configureView;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import java.util.EnumSet;

import org.testng.annotations.Test;
import org.threeten.bp.ZonedDateTime;

import com.google.common.collect.Lists;
import com.opengamma.core.position.Trade;
import com.opengamma.financial.security.cashflow.CashFlowSecurity;
import com.opengamma.financial.security.equity.EquitySecurity;
import com.opengamma.id.VersionCorrection;
import com.opengamma.sesame.EngineTestUtils;
import com.opengamma.sesame.Environment;
import com.opengamma.sesame.config.FunctionModelConfig;
import com.opengamma.sesame.config.ViewConfig;
import com.opengamma.sesame.function.AvailableImplementationsImpl;
import com.opengamma.sesame.function.AvailableOutputs;
import com.opengamma.sesame.function.AvailableOutputsImpl;
import com.opengamma.sesame.function.Output;
import com.opengamma.sesame.marketdata.CycleMarketDataFactory;
import com.opengamma.sesame.marketdata.StrategyAwareMarketDataSource;
import com.opengamma.util.test.TestGroup;
/**
 * Test that demonstrates functions that take something other than a trade, position or security as their input.
 * It shows the creation of types that wrap a trade and security, functions that accept them and how a view
 * can be configured to use them.
 */
@Test(groups = TestGroup.UNIT)
public class InputTypesTest {

  @Test
  public void tradeWithSecurity() {
    ViewConfig viewConfig = configureView("Trade with security", column("Foo"));
    AvailableOutputs availableOutputs = new AvailableOutputsImpl(EquityTradeWithSecurity.class, CashFlowTradeWithSecurity.class);
    availableOutputs.register(EquityTradeWithSecurityFn.class, CashFlowTradeWithSecurityFn.class);
    AvailableImplementationsImpl availableImplementations = new AvailableImplementationsImpl();
    availableImplementations.register(EquityTradeWithSecurityImpl.class, CashFlowTradeWithSecurityImpl.class);

    CachingManager cachingManager = new NoOpCachingManager(ComponentMap.EMPTY);
    ViewFactory viewFactory = new ViewFactory(new EngineTestUtils.DirectExecutorService(),
                                              availableOutputs,
                                              availableImplementations,
                                              FunctionModelConfig.EMPTY,
                                              EnumSet.noneOf(FunctionService.class),
                                              cachingManager);

    View view = viewFactory.createView(viewConfig, EquityTradeWithSecurity.class, CashFlowTradeWithSecurity.class);
    CycleArguments cycleArguments = new CycleArguments(ZonedDateTime.now(), VersionCorrection.LATEST,
                                                       mockCycleMarketDataFactory());
    Trade equityTrade = EngineTestUtils.createEquityTrade();
    Trade cashFlowTrade = EngineTestUtils.createCashFlowTrade();
    EquityTradeWithSecurity equityTradeWithSecurity =
        new EquityTradeWithSecurity(equityTrade, (EquitySecurity) equityTrade.getSecurity());
    CashFlowTradeWithSecurity cashFlowTradeWithSecurity =
        new CashFlowTradeWithSecurity(cashFlowTrade, (CashFlowSecurity) cashFlowTrade.getSecurity());

    Results results = view.run(cycleArguments, Lists.newArrayList(equityTradeWithSecurity, cashFlowTradeWithSecurity));

    ResultItem equityItem = results.get(0, "Foo");
    assertNotNull(equityItem);
    assertTrue(equityItem.getResult().isSuccess());
    assertEquals("1 x " + EngineTestUtils.EQUITY_NAME, equityItem.getResult().getValue());

    ResultItem cashFlowItem = results.get(1, "Foo");
    assertNotNull(cashFlowItem);
    assertTrue(cashFlowItem.getResult().isSuccess());
    assertEquals("1 x " + EngineTestUtils.CASH_FLOW_NAME, cashFlowItem.getResult().getValue());
  }

  private CycleMarketDataFactory mockCycleMarketDataFactory() {
    CycleMarketDataFactory cycleMarketDataFactory = mock(CycleMarketDataFactory.class);
    when(cycleMarketDataFactory.getPrimaryMarketDataSource()).thenReturn(mock(StrategyAwareMarketDataSource.class));
    return cycleMarketDataFactory;
  }

  /**
   * Wrapper for an equity {@link Trade} and the associated {@link EquitySecurity}.
   */
  public static class EquityTradeWithSecurity {

    private final Trade _trade;
    private final EquitySecurity _security;

    public EquityTradeWithSecurity(Trade trade, EquitySecurity security) {
      _trade = trade;
      _security = security;
    }
  }

  /**
   * Wrapper for a cash flow {@link Trade} and the associated {@link CashFlowSecurity}.
   */
  public static class CashFlowTradeWithSecurity {

    private final Trade _trade;
    private final CashFlowSecurity _security;

    public CashFlowTradeWithSecurity(Trade trade, CashFlowSecurity security) {
      _trade = trade;
      _security = security;
    }
  }

  /**
   * Function interface that takes an {@link EquityTradeWithSecurity}.
   */
  public interface EquityTradeWithSecurityFn {

    @Output("Foo")
    String foo(Environment env, EquityTradeWithSecurity tradeWithSecurity);
  }

  /**
   * Function implementation that takes an {@link EquityTradeWithSecurity}.
   */
  public static class EquityTradeWithSecurityImpl implements EquityTradeWithSecurityFn {

    @Override
    public String foo(Environment env, EquityTradeWithSecurity tradeWithSecurity) {
      return tradeWithSecurity._trade.getQuantity() + " x " + tradeWithSecurity._security.getName();
    }
  }

  /**
   * Function interface that takes an {@link CashFlowTradeWithSecurity}.
   */
  public interface CashFlowTradeWithSecurityFn {

    @Output("Foo")
    String foo(Environment env, CashFlowTradeWithSecurity tradeWithSecurity);
  }

  /**
   * Function implementation that takes an {@link CashFlowTradeWithSecurity}.
   */
  public static class CashFlowTradeWithSecurityImpl implements CashFlowTradeWithSecurityFn {

    @Override
    public String foo(Environment env, CashFlowTradeWithSecurity tradeWithSecurity) {
      return tradeWithSecurity._trade.getQuantity() + " x " + tradeWithSecurity._security.getName();
    }
  }
}
