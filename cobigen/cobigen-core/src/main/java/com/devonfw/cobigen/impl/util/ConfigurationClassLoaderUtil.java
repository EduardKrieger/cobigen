package com.devonfw.cobigen.impl.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.cobigen.api.exception.InvalidConfigurationException;
import com.devonfw.cobigen.impl.config.ConfigurationHolder;

/**
 * Utilities related to the retrieval of Templates utility classes
 */
public class ConfigurationClassLoaderUtil {

  /** Logger instance. */
  private static final Logger LOG = LoggerFactory.getLogger(ConfigurationClassLoaderUtil.class);

  /** Locations to check for template utility classes */
  private static final String[] classFolderLocations = new String[] { "target/classes" };

  /** Locations to check for context.xml */
  private static final String[] configFileLocations = new String[] { "context.xml", "src/main/templates/context.xml" };

  /// ** Locations to check for template utility classes */
  // private static final String[] templateSetClassFolderLocations = new String[] { "target/classes" };

  /** Locations to check for context.xml */
  private static final String[] templateSetConfigFileLocations = new String[] { "/template-set.xml",
  "src/main/templates/template-set.xml" };

  /**
   * Checks the ClassLoader for any context.xml provided either in configurationFolder or in templates-plugin and
   * returns its URL
   *
   * @param classLoader ClassLoader to check resources from
   * @return URL of the context configuration file path
   * @throws InvalidConfigurationException if no configuration file was found
   */
  public static URL getContextConfiguration(ClassLoader classLoader) throws InvalidConfigurationException {

    URL contextConfigurationLocation = null;
    for (String possibleLocation : configFileLocations) {
      URL configLocation = classLoader.getResource(possibleLocation);
      if (configLocation != null) {
        contextConfigurationLocation = configLocation;
        LOG.debug("Found context.xml URL @ {}", contextConfigurationLocation);
        break;
      }
    }

    if (contextConfigurationLocation == null) {
      throw new InvalidConfigurationException("No context.xml could be found in the classpath!");
    }
    return contextConfigurationLocation;
  }

  /**
   * Checks the ClassLoader for any template-set.xml provided either in configurationFolder or in templates-plugin and
   * returns its URL
   *
   * @param classLoader ClassLoader to check resources from
   * @return URL of the context configuration file path
   * @throws InvalidConfigurationException if no configuration file was found
   * @throws IOException
   * @throws URISyntaxException
   */
  public static List<URL> getTemplateSetConfiguration(ClassLoader classLoader)
      throws InvalidConfigurationException, IOException, URISyntaxException {

    List<URL> templateSetConfigurationLocation = new ArrayList<>();
    List<URL> ressources = Collections.list(classLoader.getResources(""));
    for (URL templateSet : ressources) {
      File templateSetFolder = new File(templateSet.getPath());
      if (templateSetFolder.isDirectory()) {
        for (File f : templateSetFolder.listFiles()) {
          if (f.isDirectory()) {
            for (String possibleLocation : templateSetConfigFileLocations) {
              possibleLocation = f.getName() + possibleLocation;
              List<URL> configLocation = Collections.list(classLoader.getResources(possibleLocation));
              if (configLocation.size() > 0) {
                templateSetConfigurationLocation.addAll(configLocation);
                for (URL url : configLocation) {
                  LOG.debug("Found template-set.xml URL @ {}", url);
                }
              }
            }
          } else if (f.getName().endsWith(".jar")) {
            System.out.println(f.getName());
            List<URL> templateSetFromJar = Collections.list(classLoader.getResources(f.getName()));
            Path test3 = f.toPath().resolve("template-set.xml");
            File testFile = new File(test3.toUri());
            if (testFile.exists()) {
              templateSetConfigurationLocation.add(test3.toUri().toURL());
            }
            for (URL u : templateSetFromJar) {
              System.out.println(u.toURI());
              // FileSystem fs = FileSystemUtil.getOrCreateFileSystem(u.toURI());
              Path fs2 = FileSystemUtil.createFileSystemDependentPath(u.toURI());
              // Files.exists(fs2, null)
              ZipFile zfile = new ZipFile(new File(u.toURI()));
              ZipEntry tsf = zfile.getEntry("template-set.xml");
              ZipEntry tsf2 = zfile.getEntry("functions.ftl");
              ZipEntry tsf3 = zfile.getEntry("templates");
              Path test = fs2.resolve("template-set.xml");
              URL templateSetJar = u.toURI().resolve("template-set.xml").toURL();
              InputStream in = new BufferedInputStream(templateSetJar.openStream(), 1024);
              ZipInputStream stream = new ZipInputStream(in);
              byte[] buffer = new byte[2024];
              buffer = stream.readAllBytes();
              // Path config = fs.getPath("/template-set.xml");
              u = u.toURI().resolve("template-set.xml").toURL();
              // File testFile = new File(u.toURI());
              if (testFile.exists()) {
                templateSetConfigurationLocation.add(u);
              }
            }
          }
        }
      }
    }

    if (templateSetConfigurationLocation.size() == 0) {
      throw new InvalidConfigurationException("No template-set.xml could be found in the classpath!");
    }
    return templateSetConfigurationLocation;
  }

