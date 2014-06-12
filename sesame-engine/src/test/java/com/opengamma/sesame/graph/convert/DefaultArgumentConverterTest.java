/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.graph.convert;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.reflect.TypeToken;
import com.opengamma.id.ExternalId;
import com.opengamma.sesame.function.FunctionMetadata;
import com.opengamma.sesame.function.ParameterType;
import com.opengamma.util.test.TestGroup;

@SuppressWarnings("unchecked")
@Test(groups = TestGroup.UNIT)
public class DefaultArgumentConverterTest {

  private ArgumentConverter _converter = new DefaultArgumentConverter();

  private void checkConversion(ParameterType parameterType, Object value, String sourceString, String expectedString) {
    assertTrue(_converter.isConvertible(parameterType));
    assertEquals(expectedString, _converter.convertToString(parameterType, value));
    assertEquals(value, _converter.convertFromString(parameterType, sourceString));
  }

  @Test
  public void convertString() {
    checkConversion(ParameterType.ofType(String.class), "foo", "foo", "foo");
  }

  @Test
  public void unknownType() {
    assertFalse(_converter.isConvertible(ParameterType.ofType(FunctionMetadata.class)));
  }

  @Test
  public void convertBoxedDouble() {
    ParameterType parameterType = ParameterType.ofType(Double.class);
    checkConversion(parameterType, 1.2, "1.2", "1.2");
    checkConversion(parameterType, 1.2, "1.200", "1.2");
  }

  @Test
  public void convertPrimitiveDouble() {
    ParameterType parameterType = ParameterType.ofType(Double.TYPE);
    checkConversion(parameterType, 1.2, "1.2", "1.2");
    checkConversion(parameterType, 1.2, "1.200", "1.2");
  }

  @Test
  public void convertListOfStrings() {
    TypeToken<List<String>> typeToken = new TypeToken<List<String>>() { };
    ParameterType parameterType = ParameterType.ofType(typeToken.getType());

    checkConversion(parameterType, ImmutableList.of("abc", "def"), "abc def", "abc def");
    checkConversion(parameterType, ImmutableList.of("abc", "def"), "abc, def", "abc def");
    checkConversion(parameterType, ImmutableList.of("abc", "def"), "\"abc\", def", "abc def");
    checkConversion(parameterType, ImmutableList.of("ab\"c", "def"), "\"ab\\\"c\", def", "ab\\\"c def");
  }

  @Test
  public void convertListOfDoubles() {
    TypeToken<List<Double>> typeToken = new TypeToken<List<Double>>() { };
    ParameterType parameterType = ParameterType.ofType(typeToken.getType());

    checkConversion(parameterType, ImmutableList.of(1.1, 1.2), "1.1, 1.2", "1.1 1.2");
    checkConversion(parameterType, ImmutableList.of(1.1, 1.2), "1.1 1.2", "1.1 1.2");
    checkConversion(parameterType, ImmutableList.of(1.1, 1.2), "1.10, 1.20", "1.1 1.2");
    checkConversion(parameterType, ImmutableList.of(1.1, 1.2), "\"1.1\", \"1.2\"", "1.1 1.2");
  }

  @Test
  public void cannotConvertRawList() {
    TypeToken<List> typeToken = new TypeToken<List>() { };
    ParameterType parameterType = ParameterType.ofType(typeToken.getType());
    assertFalse(_converter.isConvertible(parameterType));
  }

  @Test
  public void cannotConvertListOfUnknownType() {
    TypeToken<List<FunctionMetadata>> typeToken = new TypeToken<List<FunctionMetadata>>() { };
    ParameterType parameterType = ParameterType.ofType(typeToken.getType());
    assertFalse(_converter.isConvertible(parameterType));
  }

  @Test
  public void convertArrayOfStrings() {
    ParameterType parameterType = ParameterType.ofType(String[].class);
    String[] array = {"foo", "bar"};

    assertTrue(_converter.isConvertible(parameterType));
    assertEquals("foo bar", _converter.convertToString(parameterType, array));
    assertTrue(Arrays.equals(array, (Object[]) _converter.convertFromString(parameterType, "foo bar")));
  }

  @Test
  public void convertArrayOfBoxedDoubles() {
    ParameterType parameterType = ParameterType.ofType(Double[].class);
    Double[] array = {1.1, 1.2};

    assertTrue(_converter.isConvertible(parameterType));
    assertEquals("1.1 1.2", _converter.convertToString(parameterType, array));
    assertTrue(Arrays.equals(array, (Object[]) _converter.convertFromString(parameterType, "1.1 1.2")));
    assertTrue(Arrays.equals(array, (Object[]) _converter.convertFromString(parameterType, "1.1, 1.2")));
    assertTrue(Arrays.equals(array, (Object[]) _converter.convertFromString(parameterType, "\"1.1\", 1.20")));
  }

  @Test
  public void convertArrayOfPrimitiveDoubles() {
    ParameterType parameterType = ParameterType.ofType(double[].class);
    double[] array = {1.1, 1.2};

    assertTrue(_converter.isConvertible(parameterType));
    assertEquals("1.1 1.2", _converter.convertToString(parameterType, array));
    assertTrue(Arrays.equals(array, (double[]) _converter.convertFromString(parameterType, "1.1 1.2")));
    assertTrue(Arrays.equals(array, (double[]) _converter.convertFromString(parameterType, "1.1, 1.2")));
    assertTrue(Arrays.equals(array, (double[]) _converter.convertFromString(parameterType, "\"1.1\", 1.20")));
  }

  @Test
  public void convertEmptyList() {
    TypeToken<List<String>> typeToken = new TypeToken<List<String>>() { };
    ParameterType parameterType = ParameterType.ofType(typeToken.getType());
    checkConversion(parameterType, Collections.emptyList(), "", "");
  }

  @Test
  public void convertEmptyArray() {
    ParameterType parameterType = ParameterType.ofType(double[].class);
    double[] array = {};

    assertTrue(_converter.isConvertible(parameterType));
    assertEquals("", _converter.convertToString(parameterType, array));
    assertTrue(Arrays.equals(array, (double[]) _converter.convertFromString(parameterType, "")));
  }

  @Test
  public void convertMapOfStringToDouble() {
    TypeToken<Map<String, Double>> typeToken = new TypeToken<Map<String, Double>>() { };
    ParameterType parameterType = ParameterType.ofType(typeToken.getType());

    Map<String, Double> map = ImmutableMap.of("simpleKey", 1d, "key with spaces", 2d, "key with \"quotes\"", 3d);
    String str = "simpleKey 1.0 \"key with spaces\" 2.0 \"key with \\\"quotes\\\"\" 3.0";
    checkConversion(parameterType, map, str, str);
  }

  @Test
  public void convertMapOfExternalIdToDouble() {
    TypeToken<Map<ExternalId, Double>> typeToken = new TypeToken<Map<ExternalId, Double>>() { };
    ParameterType parameterType = ParameterType.ofType(typeToken.getType());

    Map<ExternalId, Double> map = ImmutableMap.of(ExternalId.of("a", "1"), 1d, ExternalId.of("b", "2"), 2d);
    String str = "a~1 1.0 b~2 2.0";
    checkConversion(parameterType, map, str, str);
  }

  @Test
  public void convertEmptyMap() {
    TypeToken<Map<String, Double>> typeToken = new TypeToken<Map<String, Double>>() { };
    ParameterType parameterType = ParameterType.ofType(typeToken.getType());
    checkConversion(parameterType, Collections.emptyMap(), "", "");
  }
}
