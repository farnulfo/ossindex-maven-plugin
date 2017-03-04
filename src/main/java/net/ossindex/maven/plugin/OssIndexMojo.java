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

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;

import net.ossindex.common.PackageDescriptor;
import net.ossindex.common.VulnerabilityDescriptor;
import net.ossindex.maven.utils.DependencyAuditor;

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
	 * The entry point to Aether, i.e. the component doing all the work.
	 * 
	 * @component
	 */
	@Component
	private RepositorySystem repoSystem;

	/**
	 * The current repository/network configuration of Maven.
	 * 
	 * @parameter default-value="${repositorySystemSession}"
	 */
	@Parameter(defaultValue="${repositorySystemSession}", readonly = true)
	private RepositorySystemSession repoSession;

	/**
	 * The project's remote repositories to use for the resolution of project dependencies.
	 * 
	 * @parameter default-value="${project.remoteProjectRepositories}"
	 */
	@Parameter(defaultValue="${project.remoteProjectRepositories}", readonly = true)
	private List<RemoteRepository> projectRepos;

	/**
	 * The project's remote repositories to use for the resolution of plugins and their dependencies.
	 * 
	 * @parameter default-value="${project.remotePluginRepositories}"
	 */
	@Parameter(defaultValue="${project.remotePluginRepositories}", readonly = true)
	private List<RemoteRepository> pluginRepos;

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
		DependencyAuditor auditor = new DependencyAuditor(repoSystem, repoSession);

		try
		{
			getLog().info("OSS Index dependency audit");

			int failures = 0;
			@SuppressWarnings("unchecked")

			List<Dependency> deps = project.getDependencies();
			// Assemble the query
			for (Dependency dep : deps)
			{
				auditor.add(dep.getGroupId(), dep.getArtifactId(), dep.getVersion());
			}

			// Perform the audit
			Collection<PackageDescriptor> results = auditor.run();

			// Analyze the results
			for (PackageDescriptor pkg : results) {
				String id = pkg.getGroup() + ":" + pkg.getName();
				String idVer = id + ":" + pkg.getVersion();
				if(!ignoreSet.contains(id) && !ignoreSet.contains(idVer))
				{
					PackageDescriptor parent = auditor.getParent(pkg);
					failures += report(parent, pkg);
				}
			}

			if(failures > 0)
			{
				if("true".equals(failOnError))
				{
					throw new MojoFailureException(failures + " known vulnerabilities affecting project dependencies");
				}
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		finally
		{
			auditor.close();
		}
	}

	/** Reports on all identified packages and known vulnerabilities.
	 * 
	 * @param adep List of package dependency information (with applicable audit information)
	 * @return Number of applicable vulnerabilities (indicating failure)
	 * @throws IOException On error
	 */
	private int report(PackageDescriptor parent, PackageDescriptor pkg) throws IOException
	{
		int failures = 0;
		String pkgId = pkg.getGroup() + ":" + pkg.getName() + ":" + pkg.getVersion();
		int total = pkg.getVulnerabilityTotal();

		List<VulnerabilityDescriptor> vulnerabilities = pkg.getVulnerabilities();
		if (vulnerabilities != null && !vulnerabilities.isEmpty()) {
			int matches = pkg.getVulnerabilityMatches();
			getLog().error("");
			getLog().error("--------------------------------------------------------------");
			getLog().error(pkgId + "  [VULNERABLE]");
			if(parent != null)
			{
				String parentId = parent.getGroup() + ":" + parent.getName()  + ":" + parent.getVersion();
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
				getLog().info(pkgId + ": " + total + " known vulnerabilities, 0 affecting installed version");
			} else {
				getLog().info(pkgId + ": No known vulnerabilities");
			}
		}

		return failures;
	}

}
