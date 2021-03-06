/*
 * CDDL HEADER START
 *
 * The contents of this file are subject to the terms of the
 * Common Development and Distribution License (the "License").
 * You may not use this file except in compliance with the License.
 *
 * See LICENSE.txt included in this distribution for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL HEADER in each
 * file and include the License file at LICENSE.txt.
 * If applicable, add the following below this CDDL HEADER, with the
 * fields enclosed by brackets "[]" replaced with your own identifying
 * information: Portions Copyright [yyyy] [name of copyright owner]
 *
 * CDDL HEADER END
 */

/*
 * Copyright (c) 2008, 2017, Oracle and/or its affiliates. All rights reserved.
 * Portions Copyright (c) 2017, Chris Fraire <cfraire@me.com>.
 */
package org.opensolaris.opengrok.history;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.opensolaris.opengrok.configuration.RuntimeEnvironment;
import org.opensolaris.opengrok.logger.LoggerFactory;
import org.opensolaris.opengrok.util.ClassUtil;
import org.opensolaris.opengrok.util.PathUtils;

/**
 * Class to contain the common info for a repository. This object will live on
 * the server and the client side, so don't add logic that will only work on one
 * side in this object.
 *
 * @author Trond Norbye
 */
public class RepositoryInfo implements Serializable {

    private static final Logger LOGGER =
        LoggerFactory.getLogger(RepositoryInfo.class);

    static {
        ClassUtil.remarkTransientFields(RepositoryInfo.class);
    }

    private static final long serialVersionUID = 3L;

    // dummy to avoid storing absolute path in XML encoded configuration
    // Do not use this member.
    private transient String directoryName;
    
    private String directoryNameRelative;
    protected Boolean working;
    protected String type;
    protected boolean remote;
    protected String[] datePatterns = new String[0];
    protected String parent;
    protected String branch;
    protected String currentVersion;

    /**
     * format used for printing the date in {@code currentVersion}
     */
    protected static final SimpleDateFormat outputDateFormat = new SimpleDateFormat("YYYY-MM-dd HH:mm Z");

    /**
     * Empty constructor to support serialization.
     */
    public RepositoryInfo() {
        super();
    }

    public RepositoryInfo(RepositoryInfo orig) {
        this.directoryNameRelative = orig.directoryNameRelative;
        this.type = orig.type;
        this.working = orig.isWorking();
        this.remote = orig.isRemote();
        this.datePatterns = orig.datePatterns;
        this.parent = orig.parent;
        this.branch = orig.branch;
        this.currentVersion = orig.currentVersion;
    }

    /**
     * @return relative path to source root
     */
    public String getDirectoryNameRelative() {
        return directoryNameRelative;
    }

    /**
     * Set relative path to source root
     * @param dir directory
     */
    public void setDirectoryNameRelative(String dir) {
        this.directoryNameRelative = dir;
    }

    /**
     * Get the name of the root directory for this repository.
     *
     * @return the name of the root directory
     */
    public String getDirectoryName() {
        return RuntimeEnvironment.getInstance().getSourceRootPath() +
                directoryNameRelative;
    }

    /**
     * Specify the name of the root directory for this repository.
     *
     * @param dir the new name of the root directory. Can be absolute
     * path or relative to source root.
     */
    public void setDirectoryName(File dir) {
        RuntimeEnvironment env = RuntimeEnvironment.getInstance();
        String rootPath = env.getSourceRootPath();
        String path;
        String originalPath = dir.getPath();
        try {
            path = PathUtils.getRelativeToCanonical(originalPath, rootPath);
            // N.b. OpenGrok has a weird convention that
            // `directoryNameRelative' must start with a '/', as it is
            // elsewhere directly appended to env.getSourceRootPath() and
            // also stored as such.
            if (!path.equals(originalPath)) path = File.separator + path;
        } catch (IOException e) {
            path = originalPath;
            LOGGER.log(Level.SEVERE, String.format(
                "Failed to get canonical path for {0}", path), e);
        }

        if (path.startsWith(rootPath)) {
            this.directoryNameRelative = path.substring(rootPath.length());
        } else {
            this.directoryNameRelative = path;
        }
    }

    /**
     * Returns true if this repository is usable in this context (for SCM
     * systems that use external binaries, the binary must be available etc)
     *
     * @return true if the HistoryGuru may use the repository
     */
    public boolean isWorking() {
        return working != null && working;
    }

    /**
     * Set the property working
     *
     * @param working is repository working
     */
    public void setWorking(Boolean working) {
        this.working = working;
    }

    /**
     * Is the history and version information for this repository stored on a
     * remote server?
     *
     * @return true if the history is stored on a remote server.
     */
    public boolean isRemote() {
        return remote;
    }

    /**
     * Set the property remote
     *
     * @param remote is remote repository
     */
    public void setRemote(boolean remote) {
        this.remote = remote;
    }

    /**
     * get property type
     *
     * @return type
     */
    public String getType() {
        return type;
    }

    /**
     * Set property type
     *
     * @param type repository type
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * get property parent
     *
     * @return parent
     */
    public String getParent() {
        return parent;
    }

    /**
     * Set property parent
     *
     * @param parent parent of the repository
     */
    public void setParent(String parent) {
        this.parent = parent;
    }

    public void setDatePatterns(String[] datePatterns) {
        this.datePatterns = datePatterns;
    }

    public String[] getDatePatterns() {
        return datePatterns;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getCurrentVersion() {
        return currentVersion;
    }

    public void setCurrentVersion(String currentVersion) {
        this.currentVersion = currentVersion;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof RepositoryInfo)) {
            return false;
        }
        
        RepositoryInfo ri = (RepositoryInfo) obj;
        
        // Directory paths should be unique.
        if (ri.getDirectoryNameRelative() != null && this.getDirectoryNameRelative() != null) {
            return ri.getDirectoryNameRelative().equals(this.getDirectoryNameRelative());
        } else {
            return (ri.getDirectoryNameRelative() == null && this.getDirectoryNameRelative() == null);
        }
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 89 * hash + Objects.hashCode(this.directoryNameRelative);
        return hash;
    }
}
