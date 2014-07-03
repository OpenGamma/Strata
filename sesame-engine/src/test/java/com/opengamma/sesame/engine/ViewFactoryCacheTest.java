package com.opengamma.sesame.engine;

import static com.opengamma.sesame.config.ConfigBuilder.argument;
import static com.opengamma.sesame.config.ConfigBuilder.arguments;
import static com.opengamma.sesame.config.ConfigBuilder.column;
import static com.opengamma.sesame.config.ConfigBuilder.config;
import static com.opengamma.sesame.config.ConfigBuilder.configureView;
import static com.opengamma.sesame.config.ConfigBuilder.function;
import static com.opengamma.sesame.config.ConfigBuilder.implementations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNotSame;
import static org.testng.AssertJUnit.assertSame;

import java.util.EnumSet;

import org.testng.annotations.Test;
import org.threeten.bp.ZonedDateTime;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.opengamma.engine.marketdata.spec.LiveMarketDataSpecification;
import com.opengamma.id.VersionCorrection;
import com.opengamma.service.ServiceContext;
import com.opengamma.service.ThreadLocalServiceContext;
import com.opengamma.sesame.EngineTestUtils;
import com.opengamma.sesame.cache.Cacheable;
import com.opengamma.sesame.config.FunctionModelConfig;
import com.opengamma.sesame.config.ViewConfig;
import com.opengamma.sesame.function.AvailableImplementations;
import com.opengamma.sesame.function.AvailableImplementationsImpl;
import com.opengamma.sesame.function.AvailableOutputs;
import com.opengamma.sesame.function.AvailableOutputsImpl;
import com.opengamma.sesame.function.Output;
import com.opengamma.sesame.marketdata.CycleMarketDataFactory;
import com.opengamma.sesame.marketdata.DefaultStrategyAwareMarketDataSource;
import com.opengamma.sesame.marketdata.MapMarketDataSource;
import com.opengamma.sesame.marketdata.StrategyAwareMarketDataSource;

/**
 * Tests the behaviour of {@link ViewFactory} WRT cache behaviour.
 */
public class ViewFactoryCacheTest {

  /**
  /**
   * checks that cached values created by a view are available next time it's run.
   */
  @Test
  public void cacheIsSharedBetweenRuns() {
    ViewConfig viewConfig =
        configureView(
            "view name",
            column(
                "Foo",
                config(implementations(TestFn.class, Impl.class),
                       arguments(function(Impl.class, argument("s", "s"))))));
    ViewFactory viewFactory = createViewFactory();
    View view = viewFactory.createView(viewConfig, String.class);
    ZonedDateTime now = ZonedDateTime.now();
    CycleMarketDataFactory cycleMarketDataFactory = mock(CycleMarketDataFactory.class);
    when(cycleMarketDataFactory.getPrimaryMarketDataSource()).thenReturn(mock(StrategyAwareMarketDataSource.class));
    CycleArguments cycleArguments = new CycleArguments(now, VersionCorrection.LATEST, cycleMarketDataFactory);
    Results results1 = view.run(cycleArguments, ImmutableList.of("bar"));
    Results results2 = view.run(cycleArguments, ImmutableList.of("bar"));
    assertSame(results1.get(0, 0).getResult().getValue(), results2.get(0, 0).getResult().getValue());
  }

  /**
   * Checks that whe we are capturing the cycle we do not use cached
   * values. (If we did we would not be able to intercept calls to the
   * sources as they would not be called.)
   */
  @Test
  public void cacheIsNotSharedBetweenRunsWhenCapturingCycle() {
    ThreadLocalServiceContext.init(ServiceContext.of(ImmutableMap.<Class<?>, Object>of()));
    ViewConfig viewConfig =
        configureView("view name",
                      column("Foo",
                             config(implementations(TestFn.class, Impl.class),
                                    arguments(function(Impl.class, argument("s", "s"))))));
    ViewFactory viewFactory = createViewFactory();
    View view = viewFactory.createView(viewConfig, String.class);
    ZonedDateTime now = ZonedDateTime.now();
    CycleMarketDataFactory cycleMarketDataFactory = mock(CycleMarketDataFactory.class);
    when(cycleMarketDataFactory.getPrimaryMarketDataSource()).thenReturn(new DefaultStrategyAwareMarketDataSource(
        LiveMarketDataSpecification.LIVE_SPEC, MapMarketDataSource.of()));
    CycleArguments cycleArguments = new CycleArguments(now, VersionCorrection.LATEST, cycleMarketDataFactory, true);
    Results results1 = view.run(cycleArguments, ImmutableList.of("bar"));
    Results results2 = view.run(cycleArguments, ImmutableList.of("bar"));
    assertNotSame(results1.get(0, 0).getResult().getValue(), results2.get(0, 0).getResult().getValue());
    assertNotNull(results1.getViewInputs());
    assertNotNull(results2.getViewInputs());
  }

  /**
   * checks that cached values created by a view are available to other views built by the same view factory.
   */
  @Test
  public void cacheIsSharedBetweenViews() {
    ViewConfig viewConfig =
        configureView("view name",
                      column("Foo",
                             config(implementations(TestFn.class, Impl.class),
                                    arguments(function(Impl.class, argument("s", "s"))))));
    ViewFactory viewFactory = createViewFactory();
    View view1 = viewFactory.createView(viewConfig, String.class);
    View view2 = viewFactory.createView(viewConfig, String.class);
    ZonedDateTime now = ZonedDateTime.now();
    CycleMarketDataFactory cycleMarketDataFactory = mock(CycleMarketDataFactory.class);
    when(cycleMarketDataFactory.getPrimaryMarketDataSource()).thenReturn(mock(StrategyAwareMarketDataSource.class));
    CycleArguments cycleArguments = new CycleArguments(now, VersionCorrection.LATEST, cycleMarketDataFactory);
    Results results1 = view1.run(cycleArguments, ImmutableList.of("bar"));
    Results results2 = view2.run(cycleArguments, ImmutableList.of("bar"));
    assertSame(results1.get(0, 0).getResult().getValue(), results2.get(0, 0).getResult().getValue());
  }

  private ViewFactory createViewFactory() {
    AvailableOutputs availableOutputs = new AvailableOutputsImpl(String.class);
    AvailableImplementations availableImplementations = new AvailableImplementationsImpl();
    availableOutputs.register(TestFn.class);
    availableImplementations.register(Impl.class);
    CachingManager cachingManager = new DefaultCachingManager(ComponentMap.EMPTY, EngineTestUtils.createCache());
    return new ViewFactory(new EngineTestUtils.DirectExecutorService(),
                           availableOutputs,
                           availableImplementations,
                           FunctionModelConfig.EMPTY,
                           EnumSet.of(FunctionService.CACHING),
                           cachingManager);
  }

  public interface TestFn {

    @Cacheable
    @Output("Foo")
    Object foo(String arg);
  }

  public static class Impl implements TestFn {

    private final String _s;

    public Impl(String s) {
      _s = s;
    }

    @Override
    public Object foo(String arg) {
      return _s + new Object();
    }
  }
}
