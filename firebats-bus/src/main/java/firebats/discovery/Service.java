package firebats.discovery;

import java.util.UUID;

import com.google.common.base.Objects;

import firebats.net.Uri;

public class Service{
 	private String uri;
	private String name;
	private String serviceId;
 	private String nodeId;

	/*for serialize*/private Service(){}
 	private Service(String nodeId, String name, String id,String uri) {
 		this.nodeId=nodeId;
		this.name=name;
		this.uri=uri;
		this.serviceId=id;
 	}
 	
	public static Service create(String nodeId, String name, Uri uri) {
 		return new Service(nodeId,name,UUID.randomUUID().toString(),uri.toString());
	}
	
	public String getServiceId() {
		return serviceId;
	}

 	public String getName() {
		return name;
	}
 	
	public Uri getUri() {
		return Uri.parse(uri);
	}
	public String getNodeId() {
		return nodeId;
	}

	@Override
	public boolean equals(Object other) {
		if(this==other)return true;
		if(!(other instanceof Service)) return false;
		if(!canEqual(other)) return false;
		
		Service that = (Service) other;
		return Objects.equal(this.name, that.name)
				 &&Objects.equal(this.uri, that.uri)
			 &&Objects.equal(this.serviceId, that.serviceId)
			 &&Objects.equal(this.nodeId, that.nodeId)
			 ;
	}
	
	public boolean canEqual(Object other) {
		return (other instanceof Service);
	}
	
    @Override
    public final int hashCode() {
        return Objects.hashCode(serviceId,name,uri,nodeId);
    }
    
    @Override
    public String toString() {
    	return Objects.toStringHelper(this)
    			.add("name",name)
    			.add("id",serviceId)
    			.add("uri",uri)
    			.add("nodeId",nodeId)
    			.toString();
    }

}