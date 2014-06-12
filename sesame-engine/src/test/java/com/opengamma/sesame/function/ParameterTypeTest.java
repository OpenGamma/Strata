/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.function;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.poi.ss.formula.functions.T;
import org.testng.annotations.Test;

import com.google.common.reflect.TypeToken;
import com.opengamma.util.test.TestGroup;

@Test(groups = TestGroup.UNIT)
public class ParameterTypeTest {

  @Test
  public void simpleTypeNameNonGenericType() {
    ParameterType parameterType = ParameterType.ofType(String.class);
    assertEquals("String", parameterType.getName());
  }

  @Test
  public void simpleTypeNameNonGenericArray() {
    ParameterType parameterType = ParameterType.ofType(double[].class);
    assertEquals("double[]", parameterType.getName());
  }

  @Test
  public void simpleTypeNameParameterizedType() {
    TypeToken<List<String>> typeToken = new TypeToken<List<String>>() { };
    ParameterType parameterType = ParameterType.ofType(typeToken.getType());
    assertEquals("List<String>", parameterType.getName());
  }

  @Test
  public void simpleTypeNameRawType() {
    TypeToken<List> typeToken = new TypeToken<List>() { };
    ParameterType parameterType = ParameterType.ofType(typeToken.getType());
    assertEquals("List", parameterType.getName());
  }

  @Test
  public void simpleTypeNameNestedRawType() {
    TypeToken<List<List>> typeToken = new TypeToken<List<List>>() { };
    ParameterType parameterType = ParameterType.ofType(typeToken.getType());
    assertEquals("List<List>", parameterType.getName());
  }

  @Test
  public void simpleTypeNameParameterizedType2() {
    TypeToken<List<T>> typeToken = new TypeToken<List<T>>() { };
    ParameterType parameterType = ParameterType.ofType(typeToken.getType());
    assertEquals("List<T>", parameterType.getName());
  }

  @Test
  public void simpleTypeNameNestedParameterizedType() {
    TypeToken<List<Set<String>>> typeToken = new TypeToken<List<Set<String>>>() { };
    ParameterType parameterType = ParameterType.ofType(typeToken.getType());
    assertEquals("List<Set>", parameterType.getName());
  }

  @Test
  public void simpleTypeNameParameterizedMap() {
    TypeToken<Map<String, Integer>> typeToken = new TypeToken<Map<String, Integer>>() { };
    ParameterType parameterType = ParameterType.ofType(typeToken.getType());
    assertEquals("Map<String, Integer>", parameterType.getName());
  }

  @Test
  public void simpleTypeNameNestedParameterizedMap() {
    TypeToken<Map<Set<String>, Collection<Integer>>> typeToken = new TypeToken<Map<Set<String>, Collection<Integer>>>() { };
    ParameterType parameterType = ParameterType.ofType(typeToken.getType());
    assertEquals("Map<Set, Collection>", parameterType.getName());
  }

  @Test
  public void simpleTypeNameGenericArrayType() {
    TypeToken<List<String>[]> typeToken = new TypeToken<List<String>[]>() { };
    ParameterType parameterType = ParameterType.ofType(typeToken.getType());
    assertEquals("List[]", parameterType.getName());
  }

  @Test
  public void collection() {
    TypeToken<Collection> rawCollectionToken = new TypeToken<Collection>() { };
    ParameterType rawCollectionType = ParameterType.ofType(rawCollectionToken.getType());
    assertEquals(Collection.class, rawCollectionType.getType());
    assertTrue(rawCollectionType instanceof SimpleType);

    TypeToken<Collection<String>> stringCollectionToken = new TypeToken<Collection<String>>() { };
    ParameterType stringCollectionType = ParameterType.ofType(stringCollectionToken.getType());
    assertEquals(Collection.class, stringCollectionType.getType());
    assertTrue(stringCollectionType instanceof CollectionType);
    assertEquals(String.class, ((CollectionType) stringCollectionType).getElementType());

    TypeToken<Collection<List<String>>> nestedCollectionToken = new TypeToken<Collection<List<String>>>() { };
    ParameterType nestedCollectionType = ParameterType.ofType(nestedCollectionToken.getType());
    assertEquals(Collection.class, nestedCollectionType.getType());
    assertTrue(nestedCollectionType instanceof CollectionType);
    assertEquals(List.class, ((CollectionType) nestedCollectionType).getElementType());
  }

  @Test
  public void array() {
    ParameterType stringArrayType = ParameterType.ofType(String[].class);
    assertTrue(stringArrayType instanceof ArrayType);
    assertEquals(String.class, ((ArrayType) stringArrayType).getElementType());

    TypeToken<List<String>[]> listStringArrayToken = new TypeToken<List<String>[]>() { };
    ParameterType listStringArrayType = ParameterType.ofType(listStringArrayToken.getType());
    assertTrue(listStringArrayType instanceof ArrayType);
    assertEquals(List.class, ((ArrayType) listStringArrayType).getElementType());
  }

  @Test
  public void map() {
    TypeToken<Map<String, Integer>> simpleMapToken = new TypeToken<Map<String, Integer>>() { };
    ParameterType simpleMapType = ParameterType.ofType(simpleMapToken.getType());
    assertTrue(simpleMapType instanceof MapType);
    assertEquals(String.class, ((MapType) simpleMapType).getKeyType());
    assertEquals(Integer.class, ((MapType) simpleMapType).getValueType());

    TypeToken<Map<List<String>, Set<Integer>>> nestedMapToken = new TypeToken<Map<List<String>, Set<Integer>>>() { };
    ParameterType nestedMapType = ParameterType.ofType(nestedMapToken.getType());
    assertTrue(nestedMapType instanceof MapType);
    assertEquals(List.class, ((MapType) nestedMapType).getKeyType());
    assertEquals(Set.class, ((MapType) nestedMapType).getValueType());
  }
}
