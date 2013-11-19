/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.graph;

import static com.opengamma.sesame.config.ConfigBuilder.argument;
import static com.opengamma.sesame.config.ConfigBuilder.arguments;
import static com.opengamma.sesame.config.ConfigBuilder.config;
import static com.opengamma.sesame.config.ConfigBuilder.function;
import static com.opengamma.sesame.config.ConfigBuilder.implementations;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import java.util.List;

import javax.inject.Provider;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.opengamma.sesame.config.ConfigUtils;
import com.opengamma.sesame.config.FunctionConfig;
import com.opengamma.sesame.config.GraphConfig;
import com.opengamma.sesame.engine.ComponentMap;
import com.opengamma.sesame.function.FunctionMetadata;
import com.opengamma.sesame.function.Output;
import com.opengamma.util.test.TestGroup;

@Test(groups = TestGroup.UNIT)
public class FunctionModelTest {

  private static final String INFRASTRUCTURE_COMPONENT = "some pretend infrastructure";
  private static final FunctionMetadata METADATA = ConfigUtils.createMetadata(TestFunction.class, "foo");

  @Test
  public void basicImpl() {
    FunctionConfig config = config(implementations(TestFunction.class, BasicImpl.class));
    FunctionModel functionModel = FunctionModel.forFunction(METADATA, config);
    TestFunction fn = (TestFunction) functionModel.build(new FunctionBuilder(), ComponentMap.EMPTY).getReceiver();
    assertTrue(fn instanceof BasicImpl);
  }

  @Test
  public void infrastructure() {
    final ComponentMap infrastructure = ComponentMap.of(
        ImmutableMap.<Class<?>, Object>of(String.class, INFRASTRUCTURE_COMPONENT));
    FunctionConfig config = config(implementations(TestFunction.class, InfrastructureImpl.class));
    GraphConfig graphConfig = new GraphConfig(config, infrastructure, NodeDecorator.IDENTITY);
    FunctionModel functionModel = FunctionModel.forFunction(METADATA, graphConfig);
    TestFunction fn = (TestFunction) functionModel.build(new FunctionBuilder(), infrastructure).getReceiver();
    assertTrue(fn instanceof InfrastructureImpl);
    //noinspection ConstantConditions
    assertEquals(INFRASTRUCTURE_COMPONENT, ((InfrastructureImpl) fn)._infrastructureComponent);
  }

  @Test
  public void functionCallingOtherFunction() {
    FunctionConfig config = config(implementations(TestFunction.class, CallsOtherFunction.class,
                                                   CollaboratorFunction.class, Collaborator.class));
    FunctionModel functionModel = FunctionModel.forFunction(METADATA, config);
    TestFunction fn = (TestFunction) functionModel.build(new FunctionBuilder(), ComponentMap.EMPTY).getReceiver();
    assertTrue(fn instanceof CallsOtherFunction);
    //noinspection ConstantConditions
    assertTrue(((CallsOtherFunction) fn)._collaborator instanceof Collaborator);
  }

  @Test
  public void concreteTypes() {
    FunctionMetadata metadata = ConfigUtils.createMetadata(Concrete1.class, "foo");
    FunctionModel functionModel = FunctionModel.forFunction(metadata);
    Concrete1 fn = (Concrete1) functionModel.build(new FunctionBuilder(), ComponentMap.EMPTY).getReceiver();
    assertNotNull(fn._concrete);
  }

  public void provider() {
    FunctionMetadata metadata = ConfigUtils.createMetadata(PrivateConstructor.class, "getName");
    String providerName = "the provider name";
    FunctionConfig config = config(implementations(PrivateConstructor.class, PrivateConstructorProvider.class),
                                   arguments(
                                       function(PrivateConstructorProvider.class,
                                                argument("providerName", providerName))));
    FunctionModel functionModel = FunctionModel.forFunction(metadata, config);
    PrivateConstructor fn = (PrivateConstructor) functionModel.build(new FunctionBuilder(), ComponentMap.EMPTY).getReceiver();
    assertEquals(providerName, fn.getName());
  }

