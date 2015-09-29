package firebats.component;


import org.junit.Test;

import firebats.funcs.throwable.EAction1;

public class ComponentTest {
	@Test
    public void test(){
		System.out.println(new ClusterFixture().getComponent());
		System.out.println("ssssssss");
    	ClusterFixture app=new ClusterFixture();
    	System.out.println(app.getComponent());
    	System.out.println(((ComponentContainer)app.getComponent()).reverse());
    	app.start();
    	app.stop();
    }
    private static interface IStart extends IComponentFunc{
    	public void start();
    }
//    @ComponentFuncInfo(ignoreException=true,reverseCall=true)
    private static interface IStop extends IComponentFunc{
    	public void stop();
    }
   public static class ClusterFixture implements IStart,IStop,IComponentProvider{
    	private ServiceDiscovery discovery;
    	private ZkServer zkserver=new ZkServer();
		ComponentContainer container=Component.newContainer(this); 
		public ClusterFixture(){
       		Component zkComponent=Component.newComponent(zkserver)
 			.on(IStart.class,new IStart(){
				@Override public void start() {
					zkserver.start();
				}  
    		}).on(IStop.class,new IStop(){
				@Override public void stop() {
					zkserver.stop();
				}
    		});
    		this.discovery=new ServiceDiscovery();
    		this.container.add(zkComponent);
    		this.container.add(this.discovery);
    	}
		public void start() {
			container.start();
 		}
		public void stop() {
			container.stop();
 		}
		@Override public IComponent getComponent() {
			return container;
		}
    }
    public static class ServiceDiscovery implements IStart,IStop,IComponentProvider{  
		ComponentContainer container=Component.newContainer(this); 
		Zk zk=new Zk("ServiceDiscovery");
		Bus bus=new Bus();

		public ServiceDiscovery(){
      		Component zkComponent=Component.newComponent(zk)
 			.on(IStart.class,new IStart(){
				@Override public void start() {
					zk.start();
				}  
    		}).on(IStop.class,new IStop(){
				@Override public void stop() {
					zk.stop();
				}
    		});
      		Component busComponent=Component.newComponent(bus)
 			.on(IStart.class,new IStart(){
				@Override public void start() {
					bus.start();
				}  
    		}).on(IStop.class,new IStop(){
				@Override public void stop() {
					bus.stop();
				}
    		});
      		
      		container.add(zkComponent);
      		container.add(busComponent);
    	}
		public void start() {
			container.start();
 		}
		public void stop() {
			container.stop();
 		}
		@Override public IComponent getComponent() {
			return container;
		}    
	}
    public static class Zk{
    	private String uri;
		public Zk(String uri){
    		this.uri=uri;
    	}
		public void start() {
			System.out.println(uri+" zk start");
		}

		public void stop() {
			System.out.println(uri+" zk stop");
		}
    }
    public static class ZkServer{
    	private String uri;
		public void start() {
			System.out.println(uri+" ZkServer start");
		}
		public void stop() {
			System.out.println(uri+" ZkServer stop");
		}
    }

    public static class Bus{
		public Bus(){
    	}
		public void start() {
			System.out.println(" Bus start");
		}

		public void stop() {
			System.out.println(" Bus stop");
		}
    }
}