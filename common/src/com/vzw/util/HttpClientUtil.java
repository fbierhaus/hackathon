/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vzw.util;

import com.vzw.util.LogUtil.WorkLogger;
import com.vzw.util.config.AbstractProperties;
import java.io.IOException;
import java.net.SocketException;
import java.nio.charset.Charset;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.scheme.SchemeSocketFactory;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import org.apache.log4j.Logger;

/**
 *
 * @author hud
 * 
 * To ease the usage of HttpComponent 4.x
 */
public class HttpClientUtil {
	
	private static final Logger				logger = Logger.getLogger(HttpClientUtil.class);
	
	private static final Charset			DEFAULT_CHARSET = Charset.forName("UTF-8");
	
	public static class Param {
		private static String makeKey(String scope, String name) {
			return scope + ".httpclient." + name;
		}
		
		private static String makeGlobalKey(String name) {
			return makeKey("global", name);
		}
		
		private static int getInt(AbstractProperties props, String scope, String name, int defVal) {
			return props.getInt(makeKey(scope, name), props.getInt(makeGlobalKey(name), defVal));
		}
		
		private static boolean getBoolean(AbstractProperties props, String scope, String name, boolean defVal) {
			return props.getBoolean(makeKey(scope, name), props.getBoolean(makeGlobalKey(name), defVal));
		}
		
		public static class Pool {
			public static enum Name {
				CONNECTION_LIFETIME_SECONDS("pool.connection_lifetime_seconds")
			,	DEFAULT_MAX_PER_ROUTE("pool.default_max_per_route")
			,	MAX_TOTAL("pool.max_total")

				;
				private String key = null;

				private Name(String key) {
					this.key = key;
				}

				public String key() {
					return key;
				}
				
			}
			
			private int			connectionLifetimeSeconds = -1;
			private int			defaultMaxPerRoute = 10;
			private int			maxTotal = 100;

			public Pool() {
			}
			
			public void setParams(AbstractProperties props, String scope) {
				connectionLifetimeSeconds = getInt(props, scope, Name.CONNECTION_LIFETIME_SECONDS.key(), connectionLifetimeSeconds);
				defaultMaxPerRoute = getInt(props, scope, Name.DEFAULT_MAX_PER_ROUTE.key(), defaultMaxPerRoute);
				maxTotal = getInt(props, scope, Name.MAX_TOTAL.key(), maxTotal);
			}

			public int getConnectionLifetimeSeconds() {
				return connectionLifetimeSeconds;
			}

			public void setConnectionLifetimeSeconds(int connectionLifetimeSeconds) {
				this.connectionLifetimeSeconds = connectionLifetimeSeconds;
			}

			public int getDefaultMaxPerRoute() {
				return defaultMaxPerRoute;
			}

			public void setDefaultMaxPerRoute(int defaultMaxPerRoute) {
				this.defaultMaxPerRoute = defaultMaxPerRoute;
			}

			public int getMaxTotal() {
				return maxTotal;
			}

			public void setMaxTotal(int maxTotal) {
				this.maxTotal = maxTotal;
			}
			
			
		}
		
		public static class Protocol {
			public static enum Name {
				USE_EXPECT_CONTINUE("protocol.use_expect_continue")
			,	WAIT_FOR_CONTINUE_MS("protocol.wait_for_continue_ms")

				;

				private String key = null;

				private Name(String key) {
					this.key = key;
				}

				public String key() {
					return key;
				}
			}
			
			private boolean		useExpectContinue = true;
			private int			waitForContinueMs = 20000;

			public Protocol() {
			}
			
			public void setParams(AbstractProperties props, String scope) {
				useExpectContinue = getBoolean(props, scope, Name.USE_EXPECT_CONTINUE.key(), useExpectContinue);
				waitForContinueMs = getInt(props, scope, Name.WAIT_FOR_CONTINUE_MS.key(), waitForContinueMs);
			}

			public boolean isUseExpectContinue() {
				return useExpectContinue;
			}

			public void setUseExpectContinue(boolean useExpectContinue) {
				this.useExpectContinue = useExpectContinue;
			}

			public int getWaitForContinueMs() {
				return waitForContinueMs;
			}

			public void setWaitForContinueMs(int waitForContinueMs) {
				this.waitForContinueMs = waitForContinueMs;
			}
			
			
		}
		
