/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.plugins.clean;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;

import static org.apache.commons.io.FileUtils.copyDirectory;
import static org.codehaus.plexus.util.IOUtil.copy;

/**
 * Test the clean mojo.
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 */
public class CleanMojoTest extends AbstractMojoTestCase {
    /**
     * Tests the simple removal of directories
     *
     * @throws Exception in case of an error.
     */
    public void testBasicClean() throws Exception {
        String pluginPom = getBasedir() + "/src/test/resources/unit/basic-clean-test/plugin-pom.xml";

        // safety
        copyDirectory(
                new File(getBasedir(), "src/test/resources/unit/basic-clean-test"),
                new File(getBasedir(), "target/test-classes/unit/basic-clean-test"));

        CleanMojo mojo = (CleanMojo) lookupMojo("clean", pluginPom);
        assertNotNull(mojo);

        mojo.execute();

        assertFalse(
                "Directory exists",
                checkExists(getBasedir() + "/target/test-classes/unit/" + "basic-clean-test/buildDirectory"));
        assertFalse(
                "Directory exists",
                checkExists(getBasedir() + "/target/test-classes/unit/basic-clean-test/" + "buildOutputDirectory"));
        assertFalse(
                "Directory exists",
                checkExists(getBasedir() + "/target/test-classes/unit/basic-clean-test/" + "buildTestDirectory"));
    }

    /**
     * Tests the removal of files and nested directories
     *
     * @throws Exception in case of an error.
     */
    public void testCleanNestedStructure() throws Exception {
        String pluginPom = getBasedir() + "/src/test/resources/unit/nested-clean-test/plugin-pom.xml";

        // safety
        copyDirectory(
                new File(getBasedir(), "src/test/resources/unit/nested-clean-test"),
                new File(getBasedir(), "target/test-classes/unit/nested-clean-test"));

        CleanMojo mojo = (CleanMojo) lookupMojo("clean", pluginPom);
        assertNotNull(mojo);

        mojo.execute();

        assertFalse(checkExists(getBasedir() + "/target/test-classes/unit/nested-clean-test/target"));
        assertFalse(checkExists(getBasedir() + "/target/test-classes/unit/nested-clean-test/target/classes"));
        assertFalse(checkExists(getBasedir() + "/target/test-classes/unit/nested-clean-test/target/test-classes"));
    }

    /**
     * Tests that no exception is thrown when all internal variables are empty and that it doesn't
     * just remove whats there
     *
     * @throws Exception in case of an error.
     */
    public void testCleanEmptyDirectories() throws Exception {
        String pluginPom = getBasedir() + "/src/test/resources/unit/empty-clean-test/plugin-pom.xml";

        // safety
        copyDirectory(
                new File(getBasedir(), "src/test/resources/unit/empty-clean-test"),
                new File(getBasedir(), "target/test-classes/unit/empty-clean-test"));

        CleanMojo mojo = (CleanMojo) lookupEmptyMojo("clean", pluginPom);
        assertNotNull(mojo);

        mojo.execute();

        assertTrue(checkExists(getBasedir() + "/target/test-classes/unit/empty-clean-test/testDirectoryStructure"));
        assertTrue(checkExists(
                getBasedir() + "/target/test-classes/unit/empty-clean-test/" + "testDirectoryStructure/file.txt"));
        assertTrue(checkExists(getBasedir() + "/target/test-classes/unit/empty-clean-test/"
                + "testDirectoryStructure/outputDirectory"));
        assertTrue(checkExists(getBasedir() + "/target/test-classes/unit/empty-clean-test/"
                + "testDirectoryStructure/outputDirectory/file.txt"));
    }

