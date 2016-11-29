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
import java.util.Optional;
import java.util.logging.Logger;

import org.joda.convert.RenameHandler;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.io.IniFile;
import com.opengamma.strata.collect.io.PropertySet;
import com.opengamma.strata.collect.io.ResourceConfig;

/**
 * Manager for extended enums controlled by code or configuration.
 * <p>
 * The standard Java {@code Enum} is a fixed set of constants defined at compile time.
 * In many scenarios this can be too limiting and this class provides an alternative.
 * <p>
 * An INI configuration file is used to define the set of named instances.
 * For more information on the process of loading the configuration file, see {@link ResourceConfig}.
 * <p>
 * The named instances are loaded via provider classes.
 * A provider class is either an implementation of {@link NamedLookup} or a class
 * providing {@code public static final} enum constants.
 * <p>
 * The configuration file also supports the notion of alternate names (aliases).
 * This allows many different names to be used to lookup the same instance.
 * <p>
 * Three sections control the loading of additional information.
 * <p>
 * The 'providers' section contains a number of properties, one for each provider.
 * The key is the full class name of the provider.
 * The value is 'constants', 'lookup' or 'instance', and is used to obtain a {@link NamedLookup} instance.
 * A 'constants' provider must contain public static constants of the correct type,
 * which will be reflectively located and wrapped in a {@code NamedLookup}.
 * A 'lookup' provider must implement {@link NamedLookup} and have a no-args constructor.
 * An 'instance' provider must have a static variable named "INSTANCE" of type {@link NamedLookup}.
 * <p>
 * The 'alternates' section contains a number of properties, one for each alternate name.
 * The key is the alternate name, the value is the standard name.
 * Alternate names are used when looking up an extended enum.
 * <p>
 * The 'externals' sections contains a number of properties intended to allow external enum names to be mapped.
 * Unlike 'alternates', which are always included, 'externals' are only included when requested.
 * There may be multiple external <i>groups</i> to handle different external providers of data.
 * For example, the mapping used by FpML may differ from that used by Bloomberg.
 * <p>
 * Each 'externals' section has a name of the form 'externals.Foo', where 'Foo' is the name of the group.
 * Each property line in the section is of the same format as the 'alternates' section.
 * It maps the external name to the standard name.
 * <p>
 * It is intended that this class is used as a helper class to load the configuration
 * and manage the map of names to instances. It should be created and used by the author
 * of the main abstract extended enum class, and not be application developers.
 * 
 * @param <T>  the type of the enum
 */
public final class ExtendedEnum<T extends Named> {

  /**
   * The logger.
   */
  private static final Logger log = Logger.getLogger(ExtendedEnum.class.getName());
  /**
   * Section name used for providers.
   */
  private static final String PROVIDERS_SECTION = "providers";
  /**
   * Section name used for alternates.
   */
  private static final String ALTERNATES_SECTION = "alternates";
  /**
   * Section name used for externals.
   */
  private static final String EXTERNALS_SECTION = "externals.";

  /**
   * The enum type.
   */
  private final Class<T> type;
  /**
   * The lookup functions defining the standard names.
   */
  private final ImmutableList<NamedLookup<T>> lookups;
  /**
   * The map of alternate names.
   */
  private final ImmutableMap<String, String> alternateNames;
  /**
   * The map of external names, keyed by the group name.
   * The first map holds groups of external names.
   * The inner map holds the mapping from external name to our name.
   */
  private final ImmutableMap<String, ImmutableMap<String, String>> externalNames;

