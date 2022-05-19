package com.devonfw.cobigen.impl.config.upgrade;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.cobigen.api.constants.ConfigurationConstants;
import com.devonfw.cobigen.api.exception.InvalidConfigurationException;
import com.devonfw.cobigen.impl.config.constant.ContextConfigurationVersion;
//import com.devonfw.cobigen.impl.config.entity.io.ContextConfiguration;
import com.devonfw.cobigen.impl.config.entity.io.Trigger;
import com.devonfw.cobigen.impl.config.entity.io.v3_0.Link;
import com.devonfw.cobigen.impl.config.entity.io.v3_0.Links;
import com.devonfw.cobigen.impl.config.entity.io.v3_0.Tag;
import com.devonfw.cobigen.impl.config.entity.io.v3_0.Tags;
import com.devonfw.cobigen.impl.config.entity.io.v3_0.ContextConfiguration;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import ma.glasnost.orika.MapperFacade;
import ma.glasnost.orika.MapperFactory;
import ma.glasnost.orika.impl.DefaultMapperFactory;

import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * Upgrader for the templates from v2_1 to v3_0 that splits the monolithic
 * template structure into version 3.0 with template sets
 */
public class TemplateSetUpgrader {

	private Path templatesLocation;

	/** Logger instance. */
	private static final Logger LOG = LoggerFactory.getLogger(TemplateSetUpgrader.class);
	/** Mapper factory instance. */
	private MapperFactory mapperFactory;
	/** Mapper Facade instance. */
	private MapperFacade mapper;

	/**
	 * Creates a new {@link TemplateSetUpgrader} instance
	 */
	public TemplateSetUpgrader(){//Path templatesLocation) {
		this.templatesLocation = null;//templatesLocation;
		this.mapperFactory = new DefaultMapperFactory.Builder().useAutoMapping(true).mapNulls(true).build();
		this.mapperFactory
				.classMap(com.devonfw.cobigen.impl.config.entity.io.ContainerMatcher.class,
						com.devonfw.cobigen.impl.config.entity.io.v3_0.ContainerMatcher.class)
				.field("retrieveObjectsRecursively:{isRetrieveObjectsRecursively|setRetrieveObjectsRecursively(new Boolean(%s))|type=java.lang.Boolean}",
						"retrieveObjectsRecursively:{isRetrieveObjectsRecursively|setRetrieveObjectsRecursively(new Boolean(%s))|type=java.lang.Boolean}")
				.byDefault().register();
		this.mapperFactory.classMap(com.devonfw.cobigen.impl.config.entity.io.Trigger.class,
				com.devonfw.cobigen.impl.config.entity.io.v3_0.Trigger.class).byDefault().register();
		this.mapperFactory.classMap(com.devonfw.cobigen.impl.config.entity.io.Matcher.class,
				com.devonfw.cobigen.impl.config.entity.io.v3_0.Matcher.class).byDefault().register();
		this.mapperFactory
				.classMap(com.devonfw.cobigen.impl.config.entity.io.ContainerMatcher.class,
						com.devonfw.cobigen.impl.config.entity.io.v3_0.ContainerMatcher.class)
				.field("retrieveObjectsRecursively:{isRetrieveObjectsRecursively|setRetrieveObjectsRecursively(new Boolean(%s))|type=java.lang.Boolean}",
						"retrieveObjectsRecursively:{isRetrieveObjectsRecursively|setRetrieveObjectsRecursively(new Boolean(%s))|type=java.lang.Boolean}")
				.byDefault().register();
		this.mapper = mapperFactory.getMapperFacade();
	}

