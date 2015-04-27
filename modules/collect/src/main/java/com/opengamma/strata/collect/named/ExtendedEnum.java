/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.named;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.joda.convert.RenameHandler;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.io.IniFile;
import com.opengamma.strata.collect.io.PropertiesFile;
import com.opengamma.strata.collect.io.PropertySet;
import com.opengamma.strata.collect.io.ResourceLocator;

/**
 * Manager for extended enums controlled by code or configuration.
 * <p>
 * The standard Java {@code Enum} is a fixed set of constants defined at compile time.
 * In many scenarios this can be too limiting and this class provides an alternative.
 * <p>
 * A configuration file is used to define the set of named instances via provider classes.
 * A provider class is either an implementation of {@link NamedLookup} or a class
 * providing {@code public static final} enum constants.
 * <p>
 * The configuration file also supports the notion of alternate names (aliases).
 * This allows many different names to be used to lookup the same instance.
 * <p>
 * The configuration file is found in the classpath. It has the same package location as the enum type
 * and is a chained {@linkplain IniFile#ofChained(java.util.stream.Stream) INI file}.
 * <p>
 * A chained INI file allows multiple files to be on the classpath.
 * A 'chain' section includes a 'priority' value to specify the order to load the files.
 * The 'chainNextFile' and 'chainRemoveSections' keys provide fine grained control.
 * <p>
 * Two sections control the loading of extended enum providers - 'providers' and 'alternates'.
 * <p>
 * The 'providers' section contains a number of properties, one for each provider.
 * The key is the full class name of the provider.
 * The value is either 'constants' or 'lookup'.
 * A 'constants' provider defines the extended enums are public static constants.
 * A 'lookup' provider implemented {@link NamedLookup}.
 * <p>
 * The 'alternates' section contains a number of properties, one for each alternate name.
 * The key is the alternate name, the value is the standard name.
 * Alternate names are used when looking up an extended enum.
 * <p>
 * It is intended that this class is used as a helper class to load the configuration
 * and manage the map of names to instances. It should be created and used by the author
 * of the main abstract extended enum class, and not be application developers.
 * 
 * @param <T>  the type of the enum
 */
public final class ExtendedEnum<T extends Named> {

  /**
   * Section name used for providers.
   */
  private static final String PROVIDERS_SECTION = "providers";
  /**
   * Section name used for alternates.
   */
  private static final String ALTERNATES_SECTION = "alternates";

  /**
   * The enum type.
   */
  private final Class<T> type;
  /**
   * The lookup functions.
   */
  private final ImmutableList<NamedLookup<T>> lookups;
  /**
   * The map of alternate names.
   */
  private final ImmutableMap<String, String> alternates;

  //-------------------------------------------------------------------------
  /**
   * Obtains an extended enum instance.
   * <p>
   * Calling this method loads configuration files to determine the extended enum values.
   * The configuration file has the same location as the specified type and is a
   * {@linkplain PropertiesFile properties file} with the suffix '.properties'.
   * See class-level documentation for more information.
   * 
   * @param <R>  the type of the enum
   * @param type  the type to load
   * @return the extended enum
   */
  public static <R extends Named> ExtendedEnum<R> of(Class<R> type) {
    ArgChecker.notNull(type, "type");
    try {
      // load all matching files
      String name = type.getName().replace('.', '/') + ".ini";
      IniFile config = IniFile.ofChained(
          ResourceLocator.streamOfClasspathResources(name).map(ResourceLocator::getCharSource));
      // parse files
      ImmutableList<NamedLookup<R>> lookups = parseProviders(config, type);
      ImmutableMap<String, String> alternateNames = parseAlternates(config);
      return new ExtendedEnum<>(type, lookups, alternateNames);

    } catch (RuntimeException ex) {
      // logging used because this is loaded in a static variable
      Logger logger = Logger.getLogger(ExtendedEnum.class.getName());
      logger.severe("Failed to load ExtendedEnum for " + type + ": " + Throwables.getStackTraceAsString(ex));
      // return an empty instance to avoid ExceptionInInitializerError
      return new ExtendedEnum<>(type, ImmutableList.of(), ImmutableMap.of());
    }
  }

  // parses the alternate names
  @SuppressWarnings("unchecked")
  private static <R extends Named> ImmutableList<NamedLookup<R>> parseProviders(
      IniFile config,
      Class<R> enumType) {

    if (!config.contains(PROVIDERS_SECTION)) {
      return ImmutableList.of();
    }
    PropertySet section = config.getSection(PROVIDERS_SECTION);
    ImmutableList.Builder<NamedLookup<R>> builder = ImmutableList.builder();
    for (String key : section.keys()) {
      Class<?> cls;
      try {
        cls = RenameHandler.INSTANCE.lookupType(key);
      } catch (Exception ex) {
        throw new IllegalArgumentException("Unable to find enum provider class: " + key, ex);
      }
      String value = section.getValue(key);
      if (value.equals("constants")) {
        // extract public static final constants
        builder.add(parseConstants(enumType, cls));

      } else if (value.equals("lookup")) {
        // class is a named lookup
        if (!NamedLookup.class.isAssignableFrom(cls)) {
          throw new IllegalArgumentException("Enum provider class must implement NamedLookup " + cls.getName());
        }
        try {
          Constructor<?> cons = cls.getDeclaredConstructor();
          if (!Modifier.isPublic(cls.getModifiers())) {
            cons.setAccessible(true);
          }
          builder.add((NamedLookup<R>) cons.newInstance());
        } catch (Exception ex) {
          throw new IllegalArgumentException("Invalid enum provider constructor: new " + cls.getName() + "()", ex);
        }

      } else if (value.equals("instance")) {
        // class has a named lookup INSTANCE static field
        try {
          Field field = cls.getDeclaredField("INSTANCE");
          if (!Modifier.isStatic(field.getModifiers()) || !NamedLookup.class.isAssignableFrom(field.getType())) {
            throw new IllegalArgumentException("Invalid enum provider instance: " + cls.getName() + ".INSTANCE");
          }
          if (!Modifier.isPublic(cls.getModifiers()) || !Modifier.isPublic(field.getModifiers())) {
            field.setAccessible(true);
          }
          builder.add((NamedLookup<R>) field.get(null));
        } catch (Exception ex) {
          throw new IllegalArgumentException("Invalid enum provider instance: " + cls.getName() + ".INSTANCE", ex);
        }

      } else {
        throw new IllegalArgumentException("Provider value must be either 'constants' or 'lookup'");
      }
    }
    return builder.build();
  }

