/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.vzw.util;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.log4j.Logger;

/**
 *
 * @author hud
 *
 * Use thread local to avoid synchronization issue
 * 
 * 2013-04-23: Added crypto method, default to "AES/ECB/PKCS5Padding" for cipher and "AES" for key generator
 * 
 * We also support simple encryption using HashUtil to work around sun bugs
 *	cipherAlgo = "HashUtil:method?" where ? may be 1, 2, 3
 *	keyAlgo = as usual
 *	keyLen = as usual
 */
public class CryptoUtils {
	
	private static final Logger		logger = Logger.getLogger(CryptoUtils.class);

	/**
	 * The charset used for encryption an decryption
	 */
	private static final Charset	CHARSET = Charset.forName("UTF-8");
	
	/**
	 * Default configuration
	 */
	public static final String		DEFAULT_CIPHER_ALGO = "AES/ECB/PKCS5Padding";
	public static final String		DEFAULT_KEY_ALGO = "AES";
	public static final int			DEFAULT_KEY_LEN = 128;
	
	public static final String		HASH_ALGO_PREFIX = "HashUtils:";
	
	
	
	public static class HashSecretKey implements SecretKey {
		private byte[]		encodedKey = null;
		
		
		public HashSecretKey(String keySpec) {
			// convert to array
			encodedKey = keySpec.getBytes(CHARSET);
		}

		@Override
		public String getAlgorithm() {
			return "HashUtils";
		}

		@Override
		public String getFormat() {
			return "RAW";
		}

		@Override
		public byte[] getEncoded() {
			return encodedKey;
		}
		
	}

	
	/**
	 * The executor, there is a default executor which  can be accessed through
	 * CryptoUtils's static methods
	 */
	public static class Executor {
		
		private class _Worker {

			/**
			 * May be either encryption of decryption
			 */
			private Cipher			cipher = null;

			/**
			 * Key generator is per thread, not global
			 */
			private KeyGenerator	keyGenerator = null;


			/**
			 * The secure random
			 */
			private Random			random = null;


			private _Worker() throws Exception {

				// initialize random
				initRandom();


				// now initialize them
				if (hashMethod == null) {
					cipher = Cipher.getInstance(cipherAlgo);
				}
				keyGenerator = KeyGenerator.getInstance(keyAlgo);
				keyGenerator.init(keyLen);//, random);

			}

			private void initRandom() {
				// two steps
				/*
				SecureRandom r0;

				try {
					r0 = SecureRandom.getInstance("SHA1PRNG");
				}
				catch (NoSuchAlgorithmException e) {
					r0 = new SecureRandom();
				}
				*/ 
				random = new Random(new Date().getTime());

				//r0.setSeed(new Date().getTime());

				//byte[] seed = r0.generateSeed(SEED_LEN);
				//random = new SecureRandom(seed);

			}

			private int nextRandomInt(boolean half) {
				int ret = 0;
				do {
					// we need some space above
					ret = random.nextInt(half ? Integer.MAX_VALUE / 2 : Integer.MAX_VALUE);

					// do not get 0
					if (ret != 0) {
						break;
					}
				} while (true);

				return ret;
			}

			/**
			 * This is special function. use nextBytes and Base64 encoding
			 *
			 * @param length
			 * @return
			 */
			private String nextRandomString(int length) {

				// normalize length to multiple of 4
				int len1 = (length - 1) / 4 + 1;
				int blen = len1 * 3;

				byte[] outBytes = new byte[blen];
				random.nextBytes(outBytes);

				String ret = Base64.encodeBase64String(outBytes);

				len1 *= 4;

				if (len1 > length) {
					ret = ret.substring(0, length);
				}

				return ret;
			}

			private SecretKey generateKey() {
				if (keyGenerator != null) {
					return keyGenerator.generateKey();
				}
				else {
					// generate a random key
					return generateKey(nextRandomString(keyLen));
				}
			}

			private SecretKey generateKey(String keySpec) {
				if (keySpec == null || keySpec.length() < keyLen / 8) {
					return null;
				}

				String k0 = keySpec.substring(0, keyLen / 8);
				SecretKey key = new SecretKeySpec(k0.getBytes(CHARSET), keyAlgo);

				return key;
			}

			/**
			 * 
			 * @param key
			 * @param inStr
			 * @return
			 * @throws Exception 
			 */
			private String encrypt(SecretKey key, String inStr) throws Exception {
				
				if (hashMethod != null) {
					return HashUtils.encode(hashMethod, key.getEncoded(), inStr);
				}
				else {
				
					cipher.init(Cipher.ENCRYPT_MODE, key);

					byte[] inBytes = inStr.getBytes(CHARSET);
					byte[] outBytes = cipher.doFinal(inBytes);
					
					return Base64.encodeBase64String(outBytes);
				}
				
			}

