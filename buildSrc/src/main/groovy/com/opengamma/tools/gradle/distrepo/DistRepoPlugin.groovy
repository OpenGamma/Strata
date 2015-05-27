package com.opengamma.tools.gradle.distrepo

import com.opengamma.tools.gradle.distrepo.task.DeployLocal
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.internal.plugins.DslObject
import org.gradle.api.plugins.MavenPlugin
import org.gradle.api.plugins.MavenRepositoryHandlerConvention

class DistRepoPlugin implements Plugin<Project>
{
    public final static String DIST_LOCAL_TASK_NAME = "deployLocal"
    public final static String EXTENSION_NAME = "distRepo"

    Project project

    @Override
    void apply(Project target)
    {
        this.project = target

        applyMavenPlugin()
        addExtension()
        addUploadTask()
    }

    private void applyMavenPlugin()
    {
        project.plugins.apply(MavenPlugin)
    }

    private void addExtension()
    {
        project.extensions.create(EXTENSION_NAME, DistRepoExtension)
    }

    private void addUploadTask()
    {
        createUploadTask()
        project.afterEvaluate this.&configureUploadTask
    }

    private void createUploadTask()
    {
        project.tasks.create(DIST_LOCAL_TASK_NAME, DeployLocal)
    }

    private void configureUploadTask()
    {
        DeployLocal t = project.tasks[DIST_LOCAL_TASK_NAME]

        Project deployInto = project.extensions.getByType(DistRepoExtension).deployInto ?: project
        String repoDirectoryName = project.extensions.getByType(DistRepoExtension).repoDirectoryName
        File deployIntoDir = new File(deployInto.buildDir, repoDirectoryName).canonicalFile
        final String m2RepoURL = "file://${deployIntoDir}"

        t.deployIntoDir = deployIntoDir
        t.configuration = project.configurations.getByName(Dependency.ARCHIVES_CONFIGURATION)

        MavenRepositoryHandlerConvention repositories =
                new DslObject(t.repositories).convention.getPlugin(MavenRepositoryHandlerConvention.class)

        repositories.mavenDeployer() {
            repository(url: m2RepoURL)
            pom.project project.defaultPOM
        }
    }
}
