package th.api.common;


import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.GZIPInputStream;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpTrace;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import copy.org.eclipse.core.runtime.IProgressMonitor;
import copy.org.eclipse.core.runtime.OperationCanceledException;
import copy.org.eclipse.core.runtime.SubMonitor;
import copy.org.springframework.http.ContentCodingType;
import copy.org.springframework.http.HttpHeaders;
import copy.org.springframework.http.HttpMethod;
import copy.org.springframework.http.HttpStatus;
import copy.org.springframework.http.MediaType;
import copy.org.springframework.util.FileCopyUtils;
import copy.org.springframework.util.LinkedMultiValueMap;
import copy.org.springframework.util.MultiValueMap;
import copy.org.springframework.util.StringUtils;
import firebats.json.Jackson;
import firebats.reflect.Classes;
import firebats.reflect.TypeRef;

/**
 * Rest Web Service 封装类
 * 
 */
public class Ws implements Closeable {
	public static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");
    private static Logger log=LoggerFactory.getLogger(Ws.class);
    private static List<MediaType> AcceptableMediaTypes = new ArrayList<MediaType>();
    {
        AcceptableMediaTypes.add(MediaType.APPLICATION_JSON);
    }
    private static List<ContentCodingType> AcceptableContentCodingTypes = new ArrayList<ContentCodingType>();
    {
        AcceptableContentCodingTypes.add(ContentCodingType.GZIP);
    }

    private HttpClient httpClient;

    private IWsErrorHandler errorHandler = new DefaultWsErrorHandler();
    private WsListeners wsListeners = new WsListeners();

	private IHeaderProvider defaultHeaders;

    public Ws() {
        this(new DefaultHttpClient(), EmptyHeaderProvider);
    }

    public Ws(HttpClient httpClient, IHeaderProvider headerProvider) {
        this.httpClient = httpClient;
        this.defaultHeaders=headerProvider;
     }

    public org.apache.http.client.CookieStore getCookieStore() {
        return ((AbstractHttpClient) httpClient).getCookieStore();
    }

	public WsListeners getWsListeners() {
		return wsListeners;
	}