		public static class Connection {
			public static enum Name {
				TCP_NODELAY("connection.tcp_nodelay")
			,	SO_TIMEOUT_MS("connection.so_timeout_ms")
			,	SO_LINGER_MS("connection.so_linger_ms")
			,	SO_REUSEADDR("connection.so_reuseaddr")
			,	SOCKET_BUFFER_SIZE("connection.socket_buffer_size")
			,	CONNECTION_TIMEOUT_MS("connection.connection_timeout_ms")
			,	MAX_LINE_LENGTH("connection.max_line_length")
			,	MAX_HEADER_COUNT("connection.max_header_count")
			,	STALE_CONNECTION_CHECK("connection.stale_connection_check")
				;

				private String key = null;

				private Name(String key) {
					this.key = key;
				}

				public String key() {
					return key;
				}
				
			}
			
			private boolean		tcpNodelay = true;
			private int			soTimeoutMs = 30000;
			private int			soLingerMs = -1;
			private boolean		soReuseAddr = true;
			private int			socketBufferSize = 2048;
			private int			connectionTimeoutMs = 30000;
			private int			maxLineLength = 0;
			private int			maxHeaderCount = 0;
			private boolean		staleConnectionCheck = false;

			public Connection() {
			}
			
			public void setParams(AbstractProperties props, String scope) {
				tcpNodelay = getBoolean(props, scope, Name.TCP_NODELAY.key(), tcpNodelay);
				soTimeoutMs = getInt(props, scope, Name.SO_TIMEOUT_MS.key(), soTimeoutMs);
				soLingerMs = getInt(props, scope, Name.SO_LINGER_MS.key(), soLingerMs);
				soReuseAddr = getBoolean(props, scope, Name.SO_REUSEADDR.key(), soReuseAddr);
				socketBufferSize = getInt(props, scope, Name.SOCKET_BUFFER_SIZE.key(), socketBufferSize);
				connectionTimeoutMs = getInt(props, scope, Name.CONNECTION_TIMEOUT_MS.key(), connectionTimeoutMs);
				maxLineLength = getInt(props, scope, Name.MAX_LINE_LENGTH.key(), maxLineLength);
				maxHeaderCount = getInt(props, scope, Name.MAX_HEADER_COUNT.key(), maxHeaderCount);
				staleConnectionCheck = getBoolean(props, scope, Name.STALE_CONNECTION_CHECK.key(), staleConnectionCheck);
				
			}

			public int getConnectionTimeoutMs() {
				return connectionTimeoutMs;
			}

			public void setConnectionTimeoutMs(int connectionTimeoutMs) {
				this.connectionTimeoutMs = connectionTimeoutMs;
			}

			public int getMaxHeaderCount() {
				return maxHeaderCount;
			}

			public void setMaxHeaderCount(int maxHeaderCount) {
				this.maxHeaderCount = maxHeaderCount;
			}

			public int getMaxLineLength() {
				return maxLineLength;
			}

			public void setMaxLineLength(int maxLineLength) {
				this.maxLineLength = maxLineLength;
			}

			public int getSoLingerMs() {
				return soLingerMs;
			}

			public void setSoLingerMs(int soLingerMs) {
				this.soLingerMs = soLingerMs;
			}

			public boolean isSoReuseAddr() {
				return soReuseAddr;
			}

			public void setSoReuseAddr(boolean soReuseAddr) {
				this.soReuseAddr = soReuseAddr;
			}

			public int getSoTimeoutMs() {
				return soTimeoutMs;
			}

			public void setSoTimeoutMs(int soTimeoutMs) {
				this.soTimeoutMs = soTimeoutMs;
			}

			public int getSocketBufferSize() {
				return socketBufferSize;
			}

			public void setSocketBufferSize(int socketBufferSize) {
				this.socketBufferSize = socketBufferSize;
			}

			public boolean isStaleConnectionCheck() {
				return staleConnectionCheck;
			}

			public void setStaleConnectionCheck(boolean staleConnectionCheck) {
				this.staleConnectionCheck = staleConnectionCheck;
			}

			public boolean isTcpNodelay() {
				return tcpNodelay;
			}

			public void setTcpNodelay(boolean tcpNodelay) {
				this.tcpNodelay = tcpNodelay;
			}
		}
		
		
		public static class Execution {
			public static enum Name {
				RETRY_COUNT("execution.retry_count")
			,	HEALTH_CHECK_PERIOD_SECONDS("execution.health_check_period_seconds")
				;

				private String key = null;

				private Name(String key) {
					this.key = key;
				}

				public String key() {
					return key;
				}
				
			}
			
			private int			retryCount = 3;
			private int			healthCheckPeriodSeconds = 300;
			