    /**
     * Tests the removal of files using fileset
     *
     * @throws Exception in case of an error.
     */
    public void testFilesetsClean() throws Exception {
        String pluginPom = getBasedir() + "/src/test/resources/unit/fileset-clean-test/plugin-pom.xml";

        // safety
        copyDirectory(
                new File(getBasedir(), "src/test/resources/unit/fileset-clean-test"),
                new File(getBasedir(), "target/test-classes/unit/fileset-clean-test"));

        CleanMojo mojo = (CleanMojo) lookupMojo("clean", pluginPom);
        assertNotNull(mojo);

        mojo.execute();

        // fileset 1
        assertTrue(checkExists(getBasedir() + "/target/test-classes/unit/fileset-clean-test/target"));
        assertTrue(checkExists(getBasedir() + "/target/test-classes/unit/fileset-clean-test/target/classes"));
        assertFalse(checkExists(getBasedir() + "/target/test-classes/unit/fileset-clean-test/target/test-classes"));
        assertTrue(checkExists(getBasedir() + "/target/test-classes/unit/fileset-clean-test/target/subdir"));
        assertFalse(checkExists(getBasedir() + "/target/test-classes/unit/fileset-clean-test/target/classes/file.txt"));
        assertTrue(checkEmpty(getBasedir() + "/target/test-classes/unit/fileset-clean-test/target/classes"));
        assertFalse(checkEmpty(getBasedir() + "/target/test-classes/unit/fileset-clean-test/target/subdir"));
        assertTrue(checkExists(getBasedir() + "/target/test-classes/unit/fileset-clean-test/target/subdir/file.txt"));

        // fileset 2
        assertTrue(
                checkExists(getBasedir() + "/target/test-classes/unit/fileset-clean-test/" + "buildOutputDirectory"));
        assertFalse(checkExists(
                getBasedir() + "/target/test-classes/unit/fileset-clean-test/" + "buildOutputDirectory/file.txt"));
    }

    /**
     * Tests the removal of a directory as file
     *
     * @throws Exception in case of an error.
     */
    public void testCleanInvalidDirectory() throws Exception {
        String pluginPom = getBasedir() + "/src/test/resources/unit/invalid-directory-test/plugin-pom.xml";

        // safety
        copyDirectory(
                new File(getBasedir(), "src/test/resources/unit/invalid-directory-test"),
                new File(getBasedir(), "target/test-classes/unit/invalid-directory-test"));

        CleanMojo mojo = (CleanMojo) lookupMojo("clean", pluginPom);
        assertNotNull(mojo);

        try {
            mojo.execute();

            fail("Should fail to delete a file treated as a directory");
        } catch (MojoExecutionException expected) {
            assertTrue(true);
        }
    }

    /**
     * Tests the removal of a missing directory
     *
     * @throws Exception in case of an error.
     */
    public void testMissingDirectory() throws Exception {
        String pluginPom = getBasedir() + "/src/test/resources/unit/missing-directory-test/plugin-pom.xml";

        // safety
        copyDirectory(
                new File(getBasedir(), "src/test/resources/unit/missing-directory-test"),
                new File(getBasedir(), "target/test-classes/unit/missing-directory-test"));

        CleanMojo mojo = (CleanMojo) lookupMojo("clean", pluginPom);
        assertNotNull(mojo);

        mojo.execute();

        assertFalse(checkExists(getBasedir() + "/target/test-classes/unit/missing-directory-test/does-not-exist"));
    }

    /**
     * Test the removal of a locked file on Windows systems.
     * <p>
     * Note: Unix systems doesn't lock any files.
     * </p>
     *
     * @throws Exception in case of an error.
     */
    public void testCleanLockedFile() throws Exception {
        if (!System.getProperty("os.name").toLowerCase().contains("windows")) {
            assertTrue("Ignored this test on non Windows based systems", true);
            return;
        }

        String pluginPom = getBasedir() + "/src/test/resources/unit/locked-file-test/plugin-pom.xml";

        // safety
        copyDirectory(
                new File(getBasedir(), "src/test/resources/unit/locked-file-test"),
                new File(getBasedir(), "target/test-classes/unit/locked-file-test"));

        CleanMojo mojo = (CleanMojo) lookupMojo("clean", pluginPom);
        assertNotNull(mojo);

        File f = new File(getBasedir(), "target/test-classes/unit/locked-file-test/buildDirectory/file.txt");
        try (FileChannel channel = new RandomAccessFile(f, "rw").getChannel();
                FileLock ignored = channel.lock()) {
            mojo.execute();
            fail("Should fail to delete a file that is locked");
        } catch (MojoExecutionException expected) {
            assertTrue(true);
        }
    }

