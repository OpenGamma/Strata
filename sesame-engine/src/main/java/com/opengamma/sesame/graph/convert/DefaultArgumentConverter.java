/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.graph.convert;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.joda.convert.StringConvert;
import org.joda.convert.StringConverter;

import com.opengamma.core.link.ConfigLink;
import com.opengamma.sesame.function.ArrayType;
import com.opengamma.sesame.function.CollectionType;
import com.opengamma.sesame.function.MapType;
import com.opengamma.sesame.function.ParameterType;
import com.opengamma.util.ArgumentChecker;

/**
 * <p>Converter that can convert between strings and objects used in the function configuration.
 * There are three types of converters, each of which handles a different type of object</p>
 *
 * <ul>
 *   <li>Simple types - A string is converted to a single instance. e.g. number, date, tenor</li>
 *   <li>Collections - A delimited string is converted to a collection of simple types</li>
 *   <li>Maps - A delimited string is converted to a set key / value pairs</li>
 * </ul>
 *
 * <p>All elements of a collection must be of the same type. The collection type must be {@link Collection}, {@link List},
 * {@link Set} or an array. A simple converter is used for converting the elements.</p>
 *
 * <p>All map keys must be of the same type and the values must be of the same type. Simple converters are used
 * for converting the keys and values.</p>
 *
 * <p>Strings are delimited with commas, spaces or a mixture of the two. Strings can be quoted with double quotes.
 * Quotes can be embedded in strings if they are escaped using a backslash.</p>
 */
@SuppressWarnings("unchecked")
public class DefaultArgumentConverter implements ArgumentConverter {

  private final StringConvert _stringConvert;

  // TODO do we need another constructor to allow converters to be added or replaced?
  public DefaultArgumentConverter() {
    _stringConvert = StringConvert.create();

    _stringConvert.register(String.class, new StringConverter<String>() {
      @Override
      public String convertFromString(Class<? extends String> cls, String str) {
        return str;
      }

      @Override
      public String convertToString(String object) {
        return object;
      }
    });
    _stringConvert.register(ConfigLink.class, new StringConverter<ConfigLink>() {

      @Override
      public ConfigLink convertFromString(Class<? extends ConfigLink> cls, String str) {
        throw new UnsupportedOperationException("This should not be called for ConfigLink");
      }

      @Override
      public String convertToString(ConfigLink link) {
        // TODO link API needs to include link identifier PLAT-6467
        //return link.get
        throw new UnsupportedOperationException("need changes to the ConfigLink API to implement this");
      }
    });
  }

  @Override
  public boolean isConvertible(ParameterType type) {
    return getConverter(type) != null;
  }

  @Override
  public String convertToString(ParameterType parameterType, Object object) {
    StringConverter<Object> converter = getConverter(parameterType);

    if (converter == null) {
      throw new IllegalArgumentException("Unable to convert parameter type " + parameterType);
    }
    return converter.convertToString(object);
  }

  @Override
  public Object convertFromString(ParameterType parameterType, String str) {
    StringConverter<Object> converter = getConverter(parameterType);

    if (converter == null) {
      throw new IllegalArgumentException("Unable to convert parameter type " + parameterType);
    }
    return converter.convertFromString(parameterType.getType(), str);
  }

  private StringConverter<Object> getConverter(ParameterType type) {
    ArgumentChecker.notNull(type, "type");

    if (type instanceof ArrayType) {
      Class<?> elementType = ((ArrayType) type).getElementType();

      StringConverter elementConverter = getConverterByClass(elementType);

      if (elementConverter == null) {
        return null;
      } else {
        return new ArrayConverter(elementType, elementConverter);
      }
    }

    if (type instanceof CollectionType) {
      Class<?> collectionType = type.getType();
      Class<?> elementType = ((CollectionType) type).getElementType();

      StringConverter elementConverter = getConverterByClass(elementType);

      if (elementConverter == null) {
        return null;
      } else {
        return CollectionConverter.create(collectionType, elementConverter);
      }
    }

    if (type instanceof MapType) {
      MapType mapType = (MapType) type;
      Class<?> keyType = mapType.getKeyType();
      Class<?> valueType = mapType.getValueType();
      StringConverter keyConverter = getConverterByClass(keyType);
      StringConverter valueConverter = getConverterByClass(valueType);

      if (keyConverter == null || valueConverter == null) {
        return null;
      }
      return new MapConverter(keyType, keyConverter, valueType, valueConverter);
    }

    return getConverterByClass(type.getType());
  }

  private StringConverter getConverterByClass(Class<?> type) {
    if (!_stringConvert.isConvertible(type)) {
      return null;
    }
    return _stringConvert.findConverter(type);
  }
}
