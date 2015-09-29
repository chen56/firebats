package firebats.http.server.exts.form;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;

import java.io.InputStream;

import javax.annotation.Nonnull;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.net.MediaType;

public class UploadedFile {
	private MediaType mediaType;
	private ByteBuf byteBuf;
    private String contentType;
 	private String filename; 

	public UploadedFile(String filename, String contentType, ByteBuf fileByte) {
		Preconditions.checkNotNull(fileByte);
		this.filename = filename;
		this.contentType = contentType;
		this.mediaType = safeParseMediaType(contentType);
		this.byteBuf=fileByte;
	}


	/**@return Nonnull*/
	@Nonnull
	public MediaType getMediaType() {
		return mediaType;
	}
	public Optional<String> getContentType() {
		return Optional.of(contentType);
	}

	public Optional<String> getFilename() {
		return Optional.of(filename);
	}

	private ByteBuf content() {
		return byteBuf;
	}

	public InputStream getInputStream() {
  	    return new ByteBufInputStream(content());
	}

	@Override public String toString() {
		return getMediaType() + ":" + getFilename();
	}
	//MediaType.parse解析时，空串导致异常
	private MediaType safeParseMediaType(String contentType) {
		try {
			return Strings.isNullOrEmpty(contentType) ? MediaType.APPLICATION_BINARY: MediaType.parse(contentType);
		} catch (Exception e) {
			return MediaType.APPLICATION_BINARY;
 		}
	}

}