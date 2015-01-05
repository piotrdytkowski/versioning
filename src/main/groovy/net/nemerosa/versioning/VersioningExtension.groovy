package net.nemerosa.versioning

import net.nemerosa.versioning.git.GitInfoService
import org.gradle.api.GradleException
import org.gradle.api.Project

class VersioningExtension {

    /**
     * Registry of SCM info services
     */
    private static final Map<String, GitInfoService> INFO_SERVICES = [
            'git': new GitInfoService(),
            // TODO SVN
    ]

    /**
     * Version SCM - git by default
     */
    String scm = 'git'

    /**
     * Getting the version type from a branch. Default: getting the part before the first "/". If no slash is found,
     * takes the branch name as whole.
     *
     * For example:
     *
     * * release/2.0 --> release
     * * feature/2.0 --> feature
     * * master --> master
     */
    Closure<String> branchType = { String branch ->
        int pos = branch.indexOf('/')
        if (pos > 0) {
            branch.substring(0, pos)
        } else {
            branch
        }
    }

    /**
     * Getting the version base from a branch. Default: getting the part after the first "/". If no slash is found,
     * returns empty.
     *
     * For example:
     *
     * * release/2.0 --> 2.0
     * * feature/2.0 --> 2.0
     * * master --> ''
     */
    Closure<String> base = { String branch ->
        int pos = branch.indexOf('/')
        if (pos > 0) {
            branch.substring(pos + 1)
        } else {
            ''
        }
    }

    /**
     * Computes the full version.
     */
    Closure<String> full = { branchId, abbreviated -> "${branchId}-${abbreviated}" }

    /**
     * Set of eligible branch types for computing a display version from the branch base name
     */
    Set<String> releases = [ 'release' ] as Set<String>

    /**
     * Computed version information
     */
    private VersionInfo info

    /**
     * Linked project
     */
    private final Project project

    /**
     * Constructor
     * @param project Linked project
     */
    VersioningExtension(Project project) {
        this.project = project
    }
/**
 * Gets the computed version information
 */
    VersionInfo getInfo() {
        if (!info) {
            info = computeInfo()
        }
        info
    }

    /**
     * Computes the version information.
     */
    VersionInfo computeInfo() {

        // Gets the SCM info service
        SCMInfoService scmInfoService = getSCMInfoService(scm)
        // Gets the version source
        SCMInfo scmInfo = scmInfoService.getInfo(project, this)

        // Version source
        String versionBranch = scmInfo.branch

        // Source type
        String versionBranchType = branchType(versionBranch)
        String versionBase = base(versionBranch)

        // Branch identifier
        String versionBranchId = normalise(versionBranch)

        // Full version
        String versionFull = full(versionBranchId, scmInfo)

        // Display version
        String versionDisplay
        if (versionBranchType in releases) {
            List<String> baseTags = scmInfoService.getBaseTags(project, this, versionBase)
            versionDisplay = getDisplayVersion(versionBase, baseTags)
        } else {
            versionDisplay = versionBranchId
        }

        // OK
        new VersionInfo(
                scm: scm,
                branch: versionBranch,
                branchType: versionBranchType,
                branchId: versionBranchId,
                full: versionFull,
                base: versionBase,
                display: versionDisplay,
        )
    }

    private static String getDisplayVersion(String base, List<String> baseTags) {
        if (baseTags.empty) {
            return "${base}.0"
        } else {
            def lastTag = baseTags[0].trim()
            def lastNumber = (lastTag =~ /${base}\.([\d+])/)[0][1] as int
            def newNumber = lastNumber + 1
            return "${base}.${newNumber}"
        }
    }

    private static String normalise(String value) {
        value.replaceAll(/[^A-Za-z0-9\.\-_]/, '-')
    }

    private static SCMInfoService getSCMInfoService(String type) {
        SCMInfoService scmInfoService = INFO_SERVICES[type]
        if (scmInfoService) {
            return scmInfoService
        } else {
            throw new GradleException("Unknown SCM info service: ${type}")
        }
    }
}