	/**
	 * Upgrades the template structure from v2.1 to the new structure from v3.0. The monolithic pom and context files will be split
	 * into multiple files corresponding to every template set that will be created.
	 * @throws Exception
	 */
	public void upradeTemplatesToTemplateSets() throws Exception {

		if (this.templatesLocation == null) {
			throw new Exception("Templates location cannot be null!");
		}
		//validatefunktion
		// context at current location, just split it
		// search for Context in Folder
		// create new Folder Structures
		//
		if (this.templatesLocation.endsWith(ConfigurationConstants.TEMPLATES_FOLDER)) {
			Path cobigenTemplates = this.templatesLocation.resolve(ConfigurationConstants.COBIGEN_TEMPLATES);
			if (Files.exists(cobigenTemplates)) {
				Path contextFile = cobigenTemplates.resolve(ConfigurationConstants.TEMPLATE_RESOURCE_FOLDER)
						.resolve(ConfigurationConstants.CONTEXT_CONFIG_FILENAME);
				if (Files.exists(contextFile)) {
					ContextConfiguration contextConfiguration = getContextConfiguration(contextFile);
					List<com.devonfw.cobigen.impl.config.entity.io.v3_0.ContextConfiguration> test = splitContext(contextConfiguration);
					System.out.println("Hallo");
					if (contextConfiguration != null) {
						// create new template set folder
						Path templateSetsPath = Files.createDirectory(this.templatesLocation.getParent()
								.resolve(ConfigurationConstants.TEMPLATE_SETS_FOLDER));
						Path adaptedFolder = Files
								.createDirectory(templateSetsPath.resolve(ConfigurationConstants.ADAPTED_FOLDER));
						List<Trigger> triggers = contextConfiguration.getTrigger();
						for (Trigger trigger : triggers) {
							processTrigger(trigger, cobigenTemplates, adaptedFolder);
						}

						// backup of old files
						Path backupPath = templatesLocation.resolve("backup");
						if (this.templatesLocation.endsWith(ConfigurationConstants.TEMPLATES_FOLDER)) {
							backupPath = templatesLocation.getParent().resolve("backup");
						}
						File backupFolder = backupPath.toFile();
						if (!backupFolder.exists()) {
							backupFolder.mkdir();
						}
						try {
							FileUtils.moveDirectoryToDirectory(cobigenTemplates.getParent().toFile(), backupFolder, false);
						} catch (IOException e) {
							LOG.error("Error copying and deleting the old template files", e);
							throw e;
						}
					} else {
						LOG.info("Unable to parse context.xml file {}.", contextFile);
					}
				} else {
					LOG.info("No context.xml file found. {}", contextFile);
				}
			} else {
				LOG.info("No CobiGen_Templates folder found. Upgrade needs an adapted templates folder.");
			}
		} else {
			LOG.info("The path {} is no valid templates location.", this.templatesLocation);
		}

	}

	/**
	 * Upgrades the template structure from v2.1 to the new structure from v3.0. The monolithic pom and context files will be split
	 * into multiple files corresponding to every template set that will be created.
	 * @throws Exception
	 */
	public ConfigurationUpgradeResult upgradeTemplatesToTemplateSets(ContextConfiguration context) throws Exception {

		ConfigurationUpgradeResult result = new ConfigurationUpgradeResult();
		List<com.devonfw.cobigen.impl.config.entity.io.v3_0.ContextConfiguration> test = splitContext(context);


	}

	/**
	 * Upgrades the template structure from v2.1 to the new structure from v3.0. The monolithic pom and context files will be split
	 * into multiple files corresponding to every template set that will be created.
	 * @throws Exception
	 */
	public void upgradeTemplatesToTemplateSetsAufgeäumt() throws Exception {
		// FileTree Walker
		// speichert Path zu Context und splitted
		// gucken ob Templates existieren sonst custom template pfad getten

		// speichert Util Folder
		// speichert Path zu der Pom


	}



