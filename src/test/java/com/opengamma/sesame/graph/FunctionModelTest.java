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
import static org.testng.AssertJUnit.assertTrue;

import java.util.Collections;
import java.util.Map;

import org.testng.annotations.Test;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.ZonedDateTime;

import com.google.common.collect.ImmutableMap;
import com.opengamma.sesame.config.ConfigUtils;
import com.opengamma.sesame.config.FunctionConfig;
import com.opengamma.sesame.function.FunctionMetadata;
import com.opengamma.sesame.function.Output;
import com.opengamma.sesame.function.UserParam;
import com.opengamma.util.test.TestGroup;

@Test(groups = TestGroup.UNIT)
public class FunctionModelTest {

  private static final String INFRASTRUCTURE_COMPONENT = "some pretend infrastructure";
  private static final Map<Class<?>, Object> INFRASTRUCTURE = Collections.emptyMap();

  private FunctionMetadata functionMetadata() {
    return ConfigUtils.createMetadata(TestFunction.class, "foo");
  }

  @Test
  public void basicImpl() {
    FunctionConfig config = config(implementations(TestFunction.class, BasicImpl.class));
    FunctionModel functionModel = FunctionModel.forFunction(functionMetadata(), config);
    TestFunction fn = (TestFunction) functionModel.build(INFRASTRUCTURE).getReceiver();
    assertTrue(fn instanceof BasicImpl);
  }

  @Test
  public void infrastructure() {
    ImmutableMap<Class<?>, Object> infrastructure = ImmutableMap.<Class<?>, Object>of(String.class, INFRASTRUCTURE_COMPONENT);
    FunctionConfig config = config(implementations(TestFunction.class, InfrastructureImpl.class));
    FunctionModel functionModel = FunctionModel.forFunction(functionMetadata(), config, infrastructure.keySet());
    TestFunction fn = (TestFunction) functionModel.build(infrastructure).getReceiver();
    assertTrue(fn instanceof InfrastructureImpl);
    //noinspection ConstantConditions
    assertEquals(INFRASTRUCTURE_COMPONENT, ((InfrastructureImpl) fn)._infrastructureComponent);
  }

  @Test
  public void defaultUserParams() {
    FunctionConfig config = config(implementations(TestFunction.class, UserParameters.class));
    FunctionModel functionModel = FunctionModel.forFunction(functionMetadata(), config);
    TestFunction fn = (TestFunction) functionModel.build(INFRASTRUCTURE).getReceiver();
    assertTrue(fn instanceof UserParameters);
    //noinspection ConstantConditions
    assertEquals(9, ((UserParameters) fn)._i);
    //noinspection ConstantConditions
    assertEquals(ZonedDateTime.of(2011, 3, 8, 2, 18, 0, 0, ZoneOffset.UTC), ((UserParameters) fn)._dateTime);
  }

  @Test
  public void overriddenUserParam() {
    FunctionConfig config =
        config(implementations(TestFunction.class, UserParameters.class),
               arguments(
                   function(UserParameters.class,
                            argument("i", 12))));
    FunctionModel functionModel = FunctionModel.forFunction(functionMetadata(), config);
    TestFunction fn = (TestFunction) functionModel.build(INFRASTRUCTURE).getReceiver();
    assertTrue(fn instanceof UserParameters);
    //noinspection ConstantConditions
    assertEquals(12, ((UserParameters) fn)._i);
    //noinspection ConstantConditions
    assertEquals(ZonedDateTime.of(2011, 3, 8, 2, 18, 0, 0, ZoneOffset.UTC), ((UserParameters) fn)._dateTime);
  }

  @Test
  public void functionCallingOtherFunction() {
    FunctionConfig config = config(implementations(TestFunction.class, CallsOtherFunction.class,
                                                   CollaboratorFunction.class, Collaborator.class));
    FunctionModel functionModel = FunctionModel.forFunction(functionMetadata(), config);
    // TODO this return type will change soon
    TestFunction fn = (TestFunction) functionModel.build(INFRASTRUCTURE).getReceiver();
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
  public void cyclicDependency() {


  }
}

/* package */ interface TestFunction {

  @Output("Foo")
  Object foo();
}

/* package */ class BasicImpl implements TestFunction {

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

  /* package */ UserParameters(@UserParam(name = "i", fallbackValue = "9") int i,
                               @UserParam(name = "dateTime", fallbackValue = "2011-03-08T02:18Z") ZonedDateTime dateTime) {
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

/* package */ interface CollaboratorFunction { }

/* package */ class Collaborator implements CollaboratorFunction { }
