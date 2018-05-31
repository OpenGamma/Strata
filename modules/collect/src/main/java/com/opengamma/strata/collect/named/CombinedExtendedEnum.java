/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.named;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.logging.Logger;

import org.joda.convert.RenameHandler;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.io.IniFile;
import com.opengamma.strata.collect.io.PropertySet;
import com.opengamma.strata.collect.io.ResourceConfig;

/**
 * Combines multiple extended enums into one lookup.
 * <p>
 * Each {@link ExtendedEnum} is kept separate to ensure fast lookup of the common case.
 * This class uses a configuration file to determine the extended enums to combine.
 * <p>
 * It is intended that this class is used as a helper class to load the configuration
 * and manage the map of names to instances. It should be created and used by the author
 * of the main abstract extended enum class, and not be application developers.
 * 
 * @param <T>  the type of the enum
 */
public final class CombinedExtendedEnum<T extends Named> {

  /**
   * The logger.
   */
  private static final Logger log = Logger.getLogger(CombinedExtendedEnum.class.getName());
  /**
   * Section name used for types.
   */
  private static final String TYPES_SECTION = "types";

  /**
   * The combined enum type.
   */
  private final Class<T> type;
  /**
   * The underlying extended enums.
   */
  private final ImmutableList<ExtendedEnum<? extends T>> children;

  //-------------------------------------------------------------------------
  /**
   * Obtains a combined extended enum instance.
   * <p>
   * Calling this method loads configuration files to determine which extended enums to combine.
   * The configuration file has the same simple name as the specified type and is a
   * {@linkplain IniFile INI file} with the suffix '.ini'.
   * 
   * @param <R>  the type of the enum
   * @param type  the type to load
   * @return the extended enum
   */
  public static <R extends Named> CombinedExtendedEnum<R> of(Class<R> type) {
    try {
      // load all matching files
      String name = type.getSimpleName() + ".ini";
      IniFile config = ResourceConfig.combinedIniFile(name);
      // parse files
      ImmutableList<ExtendedEnum<? extends R>> children = parseChildren(config, type);
      log.fine(() -> "Loaded combined extended enum: " + name + ", providers: " + children);
      return new CombinedExtendedEnum<>(type, children);

    } catch (RuntimeException ex) {
      // logging used because this is loaded in a static variable
      log.severe("Failed to load CombinedExtendedEnum for " + type + ": " + Throwables.getStackTraceAsString(ex));
      // return an empty instance to avoid ExceptionInInitializerError
      return new CombinedExtendedEnum<>(type, ImmutableList.of());
    }
  }

  // parses the alternate names
  @SuppressWarnings("unchecked")
  private static <R extends Named> ImmutableList<ExtendedEnum<? extends R>> parseChildren(
      IniFile config,
      Class<R> enumType) {

    if (!config.contains(TYPES_SECTION)) {
      return ImmutableList.of();
    }
    PropertySet section = config.section(TYPES_SECTION);
    ImmutableList.Builder<ExtendedEnum<? extends R>> builder = ImmutableList.builder();
    for (String key : section.keys()) {
      Class<?> cls;
      try {
        cls = RenameHandler.INSTANCE.lookupType(key);
      } catch (Exception ex) {
        throw new IllegalArgumentException("Unable to find extended enum class: " + key, ex);
      }
      Method method;
      try {
        method = cls.getMethod("extendedEnum");
      } catch (Exception ex) {
        throw new IllegalArgumentException("Unable to find extendedEnum() method on class: " + cls.getName(), ex);
      }
      if (!method.getReturnType().equals(ExtendedEnum.class)) {
        throw new IllegalArgumentException("Method extendedEnum() does not return ExtendedEnum on class: " + cls.getName());
      }
      ExtendedEnum<?> result;
      try {
        result = ExtendedEnum.class.cast(method.invoke(null));
      } catch (Exception ex) {
        throw new IllegalArgumentException("Unable to call extendedEnum() method on class: " + cls.getName(), ex);
      }
      if (!enumType.isAssignableFrom(result.getType())) {
        throw new IllegalArgumentException(
            "Method extendedEnum() returned an ExtendedEnum with an incompatible type on class: " + cls.getName());
      }
      builder.add((ExtendedEnum<? extends R>) result);
    }
    return builder.build();
  }

  //-------------------------------------------------------------------------
  /**
   * Creates an instance.
   * 
   * @param type  the enum type
   * @param children  the child extended enums to delegate to
   */
  private CombinedExtendedEnum(
      Class<T> type,
      ImmutableList<ExtendedEnum<? extends T>> children) {

    this.type = ArgChecker.notNull(type, "type");
    this.children = ArgChecker.notNull(children, "children");
  }

  //-------------------------------------------------------------------------
  /**
   * Finds an instance by name.
   * <p>
   * This finds the instance matching the specified name.
   * Instances may have alternate names (aliases), thus the returned instance
   * may have a name other than that requested.
   * 
   * @param name  the enum name to return
   * @return the named enum
   */
  public Optional<T> find(String name) {
    for (ExtendedEnum<? extends T> child : children) {
      @SuppressWarnings("unchecked")
      Optional<T> found = (Optional<T>) child.find(name);
      if (found.isPresent()) {
        return found;
      }
    }
    return Optional.empty();
  }

  /**
   * Looks up an instance by name.
   * <p>
   * This finds the instance matching the specified name.
   * Instances may have alternate names (aliases), thus the returned instance
   * may have a name other than that requested.
   * 
   * @param name  the enum name to return
   * @return the named enum
   * @throws IllegalArgumentException if the name is not found
   */
  public T lookup(String name) {
    return find(name).orElseThrow(() -> new IllegalArgumentException(type.getSimpleName() + " name not found: " + name));
  }

  //-------------------------------------------------------------------------
  @Override
  public String toString() {
    return "CombinedExtendedEnum[" + type.getSimpleName() + "]";
  }

}
