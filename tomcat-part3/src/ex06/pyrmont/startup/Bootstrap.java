package ex06.pyrmont.startup;

import ex06.pyrmont.core.SimpleContext;
import ex06.pyrmont.core.SimpleContextLifecycleListener;
import ex06.pyrmont.core.SimpleContextMapper;
import ex06.pyrmont.core.SimpleLoader;
import ex06.pyrmont.core.SimpleWrapper;
import org.apache.catalina.Connector;
import org.apache.catalina.Context;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Loader;
import org.apache.catalina.Mapper;
import org.apache.catalina.Wrapper;
import org.apache.catalina.connector.http.HttpConnector;

public final class Bootstrap {
	
	
	public static void main(String[] args) {
		//TOMCAT默认连接器
		Connector connector = new HttpConnector();
	
		Wrapper wrapper1 = new SimpleWrapper();
		wrapper1.setName("Primitive");
		wrapper1.setServletClass("PrimitiveServlet");
		
		Wrapper wrapper2 = new SimpleWrapper();
		wrapper2.setName("Modern");
		wrapper2.setServletClass("ModernServlet");

		//创建Context容器,并添加2个子容器
		Context context = new SimpleContext();
		context.addChild(wrapper1);
		context.addChild(wrapper2);

		Mapper mapper = new SimpleContextMapper();
		mapper.setProtocol("http");
		
		//添加事件监听器
		LifecycleListener listener = new SimpleContextLifecycleListener();
		((Lifecycle) context).addLifecycleListener(listener);
		
		context.addMapper(mapper);
		
		Loader loader = new SimpleLoader();
		context.setLoader(loader);
		
		// context.addServletMapping(pattern, name);
		context.addServletMapping("/Primitive", "Primitive");
		context.addServletMapping("/Modern", "Modern");
		
		connector.setContainer(context);
		
		try {
			connector.initialize();
			((Lifecycle) connector).start();
			
			//启动Lifecycle
			((Lifecycle) context).start();

			// make the application wait until we press a key.
			System.in.read();
			
			//关闭Lifecycle
			((Lifecycle) context).stop();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}