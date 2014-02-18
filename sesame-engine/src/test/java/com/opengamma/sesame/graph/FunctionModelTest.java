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
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import java.util.List;

import javax.inject.Provider;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.opengamma.sesame.config.EngineFunctionUtils;
import com.opengamma.sesame.config.FunctionModelConfig;
import com.opengamma.sesame.config.GraphConfig;
import com.opengamma.sesame.engine.ComponentMap;
import com.opengamma.sesame.function.FunctionMetadata;
import com.opengamma.sesame.function.Output;
import com.opengamma.util.test.TestGroup;

@Test(groups = TestGroup.UNIT)
public class FunctionModelTest {

  private static final String INFRASTRUCTURE_COMPONENT = "some pretend infrastructure";
  private static final FunctionMetadata METADATA = EngineFunctionUtils.createMetadata(TestFn.class, "foo");

  @Test
  public void basicImpl() {
    FunctionModelConfig config = config(implementations(TestFn.class, BasicImpl.class));
    FunctionModel functionModel = FunctionModel.forFunction(METADATA, config);
    TestFn fn = (TestFn) functionModel.build(new FunctionBuilder(), ComponentMap.EMPTY).getReceiver();
    assertTrue(fn instanceof BasicImpl);
  }

  @Test
  public void infrastructure() {
    ComponentMap infrastructure = ComponentMap.of(ImmutableMap.<Class<?>, Object>of(String.class,
                                                                                    INFRASTRUCTURE_COMPONENT));
    FunctionModelConfig config = config(implementations(TestFn.class, InfrastructureImpl.class));
    GraphConfig graphConfig = new GraphConfig(config, infrastructure, NodeDecorator.IDENTITY);
    FunctionModel functionModel = FunctionModel.forFunction(METADATA, graphConfig);
    TestFn fn = (TestFn) functionModel.build(new FunctionBuilder(), infrastructure).getReceiver();
    assertTrue(fn instanceof InfrastructureImpl);
    //noinspection ConstantConditions
    assertEquals(INFRASTRUCTURE_COMPONENT, ((InfrastructureImpl) fn)._infrastructureComponent);
  }

  @Test
  public void functionCallingOtherFunction() {
    FunctionModelConfig config = config(implementations(TestFn.class, CallsOtherFn.class,
                                                        CollaboratorFn.class, Collaborator.class));
    FunctionModel functionModel = FunctionModel.forFunction(METADATA, config);
    TestFn fn = (TestFn) functionModel.build(new FunctionBuilder(), ComponentMap.EMPTY).getReceiver();
    assertTrue(fn instanceof CallsOtherFn);
    //noinspection ConstantConditions
    assertTrue(((CallsOtherFn) fn)._collaborator instanceof Collaborator);
  }

  @Test
  public void concreteTypes() {
    FunctionMetadata metadata = EngineFunctionUtils.createMetadata(Concrete1.class, "foo");
    FunctionModel functionModel = FunctionModel.forFunction(metadata);
    Concrete1 fn = (Concrete1) functionModel.build(new FunctionBuilder(), ComponentMap.EMPTY).getReceiver();
    assertNotNull(fn._concrete);
  }

