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

import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import net.ossindex.common.resource.ScmResource;
import net.ossindex.common.utils.PackageDependency;

import org.junit.Ignore;
import org.junit.Test;

/** Test the dependency auditor
 * 
 * @author Ken Duck
 *
 */
public class DependencyAuditorTests
{
	@Test
	public void testCommonsLang3() throws IOException
	{
		DependencyAuditor auditor = new DependencyAuditor();
		PackageDependency dep = new PackageDependency("maven", "commons-lang3", "3.4");
		auditor.setDependencyInformation(new PackageDependency[] {dep});
		ScmResource scm = dep.getScm();
		assertNotNull(scm);
	}
	@Test
	public void testGoogleCollectRange() throws IOException
	{
		DependencyAuditor auditor = new DependencyAuditor();
		PackageDependency dep = new PackageDependency("maven", "google-collect", ">0");
		auditor.setDependencyInformation(new PackageDependency[] {dep});
		ScmResource scm = dep.getScm();
		assertNotNull(scm);
	}
	@Test
	@Ignore
	public void testGoogleCollect() throws IOException
	{
		DependencyAuditor auditor = new DependencyAuditor();
		PackageDependency dep = new PackageDependency("maven", "google-collect", "snapshot-20080530");
		auditor.setDependencyInformation(new PackageDependency[] {dep});
		ScmResource scm = dep.getScm();
		assertNotNull(scm);
	}
}
