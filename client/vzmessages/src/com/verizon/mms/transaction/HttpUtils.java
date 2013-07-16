/*
 * Copyright (C) 2008 Esmertec AG.
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.verizon.mms.transaction;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.params.ConnRouteParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;

import android.content.Context;
import android.net.http.AndroidHttpClient;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.mms.MmsConfig;
import com.verizon.mms.TimeOutException;

public class HttpUtils {
	// private static final String TAG = LogTag.TRANSACTION;

	public static final int HTTP_POST_METHOD = 1;
	public static final int HTTP_GET_METHOD = 2;

	// This is the value to use for the "Accept-Language" header.
	// Once it becomes possible for the user to change the locale
	// setting, this should no longer be static. We should call
	// getHttpAcceptLanguage instead.
	private static final String HDR_VALUE_ACCEPT_LANGUAGE;

	static {
		HDR_VALUE_ACCEPT_LANGUAGE = getHttpAcceptLanguage();
	}

	// Definition for necessary HTTP headers.
	private static final String HDR_KEY_ACCEPT = "Accept";
	private static final String HDR_KEY_ACCEPT_LANGUAGE = "Accept-Language";

	private static final String HDR_VALUE_ACCEPT = "*/*, application/vnd.wap.mms-message, application/vnd.wap.sic";

	private HttpUtils() {
		// To forbidden instantiate this class.
	}

	/**
	 * A helper method to send or retrieve data through HTTP protocol.
	 * 
	 * @param token
	 *            The token to identify the sending progress.
	 * @param url
	 *            The URL used in a GET request. Null when the method is
	 *            HTTP_POST_METHOD.
	 * @param pdu
	 *            The data to be POST. Null when the method is HTTP_GET_METHOD.
	 * @param method
	 *            HTTP_POST_METHOD or HTTP_GET_METHOD.
	 * @return A byte array which contains the response data. If an HTTP error
	 *         code is returned, an IOException will be thrown.
	 * @throws IOException
	 *             if any error occurred on network interface or an HTTP error
	 *             code(&gt;=400) returned from the server.
	 */
	public static byte[] httpConnection(Context context, long token,
			String url, byte[] pdu, int method, boolean isProxySet,
			String proxyHost, int proxyPort) throws IOException {

		final HttpConnector httpConnector = new HttpConnector(context, token,
				url, pdu, method, isProxySet, proxyHost, proxyPort);
		Thread timedHttpThread = new Thread(httpConnector, "TimedHttpTransaction");
		// Start thread
		timedHttpThread.start();
		try {
			// Join
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(HttpUtils.class,
						"====>>>> Joining for " + MmsConfig.getHttpReadTimeout() + " ms.");
			}
			timedHttpThread.join(MmsConfig.getHttpReadTimeout());
		} catch (InterruptedException itrExp) {
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(HttpUtils.class,
						"====>>>> Finished waiting. InterruptedException - threadAlive=" + timedHttpThread.isAlive());
			}
			handleHttpConnectionException(itrExp, url);
		}

		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(HttpUtils.class,
					"====>>>> Joined. timedHttpThread alive? " + timedHttpThread.isAlive());
		}
		
		if (timedHttpThread.isAlive()) {
			if (httpConnector.shouldWaitUntilFinish.get()) {
				if (Logger.IS_DEBUG_ENABLED) {
					Logger.debug(HttpUtils.class,
							"====>>>> The transaction reached the point of no return, we must wait for a response");
				}
				while (timedHttpThread.isAlive()) {
					try {
						timedHttpThread.join();
					} catch (InterruptedException itrExp) {
	                    Logger.error(itrExp);
					}
				}
				if (Logger.IS_DEBUG_ENABLED) {
					Logger.debug(HttpUtils.class, "====>>>> The worker thread has completed successfully");
				}					
				return handleHttpResponse(httpConnector);
			}

			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(HttpUtils.class, "====>>>> Interrupting timedHttpThread");
			}			
			
			timedHttpThread.interrupt(); // Send interrupt
			if (httpConnector.req != null) {
				if (Logger.IS_DEBUG_ENABLED) {
					Logger.debug(HttpUtils.class, "====>>>> Aborting the request");
				}					
				httpConnector.req.abort();
			} else {
				if (Logger.IS_DEBUG_ENABLED) {
					Logger.debug(HttpUtils.class,
							"====>>>> HttpConnection Lifetime exceeded before the client was started");
				}
			}
			throw new TimeOutException("Connection Lifetime Exceeded");
		}

		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(HttpUtils.class, "====>>>> The worker thread has completed successfully2");
		}					
		return handleHttpResponse(httpConnector);
	}

	private static class HttpConnector implements Runnable {

		final Context context;
		final long token;
		final String url;
		final byte[] pdu;
		final int method;
		final boolean isProxySet;
		final String proxyHost;
		final int proxyPort;

		HttpRequestBase req = null;
		byte[] response = null;
		Exception exception = null;
		AtomicBoolean shouldWaitUntilFinish = new AtomicBoolean(false);

		public HttpConnector(Context context, long token, String url,
				byte[] pdu, int method, boolean isProxySet, String proxyHost,
				int proxyPort) {
			super();
			this.context = context;
			this.token = token;
			this.url = url;
			this.pdu = pdu;
			this.method = method;
			this.isProxySet = isProxySet;
			this.proxyHost = proxyHost;
			this.proxyPort = proxyPort;
		}

		@Override
		public void run() {
			try {
				this.response = _httpConnection(context, token, url, pdu,
						method, isProxySet, proxyHost, proxyPort);
			} catch (Exception e) {
				this.exception = e;
			}
		}

		private byte[] _httpConnection(Context context, long token, String url,
				byte[] pdu, int method, boolean isProxySet, String proxyHost,
				int proxyPort) throws IOException {
			if (url == null) {
				throw new IllegalArgumentException("URL must not be null.");
			}

			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(HttpUtils.class,
						"#====> httpConnection: params list");
				Logger.debug(HttpUtils.class, "\ttoken\t\t= " + token);
				Logger.debug(HttpUtils.class, "\turl\t\t= " + url);
				Logger.debug(HttpUtils.class, "\tmethod\t\t= "
						+ ((method == HTTP_POST_METHOD) ? "POST"
								: ((method == HTTP_GET_METHOD) ? "GET"
										: "UNKNOWN")));
				Logger.debug(HttpUtils.class, "\tisProxySet\t= " + isProxySet);
				Logger.debug(HttpUtils.class, "\tproxyHost\t= " + proxyHost);
				Logger.debug(HttpUtils.class, "\tproxyPort\t= " + proxyPort);
				// TODO Print out binary data more readable.
				// Log.v(TAG, "\tpdu\t\t= " + Arrays.toString(pdu));
			}

			AndroidHttpClient client = null;

			try {
				// Make sure to use a proxy which supports CONNECT.
				URI hostUrl = new URI(url);
				HttpHost target = new HttpHost(hostUrl.getHost(),
						hostUrl.getPort(), HttpHost.DEFAULT_SCHEME_NAME);

				client = createHttpClient(context);
				switch (method) {
				case HTTP_POST_METHOD:
					ProgressCallbackEntity entity = new ProgressCallbackEntity(
							context, token, pdu);
					// Set request content type.
					entity.setContentType("application/vnd.wap.mms-message");

					HttpPost post = new HttpPost(url);
					post.setEntity(entity);
					req = post;
					break;
				case HTTP_GET_METHOD:
					req = new HttpGet(url);
					break;
				default:
					Logger.error(HttpUtils.class, "Unknown HTTP method: "
							+ method + ". Must be one of POST["
							+ HTTP_POST_METHOD + "] or GET[" + HTTP_GET_METHOD
							+ "].");
					return null;
				}
				
				if (Logger.IS_DEBUG_ENABLED) {
					Logger.debug(HttpConnector.class, "====>>>> Request formed");
				}					

				// Set route parameters for the request.
				HttpParams params = client.getParams();
				if (isProxySet) {
					ConnRouteParams.setDefaultProxy(params, new HttpHost(
							proxyHost, proxyPort));
				}
				req.setParams(params);

				// Set necessary HTTP headers for MMS transmission.
				req.addHeader(HDR_KEY_ACCEPT, HDR_VALUE_ACCEPT);
				String lineNum = ((TelephonyManager) context
						.getSystemService(Context.TELEPHONY_SERVICE))
						.getLine1Number();
				if (null == lineNum) {
					lineNum = "";
				}
				String line1Number;
				if (lineNum.startsWith("+1")) {
					line1Number = lineNum.substring(1);
				} else {
					if ((lineNum.length() != 10) || (lineNum.startsWith("1"))) {
						Logger.error(getClass(), "MMS sending unexp linenumber: " + lineNum);
						line1Number = lineNum;
					} else {
						line1Number = "1" + lineNum;
					}
				}
				if (Logger.IS_DEBUG_ENABLED) {
					Logger.debug(HttpUtils.class, "Added X-VZW-MDN:"
							+ line1Number);
				}
				req.addHeader("X-VZW-MDN", line1Number);
				req.addHeader("X-VZW-MMS_Special_Ind", "vzmessages");
				{
					String xWapProfileTagName = MmsConfig.getUaProfTagName();
					String xWapProfileUrl = MmsConfig.getUaProfUrl();

					if (xWapProfileUrl != null) {
						req.addHeader(xWapProfileTagName, xWapProfileUrl);
					}
				}

				// Extra http parameters. Split by '|' to get a list of value
				// pairs.
				// Separate each pair by the first occurrence of ':' to obtain a
				// name and
				// value. Replace the occurrence of the string returned by
				// MmsConfig.getHttpParamsLine1Key() with the users telephone
				// number
				// inside
				// the value.
				String extraHttpParams = MmsConfig.getHttpParams();

				if (extraHttpParams != null) {
					String line1Key = MmsConfig.getHttpParamsLine1Key();
					String paramList[] = extraHttpParams.split("\\|");

					for (String paramPair : paramList) {
						String splitPair[] = paramPair.split(":", 2);

						if (splitPair.length == 2) {
							String name = splitPair[0].trim();
							String value = splitPair[1].trim();

							if (line1Key != null) {
								value = value.replace(line1Key, line1Number);
							}
							if (!TextUtils.isEmpty(name)
									&& !TextUtils.isEmpty(value)) {
								req.addHeader(name, value);
							}
						}
					}
				}
				req.addHeader(HDR_KEY_ACCEPT_LANGUAGE,
						HDR_VALUE_ACCEPT_LANGUAGE);
				
				if (Logger.IS_DEBUG_ENABLED) {
					Logger.debug(HttpConnector.class, "====>>>> Will execute request now");
				}					

				int retryCount = 0;
				HttpResponse response = null;
				while ((response == null) && (retryCount < 2)) {
					try {
						response = client.execute(target, req);
					} catch(org.apache.http.conn.ConnectTimeoutException cte) {
						retryCount++;
						if (Logger.IS_DEBUG_ENABLED) {
							Logger.debug(HttpConnector.class, "====>>>> Got ConnectTimeoutRetrying");
						}										
					}
				}
				
				if (Logger.IS_DEBUG_ENABLED) {
					Logger.debug(HttpConnector.class, "====>>>> Received respone");
					if (retryCount > 0) {
						Logger.debug(HttpUtils.class, "Did retry and succeeded!! - " + retryCount);
					}
				}					

				StatusLine status = response.getStatusLine();
				
				if (Logger.IS_DEBUG_ENABLED) {
					Logger.debug(HttpConnector.class, "====>>>> Response status ==> " + status.getStatusCode());
				}					

				final int statusCode = status.getStatusCode();
				if (status.getStatusCode() != 200) { // HTTP 200 is success.
					throw new HTTPException("HTTP error: " + status.getReasonPhrase(), statusCode);
				}

				// If method==post, and status is 200, then force wait until
				// whole response is read
				if (method == HTTP_POST_METHOD) {
					this.shouldWaitUntilFinish.set(true);
				}

				if (Logger.IS_DEBUG_ENABLED) {
					Logger.debug(HttpConnector.class, "====>>>> Reading response body");
				}					

				HttpEntity entity = response.getEntity();
				byte[] body = null;
				if (entity != null) {
					try {
						if (entity.getContentLength() > 0) {
							body = new byte[(int) entity.getContentLength()];
							DataInputStream dis = new DataInputStream(
									entity.getContent());
							try {
								dis.readFully(body);
							} finally {
								try {
									dis.close();
								} catch (IOException e) {
									Logger.error(HttpUtils.class, e);
								}
							}
						}
					} finally {
						if (entity != null) {
							entity.consumeContent();
						}
					}
				}
				
				if (Logger.IS_DEBUG_ENABLED) {
					Logger.debug(HttpConnector.class, "====>>>> Response is read fully");
				}					

				return body;
			} catch (URISyntaxException e) {
				handleHttpConnectionException(e, url);
			} catch (IllegalStateException e) {
				handleHttpConnectionException(e, url);
			} catch (IllegalArgumentException e) {
				handleHttpConnectionException(e, url);
			} catch (SocketException e) {
				handleHttpConnectionException(e, url);
			} catch (Exception e) {
				handleHttpConnectionException(e, url);
			} finally {
				if (client != null) {
					client.close();
				}
			}
			return null;
		}

	}

	private static byte[] handleHttpResponse(HttpConnector httpConnector)
			throws IOException {
		if (httpConnector.exception != null) {
			handleHttpConnectionException(httpConnector.exception,
					httpConnector.url);
		}
		return httpConnector.response;
	}

	private static void handleHttpConnectionException(Exception exception,
			String url) throws IOException {
		if (Logger.IS_ERROR_ENABLED) {
			Logger.error(HttpUtils.class, "handleHttpConnectionException ===>>>> " + "Url: " + url + "\n" + exception.getMessage());
		}
		// Inner exception should be logged to make life easier.
		IOException e = new IOException(exception.getMessage());
		e.initCause(exception);
		throw e;
	}

	private static AndroidHttpClient createHttpClient(Context context) {
		String userAgent = MmsConfig.getUserAgent();
		AndroidHttpClient client = AndroidHttpClient.newInstance(userAgent,
				context);
		HttpParams params = client.getParams();
		HttpProtocolParams.setContentCharset(params, "UTF-8");

		// set the socket timeout
		int soTimeout = MmsConfig.getHttpSocketTimeout();
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(HttpUtils.class,
					"###############===> [HttpUtils] createHttpClient w/ socket timeout "
							+ soTimeout + " ms, " + "UA=" + userAgent);
		}
		// Toast.makeText(context, String.format("Model=%s, UA=%s",
		// OEM.deviceModel, userAgent),
		// Toast.LENGTH_LONG).show();

		HttpConnectionParams.setSoTimeout(params, soTimeout);
		return client;
	}

	/**
	 * Return the Accept-Language header. Use the current locale plus US if we
	 * are in a different locale than US.
	 */
	private static String getHttpAcceptLanguage() {
		Locale locale = Locale.getDefault();
		StringBuilder builder = new StringBuilder();

		addLocaleToHttpAcceptLanguage(builder, locale);
		if (!locale.equals(Locale.US)) {
			if (builder.length() > 0) {
				builder.append(", ");
			}
			addLocaleToHttpAcceptLanguage(builder, Locale.US);
		}
		return builder.toString();
	}

	private static void addLocaleToHttpAcceptLanguage(StringBuilder builder,
			Locale locale) {
		String language = locale.getLanguage();

		if (language != null) {
			builder.append(language);

			String country = locale.getCountry();

			if (country != null) {
				builder.append("-");
				builder.append(country);
			}
		}
	}
}
