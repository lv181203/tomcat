package ex05.pyrmont.core;

import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.HashMap;
import javax.naming.directory.DirContext;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.apache.catalina.Cluster;
import org.apache.catalina.Container;
import org.apache.catalina.ContainerListener;
import org.apache.catalina.Context;
import org.apache.catalina.Loader;
import org.apache.catalina.Logger;
import org.apache.catalina.Manager;
import org.apache.catalina.Mapper;
import org.apache.catalina.Pipeline;
import org.apache.catalina.Realm;
import org.apache.catalina.Request;
import org.apache.catalina.Response;
import org.apache.catalina.Valve;
import org.apache.catalina.Wrapper;
import org.apache.catalina.deploy.ApplicationParameter;
import org.apache.catalina.deploy.ContextEjb;
import org.apache.catalina.deploy.ContextEnvironment;
import org.apache.catalina.deploy.ContextLocalEjb;
import org.apache.catalina.deploy.ContextResource;
import org.apache.catalina.deploy.ContextResourceLink;
import org.apache.catalina.deploy.ErrorPage;
import org.apache.catalina.deploy.FilterDef;
import org.apache.catalina.deploy.FilterMap;
import org.apache.catalina.deploy.LoginConfig;
import org.apache.catalina.deploy.NamingResources;
import org.apache.catalina.deploy.SecurityConstraint;
import org.apache.catalina.util.CharsetMapper;

/**
 *  Context容器
 *  1、容器可以包含一些支持的组件， 如载入器、记录器、管理器、领域、和资源。
 *    并且提供了get、set方法将这些组件与容器相关联
 *  2、一个servlet容器可以有一条管道, 当调用了容器的invoke() 后，容器会将处理工作交由管道完成。
 */
public class SimpleContext implements Context, Pipeline {

	//第一个添加的映射器会成为默认映射器
	protected Mapper mapper = null;
	//该容器可用的映射器
	protected HashMap mappers = new HashMap();

	protected HashMap servletMappings = new HashMap();
	protected Loader loader = null;

	private Container parent = null;
	protected HashMap children = new HashMap();

	protected SimplePipeline pipeline = new SimplePipeline(this);

	
	public SimpleContext() {
		pipeline.setBasic(new SimpleContextValve());
	}
	
	/**
	 * 添加子容器
	 */
	public void addChild(Container child) {
		//把该子容器的父容器设置为当前容器
		child.setParent((Container) this);
		children.put(child.getName(), child);
	}
	
	/**
	 * 添加一个URL模式/Wrapper实例的名称； 通过给定的名称添加用于调用Wrapper实例的每种模式。
	 */
	public void addServletMapping(String pattern, String name) {
		synchronized (servletMappings) {
			servletMappings.put(pattern, name);
		}
	}

	/**
	 * 通过URL模式查找对应的Wrapper实例名称。
	 * 该方法用来查找某个特殊URL模式对应的Wrapper实例。
	 * 如果给定的URL模式并没有使用addServletMapping()注册，测返回null
	 */
	public String findServletMapping(String pattern) {
		synchronized (servletMappings) {
			return ((String) servletMappings.get(pattern));
		}
	}

	public Loader getLoader() {
		if (loader != null)
			return (loader);
		if (parent != null)
			return (parent.getLoader());
		return (null);
	}

	public void setLoader(Loader loader) {
		this.loader = loader;
	}

	// method implementations of Pipeline
	public Valve getBasic() {
		return pipeline.getBasic();
	}

	public void setBasic(Valve valve) {
		pipeline.setBasic(valve);
	}

	public synchronized void addValve(Valve valve) {
		pipeline.addValve(valve);
	}

	public Valve[] getValves() {
		return pipeline.getValves();
	}

	public void removeValve(Valve valve) {
		pipeline.removeValve(valve);
	}

	/**
	 * 在Context容器中添加一个映射器。 
	 * 第一个添加的映射器会成为默认映射器。
	 */
	public void addMapper(Mapper mapper) {
		// this method is adopted from addMapper in ContainerBase
		// the first mapper added becomes the default mapper
		mapper.setContainer((Container) this); // May throw IAE
		this.mapper = mapper;
		
		synchronized (mappers) {
			if (mappers.get(mapper.getProtocol()) != null) {
				throw new IllegalArgumentException("addMapper:  Protocol '" + mapper.getProtocol() + "' is not unique");
			}
			
			mapper.setContainer((Container) this); // May throw IAE
			mappers.put(mapper.getProtocol(), mapper);
			
			if (mappers.size() == 1) {
				this.mapper = mapper;
			} else {
				this.mapper = null;
			}
		}
	}
	
