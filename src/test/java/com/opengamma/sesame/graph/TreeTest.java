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
import static com.opengamma.sesame.config.ConfigBuilder.overrides;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import java.util.Collections;
import java.util.Map;

import org.testng.annotations.Test;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.ZonedDateTime;

import com.google.common.collect.ImmutableMap;
import com.opengamma.sesame.config.FunctionConfig;
import com.opengamma.sesame.function.DefaultImplementation;
import com.opengamma.sesame.function.UserParam;
import com.opengamma.util.test.TestGroup;

@Test(groups = TestGroup.UNIT)
public class TreeTest {

  private static final String INFRASTRUCTURE_COMPONENT = "some pretend infrastructure";
  private static final Map<Class<?>, Object> INFRASTRUCTURE = Collections.emptyMap();

  // TODO test PortfolioOutputFunction implementation

  @Test
  public void defaultImpl() {
    Tree<TestFunction> tree = Tree.forFunction(TestFunction.class);
    TestFunction fn = tree.build(INFRASTRUCTURE);
    assertTrue(fn instanceof DefaultImpl);
  }

  @Test
  public void overriddenImpl() {
    FunctionConfig config = config(overrides(TestFunction.class, AlternativeImpl.class));
    Tree<TestFunction> tree = Tree.forFunction(TestFunction.class, config);
    TestFunction fn = tree.build(INFRASTRUCTURE);
    assertTrue(fn instanceof AlternativeImpl);
  }

  @Test
  public void infrastructure() {
    ImmutableMap<Class<?>, Object> infrastructure = ImmutableMap.<Class<?>, Object>of(String.class, INFRASTRUCTURE_COMPONENT);
    FunctionConfig config = config(overrides(TestFunction.class, InfrastructureImpl.class));
    Tree<TestFunction> tree = Tree.forFunction(TestFunction.class, config, infrastructure.keySet());
    TestFunction fn = tree.build(infrastructure);
    assertTrue(fn instanceof InfrastructureImpl);
    //noinspection ConstantConditions
    assertEquals(INFRASTRUCTURE_COMPONENT, ((InfrastructureImpl) fn)._infrastructureComponent);
  }

  @Test
  public void defaultUserParams() {
    FunctionConfig config = config(overrides(TestFunction.class, UserParameters.class));
    Tree<TestFunction> tree = Tree.forFunction(TestFunction.class, config);
    TestFunction fn = tree.build(INFRASTRUCTURE);
    assertTrue(fn instanceof UserParameters);
    //noinspection ConstantConditions
    assertEquals(9, ((UserParameters) fn)._i);
    //noinspection ConstantConditions
    assertEquals(ZonedDateTime.of(2011, 3, 8, 2, 18, 0, 0, ZoneOffset.UTC), ((UserParameters) fn)._dateTime);
  }

  @Test
  public void overriddenUserParam() {
    FunctionConfig config =
        config(overrides(TestFunction.class, UserParameters.class),
               arguments(
                   function(UserParameters.class,
                            argument("i", 12))));
    Tree<TestFunction> tree = Tree.forFunction(TestFunction.class, config);
    TestFunction fn = tree.build(INFRASTRUCTURE);
    assertTrue(fn instanceof UserParameters);
    //noinspection ConstantConditions
    assertEquals(12, ((UserParameters) fn)._i);
    //noinspection ConstantConditions
    assertEquals(ZonedDateTime.of(2011, 3, 8, 2, 18, 0, 0, ZoneOffset.UTC), ((UserParameters) fn)._dateTime);
  }

  @Test
  public void functionCallingOtherFunction() {
    FunctionConfig config = config(overrides(TestFunction.class, CallsOtherFunction.class));
    Tree<TestFunction> tree = Tree.forFunction(TestFunction.class, config);
    TestFunction fn = tree.build(INFRASTRUCTURE);
    assertTrue(fn instanceof CallsOtherFunction);
    //noinspection ConstantConditions
    assertTrue(((CallsOtherFunction) fn)._collaborator instanceof Collaborator);
  }

  @Test
  public void concreteFunctionType() {


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
  public void noDefaultImpl() {


  }

  @Test
  public void cyclicDependency() {


  }
}

@DefaultImplementation(DefaultImpl.class)
/* package */ interface TestFunction {

  Object foo();
}

/* package */ class DefaultImpl implements TestFunction {

  @Override
  public Object foo() {
    return null;
  }
}

/* package */ class AlternativeImpl implements TestFunction {

  @Override
  public Object foo() {
    return null;
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

/* package */ class UserParameters implements TestFunction {

  /* package */ final int _i;
  /* package */ final ZonedDateTime _dateTime;

  /* package */ UserParameters(@UserParam(name = "i", defaultValue = "9") int i,
                               @UserParam(name = "dateTime", defaultValue = "2011-03-08T02:18Z") ZonedDateTime dateTime) {
    _i = i;
    _dateTime = dateTime;
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

@DefaultImplementation(Collaborator.class)
/* package */ interface CollaboratorFunction { }

/* package */ class Collaborator implements CollaboratorFunction { }
