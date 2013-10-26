/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.graph;

import static org.testng.AssertJUnit.assertTrue;

import java.util.Collections;
import java.util.Map;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.ZonedDateTime;

import com.google.common.collect.ImmutableMap;
import com.opengamma.sesame.config.FunctionArguments;
import com.opengamma.sesame.config.FunctionConfig;
import com.opengamma.sesame.function.DefaultImplementation;
import com.opengamma.sesame.function.UserParam;
import com.opengamma.util.test.TestGroup;
import com.opengamma.util.tuple.Pair;

@Test(groups = TestGroup.UNIT)
public class TreeTest {

  private static final String INFRASTRUCTURE_COMPONENT = "some pretend infrastructure";
  private static final Map<Class<?>, Object> INFRASTRUCTURE = Collections.emptyMap();
  private static final Map<Pair<String, Class<?>>, Class<?>> OUTPUT_FUNCTIONS = Collections.emptyMap();

  // TODO test PortfolioOutputFunction implementation

  @Test
  public void defaultImpl() {
    Tree<TestFunction> tree = Tree.forFunction(TestFunction.class);
    TestFunction fn = tree.build(INFRASTRUCTURE);
    assertTrue(fn instanceof DefaultImpl);
  }

  @Test
  public void overriddenImpl() {
    FunctionConfig config = config(TestFunction.class, AlternativeImpl.class);
    Tree<TestFunction> tree = Tree.forFunction(TestFunction.class, config);
    TestFunction fn = tree.build(INFRASTRUCTURE);
    assertTrue(fn instanceof AlternativeImpl);
  }

  @Test
  public void infrastructure() {
    ImmutableMap<Class<?>, Object> infrastructure = ImmutableMap.<Class<?>, Object>of(String.class, INFRASTRUCTURE_COMPONENT);
    FunctionConfig config = config(TestFunction.class, InfrastructureImpl.class);
    Tree<TestFunction> tree = Tree.forFunction(TestFunction.class, config, infrastructure.keySet());
    TestFunction fn = tree.build(infrastructure);
    assertTrue(fn instanceof InfrastructureImpl);
    //noinspection ConstantConditions
    AssertJUnit.assertEquals(INFRASTRUCTURE_COMPONENT, ((InfrastructureImpl) fn)._infrastructureComponent);
  }

  @Test
  public void defaultUserParams() {
    FunctionConfig config = config(TestFunction.class, UserParameters.class);
    Tree<TestFunction> tree = Tree.forFunction(TestFunction.class, config);
    TestFunction fn = tree.build(INFRASTRUCTURE);
    assertTrue(fn instanceof UserParameters);
    //noinspection ConstantConditions
    AssertJUnit.assertEquals(9, ((UserParameters) fn)._i);
    //noinspection ConstantConditions
    AssertJUnit.assertEquals(ZonedDateTime.of(2011, 3, 8, 2, 18, 0, 0, ZoneOffset.UTC), ((UserParameters) fn)._dateTime);
  }

  @Test
  public void overriddenUserParam() {
    FunctionArguments args = new FunctionArguments(ImmutableMap.<String, Object>of("i", 12));
    FunctionConfig config =
        new FunctionConfig(OUTPUT_FUNCTIONS, ImmutableMap.<Class<?>, Class<?>>of(TestFunction.class, UserParameters.class),
                           ImmutableMap.<Class<?>, FunctionArguments>of(UserParameters.class, args));
    Tree<TestFunction> tree = Tree.forFunction(TestFunction.class, config);
    TestFunction fn = tree.build(INFRASTRUCTURE);
    assertTrue(fn instanceof UserParameters);
    //noinspection ConstantConditions
    AssertJUnit.assertEquals(12, ((UserParameters) fn)._i);
    //noinspection ConstantConditions
    AssertJUnit.assertEquals(ZonedDateTime.of(2011, 3, 8, 2, 18, 0, 0, ZoneOffset.UTC), ((UserParameters) fn)._dateTime);
  }

  @Test
  public void functionCallingOtherFunction() {
    FunctionConfig config = config(TestFunction.class, CallsOtherFunction.class);
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

  private static FunctionConfig config(Class<?> fnType, Class<?> implType) {
    return new FunctionConfig(OUTPUT_FUNCTIONS, ImmutableMap.<Class<?>, Class<?>>of(fnType, implType), Collections.<Class<?>, FunctionArguments>emptyMap());
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
