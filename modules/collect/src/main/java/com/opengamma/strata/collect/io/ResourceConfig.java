/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.io;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.Unchecked;

/**
 * Provides access to configuration files.
 * <p>
 * A standard approach to configuration is provided by this class.
 * Any configuration information provided by this library can be overridden or added to by applications.
 * <p>
 * By default, there are three groups of recognized configuration directories:
 * <ul>
 * <li>base
 * <li>library
 * <li>application
 * </ul>
 * <p>
 * Each group consists of ten directories using a numeric suffix:
 * <ul>
 * <li>{@code com/opengamma/strata/config/base}
 * <li>{@code com/opengamma/strata/config/base1}
 * <li>{@code com/opengamma/strata/config/base2}
 * <li>...
 * <li>{@code com/opengamma/strata/config/base9}
 * <li>{@code com/opengamma/strata/config/library}
 * <li>{@code com/opengamma/strata/config/library1}
 * <li>...
 * <li>{@code com/opengamma/strata/config/library9}
 * <li>{@code com/opengamma/strata/config/application}
 * <li>{@code com/opengamma/strata/config/application1}
 * <li>...
 * <li>{@code com/opengamma/strata/config/application9}
 * </ul>
 * These form a complete set of thirty directories that are searched for configuration.
 * <p>
 * The search strategy looks for the same file name in each of the thirty directories.
 * All the files that are found are then merged, with directories lower down the list taking priorty.
 * Thus, any configuration file in the 'application9' directory will override the same file
 * in the 'appication1' directory, which will override the same file in the 'library' group,
 * which will further override the same file in the 'base' group.
 * <p>
 * The 'base' group is reserved for Strata.
 * The 'library' group is reserved for libraries built directly on Strata.
 * <p>
 * The set of configuration directories can be changed using the system property
 * 'com.opengamma.strata.config.directories'.
 * This must be a comma separated list, such as 'base,base1,base2,override,application'.
 * <p>
 * In general, the configuration managed by this class will be in INI format.
 * The {@link #combinedIniFile(String)} method is the main entry point, returning a single
 * INI file merged from all available configuration files.
 */
public final class ResourceConfig {

  /**
   * The logger.
   */
  private static final Logger log = Logger.getLogger(ResourceConfig.class.getName());
  /**
   * The package/folder location for the configuration.
   */
  private static final String CONFIG_PACKAGE = "com/opengamma/strata/config/";
  /**
   * The default set of directories to query configuration files in.
   */
  private static final ImmutableList<String> DEFAULT_DIRS = ImmutableList.of(
      "base",
      "base1",
      "base2",
      "base3",
      "base4",
      "base5",
      "base6",
      "base7",
      "base8",
      "base9",
      "library",
      "library1",
      "library2",
      "library3",
      "library4",
      "library5",
      "library6",
      "library7",
      "library8",
      "library9",
      "application",
      "application1",
      "application2",
      "application3",
      "application4",
      "application5",
      "application6",
      "application7",
      "application8",
      "application9");
  /**
   * The system property defining the comma separated list of groups.
   */
  public static final String RESOURCE_DIRS_PROPERTY = "com.opengamma.strata.config.directories";
  /**
   * The resource groups.
   * Always falls back to the known set in case of error.
   */
  private static final ImmutableList<String> RESOURCE_DIRS;
  static {
    List<String> dirs = DEFAULT_DIRS;
    String property = null;
    try {
      property = System.getProperty(RESOURCE_DIRS_PROPERTY);
    } catch (Exception ex) {
      log.warning("Unable to access system property: " + ex.toString());
    }
    if (property != null && !property.isEmpty()) {
      try {
        dirs = Splitter.on(',').trimResults().splitToList(property);
      } catch (Exception ex) {
        log.warning("Invalid system property: " + property + ": " + ex.toString());
      }
      for (String dir : dirs) {
        if (!dir.matches("[A-Za-z0-9-]+")) {
          log.warning("Invalid system property directory, must match regex [A-Za-z0-9-]+: " + dir);
        }
      }
    }
    log.config("Using directories: " + dirs);
    RESOURCE_DIRS = ImmutableList.copyOf(dirs);
  }
  /**
   * INI section name used for chaining.
   */
  private static final String CHAIN_SECTION = "chain";
  /**
   * INI property name used for chaining.
   */
  private static final String CHAIN_NEXT = "chainNextFile";
  /**
   * INI property name used for removing sections.
   */
  private static final String CHAIN_REMOVE = "chainRemoveSections";