	protected List<com.devonfw.cobigen.impl.config.entity.io.v3_0.ContextConfiguration> splitContext(ContextConfiguration monolitic){
		List<com.devonfw.cobigen.impl.config.entity.io.v3_0.ContextConfiguration> splittedContexts = new ArrayList<>();
		List<Trigger> triggerList = monolitic.getTrigger();
		for(Trigger trigger : triggerList) {
			com.devonfw.cobigen.impl.config.entity.io.v3_0.ContextConfiguration contextConfiguration3_0 = new com.devonfw.cobigen.impl.config.entity.io.v3_0.ContextConfiguration();
			com.devonfw.cobigen.impl.config.entity.io.v3_0.Trigger trigger3_0 = new com.devonfw.cobigen.impl.config.entity.io.v3_0.Trigger();
			trigger3_0.setId(trigger.getId());
			trigger3_0.setInputCharset(trigger.getInputCharset());
			trigger3_0.setType(trigger.getType());
			trigger3_0.setTemplateFolder(trigger.getTemplateFolder());

			List<com.devonfw.cobigen.impl.config.entity.io.v3_0.Matcher> v3MList = mapper.mapAsList(trigger.getMatcher(),
					com.devonfw.cobigen.impl.config.entity.io.v3_0.Matcher.class);
			List<com.devonfw.cobigen.impl.config.entity.io.v3_0.ContainerMatcher> v3CMList = mapper.mapAsList(
					trigger.getContainerMatcher(), com.devonfw.cobigen.impl.config.entity.io.v3_0.ContainerMatcher.class);
			trigger3_0.getContainerMatcher().addAll(v3CMList);
			trigger3_0.getMatcher().addAll(v3MList);
			contextConfiguration3_0.getTrigger().add(trigger3_0);
			Tags tags = new Tags();
			Tag tag = new Tag();
			tag.setName("PLACEHOLDER---This tag was inserted through the upgrade process and has to be changed manually---PLACEHOLDER");
			tags.getTag().add(tag);
			contextConfiguration3_0.setTags(tags);
			Links links = new Links();
			Link link = new Link();
			link.setUrl("PLACEHOLDER---This tag was inserted through the upgrade process and has to be changed manually---PLACEHOLDER");
			links.getLink().add(link);
			contextConfiguration3_0.setLinks(links);
			contextConfiguration3_0.setVersion(new BigDecimal("3.0"));
			splittedContexts.add(contextConfiguration3_0);
		}
		//TODO diese Funktion vielleicht schöner schreiben mit mehr mapper
		return splittedContexts;
	}

	protected boolean writeContext(Path contextPath, com.devonfw.cobigen.impl.config.entity.io.v3_0.ContextConfiguration contextConfiguration) {
		if(contextPath.resolve(ConfigurationConstants.CONTEXT_CONFIG_FILENAME).toFile().exists()) {
			LOG.error("Context.xml already exist in this folder");
			return false;
		}
		try {
			Marshaller marshaller = JAXBContext.newInstance("com.devonfw.cobigen.impl.config.entity.io.v3_0")
					.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			marshaller.marshal(contextConfiguration, contextPath.toFile());
		} catch (JAXBException e) {
			throw new InvalidConfigurationException("Parsing of the context file provided some XML errors", e);

		}
		return true;
	}

	protected Model readMonolithicPom(Path pomPath) throws FileNotFoundException, IOException, XmlPullParserException {
		MavenXpp3Reader reader = new MavenXpp3Reader();
		Model model = null;
		try(
				FileInputStream pomInputStream = new FileInputStream(pomPath.resolve("pom.xml").toFile());
		){
			try {
				model = reader.read(pomInputStream);
			}catch (IOException e) {
				LOG.error("IOError while reading the monolithic pom file", e);
				pomInputStream.close();
				throw e;
			} catch (XmlPullParserException e) {
				LOG.error("XMLError while parsing the monolitic pom file", e);
				pomInputStream.close();
				throw e;
			}
		}
		return model;
	}

