/*
 * Copyright (C) 2025 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.named;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;

/**
 * Test that reproduces ExtendedEnum failures in isolated classloader hierarchies.
 * <p>
 * This program recreates the classloader structure found in application servers,
 * OSGi containers, and multi-module applications where libraries are separated
 * into parent (common/shared) and child (application-specific) classloaders:
 * <p>
 * Parent ClassLoader (Common/Shared Libraries)
 * <ul>
 * <li>Joda-Convert (includes RenameHandler)</li>
 * <li>Joda-Beans</li>
 * <li>Guava</li>
 * </ul>
 * <p>
 * Child ClassLoader (Application Libraries)
 * <ul>
 * <li>Strata modules (ExtendedEnum, StandardDayCounts, etc.)</li>
 * <li>Delegates to parent for common libraries</li>
 * </ul>
 * <p>
 * ForkJoinPool Worker Threads - Thread Context ClassLoader (TCCL) = null
 * <p>
 * When ExtendedEnum.parseProviders() (loaded in child) calls RenameHandler.lookupType()
 * (loaded in parent) to find StandardDayCounts:
 * <ol>
 * <li>RenameHandler checks TCCL → null → unavailable</li>
 * <li>RenameHandler uses its own classloader → parent</li>
 * <li>Parent classloader cannot see child classes → ClassNotFoundException</li>
 * </ol>
 * <p>
 * This reproduces the exact failure reported in production environments.
 * <p>
 * Usage:
 * <pre>
 *   java ExtendedEnumClassLoaderIsolationTest &lt;strata-module-jar&gt; [&lt;strata-module-jar&gt; ...]
 * </pre>
 * <p>
 * Arguments should be Strata module JARs to load in the child classloader (the code under test).
 * Parent classloader dependencies (Joda-Convert, Guava, etc.) are automatically discovered from
 * the local Maven repository to simulate production application server environments.
 * <p>
 * This test is executed automatically during Maven's verify phase via exec-maven-plugin.
 */
public class ExtendedEnumClassLoaderIsolationTest {

  public static void main(String[] args) throws Exception {
    if (args.length < 1) {
      System.err.println("Usage: ExtendedEnumClassLoaderIsolationTest <strata-module-jar> [<strata-module-jar> ...]");
      System.err.println();
      System.err.println("Arguments: Strata module JARs to load in CHILD classloader (the code under test)");
      System.err.println("Note: Parent classloader dependencies (Joda-Convert, Guava, etc.) are automatically");
      System.err.println("      discovered from the local Maven repository to simulate production environments.");
      System.exit(1);
    }
    
    System.out.println("ExtendedEnum ClassLoader Isolation Test");
    System.out.println("Testing ExtendedEnum initialization in isolated classloader hierarchy");
    
    String m2Repo = System.getProperty("user.home") + "/.m2/repository";
    
    // PARENT CLASSLOADER: Common/shared libraries (like in app server's lib/)
    // This simulates the "common" classloader in Tomcat, or shared bundle in OSGi
    List<URL> parentJars = new ArrayList<>();
    addJarsFromDir(parentJars, new File(m2Repo + "/org/joda/joda-convert"));
    addJarsFromDir(parentJars, new File(m2Repo + "/org/joda/joda-beans"));
    addJarsFromDir(parentJars, new File(m2Repo + "/com/google/guava"));
    addJarsFromDir(parentJars, new File(m2Repo + "/com/google/code/findbugs"));
    addJarsFromDir(parentJars, new File(m2Repo + "/com/google/errorprone"));
    
    System.out.println("  Parent classloader: " + parentJars.size() + " JARs (Joda-Convert, Joda-Beans, Guava)");
    
    URLClassLoader parentCL = new URLClassLoader(
        parentJars.toArray(new URL[0]), ClassLoader.getSystemClassLoader().getParent());
    
    // CHILD CLASSLOADER: Application classes (like in app server's webapps/)
    // This simulates the "webapp" classloader in Tomcat, or application bundle in OSGi
    List<URL> childJars = new ArrayList<>();
    for (String jarPath : args) {
      addIfExists(childJars, jarPath);
    }
    
    System.out.println("  Child classloader: " + childJars.size() + " JARs (Strata modules)");
    
    URLClassLoader childCL = new URLClassLoader(
        childJars.toArray(new URL[0]), parentCL);  // Parent can see Joda-Convert, but child has Strata
    
    // Verify the hierarchy is correctly isolated
    parentCL.loadClass("org.joda.convert.RenameHandler");
    childCL.loadClass("com.opengamma.strata.collect.named.ExtendedEnum");
    try {
      parentCL.loadClass("com.opengamma.strata.basics.date.StandardDayCounts");
      throw new AssertionError("Parent classloader should not see StandardDayCounts");
    } catch (ClassNotFoundException ignored) {
      // Expected - parent cannot see child classes
    }
    childCL.loadClass("com.opengamma.strata.basics.date.StandardDayCounts");
    System.out.println("  Classloader isolation verified");
    
    ForkJoinPool pool = new ForkJoinPool(
        1,
        poolArg -> {
          ForkJoinWorkerThread thread = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(poolArg);
          thread.setContextClassLoader(null);  // NULL TCCL
          return thread;
        },
        null,
        false);
    
    try {
      Callable<Void> task = () -> {
        try {
          Class<?> dayCountClass = childCL.loadClass("com.opengamma.strata.basics.date.DayCount");
          Method extendedEnumMethod = dayCountClass.getMethod("extendedEnum");
          extendedEnumMethod.invoke(null);
          return null;
        } catch (Exception e) {
          // Extract root cause
          Throwable cause = e;
          while (cause.getCause() != null) {
            cause = cause.getCause();
          }
          
          if (cause instanceof ClassNotFoundException) {
            throw new AssertionError(
                "ClassNotFoundException in ForkJoinPool with isolated classloaders. " +
                "This indicates issue #2748 has regressed. " +
                "ExtendedEnum failed to load: " + cause.getMessage(), cause);
          }
          
          throw e;
        }
      };
      
      pool.submit(task).get();
      System.out.println("  ✓ ExtendedEnum initialization successful in ForkJoinPool with null TCCL");
      System.out.println("  ✓ Test passed");
      
    } catch (Exception e) {
      System.err.println("TEST FAILED:");
      e.printStackTrace(System.err);
      System.exit(1);
    } finally {
      pool.shutdown();
    }
  }
  
  private static void addIfExists(List<URL> urls, String path) {
    File file = new File(path);
    if (file.exists()) {
      try {
        urls.add(file.toURI().toURL());
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }
  
  private static void addJarsFromDir(List<URL> urls, File dir) {
    if (!dir.exists()) {
      return;
    }
    addJarsRecursive(urls, dir);
  }
  
  private static void addJarsRecursive(List<URL> urls, File dir) {
    File[] files = dir.listFiles();
    if (files == null) {
      return;
    }
    
    for (File file : files) {
      if (file.isDirectory()) {
        addJarsRecursive(urls, file);
      } else if (file.getName().endsWith(".jar")) {
        try {
          urls.add(file.toURI().toURL());
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
  }
}