  @Test
  public void decorators() {
    NodeDecorator decorator = new NodeDecorator() {

      @Override
      public Node decorateNode(final Node node) {
        return new DependentNode(Object.class, null, node) {
          @Override
          protected Object doCreate(ComponentMap componentMap, List<Object> dependencies) {
            final TestFunction fn = (TestFunction) dependencies.get(0);
            return new TestFunction() {
              @Override
              public Object foo() {
                return Lists.newArrayList("decorated", fn.foo());
              }
            };
          }
        };
      }
    };
    FunctionConfig config = config(implementations(TestFunction.class, BasicImpl.class));
    GraphConfig graphConfig = new GraphConfig(config, ComponentMap.EMPTY, decorator);
    FunctionModel functionModel = FunctionModel.forFunction(METADATA, graphConfig);
    TestFunction fn = (TestFunction) functionModel.build(new FunctionBuilder(), ComponentMap.EMPTY).getReceiver();
    // the basic method just returns "foo"
    assertEquals(Lists.newArrayList("decorated", "foo"), fn.foo());
  }

  @Test
  public void buildDirectly1() {
    Concrete1 fn = FunctionModel.build(Concrete1.class, "foo");
    assertNotNull(fn);
  }

  @Test
  public void buildDirectly2() {
    FunctionConfig config = config(implementations(TestFunction.class, BasicImpl.class));
    TestFunction fn = FunctionModel.build(TestFunction.class, "foo", config);
    assertTrue(fn instanceof BasicImpl);
  }

  @Test
  public void buildDirectly3() {
    final ComponentMap infrastructure = ComponentMap.of(
        ImmutableMap.<Class<?>, Object>of(String.class, INFRASTRUCTURE_COMPONENT));
    FunctionConfig config = config(implementations(TestFunction.class, InfrastructureImpl.class));
    GraphConfig graphConfig = new GraphConfig(config, infrastructure, NodeDecorator.IDENTITY);
    TestFunction fn = FunctionModel.build(TestFunction.class, "foo", graphConfig);
    assertTrue(fn instanceof InfrastructureImpl);
    //noinspection ConstantConditions
    assertEquals(INFRASTRUCTURE_COMPONENT, ((InfrastructureImpl) fn)._infrastructureComponent);
  }

  @Test
  public void noVisibleConstructors() {


  }

  @Test
  public void multipleInjectableConstructors() {


  }

  @Test
  public void infrastructureNotFound() {


  }

  @Test
  public void cyclicDependency() {


  }

  @Test
  public void sharedNodes() {
    // TODO or should this be in a test for FunctionBuilder

  }
}

/* package */ interface TestFunction {

  @Output("Foo")
  Object foo();
}

/* package */ class BasicImpl implements TestFunction {

  @Override
  public Object foo() {
    return "foo";
  }
}

/* package */ class InfrastructureImpl implements TestFunction {

  /* package */ final String _infrastructureComponent;

  /* package */ InfrastructureImpl(String infrastructureComponent) {
    _infrastructureComponent = infrastructureComponent;
  }

  @Override
  public Object foo() {
    return null;
  }
}

/* package */ class CallsOtherFunction implements TestFunction {

  /* package */ final CollaboratorFunction _collaborator;

  /* package */ CallsOtherFunction(CollaboratorFunction collaborator) {
    _collaborator = collaborator;
  }

  @Override
  public Object foo() {
    return null;
  }
}

/* package */ interface CollaboratorFunction { }

/* package */ class Collaborator implements CollaboratorFunction { }

/* package */ class Concrete1 {

  /* package */ final Concrete2 _concrete;

  /* package */ Concrete1(Concrete2 concrete) {
    _concrete = concrete;
  }

  @Output("Foo")
  public Object foo() {
    return null;
  }
}
/* package */ class Concrete2 { }

/**
 * A class with a private constructor that can only be created via a factory method. Need to use a provider to build.
 */
/* package */ class PrivateConstructor {

  private final String _name;

  private PrivateConstructor(String name) {
    _name = name;
  }

  /* package */ static PrivateConstructor build(String name) {
    return new PrivateConstructor(name);
  }

  @Output("Name")
  public String getName() {
    return _name;
  }
}

/**
 * A provider that creates a class using a factory method. Has injectable parameters of its own.
 */
/* package */ class PrivateConstructorProvider implements Provider<PrivateConstructor> {

  private final String _providerName;

  /* package */ PrivateConstructorProvider(String providerName) {
    _providerName = providerName;
  }

  @Override
  public PrivateConstructor get() {
    return PrivateConstructor.build(_providerName);
  }
}
