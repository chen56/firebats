package firebats.facade;

import org.apache.curator.RetrySleeper;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.CuratorEvent;
import org.apache.curator.framework.api.CuratorListener;
import org.apache.curator.framework.api.UnhandledErrorListener;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.curator.retry.RetryNTimes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.net.InetAddresses;

import firebats.bus.RxBus;
import firebats.cluster.Node;
import firebats.component.Component;
import firebats.component.ComponentContainer;
import firebats.component.IComponent;
import firebats.component.IComponentProvider;
import firebats.component.funcs.IStart;
import firebats.component.funcs.IStop;
import firebats.discovery.IDiscovery;
import firebats.internal.cluster.ZkDiscovery;
import firebats.net.ConnectionString;
import firebats.net.InterfaceAddresses;
import firebats.net.Path;
/**
 * 集群模块的facade</p> 
 * Firebat表现一个firebats分布式节点
 * 
 * 暂时未实现Node控制
 */
public class Firebat implements IComponentProvider{
	private CuratorFramework    curator;
	private ConnectionString zkConnString;
	private ZkDiscovery discovery;
	private Node localNode;
	private RxBus bus;
    private ComponentContainer container=Component.newContainer(this);
    private static Logger log=LoggerFactory.getLogger(Firebat.class);
    public static class MyRetryNTimes extends RetryNTimes  {
        public MyRetryNTimes(int n, int sleepMsBetweenRetries){
            super(n,sleepMsBetweenRetries);
        }
        @Override
        public boolean allowRetry(int retryCount, long elapsedTimeMs,
        		RetrySleeper sleeper) {
         	return super.allowRetry(retryCount, elapsedTimeMs, sleeper);
        }
    }

	private Firebat(FirebatBuilder builder) {
		Preconditions.checkNotNull(builder);
		Preconditions.checkNotNull(builder.zkConnString);
		this.localNode=builder.localNode;
		this.zkConnString=builder.zkConnString;
		curator=CuratorFrameworkFactory.builder().
        connectString(zkConnString.getHostPortsString()).
        sessionTimeoutMs(10*1000).
        connectionTimeoutMs(10*1000).
        retryPolicy(new MyRetryNTimes(Integer.MAX_VALUE,5*1000)).
        build();
		curator.getCuratorListenable().addListener(new CuratorListener() {
			@Override
			public void eventReceived(CuratorFramework client, CuratorEvent event)
					throws Exception {
	        	log.debug("zk:"+event);
			}
		});
		curator.getConnectionStateListenable().addListener(new ConnectionStateListener() {
			@Override
			public void stateChanged(CuratorFramework client, ConnectionState newState) {
	        	log.info("zk:"+newState);
			}
		});
		curator.getUnhandledErrorListenable().addListener(new UnhandledErrorListener() {
			
			@Override
			public void unhandledError(String message, Throwable e) {
	        	log.info("zk:"+message,e);
			}
		});
        container.add(
        	Component.newComponent(curator)
        	.on(IStart.class,new IStart(){
				@Override public void start() throws Exception {
					curator.start();
				}
			})
        	.on(IStop.class,new IStop(){
				@Override public void stop() throws Exception {
					curator.close();
				}
			})
        );
        
		discovery=new ZkDiscovery(localNode,curator,Path.fromPortableString(zkConnString.getPath()).append("services").toPortableString());
        container.add(discovery);
        
        bus=RxBus.builder().discovery(getDiscovery()).address(localNode.getHost(), InterfaceAddresses.getRandomPort()).build();
        container.add(bus);

	}
	public static FirebatBuilder builder(ConnectionString zkConnString){
		return new FirebatBuilder(zkConnString);
	}
	public static Firebat newInstance(ConnectionString zkConnString) {
		return builder(zkConnString).build();
	}

	@Override public IComponent getComponent() {
 		return container;
	}

    public void start(){
    	container.start();
    }
    
    public void stop(){
    	container.stop();
    }
	public RxBus getBus() {
		return bus;
	}

	public IDiscovery getDiscovery() {
		return discovery;
	}
	
	public Node getLocalNode() {
		return localNode;
	}
	
	public ConnectionString getZkConnString() {
		return zkConnString;
	}
	
	public static class FirebatBuilder{
		private ConnectionString zkConnString;
		private Node localNode;

		public FirebatBuilder(ConnectionString zkConnString){
			this.zkConnString=zkConnString;
		}
		/**可选提供，若不提供则自动猜测本机ip*/
		public FirebatBuilder host(String host){
			Preconditions.checkArgument(InetAddresses.isInetAddress(host));
			this.localNode=Node.newNode(host);
			return this;
 		}
		public Firebat build(){
			Preconditions.checkNotNull(localNode,"not config host");
			return new Firebat(this);
		}
		public Firebat buildAndStart(){
			Firebat result= build();
			result.start();
			return result;
		}
		public ConnectionString getZkConnString() {
			return zkConnString;
		}
		public Node getLocalNode() {
			return localNode;
		}
	}

}