			public Execution() {
				
			}
			
			public void setParams(AbstractProperties props, String scope) {
				retryCount = getInt(props, scope, Name.RETRY_COUNT.key(), retryCount);
				healthCheckPeriodSeconds = getInt(props, scope, Name.HEALTH_CHECK_PERIOD_SECONDS.key(), healthCheckPeriodSeconds);
				
			}

			public int getRetryCount() {
				return retryCount;
			}

			public void setRetryCount(int retryCount) {
				this.retryCount = retryCount;
			}

			public int getHealthCheckPeriodSeconds() {
				return healthCheckPeriodSeconds;
			}

			public void setHealthCheckPeriodSeconds(int healthCheckPeriodSeconds) {
				this.healthCheckPeriodSeconds = healthCheckPeriodSeconds;
			}
			
			
		}
		
		private Pool		pool = new Pool();
		private Protocol	protocol = new Protocol();
		private Connection	connection = new Connection();
		private Execution	execution = new Execution();

		public Param() {
		}

		public Connection getConnection() {
			return connection;
		}

		public void setConnection(Connection connection) {
			this.connection = connection;
		}

		public Pool getPool() {
			return pool;
		}

		public void setPool(Pool pool) {
			this.pool = pool;
		}

		public Protocol getProtocol() {
			return protocol;
		}

		public void setProtocol(Protocol protocol) {
			this.protocol = protocol;
		}

		public Execution getExecution() {
			return execution;
		}

		public void setExecution(Execution execution) {
			this.execution = execution;
		}
		
		
	}
	
	
	public static class BypassTrustManager implements X509TrustManager {

		@Override
		public void checkClientTrusted(X509Certificate[] xcs, String string) throws CertificateException {
			// do nothing, we do not check certificate here as we know it's optionshouse
		}

		@Override
		public void checkServerTrusted(X509Certificate[] xcs, String string) throws CertificateException {
			// do nothing, we do not check certificate here as we know it's optionshouse
		}

		@Override
		public X509Certificate[] getAcceptedIssuers() {
			return null;
		}
		
	}
	
	
	public static class RetryHandler implements HttpRequestRetryHandler {
		
		private final int		retryCount;
		
		public RetryHandler(int retryCount) {
			this.retryCount = retryCount;
		}
		
		@Override
		public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {
			
			boolean ret = false;
			if(executionCount < retryCount){
				if(exception instanceof NoHttpResponseException){
					//LogUtil.info(logger, exception, "Retry HTTP Request started (count = {0})", executionCount);
					LogUtil.warn(logger, "NoHttpResponseException: {0}. Retry HTTP Request started (count = {1})", exception.getMessage(), executionCount);
					ret = true;
				}
				else if (exception instanceof ClientProtocolException){
					//LogUtil.info(logger, exception, "Retry HTTP Request started (count = {0})", executionCount);
					LogUtil.warn(logger, "ClientProtocolException: {0}. Retry HTTP Request started (count = {1})", exception.getMessage(), executionCount);
					ret = true;
				}
				else if (exception instanceof SocketException){
					//LogUtil.info(logger, exception, "Retry HTTP Request started (count = {0})", executionCount);
					LogUtil.warn(logger, "SocketException: {0}. Retry HTTP Request started (count = {1})", exception.getMessage(), executionCount);
					ret = true;
				}
				else {
					LogUtil.error(logger, exception, "Unknown exception was thrown. Retry HTTP Request stopped (count = {0})", executionCount);
				}
			} 
			else {
				LogUtil.error(logger, exception, "Reached maximum retries. Retry HTTP Request ended (count = {0})", executionCount);
			}
				
			return ret;
		}
		
	}
	
	
	public static class BaseUrl {
		private boolean							valid = true;		// whether the url is valid or not
		private String							url = null;			// the actual url
		
		/**
		 * Only contains scheme://host:port
		 */
		private String							rootUrl = null;
		
		
		public BaseUrl() {
			
		}
		
		public BaseUrl(String url) {
			this(url, true);
		}
		
		public BaseUrl(boolean valid) {
			this(null, valid);
		}
		
		public BaseUrl(String url, boolean valid) {
			this.setUrl(url);
			this.valid = valid;
		}

		public boolean isValid() {
			return valid;
		}

		public void setValid(boolean valid) {
			this.valid = valid;
		}

		public String getUrl() {
			return url;
		}

