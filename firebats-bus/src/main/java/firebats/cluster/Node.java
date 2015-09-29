package firebats.cluster;

import java.util.UUID;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

import firebats.net.Uri;

public class Node {
    private String uri;
	private String nodeId;
	private String host;
	/*for serialize*/private Node(){}
	private Node(String host){
		this.nodeId=UUID.randomUUID().toString();
    	this.host=host;
    }
	public Node withUri(Uri uri){
		Preconditions.checkNotNull(uri);
		this.uri=uri.toString();
		return this;
	}
	public Node withId(String nodeId){
		Preconditions.checkNotNull(nodeId);
		this.nodeId=nodeId;
		return this;
	}
	public Node withHost(String host) {
		Preconditions.checkNotNull(host);
		this.host=host;
		return this;
	}

	public String getHost() {
		return host;
	}
	public static Node newNode(String host){
		return new Node(host);
	}
	public String getNodeId() {
		return nodeId;
	}
	public Uri getUri(){
		return Uri.parse(uri);
	}
	
	public String getAddress() {
		return uri;
	}
	
	@Override
	public boolean equals(Object other) {
		if(this==other)return true;
		if(!(other instanceof Node)) return false;
		if(!canEqual(other)) return false;
		
		Node that = (Node) other;
		return Objects.equal(this.uri, that.uri)
				&&Objects.equal(this.nodeId, that.nodeId);
	}
	public boolean canEqual(Object other) {
		return (other instanceof Node);
	}
    @Override
    public final int hashCode() {
        return Objects.hashCode(uri);
    }
    @Override
    public String toString() {
    	return MoreObjects.toStringHelper(this)
    			.add("id",nodeId)
    			.add("uri",uri).toString();
    }
}