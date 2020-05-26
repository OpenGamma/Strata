/*
 * Copyright (C) 2020 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.io;

import static com.opengamma.strata.collect.Guavate.toImmutableList;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Provides methods to operate on files using {@code Path} that avoid leaking file handles.
 */
public final class SafeFiles {

  private SafeFiles() {
  }

  //-------------------------------------------------------------------------
  /**
   * Lists the elements in the specified directory without recursing into subdirectories.
   * <p>
   * This is a safe wrapper for {@link Files#list(Path)}.
   * 
   * @param dir  the path to list files
   * @return the list of paths in the specified directory
   * @throws SecurityException if access is rejected by a security manager
   * @throws UncheckedIOException if an IO error occurs
   */
  public static List<Path> listAll(Path dir) {
    return list(dir, stream -> stream.collect(toImmutableList()));
  }

  /**
   * Streams the elements in the specified directory without recursing into subdirectories.
   * <p>
   * This is a safe wrapper for {@link Files#list(Path)}.
   * 
   * @param <T>  the type of the result
   * @param dir  the path to list files
   * @param function  the function to apply to the stream
   * @return the result of the function
   * @throws SecurityException if access is rejected by a security manager
   * @throws UncheckedIOException if an IO error occurs
   */
  public static <T> T list(Path dir, Function<Stream<Path>, T> function) {
    try (Stream<Path> stream = Files.list(dir)) {
      return function.apply(stream);
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Lists the elements in the specified directory, recursing depth-first into subdirectories.
   * <p>
   * This is a safe wrapper for {@link Files#walk(Path, FileVisitOption...)}.
   * It recurses into all subdirectories and does not follow links.
   * 
   * @param dir  the start path to list files from
   * @return the list of paths in the specified directory
   * @throws IllegalArgumentException if maxDepth is negative
   * @throws SecurityException if access is rejected by a security manager
   * @throws UncheckedIOException if an IO error occurs
   */
  public static List<Path> walkAll(Path dir) {
    return walk(dir, Integer.MAX_VALUE, stream -> stream.collect(toImmutableList()));
  }

  /**
   * Lists the elements in the specified directory, recursing depth-first into subdirectories.
   * <p>
   * This is a safe wrapper for {@link Files#walk(Path, int, FileVisitOption...)}.
   * 
   * @param dir  the start path to list files from
   * @param maxDepth  the maximum depth to recurse to, one for one-level and so on
   * @param options  the options
   * @return the list of paths in the specified directory
   * @throws IllegalArgumentException if maxDepth is negative
   * @throws SecurityException if access is rejected by a security manager
   * @throws UncheckedIOException if an IO error occurs
   */
  public static List<Path> walkAll(Path dir, int maxDepth, FileVisitOption... options) {
    return walk(dir, maxDepth, stream -> stream.collect(toImmutableList()), options);
  }

  /**
   * Lists the elements in the specified directory, recursing depth-first into subdirectories.
   * <p>
   * This is a safe wrapper for {@link Files#walk(Path, int, FileVisitOption...)}.
   * 
   * @param <T>  the type of the result
   * @param dir  the start path to list files from
   * @param maxDepth  the maximum depth to recurse to, one for one-level and so on
   * @param function  the function to apply to the stream
   * @param options  the options
   * @return the result of the function
   * @throws IllegalArgumentException if maxDepth is negative
   * @throws SecurityException if access is rejected by a security manager
   * @throws UncheckedIOException if an IO error occurs
   */
  public static <T> T walk(Path dir, int maxDepth, Function<Stream<Path>, T> function, FileVisitOption... options) {
    try (Stream<Path> stream = Files.walk(dir, maxDepth, options)) {
      return function.apply(stream);
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Loads the specified file as a list of lines using UTF-8.
   * <p>
   * This is a safe wrapper for {@link Files#lines(Path)}.
   * 
   * @param file  the path to read
   * @return the list of lines in the specified file
   * @throws SecurityException if access is rejected by a security manager
   * @throws UncheckedIOException if an IO error occurs
   */
  public static List<String> linesAll(Path file) {
    return lines(file, stream -> stream.collect(toImmutableList()));
  }

  /**
   * Streams the lines in the specified file using UTF-8.
   * <p>
   * This is a safe wrapper for {@link Files#lines(Path)}.
   * 
   * @param <T>  the type of the result
   * @param file  the path to read
   * @param function  the function to apply to the stream
   * @return the result of the function
   * @throws SecurityException if access is rejected by a security manager
   * @throws UncheckedIOException if an IO error occurs
   */
  public static <T> T lines(Path file, Function<Stream<String>, T> function) {
    try (Stream<String> stream = Files.lines(file)) {
      return function.apply(stream);
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

}
