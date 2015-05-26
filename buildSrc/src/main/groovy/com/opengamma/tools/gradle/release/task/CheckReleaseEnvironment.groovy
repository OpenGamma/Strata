package com.opengamma.tools.gradle.release.task

import com.github.zafarkhaja.semver.UnexpectedCharacterException
import com.github.zafarkhaja.semver.Version
import com.monochromeroad.gradle.plugin.aws.s3.S3Sync
import com.opengamma.tools.gradle.distrepo.task.DeployLocal
import com.opengamma.tools.gradle.git.task.GitWriteTask
import com.opengamma.tools.gradle.release.ReleaseExtension
import com.opengamma.tools.gradle.simpleexec.SimpleExec
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Task
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.Upload

import java.text.ParseException

class CheckReleaseEnvironment extends DefaultTask
{
	boolean forceVersions = false
	boolean dryRun = false
	boolean skipS3 = false
	Version releaseVersion
	String gitRef
	List<String> ownerBranches = []

	@TaskAction
	void check()
	{
		configureEnvironment()
		checkRepo()
		checkVersionProgression()
		disableS3IfNecessary()
		disableTasksForDryRunIfNecessary()
		copyConfiguration()
	}

	/**
	 * Derive the name of the tag that we should release into by combining the releaseTag property with token substitution
	 * on the @version@ token
	 * @return - A String containing the fully parsed tag name to release into
	 */
	String getReleaseTagName()
	{
		return project.release.releaseTagTemplate.replaceAll("@version@", releaseVersion.toString())
	}

	private void configureEnvironment()
	{
		String inputReleaseVersion = System.properties["release.version"]
		String inputSkipS3 = System.properties["release.skipS3"]
		String inputDryRun = System.properties["release.dryRun"]
		String inputForceVersions = System.properties["release.forceVersions"]

		if (!inputReleaseVersion)
			throw new GradleException("Property release.version is mandatory for release")

		try
		{
			releaseVersion = Version.valueOf(inputReleaseVersion)
		} catch (final IllegalArgumentException | ParseException | UnexpectedCharacterException ex)
		{
			throw new GradleException("Could not parse version string required for release", ex)
		}

		forceVersions = Boolean.parseBoolean(inputForceVersions)

		skipS3 = Boolean.parseBoolean(inputSkipS3)
		dryRun = Boolean.parseBoolean(inputDryRun)
	}

	private void checkRepo()
	{
		if(System.hasProperty("sha1"))
		{
			gitRef = System.properties['sha1'].trim()
		}
		else
		{
			SimpleExec getCurrentRef = project.tasks.create("getCurrentRef", SimpleExec).configure {
				command = "git rev-parse HEAD"
			}
			getCurrentRef.execute()
			gitRef = getCurrentRef.output.stdOut
		}

		SimpleExec getOwnerBranches = project.tasks.create("checkOwnerBranches", SimpleExec).configure {
			command = "git branch --contains ${gitRef}"
		}
		getOwnerBranches.execute()

		ownerBranches = getOwnerBranches.output.stdOut.readLines().collect {
			it.replaceAll(/(\* | )/, "")
		}
	}

	/**
	 * For a release build, check that the version that is about to be released is sound, and that the version given as the
	 * next development version logically follows on. Also checks for Git tag conflicts.
	 *
	 * This method is designed to be used by a task, and to halt that task when the versions are not sound. Returns void,
	 * but will throw GradleException on error.
	 *
	 * @param releaseVersion - The version that is being released now
	 */
	private void checkVersionProgression()
	{
		SimpleExec getTags = project.tasks.create("listGitTags", SimpleExec).configure {
			command = "git tag -l"
		}
		getTags.execute()
		List<String> tagList = getTags.output.stdOut.readLines()

		if(tagList.contains(releaseTagName))
			throw new GradleException(
					"Release version ${releaseVersion} would release into tag ${releaseTagName} but tag already exists")

		if(releaseVersion.preReleaseVersion == "SNAPSHOT")
			throw new GradleException("Release version must not be a SNAPSHOT version")

		boolean releaseCommitIsSane = ownerBranches.any {
			it == "master" || it.startsWith("dev/")
		}

		if( ! releaseCommitIsSane && ! forceVersions)
			throw new GradleException("Release ref ${gitRef} does not seem to be a suitable source for a " +
					"release; there is no parent commit that is either on the master branch or a fixes branch. " +
					"Use the forceVersions flag if you really want to release this commit.")
	}

	private void disableS3IfNecessary()
	{
		if( ! skipS3) return

		project.allprojects*.tasks*.withType(S3Sync) {
			it.enabled = false
		}
	}

	private void disableTasksForDryRunIfNecessary()
	{
		if( ! dryRun) return

		[S3Sync, Upload, GitWriteTask].each { Class<? extends DefaultTask> t ->
			project.allprojects*.tasks*.withType(t) { Task it ->
				if(it instanceof DeployLocal) return
				it.enabled = false
			}
		}
	}

	private void copyConfiguration()
	{
		project.extensions.findByType(ReleaseExtension)?.with {
			releaseBuild = true
			dryRun = this.dryRun
			skipS3 = this.skipS3
			releaseVersion = this.releaseVersion
//			releaseTagName = this.releaseTagName
		}
	}
}
