/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.examples.marketdata;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.io.ResourceLocator;

/**
 * Loads market data from the standard directory structure embedded within a JAR file.
 */
public class JarMarketDataBuilder extends MarketDataBuilder {

  /**
   * The JAR file containing the expected structure of resources.
   */
  private final File jarFile;

  /**
   * The root path to the resources within the JAR file.
   */
  private final String rootPath;

  /**
   * Constructs an instance.
   * 
   * @param jarFile  the JAR file containing the expected structure of resources
   * @param rootPath  the root path to the resources within the JAR file
   */
  public JarMarketDataBuilder(File jarFile, String rootPath) {
    String jarRoot = rootPath.startsWith(File.separator) ? rootPath.substring(1) : rootPath;
    if (!jarRoot.endsWith(File.separator)) {
      jarRoot += File.separator;
    }
    this.jarFile = jarFile;
    this.rootPath = jarRoot;
  }

  @Override
  protected Collection<ResourceLocator> getAllResources(String subdirectoryName) {
    String fullSubdirectory = String.format("%s%s", rootPath, subdirectoryName);
    try (JarFile jar = new JarFile(jarFile)) {
      List<ResourceLocator> resources = new ArrayList<ResourceLocator>();
      Enumeration<JarEntry> jarEntries = jar.entries();
      while (jarEntries.hasMoreElements()) {
        JarEntry entry = jarEntries.nextElement();
        String entryName = entry.getName();
        if (entryName.startsWith(fullSubdirectory) && !entryName.equals(fullSubdirectory)) {
          resources.add(getEntryLocator(entry));
        }
      }
      return resources;
    } catch (Exception e) {
      throw new IllegalArgumentException(
          Messages.format("Error loading market data from JAR file: {}", jarFile), e);
    }
  }

  @Override
  protected ResourceLocator getResource(String subdirectoryName, String resourceName) {
    String fullLocation = String.format("%s%s%s%s", rootPath, subdirectoryName, File.separator, resourceName);
    try (JarFile jar = new JarFile(jarFile)) {
      JarEntry entry = jar.getJarEntry(fullLocation);
      if (entry == null) {
        return null;
      }
      return getEntryLocator(entry);
    } catch (Exception e) {
      throw new IllegalArgumentException(
          Messages.format("Error loading resource from JAR file: {}", jarFile), e);
    }
  }

  @Override
  protected boolean subdirectoryExists(String subdirectoryName) {
    String fullSubdirectory = String.format("%s%s", rootPath, subdirectoryName);
    try (JarFile jar = new JarFile(jarFile)) {
      Enumeration<JarEntry> jarEntries = jar.entries();
      while (jarEntries.hasMoreElements()) {
        JarEntry entry = jarEntries.nextElement();
        String entryName = entry.getName();
        if (entryName.startsWith(fullSubdirectory)) {
          return true;
        }
      }
      return false;
    } catch (Exception e) {
      throw new IllegalArgumentException(
          Messages.format("Error loading resource from JAR file: {}", jarFile), e);
    }
  }
  
  //-------------------------------------------------------------------------
  // Gets the resource locator corresponding to a given entry
  private ResourceLocator getEntryLocator(JarEntry entry) {
    return ResourceLocator.of(ResourceLocator.CLASSPATH_URL_PREFIX + entry.getName());
  }

}
