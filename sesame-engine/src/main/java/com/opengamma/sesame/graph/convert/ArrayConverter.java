/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.graph.convert;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.joda.convert.StringConverter;

import com.opengamma.util.ArgumentChecker;

@SuppressWarnings("unchecked")
/* package */ class ArrayConverter implements StringConverter {

  private final StringConverter _elementConverter;
  private final Class _elementType;

  /* package */ ArrayConverter(Class elementType, StringConverter elementConverter) {
    _elementType = ArgumentChecker.notNull(elementType, "elementType");
    _elementConverter = ArgumentChecker.notNull(elementConverter, "elementConverter");
  }

  @Override
  public Object convertFromString(Class type, String value) {
    List<String> stringValues = StringParser.parse(value);
    Object array = Array.newInstance(_elementType, stringValues.size());
    int index = 0;

    for (String stringValue : stringValues) {
      Array.set(array, index++, _elementConverter.convertFromString(_elementType, stringValue));
    }
    return array;
  }

  @Override
  public String convertToString(Object value) {
    int length = Array.getLength(value);
    List<String> stringValues = new ArrayList<>(length);

    for (int i = 0; i < length; i++) {
      stringValues.add(_elementConverter.convertToString(Array.get(value, i)));
    }
    return StringUtils.join(stringValues, " ");
  }
}
