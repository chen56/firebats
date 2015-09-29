package firebats.discovery.memory;

import java.util.Collection;
import java.util.concurrent.ConcurrentMap;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.collect.Maps;

import firebats.cluster.Node;
import firebats.discovery.IDiscovery;
import firebats.discovery.Service;
import firebats.net.InterfaceAddresses;
import firebats.net.Uri;
/**
 * 内存实现，主要用来测试
 */
public class MemoryDiscovery  implements IDiscovery{
	public void start(){
		
	}
	public void close(){
		
	}
	private ConcurrentMap<String, Service> localServices_id_service = Maps.newConcurrentMap();
    private Node localNode=Node.newNode(InterfaceAddresses.guessIpAddress().get().getHostAddress());
    
	@Override public Service newService(String name, Uri uri) {
		return Service.create(localNode.getNodeId(), name, uri);
	}

	@Override public void register(Service service) {
 		localServices_id_service.put(service.getName(), service);
	}

	@Override public void unregister(String serviceId) {
		localServices_id_service.remove(serviceId);
	}

	@Override public Optional<Service> select(String serviceName) {
		for (Service s : localServices_id_service.values()) {
			if(Objects.equal(s.getName(),serviceName)){
				return Optional.of(s);
			}
		}
		return Optional.absent();
	}

	@Override public Collection<Service> getAll(String serviceName) {
		return localServices_id_service.values();
	}
}