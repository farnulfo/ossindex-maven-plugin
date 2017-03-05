package net.ossindex.maven.utils;

import java.util.Collection;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

/** Simple class used to wrap the results prior to export as XML.
 * 
 * @author Ken Duck
 *
 */
@XmlRootElement(name = "OssIndex")
public class OssIndexResultsWrapper {

	private Collection<MavenPackageDescriptor> packages;
	
	public OssIndexResultsWrapper() {
		
	}

	public OssIndexResultsWrapper(Collection<MavenPackageDescriptor> results) {
		this.setPackages(results);
	}

	public Collection<MavenPackageDescriptor> getPackages() {
		return packages;
	}

	@XmlElementWrapper(name="packages")
	@XmlElement(name = "package")
	public void setPackages(Collection<MavenPackageDescriptor> packages) {
		this.packages = packages;
	}

}
