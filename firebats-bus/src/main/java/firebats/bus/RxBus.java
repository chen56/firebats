package firebats.bus;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.Observable;
import rx.Observer;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subjects.AsyncSubject;
import firebats.bus.BusMonitor.ServerMonitor;
import firebats.bus.Message.IMessageSpecProvider;
import firebats.bus.Message.MessageSpec;
import firebats.bus.exceptions.BusException;
import firebats.check.Check;
import firebats.check.Check.GeneralException;
import firebats.check.CheckError;
import firebats.check.CheckException;
import firebats.component.IComponent;
import firebats.component.IComponentProvider;
import firebats.discovery.IDiscovery;
import firebats.internal.bus.RawBus;
import firebats.net.Uri;

/**
 * 
 * <p/>
 * 此类不应被客户程序继承
 * <p/>
 * 此类线程安全
 */
public final class RxBus implements IComponentProvider{
	protected RawBus rawBus;
	private SyncBus syncBus;
	private static Logger log=LoggerFactory.getLogger(RxBus.class);
	private RxBus(RawBus rawBus) {
		this.rawBus = rawBus;
		this.syncBus=new SyncBus(this);
	}
	/**bus uri是固定的，可以用此方法获取*/
	public static Uri newBusUri(String host, int port) {
		return Uri.parse(String.format("bus://%s:%s/bus",host,port));
	}

	public static Builder builder() {
		return new Builder();
	}

	@Override public IComponent getComponent() {
 		return rawBus.getComponent();
	}
	public void start() {
		rawBus.start();
	}
	public void startAndWait() {
		rawBus.startAndWait();
	}

	public void stop() {
		rawBus.stop();
	}
	public long getTimeout() {
		return rawBus.getTimeout();
	}
	public TimeUnit getTimeoutTimeUnit() {
		return rawBus.getTimeoutTimeUnit();
	}

	public SyncBus toSyncBus(){
		return syncBus;
	}
	public <BODY,REPLY> void receive(final MessageSpec<BODY,REPLY> spec,final Func1<BODY, Observable<REPLY>> onReceive) {
		rawBus.receive(spec.getChannel())		
		.observeOn(Schedulers.io())
		.subscribeOn(Schedulers.io())
        .subscribe(new Action1<IRawRequest>() {
			@Override
			public void call(final IRawRequest rawRequest) {
				try {
					Message<BODY> m=spec.getRequestMapper().decode(rawRequest.getMessage());
					final RequestWarpper<BODY,REPLY> parsedMessage=new RequestWarpper<BODY, REPLY>(spec,m,rawRequest);
					
					Observable<REPLY> result=null;
					try {
						result=onReceive.call(m.getBody());
					} catch (Throwable e) {
						if(!(e instanceof CheckException)){
							log.error("receive: MessageProcessor call fail",e);
						}
						parsedMessage.replyFail(e);
					}
					if(result==null){
						return;
					}
					
					result
                    .subscribe(new Action1<REPLY>() {
						@Override
						public void call(REPLY reply) {
							parsedMessage.reply(reply);
 						}
					},new Action1<Throwable>() {
						@Override
						public void call(Throwable e) {
							if(!(e instanceof CheckException)){
								RxBus.log.error("receive: reply fail",e);
							}
							parsedMessage.replyFail(e);
						}
					});
				} catch (Throwable e) {
					RxBus.log.error("receive.subscribe",e); 
				}
			}
 		}) ;		
	}
	public <BODY extends IMessageSpecProvider<BODY,REPLY>,REPLY> Observable<Void> send(BODY body) {
	    return send(body.getMessageSpec(), body);
    }

	public <BODY,REPLY> Observable<Void> send(final MessageSpec<BODY,REPLY> spec,BODY body) {
			String requestText = spec.getRequestMapper().encode(Message.message(spec.getChannel(),body));
		log.debug("send : " + requestText);
		return rawBus.send(spec.getChannel(), requestText);
	}
	public <TBody,TReply> Observable<Message<TReply>> request(final MessageSpec<TBody,TReply> spec, Message<TBody> request) {
		if(spec.isNoReply()){
			return Observable.error(new BusException("messageSpec的返回类型为NoReply,不能使用request/reply模式发送消息"));
		}
		final String requestText = spec.getRequestMapper().encode(request);
		log.debug("request -: " + requestText);
		
 		final AsyncSubject<Message<TReply>> result=AsyncSubject.create();
 		rawBus.request(spec.getChannel(), request.getCorrelationId(),requestText)
 		.subscribe(new Observer<String>() {
 			@Override public void onCompleted() {
 				result.onCompleted();
			}
			@Override public void onError(Throwable e) {
 				result.onError(e);
			}
			@Override public void onNext(String reply) {
				log.debug("request received Reply 1: "+spec.getChannel()+" : " + reply);
                Message<TReply> replyMessage = spec.getReplyMapper().decode(reply);
				if(replyMessage.isError()){
					log.debug("request received Reply error, c:"+spec.getChannel()+",detail : " + replyMessage.getError().getDetail());
					String detail=String.format("\r\n<cause>\r\n"
		                    + "  <request>\r\n"
		                    + "    %s\r\n"
		                    + "  </request>\r\n"
		                    + "  <response>\r\n"
		                    + "    %s\r\n"
		                    + "  </response>\r\n"
		                    + "  <remote-error-detail>\r\n"
		                    + "    %s"
		                    + "  </remote-error-detail>\r\n"
		                    + "</cause>\r\n", requestText,reply,replyMessage.getError().getDetail());
					result.onError(replyMessage.getError().toException(detail));
				}else{
					result.onNext(replyMessage);
				}
				log.debug("request received Reply 2: "+spec.getChannel()+" : " + reply);

			}});
		return result;
	}
	