  public void provider() {
    FunctionMetadata metadata = EngineFunctionUtils.createMetadata(PrivateConstructor.class, "getName");
    String providerName = "the provider name";
    FunctionModelConfig config = config(implementations(PrivateConstructor.class, PrivateConstructorProvider.class),
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
            final TestFn fn = (TestFn) dependencies.get(0);
            return new TestFn() {
              @Override
              public Object foo() {
                return Lists.newArrayList("decorated", fn.foo());
              }
            };
          }
        };
      }
    };
    FunctionModelConfig config = config(implementations(TestFn.class, BasicImpl.class));
    GraphConfig graphConfig = new GraphConfig(config, ComponentMap.EMPTY, decorator);
    FunctionModel functionModel = FunctionModel.forFunction(METADATA, graphConfig);
    TestFn fn = (TestFn) functionModel.build(new FunctionBuilder(), ComponentMap.EMPTY).getReceiver();
    // the basic method just returns "foo"
    assertEquals(Lists.newArrayList("decorated", "foo"), fn.foo());
  }

  @Test
  public void buildDirectly1() {
    Concrete1 fn = FunctionModel.build(Concrete1.class);
    assertNotNull(fn);
  }

  @Test
  public void buildDirectly2() {
    FunctionModelConfig config = config(implementations(TestFn.class, BasicImpl.class));
    TestFn fn = FunctionModel.build(TestFn.class, config);
    assertTrue(fn instanceof BasicImpl);
  }

  @Test
  public void buildDirectly3() {
    ComponentMap infrastructure = ComponentMap.of(ImmutableMap.<Class<?>, Object>of(String.class,
                                                                                    INFRASTRUCTURE_COMPONENT));
    FunctionModelConfig config = config(implementations(TestFn.class, InfrastructureImpl.class));
    GraphConfig graphConfig = new GraphConfig(config, infrastructure, NodeDecorator.IDENTITY);
    TestFn fn = FunctionModel.build(TestFn.class, graphConfig);
    assertTrue(fn instanceof InfrastructureImpl);
    //noinspection ConstantConditions
    assertEquals(INFRASTRUCTURE_COMPONENT, ((InfrastructureImpl) fn)._infrastructureComponent);
  }

  @Test
  public void noVisibleConstructors() {
    FunctionMetadata metadata = EngineFunctionUtils.createMetadata(PrivateConstructor.class, "getName");
    FunctionModelConfig config = config(arguments(function(PrivateConstructor.class, argument("name", "the name"))));
    FunctionModel functionModel = FunctionModel.forFunction(metadata, config);
    assertFalse(functionModel.isValid());
  }

  @Test
  public void infrastructureNotFound() {
    FunctionModelConfig config = config(implementations(TestFn.class, InfrastructureImpl.class));
    GraphConfig graphConfig = new GraphConfig(config, ComponentMap.EMPTY, NodeDecorator.IDENTITY);
    FunctionModel functionModel = FunctionModel.forFunction(METADATA, graphConfig);
    assertFalse(functionModel.isValid());
  }

  @Test
  public void multipleInjectableConstructors() {
    FunctionMetadata metadata = EngineFunctionUtils.createMetadata(NoSuitableConstructor.class, "foo");
    FunctionModel functionModel = FunctionModel.forFunction(metadata);
    assertFalse(functionModel.isValid());
  }

  /** test that error nodes are marked in the pretty printed output */
  @Test
  public void prettyPrintErrors() {
    FunctionModelConfig config = config(implementations(TestFn.class, CallsOtherFn.class,
                                                        CollaboratorFn.class, BrokenCollaborator.class));
    FunctionModel functionModel = FunctionModel.forFunction(METADATA, config);
    String tree = functionModel.prettyPrint();
    String[] lines = tree.split("\n");
    assertEquals(4, lines.length);
    System.out.println(tree);
    assertTrue(lines[3].startsWith("->"));
  }

  /** test that non-error nodes aren't marked in the pretty printed output */
  @Test
  public void prettyPrintNoErrors() {
    FunctionModelConfig config = config(implementations(TestFn.class, CallsOtherFn.class,
                                                        CollaboratorFn.class, Collaborator.class));
    FunctionModel functionModel = FunctionModel.forFunction(METADATA, config);
    String tree = functionModel.prettyPrint();
    String[] lines = tree.split("\n");
    assertEquals(3, lines.length);
    assertFalse(lines[2].startsWith("->"));
    System.out.println(tree);
  }

  @Test
  public void cyclicDependency() {


  }

  @Test
  public void sharedNodes() {
    // TODO or should this be in a test for FunctionBuilder

  }
}

/* package */ interface TestFn {

  @Output("Foo")
  Object foo();
}

/* package */ class BasicImpl implements TestFn {

  public BasicImpl() {
  }

  @Override
  public Object foo() {
    return "foo";
  }
}

/* package */ class InfrastructureImpl implements TestFn {

  /* package */ final String _infrastructureComponent;

  public InfrastructureImpl(String infrastructureComponent) {
    _infrastructureComponent = infrastructureComponent;
  }

  @Override
  public Object foo() {
    return null;
  }
}

/* package */ class CallsOtherFn implements TestFn {

  /* package */ final CollaboratorFn _collaborator;

  public CallsOtherFn(CollaboratorFn collaborator) {
    _collaborator = collaborator;
  }

  @Override
  public Object foo() {
    return null;
  }
}

/* package */ interface CollaboratorFn { }

/* package */ class Collaborator implements CollaboratorFn {

  public Collaborator() {
  }
}

/* package */ class BrokenCollaborator implements CollaboratorFn {

  public BrokenCollaborator(Object unsatisfiedArg) {
  }
}

/* package */ class Concrete1 {

  /* package */ final Concrete2 _concrete;

  public Concrete1(Concrete2 concrete) {
    _concrete = concrete;
  }

  @Output("Foo")
  public Object foo() {
    return null;
  }
}
/* package */ class Concrete2 {

  public Concrete2() {
  }
}

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

  public PrivateConstructorProvider(String providerName) {
    _providerName = providerName;
  }

  @Override
  public PrivateConstructor get() {
    return PrivateConstructor.build(_providerName);
  }
}

/* package */ class NoSuitableConstructor {

  public NoSuitableConstructor() {
  }

  public NoSuitableConstructor(String ignored) {
  }

  @Output("Foo")
  public Object foo() {
    return null;
  }
}
