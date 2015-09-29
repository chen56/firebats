package firebats.bus;

import java.util.concurrent.atomic.AtomicLong;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

import firebats.check.CheckError;
import firebats.internal.bus.json.DefaultMessageMapper;
import firebats.reflect.TypeRef;
/**
 * Message  =  envelope + body + [error]
 * envelope = Correlation Identifier + Channel Name
 * error = code + message + detail
 * <p/>
 * 不隐藏作为信封概念的Message，用以表明面向协议而非面向框架的设计思路
 * <p/>
 * 术语：
 * <ul>
 * <li>request: 请求，有reply的消息</li>
 * <li>message: 泛指消息，有时也指没有reply的消息</li>
 * </ul>
 * <p/>
 * 此类不能被客户程序继承
 * <p/>
 * 此类线程安全
 */
public final class Message<BODY> implements java.io.Serializable {
	private static final long serialVersionUID = 7574105737853226878L;
	
	public static final NoReply NoReply = new NoReply();
	private static AtomicLong cidSequence=new AtomicLong(0);
	/**
	 * Correlation Identifier ref:
	 * http://www.eaipatterns.com/CorrelationIdentifier.html
	 */
	private Long cid;
	
	/** channel */
	private String c;
	
	/**
	 * 暂时不需要Message Identifier 但保留名称：id
	 */
	@SuppressWarnings("unused")
	private transient String id = null;

	/** message == body */
	private BODY data;

	/*use for json mapper*/
	private Message(){}
	
	private CheckError error;
	
	private String langRanges;
	
	/*internal*/ Message(String channel,Long cid,BODY body,CheckError error) {
		this.data=body;
		this.cid=cid;
		this.c=channel;
 		this.error=error;
 	}

	public static <BODY> Message<BODY> request(String channel,BODY msg){
		return new Message<>(channel,nextCid(),msg,null);
	}
	
	public static <BODY> Message<BODY> message(String channel,BODY msg){
		return new Message<>(channel,null,msg,null);
	}
	
	public Message<BODY> withError(CheckError error) {
		Message<BODY> result=copy();
		result.error=error;
		return result;
	}
	
	public Message<BODY> withCorrelationId(Long cid) {
		Message<BODY> result=copy();
		result.cid=cid;
		return result;
	}
	
	public Message<BODY> withLangRanges(String langRanges) {
		Message<BODY> result=copy();
		result.langRanges=langRanges;
		return result;
	}

	private Message<BODY> copy() {
		Message<BODY> result=new Message<BODY>();
		result.c=c;
		result.cid=cid;
		result.data=data;
		result.error=error;
		result.langRanges=langRanges;
		return result;
	}

	//cid只需保证一个bus实例内唯一即可，因为reply会回复到本bus
	private static Long nextCid() {
 		return cidSequence.getAndIncrement();
	}

	public BODY getBody() {
		return data;
	}

	public Long getCorrelationId() {
		return cid;
	}

	public String getChannel() {
		return c;
	}
	
  	public CheckError getError() {
		return error;
	}
  	
	public boolean isError() {
 		return error!=null;
	}

 	public boolean isReplyable() {
		return cid != null;
	}

	@Override
	public String toString() {
		return Objects.toStringHelper("Message")
				.add("c", c)
				.add("cid", cid)
				.add("error", error)
				.add("b", data)
				.toString();
	}

	/**
	 * 工厂方法,request消息的规格定义<p/>
	 * @return 制造一个MessageSpec<BODY,REPLY>的消息定义
	 **/
	public static <BODY,REPLY>  MessageSpec<BODY,REPLY> newRequestSpec(String channel, Class<BODY> requestClass,Class<REPLY> replyClass) {
		return new MessageSpec<>(channel,DefaultMessageMapper.of(requestClass),DefaultMessageMapper.of(replyClass));
	}

	/**
	 * 工厂方法,request消息的规格定义<p/>
	 * @return 制造一个MessageSpec<BODY,REPLY>的消息定义
	 **/
	public static <BODY,REPLY>  MessageSpec<BODY,REPLY> newRequestSpec(String channel, TypeRef<BODY> requestClass,TypeRef<REPLY> replyClass) {
		return new MessageSpec<>(channel,DefaultMessageMapper.of(requestClass),DefaultMessageMapper.of(replyClass));
	}

	/**
	 * 工厂方法,message消息的规格定义<p/>
	 * @return 制造一个MessageSpec<BODY,NoReply>的消息定义
	 **/
	public static <BODY>  MessageSpec<BODY,NoReply> newMessageSpec(String channel, Class<BODY> requestClass) {
		return new MessageSpec<>(channel,DefaultMessageMapper.of(requestClass),MessageSpec.NoReplyMapper);
	}
	/**
	 * 工厂方法,message消息的规格定义<p/>
	 * @return 制造一个MessageSpec<BODY,NoReply>的消息定义
	 **/
	public static <BODY>  MessageSpec<BODY,NoReply> newMessageSpec(String channel, TypeRef<BODY> requestClass) {
		return new MessageSpec<>(channel,DefaultMessageMapper.of(requestClass),MessageSpec.NoReplyMapper);
	}

	public static interface MessageMapper<BODY>{
		String encode(Message<BODY> obj);
		Message<BODY> decode(String text);
		TypeRef<BODY> getBodyType();
	}
	
	public static interface IMessageSpecProvider<BODY,REPLY> {
		MessageSpec<BODY,REPLY> getMessageSpec();
	}

	/**
	 * 此类描述消息的：
	 * <ul>
	 * <ul>
	 *   <li>定义channel name</li>
	 *   <li>定义body和reply body的类型化信息</li>
	 *   <li>提供消息的序列化功能</li>
	 * </ul>
 	 * <p/>
	 * 此类不应被客户程序继承
	 */
	public final static class MessageSpec<BODY,REPLY> {
		private static MessageMapper<NoReply> NoReplyMapper=new MessageMapper<NoReply>(){
			@Override
			public String encode(Message<NoReply> message) {
				Preconditions.checkState(false,"NoReply 无法序列化");
				return null;
			}
			@Override
			public Message<NoReply> decode(String text) {
				Preconditions.checkState(false,"NoReply 无法反序列化");
				return null;
			}
			@Override public TypeRef<firebats.bus.Message.NoReply> getBodyType() {
				return new TypeRef<firebats.bus.Message.NoReply>(){};
			}};
		private final String channel;
		
		private MessageMapper<BODY> requestMapper; 
		private MessageMapper<REPLY> replyMapper; 
 		private MessageSpec(String channel, MessageMapper<BODY> requestMapper,MessageMapper<REPLY> replyMapper) {
			this.channel = channel;
			this.requestMapper=requestMapper;
			this.replyMapper=replyMapper;
		}

 		public Message<BODY> newRequest(BODY body){
 			return Message.request(channel, body);
 		}
		public String getChannel() {
			return channel;
		}
		/**
		 * @return 是否是无返回，即返回类型为NoReply
		 **/
		public boolean isNoReply() {
			return replyMapper==NoReplyMapper;
		}

		/*internal*/ MessageMapper<BODY> getRequestMapper() {
			return requestMapper;
		}

		/*internal*/ MessageMapper<REPLY> getReplyMapper() {
			return replyMapper;
		}

		public TypeRef<REPLY> getReplyType() {
			return replyMapper.getBodyType();
		}
		public TypeRef<BODY> getRequestType() {
			return requestMapper.getBodyType();
		}
	}
	
	public final static class NoReply{
		private NoReply(){}
	}

}