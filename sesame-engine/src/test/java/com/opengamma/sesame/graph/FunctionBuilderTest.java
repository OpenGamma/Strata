/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.graph;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

import java.util.List;

import org.testng.annotations.Test;

import com.opengamma.sesame.config.EngineUtils;
import com.opengamma.sesame.engine.ComponentMap;
import com.opengamma.sesame.function.FunctionMetadata;
import com.opengamma.sesame.function.Output;
import com.opengamma.sesame.function.Parameter;
import com.opengamma.util.test.TestGroup;

/**
 * Test.
 */
@Test(groups = TestGroup.UNIT)
public class FunctionBuilderTest {

  @Test(expectedExceptions = GraphBuildException.class)
  public void invalidGraph() {
    FunctionMetadata metadata = EngineUtils.createMetadata(NoPublicConstructor.class, "foo");
    FunctionModel functionModel = FunctionModel.forFunction(metadata);
    assertFalse(functionModel.isValid());
    functionModel.build(new FunctionBuilder(), ComponentMap.EMPTY);
  }

  @Test
  public void multipleInvalidNodes() {
    FunctionMetadata metadata = EngineUtils.createMetadata(RootFn.class, "foo");
    FunctionModel functionModel = FunctionModel.forFunction(metadata);
    assertFalse(functionModel.isValid());
    System.out.println(functionModel.prettyPrint(false));

    List<InvalidGraphException> exceptions = functionModel.getRoot().getExceptions();
    assertEquals(2, exceptions.size());

    InvalidGraphException ex0 = exceptions.get(0);
    assertTrue(ex0 instanceof NoSuitableConstructorException);
    List<Parameter> path0 = ex0.getPath();
    assertEquals(2, path0.size());
    assertEquals(ChildFn.class, path0.get(0).getType());
    assertEquals("childFn", path0.get(0).getName());
    assertEquals(NoSuitableConstructor.class, path0.get(1).getType());
    assertEquals("noSuitableConstructor", path0.get(1).getName());

    InvalidGraphException ex1 = exceptions.get(1);
    assertTrue(ex1 instanceof NoSuitableConstructorException);
    List<Parameter> path1 = ex1.getPath();
    assertEquals(1, path1.size());
    assertEquals("noPublicConstructor", path1.get(0).getName());
    assertEquals(NoPublicConstructor.class, path1.get(0).getType());
  }

  public static class NoPublicConstructor {

    private NoPublicConstructor() {
    }

    @Output("Foo")
    public Object foo() {
      return null;
    }
  }

  public static class NoSuitableConstructor {

    public NoSuitableConstructor() {
    }

    public NoSuitableConstructor(String ignored) {
    }
  }

  public static class RootFn {
    public RootFn(ChildFn childFn, NoPublicConstructor noPublicConstructor) {
    }
    @Output("Foo")
    public Object foo() {
      return null;
    }
  }

  public static class ChildFn {
    public ChildFn(NoSuitableConstructor noSuitableConstructor) {
    }
  }

}
