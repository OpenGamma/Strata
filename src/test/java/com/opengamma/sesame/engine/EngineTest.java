/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.engine;

import static com.opengamma.sesame.config.ConfigBuilder.column;
import static com.opengamma.sesame.config.ConfigBuilder.output;
import static com.opengamma.sesame.config.ConfigBuilder.viewDef;
import static org.testng.AssertJUnit.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.core.position.Trade;
import com.opengamma.core.position.impl.SimpleTrade;
import com.opengamma.core.security.impl.SimpleSecurityLink;
import com.opengamma.financial.security.equity.EquitySecurity;
import com.opengamma.id.ExternalId;
import com.opengamma.id.UniqueId;
import com.opengamma.sesame.config.ViewDef;
import com.opengamma.sesame.example.EquityDescriptionFunction;
import com.opengamma.sesame.example.OutputNames;
import com.opengamma.sesame.function.MapFunctionRepo;
import com.opengamma.util.money.Currency;
import com.opengamma.util.test.TestGroup;

@Test(groups = TestGroup.UNIT)
public class EngineTest {

  protected static final UniqueId TRADE_ID = UniqueId.of("trdId", "321");
  protected static final String DESCRIPTION_HEADER = "Description";
  protected static final String SECURITY_NAME = "My first security";

  @Test
  public void defaultFunctionImpls() {
    ViewDef viewDef =
        viewDef("Trivial Test View",
                column(DESCRIPTION_HEADER,
                       output(OutputNames.DESCRIPTION, EquitySecurity.class)));
    MapFunctionRepo functionRepo = new MapFunctionRepo();
    functionRepo.register(EquityDescriptionFunction.class);
    Engine engine = new Engine(new DirectExecutorService(), functionRepo);
    Listener listener = new Listener();
    List<Trade> trades = createTrades();
    Engine.View view = engine.createView(viewDef, trades, listener);
    view.run();
    Results results = listener.getResults();
    Map<String, Object> tradeResults = results.getTargetResults(TRADE_ID.getObjectId());
    Object descriptionColumnResult = tradeResults.get(DESCRIPTION_HEADER);
    assertEquals(SECURITY_NAME, descriptionColumnResult);
    System.out.println(results);
  }

  private static List<Trade> createTrades() {
    EquitySecurity security = new EquitySecurity("exc", "exc", "compName", Currency.AUD);
    security.setUniqueId(UniqueId.of("secId", "123"));
    security.setName(SECURITY_NAME);
    SimpleTrade trade = new SimpleTrade();
    SimpleSecurityLink securityLink = new SimpleSecurityLink(ExternalId.of("extId", "123"));
    securityLink.setTarget(security);
    trade.setSecurityLink(securityLink);
    trade.setUniqueId(TRADE_ID);
    return ImmutableList.<Trade>of(trade);
  }

  private static class Listener implements Engine.Listener {

    private Results _results;

    @Override
    public void cycleComplete(Results results) {
      _results = results;
    }

    public Results getResults() {
      return _results;
    }
  }

  private static class DirectExecutorService extends AbstractExecutorService {

    @Override
    public void execute(Runnable command) {
      command.run();
    }

    @Override
    public void shutdown() {
      throw new UnsupportedOperationException("shutdown not supported");
    }

    @Override
    public List<Runnable> shutdownNow() {
      throw new UnsupportedOperationException("shutdownNow not supported");
    }

    @Override
    public boolean isShutdown() {
      throw new UnsupportedOperationException("isShutdown not supported");
    }

    @Override
    public boolean isTerminated() {
      throw new UnsupportedOperationException("isTerminated not supported");
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
      throw new UnsupportedOperationException("awaitTermination not supported");
    }
  }
}