		final public void setUrl(String url) {
			URLUtil.URLComponents uc = URLUtil.parse(url);
			this.url = url;
			this.rootUrl = uc.toRootUrl();
		}

		public String getRootUrl() {
			return rootUrl;
		}
		
		

		@Override
		public String toString() {
			return String.format("valid=%b,url=%s",	valid, url);
		}

		@Override
		public BaseUrl clone() {
			BaseUrl ret = new BaseUrl();
			ret.setValid(valid);
			ret.setUrl(url);
			return ret;
		}
		
		
		
	}
	
	
	/**
	 * This class also supports base url health check
	 * Basically, all the baseUrls will be health checked in a scheduled executor. If it fails
	 * it will be marked as a invalid one and will be checked again later. In this case, the url will
	 * be skipped when getNextBaseUrl gets called
	 * 
	 * 
	 * More over, here is how it is working
	 * 
	 * 1. A next base url is retrieved
	 * 2. A request has been made
	 * 3. An IOException is thrown
	 */
	public static class Client {
		private HttpClient						httpClient = null;
		
		/**
		 * Base url for the requests, will need to be rotated
		 */
		private BaseUrl[]						baseUrls = null;
		
		/**
		 * If not provided, use "/" by default
		 */
		private String							healthCheckUri = null;
		
		/**
		 * for rotation
		 */
		private int								curBaseUrlIndex = 0;
		
		
		
		private ScheduledExecutorService		healthCheckExecutor = null;
		
		
		private ReentrantLock					accessUrlsLock = new ReentrantLock();
		

		/**
		 * This method will go over all the base urls and update their flag as needed
		 * If for some reason a request comes in while health check is not in time able to
		 * find out a stale connection, an IOException is thrown anyway to the client, though
		 * it is rare.
		 * 
		 * Use HttpGet always
		 */
		private class HealthCheckRunnable implements Runnable {

			@Override
			public void run() {
				LogUtil.info(logger, "HttpClientUtil health check started.");
				
				// make a copy first
				BaseUrl[] _baseUrls = new BaseUrl[baseUrls.length];
				for (int i = 0; i < _baseUrls.length; ++ i) {
					_baseUrls[i] = baseUrls[i].clone();
				}
				
				for (BaseUrl baseUrl : _baseUrls) {
					final String healthCheckUrl;
					
					if (healthCheckUri == null) {
						healthCheckUrl = baseUrl.getUrl();	// use default
					}
					else {
						healthCheckUrl = baseUrl.getRootUrl() + healthCheckUri;
					}
					LogUtil.info(logger, "HttpClientUtil health check on: {0}.", healthCheckUrl);

					try {
						HttpGet req = new HttpGet(healthCheckUrl);
						httpClient.execute(req, new ResponseHandler<Boolean>() {

							@Override
							public Boolean handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
								// do nothing as it just responded.
								LogUtil.info(logger, "HttpClientUtil health check done successfully on: {0} ", healthCheckUrl);
								return true;
							}
						});
						
						// upon success, set valid to true
						baseUrl.setValid(true);
					}
					catch (IOException e) {
						LogUtil.info(logger, e, "HttpClientUtil health check failed on: {0} ", healthCheckUrl);
						
						// set valid flag to false
						baseUrl.setValid(false);
					}
				}
				
				// once done, update the baseurl, need synchronization
				accessUrlsLock.lock();
				try {
					for (int i = 0; i < _baseUrls.length; ++ i) {
						baseUrls[i].setValid(_baseUrls[i].isValid());
					}
				}
				finally {
					accessUrlsLock.unlock();
				}
				
				LogUtil.info(logger, "HttpClientUtil health check ended.");
			}
			
		}

		private Client(String[] baseUrls, String healthCheckUri, HttpClient httpClient, int healthCheckPeriodSeconds) {
			this.baseUrls = new BaseUrl[baseUrls.length];
			for (int i = 0; i < baseUrls.length; ++ i) {
				this.baseUrls[i] = new BaseUrl(baseUrls[i]);
			}
			
			this.httpClient = httpClient;
			
			this.healthCheckUri = healthCheckUri;
			
			// init health check if healthCheckUri is not null
			if (healthCheckUri != null) {
				initHealthCheck(healthCheckPeriodSeconds);
			}
		}
		
