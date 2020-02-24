/*
 * Copyright (C) 2019 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.ByteStreams;
import com.opengamma.strata.collect.ArgChecker;

/**
 * Utility class to simplify accessing and creating zip files, and other packed formats.
 */
public final class ZipUtils {

  private ZipUtils() {
  }

  //-------------------------------------------------------------------------
  /**
   * Unzips the source returning the file names that are contained.
   * <p>
   * The result is a set of relative path names within the zip.
   * Only files are returned, not folders.
   * 
   * @param source  the byte source to unzip
   * @return the set of relative path names
   * @throws UncheckedIOException if an IO error occurs
   */
  public static Set<String> unzipPathNames(BeanByteSource source) {
    ImmutableSet.Builder<String> entryNames = ImmutableSet.builder();
    try (ZipInputStream in = new ZipInputStream(source.openStream())) {
      ZipEntry entry = in.getNextEntry();
      while (entry != null) {
        if (!entry.isDirectory()) {
          entryNames.add(entry.getName());
        }
        in.closeEntry();
        entry = in.getNextEntry();
      }
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
    return entryNames.build();
  }

  //-------------------------------------------------------------------------
  /**
   * Unzips a single file from the source in memory.
   * <p>
   * Callers can use {@link #unzipPathNames(BeanByteSource)} to find the available names.
   * The relative path name must match exactly that in the zip.
   * 
   * @param source  the byte source to unzip
   * @param relativePathName  the exact relative path name that the file is stored as
   * @return the unzipped file, empty if not found
   * @throws UncheckedIOException if an IO error occurs
   */
  public static Optional<ArrayByteSource> unzipPathNameInMemory(BeanByteSource source, String relativePathName) {
    ArgChecker.notNull(relativePathName, "relativePathName");
    try (ZipInputStream in = new ZipInputStream(source.openStream())) {
      ZipEntry entry = in.getNextEntry();
      while (entry != null) {
        if (!entry.isDirectory() && entry.getName().equals(relativePathName)) {
          return Optional.of(ArrayByteSource.ofUnsafe(ByteStreams.toByteArray(in)).withFileName(entry.getName()));
        }
        in.closeEntry();
        entry = in.getNextEntry();
      }
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
    return Optional.empty();
  }

  //-------------------------------------------------------------------------
  /**
   * Unzips the source to a file path.
   * <p>
   * Empty folders in the zip will not be created.
   * 
   * @param source  the byte source to unzip
   * @param path  the path to unzip to
   * @throws UncheckedIOException if an IO error occurs
   */
  public static void unzip(BeanByteSource source, Path path) {
    Set<ZipKey> deduplicate = new HashSet<>();
    try (ZipInputStream in = new ZipInputStream(source.openStream())) {
      ZipEntry entry = in.getNextEntry();
      while (entry != null) {
        if (!entry.isDirectory()) {
          if (deduplicate.add(new ZipKey(entry))) {
            Path resolved = path.resolve(entry.getName());
            Files.createDirectories(resolved);
            Files.copy(in, resolved, StandardCopyOption.REPLACE_EXISTING);
          }
        }
        in.closeEntry();
        entry = in.getNextEntry();
      }
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Creates a zip file from the list of files in memory.
   * <p>
   * Each source must have a file name.
   * The output will not contain subfolders.
   * 
   * @param sources  the byte sources to zip
   * @return the zip file
   * @throws UncheckedIOException if an IO error occurs
   */
  public static ArrayByteSource zipInMemory(List<? extends BeanByteSource> sources) {
    long instantMillis = System.currentTimeMillis();
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      try (ZipOutputStream out = new ZipOutputStream(baos)) {
        for (BeanByteSource source : sources) {
          ZipEntry entry = new ZipEntry(
              source.getFileName()
                  .orElseThrow(() -> new IllegalArgumentException("ByteSource must have a name in order to be zipped")));
          entry.setTime(instantMillis);
          out.putNextEntry(entry);
          source.copyTo(out);
          out.closeEntry();
        }
      }
      return ArrayByteSource.ofUnsafe(baos.toByteArray());
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Unpacks the source into memory, returning a map.
   * <p>
   * This method loads the whole unpacked file into memory.
   * Where possible, {@link #unpackInMemory(BeanByteSource, BiConsumer)} should be preferred
   * as it only loads files one at a time.
   * <p>
   * Unpacking handles ZIP, GZ and BASE64 formats based entirely on the suffix of the input file name.
   * If the input suffix is not recognized as a packed format, the consumer is invoked with the original file.
   * 
   * @param source  the byte source to unpack
   * @return the map of unzipped byte sources, keyed by relative path name
   * @throws UncheckedIOException if an IO error occurs
   */
  public static Map<String, ArrayByteSource> unpackInMemory(BeanByteSource source) {
    ImmutableMap.Builder<String, ArrayByteSource> builder = ImmutableMap.builder();
    unpackInMemory(source, builder::put);
    return builder.build();
  }

  //-------------------------------------------------------------------------
  /**
   * Unpacks the source into memory, invoking the consumer for each entry.
   * <p>
   * Unpacking handles ZIP, GZ and BASE64 formats based entirely on the suffix of the input file name.
   * If the input suffix is not recognized as a packed format, the consumer is invoked with the original file.
   * 
   * @param source  the byte source to unpack
   * @param consumer  the consumer, which is passed the relative path name and content for each entry
   * @throws UncheckedIOException if an IO error occurs
   */
  public static void unpackInMemory(BeanByteSource source, BiConsumer<String, ArrayByteSource> consumer) {
    String fileName = source.getFileName().orElse("");
    if (suffixMatches(fileName, ".zip")) {
      unzipInMemory(source, consumer);

    } else if (suffixMatches(fileName, ".gz")) {
      ungzInMemory(source, fileName, consumer);

    } else if (suffixMatches(fileName, ".base64")) {
      try (InputStream in = Base64.getDecoder().wrap(source.openBufferedStream())) {
        String shortFileName = fileName.substring(0, fileName.length() - 7);
        ArrayByteSource unbase64 = ArrayByteSource.from(in).withFileName(shortFileName);
        consumer.accept(shortFileName, unbase64);
      } catch (IOException ex) {
        throw new UncheckedIOException(ex);
      }

    } else {
      consumer.accept(fileName, source.load());
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Unzips the source into memory, returning a map.
   * <p>
   * This method loads the whole unpacked file into memory.
   * Where possible, {@link #unzipInMemory(BeanByteSource, BiConsumer)} should be preferred
   * as it only loads files one at a time.
   * <p>
   * Unlike {@link #unpackInMemory(BeanByteSource)}, this method always treats the input as a zip file.
   * 
   * @param source  the byte source to unpack
   * @return the map of unzipped byte sources, keyed by relative path name
   * @throws UncheckedIOException if an IO error occurs
   */
  public static Map<String, ArrayByteSource> unzipInMemory(BeanByteSource source) {
    ImmutableMap.Builder<String, ArrayByteSource> builder = ImmutableMap.builder();
    unzipInMemory(source, builder::put);
    return builder.build();
  }

  //-------------------------------------------------------------------------
  /**
   * Unzips the source into memory, invoking the consumer for each entry.
   * <p>
   * Unlike {@link #unpackInMemory(BeanByteSource, BiConsumer)}, this method always treats the input as a zip file.
   * 
   * @param source  the byte source to unpack
   * @param consumer  the consumer, which is passed the relative path name and content for each entry
   * @throws UncheckedIOException if an IO error occurs
   */
  public static void unzipInMemory(BeanByteSource source, BiConsumer<String, ArrayByteSource> consumer) {
    Set<ZipKey> deduplicate = new HashSet<>();
    try (ZipInputStream in = new ZipInputStream(source.openStream())) {
      ZipEntry entry = in.getNextEntry();
      while (entry != null) {
        if (!entry.isDirectory()) {
          if (deduplicate.add(new ZipKey(entry))) {
            ArrayByteSource entrySource = ArrayByteSource.ofUnsafe(ByteStreams.toByteArray(in)).withFileName(entry.getName());
            consumer.accept(entry.getName(), entrySource);
          }
        }
        in.closeEntry();
        entry = in.getNextEntry();
      }
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  //-------------------------------------------------------------------------
  // ungz the contents
  private static void ungzInMemory(BeanByteSource source, String fileName, BiConsumer<String, ArrayByteSource> consumer) {
    try (GZIPInputStream in = new GZIPInputStream(source.openStream())) {
      String shortFileName = fileName.substring(0, fileName.length() - 3);
      ArrayByteSource entrySource = ArrayByteSource.ofUnsafe(ByteStreams.toByteArray(in)).withFileName(shortFileName);
      consumer.accept(shortFileName, entrySource);
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  // safely extract the suffix
  private static boolean suffixMatches(String name, String suffix) {
    return name.regionMatches(true, name.length() - suffix.length(), suffix, 0, suffix.length());
  }

  //-------------------------------------------------------------------------
  // handle duplicate entries in a zip file
  // while such a zip file is stupid, it is apparently valid
  private static class ZipKey {
    private final String fileName;
    private final long crc;
    private final long size;

    private ZipKey(ZipEntry entry) {
      fileName = entry.getName();
      crc = entry.getCrc();
      size = entry.getSize();
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof ZipKey) {
        ZipKey key = (ZipKey) obj;
        return fileName.equals(key.fileName) &&
            crc == key.crc &&
            size == key.size;
      }
      return false;
    }

    @Override
    public int hashCode() {
      return super.hashCode();
    }
  }

}
