package firebats.internal.http.server;

import io.netty.buffer.ByteBuf;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.google.common.net.MediaType;


/**
 * Data that potentially has a content type.
 */
public interface TypedData {

  /**
   * The type of the data.
   *
   * @return The type of the data.
   */
  MediaType getContentType();

  /**
   * The data as text.
   * <p>
   * If a content type was provided, and it provided a charset parameter, that charset will be used to decode the text.
   * If no charset was provided, {@code UTF-8} will be assumed.
   * <p>
   * This can lead to incorrect results for non {@code text/*} type content types.
   * For example, {@code application/json} is implicitly {@code UTF-8} but this method will not know that.
   *
   * @return The data decoded as text
   */
  String getText();

  /**
   * The raw data as bytes.
   *
   * @return the raw data as bytes.
   */
  byte[] getBytes();

  /**
   * The raw data as a (unmodifiable) buffer.
   *
   * @return the raw data as bytes.
   */
  ByteBuf getBuffer();

  /**
   * Writes the data to the given output stream.
   * <p>
   * This method does not flush or close the stream.
   *
   * @param outputStream The stream to write to
   * @throws IOException any thrown when writing to the output stream
   */
  void writeTo(OutputStream outputStream) throws IOException;

  /**
   * An input stream of the data.
   *
   * @return an input stream of the data.
   */
  InputStream getInputStream();

}

