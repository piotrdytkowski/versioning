package net.nemerosa.versioning.core;

import org.apache.commons.lang3.StringUtils;

/**
 * Snapshot release mode
 */
public class SnapshotReleaseMode implements ReleaseMode {

    public static final ReleaseMode INSTANCE = new SnapshotReleaseMode();

    @Override
    public String getDisplayVersion(String nextTag, String lastTag, String currentTag, VersioningConfig config) {
        return StringUtils.isNotBlank(currentTag) ? String.format("%s%s", nextTag, config.getSnapshot()) : currentTag;
    }

}
