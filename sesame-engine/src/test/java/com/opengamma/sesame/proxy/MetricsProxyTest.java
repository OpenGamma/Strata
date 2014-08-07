package com.opengamma.sesame.proxy;

import static com.opengamma.sesame.config.ConfigBuilder.config;
import static com.opengamma.sesame.config.ConfigBuilder.implementations;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.hamcrest.core.Is.is;

import java.util.Random;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.opengamma.sesame.config.FunctionModelConfig;
import com.opengamma.sesame.engine.ComponentMap;
import com.opengamma.sesame.function.Output;
import com.opengamma.sesame.graph.FunctionModel;
import com.opengamma.util.metric.OpenGammaMetricRegistry;
import com.opengamma.util.result.Result;
import com.opengamma.util.test.TestGroup;

/**
 * Tests the metrics proxy records data in a Metrics repository.
 */
@Test(groups = TestGroup.UNIT)
public class MetricsProxyTest {

  private MetricRegistry _registry;

  @BeforeMethod
  public void setUp() {
    _registry = new MetricRegistry();
  }

  @Test
  public void metricsShouldGetRecordedForInterfaceMethods() {

    MockFn fn = createMockFn(SimpleMockFn.class);

    for (int i = 0; i < 10; i++) {
      fn.doSomething();
      fn.doSomethingElse();
    }

    assertThat(_registry.getTimers().size(), is(2));

    String[] expectedKeys = {
        "com.opengamma.sesame.proxy.MetricsProxyTest.SimpleMockFn.doSomething",
        "com.opengamma.sesame.proxy.MetricsProxyTest.SimpleMockFn.doSomethingElse"};

    assertThat(_registry.getTimers().keySet(), contains(expectedKeys));
    for (Timer timer : _registry.getTimers().values()) {
      assertThat(timer.getCount(), is(10L));
    }
  }

  @Test
  public void noMetricsForStandardObjectMethods() {

    MockFn fn = createMockFn(SimpleMockFn.class);

    for (int i = 0; i < 10; i++) {
      fn.hashCode();
      fn.toString();
    }

    assertThat(_registry.getTimers().isEmpty(), is(true));
  }

  private MockFn createMockFn(Class<? extends MockFn> implementationClass) {
    FunctionModelConfig config = config(implementations(MockFn.class, implementationClass));
    return FunctionModel.build(MockFn.class, config, ComponentMap.EMPTY,
                               ExceptionWrappingProxy.INSTANCE, MetricsProxy.of(_registry));
  }

  private interface MockFn {
    @Output("this")
    Result<Boolean> doSomething();
    @Output("that")
    Result<Boolean> doSomethingElse();
  }

  // Class needs to be public so that its default
  // constructor is visible to the FunctionModelConfig
  public static class SimpleMockFn implements MockFn {

    public Result<Boolean> doSomething() {
      randomWait();
      return Result.success(true);
    }

    @Override
    public Result<Boolean> doSomethingElse() {
      randomWait();
      return Result.success(true);
    }

    private void randomWait() {
      try {
        Thread.sleep((new Random()).nextInt(100));
      } catch (InterruptedException e) {
        // Ignored
      }
    }
  }

}