			/**
			 * 
			 * @param key
			 * @param inStr
			 * @return
			 * @throws Exception 
			 */
			private String decrypt(SecretKey key, String inStr) throws Exception {
				
				if (hashMethod != null) {
					String[] sa = HashUtils.decode(hashMethod, inStr);
					// sa[0] is the key, simply ignored
					return sa[1];
				}
				else {
					cipher.init(Cipher.DECRYPT_MODE, key);

					byte[] inBytes = Base64.decodeBase64(inStr);
					byte[] outBytes = cipher.doFinal(inBytes);

					return new String(outBytes, CHARSET);
				}
			}
		}


		/**
		 * The thread local worker
		 */
		private ThreadLocal<_Worker>		tlWorker;
		
		
		private String						cipherAlgo;
		private String						keyAlgo;
		private int							keyLen;
		
		private HashUtils.Method			hashMethod = null;
		
		
		public Executor(String cipherAlgo, String keyAlgo, int keyLen) {
			
			final String	_cipherAlgo;
			final String	_keyAlgo;
			final int		_keyLen;
			if (cipherAlgo == null) {
				_cipherAlgo = DEFAULT_CIPHER_ALGO;
				_keyAlgo = DEFAULT_KEY_ALGO;
			}
			else {
				_cipherAlgo = cipherAlgo;
				
				
				if (cipherAlgo.startsWith(HASH_ALGO_PREFIX)) {
					String methodStr = cipherAlgo.substring(HASH_ALGO_PREFIX.length());
					hashMethod = HashUtils.Method.valueOf(methodStr);
				}
				
				if (keyAlgo != null) {
					_keyAlgo = keyAlgo;
				}
				else {
					// pick the first part of the cipherAlgo
					int i = cipherAlgo.indexOf("/");
					if (i >= 0) {
						_keyAlgo = cipherAlgo.substring(0, i);
					}
					else {
						_keyAlgo = DEFAULT_KEY_ALGO;
					}
				}
			}

			if (keyLen > 0) {
				_keyLen = keyLen;
			}
			else {
				_keyLen = DEFAULT_KEY_LEN;
			}
			
			this.cipherAlgo = _cipherAlgo;
			this.keyAlgo = _keyAlgo;
			this.keyLen = _keyLen;
			
			
			// now initialize the thread local for worker
			tlWorker = new ThreadLocal<_Worker>() {
				@Override
				protected _Worker initialValue() {
					_Worker ret = null;
					try {
						ret = new _Worker();
					}
					catch (Exception e) {
						LogUtil.error(logger, e, "Unable to initialize crypto worker");
					}

					return ret;
				}
			};			
		}
		
		public String getId() {
			return buildExecutorId(cipherAlgo, keyAlgo, keyLen);
		}
		
		
		/**
		 * Key should only be used within a session
		 * @return
		 */
		public SecretKey generateKey() {
			_Worker worker = tlWorker.get();
			return worker.generateKey();
		}
		
		/**
		 * 
		 * @param keySpec
		 * @return 
		 */
		public SecretKey generateKey(String keySpec) {
			_Worker worker = tlWorker.get();
			return worker.generateKey(keySpec);
		}

	
		/**
		 * Encrypt the input string and convert to base64
		 * @param key
		 * @param inStr
		 * @return
		 * @throws Exception
		 */
		public String encrypt(SecretKey key, String inStr) throws Exception {
			if (inStr == null) {
				return null;
			}

			_Worker worker = tlWorker.get();

			return worker.encrypt(key, inStr);
		}
		

		/**
		 * Decrypt the input string
		 * @param key
		 * @param inStr
		 * @return
		 * @throws Exception
		 */
		public String decrypt(SecretKey key, String inStr) throws Exception {
			if (inStr == null) {
				return null;
			}

			_Worker worker = tlWorker.get();

			return worker.decrypt(key, inStr);
		}
		
		
		/**
		 * 
		 * @return
		 */
		public int nextRandomInt(boolean half) {
			return tlWorker.get().nextRandomInt(half);
		}


		/**
		 * 
		 * @param length
		 * @return
		 */
		public String nextRandomString(int length) {
			return tlWorker.get().nextRandomString(length);
		}		
		
		

		/**
		 * 
		 * @return
		 * @throws Exception
		 */
		public KeyData generateConvertedKey() throws Exception {
			KeyData keyData = new KeyData();

			keyData.object = generateKey();
			keyData.string = convertKeyToString(keyData.object);

			return keyData;
		}		
	}