	protected boolean writePom(Path pomPath, Model monolithicPom, com.devonfw.cobigen.impl.config.entity.io.v3_0.ContextConfiguration contextConfiguration) throws FileNotFoundException, IOException {
		if(pomPath.resolve("pom.xml").toFile().exists()) {
			LOG.error("Context.xml already exist in this folder");
			return false;
		}
		// Pom.xml creation
		MavenXpp3Writer writer = new MavenXpp3Writer();

		Model m = new Model();
		Parent p = new Parent();
		p.setArtifactId(monolithicPom.getArtifactId());
		p.setGroupId(monolithicPom.getGroupId());
		p.setVersion(monolithicPom.getVersion());
		m.setParent(p);
		m.setDependencies(monolithicPom.getDependencies());
		contextConfiguration.getTrigger().get(0).getId().replace('_', '-');
		m.setName("PLACEHOLDER---Replace this text with a correct template name---PLACEHOLDER");
		try (
				FileOutputStream pomOutputStream = new FileOutputStream(pomPath.resolve("pom.xml").toFile());
		){
			try {
				writer.write(new FileOutputStream(pomPath.resolve("pom.xml").toFile()), m);
			} catch (FileNotFoundException e) {
				LOG.error("Error while creating the new v3_0 pom file", e);
				throw e;
			} catch (IOException e) {
				LOG.error("IOError while writing the new v3_0 pom file", e);
				throw e;
			}
		}
		return true;
	}

	/**
	 * Locates the correct context file
	 * @param {@link Path} to the contextFile
	 * @return {@link ContextConfiguration}
	 */
	private ContextConfiguration getContextConfiguration(Path contextFile) {

		try (InputStream in = Files.newInputStream(contextFile)) {
			Unmarshaller unmarschaller = JAXBContext.newInstance(ContextConfiguration.class).createUnmarshaller();

			Object rootNode = unmarschaller.unmarshal(in);
			if (rootNode instanceof ContextConfiguration) {
				return (ContextConfiguration) rootNode;
			}
		} catch (IOException e) {
			throw new InvalidConfigurationException("Context file could not be found", e);
		} catch (JAXBException e) {
			throw new InvalidConfigurationException("Context file provided some XML errors", e);
		}
		return null;
	}