  /**
   * Initializes the ClassLoader with given URLs array
   *
   * @param urls URL[] Array of URLs to load into ClassLoader
   * @param classLoader to add urls to
   * @return ClassLoader to load resources from
   */
  private static ClassLoader getUrlClassLoader(URL[] urls, ClassLoader classLoader) {

    ClassLoader inputClassLoader = null;
    if (classLoader != null) {
      inputClassLoader = URLClassLoader.newInstance(urls, classLoader);
    } else {
      inputClassLoader = URLClassLoader.newInstance(urls, ConfigurationClassLoaderUtil.class.getClassLoader());
    }

    return inputClassLoader;
  }

  /**
   * Adds folders to class loader urls e.g. src/main/templates for config.xml detection
   *
   * @param configurationFolder Path configuration folder for which to generate paths
   * @return ArrayList<URL> of URLs
   * @throws MalformedURLException if the URL was malformed
   */
  private static List<URL> addFoldersToClassLoaderUrls(Path configurationFolder) throws MalformedURLException {

    List<URL> classLoaderUrls = new ArrayList<>();
    for (String possibleLocation : classFolderLocations) {
      Path folder = configurationFolder;
      folder = folder.resolve(possibleLocation);
      if (Files.exists(folder)) {
        classLoaderUrls.add(folder.toUri().toURL());
        LOG.debug("Added {} to class path", folder);
      }
    }
    return classLoaderUrls;
  }

  /**
   * Walks the class path in search of a 'context.xml' resource to identify the enclosing folder or jar file. That
   * location is then searched for class files and a list with those loaded classes is returned. If the sources are not
   * compiled, the templates will not be able to be generated.
   *
   * @param configurationHolder {@link ConfigurationHolder}
   * @param classLoader ClassLoader to load jar from
   * @return a List of Classes for template generation.
   * @throws IOException if either templates jar or templates folder could not be read
   */
  public static List<Class<?>> resolveUtilClasses(ConfigurationHolder configurationHolder, ClassLoader classLoader)
      throws IOException {

    List<Class<?>> result = new LinkedList<>();
    List<URL> classLoaderUrls = new ArrayList<>(); // stores ClassLoader URLs

    List<Path> utilsLocations = configurationHolder.getUtilsLocation();
    for (Path utilsLocation : utilsLocations) {
      if (FileSystemUtil.isZipFile(utilsLocation.toUri())) {
        result.addAll(resolveFromJar(classLoader, utilsLocation));
      } else {
        ClassLoader inputClassLoader = null;
        classLoaderUrls = addFoldersToClassLoaderUrls(utilsLocation);
        inputClassLoader = getUrlClassLoader(classLoaderUrls.toArray(new URL[] {}), classLoader);
        result.addAll(resolveFromFolder(utilsLocation, inputClassLoader));
      }
    }

    return result;
  }

  /**
   * Resolves utility classes from Folder
   *
   * @param templateRoot Path to template folder containing classes
   * @param inputClassLoader ClassLoader to use for storing of classes
   * @return List of classes to load utilities from
   */
  private static List<Class<?>> resolveFromFolder(Path templateRoot, ClassLoader inputClassLoader) {

    LOG.debug("Processing configuration from {}", templateRoot.toString());
    List<Path> foundPaths = new LinkedList<>();
    List<Class<?>> result = new ArrayList<>();

    try {
      foundPaths = walkTemplateFolder(templateRoot);
    } catch (IOException e) {
      LOG.error("Could not read templates folder", e);
    }
    if (foundPaths.size() > 0) {

      // clean up test classes
      Iterator<Path> it = foundPaths.iterator();
      while (it.hasNext()) {
        Path next = it.next();
        if (!templateRoot.relativize(next).startsWith("target/classes")) {
          LOG.debug("    * Removed test class file {}", next);
          it.remove();
        }
      }

      for (Path path : foundPaths) {
        try {
          result.add(loadClassByPath(templateRoot.relativize(path), inputClassLoader));
        } catch (ClassNotFoundException e) {
          LOG.error("Class could not be loaded into ClassLoader", e);
        }
      }
    } else {
      LOG.debug("Could not find any compiled classes to be loaded as util classes in template folder.");
    }

    return result;
  }

