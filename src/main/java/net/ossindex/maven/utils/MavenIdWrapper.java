/**
 *	Copyright (c) 2017 Vör Security Inc.
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

import net.ossindex.common.PackageDescriptor;

/** A simple maven ID wrapper, used in particular for export purposes.
 * 
 * @author Ken Duck
 *
 */
public class MavenIdWrapper {

	protected String groupId;
	protected String artifactId;
	protected String version;
	
	/**
	 * Required for serialization
	 */
	public MavenIdWrapper() {
		
	}

	public MavenIdWrapper(PackageDescriptor pkg) {
		this.setGroupId(pkg.getGroup());
		this.setArtifactId(pkg.getName());
		this.setVersion(pkg.getVersion());
	}

	/**
	 * @return the groupId
	 */
	public String getGroupId() {
		return groupId;
	}

	/**
	 * @param groupId the groupId to set
	 */
	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}

	/**
	 * @return the artifactId
	 */
	public String getArtifactId() {
		return artifactId;
	}

	/**
	 * @param artifactId the artifactId to set
	 */
	public void setArtifactId(String artifactId) {
		this.artifactId = artifactId;
	}

	/**
	 * @return the version
	 */
	public String getVersion() {
		return version;
	}

	/**
	 * @param version the version to set
	 */
	public void setVersion(String version) {
		this.version = version;
	}

	/**
	 * Get the Maven ID excluding the version
	 * @return the Maven ID
	 */
	public String getMavenPackageId() {
		StringBuilder sb = new StringBuilder();
		if (groupId != null) {
			sb.append(groupId);
		}
		sb.append(":");
		if (artifactId != null) {
			sb.append(artifactId);
		}
		return sb.toString();
	}

	/** Get the maven ID including the version
	 * 
	 * @return the maven ID
	 */
	public String getMavenVersionId() {
		StringBuilder sb = new StringBuilder();
		if (groupId != null) {
			sb.append(groupId);
		}
		sb.append(":");
		if (artifactId != null) {
			sb.append(artifactId);
		}
		sb.append(":");
		if (version != null) {
			sb.append(version);
		}
		return sb.toString();
	}
}
