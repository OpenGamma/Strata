/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.named;

import java.util.EnumMap;
import java.util.Locale;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import com.google.common.base.CaseFormat;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.Messages;

/**
 * Helper that allows enum names to be created and parsed.
 * 
 * @param <T>  the type of the enum
 */
public final class EnumNames<T extends Enum<T> & NamedEnum> {

  /**
   * Parsing map.
   */
  private final ImmutableSortedMap<String, T> parseMap;
  /**
   * Formatted forms.
   */
  private final ImmutableSortedSet<String> formattedSet;
  /**
   * Format map (mutable, but treated as immutable).
   */
  private final EnumMap<T, String> formatMap;
  /**
   * Class of the enum.
   */
  private final Class<T> enumType;

  /**
   * Creates an instance deriving the formatted string from the enum constant name.
   * 
   * @param <T>  the type of the enum
   * @param enumType  the type of the enum
   * @return the names instance
   */
  public static <T extends Enum<T> & NamedEnum> EnumNames<T> of(Class<T> enumType) {
    return new EnumNames<>(enumType, false);
  }

  /**
   * Creates an instance where the {@code toString} method is written manually.
   * <p>
   * The {@code toString} method is called to extract the correct formatted string.
   * 
   * @param <T>  the type of the enum
   * @param enumType  the type of the enum
   * @return the names instance
   */
  public static <T extends Enum<T> & NamedEnum> EnumNames<T> ofManualToString(Class<T> enumType) {
    return new EnumNames<>(enumType, true);
  }

  // restricted constructor
  private EnumNames(Class<T> enumType, boolean manualToString) {
    this.enumType = ArgChecker.notNull(enumType, "enumType");
    SortedMap<String, T> parseMap = new TreeMap<>();
    SortedSet<String> formattedSet = new TreeSet<>();
    EnumMap<T, String> formatMap = new EnumMap<>(enumType);
    for (T value : enumType.getEnumConstants()) {
      String formatted =
          manualToString ? value.toString() : CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, value.name());
      parseMap.put(value.name(), value);
      parseMap.put(value.name().toUpperCase(Locale.ENGLISH), value);
      parseMap.put(value.name().toLowerCase(Locale.ENGLISH), value);
      parseMap.put(formatted, value);
      parseMap.put(formatted.toUpperCase(Locale.ENGLISH), value);
      parseMap.put(formatted.toLowerCase(Locale.ENGLISH), value);
      formattedSet.add(formatted);
      formatMap.put(value, formatted);
    }
    this.parseMap = ImmutableSortedMap.copyOf(parseMap);
    this.formattedSet = ImmutableSortedSet.copyOf(formattedSet);
    this.formatMap = formatMap;
  }

  private EnumNames(Class<T> enumType, SortedMap<String, T> parseMap, SortedSet<String> formattedSet, EnumMap<T, String> formatMap) {
    this.enumType = enumType;
    this.parseMap = ImmutableSortedMap.copyOf(parseMap);
    this.formattedSet = ImmutableSortedSet.copyOf(formattedSet);
    this.formatMap = formatMap;
  }

  //-------------------------------------------------------------------------
  /**
   * Returns an instance with an additional alias added.
   *
   * @param alias  the string of the alias
   * @param value  the enum value that the alias represents
   * @return the converted name
   */
  public EnumNames<T> withParseAlias(String alias, T value) {
    SortedMap<String, T> parseMap = new TreeMap<>(this.parseMap);
    SortedSet<String> formattedSet = new TreeSet<>(this.formattedSet);
    parseMap.put(alias, value);
    parseMap.put(alias.toUpperCase(Locale.ENGLISH), value);
    parseMap.put(alias.toLowerCase(Locale.ENGLISH), value);
    formattedSet.add(alias);
    return new EnumNames<T>(enumType, parseMap, formattedSet, formatMap);
  }

  /**
   * Creates a standard Strata mixed case name from an enum-style constant.
   *
   * @param value  the enum value to convert
   * @return the converted name
   */
  public String format(T value) {
    // this should never return null
    return formatMap.get(value);
  }

  /**
   * Parses the standard external name for an enum.
   * 
   * @param name  the external name
   * @return the enum value
   */
  public T parse(String name) {
    ArgChecker.notNull(name, "name");
    T value = parseMap.get(name);
    if (value == null) {
      throw new IllegalArgumentException(Messages.format(
          "Unknown enum name '{}' for type {}, valid values are {}", name, enumType.getName(), formattedSet));
    }
    return value;
  }

}