  //-------------------------------------------------------------------------
  /**
   * Returns a combined INI file formed by merging INI files with the specified name.
   * <p>
   * This finds the all files with the specified name in the configuration directories.
   * Each file is loaded, with the result being formed by merging the files into one.
   * See {@link #combinedIniFile(List)} for more details on the merge process.
   * 
   * @param resourceName  the resource name
   * @return the resource locators
   * @throws UncheckedIOException if an IO exception occurs
   * @throws IllegalStateException if there is a configuration error
   */
  public static IniFile combinedIniFile(String resourceName) {
    ArgChecker.notNull(resourceName, "resourceName");
    return ResourceConfig.combinedIniFile(ResourceConfig.orderedResources(resourceName));
  }

  /**
   * Returns a combined INI file formed by merging the specified INI files.
   * <p>
   * The result of this method is formed by merging the specified files together.
   * The files are combined in order forming a chain.
   * The first file in the list has the lowest priority.
   * The last file in the list has the highest priority.
   * <p>
   * The algorithm starts with all the sections and properties from the highest priority file.
   * It then adds any sections or properties from subsequent files that are not already present.
   * <p>
   * The algorithm can be controlled by providing a '[chain]' section.
   * Within the 'chain' section, if 'chainNextFile' is 'false', then processing stops,
   * and lower priority files are ignored. If the 'chainRemoveSections' property is specified,
   * the listed sections are ignored from the files lower in the chain.
   * 
   * @param resources  the INI file resources to read
   * @return the combined chained INI file
   * @throws UncheckedIOException if an IO error occurs
   * @throws IllegalArgumentException if the configuration is invalid
   */
  public static IniFile combinedIniFile(List<ResourceLocator> resources) {
    ArgChecker.notNull(resources, "resources");
    Map<String, PropertySet> sectionMap = new LinkedHashMap<>();
    for (ResourceLocator resource : resources) {
      IniFile file = IniFile.of(resource.getCharSource());
      if (file.contains(CHAIN_SECTION)) {
        PropertySet chainSection = file.section(CHAIN_SECTION);
        // remove everything from lower priority files if not chaining
        if (chainSection.contains(CHAIN_NEXT) && Boolean.parseBoolean(chainSection.value(CHAIN_NEXT)) == false) {
          sectionMap.clear();
        } else {
          // remove sections from lower priority files
          sectionMap.keySet().removeAll(chainSection.valueList(CHAIN_REMOVE));
        }
      }
      // add entries, replacing existing data
      for (String sectionName : file.asMap().keySet()) {
        if (!sectionName.equals(CHAIN_SECTION)) {
          sectionMap.merge(sectionName, file.section(sectionName), PropertySet::overrideWith);
        }
      }
    }
    return IniFile.of(sectionMap);
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains an ordered list of resource locators.
   * <p>
   * This finds the all files with the specified name in the configuration directories.
   * The result is ordered from the lowest priority (base) file to the highest priority (application) file.
   * The result will always contain at least one file, but it may contain more than one.
   * 
   * @param resourceName  the resource name
   * @return the resource locators
   * @throws UncheckedIOException if an IO exception occurs
   * @throws IllegalStateException if there is a configuration error
   */
  public static List<ResourceLocator> orderedResources(String resourceName) {
    ArgChecker.notNull(resourceName, "resourceName");
    return Unchecked.wrap(() -> orderedResources0(resourceName));
  }

  // find the list of resources
  private static List<ResourceLocator> orderedResources0(String classpathResourceName) throws IOException {
    ClassLoader classLoader = ResourceLocator.classLoader();
    List<String> names = new ArrayList<>();
    List<ResourceLocator> result = new ArrayList<>();
    for (String dir : RESOURCE_DIRS) {
      String name = CONFIG_PACKAGE + dir + "/" + classpathResourceName;
      names.add(name);
      List<URL> urls = Collections.list(classLoader.getResources(name));
      switch (urls.size()) {
        case 0:
          continue;
        case 1:
          result.add(ResourceLocator.ofClasspathUrl(urls.get(0)));
          break;
        default:
          // handle case where Strata is on the classpath more than once
          // only accept this if the data being read is the same in all URLs
          ResourceLocator baseResource = ResourceLocator.ofClasspathUrl(urls.get(0));
          for (int i = 1; i < urls.size(); i++) {
            ResourceLocator otherResource = ResourceLocator.ofClasspathUrl(urls.get(i));
            if (!baseResource.getByteSource().contentEquals(otherResource.getByteSource())) {
              log.severe("More than one file found on the classpath: " + name + ": " + urls);
              throw new IllegalStateException("More than one file found on the classpath: " + name + ": " + urls);
            }
          }
          result.add(baseResource);
          break;
      }
    }
    if (result.isEmpty()) {
      log.severe("No resource files found on the classpath: " + names);
      throw new IllegalStateException("No files found on the classpath: " + names);
    }
    log.config(() -> "Resources found: " + result);
    return result;
  }

  //-------------------------------------------------------------------------
  private ResourceConfig() {
  }

}
