/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package firebats.http.server.exts.form;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.FileUpload;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.data.validation.ValidationError;
import rx.functions.Action0;

import com.google.common.base.Throwables;
import com.google.common.collect.Maps;

import firebats.check.Check;
import firebats.http.server.Context;

/**
 * http请求中包含Content-Type，而Content-Type中又包含MediaType，http request常见的MediaType有以下几种：
 * <ul>
 * <li>application/x-www-form-urlencoded</li>
 * <li>multipart/form-data</li>
 * <li>application/json</li>
 * <li>text/xml</li>
 * </ul>
 * 
 * 我们把application/x-www-form-urlencoded和multipart/form-data格式的数据抽象为Form的概念(如Play
 * |Ratpack)， Form由3种数据组成：
 * <ul>
 * <li>uri的queryString</li>
 * <li>x-www-form-urlencoded或multipart/form-data</li>
 * <li>上传的文件</li>
 * </ul>
 * 而我们的Form把他们归类如下
 * <table border="1">
 * <tr>
 * <th>词汇</th>
 * <th>对应方法</th>
 * <th>数据来源</th>
 * <th>类型</th>
 * </tr>
 * <tr>
 * <td>query</td><td> {@link #getQuery} {@link #getQuery(String name)}</td><td>uri queryString中的键值对(也许以后会包含uri模板的解析)</td><td>String</td>
 * </tr>
 * <tr>
 * <td>form</td><td> {@link #getForm} {@link #getForm(String name)}</td><td>x-www-form-urlencoded或multipart/form-data中的键值对</td><td>String</td>
 * </tr>
 * <td>attr</td><td> {@link #getAttrs} {@link #getAttr(String name)}</td><td>query + form</td><td>String</td>
 * </tr>
 * <tr>
 * <td>file</td><td> {@link #getFiles} {@link #getFile(String name)}</td><td>上传的文件，键值对</td><td>{@link UploadedFile}</td>
 * </tr>
 * </table>
 */
public class Form {
	private static Logger log=LoggerFactory.getLogger(Form.class);
	
	private final Map<String, String> formParams;
	private final Map<String, UploadedFile> fileParams;
	private Map<String, String> queryParams;
	private Map<String, String> formAndQuery;
	private Form(Map<String, String> queryParams,Map<String, String> formParams,Map<String, UploadedFile> files) {
		this.queryParams = Collections.unmodifiableMap(queryParams);
		this.formParams = Collections.unmodifiableMap(formParams);
		this.fileParams = Collections.unmodifiableMap(files);
		formAndQuery=Maps.newLinkedHashMap();
		formAndQuery.putAll(queryParams);
		formAndQuery.putAll(formParams);
		formAndQuery=Collections.unmodifiableMap(formAndQuery);
	}
	public static Form decode(Context context,ByteBuf content) {
		return content!=null&&content.isReadable()?Form.decodeWithContent(context,content):Form.decodeWithoutContent(context);
	}

	public static Form fromQuery(Map<String, String> queryParams) {
		return new Form(queryParams,Collections.<String, String>emptyMap(), Collections.<String, UploadedFile>emptyMap());
	}
	public static Form fromAll(Map<String, String> queryParams,Map<String, String> formAndQuery,Map<String, UploadedFile> fileParams) {
		return new Form(queryParams,formAndQuery, fileParams);
	}
	/**
	 * @return copy new HttpForm with formParams
	 **/
	public Form withForm(Map<String, String> formParams) {
		return new Form(queryParams,formParams, fileParams);
	}
	/**
	 * @return copy new HttpForm with filesParams
	 **/
	public Form withFiles(Map<String, UploadedFile> filesParams) {
		return new Form(queryParams,formParams, filesParams);
	}
	
	/**所有文件参数*/
	public UploadedFile getFile(String key) {
		return getFiles().get(key);
	}

	/**getForm()+getQuery()
	 *
	 * 从QueryParameters | application/x-www-form-urlencoded | multipart/form-data 中获取普通字符串参数</br>
	 * 对少量和简单的参数，用此方法替代getFrom()将是一个很好的选择，且性能更高
	 * <pre>
	 * String email = requestContext.get("email");
	 * _VerificationCodeApiBus.SendCodeToEmail cmd=new _VerificationCodeApiBus.SendCodeToEmail();
	 * cmd.email=email;
	 * bus.request(cmd);
	 * </pre>
	 */
	public String getAttr(String key) {
		return getAttrs().get(key);
	}
	
	public boolean contains(String key) {
		return getAttrs().containsKey(key)||getFiles().containsKey(key);
	}
	
	public boolean containsAttr(String key) {
		return getAttrs().containsKey(key);
	}
	public boolean containsFile(String key) {
 		return fileParams.containsKey(key);
	}
	/**uri中的查询串参数*/
	public Map<String, String> getQuery() {
		return queryParams;
	}