	/**
	 * 找到正确的映射器 ，在本应用中返回默认映射器
	 */
	public Mapper findMapper(String protocol) {
		// the default mapper will always be returned, if any,
		// regardless the value of protocol
		if (mapper != null) {
			return (mapper);
		} else {
			synchronized (mappers) {
				return ((Mapper) mappers.get(protocol));
			}
		}
	}

	public Container findChild(String name) {
		if (name == null)
			return (null);
		synchronized (children) { // Required by post-start changes
			return ((Container) children.get(name));
		}
	}

	public Container[] findChildren() {
		synchronized (children) {
			Container results[] = new Container[children.size()];
			return ((Container[]) children.values().toArray(results));
		}
	}

	public void invoke(Request request, Response response) 
			throws IOException, ServletException {
		pipeline.invoke(request, response);
	}
	
	/**
	 *  返回处理当前请求的Wrapper实例
	 */
	public Container map(Request request, boolean update) {
		// this method is taken from the map method in
		// org.apache.cataline.core.ContainerBase
		// the findMapper method always returns the default mapper, if any,
		// regardless the
		// request's protocol
		Mapper mapper = findMapper(request.getRequest().getProtocol());
		if (mapper == null)
			return (null);

		// Use this Mapper to perform this mapping
		return (mapper.map(request, update));
	}

	public Mapper[] findMappers() {
		return null;
	}
	
	public Object[] getApplicationListeners() {
		return null;
	}

	public void setApplicationListeners(Object listeners[]) {
	}

	public boolean getAvailable() {
		return false;
	}

	public void setAvailable(boolean flag) {
	}

	public CharsetMapper getCharsetMapper() {
		return null;
	}

	public void setCharsetMapper(CharsetMapper mapper) {
	}

	public boolean getConfigured() {
		return false;
	}

	public void setConfigured(boolean configured) {
	}

	public boolean getCookies() {
		return false;
	}

	public void setCookies(boolean cookies) {
	}

	public boolean getCrossContext() {
		return false;
	}

	public void setCrossContext(boolean crossContext) {
	}

	public String getDisplayName() {
		return null;
	}

	public void setDisplayName(String displayName) {
	}

	public boolean getDistributable() {
		return false;
	}

	public void setDistributable(boolean distributable) {
	}

	public String getDocBase() {
		return null;
	}

	public void setDocBase(String docBase) {
	}

	public LoginConfig getLoginConfig() {
		return null;
	}

	public void setLoginConfig(LoginConfig config) {
	}

	public NamingResources getNamingResources() {
		return null;
	}

	public void setNamingResources(NamingResources namingResources) {
	}

	public String getPath() {
		return null;
	}

	public void setPath(String path) {
	}

	public String getPublicId() {
		return null;
	}

	public void setPublicId(String publicId) {
	}

	public boolean getReloadable() {
		return false;
	}

	public void setReloadable(boolean reloadable) {
	}

	public boolean getOverride() {
		return false;
	}

	public void setOverride(boolean override) {
	}

	public boolean getPrivileged() {
		return false;
	}

	public void setPrivileged(boolean privileged) {
	}

	public ServletContext getServletContext() {
		return null;
	}

	public int getSessionTimeout() {
		return 0;
	}

	public void setSessionTimeout(int timeout) {
	}

	public String getWrapperClass() {
		return null;
	}

	public void setWrapperClass(String wrapperClass) {
	}

	public void addApplicationListener(String listener) {
	}

	public void addApplicationParameter(ApplicationParameter parameter) {
	}

	public void addConstraint(SecurityConstraint constraint) {
	}

	public void addEjb(ContextEjb ejb) {
	}

	public void addEnvironment(ContextEnvironment environment) {
	}

	public void addErrorPage(ErrorPage errorPage) {
	}

	public void addFilterDef(FilterDef filterDef) {
	}

	public void addFilterMap(FilterMap filterMap) {
	}

	public void addInstanceListener(String listener) {
	}

	public void addLocalEjb(ContextLocalEjb ejb) {
	}

	public void addMimeMapping(String extension, String mimeType) {
	}

	public void addParameter(String name, String value) {
	}

	public void addResource(ContextResource resource) {
	}

	public void addResourceEnvRef(String name, String type) {
	}

	public void addResourceLink(ContextResourceLink resourceLink) {
	}

	public void addRoleMapping(String role, String link) {
	}

	public void addSecurityRole(String role) {
	}


	public void addTaglib(String uri, String location) {
	}

	public void addWelcomeFile(String name) {
	}

	public void addWrapperLifecycle(String listener) {
	}

	public void addWrapperListener(String listener) {
	}

	public Wrapper createWrapper() {
		return null;
	}

	public String[] findApplicationListeners() {
		return null;
	}

	public ApplicationParameter[] findApplicationParameters() {
		return null;
	}

	public SecurityConstraint[] findConstraints() {
		return null;
	}

