/**
 *	Copyright (c) 2015 Vör Security Inc.
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
import java.net.SocketException;
import java.net.URI;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import net.ossindex.common.resource.ScmResource;
import net.ossindex.common.resource.VulnerabilityResource;
import net.ossindex.common.utils.PackageDependency;
import net.ossindex.maven.utils.DependencyAuditor;

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
	 * @readonly
	 */
	@Parameter(defaultValue="${repositorySystemSession}", readonly = true)
	private RepositorySystemSession repoSession;

	/**
	 * The project's remote repositories to use for the resolution of project dependencies.
	 * 
	 * @parameter default-value="${project.remoteProjectRepositories}"
	 * @readonly
	 */
	@Parameter(defaultValue="${project.remoteProjectRepositories}", readonly = true)
	private List<RemoteRepository> projectRepos;

	/**
	 * The project's remote repositories to use for the resolution of plugins and their dependencies.
	 * 
	 * @parameter default-value="${project.remotePluginRepositories}"
	 * @readonly
	 */
	@Parameter(defaultValue="${project.remotePluginRepositories}", readonly = true)
	private List<RemoteRepository> pluginRepos;

	/**
	 * 
	 */
	@Parameter(defaultValue="${project}", readonly = true, required = true)
	private MavenProject project;

	// Your other mojo parameters and code here
	/*
	 * (non-Javadoc)
	 * @see org.apache.maven.plugin.Mojo#execute()
	 */
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException
	{
		DependencyAuditor auditor = new DependencyAuditor(repoSystem, repoSession);

		getLog().info("OSS Index dependency audit");
		
		int failures = 0;
		@SuppressWarnings("unchecked")
		List<Dependency> deps = project.getDependencies();
		for (Dependency dep : deps)
		{
			try
			{
				Collection<PackageDependency> auditedDependencies = auditor.auditArtifact(dep.getGroupId(), dep.getArtifactId(), dep.getVersion());
				for (PackageDependency adep : auditedDependencies)
				{
					failures += report(adep);
				}
			}
			catch (SocketException e)
			{
				getLog().error(e.getMessage());
				break;
			}
			catch (IOException e)
			{
				getLog().error("Exception auditing dependency " + dep.getGroupId() + ":" + dep.getArtifactId() + ":" + dep.getVersion(), e);
			}
		}
		
		if(failures > 0)
		{
			throw new MojoFailureException(failures + " known vulnerabilities affecting project dependencies");
		}
	}

	/** Reports on all identified packages and known vulnerabilities.
	 * 
	 * @param adep List of package dependency information (with applicable audit information)
	 * @return Number of applicable vulnerabilities (indicating failure)
	 * @throws IOException
	 */
	private int report(PackageDependency adep) throws IOException
	{
		int failures = 0;
		String pkgId = adep.getId();
		ScmResource scm = adep.getScm();
		if(scm != null)
		{
			VulnerabilityResource[] vulnerabilities = scm.getVulnerabilities();
			if(vulnerabilities != null)
			{
				if(vulnerabilities.length > 0)
				{
					List<VulnerabilityResource> myVulnerabilities = new LinkedList<VulnerabilityResource>();
					for (VulnerabilityResource vulnerability : vulnerabilities)
					{
						if(vulnerability.appliesTo(adep.getVersion()))
						{
							myVulnerabilities.add(vulnerability);
						}
						URI uri = vulnerability.getUri();
						String description = vulnerability.getDescription();
					}
					
					if(myVulnerabilities.isEmpty())
					{
						getLog().info(pkgId + "  " + vulnerabilities.length + " known vulnerabilities, 0 affecting installed version");
					}
					else
					{
						getLog().error("");
						getLog().error("--------------------------------------------------------------");
						getLog().error(pkgId + "  [VULNERABLE]");
						getLog().error(vulnerabilities.length + " known vulnerabilities, " + myVulnerabilities.size() + " affecting installed version");
						getLog().error("");
						for (VulnerabilityResource vulnerability : myVulnerabilities)
						{
							getLog().error(vulnerability.getUri().toString());
							getLog().error(vulnerability.getDescription());
							getLog().error("");
						}
						getLog().error("--------------------------------------------------------------");
						getLog().error("");
						failures += myVulnerabilities.size();
					}

				}
				else
				{
					getLog().info(pkgId + "  No known vulnerabilities");
				}
			}
			else
			{
				getLog().info(pkgId + "  No known vulnerabilities");
			}
		}
		else
		{
			getLog().info(pkgId + "  Unknown source for package");
		}
		
		return failures;
	}

}