  // parses the public static final constants
  private static <R extends Named> NamedLookup<R> parseConstants(Class<R> enumType, Class<?> constantsType) {
    Field[] fields = constantsType.getDeclaredFields();
    Map<String, R> instances = new HashMap<>();
    for (Field field : fields) {
      if (Modifier.isPublic(field.getModifiers()) && Modifier.isStatic(field.getModifiers()) &&
          Modifier.isFinal(field.getModifiers()) && enumType.isAssignableFrom(field.getType())) {
        if (Modifier.isPublic(constantsType.getModifiers()) == false) {
          field.setAccessible(true);
        }
        try {
          R instance = enumType.cast(field.get(null));
          instances.putIfAbsent(instance.getName(), instance);
        } catch (Exception ex) {
          throw new IllegalArgumentException("Unable to query field: " + field, ex);
        }
      }
    }
    ImmutableMap<String, R> constants = ImmutableMap.copyOf(instances);
    return new NamedLookup<R>() {
      @Override
      public ImmutableMap<String, R> lookupAll() {
        return constants;
      }
    };
  }

  // parses the alternate names.
  private static ImmutableMap<String, String> parseAlternates(IniFile config) {
    if (!config.contains(ALTERNATES_SECTION)) {
      return ImmutableMap.of();
    }
    ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
    PropertySet section = config.getSection(ALTERNATES_SECTION);
    for (String key : section.keys()) {
      builder.put(key, section.getValue(key));
    }
    return builder.build();
  }

  //-------------------------------------------------------------------------
  /**
   * Creates an instance.
   * 
   * @param type  the enum type
   * @param lookups  the lookup functions to find instances
   * @param alternates  the map of alternate name to standard name
   */
  private ExtendedEnum(Class<T> type, ImmutableList<NamedLookup<T>> lookups, ImmutableMap<String, String> alternates) {
    ArgChecker.notNull(type, "type");
    ArgChecker.notNull(alternates, "alternates");
    ArgChecker.notNull(lookups, "lookups");
    this.type = type;
    this.lookups = lookups;
    this.alternates = alternates;
  }

  //-------------------------------------------------------------------------
  /**
   * Looks up an instance by name.
   * <p>
   * This finds the instance matching the specified name.
   * Instances may have alternate names (aliases), thus the returned instance
   * may have a name other than that requested.
   * 
   * @param name  the enum name to return
   * @return the named enum
   */
  public T lookup(String name) {
    ArgChecker.notNull(name, "name");
    String standardName = alternates.getOrDefault(name, name);
    for (NamedLookup<T> lookup : lookups) {
      T instance = lookup.lookup(standardName);
      if (instance != null) {
        return instance;
      }
    }
    throw new IllegalArgumentException(type.getSimpleName() + " name not found: " + name);
  }

  /**
   * Looks up an instance by name and type.
   * <p>
   * This finds the instance matching the specified name, ensuring it is of the specified type.
   * Instances may have alternate names (aliases), thus the returned instance
   * may have a name other than that requested.
   * 
   * @param <S>  the enum subtype
   * @param subtype  the enum subtype to match
   * @param name  the enum name to return
   * @return the named enum
   */
  public <S extends T> S lookup(String name, Class<S> subtype) {
    T result = lookup(name);
    if (!subtype.isInstance(result)) {
      throw new IllegalArgumentException(type.getSimpleName() + " name found but did not match expected type: " + name);
    }
    return subtype.cast(result);
  }

  //-------------------------------------------------------------------------
  /**
   * Returns the map of known instances by name.
   * <p>
   * This method returns all known instances.
   * It is permitted for an enum provider implementation to return an empty map,
   * thus the map may not be complete.
   * The map may include instances keyed under an alternate name, however it
   * will not include the base set of {@linkplain #alternateNames() alternate names}.
   * 
   * @return the map of enum instance by name
   */
  public ImmutableMap<String, T> lookupAll() {
    Map<String, T> map = new HashMap<>();
    for (NamedLookup<T> lookup : lookups) {
      Map<String, T> lookupMap = lookup.lookupAll();
      for (Entry<String, T> entry : lookupMap.entrySet()) {
        map.putIfAbsent(entry.getKey(), entry.getValue());
      }
    }
    return ImmutableMap.copyOf(map);
  }

  /**
   * Returns the complete map of alternate name to standard name.
   * <p>
   * The map is keyed by the alternate name.
   * 
   * @return the map of alternate names
   */
  public ImmutableMap<String, String> alternateNames() {
    return alternates;
  }

  //-------------------------------------------------------------------------
  @Override
  public String toString() {
    return "ExtendedEnum[" + type.getSimpleName() + "]";
  }

}
