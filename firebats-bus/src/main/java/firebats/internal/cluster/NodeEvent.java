//package firebats.internal.cluster;
//
//import firebats.cluster.Node;
//
//public class NodeEvent {
//	private NodeEventType changedType;
//	private Node server;
//    
//	private NodeEvent(NodeEventType changedType,Node server) {
//		this.changedType=changedType;
//		this.server=server;
// 	}
//
//	public static NodeEvent create(NodeEventType changedType,Node node){
//		NodeEvent result=new NodeEvent(changedType,node);
//		return result;
//	}
//	
//	public NodeEventType getChangedType(){
//    	return changedType;
//    } 
//	
//    public Node getServer(){
//    	return server;
//    } 
//    
//    public static enum NodeEventType {
//    	Updated, Created, Deleted
//    }
//}