	public ContextEjb findEjb(String name) {
		return null;
	}

	public ContextEjb[] findEjbs() {
		return null;
	}

	public ContextEnvironment findEnvironment(String name) {
		return null;
	}

	public ContextEnvironment[] findEnvironments() {
		return null;
	}

	public ErrorPage findErrorPage(int errorCode) {
		return null;
	}

	public ErrorPage findErrorPage(String exceptionType) {
		return null;
	}

	public ErrorPage[] findErrorPages() {
		return null;
	}

	public FilterDef findFilterDef(String filterName) {
		return null;
	}

	public FilterDef[] findFilterDefs() {
		return null;
	}

	public FilterMap[] findFilterMaps() {
		return null;
	}

	public String[] findInstanceListeners() {
		return null;
	}

	public ContextLocalEjb findLocalEjb(String name) {
		return null;
	}

	public ContextLocalEjb[] findLocalEjbs() {
		return null;
	}

	public String findMimeMapping(String extension) {
		return null;
	}

	public String[] findMimeMappings() {
		return null;
	}

	public String findParameter(String name) {
		return null;
	}

	public String[] findParameters() {
		return null;
	}

	public ContextResource findResource(String name) {
		return null;
	}

	public String findResourceEnvRef(String name) {
		return null;
	}

	public String[] findResourceEnvRefs() {
		return null;
	}

	public ContextResourceLink findResourceLink(String name) {
		return null;
	}

	public ContextResourceLink[] findResourceLinks() {
		return null;
	}

	public ContextResource[] findResources() {
		return null;
	}

	public String findRoleMapping(String role) {
		return null;
	}

	public boolean findSecurityRole(String role) {
		return false;
	}

	public String[] findSecurityRoles() {
		return null;
	}

	public String[] findServletMappings() {
		return null;
	}

	public String findStatusPage(int status) {
		return null;
	}

	public int[] findStatusPages() {
		return null;
	}

	public String findTaglib(String uri) {
		return null;
	}

	public String[] findTaglibs() {
		return null;
	}

	public boolean findWelcomeFile(String name) {
		return false;
	}

	public String[] findWelcomeFiles() {
		return null;
	}

	public String[] findWrapperLifecycles() {
		return null;
	}

	public String[] findWrapperListeners() {
		return null;
	}

	public void reload() {
	}

	public void removeApplicationListener(String listener) {
	}

	public void removeApplicationParameter(String name) {
	}

	public void removeConstraint(SecurityConstraint constraint) {
	}

	public void removeEjb(String name) {
	}

	public void removeEnvironment(String name) {
	}

	public void removeErrorPage(ErrorPage errorPage) {
	}

	public void removeFilterDef(FilterDef filterDef) {
	}

	public void removeFilterMap(FilterMap filterMap) {
	}

	public void removeInstanceListener(String listener) {
	}

	public void removeLocalEjb(String name) {
	}

	public void removeMimeMapping(String extension) {
	}

	public void removeParameter(String name) {
	}

	public void removeResource(String name) {
	}

	public void removeResourceEnvRef(String name) {
	}

	public void removeResourceLink(String name) {
	}

	public void removeRoleMapping(String role) {
	}

	public void removeSecurityRole(String role) {
	}

	public void removeServletMapping(String pattern) {
	}

	public void removeTaglib(String uri) {
	}

	public void removeWelcomeFile(String name) {
	}

	public void removeWrapperLifecycle(String listener) {
	}

	public void removeWrapperListener(String listener) {
	}

	// methods of the Container interface
	public String getInfo() {
		return null;
	}

	public Logger getLogger() {
		return null;
	}

	public void setLogger(Logger logger) {
	}

	public Manager getManager() {
		return null;
	}

	public void setManager(Manager manager) {
	}

	public Cluster getCluster() {
		return null;
	}

	public void setCluster(Cluster cluster) {
	}

	public String getName() {
		return null;
	}

	public void setName(String name) {
	}

	public Container getParent() {
		return null;
	}

	public void setParent(Container container) {
	}

	public ClassLoader getParentClassLoader() {
		return null;
	}

	public void setParentClassLoader(ClassLoader parent) {
	}

	public Realm getRealm() {
		return null;
	}

	public void setRealm(Realm realm) {
	}

	public DirContext getResources() {
		return null;
	}

	public void setResources(DirContext resources) {
	}

	public ContainerListener[] findContainerListeners() {
		return null;
	}
	
	public void addContainerListener(ContainerListener listener) {
	}
	
	public void addPropertyChangeListener(PropertyChangeListener listener) {
	}
	
	public void removeChild(Container child) {
	}

	public void removeContainerListener(ContainerListener listener) {
	}

	public void removeMapper(Mapper mapper) {
	}

	public void removePropertyChangeListener(PropertyChangeListener listener) {
	}


}