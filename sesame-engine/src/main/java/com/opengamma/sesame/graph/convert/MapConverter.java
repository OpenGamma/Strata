/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.graph.convert;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.joda.convert.StringConverter;

import com.opengamma.sesame.function.ParameterUtils;
import com.opengamma.util.ArgumentChecker;

@SuppressWarnings("unchecked")
class MapConverter implements StringConverter {

  private final StringConverter _keyConverter;
  private final StringConverter _valueConverter;
  private final Class<?> _keyType;
  private final Class<?> _valueType;

  public MapConverter(Class<?> keyType,
                      StringConverter keyConverter,
                      Class<?> valueType,
                      StringConverter valueConverter) {
    _keyType = keyType;
    _valueType = valueType;
    _keyConverter = ArgumentChecker.notNull(keyConverter, "keyConverter");
    _valueConverter = ArgumentChecker.notNull(valueConverter, "valueConverter");
  }

  @Override
  public Map<?, ?> convertFromString(Class type, String value) {
    List<String> strings = StringParser.parse(value);
    Map<Object, Object> map = new HashMap<>();

    if ((strings.size() % 2) != 0) {
      throw new IllegalArgumentException("A map must be specified as 'key value key value' but an odd number of " +
                                             "items were found (" + strings.size() + ")");
    }
    for (int i = 0; i < (strings.size() / 2); i++) {
      String keyStr = strings.get(i * 2);
      String valStr = strings.get((i * 2) + 1);
      Object key = _keyConverter.convertFromString(_keyType, keyStr);
      Object val = _valueConverter.convertFromString(_valueType, valStr);
      map.put(key, val);
    }
    return map;
  }

  @Override
  public String convertToString(Object value) {
    Map<Object, Object> map = (Map<Object, Object>) value;
    List<String> strings = new ArrayList<>(map.size() * 2);

    for (Map.Entry<Object, Object> entry : map.entrySet()) {
      Object key = entry.getKey();
      Object val = entry.getValue();

      String keyStr = _keyConverter.convertToString(key);
      String valStr = _valueConverter.convertToString(val);
      strings.add(ParameterUtils.escapeString(keyStr));
      strings.add(ParameterUtils.escapeString(valStr));
    }
    return StringUtils.join(strings, " ");
  }
}
