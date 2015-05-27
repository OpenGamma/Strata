package com.opengamma.tools.gradle.release

import com.github.zafarkhaja.semver.Version

class AutoVersionExtension
{
    /**
     * Whether or not to include the commit count as part of this snapshot's metadata.
     * This commit count is the number of commits since the last tag.
     */
    boolean includeCommitcount = true

    /**
     * Whether or not to include the Git object name as part of this snapshot's metadata.
     * The Git object name is sha (in short form) prefixed with 'g'.
     */
    boolean includeObjectName = true

    /**
     * Whether or not to include the build number (if available) as part of this snapshot's metadata.
     * The build number is sourced from a Jenkins environment variable, and will not be included if unavailable.
     */
    boolean includeBuildNumber = true

    Version fullVersion
}