  /**
   * Resolves utility classes from Jar archive
   *
   * @param inputClassLoader ClassLoader to use for storing of classes
   * @param configurationHolder configuration holder
   * @return List of classes to load utilities from
   */
  private static List<Class<?>> resolveFromJar(ClassLoader inputClassLoader, Path utilLocation) {

    LOG.debug("Processing configuration archive {}", utilLocation);
    LOG.info("Searching for classes in configuration archive...");

    List<Class<?>> result = new ArrayList<>();
    List<String> foundClasses = new LinkedList<>();
    try {
      foundClasses = walkJarFile(utilLocation);
    } catch (IOException e) {
      LOG.error("Could not read templates jar file", e);
    }
    if (foundClasses.size() > 0) {
      for (String className : foundClasses) {
        try {
          result.add(inputClassLoader.loadClass(className));
        } catch (ClassNotFoundException e) {
          LOG.warn("Could not load {} from classpath", className);
          LOG.debug("Class was not found", e);
        }
      }
    } else {
      LOG.info("Could not find any compiled classes to be loaded as util classes in jar file.");
    }
    return result;
  }

  /**
   * Walks the template folder in search of utility classes
   *
   * @param templateRoot Path to template folder
   * @return List<Path> of paths containing a class file
   * @throws IOException if file could not be visited
   */
  private static List<Path> walkTemplateFolder(Path templateRoot) throws IOException {

    final List<Path> foundPaths = new LinkedList<>();
    Files.walkFileTree(templateRoot, new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {

        if (file.toString().endsWith(".class")) {
          foundPaths.add(file);
          LOG.debug("    * Found class file {}", file);
        }
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {

        // Log errors but do not throw an exception
        LOG.warn("An IOException occurred while reading file on path {} with message: {}", file, exc.getMessage(),
            LOG.isDebugEnabled() ? exc : null);
        return FileVisitResult.CONTINUE;
      }
    });
    return foundPaths;
  }

  /**
   * Walks the jar file in search of utility classes
   *
   * @param configurationHolder the holder of the parsed configuration
   * @return List<String> of file paths containing class files
   * @throws IOException if file could not be visited
   */
  private static List<String> walkJarFile(Path utilLocation) throws IOException {

    List<String> foundClasses = new LinkedList<>();
    // walk the jar file
    LOG.debug("Searching for classes in {}", utilLocation);
    Path configurationPath = utilLocation;
    if (FileSystemUtil.isZipFile(utilLocation.toUri())) {
      configurationPath = FileSystemUtil.createFileSystemDependentPath(utilLocation.toUri());
    }
    Files.walkFileTree(configurationPath, new SimpleFileVisitor<Path>() {

      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {

        if (file.toString().endsWith(".class")) {
          LOG.debug("    * Found class file {}", file);
          // remove the leading '/' and the trailing '.class'
          String fileName = file.toString().substring(1, file.toString().length() - 6);
          // replace the path separator '/' with package separator '.' and add it to the
          // list of found files
          foundClasses.add(fileName.replace("/", "."));
        }
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {

        // Log errors but do not throw an exception
        LOG.warn("An IOException occurred while reading file on path {} with message: {}", file, exc.getMessage(),
            LOG.isDebugEnabled() ? exc : null);
        return FileVisitResult.CONTINUE;
      }
    });
    return foundClasses;
  }

  /**
   * Tries to load a class over it's file path. If the path is /a/b/c/Some.class this method tries to load the following
   * classes in this order: <list>
   * <li>Some</li>
   * <li>c.Some</li>
   * <li>b.c.Some</li>
   * <li>a.b.c.Some</> </list>
   *
   * @param classPath the {@link Path} of the Class file
   * @param cl the used ClassLoader
   * @return Class<?> of the class file
   * @throws ClassNotFoundException if no class could be found all the way up to the path root
   */
  private static Class<?> loadClassByPath(Path classPath, ClassLoader cl) throws ClassNotFoundException {

    // Get a list with all path segments, starting with the class name
    Queue<String> pathSegments = new LinkedList<>();
    // Split the path by the systems file separator and without the .class suffix
    for (int i = classPath.getNameCount() - 1; i > -1; i--) {
      pathSegments.add(FilenameUtils.removeExtension(classPath.getName(i).getFileName().toString()));
    }

    if (!pathSegments.isEmpty()) {
      String className = "";
      while (!pathSegments.isEmpty()) {
        if (className == "") {
          className = pathSegments.poll();
        } else {
          className = pathSegments.poll() + "." + className;
        }
        try {
          return cl.loadClass(className);
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
          continue;
        }
      }
    }
    throw new ClassNotFoundException("Could not find class on path " + classPath.toString());

  }
}