  //-------------------------------------------------------------------------
  /**
   * Obtains an extended enum instance.
   * <p>
   * Calling this method loads configuration files to determine the extended enum values.
   * The configuration file has the same simple name as the specified type and is a
   * {@linkplain IniFile INI file} with the suffix '.ini'.
   * See class-level documentation for more information.
   * 
   * @param <R>  the type of the enum
   * @param type  the type to load
   * @return the extended enum
   */
  public static <R extends Named> ExtendedEnum<R> of(Class<R> type) {
    try {
      // load all matching files
      String name = type.getSimpleName() + ".ini";
      IniFile config = ResourceConfig.combinedIniFile(name);
      // parse files
      ImmutableList<NamedLookup<R>> lookups = parseProviders(config, type);
      ImmutableMap<String, String> alternateNames = parseAlternates(config);
      ImmutableMap<String, ImmutableMap<String, String>> externalNames = parseExternals(config);
      log.fine(() -> "Loaded extended enum: " + name + ", providers: " + lookups);
      return new ExtendedEnum<>(type, lookups, alternateNames, externalNames);

    } catch (RuntimeException ex) {
      // logging used because this is loaded in a static variable
      log.severe("Failed to load ExtendedEnum for " + type + ": " + Throwables.getStackTraceAsString(ex));
      // return an empty instance to avoid ExceptionInInitializerError
      return new ExtendedEnum<>(type, ImmutableList.of(), ImmutableMap.of(), ImmutableMap.of());
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
    PropertySet section = config.section(PROVIDERS_SECTION);
    ImmutableList.Builder<NamedLookup<R>> builder = ImmutableList.builder();
    for (String key : section.keys()) {
      Class<?> cls;
      try {
        cls = RenameHandler.INSTANCE.lookupType(key);
      } catch (Exception ex) {
        throw new IllegalArgumentException("Unable to find enum provider class: " + key, ex);
      }
      String value = section.value(key);
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
    return config.section(ALTERNATES_SECTION).asMap();
  }

  // parses the external names.
  private static ImmutableMap<String, ImmutableMap<String, String>> parseExternals(IniFile config) {
    ImmutableMap.Builder<String, ImmutableMap<String, String>> builder = ImmutableMap.builder();
    for (String sectionName : config.sections()) {
      if (sectionName.startsWith(EXTERNALS_SECTION)) {
        String group = sectionName.substring(EXTERNALS_SECTION.length());
        builder.put(group, config.section(sectionName).asMap());
      }
    }
    return builder.build();
  }

  //-------------------------------------------------------------------------
  /**
   * Creates an instance.
   * 
   * @param type  the enum type
   * @param lookups  the lookup functions to find instances
   * @param alternateNames  the map of alternate name to standard name
   * @param externalNames  the map of external name groups
   */
  private ExtendedEnum(
      Class<T> type,
      ImmutableList<NamedLookup<T>> lookups,
      ImmutableMap<String, String> alternateNames,
      ImmutableMap<String, ImmutableMap<String, String>> externalNames) {

    this.type = ArgChecker.notNull(type, "type");
    this.lookups = ArgChecker.notNull(lookups, "lookups");
    this.alternateNames = ArgChecker.notNull(alternateNames, "alternateNames");
    this.externalNames = ArgChecker.notNull(externalNames, "externalNames");
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
    String standardName = alternateNames.getOrDefault(name, name);
    for (NamedLookup<T> lookup : lookups) {
      T instance = lookup.lookup(standardName);
      if (instance != null) {
        return Optional.of(instance);
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
    String standardName = alternateNames.getOrDefault(name, name);
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
   * @throws IllegalArgumentException if the name is not found or has the wrong type
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
    return alternateNames;
  }

  //-------------------------------------------------------------------------
  /**
   * Returns the set of groups that have external names defined.
   * <p>
   * External names are used to map names used by external systems to the standard name used here.
   * There can be multiple groups of mappings to external systems,
   * For example, the mapping used by FpML may differ from that used by Bloomberg.
   * 
   * @return the set of groups that have external names
   */
  public ImmutableSet<String> externalNameGroups() {
    return externalNames.keySet();
  }

  /**
   * Returns the mapping of external names to standard names for a group.
   * <p>
   * External names are used to map names used by external systems to the standard name used here.
   * There can be multiple groups of mappings to external systems,
   * For example, the mapping used by FpML may differ from that used by Bloomberg.
   * <p>
   * The result provides mapping between the external name and the standard name.
   * 
   * @param group  the group name to find external names for
   * @return the map of external names for the group
   * @throws IllegalArgumentException if the group is not found
   */
  public ExternalEnumNames<T> externalNames(String group) {
    ImmutableMap<String, String> externals = externalNames.get(group);
    if (externals == null) {
      throw new IllegalArgumentException(type.getSimpleName() + " group not found: " + group);
    }
    return new ExternalEnumNames<T>(this, group, externals);
  }

  //-------------------------------------------------------------------------
  @Override
  public String toString() {
    return "ExtendedEnum[" + type.getSimpleName() + "]";
  }

  //-------------------------------------------------------------------------
  /**
   * Maps names used by external systems to the standard name used here.
   * <p>
   * A frequent problem in parsing external file formats is converting enum values.
   * This class provides a suitable mapping, allowing multiple external names to map to one standard name.
   * <p>
   * A single instance represents the mapping for a single external group.
   * This allows the mapping for different groups to differ.
   * For example, the mapping used by FpML may differ from that used by Bloomberg.
   * <p>
   * Instances of this class are configured via INI files and provided via {@link ExtendedEnum}.
   * 
   * @param <T>  the type of the enum
   */
  public static final class ExternalEnumNames<T extends Named> {

    private ExtendedEnum<T> extendedEnum;
    private String group;
    private ImmutableMap<String, String> externalNames;

    private ExternalEnumNames(ExtendedEnum<T> extendedEnum, String group, ImmutableMap<String, String> externalNames) {
      this.extendedEnum = extendedEnum;
      this.group = group;
      this.externalNames = externalNames;
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
      String standardName = externalNames.getOrDefault(name, name);
      try {
        return extendedEnum.lookup(standardName);
      } catch (IllegalArgumentException ex) {
        throw new IllegalArgumentException(Messages.format(
            "{}:{} unable to find external name: {}", extendedEnum.type.getSimpleName(), group, name));
      }
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
     * @throws IllegalArgumentException if the name is not found or has the wrong type
     */
    public <S extends T> S lookup(String name, Class<S> subtype) {
      T result = lookup(name);
      if (!subtype.isInstance(result)) {
        throw new IllegalArgumentException(Messages.format(
            "{}:{} external name found but did not match expected type: {}", extendedEnum.type.getSimpleName(), group, name));
      }
      return subtype.cast(result);
    }

    /**
     * Returns the complete map of external name to standard name.
     * <p>
     * The map is keyed by the external name.
     * 
     * @return the map of external names
     */
    public ImmutableMap<String, String> externalNames() {
      return externalNames;
    }

    /**
     * Looks up the external name given a standard enum instance.
     * <p>
     * This searches the map of external names and returns the first matching entry
     * that maps to the given standard name.
     * 
     * @param namedEnum  the named enum to find an external name for
     * @return the external name
     * @throws IllegalArgumentException if there is no external name
     */
    public String reverseLookup(T namedEnum) {
      String name = namedEnum.getName();
      for (Entry<String, String> entry : externalNames.entrySet()) {
        if (entry.getValue().equals(name)) {
          return entry.getKey();
        }
      }
      throw new IllegalArgumentException(Messages.format(
          "{}:{} external name not found for standard name: {}", extendedEnum.type.getSimpleName(), group, name));
    }

    //-------------------------------------------------------------------------
    @Override
    public String toString() {
      return "ExternalEnumNames[" + extendedEnum.type.getSimpleName() + ":" + group + "]";
    }
  }

}
