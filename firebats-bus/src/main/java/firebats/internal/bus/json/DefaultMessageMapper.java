package firebats.internal.bus.json;

import firebats.bus.Message;
import firebats.bus.Message.MessageMapper;
import firebats.json.Jackson;
import firebats.reflect.TypeRef;
import firebats.reflect.TypeRef.TypeParameter;

public class DefaultMessageMapper<BODY> implements MessageMapper<BODY> {
	private TypeRef<Message<BODY>> messageType;
	private TypeRef<BODY> bodyType;

	public static <BODY> DefaultMessageMapper<BODY> of(Class<BODY> bodyType) {
		DefaultMessageMapper<BODY> result = new DefaultMessageMapper<>();
		result.messageType = new TypeRef<Message<BODY>>() {}.where(new TypeParameter<BODY>() {}, bodyType);
		result.bodyType = TypeRef.of(bodyType);
		return result;
	}
	public static <BODY> DefaultMessageMapper<BODY> of(TypeRef<BODY> bodyType) {
		DefaultMessageMapper<BODY> result = new DefaultMessageMapper<>();
		result.messageType = new TypeRef<Message<BODY>>() {}.where(new TypeParameter<BODY>() {}, bodyType);
		result.bodyType = bodyType;
		return result;
	}

	public String encode(Message<BODY> message) {
		return Jackson.normal().encode(message);
	}

	public Message<BODY> decode(String text) {
		return Jackson.normal().decode(text, messageType);
	}

	public TypeRef<BODY> getBodyType() {
		return bodyType;
	}
	
	public TypeRef<Message<BODY>> getMessageType() {
		return messageType;
	}
}