	/**getForm()+getQuery()*/
	public Map<String, String> getAttrs() {
		return formAndQuery;
	}

	/**所有文件参数*/
	public Map<String, UploadedFile> getFiles() {
		return fileParams;
	}

	/**所有form表单参数，即post上来的内容,不包含uri中的查询串参数*/
	public Map<String, String> getForm() {
		return formParams;
	}
	/**attrs + files*/
	public Map<String, Object> getAll() {
		LinkedHashMap<String, Object> result = Maps.<String,Object>newLinkedHashMap(fileParams);
		result.putAll(getAttrs());
	    return result;
	}

	public <T> T as(Class<T> clazz) {
		play.data.Form<T> result=play.data.Form.form(clazz).bind(formAndQuery);
		if(result.hasErrors()){
			//TODO ERROR 处理
			Entry<String, List<ValidationError>> x = result.errors().entrySet().iterator().next();
			Check.GeneralFail.check(!result.hasErrors(),p->{p.info=clazz.getName()+":"+x.getValue();});
		}
		return result.get();
	}
	
	@Override public String toString() {
		return ""+queryParams+formParams+fileParams;
	}
	public static Map<String, String> toFlatQueryParams(Map<String, List<String>> queryParameters) {
		Map<String, String> result = new LinkedHashMap<>();
	    for(String key: queryParameters.keySet()) {
	        Collection<String> values = queryParameters.get(key);
	        if(!values.isEmpty()) {
	            result.put(key, values.iterator().next());
	        }
	    }
		return result;
	}
	private static Form decodeWithContent(Context context,ByteBuf content) {
			//用new DefaultHttpDataFactory(/*useDisk*/true)创建出的decoder会把所有属性也存到文件，不妥
			//还是用默认的Mix方式，默认大于16K的数据才寸磁盘
	// 		final HttpPostRequestDecoder decoder = new HttpPostRequestDecoder(new DefaultHttpDataFactory(/*useDisk*/true),toNettyHttpRequest(context.request));
			HttpServerRequest<ByteBuf> rxRequest = context.getRequest();
			HttpRequest nettyRequest = new DefaultHttpRequest(HttpVersion.HTTP_1_1,rxRequest.getHttpMethod(), rxRequest.getUri());
			for (Map.Entry<String,String> header : rxRequest.getHeaders().entries()) {
				nettyRequest.headers().add(header.getKey(),header.getValue());
			}
	 		final HttpPostRequestDecoder decoder = new HttpPostRequestDecoder(nettyRequest);
	 		HttpContent httpContent = new DefaultHttpContent(content);
			decoder.offer(httpContent);
			decoder.offer(LastHttpContent.EMPTY_LAST_CONTENT);
			
			Map<String, String> formParams = new LinkedHashMap<>();
			Map<String, UploadedFile> files = new LinkedHashMap<>();
			try {
				while (decoder.hasNext()) {
					InterfaceHttpData data = decoder.next();
					if (data.getHttpDataType().equals(InterfaceHttpData.HttpDataType.Attribute)) {
						try {
							Attribute attr=(Attribute) data;
							if (!formParams.containsKey(data.getName())) {
								formParams.put(attr.getName(),attr.getValue());
							}
						} catch (IOException e) {
							Throwables.propagate(e);
						} finally {
							//普通字串属性立即释放
							data.release();
						}
					} else if (data.getHttpDataType().equals(InterfaceHttpData.HttpDataType.FileUpload)) {
						try {
							if (!files.containsKey(data.getName())) {
								final FileUpload nettyFileUpload = (FileUpload) data;
								final ByteBuf byteBuf = nettyFileUpload.content();
								byteBuf.retain();
								context.onComplete(new Action0(){
								    @Override public void call() {
								    	if(log.isDebugEnabled()){
									    	log.debug("form upload file release["+data.getName()+":"+nettyFileUpload.getFilename()+"]");
								    	}
									    byteBuf.release();
								    }
							    });
								UploadedFile fileUpload = new UploadedFile(nettyFileUpload.getFilename(),nettyFileUpload.getContentType(),byteBuf);
								files.put(data.getName(), fileUpload);
							}
						} finally{
							data.release();
						}
					}
				}
			} catch (HttpPostRequestDecoder.EndOfDataDecoderException ignore) {
				// ignore
			}finally{
				decoder.destroy();
			}
	        Map<String, String> query = Form.toFlatQueryParams(context.getRequest().getQueryParameters());
			return fromAll(query,formParams, files);
		}
	private static Form decodeWithoutContent(Context context) {
		return fromQuery(Form.toFlatQueryParams(context.getRequest().getQueryParameters()));
	}
}
