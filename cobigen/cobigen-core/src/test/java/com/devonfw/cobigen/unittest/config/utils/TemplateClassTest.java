package com.devonfw.cobigen.unittest.config.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.devonfw.cobigen.impl.config.ConfigurationHolder;
import com.devonfw.cobigen.impl.config.entity.TemplatePath;
import com.devonfw.cobigen.impl.util.ConfigurationClassLoaderUtil;
import com.devonfw.cobigen.unittest.config.common.AbstractUnitTest;

/**
 * Test of {@link TemplatePath} and its sub-classes.
 */
public class TemplateClassTest extends AbstractUnitTest {

  @Rule
  public TemporaryFolder tmpFolder = new TemporaryFolder();

  /** Root path of the test resources */
  private static final String TEST_FILES_ROOT_PATH = "src/test/resources/testdata/unittest/config/entity/TemplateClassTest/";
  // TODO add test for Sets

  /**
   * Tests if the template utility classes can be loaded from a configuration folder
   *
   * @throws IOException test fails
   */
  @Test
  public void testResolveUtilClassesFromTemplatesFolder() throws IOException {

    String filename = "folder";
    Path path = Paths.get(TEST_FILES_ROOT_PATH + filename);

    List<Class<?>> classes = ConfigurationClassLoaderUtil.resolveUtilClasses(new ConfigurationHolder(path.toUri()),
        null);
    assertThat(classes.get(0).getName()).contains("IDGenerator");
  }

  /**
   * Tests if the template utility classes can be loaded from a configuration folder
   *
   * @throws IOException test fails
   */
  @Test
  public void testResolveUtilClassesFromTemplateSetFolder() throws IOException {

    String filename = "TemplateSetfolder";
    Path path = Paths.get(TEST_FILES_ROOT_PATH + filename);

    List<Class<?>> classes = ConfigurationClassLoaderUtil.resolveUtilClasses(new ConfigurationHolder(path.toUri()),
        null);
    assertThat(classes.size()).isEqualTo(2);
    // assertThat(classes.get(0).getName()).contains("JavaUtil");
  }

  /**
   * Tests if the template utility classes can be loaded from a jar archive
   *
   * @throws IOException test fails
   */
  @Test
  public void testResolveUtilClassesFromJarArchive() throws IOException {

    String filename = "archive";

    Path path = Paths.get(TEST_FILES_ROOT_PATH + filename + File.separator + "templates.jar");

    ClassLoader inputClassLoader = URLClassLoader.newInstance(new URL[] { path.toUri().toURL() },
        getClass().getClassLoader());

    List<Class<?>> classes = ConfigurationClassLoaderUtil.resolveUtilClasses(new ConfigurationHolder(path.toUri()),
        inputClassLoader);
    assertThat(classes).isNotEmpty();
    assertThat(classes.get(0).getName()).contains("IDGenerator");
  }

  /**
   * Tests if the template utility classes get loaded from a folder when both sources are available
   *
   * @throws IOException test fails
   */
  @Test
  public void testResolveUtilClassesFromFolderFirst() throws IOException {

    Path pathArchive = Paths.get(TEST_FILES_ROOT_PATH + "archive" + File.separator + "templates.jar");
    Path pathFolder = Paths.get(TEST_FILES_ROOT_PATH + "folder");

    ClassLoader inputClassLoader = URLClassLoader.newInstance(
        new URL[] { pathArchive.toUri().toURL(), pathFolder.toUri().toURL() }, getClass().getClassLoader());

    List<Class<?>> classes = ConfigurationClassLoaderUtil
        .resolveUtilClasses(new ConfigurationHolder(pathFolder.toUri()), inputClassLoader);
    assertThat(classes.get(0).getName()).contains("IDGenerator");
  }

  /**
   * Todo
   *
   * @throws IOException test fails
   */
  @Test
  public void testGetTemplateSetConfigurationFromFolder() throws Exception {

    String filename = "TemplateSetfolder";

    Path path = Paths.get(TEST_FILES_ROOT_PATH + filename);

    ClassLoader inputClassLoader = URLClassLoader.newInstance(new URL[] { path.toUri().toURL() },
        getClass().getClassLoader());

    // URL url = ConfigurationClassLoaderUtil.getContextConfiguration(inputClassLoader);
    List<URL> url = ConfigurationClassLoaderUtil.getTemplateSetConfiguration(inputClassLoader);
    assertThat(url).isNotNull();
    assertThat(url).doesNotContainNull();
    assertThat(url).size().isEqualTo(2);
  }

  /**
   * Todo
   *
   * @throws IOException test fails
   */
  @Test
  public void testGetTemplateSetConfigurationFromArchive() throws Exception {

    File downloaded = this.tmpFolder.newFolder("downloaded");
    // TODO den download von den jars
    String filename = "archive";
    // TODO get jars from templates and copy into a new folder. use at least 2 than use this function

    Path path = Paths.get(TEST_FILES_ROOT_PATH + filename);

    ClassLoader inputClassLoader = URLClassLoader.newInstance(new URL[] { path.toUri().toURL() },
        getClass().getClassLoader());
    List<URL> url = ConfigurationClassLoaderUtil.getTemplateSetConfiguration(inputClassLoader);
    assertThat(url).isNotNull();
  }

  /**
   * Tests if the context.xml can be detected from templates folder
   *
   * @throws IOException test fails
   */
  @Test
  public void testGetContextConfigurationFromFolder() throws IOException {

    String filename = "folder";

    Path path = Paths.get(TEST_FILES_ROOT_PATH + filename);

    ClassLoader inputClassLoader = URLClassLoader.newInstance(new URL[] { path.toUri().toURL() },
        getClass().getClassLoader());

    URL url = ConfigurationClassLoaderUtil.getContextConfiguration(inputClassLoader);
    assertThat(url).isNotNull();
  }

  /**
   * Tests if the context.xml can be detected from templates archive jar file
   *
   * @throws IOException test fails
   */
  @Test
  public void testGetContextConfigurationFromArchive() throws IOException {

    String filename = "archive";

    Path path = Paths.get(TEST_FILES_ROOT_PATH + filename + File.separator + "templates.jar");

    ClassLoader inputClassLoader = URLClassLoader.newInstance(new URL[] { path.toUri().toURL() },
        getClass().getClassLoader());

    URL url = ConfigurationClassLoaderUtil.getContextConfiguration(inputClassLoader);
    assertThat(url).isNotNull();
  }

}
