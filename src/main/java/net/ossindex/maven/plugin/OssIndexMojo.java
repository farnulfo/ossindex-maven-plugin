/**
 *	Copyright (c) 2015-2017 Vör Security Inc.
 *	All rights reserved.
 *	
 *	Redistribution and use in source and binary forms, with or without
 *	modification, are permitted provided that the following conditions are met:
 *	    * Redistributions of source code must retain the above copyright
 *	      notice, this list of conditions and the following disclaimer.
 *	    * Redistributions in binary form must reproduce the above copyright
 *	      notice, this list of conditions and the following disclaimer in the
 *	      documentation and/or other materials provided with the distribution.
 *	    * Neither the name of the <organization> nor the
 *	      names of its contributors may be used to endorse or promote products
 *	      derived from this software without specific prior written permission.
 *	
 *	THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 *	ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *	WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *	DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 *	DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *	(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *	LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *	ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *	(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *	SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.ossindex.maven.plugin;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilderException;
import org.apache.maven.shared.dependency.graph.DependencyNode;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.ossindex.common.VulnerabilityDescriptor;
import net.ossindex.maven.utils.DependencyAuditor;
import net.ossindex.maven.utils.MavenIdWrapper;
import net.ossindex.maven.utils.MavenPackageDescriptor;
import net.ossindex.maven.utils.OssIndexResultsWrapper;

/** Cross reference the project against information in OSS Index to identify
 * security and maintenance problems.
 * 
 * @author Ken Duck
 *
 */
@Mojo( name = "audit")
public class OssIndexMojo extends AbstractMojo
{

	/**
	 * @parameter default-value="${project}"
	 */
	@Parameter(defaultValue="${project}", readonly = true, required = true)
	private MavenProject project;

	/**
	 * Comma separated list of artifacts to ignore errors for
	 * 
	 * @parameter
	 */
	@Parameter(property = "audit.ignore", defaultValue = "")
	private String ignore;
	private Set<String> ignoreSet = new HashSet<String>();

	/**
	 * Should the plugin cause a build failure?
	 * 
	 * @parameter
	 */
	@Parameter(property = "audit.failOnError", defaultValue = "true")
	private String failOnError;

	/**
	 * Comma separated list of output file paths
	 * 
	 * @parameter
	 */
	@Parameter(property = "audit.output", defaultValue = "")
	private String output;
	private Set<File> outputFiles = new HashSet<File>();

	@Parameter( defaultValue = "${session}", readonly = true, required = true )
	private MavenSession session;

	/**
	 * The dependency tree builder to use.
	 */
	@Component( hint = "default" )
	private DependencyGraphBuilder dependencyGraphBuilder;

