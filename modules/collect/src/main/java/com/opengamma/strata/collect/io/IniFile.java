/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.io;

import static java.util.stream.Collectors.toList;

import java.io.UncheckedIOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.io.CharSource;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.Unchecked;

/**
 * An INI file.
 * <p>
 * Represents an INI file together with the ability to parse it from a {@link CharSource}.
 * <p>
 * The INI file format used here is deliberately simple.
 * There are two elements - key-value pairs and sections.
 * <p>
 * The basic element is a key-value pair.
 * The key is separated from the value using the '=' symbol.
 * Duplicate keys are allowed.
 * For example 'key = value'.
 * <p>
 * All properties are grouped into named sections.
 * The section name occurs on a line by itself surrounded by square brackets.
 * Duplicate section names are not allowed.
 * For example '[section]'.
 * <p>
 * Keys, values and section names are trimmed.
 * Blank lines are ignored.
 * Whole line comments begin with hash '#' or semicolon ';'.
 * No escape format is available.
 * Lookup is case sensitive.
 * <p>
 * This example explains the format:
 * <pre>
 *  # line comment
 *  [foo]
 *  key = value
 * 
 *  [bar]
 *  key = value
 *  month = January
 * </pre>
 * <p>
 * The aim of this class is to parse the basic format.
 * Interpolation of variables is not supported.
 */
public final class IniFile {

  /**
   * Section name used for chaining.
   */
  private static final String CHAIN_SECTION = "chain";
  /**
   * Property name used for priority.
   */
  private static final String PRIORITY = "priority";
  /**
   * Property name used for chaining.
   */
  private static final String CHAIN_NEXT = "chainNextFile";
  /**
   * Property name used for removing sections.
   */
  private static final String CHAIN_REMOVE = "chainRemoveSections";

  /**
   * The INI sections.
   */
  private final ImmutableMap<String, PropertySet> sectionMap;