	/**
	 * For easy access
	 */
	public static class KeyData {
		public SecretKey		object = null;
		public String			string = null;
	}


	private final static Executor					defaultExecutor;
	
	private final static Map<String, Executor>		executorMap;
	private static ReentrantLock					mapLock = new ReentrantLock();




	static {
		// create default executor
		defaultExecutor = new Executor(DEFAULT_CIPHER_ALGO, DEFAULT_KEY_ALGO, DEFAULT_KEY_LEN);
		
		executorMap = new HashMap<String, Executor>();
		executorMap.put(defaultExecutor.getId(), defaultExecutor);
		
	}
	
	
	/**
	 * 
	 * @return 
	 */
	public static Executor getDefaultExecutor() {
		return defaultExecutor;
	}
	
	
	/**
	 * Construct an executor if necessary
	 * @param cipherAlgo
	 * @param keyAlgo
	 * @param keyLen
	 * @return 
	 */
	public static Executor getExecutor(String cipherAlgo, String keyAlgo, int keyLen) {
		
		Executor executor = null;
		String exeId = buildExecutorId(cipherAlgo, keyAlgo, keyLen);
		
		mapLock.lock();
		try {
			executor = executorMap.get(exeId);
			if (executor == null) {
				executor = new Executor(cipherAlgo, keyAlgo, keyLen);
				executorMap.put(exeId, executor);
			}
		}
		finally {
			mapLock.unlock();
		}
		
		return executor;
	}


	/**
	 * 
	 * @param cipherAlgo
	 * @param keyAlgo
	 * @param keyLen
	 * @return 
	 */
	private static String buildExecutorId(String cipherAlgo, String keyAlgo, int keyLen) {
		return cipherAlgo + "##" + keyAlgo + "##" + keyLen;
	}
	
	/**
	 * Key should only be used within a session
	 * @return
	 */
	public static SecretKey generateKey() {
		return defaultExecutor.generateKey();
	}
	
	
	/**
	 * 
	 * @param keySpec
	 * @return 
	 */
	public static SecretKey generateKey(String keySpec) {
		return defaultExecutor.generateKey(keySpec);
	}


	/**
	 * 
	 * @return
	 * @throws Exception
	 */
	public static KeyData generateConvertedKey() throws Exception {
		return defaultExecutor.generateConvertedKey();
	}

	/**
	 * Use base64, for database storing and may be for exchange
	 * 
	 * @param key
	 * @return
	 * @throws Exception 
	 */
	public static String convertKeyToString(Object key) throws Exception {
		if (key == null) {
			return null;
		}

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(key);

		return Base64.encodeBase64String(baos.toByteArray());
	}

	/**
	 * Convert base64 key string to secret key object
	 * @param base64KeyStr
	 * @return
	 * @throws Exception
	 */
	public static SecretKey convertStringToKey(String base64KeyStr) throws Exception {
		if (base64KeyStr == null) {
			return null;
		}

		//ByteArrayInputStream bais = new ByteArrayInputStream(base64KeyStr.getBytes(CHARSET));
		ByteArrayInputStream bais = new ByteArrayInputStream(Base64.decodeBase64(base64KeyStr));
		ObjectInputStream ois = new ObjectInputStream(bais);

		return (SecretKey)ois.readObject();
	}


	/**
	 * 
	 * @param keyStr
	 * @return
	 * @throws Exception 
	 */
	public static KeyData createKeyData(String keyStr) throws Exception {
		KeyData keyData = new KeyData();
		keyData.string = keyStr;
		keyData.object = convertStringToKey(keyStr);

		return keyData;
	}


	/**
	 * Encrypt the input string and convert to base64
	 * @param key
	 * @param inStr
	 * @return
	 * @throws Exception
	 */
	public static String encrypt(SecretKey key, String inStr) throws Exception {
		return defaultExecutor.encrypt(key, inStr);
	}


	/**
	 * Decrypt the input string
	 * @param key
	 * @param inStr
	 * @return
	 * @throws Exception
	 */
	public static String decrypt(SecretKey key, String inStr) throws Exception {
		return defaultExecutor.decrypt(key, inStr);
	}


	/**
	 * 
	 * @return
	 */
	public static int nextRandomInt(boolean half) {
		return defaultExecutor.nextRandomInt(half);
	}


	/**
	 * 
	 * @param length
	 * @return
	 */
	public static String nextRandomString(int length) {
		return defaultExecutor.nextRandomString(length);
	}
}