		private void initHealthCheck(int healthCheckPeriodSeconds) {
			if (baseUrls.length <= 1) {
				return;		// do nothing if only one base url
			}
			
			
			// need to set up scheduled executor
			healthCheckExecutor = Executors.newSingleThreadScheduledExecutor();
			healthCheckExecutor.scheduleAtFixedRate(new HealthCheckRunnable(), 0, healthCheckPeriodSeconds, TimeUnit.SECONDS);
		}
		
		
		public HttpClient getHttpClient() {
			return httpClient;
		}

		
		public void shutdown() {
			if (healthCheckExecutor != null) {
				healthCheckExecutor.shutdown();
			}
			
			if (httpClient != null) {
				ClientConnectionManager cm = httpClient.getConnectionManager();
				if (cm != null) {
					cm.shutdown();
				}
				
				httpClient = null;
			}
			
		}
		
		/**
		 * For rotation
		 * @return httpClient
		 */
		public String getNextBaseUrl(WorkLogger workLogger) {
			String retUrl = null;
			boolean warn = false;
			
			accessUrlsLock.lock();
			
			try {
				
				for (int cnt = 0; cnt < baseUrls.length; ++ cnt) {
					BaseUrl buNext = baseUrls[curBaseUrlIndex];
					curBaseUrlIndex = (curBaseUrlIndex + 1) % baseUrls.length;
					if (buNext.isValid()) {
						retUrl = buNext.getUrl();
						break;
					}
				}

				if (retUrl == null) {
					warn = true;
					retUrl = baseUrls[0].getUrl();
				}

			}
			finally {
				accessUrlsLock.unlock();
			}

			if (warn) {
				LogUtil.warn(workLogger, "Unable to get http base url as all the health checks seem to have failed. Use the first by default: {0}", baseUrls[0]);
			}
			
			LogUtil.info(workLogger, "got next url={0}", retUrl);
			return retUrl;
		}
		
		
		public String getNextBaseUrl() {
			return getNextBaseUrl(new WorkLogger(logger, null));
		}
		
		
		/**
		 * Wrapper of executing HTTP request. Support health check and base url rotation
		 * If there is only one base url, no healthcheck or rotation is needed
		 * 
		 * Currently, it does nothing as health check is handled in background
		 * @param <T>
		 * @param req
		 * @param rh
		 * @return
		 * @throws IOException 
		 */
		public <T> T execute(HttpUriRequest req, ResponseHandler<? extends T> rh) throws IOException {
			return httpClient.execute(req, rh);
		}
		