  //-------------------------------------------------------------------------
  /**
   * Parses the specified source as an INI file.
   * <p>
   * This parses the specified character source expecting an INI file format.
   * The resulting instance can be queried for each section in the file.
   * 
   * @param source  the INI file resource
   * @return the INI file
   * @throws UncheckedIOException if an IO exception occurs
   * @throws IllegalArgumentException if the file cannot be parsed
   */
  public static IniFile of(CharSource source) {
    ArgChecker.notNull(source, "source");
    ImmutableList<String> lines = Unchecked.wrap(() -> source.readLines());
    Map<String, Multimap<String, String>> parsedIni = parse(lines);
    ImmutableMap.Builder<String, PropertySet> builder = ImmutableMap.builder();
    parsedIni.forEach((sectionName, sectionData) -> builder.put(sectionName, PropertySet.of(sectionData)));
    return new IniFile(builder.build());
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a single INI file that is the chained combination of the inputs.
   * <p>
   * The result of this method is formed by chaining all the specified files together.
   * The files are combined using a simple algorithm defined in the '[chain]' section.
   * Firstly, the 'priority' value is used to sort the files, higher numbers have higher priority
   * All entries in the highest priority file are used
   * <p>
   * Once data from the highest priority file is included, the 'chainNextFile' property is examined.
   * If 'chainNextFile' is 'true', then the next file in the chain is considered.
   * The 'chainRemoveSections' property can be used to ignore specific sections from the files lower in the chain.
   * The chain process continues until the 'chainNextFile' is 'false', or all files have been combined.
   * 
   * @param sources  the INI file sources to read
   * @return the combined chained INI file
   * @throws UncheckedIOException if an IO error occurs
   * @throws IllegalArgumentException if the configuration is invalid
   */
  public static IniFile ofChained(Stream<CharSource> sources) {
    ArgChecker.notNull(sources, "sources");
    List<IniFile> files = sources
        .map(IniFile::of)
        .sorted(IniFile::compareByReversePriority)
        .collect(toList());
    // combine files, based on chain flag
    Map<String, PropertySet> builder = new LinkedHashMap<>();
    for (IniFile file : files) {
      // remove everything from lower priority files if not chaining
      if (Boolean.parseBoolean(file.section(CHAIN_SECTION).value(CHAIN_NEXT)) == false) {
        builder.clear();
      } else {
        // remove sections from lower priority files
        builder.keySet().removeAll(file.section(CHAIN_SECTION).valueList(CHAIN_REMOVE));
      }
      // add entries, replacing existing data
      for (String sectionName : file.asMap().keySet()) {
        if (!sectionName.equals(CHAIN_SECTION)) {
          builder.merge(sectionName, file.section(sectionName), PropertySet::combinedWith);
        }
      }
    }
    return new IniFile(ImmutableMap.copyOf(builder));
  }

  // sort by priority, lowest first
  private static int compareByReversePriority(IniFile a, IniFile b) {
    int priority1 = Integer.parseInt(a.section(CHAIN_SECTION).value(PRIORITY));
    int priority2 = Integer.parseInt(b.section(CHAIN_SECTION).value(PRIORITY));
    return Integer.compare(priority1, priority2);
  }

  //-------------------------------------------------------------------------
  // parses the INI file format
  private static Map<String, Multimap<String, String>> parse(ImmutableList<String> lines) {
    Map<String, Multimap<String, String>> ini = new LinkedHashMap<>();
    Multimap<String, String> currentSection = null;
    int lineNum = 0;
    for (String line : lines) {
      lineNum++;
      line = line.trim();
      if (line.length() == 0 || line.startsWith("#") || line.startsWith(";")) {
        continue;
      }
      if (line.startsWith("[") && line.endsWith("]")) {
        String sectionName = line.substring(1, line.length() - 1).trim();
        if (ini.containsKey(sectionName)) {
          throw new IllegalArgumentException("Invalid INI file, duplicate section not allowed, line " + lineNum);
        }
        currentSection = ArrayListMultimap.create();
        ini.put(sectionName, currentSection);

      } else if (currentSection == null) {
        throw new IllegalArgumentException("Invalid INI file, properties must be within a [section], line " + lineNum);

      } else {
        int equalsPosition = line.indexOf('=');
        if (equalsPosition < 0) {
          throw new IllegalArgumentException("Invalid INI file, expected key=value property, line " + lineNum);
        }
        String key = line.substring(0, equalsPosition).trim();
        String value = line.substring(equalsPosition + 1).trim();
        if (key.length() == 0) {
          throw new IllegalArgumentException("Invalid INI file, empty key, line " + lineNum);
        }
        currentSection.put(key, value);
      }
    }
    return ini;
  }

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   * 
   * @param sectionMap  the sections
   */
  private IniFile(ImmutableMap<String, PropertySet> sectionMap) {
    this.sectionMap = sectionMap;
  }

  //-------------------------------------------------------------------------
  /**
   * Returns the set of sections of this INI file.
   * 
   * @return the set of sections
   */
  public ImmutableSet<String> sections() {
    return sectionMap.keySet();
  }

  /**
   * Returns the INI file as a map.
   * <p>
   * The iteration order of the map matches that of the original file.
   * 
   * @return the INI file sections
   */
  public ImmutableMap<String, PropertySet> asMap() {
    return sectionMap;
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if this INI file contains the specified section.
   * 
   * @param name  the section name
   * @return true if the section exists
   */
  public boolean contains(String name) {
    ArgChecker.notNull(name, "name");
    return sectionMap.containsKey(name);
  }

  /**
   * Gets a single section of this INI file.
   * <p>
   * This returns the section associated with the specified name.
   * If the section does not exist an exception is thrown.
   * 
   * @param name  the section name
   * @return the INI file section
   * @throws IllegalArgumentException if the section does not exist
   */
  public PropertySet section(String name) {
    ArgChecker.notNull(name, "name");
    if (contains(name) == false) {
      throw new IllegalArgumentException("Unknown INI file section: " + name);
    }
    return sectionMap.get(name);
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if this INI file equals another.
   * <p>
   * The comparison checks the content.
   * 
   * @param obj  the other file, null returns false
   * @return true if equal
   */
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj instanceof IniFile) {
      return sectionMap.equals(((IniFile) obj).sectionMap);
    }
    return false;
  }

  /**
   * Returns a suitable hash code for the INI file.
   * 
   * @return the hash code
   */
  @Override
  public int hashCode() {
    return sectionMap.hashCode();
  }

  /**
   * Returns a string describing the INI file.
   * 
   * @return the descriptive string
   */
  @Override
  public String toString() {
    return sectionMap.toString();
  }

}