	static
	{
		// Default log4j configuration. Hides configuration warnings.
		BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.WARN);
	}


	// Your other mojo parameters and code here
	/*
	 * (non-Javadoc)
	 * @see org.apache.maven.plugin.Mojo#execute()
	 */
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException
	{
		if(ignore != null)
		{
			ignore = ignore.trim();
			if(!ignore.isEmpty())
			{
				String[] tokens = ignore.split(",");
				for (String token : tokens)
				{
					ignoreSet.add(token.trim());
				}
			}
		}

		if (output != null) {
			output = output.trim();
			if (!output.isEmpty()) {
				String[] tokens = output.split(",");
				for (String token : tokens)
				{
					outputFiles.add(new File(token));
				}
			}
		}

		DependencyAuditor auditor = new DependencyAuditor();

		try
		{
			getLog().info("OSS Index dependency audit");

			int failures = 0;

			ArtifactFilter artifactFilter = null;
			ProjectBuildingRequest buildingRequest =
					new DefaultProjectBuildingRequest( session.getProjectBuildingRequest() );

			buildingRequest.setProject( project );

			// The computed dependency tree root node of the Maven project.
			DependencyNode rootNode = dependencyGraphBuilder.buildDependencyGraph( buildingRequest, artifactFilter );
			List<DependencyNode> depNodes = rootNode.getChildren();
			
			for (DependencyNode dep : depNodes) {
				Artifact artifact = dep.getArtifact();
				auditor.add(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(), dep);
			}

			// Perform the audit
			Collection<MavenPackageDescriptor> results = auditor.run();

			// Analyze the results
			for (MavenPackageDescriptor pkg : results) {
				String idPkg = pkg.getMavenPackageId();
				String idVer = pkg.getMavenVersionId();
				if(!ignoreSet.contains(idPkg) && !ignoreSet.contains(idVer))
				{
					MavenIdWrapper parentPkg = pkg.getParent();

					failures += report(parentPkg, pkg);
				}
			}

			// Report to various file loggers
			for (File file: outputFiles) {
				if (file.getName().endsWith(".txt")) {
					exportTxt(file, results);
				}
				if (file.getName().endsWith(".json")) {
					exportJson(file, results);
				}
				if (file.getName().endsWith(".xml")) {
					exportXml(file, results);
				}
			}

			if(failures > 0) {
				if("true".equals(failOnError)) {
					throw new MojoFailureException(failures + " known vulnerabilities affecting project dependencies");
				}
			} else {

			}
		}
		catch (IOException | DependencyGraphBuilderException e) {
			e.printStackTrace();
		}
		finally
		{
			auditor.close();
		}
	}

	/**
	 * Export the results to a text file
	 * @param file File to export to
	 * @param results Data to export
	 */
	private void exportTxt(File file, Collection<MavenPackageDescriptor> results) {
		PrintWriter out = null;
		try {
			out = new PrintWriter(new FileWriter(file));
			for (MavenPackageDescriptor pkg : results) {
				MavenIdWrapper parentPkg = pkg.getParent();
				String pkgId = pkg.getMavenVersionId();
				int total = pkg.getVulnerabilityTotal();

				List<VulnerabilityDescriptor> vulnerabilities = pkg.getVulnerabilities();
				if (vulnerabilities != null && !vulnerabilities.isEmpty()) {
					int matches = pkg.getVulnerabilityMatches();
					out.println("");
					out.println("--------------------------------------------------------------");
					out.println(pkgId + "  [VULNERABLE]");
					if(parentPkg != null)
					{
						String parentId = parentPkg.getMavenVersionId();
						out.println("  required by " + parentId);
					}
					out.println(total + " known vulnerabilities, " + matches + " affecting installed version");
					out.println("");
					for (VulnerabilityDescriptor vulnerability : vulnerabilities) {
						out.println(vulnerability.getTitle());
						out.println(vulnerability.getUriString());
						out.println(vulnerability.getDescription());
						out.println("");
					}
					out.println("--------------------------------------------------------------");
					out.println("");
				} else {
					if (total > 0) {
						out.println(pkgId + ": " + total + " known vulnerabilities, 0 affecting installed version");
					} else {
						out.println(pkgId + ": No known vulnerabilities");
					}
				}
			}
		} catch (IOException e) {
			getLog().warn("Cannot export to " + file + ": " + e.getMessage());
		} finally {
			if (out != null) {
				out.close();
			}
		}
	}

	/**
	 * Export the results to a JSON file
	 * @param file File to export to
	 * @param results Data to export
	 */
	private void exportJson(File file, Collection<MavenPackageDescriptor> results) {
		Gson gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
		String json = gson.toJson(results);
		try {
			FileUtils.writeStringToFile(file, json);
		} catch (IOException e) {
			getLog().warn("Cannot export to " + file + ": " + e.getMessage());
		}
	}

	/**
	 * Export the results to an XML file
	 * @param file File to export to
	 * @param results Data to export
	 */
	private void exportXml(File file, Collection<MavenPackageDescriptor> results) {
		OssIndexResultsWrapper wrapper = new OssIndexResultsWrapper(results);
		OutputStream out = null;
		try {
			out = new FileOutputStream(file);
			JAXBContext context = JAXBContext.newInstance(OssIndexResultsWrapper.class);
			Marshaller m = context.createMarshaller();
			m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			m.marshal(wrapper, out);

		} catch (FileNotFoundException e) {
			getLog().warn("Cannot export to " + file + ": " + e.getMessage());
		} catch (JAXBException e) {
			e.printStackTrace();
			getLog().warn("Cannot export to " + file + ": " + e.getMessage());
		} finally {
			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
					getLog().warn("Exception closing " + file + ": " + e.getMessage());
				}
			}
		}
	}

	/** Reports on all identified packages and known vulnerabilities.
	 * 
	 * @param adep List of package dependency information (with applicable audit information)
	 * @return Number of applicable vulnerabilities (indicating failure)
	 * @throws IOException On error
	 */
	private int report(MavenIdWrapper parentPkg, MavenPackageDescriptor pkg) throws IOException
	{
		int failures = 0;
		String pkgId = pkg.getMavenVersionId();
		int total = pkg.getVulnerabilityTotal();

		List<VulnerabilityDescriptor> vulnerabilities = pkg.getVulnerabilities();
		if (vulnerabilities != null && !vulnerabilities.isEmpty()) {
			int matches = pkg.getVulnerabilityMatches();
			getLog().error("");
			getLog().error("--------------------------------------------------------------");
			getLog().error(pkgId + "  [VULNERABLE]");
			if(parentPkg != null)
			{
				String parentId = parentPkg.getMavenVersionId();
				getLog().error("  required by " + parentId);
			}
			getLog().error(total + " known vulnerabilities, " + matches + " affecting installed version");
			getLog().error("");
			for (VulnerabilityDescriptor vulnerability : vulnerabilities) {
				getLog().error(vulnerability.getTitle());
				getLog().error(vulnerability.getUriString());
				getLog().error(vulnerability.getDescription());
				getLog().error("");
			}
			getLog().error("--------------------------------------------------------------");
			getLog().error("");
			failures += matches;
		} else {
			if (total > 0) {
				getLog().info(pkgId + " - " + total + " known vulnerabilities, 0 affecting installed version");
			} else {
				getLog().info(pkgId + " - No known vulnerabilities");
			}
		}

		return failures;
	}

}