	/**
	 * Converts a {@link Trigger} to a v3 Trigger and uses the data from the Trigger to create a new Template Set with the
	 * template files from the old structure and builds context.xml and pom.xml from the Trigger data.
	 * @param {@link Trigger} trigger for a template set
	 * @param {@link Path} Path to the cobigen templates folder
	 * @param {@link Path} Path to the adapted Folder in template sets
	 * @throws IOException
	 * @throws XmlPullParserException
	 */
	private void processTrigger(Trigger trigger, Path cobigenTemplates, Path templateSetsAdapted)
			throws IOException, XmlPullParserException {

		Path templatesPath = cobigenTemplates.resolve(ConfigurationConstants.TEMPLATE_RESOURCE_FOLDER)
				.resolve(trigger.getTemplateFolder());
		Path templateSetPath = Files.createDirectory(templateSetsAdapted.resolve(trigger.getTemplateFolder()));

		// copy template files
		FileUtils.copyDirectory(templatesPath.toFile(),
				templateSetPath.resolve(ConfigurationConstants.TEMPLATE_RESOURCE_FOLDER).toFile());

		// copy java utils
		Path utilsPath = cobigenTemplates.resolve("src/main/java");
		FileUtils.copyDirectory(utilsPath.toFile(), templateSetPath.resolve("src/main/java").toFile());
		// src main ressources copieren

		// create context.xml
		com.devonfw.cobigen.impl.config.entity.io.v3_0.ContextConfiguration contextConfiguration = new com.devonfw.cobigen.impl.config.entity.io.v3_0.ContextConfiguration();
		contextConfiguration.setVersion(new BigDecimal(3.0));

		// create new trigger
		List<com.devonfw.cobigen.impl.config.entity.io.v3_0.Trigger> triggerList = contextConfiguration.getTrigger();
		com.devonfw.cobigen.impl.config.entity.io.v3_0.Trigger trigger3_0 = new com.devonfw.cobigen.impl.config.entity.io.v3_0.Trigger();
		trigger3_0.setId(trigger.getId());
		trigger3_0.setInputCharset(trigger.getInputCharset());
		trigger3_0.setType(trigger.getType());
		trigger3_0.setTemplateFolder(trigger.getTemplateFolder());

		// map containerMatcher and matcher to v.3_0
		List<com.devonfw.cobigen.impl.config.entity.io.v3_0.Matcher> v3MList = mapper.mapAsList(trigger.getMatcher(),
				com.devonfw.cobigen.impl.config.entity.io.v3_0.Matcher.class);
		List<com.devonfw.cobigen.impl.config.entity.io.v3_0.ContainerMatcher> v3CMList = mapper.mapAsList(
				trigger.getContainerMatcher(), com.devonfw.cobigen.impl.config.entity.io.v3_0.ContainerMatcher.class);
		trigger3_0.getContainerMatcher().addAll(v3CMList);
		trigger3_0.getMatcher().addAll(v3MList);

		// add trigger to context
		triggerList.add(trigger3_0);
		Tags tags = new Tags();
		Tag tag = new Tag();
		tag.setName("PLACEHOLDER---This tag was inserted through the upgrade process and has to be changed manually---PLACEHOLDER");
		tags.getTag().add(tag);
		contextConfiguration.setTags(tags);
		Links links = new Links();
		Link link = new Link();
		link.setUrl("PLACEHOLDER---This tag was inserted through the upgrade process and has to be changed manually---PLACEHOLDER");
		links.getLink().add(link);
		contextConfiguration.setLinks(links);

		// write context.xml
		Path newContextPath = templateSetPath.resolve(ConfigurationConstants.TEMPLATE_RESOURCE_FOLDER);
		newContextPath = newContextPath.resolve(ConfigurationConstants.CONTEXT_CONFIG_FILENAME);
		try {
			Marshaller marshaller = JAXBContext.newInstance("com.devonfw.cobigen.impl.config.entity.io.v3_0")
					.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			marshaller.marshal(contextConfiguration, newContextPath.toFile());
		} catch (JAXBException e) {
			throw new InvalidConfigurationException("Parsing of the context file provided some XML errors", e);
		}

		// Pom.xml creation
		MavenXpp3Reader reader = new MavenXpp3Reader();
		MavenXpp3Writer writer = new MavenXpp3Writer();
		Model mMonolithicPom;
		try(
				FileInputStream pomInputStream = new FileInputStream(cobigenTemplates.resolve("pom.xml").toFile());
		){
			try {
				mMonolithicPom = reader.read(pomInputStream);
			} catch (FileNotFoundException e) {
				LOG.error("Monolitic pom file could not be found", e);
				pomInputStream.close();
				throw e;
			} catch (IOException e) {
				LOG.error("IOError while reading the monolithic pom file", e);
				pomInputStream.close();
				throw e;
			} catch (XmlPullParserException e) {
				LOG.error("XMLError while parsing the monolitic pom file", e);
				pomInputStream.close();
				throw e;
			}
		}
		Model m = new Model();
		Parent p = new Parent();
		p.setArtifactId(mMonolithicPom.getArtifactId());
		p.setGroupId(mMonolithicPom.getGroupId());
		p.setVersion(mMonolithicPom.getVersion());
		m.setParent(p);
		m.setDependencies(mMonolithicPom.getDependencies());
		m.setArtifactId(trigger.getId().replace('_', '-'));
		m.setName("PLACEHOLDER---Replace this text with a correct template name---PLACEHOLDER");
		try (
				FileOutputStream pomOutputStream = new FileOutputStream(templateSetPath.resolve("pom.xml").toFile());
		){
			try {
				writer.write(new FileOutputStream(templateSetPath.resolve("pom.xml").toFile()), m);
			} catch (FileNotFoundException e) {
				LOG.error("Error while creating the new v3_0 pom file", e);
				throw e;
			} catch (IOException e) {
				LOG.error("IOError while writing the new v3_0 pom file", e);
				throw e;
			}
		}
	}

}
