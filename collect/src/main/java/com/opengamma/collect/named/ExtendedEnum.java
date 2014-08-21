/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.collect.named;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.joda.convert.RenameHandler;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.opengamma.collect.ArgChecker;
import com.opengamma.collect.io.PropertiesFile;
import com.opengamma.collect.io.ResourceLocator;

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
 * The configuration file is found in the classpath.
 * It has the same location as the enum type and is a {@linkplain PropertiesFile properties file}.
 * Where multiple files are found in the classpath, a 'priority' value specifies which to load first
 * and a 'chain' flag specifies whether to chain to the next lower priority.
 * Provider classes are defined using the 'provider' key, where the value is the full class name of the provider.
 * Alternate names are defined using the 'alternate' key, where the value is 'AlternateName -&gt; StandardName'.
 * <p>
 * It is intended that this class is used as a helper class to load the configuration
 * and manage the map of names to instances. It should be created and used by the author
 * of the main abstract extended enum class, and not be application developers.
 * 
 * @param <T>  the type of the enum
 */
public final class ExtendedEnum<T extends Named> {
  
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
   * @param type  the type to load
   * @return the extended enum
   */
  public static <R extends Named> ExtendedEnum<R> of(Class<R> type) {
    ArgChecker.notNull(type, "type");
    String name = type.getName().replace('.', '/') + ".properties";
    try {
      // load all matching XML files
      List<PropertiesFile> configs = Collections.list(type.getClassLoader().getResources(name)).stream()
          .map(url -> PropertiesFile.of(ResourceLocator.ofClasspathUrl(url).getCharSource()))
          .sorted(ExtendedEnum::sortByPriority)
          .collect(Collectors.toList());
      // parse XML files
      List<NamedLookup<R>> lookups = new ArrayList<>();
      Map<String, String> alternateNames = new HashMap<>();
      for (PropertiesFile config : configs) {
        lookups.addAll(parseProviders(config, type));
        alternateNames = parseAlternates(config, alternateNames);
        if (Boolean.parseBoolean(config.getProperties().getValue("chain")) == false) {
          break;
        }
      }
      return new ExtendedEnum<>(type, ImmutableList.copyOf(lookups), ImmutableMap.copyOf(alternateNames));
      
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  // parses the alternate names
  @SuppressWarnings("unchecked")
  private static <R extends Named> List<NamedLookup<R>> parseProviders(PropertiesFile config, Class<R> enumType) {
    List<NamedLookup<R>> result = new ArrayList<>();
    if (config.getProperties().contains("provider")) {
      ImmutableList<String> providers = config.getProperties().getValueList("provider");
      for (String providerStr : providers) {
        try {
          Class<?> cls = RenameHandler.INSTANCE.lookupType(providerStr);
          if (NamedLookup.class.isAssignableFrom(cls)) {
            // class is a named lookup
            try {
              Constructor<?> cons = cls.getDeclaredConstructor();
              if (Modifier.isPublic(cls.getModifiers()) == false) {
                cons.setAccessible(true);
              }
              result.add((NamedLookup<R>) cons.newInstance());
            } catch (Exception ex) {
              throw new IllegalArgumentException("Invalid enum provider constructor: new " + cls.getName() + "()", ex);
            }
          } else {
            // extract public static final constants
            result.add(parseConstants(enumType, cls));
          }
        } catch (Exception ex) {
            throw new IllegalArgumentException("Unable to interpret enum provider: " + providerStr, ex);
        }
      }
    }
    return result;
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
  private static Map<String, String> parseAlternates(PropertiesFile config, Map<String, String> alternates) {
    if (config.getProperties().contains("alternate")) {
      ImmutableList<String> parsedAlternates = config.getProperties().getValueList("alternate");
      for (String parsedAlternate : parsedAlternates) {
        List<String> split = Splitter.on("->").limit(2).splitToList(parsedAlternate);
        if (split.size() != 2) {
          throw new IllegalArgumentException(
              "Alternate name must have format 'alternateName -> standardName', but was: " + parsedAlternate);
        }
        alternates.putIfAbsent(split.get(0).trim(), split.get(1).trim());
      }
    }
    return alternates;
  }

  // sort by priority largest first
  private static int sortByPriority(PropertiesFile a, PropertiesFile b) {
    int priority1 = Integer.parseInt(a.getProperties().getValue("priority"));
    int priority2 = Integer.parseInt(b.getProperties().getValue("priority"));
    return Integer.compare(priority2, priority1);
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
   * This find the instance matching the specified name.
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
      ImmutableMap<String, T> lookupMap = lookup.lookupAll();
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
