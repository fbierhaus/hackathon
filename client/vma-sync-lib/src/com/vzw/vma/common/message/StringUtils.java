package com.vzw.vma.common.message;

import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

//import org.apache.log4j.Logger;

public class StringUtils {
    //private static Logger logger = Logger.getLogger(StringUtils.class.getName());
    private static CharsetEncoder asciiEncoder = Charset.forName("US-ASCII").newEncoder();
    
    public static boolean isEmpty(String str) {
    	return str==null || "".equals(str.trim());
    }
    
    public static boolean isNotEmpty(String s) {
    	return !isEmpty(s);
    }
    
    public static boolean isNumber(String s) {
		try {
			Long.valueOf(s);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
    }
    
    public static String getShaHash(String content) {
        return getHash("SHA-1",content);
    }
    
    public static String getMd5Hash(String content) {
        return getHash("MD5",content);
    }
    
    public static String getHash(String alr, String content) {
        String hash = content;
        try {
            //logger.debug("getting " + alr + " hash for " + content);
            byte[] stuffs = content.getBytes();
            MessageDigest algorithm = MessageDigest.getInstance(alr);
            algorithm.reset();
            algorithm.update(stuffs);
            byte[] digest = algorithm.digest();
            StringBuffer hexString = new StringBuffer();
            for (int i=0;i<digest.length;i++) {
            	String hex = Integer.toHexString(0xFF & digest[i]);
            	if (hex.length() == 1) {
            		hex = "0" + hex;
            	}
            	hexString.append(hex);
            }
            hash = hexString.toString();
            //logger.debug("hash is " + hash);
        } catch (Exception e) {
           //logger.error("cannot get hash for content, return content itself");
        }
        return hash;
    }
    
    /**
     * Get random string up to 20 characters
     * 
     * @param length
     * @return
     */
    public static String getRandomString(int length) {
    	String randomPassword = null; 
		try {
			SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
		    random.setSeed(System.nanoTime());
		    byte[] bytes = new byte[20];
		    random.nextBytes(bytes);
            StringBuffer hexString = new StringBuffer();
            for (int i=0;i<bytes.length;i++) {
            	String hex = Integer.toHexString(0xFF & bytes[i]);
            	if (hex.length() == 1) {
            		hex = "0" + hex;
            	}
            	hexString.append(hex);
            }
            randomPassword = hexString.toString().substring(0,length>20?20:length);
		} catch (NoSuchAlgorithmException e) {
			//logger.error(e);
		}
		return randomPassword;
    }
    
    public static boolean isValidMDN(String mdn) {
    	if (isEmpty(mdn) || mdn.trim().length()!=10) {
    		return false;
    	}
    	try {
    		Long.valueOf(mdn.trim());
    	} catch (Exception e) {
			return false;
		}
    	return true;
    }
    
    public static String getHexString(byte b) {
    	return getHexString(new byte[] {b});
    }
    
    public static String getHexString(byte[] b) {
		char[] hexChar = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

		StringBuffer sb = new StringBuffer(b.length * 2);
		for (int i = 0; i < b.length; i++) {
			// look up high nibble char
			sb.append(hexChar[(b[i] & 0xf0) >>> 4]);

			// look up low nibble char
			sb.append(hexChar[b[i] & 0x0f]);
		}
		return sb.toString();

	}
    
	public static boolean hasTraversalString(String s) {
		if (s == null) {
			return false;
		}
		
		if (s.indexOf("/..") != -1 || 
				s.indexOf("../") != -1 || 
				s.equals("..")) {
			return true;
		}
		
		return false;
	}

    /**
     * Returns a list of byte array encoded in char set 
     * @param src			String to be converted
     * @param segmentLength	length of string to be segmented into byte array
     * @param charset		char set to be encoded in
     * @return
     */
    public static ArrayList<byte[]> getEmsByteArray(String src, int segmentLength, String charset) {
    	if(src == null || src.length()==0 || segmentLength <=0) 
    		return null;
    	ArrayList<byte[]> byteArray = new ArrayList<byte[]>();
    	try {
	    	if(src.length()<segmentLength) {
	    		byte[] abcByte = src.getBytes(charset);
	    		//logger.debug("Source:"+src+" charset:"+charset+" Bytes string:"+displayHexString(abcByte));
	    		byteArray.add(abcByte);
	    		return byteArray;
	    	} else {
	    		int startInd = 0;
	    		int endInd = 0;
	    		while(startInd <= src.length()) {
	    			endInd = startInd+segmentLength;
	    			if(endInd >= src.length()) {
	    				byte[] abcByte = src.substring(startInd, src.length()).getBytes(charset);
		    			byteArray.add(abcByte);
	    				break;
	    			} else {
	    				byte[] abcByte = src.substring(startInd, endInd).getBytes(charset);
		    			byteArray.add(abcByte);
		    			startInd+=segmentLength;
	    			}
	    		}
	    	}
    	} catch (Exception ex) {
    		//logger.error("Failed to convert to EMS byte array");
    		return null;
    	}
    	return byteArray;
    }
    
    public static ArrayList<byte[]> getEmsUCS2ByteArray(String src, int segmentLength) {
    	if(src == null || src.length()==0 || segmentLength <=0) 
    		return null;
    	ArrayList<byte[]> byteArray = new ArrayList<byte[]>();
    	try {
    		byte[] oByte = src.getBytes();
    		String msg8 = new String(oByte, "UTF-8");
    		byte[] oByte16 = msg8.getBytes("UTF-16BE");
    		//logger.debug("Source:"+src+" Bytes string:"+displayHexString(oByte16));

	    	int startInd = 0;
    		int endInd = 0;
    		while(startInd <= oByte16.length) {
    			endInd = startInd+segmentLength;
    			if(endInd >= oByte16.length) {
    				byte [] abcByte = new byte[oByte16.length-startInd];
    				System.arraycopy(oByte16, startInd, abcByte, 0, oByte16.length-startInd);
    				//logger.debug("Segment Bytes string:"+displayHexString(abcByte));
	    			byteArray.add(abcByte);
    				break;
    			} else {
    				byte [] abcByte = new byte[endInd-startInd];
    				System.arraycopy(oByte16, startInd, abcByte, 0, endInd-startInd);
    				//logger.debug("Segment Bytes string:"+displayHexString(abcByte));
	    			byteArray.add(abcByte);
	    			startInd+=segmentLength;
    			}
    		}
    	} catch (Exception ex) {
    		//logger.error("Failed to convert to EMS byte array");
    		return null;
    	}
    	return byteArray;
    }
    
	public static String displayHexString(byte[] b) {
		String result = "";
		for (int i=0; i < b.length; i++) {
			result += Integer.toString( ( b[i] & 0xff ) + 0x100, 16).substring( 1 );
		}
		return result;
	}

	/**
	 * NCR decoder
	 * @param str
	 * @param unknownCh
	 * @return
	 */
	public static String decode(String str, char unknownCh) {
      StringBuffer sb = new StringBuffer();
      int i1=0;
      int i2=0;

      while(i2<str.length()) {
         i1 = str.indexOf("&#",i2);
         if (i1 == -1 ) {
              sb.append(str.substring(i2));
              break ;
         }
         sb.append(str.substring(i2, i1));
         i2 = str.indexOf(";", i1);
         if (i2 == -1 ) {
              sb.append(str.substring(i1));
              break ;
         }

         String tok = str.substring(i1+2, i2);
          try {
               int radix = 10 ;
               if (tok.charAt(0) == 'x' || tok.charAt(0) == 'X') {
                  radix = 16 ;
                  tok = tok.substring(1);
               }
               sb.append((char) Integer.parseInt(tok, radix));
          } catch (NumberFormatException exp) {
               sb.append(unknownCh);
          }
          i2++ ;
      }
      return sb.toString();
	}
	
	public static boolean isAscii(String v) {
	    return asciiEncoder.canEncode(v);
	}

	/**
	  * Validate the form of an email address.
	  *
	  * <P>Return <tt>true</tt> only if 
	  *<ul> 
	  * <li> <tt>aEmailAddress</tt> can successfully construct an 
	  * {@link javax.mail.internet.InternetAddress} 
	  * <li> when parsed with "@" as delimiter, <tt>aEmailAddress</tt> contains 
	  * two tokens which satisfy {@link hirondelle.web4j.util.Util#textHasContent}.
	  *</ul>
	  *
	  *<P> The second condition arises since local email addresses, simply of the form
	  * "<tt>albert</tt>", for example, are valid for 
	  * {@link javax.mail.internet.InternetAddress}, but almost always undesired.
	  */
	public static boolean isValidEmailAddress(String aEmailAddress) {
		if (aEmailAddress == null) return false;
		boolean result = true;
	    try {
	    	InternetAddress emailAddr = new InternetAddress(aEmailAddress);
	    	if ( ! hasNameAndDomain(aEmailAddress) ) {
	    		result = false;
	    	}
	    }
	    catch (AddressException ex){
	    	result = false;
	    }
	    return result;
	}

	private static boolean hasNameAndDomain(String aEmailAddress) {
	    String[] tokens = aEmailAddress.split("@");
	    return 
	    	tokens.length == 2 &&
	    	textHasContent( tokens[0] ) && 
	    	textHasContent( tokens[1] ) ;
	}

	private static boolean textHasContent(String aText) {
		return (aText != null) && (aText.trim().length() > 0);
	}


	
	/**
	 * NCR encoder
	 * @param str
	 * @return
	 */
	public static String encode( String str ) {
		char[] ch = str.toCharArray();
		StringBuffer sb = new StringBuffer();
		for ( int i = 0 ; i < ch.length ; i++ ) {
			if ( ch[i] < 0x20 || ch[i] > 0x7f )
				sb.append("&#").append((int) ch[i]).append(";");
			else
				sb.append(ch[i]);
		}
		return sb.toString();
	}
	
	
	public static int countOccurrences(String str, char cha) {
		if (str==null) {
			return 0;
		}
		int count = 0;
		for (int i = 0; i < str.length(); i++) {
			if (str.charAt(i) == cha) {
				count++;
			}
		}
		return count;
	}
	
    public static void main(String[] args) {
        //System.out.println(getShaHash("teststring"));
        //System.out.println(getMd5Hash("teststring"));
        //System.out.println(getMsaPassword("9255551234"));
        
    }
    

}
