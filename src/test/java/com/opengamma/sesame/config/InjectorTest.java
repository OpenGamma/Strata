/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.config;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import java.util.Collections;

import org.testng.annotations.Test;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.ZonedDateTime;

import com.google.common.collect.ImmutableMap;
import com.opengamma.financial.security.equity.EquitySecurity;
import com.opengamma.util.test.TestGroup;

@Test(groups = TestGroup.UNIT)
public class InjectorTest {

  /* package */ static final String VALUE_NAME = "ValueName";

  private static final String INFRASTRUCTURE_COMPONENT = "some pretend infrastructure";

  @Test
  public void defaultImpl() {
    Injector injector = new Injector();
    TestFunction fn = injector.create(TestFunction.class, requirement());
    assertTrue(fn instanceof Default);
  }

  @Test
  public void overriddenImpl() {
    Injector injector = new Injector();
    ColumnRequirement requirement = requirement(TestFunction.class, Alternative.class);
    TestFunction fn = injector.create(TestFunction.class, requirement);
    assertTrue(fn instanceof Alternative);
  }

  @Test
  public void infrastructure() {
    Injector injector = new Injector(ImmutableMap.<Class<?>, Object>of(String.class, INFRASTRUCTURE_COMPONENT));
    ColumnRequirement requirement = requirement(TestFunction.class, Infrastructure.class);
    TestFunction fn = injector.create(TestFunction.class, requirement);
    assertTrue(fn instanceof Infrastructure);
    //noinspection ConstantConditions
    assertEquals(INFRASTRUCTURE_COMPONENT, ((Infrastructure) fn)._infrastructureComponent);
  }

  @Test
  public void defaultUserParams() {
    Injector injector = new Injector();
    ColumnRequirement requirement = requirement(TestFunction.class, UserParameters.class);
    TestFunction fn = injector.create(TestFunction.class, requirement);
    assertTrue(fn instanceof UserParameters);
    //noinspection ConstantConditions
    assertEquals(9, ((UserParameters) fn)._i);
    //noinspection ConstantConditions
    assertEquals(ZonedDateTime.of(2011, 3, 8, 2, 18, 0, 0, ZoneOffset.UTC), ((UserParameters) fn)._dateTime);
  }

  @Test
  public void overriddenUserParam() {
    Injector injector = new Injector();
    FunctionArguments args = new FunctionArguments(ImmutableMap.<String, Object>of("i", 12));
    ColumnRequirement requirement =
        new ColumnRequirement(VALUE_NAME,
                              EquitySecurity.class,
                              ImmutableMap.<Class<?>, Class<?>>of(TestFunction.class, UserParameters.class),
                              ImmutableMap.<Class<?>, FunctionArguments>of(UserParameters.class, args));
    TestFunction fn = injector.create(TestFunction.class, requirement);
    assertTrue(fn instanceof UserParameters);
    //noinspection ConstantConditions
    assertEquals(12, ((UserParameters) fn)._i);
    //noinspection ConstantConditions
    assertEquals(ZonedDateTime.of(2011, 3, 8, 2, 18, 0, 0, ZoneOffset.UTC), ((UserParameters) fn)._dateTime);
  }

  @Test
  public void functionCallingOtherFunction() {
    Injector injector = new Injector();
    ColumnRequirement requirement = requirement(TestFunction.class, CallsOtherFunction.class);
    TestFunction fn = injector.create(TestFunction.class, requirement);
    assertTrue(fn instanceof CallsOtherFunction);
    //noinspection ConstantConditions
    assertTrue(((CallsOtherFunction) fn)._collaborator instanceof Collaborator);
  }

  @Test
  public void concreteType() {


  }

  @Test
  public void noConstructors() {


  }

  @Test
  public void multipleInjectableConstructors() {


  }

  private static ColumnRequirement requirement() {
    return new ColumnRequirement(VALUE_NAME,
                                 EquitySecurity.class,
                                 Collections.<Class<?>, Class<?>>emptyMap(),
                                 Collections.<Class<?>, FunctionArguments>emptyMap());
  }

  private static ColumnRequirement requirement(Class<?> fnType, Class<?> implType) {
    return new ColumnRequirement(VALUE_NAME,
                                 EquitySecurity.class,
                                 ImmutableMap.<Class<?>, Class<?>>of(fnType, implType),
                                 Collections.<Class<?>, FunctionArguments>emptyMap());
  }
}

@EngineFunction(InjectorTest.VALUE_NAME)
@DefaultImplementation(Default.class)
/* package */ interface TestFunction { }

/* package */ class Default implements TestFunction { }

/* package */ class Alternative implements TestFunction { }

/* package */ class Infrastructure implements TestFunction {

  /* package */ final String _infrastructureComponent;

  /* package */ Infrastructure(String infrastructureComponent) {
    _infrastructureComponent = infrastructureComponent;
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
}

/* package */ class CallsOtherFunction implements TestFunction {

  /* package */ final CollaboratorFunction _collaborator;

  /* package */ CallsOtherFunction(CollaboratorFunction collaborator) {
    _collaborator = collaborator;
  }
}

@DefaultImplementation(Collaborator.class)
/* package */ interface CollaboratorFunction { }

/* package */ class Collaborator implements CollaboratorFunction { }
