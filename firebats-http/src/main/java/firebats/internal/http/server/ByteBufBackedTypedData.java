package firebats.internal.http.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.net.MediaType;

public class ByteBufBackedTypedData implements TypedData {

	private final ByteBuf byteBuf;
	private final MediaType mediaType;

	public ByteBufBackedTypedData(ByteBuf byteBuf, MediaType mediaType) {
		this.mediaType = mediaType;
		this.byteBuf = byteBuf;
	}

	public ByteBufBackedTypedData(ByteBuf byteBuf, String mediaType) {
		this.mediaType =mediaType == null ? MediaType.APPLICATION_BINARY
				: MediaType.parse(mediaType);
		this.byteBuf = byteBuf;
	}

	/**
	 * ContentType
	 * 
	 * @return 可能为空
	 */
	@Override public MediaType getContentType() {
		return mediaType;
	}

	@Override public ByteBuf getBuffer() {
		return Unpooled.unmodifiableBuffer(byteBuf);
	}

	@Override public String getText() {
		if (mediaType == null) {
			return byteBuf.toString(Charsets.UTF_8);
		} else {
			return byteBuf.toString(getCharset());
		}
	}

	/**
	 * 安全的获取字符集
	 * 
	 * @return 非空
	 */
	public Charset getCharset() {
		Optional<Charset> charset = mediaType.charset();
		return charset.isPresent() ? charset.get() : Charsets.UTF_8;
	}

	@Override public byte[] getBytes() {
		if (byteBuf.hasArray()) {
			return byteBuf.array();
		} else {
			ByteArrayOutputStream baos = new ByteArrayOutputStream(
					byteBuf.writerIndex());
			try {
				writeTo(baos);
			} catch (IOException e) {
				Throwables.propagate(e);
			}
			return baos.toByteArray();
		}
	}

	@Override public void writeTo(OutputStream outputStream) throws IOException {
		byteBuf.resetReaderIndex();
		byteBuf.readBytes(outputStream, byteBuf.writerIndex());
	}

	@Override public InputStream getInputStream() {
		return new ByteBufInputStream(byteBuf);
	}
}
