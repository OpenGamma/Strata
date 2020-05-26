/*
 * Copyright (C) 2020 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.io;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import com.google.common.io.MoreFiles;

/**
 * Test {@link SafeFiles}.
 */
@TestInstance(Lifecycle.PER_CLASS)
public class SafeFilesTest {

  private static final Path DIR = Paths.get("dir");
  private static final Path SUBDIR = DIR.resolve("sub");
  private static final Path PATH1 = SUBDIR.resolve("file1.txt");
  private static final Path PATH2 = DIR.resolve("file2.txt");
  private static final Path PATH3 = Paths.get("file3.txt");

  private Path tmpDir;

  @BeforeAll
  public void setup() throws IOException {
    tmpDir = Files.createTempDirectory("safe-files-test");
    Files.createDirectories(tmpDir.resolve(SUBDIR));
    Files.write(tmpDir.resolve(PATH1), "HELLO".getBytes(StandardCharsets.UTF_8));
    Files.write(tmpDir.resolve(PATH2), "HELLO".getBytes(StandardCharsets.UTF_8));
    Files.write(tmpDir.resolve(PATH3), "HELLO".getBytes(StandardCharsets.UTF_8));
  }

  @AfterAll
  public void tearDown() {
    try {
      MoreFiles.deleteRecursively(tmpDir);
    } catch (IOException ex) {
      // ignore
    }
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_listAll() {
    assertThat(SafeFiles.listAll(tmpDir)).containsOnly(tmpDir.resolve(DIR), tmpDir.resolve(PATH3));
    assertThat(SafeFiles.listAll(tmpDir.resolve(DIR))).containsOnly(tmpDir.resolve(SUBDIR), tmpDir.resolve(PATH2));
    assertThat(SafeFiles.listAll(tmpDir.resolve(SUBDIR))).containsOnly(tmpDir.resolve(PATH1));
  }

  @Test
  public void test_walkAll() {
    // the order seems to have changed between Java 8 and 11
    assertThat(SafeFiles.walkAll(tmpDir))
        .containsOnly(
            tmpDir,
            tmpDir.resolve(PATH3),
            tmpDir.resolve(DIR),
            tmpDir.resolve(PATH2),
            tmpDir.resolve(SUBDIR),
            tmpDir.resolve(PATH1));
  }

  @Test
  public void test_linesAll() {
    assertThat(SafeFiles.linesAll(tmpDir.resolve(PATH3))).containsExactly("HELLO");
  }

}