	public <TBody extends IMessageSpecProvider<TBody,TReply>,TReply> Observable<TReply> request(TBody body) {
	    return request(body.getMessageSpec(), body);
    }

	public <TBody,TReply> Observable<TReply> request(final MessageSpec<TBody,TReply> spec, TBody body) {
		return request(spec,spec.newRequest(body)).map(new Func1<Message<TReply>,TReply>() {
			@Override
			public TReply call(Message<TReply> t1) {
				return t1.getBody();
			}
		});
	}

	public Observable<String> rawRequest(String channel, String message) {
		return rawBus.request(channel, message);
	}

	public <OUT> Observable<Void> rawSend(String channel, String message) {
		return rawBus.send(channel, message);
	}

	public Observable<IRawRequest> rawReceive(String channel) {
		return rawBus.receive(channel);
	}

	public enum BusState {
		Starting,Running, Stoping, Stoped
	}
	/**
	 * 
	 * <p/>
	 * 此接口不应被客户程序继承
	 * <p/>
	 * 此接口线程安全
	 */
	public interface IRawRequest {
		public Observable<Void> reply(String message);
		public String getMessage();
	}

	public static class Builder {
		public Uri uri;
		public ServerMonitor monitor = ServerMonitor.Dummy;
		public IDiscovery discovery;
        public long timeout=5;
        public TimeUnit timeoutTimeUnit=TimeUnit.SECONDS;
		public Builder address(String host, int port) {
			this.uri=Uri.parse(String.format("bus://%s:%s/bus",host,port));
			return this;
		}
		public Builder monitor(ServerMonitor monitor) {
			this.monitor = monitor == null ? ServerMonitor.Dummy : monitor;
			return this;
		}

		public Builder discovery(IDiscovery discovery) {
			this.discovery = discovery;
			return this;
		}
		public Builder timeoutUnit(long timeout,TimeUnit timeoutTimeUnit) {
			this.timeout = timeout;
			this.timeoutTimeUnit = timeoutTimeUnit;
			return this;
		}

		public RxBus buildAndStart() {
			RxBus bus = build();
			bus.start();
			return bus;
		}
		
		public RxBus build() {
 			return new RxBus(new RawBus(this));
		}
	}
	/**
	 * ReceivedMessage  =  message + replyType, 本类并没有附加通信数据，只是附带了reply类型的参数，及一些replyXXX()方法
	 * <p/>
	 * 不隐藏作为信封概念的Message，用以表明面向协议而非面向框架的设计思路
	 * <p/>
	 * 此类只是封装类，不被序列化
 	 * 
	 * <p/>
	 * 此类不应被客户程序继承
	 * <p/>
	 * 此类线程安全
	 */
     private static class RequestWarpper<BODY,REPLY>{
    	private transient IRawRequest rawRequest;
        private transient MessageSpec<BODY,REPLY> channelSpec;
		private final Message<BODY> request; 
        public RequestWarpper(MessageSpec<BODY,REPLY> channelSpec,Message<BODY> message,IRawRequest rawRequest){
        	this.channelSpec=channelSpec;
        	this.request=message;
        	this.rawRequest=rawRequest;
        }
        
    	public Observable<Void> reply(REPLY replyBody) {
    		if(!request.isReplyable()){
    			return Observable.just((Void)null);
    		}
    		String replyEnvelope=channelSpec.getReplyMapper().encode(new Message<REPLY>(/*channel=*/null,request.getCorrelationId(),replyBody,/*error=*/null));
    		log.debug("Message.reply : "+replyEnvelope);
			return rawRequest.reply(replyEnvelope); 
    	}
    	
		public Observable<Void> replyFail(Throwable e) {
    		if(!request.isReplyable()){
    			return Observable.just((Void)null);
    		}

    		CheckError error=null;
    		if( e instanceof CheckException){
    			error=((CheckException)e).getError();
    		}else{
    			error=Check.GeneralException.toCheckError(new GeneralException(){{info="Service temporarily unavailable";}}).withDetail(e);
    		}
    		
    		String replyEnvelope=channelSpec.getReplyMapper().encode(
    				new Message<REPLY>(/*channel=*/null,request.getCorrelationId(),/*body=*/null,error));
    		log.debug("Envelope.reply error : "+replyEnvelope);
 			return rawRequest.reply(replyEnvelope);
		}
    }
 }