    /**
     * Test the removal of a locked file on Windows systems.
     * <p>
     * Note: Unix systems doesn't lock any files.
     * </p>
     *
     * @throws Exception in case of an error.
     */
    public void testCleanLockedFileWithNoError() throws Exception {
        if (!System.getProperty("os.name").toLowerCase().contains("windows")) {
            assertTrue("Ignore this test on non Windows based systems", true);
            return;
        }

        String pluginPom = getBasedir() + "/src/test/resources/unit/locked-file-test/plugin-pom.xml";

        // safety
        copyDirectory(
                new File(getBasedir(), "src/test/resources/unit/locked-file-test"),
                new File(getBasedir(), "target/test-classes/unit/locked-file-test"));

        CleanMojo mojo = (CleanMojo) lookupMojo("clean", pluginPom);
        setVariableValueToObject(mojo, "failOnError", Boolean.FALSE);
        assertNotNull(mojo);

        File f = new File(getBasedir(), "target/test-classes/unit/locked-file-test/buildDirectory/file.txt");
        try (FileChannel channel = new RandomAccessFile(f, "rw").getChannel();
                FileLock ignored = channel.lock()) {
            mojo.execute();
            assertTrue(true);
        } catch (MojoExecutionException expected) {
            fail("Should display a warning when deleting a file that is locked");
        }
    }

    /**
     * Test the followLink option with windows junctions
     *
     * @throws Exception
     */
    public void testFollowLinksWithWindowsJunction() throws Exception {
        if (!System.getProperty("os.name").toLowerCase().contains("windows")) {
            assertTrue("Ignore this test on non Windows based systems", true);
            return;
        }

        testSymlink((link, target) -> {
            Process process = new ProcessBuilder()
                    .directory(link.getParent().toFile())
                    .command("cmd", "/c", "mklink", "/j", link.getFileName().toString(), target.toString())
                    .start();
            process.waitFor();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            copy(process.getInputStream(), baos);
            copy(process.getErrorStream(), baos);
            if (!Files.exists(link)) {
                throw new IOException("Unable to create junction: " + baos);
            }
        });
    }

    /**
     * Test the followLink option with sym link
     *
     * @throws Exception
     */
    public void testFollowLinksWithSymLinkOnPosix() throws Exception {
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            assertTrue("Ignore this test on Windows based systems", true);
            return;
        }

        testSymlink((link, target) -> {
            try {
                Files.createSymbolicLink(link, target);
            } catch (IOException e) {
                throw new IOException("Unable to create symbolic link", e);
            }
        });
    }

    private void testSymlink(LinkCreator linkCreator) throws Exception {
        // We use the SystemStreamLog() as the AbstractMojo class, because from there the Log is always provided
        Cleaner cleaner = new Cleaner(null, new SystemStreamLog(), false, null, null);
        Path testDir = Paths.get("target/test-classes/unit/test-dir").toAbsolutePath();
        Path dirWithLnk = testDir.resolve("dir");
        Path orgDir = testDir.resolve("org-dir");
        Path jctDir = dirWithLnk.resolve("jct-dir");
        Path file = orgDir.resolve("file.txt");

        // create directories, links and file
        Files.createDirectories(dirWithLnk);
        Files.createDirectories(orgDir);
        Files.write(file, Collections.singleton("Hello world"));
        linkCreator.createLink(jctDir, orgDir);
        // delete
        cleaner.delete(dirWithLnk.toFile(), null, false, true, false);
        // verify
        assertTrue(Files.exists(file));
        assertFalse(Files.exists(jctDir));
        assertTrue(Files.exists(orgDir));
        assertFalse(Files.exists(dirWithLnk));

        // create directories, links and file
        Files.createDirectories(dirWithLnk);
        Files.createDirectories(orgDir);
        Files.write(file, Collections.singleton("Hello world"));
        linkCreator.createLink(jctDir, orgDir);
        // delete
        cleaner.delete(dirWithLnk.toFile(), null, true, true, false);
        // verify
        assertFalse(Files.exists(file));
        assertFalse(Files.exists(jctDir));
        assertTrue(Files.exists(orgDir));
        assertFalse(Files.exists(dirWithLnk));
    }

    /**
     * @param dir a dir or a file
     * @return true if a file/dir exists, false otherwise
     */
    private boolean checkExists(String dir) {
        return new File(new File(dir).getAbsolutePath()).exists();
    }

    /**
     * @param dir a directory
     * @return true if a dir is empty, false otherwise
     */
    private boolean checkEmpty(String dir) {
        File[] files = new File(dir).listFiles();
        return files == null || files.length == 0;
    }

    @FunctionalInterface
    interface LinkCreator {
        void createLink(Path link, Path target) throws Exception;
    }
}
