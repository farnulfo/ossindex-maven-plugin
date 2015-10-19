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
package net.ossindex.maven.utils;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.ossindex.common.ResourceFactory;
import net.ossindex.common.cache.MapDbCache;
import net.ossindex.common.resource.ArtifactResource;
import net.ossindex.common.resource.ScmResource;
import net.ossindex.common.utils.PackageDependency;

import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.util.graph.visitor.PreorderNodeListGenerator;

/** Utility code that performs the Maven dependency auditing. Written in a manner
 * that will allow it to be used within Maven plugins as well as outside.
 * 
 * @author Ken Duck
 *
 */
public class DependencyAuditor
{
	private RepositorySystem repoSystem;
	private RepositorySystemSession session;

	/**
	 * Testing constructor only
	 */
	DependencyAuditor()
	{
		File root = getCacheDir();
		if(root != null)
		{
			ResourceFactory.getResourceFactory().setCache(new MapDbCache(getCacheDir()));
		}
	}

	/** Make a new dependency auditor
	 * 
	 * @param repoSystem Maven repository system
	 * @param session Maven repository system session
	 */
	public DependencyAuditor(RepositorySystem repoSystem, RepositorySystemSession session)
	{
		File root = getCacheDir();
		if(root != null)
		{
			ResourceFactory.getResourceFactory().setCache(new MapDbCache(getCacheDir()));
		}
		this.repoSystem = repoSystem;
		this.session = session;
	}

	/** Get a cache directory
	 * 
	 * @return The cache directory
	 */
	private File getCacheDir()
	{
		File tmp = new File(System.getProperty("java.io.tmpdir"));
		File root = new File(tmp, "ossindex.cache");
		if(tmp.exists())
		{
			if(!root.exists()) root.mkdirs();
			if(root.exists() && root.isDirectory()) return root;
		}
		return null;
	}

	/** Audit the artifact and its dependencies
	 * 
	 * @param groupId Artifact group ID
	 * @param artifactId Artifact OD
	 * @param version Version number
	 * @throws IOException On error
	 * @return Collection of dependencies
	 */
	public Collection<PackageDependency> auditArtifact(String groupId, String artifactId, String version) throws IOException
	{
		List<PackageDependency> deps = getPackageDependencies(groupId, artifactId, version);
		setDependencyInformation(deps.toArray(new PackageDependency[deps.size()]));
		return deps;
	}


	/** Find all of the dependencies for a specified artifact
	 * 
	 * @param groupId Artifact group ID
	 * @param artifactId Artifact OD
	 * @param version Version number
	 * @return List of package dependencies
	 */
	private List<PackageDependency> getPackageDependencies(String groupId, String artifactId, String version)
	{
		List<PackageDependency> packageDependency = new LinkedList<PackageDependency>();
		String aid = groupId + ":" + artifactId + ":";
		if(version != null) aid += version;
		Dependency dependency = new Dependency( new DefaultArtifact( aid ), "compile" );

		CollectRequest collectRequest = new CollectRequest();
		collectRequest.setRoot( dependency );

		try
		{
			DependencyNode node = repoSystem.collectDependencies( session, collectRequest ).getRoot();

			DependencyRequest dependencyRequest = new DependencyRequest();
			dependencyRequest.setRoot( node );

			repoSystem.resolveDependencies( session, dependencyRequest  );

			PreorderNodeListGenerator nlg = new PreorderNodeListGenerator();
			node.accept( nlg );

			List<Artifact> artifacts = nlg.getArtifacts(false);
			for (Artifact artifact : artifacts)
			{
				PackageDependency pkgDep = new PackageDependency("maven", artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion());
				packageDependency.add(pkgDep);
			}
		}
		catch(DependencyCollectionException | DependencyResolutionException e)
		{
			// Ignore so we don't pollute Maven
			//e.printStackTrace();
		}
		return packageDependency;
	}

	/** Query OSS Index to get useful information about the dependencies. Sets the
	 * information in the appropriate package dependency.
	 * 
	 * Package protected to allow us to test
	 * 
	 * @param pkgs Packages to retrieve additional information from OSS Index for
	 * @throws IOException On error 
	 */
	void setDependencyInformation(PackageDependency[] pkgs) throws IOException
	{
		//		AbstractRemoteResource.setDebug(true);
		ArtifactResource[] artifactMatches = ResourceFactory.getResourceFactory().findArtifactResources(pkgs);
		Map<String,ArtifactResource> matches = new HashMap<String,ArtifactResource>();
		// System.err.println("FIND MATCH:");
		for (ArtifactResource artifact : artifactMatches)
		{
			if(artifact != null)
			{
				String name = artifact.getPackageName();
				// System.err.println("  * " + name);
				if(!matches.containsKey(name))
				{
					matches.put(name, artifact);
				}
				else
				{
					ArtifactResource ar = matches.get(name);
					if(artifact.compareTo(ar) > 0) matches.put(name, artifact);
				}
			}
		}

		List<PackageDependency> packages = new LinkedList<PackageDependency>();
		List<Long> scmIds = new LinkedList<Long>();
		for(PackageDependency pkg: pkgs)
		{
			String pkgName = pkg.getName();
			if(matches.containsKey(pkgName))
			{
				ArtifactResource artifact = matches.get(pkg.getName());
				long scmId = artifact.getScmId();
				// only continue with the artifact if it has a known SCM id.
				if(scmId > 0)
				{
					pkg.setArtifact(artifact);
					packages.add(pkg);
					scmIds.add(artifact.getScmId());
				}
			}
			else
			{
				//System.err.println("ZOUNDS: " + pkgName + " has no matching artifact");
			}
		}

		Long[] tmp = scmIds.toArray(new Long[scmIds.size()]);
		ScmResource[] scmResources = ResourceFactory.getResourceFactory().findScmResources(ArrayUtils.toPrimitive(tmp));
		// This should never happen
		if(scmResources == null) return;

		for(int i = 0; i < packages.size(); i++)
		{
			PackageDependency pkg = packages.get(i);
			pkg.setScm(scmResources[i]);
		}
	}

	/**
	 * Close the cache, required for clean running
	 */
	public void close()
	{
		ResourceFactory.getResourceFactory().closeCache();
	}

}
