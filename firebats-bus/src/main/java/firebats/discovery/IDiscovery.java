package firebats.discovery;

import java.util.Collection;

import com.google.common.base.Optional;

import firebats.net.Uri;

public interface IDiscovery {
	public abstract void register(Service service);
	public abstract Service newService(String name,Uri uri);
	public abstract void unregister(String serviceId);
	public abstract Optional<Service> select(String serviceName);
	public abstract Collection<Service> getAll(String serviceName);
}