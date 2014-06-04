/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.graph.convert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.joda.convert.StringConverter;

import com.opengamma.sesame.function.ParameterUtils;
import com.opengamma.util.ArgumentChecker;

@SuppressWarnings("unchecked")
/* package */ abstract class CollectionConverter implements StringConverter<Object> {

  private final StringConverter<Object> _elementConverter;

  /* package */ CollectionConverter(StringConverter<Object> elementConverter) {
    _elementConverter = ArgumentChecker.notNull(elementConverter, "elementConverter");
  }

  @Override
  public Collection<?> convertFromString(Class<?> type, String value) {
    Collection<String> stringValues = StringParser.parse(value);
    Collection<Object> convertedValues = createEmptyCollection(stringValues.size());

    for (String stringValue : stringValues) {
      convertedValues.add(_elementConverter.convertFromString(type, stringValue));
    }
    return convertedValues;
  }

  protected abstract Collection<Object> createEmptyCollection(int size);

  @Override
  public String convertToString(Object value) {
    ArgumentChecker.notNull(value, "value");

    if (!(value instanceof Collection<?>)) {
      throw new IllegalArgumentException("value must be a collection. " + value);
    }
    List<Object> list = (List<Object>) value;
    List<String> convertedElements = new ArrayList<>(list.size());

    for (Object item : list) {
      convertedElements.add(ParameterUtils.escapeString(_elementConverter.convertToString(item)));
    }
    return StringUtils.join(convertedElements, " ");
  }

  public static StringConverter<Object> create(Class<?> collectionType, StringConverter<Object> elementConverter) {
    if (collectionType == List.class) {
      return new ListConverter(elementConverter);
    }
    if (collectionType == Set.class) {
      return new SetConverter(elementConverter);
    }
    throw new IllegalArgumentException("Unexpected collection type " + collectionType);
  }

  private static class SetConverter extends CollectionConverter {

    /* package */ SetConverter(StringConverter<Object> elementConverter) {
      super(elementConverter);
    }

    @Override
    protected Collection<Object> createEmptyCollection(int size) {
      return new HashSet<>(size);
    }
  }

  private static class ListConverter extends CollectionConverter {

    /* package */ ListConverter(StringConverter<Object> elementConverter) {
      super(elementConverter);
    }

    @Override
    protected Collection<Object> createEmptyCollection(int size) {
      return new ArrayList<>(size);
    }
  }
}