    /**
     * 配置代理服务器
     */
    public void setProxy(HttpHost proxy) {
        Preconditions.checkNotNull(proxy, "proxy参数名不能为空");
        httpClient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY,
                proxy);
    }

    public WsRequest newRequest(String uri) {
    	Preconditions.checkNotNull(uri, "uri参数名不能为空");

        WsRequest result = new WsRequest(this, uri);
        return result;
    }

    public WsRequest newRequest() {
        return new WsRequest(this);
    }

    /**
     * 释放网络连接资源
     */
    @Override
    public void close() {
        this.httpClient.getConnectionManager().shutdown();
    }

    private static void close(HttpResponse httpResponse) {
        HttpEntity entity = httpResponse.getEntity();
        try {
            // This will cause the underlying connection
            // to be released back to the connection manager
            // EntityUtils.consume(entity);
            if (entity == null) {
                return;
            }
            if (entity.isStreaming()) {
                InputStream instream = entity.getContent();
                if (instream != null) {
                    instream.close();
                }
            }
        }
        catch (IOException ignore) {
            // Connection will be released automatically
        }
    }

    public void setErrorHandler(IWsErrorHandler errorHandler) {
        Preconditions.checkNotNull(errorHandler);
        this.errorHandler = errorHandler;
    }

    public static String toQueryString(
            MultiValueMap<String, Object> parameters, String spliter,
            boolean encode) {
        StringBuilder builder = new StringBuilder();
        for (Iterator<String> keys = parameters.keySet().iterator(); keys
                .hasNext();) {
            String key = keys.next();
            for (Iterator<Object> values = parameters.get(key).iterator(); values
                    .hasNext();) {
                Object value = values.next();
                builder.append(encode ? encodeURL(key) : key).append("=");
                if (value != null) {
                    builder.append(encode ? encodeURL(value.toString())
                            : value.toString());
                }
                if (values.hasNext()) {
                    builder.append(spliter);
                }
            }
            if (keys.hasNext()) {
                builder.append(spliter);
            }
        }
        return builder.toString();
    }

    public static class WsRequest {

        private final HttpHeaders headers = new HttpHeaders();

        private MultiValueMap<String, Object> parameters = new LinkedMultiValueMap<String, Object>();
        private MultiValueMap<String, File> files = new LinkedMultiValueMap<String, File>();

        private List<String> pathParts = new ArrayList<String>();

        private String scheme;

        private String host;

        private int port;

        private String fragment;

        private Ws ws;


        private UUID id=UUID.randomUUID();
        
        public UUID getId() {
			return id;
		}

		private WsRequest(Ws ws, String uri) {
            this(ws);
            try {
                URI u = new URI(uri);
                scheme = u.getScheme();
                if (scheme != null) {
                    scheme = scheme.toLowerCase();
                }
                host = u.getHost();
                port = u.getPort();
                this.pathParts = toPathParts(u.getPath());
                fragment = u.getFragment();
                parameters = toParmeters(u.getQuery());

            }
            catch (URISyntaxException e) {
                throw new WsException(e);
            }
        }

        private WsRequest(Ws ws) {
            this.ws = ws;
            this.headers.putAll(ws.defaultHeaders.getHeaders());
        }

        /**
         * 给请求添加参数， 若是get请求，则参数将拼为uri query部分 若是post请求，则参数作为form内容体提交
         */
        public WsRequest addParameter(String key, Object value) {
        	Preconditions.checkArgument(!Strings.isNullOrEmpty(key), "参数名不能为空");
            if(value==null){
//            	return this;
            }
            parameters.add(key.trim(), value==null?null:value.toString());
            return this;
        }
        
        /**
         * 给请求添加参数， 若是get请求，则参数将拼为uri query部分 若是post请求，则参数作为form内容体提交
         */
        public WsRequest addParameters(String key, Object ... values) {
        	Preconditions.checkArgument(!Strings.isNullOrEmpty(key), "参数名不能为空");
            for (Object value : values) {
                parameters.add(key.trim(), value==null?null:value.toString());
            }
            return this;
        }

        /**
         * 给uri上添加path部分，例如: a/b , a/b/c
         */
        public WsRequest addPath(String pathPart) {
        	Preconditions.checkArgument(!Strings.isNullOrEmpty(pathPart), "pathPart参数不能为空");
            pathParts.addAll(toPathParts(pathPart));
            return this;
        }

        /**
         * get请求uri，参数将拼为uri query部分
         */
        public URI getURIWithQuery() {
            return getURI(getQuery(false));
        }

        public URI getURIWithoutQuery() {
            return getURI(null);
        }

        /**
         * get请求uri，参数将拼为uri query部分
         */
        public URI getURI(String query) {
            try {
                query = Strings.isNullOrEmpty(query) ? null : query;
                String path = "/"
                        + StringUtils.collectionToDelimitedString(pathParts,
                                "/");
                return new URI(scheme, null, host, port, path, query, fragment);
            }
            catch (URISyntaxException e) {
                throw new WsException(e);
            }
        }

        /**
         * 运行Get类型的请求
         */
        public WsResponse get() {
            HttpUriRequest httpRequest = createHttpClientRequest(
                    getURI(getQuery(false)), HttpMethod.GET);
            return execute(httpRequest);
        }

        /**
         * 运行post类型的请求, 如果没有文件参数，则post: application/x-www-form-urlencoded
         * 如果有文件参数，则post: multipart/form-data
         */
        public WsResponse post() {
            return files.isEmpty() ? postFormUrlEncoded() : postMultipart(null);
        }

        /**
         * 运行post类型的请求, 如果没有文件参数，则post: application/x-www-form-urlencoded
         * 如果有文件参数，则post: multipart/form-data
         */
        public WsResponse post(IProgressMonitor monitor) {
            return files.isEmpty() ? postFormUrlEncoded()
                    : postMultipart(monitor);
        }

        /**
         * 运行post类型的请求
         * 
         * @param gson
         */
        private WsResponse postFormUrlEncoded() {
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            StringEntity entity = null;
            try {
                entity = new StringEntity(getQuery(false),DEFAULT_CHARSET.name());
            }
            catch (UnsupportedEncodingException e) {
                // 不可能抛出异常
                throw new WsException(e);
            }
            HttpPost httpRequest = (HttpPost) createHttpClientRequest(
                    getURI(null), HttpMethod.POST);
            httpRequest.setEntity(entity);
            return execute(httpRequest);
        }

        /**
         * 运行post类型的请求
         * 
         * @param monitor
         * @param gson
         */
        private WsResponse postMultipart(IProgressMonitor monitor) {
        	MultipartEntity entity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
            for (String name : parameters.keySet()) {
                for (Object value : parameters.get(name)) {
                    try {
                        entity.addPart(name, new StringBody(value==null?"":value.toString(),DEFAULT_CHARSET));
                    }
                    catch (UnsupportedEncodingException e) {
                        // 不可能抛出UnsupportedEncodingException异常
                        throw new WsException(e);
                    }
                }
            }
            
            List<FileBodyWithMonitor> fileBodies = new ArrayList<FileBodyWithMonitor>();
            for (String key : files.keySet()) {
                for (File file : files.get(key)) {
                    if (file != null && file.exists()) {
                        FileBodyWithMonitor fb = new FileBodyWithMonitor(file,
                                encodeURL(file.getName()),
                                MediaType.APPLICATION_OCTET_STREAM.toString(),
                                null);
                        fileBodies.add(fb);
                        entity.addPart(key, fb);
                    }
                }
            }
            
            int length = (int) entity.getContentLength();
            try {
                SubMonitor sub = SubMonitor.convert(monitor, "上传", length);
                for (int i = 0; i < fileBodies.size(); i++) {
                    fileBodies.get(i).setMonitor(sub, fileBodies.size(), i);
                }
                HttpPost httpRequest = (HttpPost) createHttpClientRequest(
                        getURI(null), HttpMethod.POST);
                httpRequest.setEntity(entity);
                return execute(httpRequest);
            }
            finally {
                if (monitor != null) {
                    monitor.done();
                }
            }
        }

 
        private class FileBodyWithMonitor extends FileBody {
            private IProgressMonitor monitor;

            public FileBodyWithMonitor(File file, String filename,
                    String mimeType, String charset) {
                super(file, filename, mimeType, charset);
            }

            public void setMonitor(IProgressMonitor monitor,
                    int totalFileCount, int number) {
                this.monitor = monitor;
            }

            public void writeTo(final OutputStream out) throws IOException {
                if (out == null) {
                    throw new IllegalArgumentException(
                            "Output stream may not be null");
                }
                this.monitor.subTask(getFile().getName());
                InputStream in = new FileInputStream(this.getFile());
                try {
                    byte[] tmp = new byte[4096];
                    int l;
                    while ((l = in.read(tmp)) != -1) {
                        if (monitor.isCanceled()) {
                            throw new OperationCanceledException();
                        }
                        out.write(tmp, 0, l);
                        this.monitor.worked(l);
                        this.monitor.subTask("");
                        // try {
                        // Thread.sleep(100);
                        // } catch (Exception e) {
                        // }
                    }
                    out.flush();
                }
                finally {
                    in.close();
                }
            }

        }

        public final HttpHeaders getHeaders() {
            return headers;
        }

        @Override
        public String toString() {
            return getURI(getQuery(false)).toString();
        }

        /*
         * 参数集合拼为uri查询串 [{a:[1,2]},b:1] -> a=1&a=2&b=1
         */
        private String getQuery(boolean encode) {
            return Ws.toQueryString(parameters, "&", encode);
        }

        /*
         * 分隔uri path字符串部分为数组： /a/b -> [a,b]
         */
        private static List<String> toPathParts(String uriPath) {
            String[] paths = uriPath.split("/");
            List<String> result = new ArrayList<String>();
            for (String pathPart : paths) {
                if (!Strings.isNullOrEmpty(pathPart)) {
                    result.add(pathPart.trim());
                }
            }
            return result;
        }

        /*
         * 分隔uri query字符串部分为数组： a=1&a=2&b=1 -> [{a:[1,2]},b:[1]]
         */
        private static MultiValueMap<String, Object> toParmeters(String uriQuery) {
            MultiValueMap<String, Object> result = new LinkedMultiValueMap<String, Object>();
            if (uriQuery == null) {
                return result;
            }
            String[] p = uriQuery.split("&");
            for (String nameValue : p) {
                String[] splittedElement = nameValue.split("=");
                if (splittedElement.length == 1) {
                    result.add(splittedElement[0].trim(), null);
                }
                if (splittedElement.length == 2) {
                    String key = splittedElement[0];
                    String value = splittedElement[1];
                    if (!Strings.isNullOrEmpty(key)
                            && !Strings.isNullOrEmpty(value)) {
                        result.add(key, value);
                    }
                }
            }
            return result;
        }

        public MultiValueMap<String, Object> getParameters() {
            return new LinkedMultiValueMap<String, Object>(parameters);
        }

        public WsRequest setHost(String host) {
        	Preconditions.checkNotNull(host, "host参数不能为空");

            this.host = host.trim();
            return this;
        }

        public WsRequest setPort(int port) {
            this.port = port;
            return this;
        }

        public WsRequest setScheme(String scheme) {
        	Preconditions.checkNotNull(scheme, "shceme参数不能为空");

            this.scheme = scheme.trim().toLowerCase();
            return this;
        }

        public String getScheme() {
            return scheme;
        }

        private WsResponse execute(HttpUriRequest httpRequest) {
            ws.wsListeners.beforeRequest(this);

            if(log.isDebugEnabled()){
            	log.debug(getDebugInfo(httpRequest.getMethod()).toString());
            }
            HttpResponse httpResponse = null;
            try {
                httpResponse = ws.httpClient.execute(httpRequest);
            }
            catch (ClientProtocolException e) {
                throw new RuntimeIOException(e);
            }
            catch (IOException e) {
                // IO异常,尝试清除dns缓存
                safeClearDnsCache(host);
                throw new RuntimeIOException(e);
            }
            WsResponse result = new WsResponse(this, httpResponse,httpRequest.getMethod());
            if(log.isDebugEnabled()){
            	log.debug(result.getDebugInfo().toString());
            }
            if (ws.errorHandler != null && ws.errorHandler.hasError(result)) {
                ws.errorHandler.handleError(result);
            }
            ws.wsListeners.afterRequest(this,result);
            return result;
        }

        @SuppressWarnings("unchecked")
        private static Map<String, Object> safeGetDnsCache() {
            try {
                Field cacheField = Classes.safeGetFieldOf(InetAddress.class,
                        "addressCache");
                if (cacheField == null) {
                    return new HashMap<String, Object>();
                }

                Object cache = Classes.safeGetFieldValue(InetAddress.class,
                        cacheField);// cacheField.get(InetAddress.class.getClass());
                if (cache == null) {
                    return new HashMap<String, Object>();
                }

                // android 系统的缓存map字段名
                Field mapField = Classes.safeGetFieldOf(cacheField.getType(),
                        "map");
                if (mapField == null) {
                    // 标准jdk 系统的缓存cache字段名
                    mapField = Classes.safeGetFieldOf(cacheField.getType(),
                            "cache");
                }
                if (mapField == null) {
                    if(log.isErrorEnabled()){
                    	log.error("找不到dns缓存字段,无法清除缓存 ");
                    }
                    return new HashMap<String, Object>();
                }
                Map<String, Object> result = (Map<String, Object>) Classes
                        .safeGetFieldValue(cache, mapField);
                return result == null ? new HashMap<String, Object>() : result;
            }
            catch (Throwable e) {
                if(log.isErrorEnabled()){
                	log.error("clear dns cache error ");
                }
                return new HashMap<String, Object>();
            }
        }

        private static void safeClearDnsCache(String host) {
            safeGetDnsCache().clear();
        }

        private HttpUriRequest createHttpClientRequest(URI uri,
                HttpMethod httpMethod) {
            HttpUriRequest httpRequest = null;
            switch (httpMethod) {
                case GET:
                    httpRequest = new HttpGet(uri);
                    break;
                case DELETE:
                    httpRequest = new HttpDelete(uri);
                    break;

                case HEAD:
                    httpRequest = new HttpHead(uri);
                    break;

                case OPTIONS:
                    httpRequest = new HttpOptions(uri);
                    break;

                case POST:
                    httpRequest = new HttpPost(uri);
                    break;

                case PUT:
                    httpRequest = new HttpPut(uri);
                    break;

                case TRACE:
                    httpRequest = new HttpTrace(uri);
                    break;

                default:
                    throw new IllegalArgumentException("Invalid HTTP method: "
                            + httpMethod);
            }

            HttpParams params = httpRequest.getParams();
            // 禁用头：Expect: 100-Continue'
            // 'Expect: 100-Continue' 会用先和服务器握手，后发送post或put内容的方式来增强性能。
            // 但某些服务器或代理不支持。
            HttpProtocolParams.setUseExpectContinue(params, false);

            headers.setAccept(AcceptableMediaTypes);
            headers.setAcceptEncoding(AcceptableContentCodingTypes);

            // 复制WsRequest头信息->HttpUriRequest
            for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                String headerName = entry.getKey();
                if (!headerName.equalsIgnoreCase(HTTP.CONTENT_LEN)
                        && !headerName.equalsIgnoreCase(HTTP.TRANSFER_ENCODING)) {
                    for (String headerValue : entry.getValue()) {
                        httpRequest.addHeader(headerName, headerValue);
                    }
                }
            }
            return httpRequest;
        }

        public WsRequest addFileParameter(String key, File file) {
        	Preconditions.checkArgument(!Strings.isNullOrEmpty(key), "key参数应不为空");

            if (file == null || !file.exists()) {
                return this;
            }
            files.add(key, file);
            return this;
        }

        public void addAllParameter(MultiValueMap<String, Object> parameters) {
            this.parameters.putAll(parameters);
        }

		public List<Cookie> getCookies() {
			return ws.getCookieStore().getCookies();
		}
		public void addCookie(Cookie cookie){
			ws.getCookieStore().addCookie(cookie);
		}
		
		private StringBuffer getDebugInfo(String method){
            StringBuffer result = new StringBuffer();
            if (Objects.equal(HttpMethod.GET.name(),method)) {
                result.append(method + " "
                        + getURI(getQuery(false)) + "\n");
            }
            else {
                result.append(method + " " + getURI(null)
                        + "\n");
                for (String key : parameters.keySet()) {
                    result.append("        " + key + ":"
                            + parameters.get(key) + "\n");
                }
                for (String key : files.keySet()) {
                    result.append("        " + key + ":"
                            + files.get(key) + "\n");
                }
            }
            for (Cookie c : getCookies()) {
                result.append("        cookie:" + c +"\n");
			}
            
            result.append("    Request\n");
            for (String key : headers.keySet()) {
                result.append("        " + key + ":" + headers.get(key)
                        + "\n");
            }
            return result;
		}
    }

    public static class WsResponse {
        private final HttpResponse httpResponse;
        private HttpHeaders headers = new HttpHeaders();
        private WsRequest request;
        private String cacheBody;
        private String method;

        private WsResponse(WsRequest wsRequest, HttpResponse httpResponse,
                String method) {
            this.request = wsRequest;
            this.httpResponse = httpResponse;
            this.method = method;
            for (Header header : httpResponse.getAllHeaders()) {
                headers.add(header.getName(), header.getValue());
            }
        }
        public WsRequest getRequest(){
        	return request;
        }
        public HttpMethod getMethod() {
            return HttpMethod.valueOf(method);
        }

        public <T> T getObject(TypeRef<T> type) {
            return Jackson.normal().decode(getString(), type);
        }

        public <T> T getObject(Class<T> clazz) {
            return Jackson.normal().decode(getString(), clazz);
        }

        public String getString() {
            try {
                if (cacheBody != null) {
                    return cacheBody;
                }
                else {
                    cacheBody = FileCopyUtils
                            .copyToString(new InputStreamReader(getContent(),
                                    getCharset()));
                    return cacheBody;
                }
            }
            catch (IOException e) {
                throw new RuntimeIOException(e);
            }
            finally {
                close();
            }
        }

        public HttpHeaders getHeaders() {
            return headers;
        }

        public HttpStatus getStatusCode() {
            return HttpStatus.valueOf(httpResponse.getStatusLine()
                    .getStatusCode());
        }

        public String getStatusText() {
            return httpResponse.getStatusLine().getReasonPhrase();
        }

        /**
         * body 流，若内容被gzip压缩，则返回解压后的流
         */
        public InputStream getContent() throws IOException {
            HttpEntity entity = httpResponse.getEntity();
            if (entity == null) {
                return new ByteArrayInputStream(new byte[] {});
            }

            // 从Header中判断body是否被gzip压缩
            List<ContentCodingType> contentCodingTypes = headers
                    .getContentEncoding();
            for (ContentCodingType contentCodingType : contentCodingTypes) {
                if (contentCodingType.equals(ContentCodingType.GZIP)) {
                    return new GZIPInputStream(entity.getContent());
                }
            }
            return entity.getContent();
        }

        /* internal */private WsResponse close() {
            Ws.close(httpResponse);
            return this;
        }

        /**
         * 读取content时使用的字符集。
         * 
         * 如果response头中没有类似定义： Content-Type text/html;charset=UTF-8
         * 则使用UTF-8作为缺省字符集读取content。
         */
        public Charset getCharset() {
            if (headers.getContentType() != null
                    && headers.getContentType().getCharSet() != null) {
                return headers.getContentType().getCharSet();
            }
            return DEFAULT_CHARSET;
        }

        public StringBuffer getDebugInfo() {
            StringBuffer result = new StringBuffer();
            result.append("    Response    " + this.getStatusCode() + " "
                    + this.getStatusText() + "\n");

            for (String key : headers.keySet()) {
                result.append("        " + key + ":"
                        + decodeURL(headers.get(key).toString()) + "\n");
            }
            result.append("        ----------------------------------http content\n");
            result.append("        " + getString() + "\n");
            result.append("        ----------------------------------http json\n");
            result.append("\n");
            return result;
        }

    }
	public static String encodeURL(String data) {
		if(Strings.isNullOrEmpty(data)){
			return data;
		}

		try {
			return URLEncoder.encode(data, DEFAULT_CHARSET.displayName());
		} catch (UnsupportedEncodingException wontHappen) {
			throw new IllegalStateException(wontHappen);
		}
	}

	public static String decodeURL(String data) {
		if(Strings.isNullOrEmpty(data)){
			return data;
		}
		try {
			return URLDecoder.decode(data, DEFAULT_CHARSET.displayName());
		} catch (UnsupportedEncodingException wontHappen) {
			throw new IllegalStateException(wontHappen);
		}
	}

    public static interface IWsErrorHandler {
        public boolean hasError(WsResponse response);

        public void handleError(WsResponse response);
    }

    /**
     * 由Ws类进行网络调用所引发的多有错误的异常根
     */
    public static class WsException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public WsException(String message) {
            super(message);
        }

        public WsException(String message, Throwable e) {
            super(message, e);
        }

        public WsException(Throwable e) {
            super(e);
        }
    }

    /**
	 */
    public static class RuntimeIOException extends WsException {
        private static final long serialVersionUID = 1L;

        public RuntimeIOException(String msg) {
            super(msg);
        }

        public RuntimeIOException(String msg, IOException ex) {
            super(msg, ex);
        }

        public RuntimeIOException(IOException e) {
            super(e);
        }
    }

    public static class DefaultWsErrorHandler implements IWsErrorHandler {
        public void handleError(WsResponse response) {
            HttpStatus statusCode = response.getStatusCode();
            switch (statusCode.series()) {
                case CLIENT_ERROR:
                    throw new HttpClientErrorException(response);
                case SERVER_ERROR:
                    throw new HttpServerErrorException(response);
                default:
                    throw new WsException("Unknown status code [" + statusCode
                            + "]");
            }
        }

        @Override
        public boolean hasError(WsResponse response) {
            HttpStatus statusCode = response.getStatusCode();
            return (statusCode.series() == HttpStatus.Series.CLIENT_ERROR || statusCode
                    .series() == HttpStatus.Series.SERVER_ERROR);
        }

        public static abstract class HttpStatusCodeException extends
                WsException {

            private static final long serialVersionUID = 1L;

            private final HttpStatus statusCode;

            private final String statusText;

            private final String responseBody;

            public HttpStatusCodeException(WsResponse response) {
                super(response.getStatusCode().value() + " "
                        + response.getStatusText());
                this.statusCode = response.getStatusCode();
                this.statusText = response.getStatusText();
                this.responseBody = response.getString();
            }

            /**
             * HTTP status code.
             */
            public HttpStatus getStatusCode() {
                return this.statusCode;
            }

            /**
             * HTTP status text.
             */
            public String getStatusText() {
                return this.statusText;
            }

            /**
             * response body .
             */
            public String getResponseBodyAsString() {
                return this.responseBody;
            }
        }

        public static class HttpClientErrorException extends
                HttpStatusCodeException {
            private static final long serialVersionUID = 1L;

            public HttpClientErrorException(WsResponse response) {
                super(response);
            }
        }

        public static class HttpServerErrorException extends
                HttpStatusCodeException {
            public HttpServerErrorException(WsResponse response) {
                super(response);
            }

            private static final long serialVersionUID = 1L;
        }

    }

    public static interface IHeaderProvider{
    	HttpHeaders getHeaders();
    	
    }
    public static final IHeaderProvider EmptyHeaderProvider = new EmptyHeaderProvider();
    public static class EmptyHeaderProvider implements IHeaderProvider{
		@Override
		public HttpHeaders getHeaders() {
			return new HttpHeaders();
		}
    }
    static public interface IWsListener{
    	public void beforeRequest(WsRequest request);
 	    public void afterRequest(WsRequest request,WsResponse response);
    }
    /**请求响应周期监听器*/
    static public class DefaultWsListener implements IWsListener {
    	public void beforeRequest(WsRequest request){}
 	    public void afterRequest(WsRequest request,WsResponse response){}
    }
    static public class WsListeners{
    	private final List<IWsListener> listeners= new ArrayList<IWsListener>();
 
    	public void add(IWsListener listener){
    		listeners.add(listener);
    	}
    	public void remove(IWsListener listener){
    		listeners.remove(listener);
    	}
    	public void beforeRequest(WsRequest request){
			for (Iterator<IWsListener> all= listeners.iterator(); all.hasNext();){
				all.next().beforeRequest(request);
			}
    	}
 	    public void afterRequest(WsRequest request,WsResponse response){
			for (Iterator<IWsListener> all= listeners.iterator(); all.hasNext();){
				all.next().afterRequest(request,response);
			}
 	    }
    }
}