		/**
		 * 
		 * @param <T>
		 * @param req
		 * @param rh
		 * @param context
		 * @return
		 * @throws IOException 
		 */
		public <T> T execute(HttpUriRequest req, ResponseHandler<? extends T> rh, HttpContext context) throws IOException {
			return httpClient.execute(req, rh, context);
		}
		
		
	}
	
	
	
	
	/**
	 * key is:
	 * <scope>.httpclient.<param_name>
	 * 
	 * @param props
	 * @param prefix
	 * @return 
	 */
	public static Param getParam(AbstractProperties props, String scope) {
		Param param = new Param();
		
		param.getProtocol().setParams(props, scope);
		param.getPool().setParams(props, scope);
		param.getConnection().setParams(props, scope);
		param.getExecution().setParams(props, scope);
		
		return param;
	}
	
	
	public static SchemeSocketFactory createBypassSSLSocketFactory() throws Exception {
		BypassTrustManager trustManager = new BypassTrustManager();
		SSLContext sslContext = SSLContext.getInstance("TLS");
		sslContext.init(null, new TrustManager[] {trustManager}, null);
		SSLSocketFactory socketFactory = new SSLSocketFactory(sslContext);
		return socketFactory;
		
	}
	
	public static Client initClient(AbstractProperties props, String scope, String[] baseUrls, String healthCheckUri) {
		return initClient(getParam(props, scope), baseUrls, healthCheckUri, null);
	}
	
	public static Client initClient(AbstractProperties props, String scope, String[] baseUrls) {
		return initClient(getParam(props, scope), baseUrls);
	}
	
	public static Client initClient(AbstractProperties props, String scope, String[] baseUrls, SchemeSocketFactory socketFactory) {
		return initClient(getParam(props, scope), baseUrls, null, socketFactory);
	}
	
	public static Client initClient(Param param, String[] baseUrls) {
		return initClient(param, baseUrls, null, null);
	}
	
	
	public static Client initClient(AbstractProperties props, String scope, String url) {
		return initClient(getParam(props, scope), url);
	}
	
	public static Client initClient(AbstractProperties props, String scope, String url, String healthCheckUri) {
		return initClient(getParam(props, scope), new String[]{url}, healthCheckUri, null);
	}
	
	public static Client initClient(AbstractProperties props, String scope, String url, SchemeSocketFactory socketFactory) {
		return initClient(getParam(props, scope), new String[]{ url }, null, socketFactory);
	}
	
	public static Client initClient(Param param, String url) {
		return initClient(param, new String[]{ url }, null, null);
	}
	
	/**
	 * Prepare the http client
	 * 
	 * @param param
	 * @param baseUrls
	 * @param healthCheckUri
	 *		The actual healthCheckUrl = schema:host:port + healthCheckUri
	 * @param socketFactory
	 * @return 
	 */
	public static Client initClient(Param param, String[] baseUrls, String healthCheckUri, SchemeSocketFactory socketFactory) {
		
		Client client = null;

		try {
			
			// create scheme registry
			SchemeRegistry schemeRegistry = new SchemeRegistry();
			
			// usually only one scheme is registered
			for (String baseUrl : baseUrls) {
				URLUtil.URLComponents uc = URLUtil.parse(baseUrl);
				Scheme scheme = schemeRegistry.get(uc.getScheme());
				if (scheme == null) {
					// get socketFactory
					SchemeSocketFactory ssf = socketFactory;

					if (ssf == null) {
						if (uc.isSecured()) {
							ssf = createBypassSSLSocketFactory();
						}
						else {
							ssf = PlainSocketFactory.getSocketFactory();
						}
					}
					
					// register it
					scheme = new Scheme(uc.getScheme(), uc.getPort(), ssf);
					schemeRegistry.register(scheme);
				}
			}
			



			PoolingClientConnectionManager httpConnManager = new PoolingClientConnectionManager(
					schemeRegistry, 
					param.getPool().getConnectionLifetimeSeconds(),
					TimeUnit.SECONDS);
			httpConnManager.setMaxTotal(param.getPool().getMaxTotal());
			httpConnManager.setDefaultMaxPerRoute(param.getPool().getDefaultMaxPerRoute());


			// create the pooled client
			HttpParams httpParams = new BasicHttpParams();
			httpParams.setBooleanParameter(CoreProtocolPNames.USE_EXPECT_CONTINUE, param.getProtocol().isUseExpectContinue());
			httpParams.setIntParameter(CoreProtocolPNames.WAIT_FOR_CONTINUE, param.getProtocol().getWaitForContinueMs());
			httpParams.setBooleanParameter(CoreConnectionPNames.TCP_NODELAY, param.getConnection().isTcpNodelay());
			httpParams.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, param.getConnection().getSoTimeoutMs());
			httpParams.setIntParameter(CoreConnectionPNames.SO_LINGER, param.getConnection().getSoLingerMs());
			httpParams.setBooleanParameter(CoreConnectionPNames.SO_REUSEADDR, param.getConnection().isSoReuseAddr());
			httpParams.setIntParameter(CoreConnectionPNames.SOCKET_BUFFER_SIZE, param.getConnection().getSocketBufferSize());
			httpParams.setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, param.getConnection().getConnectionTimeoutMs());
			httpParams.setIntParameter(CoreConnectionPNames.MAX_LINE_LENGTH, param.getConnection().getMaxLineLength());
			httpParams.setIntParameter(CoreConnectionPNames.MAX_HEADER_COUNT, param.getConnection().getMaxHeaderCount());
			httpParams.setBooleanParameter(CoreConnectionPNames.STALE_CONNECTION_CHECK, param.getConnection().isStaleConnectionCheck());

			DefaultHttpClient hc = new DefaultHttpClient(httpConnManager, httpParams);
			hc.setHttpRequestRetryHandler(new RetryHandler(param.getExecution().getRetryCount()));


			client = new Client(baseUrls, healthCheckUri, hc, param.getExecution().getHealthCheckPeriodSeconds());
		}		
		catch (Exception e) {
			LogUtil.error(logger, e, "Failed to initialize HttpClientUtil");
		}
		
		return client;
		
	}
	
	
	/**
	 * 
	 * @param request
	 * @param param 
	 */
	public static UrlEncodedFormEntity setFormParams(HttpPost request, NameValuePair...params) {
		
		UrlEncodedFormEntity entity = null;
		try {
			List<NameValuePair> paramList = Arrays.asList(params);
			entity = new UrlEncodedFormEntity(paramList, DEFAULT_CHARSET.name());
			request.setEntity(entity);
		}
		catch (Exception e) {
			LogUtil.error(logger, e, "Failed to set form params");
		}
		
		return entity;
	}
}
