/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */

package com.opengamma.tools.gradle.release

import com.github.zafarkhaja.semver.Version

class ReleaseExtension
{
    boolean releaseBuild = false
    boolean skipS3 = false
    boolean dryRun = false
    Version releaseVersion
    String releaseTagTemplate = "@version@"
}
