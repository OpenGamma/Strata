package com.opengamma.tools.gradle.analytics

import com.opengamma.tools.gradle.git.task.GitClone
import com.opengamma.tools.gradle.release.ReleaseExtension
import com.opengamma.tools.gradle.release.ReleasePlugin
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.SourceTask

class AnalyticsPlugin implements Plugin<Project>
{
    private final static String ANALYTICS_REPO = "git@github.com:OpenGamma/Analytics.git"

    Project project
    Project analytics
    String analyticsRef
    public final static String CLONE_ANALYTICS_TASK_NAME = "cloneAnalytics"
    public final static String STOP_RELEASE_TASK_NAME = "analyticsReleaseCheck"

    @Override
    void apply(Project target)
    {
        this.project = target
        extendAllDependencyHandlers()

        analytics = project.findProject(":analytics")
        if( ! analytics)
            return

        analyticsRef = System.properties['analytics.ref']?.trim()
        if( ! analyticsRef)
            throw new GradleException("Supposed to bootstrap analytics but no ref supplied")

        addStopReleaseTask()
        addCloneAnalyticsTask()
        configureAnalytics()
    }

    private void addCloneAnalyticsTask()
    {
        GitClone t = project.tasks.create(CLONE_ANALYTICS_TASK_NAME, GitClone)
        t.gitBranch = analyticsRef
        t.gitRepoURL = ANALYTICS_REPO
        t.outputDirectory = analytics.projectDir
        t.dependsOn STOP_RELEASE_TASK_NAME
    }

    private void configureAnalytics()
    {
        project.allprojects*.tasks*.withType(SourceTask) { t ->
            t.dependsOn this.project.tasks[CLONE_ANALYTICS_TASK_NAME]
        }
    }

    private void extendDependencyHandler(Project target)
    {
        target.dependencies.ext.ogAnalytics = {
            analytics ?
                    project.project(":analytics") :
                    "com.opengamma.analytics:og-analytics:${target.rootProject.ext.ogAnalyticsVersion}"
        }
    }

    private void extendAllDependencyHandlers()
    {
        project.allprojects.each { extendDependencyHandler(it) }
    }

    private void addStopReleaseTask()
    {
        Task t = project.tasks.create(STOP_RELEASE_TASK_NAME, DefaultTask)
        t.doLast {
            if(project.extensions.getByType(ReleaseExtension).releaseBuild)
                throw new GradleException("Release builds cannot build Analytics")
        }
        t.mustRunAfter ReleasePlugin.CHECK_RELEASE_ENVIRONMENT_TASK_NAME
    }
}
