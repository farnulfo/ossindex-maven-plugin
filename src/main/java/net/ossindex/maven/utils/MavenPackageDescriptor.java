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

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

import com.google.gson.annotations.SerializedName;

import net.ossindex.common.PackageDescriptor;
import net.ossindex.common.VulnerabilityDescriptor;

/** Wrapper around package descriptor data that prepares the information for export
 * to files. The data is represented in a more maven expected format.
 * 
 * @author Ken Duck
 *
 */
public class MavenPackageDescriptor extends MavenIdWrapper {
	
	private MavenIdWrapper parent;

	@XmlElement(name = "vulnerability-total")
	@SerializedName("vulnerability-total")
	private int vulnerabilityTotal;

	@XmlElement(name = "vulnerability-matches")
	@SerializedName("vulnerability-matches")
	private int vulnerabilityMatches;
	
	@XmlElementWrapper(name="vulnerabilities")
	@XmlElement(name = "vulnerability")
	private List<VulnerabilityDescriptor> vulnerabilities;
	
	/**
	 * Constructor required by jaxb
	 */
	public MavenPackageDescriptor() {
		
	}
	
	public MavenPackageDescriptor(PackageDescriptor pkg) {
		groupId = pkg.getGroup();
		artifactId = pkg.getName();
		version = pkg.getVersion();
		vulnerabilityTotal = pkg.getVulnerabilityTotal();
		vulnerabilityMatches = pkg.getVulnerabilityMatches();
		vulnerabilities = pkg.getVulnerabilities();
	}

	public void setParent(MavenIdWrapper parent) {
		this.parent = parent;
	}
	
	public MavenIdWrapper getParent() {
		return parent;
	}
	
	/**
	 * Get the total number of vulnerabilities for the package identified on the server.
	 * @return Total number of vulnerabilities.
	 */
	public int getVulnerabilityTotal() {
		return vulnerabilityTotal;
	}

	/**
	 * Get the total number of vulnerabilities matching the supplied version.
	 * @return Number of matching vulnerabilities
	 */
	public int getVulnerabilityMatches() {
		return vulnerabilityMatches;
	}
	
	/**
	 * Get vulnerabilities belonging to this package.
	 */
	public List<VulnerabilityDescriptor> getVulnerabilities() {
		return vulnerabilities;
	}
}
