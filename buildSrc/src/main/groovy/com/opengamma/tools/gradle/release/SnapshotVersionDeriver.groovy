/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */

package com.opengamma.tools.gradle.release

import com.github.zafarkhaja.semver.Version
import groovy.transform.Immutable

@Immutable
class SnapshotVersionDeriver
{
    String baseTagDescription
    String commitDescription
    String releaseTagTemplate
    Integer buildNumber

    /**
     * TODO - This doc copied from original OGM implementation - needs updating
     *
     * <pre>
     * Derives the current SNAPSHOT version by examining the name of the last release tag, and the distance between the
     * tag and the current commit. The NormalVersion is set to the the version of the last tag (the "base version") and
     * the BuildMetadata is composed of the number of commits since the tag, and the Git object reference representing the
     * current commit.
     *
     * Returns a Version object which will be rendered in the format Maj.Min.Patch(-Pre)+Commit.gSha, which is correct to
     * Semantic Versioning v2.0.0 (Spec at <a href="http://www.semver.org/">SemVer.org</a>)
     *
     * If the last release tag was rel/OGM-1.0.0, there have been 6 commits since the tag, and the current commit sha is
     * 123abc, then the snapshot version will be 1.0.0+6.g123abc
     *
     * Pre-release information in the base version is handled properly, and this will be included in the derived version
     * (e.g. 1.1.0-alpha2+8.g234bcd )
     *
     * @return - a Version object populated with the base version plus derived build metadata
     */
    public Version deriveSnapshot() // TODO - Refactor this swamp
    {
        def (String tagVersionPrefix, String tagVersionSuffix) = releaseTagTemplate.split("@version@", 2)
        String rawMetadata = commitDescription.replaceFirst("${baseTagDescription}", "")
	    if(rawMetadata.startsWith("-"))
		    rawMetadata = rawMetadata.replaceFirst("-", "")
        def (String commitCount, String objectName) = rawMetadata ? rawMetadata.split("-", 2) : ["", ""]
	    if(commitCount)
		    commitCount += "."
        String tagVersion = baseTagDescription.replaceFirst(tagVersionPrefix, "")
        tagVersion = tagVersion - tagVersionSuffix
        Version baseVersion = Version.valueOf(tagVersion)
	    String build = null != buildNumber ? "b${buildNumber}" : ""
	    if(objectName && build)
		    objectName += "."
        Version snapshotVersion = new Version.Builder().
                setNormalVersion(baseVersion.normalVersion).
                setPreReleaseVersion(baseVersion.preReleaseVersion).
                setBuildMetadata("${commitCount}${objectName}${build}").
                build()

        return snapshotVersion
    }
}
