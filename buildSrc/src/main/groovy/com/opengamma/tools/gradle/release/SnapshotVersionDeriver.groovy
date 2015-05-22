/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */

package com.opengamma.tools.gradle.release

import com.github.zafarkhaja.semver.Version

class SnapshotVersionDeriver
{
    private static final String VERSION_TOKEN = "@version@"

    private final String baseTagDescription
    private final String commitDescription
    private final String releaseTagTemplate
    private final Integer buildNumber

    private final String tagVersionPrefix
    private final String tagVersionSuffix

    private final boolean includeCommitCount
    private final boolean includeGitObjectName
    private final boolean includeBuild

    private String rawMetadata
    private String commitCount
    private String gitObjectName
    private String tagVersion
    private String build

    SnapshotVersionDeriver(
            String baseTagDescription,
            String commitDescription,
            String releaseTagTemplate,
            Integer buildNumber,
            boolean includeCommitCount = true,
            boolean includeGitObjectName = true,
            boolean includeBuild = true)
    {
        this.baseTagDescription = baseTagDescription
        this.commitDescription = commitDescription
        this.releaseTagTemplate = releaseTagTemplate
        this.buildNumber = buildNumber
        this.includeCommitCount = includeCommitCount
        this.includeGitObjectName = includeGitObjectName
        this.includeBuild = includeBuild

        (tagVersionPrefix, tagVersionSuffix) = releaseTagTemplate.split(VERSION_TOKEN, 2)
    }

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
    public Version deriveSnapshot()
    {
        parseGitMetadata()
        extractCommitCountAndObjectName()
        extractLastTaggedVersion()
	    extractBuildMetadata()

        Version baseVersion = Version.valueOf(tagVersion)
        Version snapshotVersion = new Version.Builder().
                setNormalVersion(baseVersion.normalVersion).
                setPreReleaseVersion(baseVersion.preReleaseVersion).
                setBuildMetadata(versionMetadata).
                build()

        return snapshotVersion
    }

    private void parseGitMetadata()
    {
        rawMetadata = commitDescription.replaceFirst("${baseTagDescription}", "")
        if(this.rawMetadata.startsWith("-"))
            this.rawMetadata = this.rawMetadata.replaceFirst("-", "")
    }

    private void extractCommitCountAndObjectName()
    {
        (commitCount, gitObjectName) = this.rawMetadata ? this.rawMetadata.split("-", 2) : ["", ""]
    }

    private void extractLastTaggedVersion()
    {
        String tagVersion = baseTagDescription.replaceFirst(tagVersionPrefix, "")
        tagVersion = tagVersion - tagVersionSuffix
        this.tagVersion = tagVersion
    }

    private void extractBuildMetadata()
    {
        build = null != buildNumber ? "b${buildNumber}" : ""
    }

    private String getVersionMetadata()
    {
        List<String> metadata = []

        if(commitCount && includeCommitCount)
            metadata << commitCount

        if(gitObjectName && includeGitObjectName)
            metadata << gitObjectName

        if(build && includeBuild)
            metadata << build

        return metadata.join(".")
    }
}
