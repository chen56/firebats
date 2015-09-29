package firebats.internal.cluster;

import java.util.Collection;
import java.util.concurrent.ConcurrentMap;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.utils.CloseableUtils;
import org.apache.curator.x.discovery.ServiceDiscovery;
import org.apache.curator.x.discovery.ServiceDiscoveryBuilder;
import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.curator.x.discovery.ServiceProvider;
import org.apache.curator.x.discovery.ServiceType;
import org.apache.curator.x.discovery.details.InstanceSerializer;
import org.apache.curator.x.discovery.strategies.RoundRobinStrategy;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.Collections2;
import com.google.common.collect.Maps;

import firebats.cluster.Node;
import firebats.component.Component;
import firebats.component.ComponentContainer;
import firebats.component.IComponent;
import firebats.component.IComponentProvider;
import firebats.component.funcs.IStart;
import firebats.component.funcs.IStop;
import firebats.discovery.IDiscovery;
import firebats.discovery.Service;
import firebats.json.Jackson;
import firebats.net.Uri;
import firebats.reflect.TypeRef;


public class ZkDiscovery implements IDiscovery,IComponentProvider {
	private ConcurrentMap<String, ServiceProvider<Service>> providers = Maps.newConcurrentMap();
	private ConcurrentMap<String, ServiceInstance<Service>> localServices_id_serviceInstance = Maps.newConcurrentMap();

	private ServiceDiscovery<Service> discovery;
	private Node owner;
    private ComponentContainer container=Component.newContainer(this);

	/*internal*/public ZkDiscovery(Node owner,CuratorFramework client,String basePath) {
		this.owner=owner;
        discovery = ServiceDiscoveryBuilder.builder(Service.class)
        		.basePath(basePath)
        		.client(client)
        		.serializer(new InstanceSerializer<Service>() {
					@Override public byte[] serialize(ServiceInstance<Service> instance)
							throws Exception {
						return Jackson.normal().encodeToByte(instance);
					}
					@Override public ServiceInstance<Service> deserialize(byte[] bytes)
							throws Exception {
						return Jackson.normal().decode(bytes, new TypeRef<ServiceInstance<Service>>(){});
					}
				})
        		.build();
        
        container.add(
            	Component.newComponent(discovery)
            	.on(IStart.class,new IStart(){
    				@Override public void start() throws Exception {
    					discovery.start();
    				}
    			})
            	.on(IStop.class,new IStop(){
    				@Override public void stop() throws Exception {
    					discovery.close();
    				}
    			})
            );
        container.add(
            	Component.newComponent(providers)
            	.on(IStop.class,new IStop(){
    				@Override public void stop() throws Exception {
    					try {
    						for (ServiceProvider<Service> p : providers.values()) {
    				            CloseableUtils.closeQuietly(p);
    						}
    					} finally {
    						providers.clear();
    			 		}
    				}
    			})
            );
	}
	
	public Service newService(String name, Uri uri){
		return Service.create(owner.getNodeId(), name, uri);
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

	@Override public void register(Service service) {
		try {
 			ServiceInstance<Service> serviceInstance=ServiceInstance
					.<Service>builder()
					.name(service.getName())
					.id(service.getServiceId())
					.serviceType(ServiceType.DYNAMIC)
					.payload(service)
					.build();
			discovery.registerService(serviceInstance);
			localServices_id_serviceInstance.put(service.getServiceId(), serviceInstance);
		} catch (Exception e) {
			throw new RuntimeException(e);
 		}
	}

	@Override public void unregister(String serviceId) {
		ServiceInstance<Service> serviceInstance=localServices_id_serviceInstance.get(serviceId);
		if(serviceInstance==null){
			return;
		}
		try {
			discovery.unregisterService(serviceInstance);
			localServices_id_serviceInstance.remove(serviceId);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override public Optional<Service> select(String serviceName) {
        ServiceProvider<Service> provider = _getOrLoadProvider(serviceName);
		try {
			ServiceInstance<Service> serviceInstance = provider.getInstance();
			Service result=serviceInstance==null?null:serviceInstance.getPayload();
			return result==null? Optional.<Service>absent():Optional.of(result);
		} catch (Exception e) {
			Throwables.propagate(e);
		}
		//never
 		return null;
	}
	@Override public Collection<Service> getAll(String serviceName) {
 		try {
			return Collections2.transform(_getAllServiceInstances(serviceName), new Function<ServiceInstance<Service>,Service>(){
				@Override public Service apply(ServiceInstance<Service> input) {
					return input.getPayload();
				}
			});
		} catch (Exception e) {
			Throwables.propagate(e);
		}
		//never
 		return null;
	}
	
	private Collection<ServiceInstance<Service>> _getAllServiceInstances(String serviceName) {
 		try {
			return _getOrLoadProvider(serviceName).getAllInstances();
		} catch (Exception e) {
			Throwables.propagate(e);
		}
		//never
 		return null;
	}

	private ServiceProvider<Service> _getOrLoadProvider(String serviceName) {
		ServiceProvider<Service>    provider = providers.get(serviceName);
        if ( provider == null )
        {
        	ServiceProvider<Service> newProvider = discovery.serviceProviderBuilder().serviceName(serviceName).providerStrategy(new RoundRobinStrategy<Service>()).build();
        	ServiceProvider<Service> oldProvider = providers.putIfAbsent(serviceName,newProvider);
        	if(oldProvider==null){
            	provider = newProvider;
                try {
    				provider.start();
    			} catch (Exception e) {
    				Throwables.propagate(e);
    			}
        	}else{
            	provider = oldProvider;
        	}
        }
		return provider;
	}
}