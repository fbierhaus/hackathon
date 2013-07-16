/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.verizon.mms.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.text.TextUtils;

import com.strumsoft.android.commons.logger.Logger;

/**
 *
 * @author hud
 * 
 * 
 * 
 */
public class RecipientAddressUtil {
	private static final Pattern				NORTH_AMERICAN_REGEX_PATTERN = Pattern.compile("((1|(\\+1)|(0111))\\s*)?(([0-9]{10})|(\\(\\s*[0-9]{3}\\s*\\)\\s*[0-9]{3}\\s*\\-\\s*[0-9]{4})|([0-9]{3}\\s*\\-\\s*[0-9]{3}\\s*\\-\\s*[0-9]{4})|([0-9]{7})|([0-9]{3}\\s*\\-\\s*[0-9]{4}))");
	private static final Pattern				LONG_CODE_REGEX_PATTERN = Pattern.compile("([0-9]{3,6}|[0-9]{12})");
	private static final Pattern				INTERNATIONAL_REGEX_PATTERN = Pattern.compile("((011)|\\+)([02-9][0-9]+)");
	private static final Pattern				EMAIL_FULL_REGEX_PATTERN = Pattern.compile("(?i)([a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*)@((?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?)");
	//private static final Pattern				EMAIL_PREFIX_REGEX_PATTERN = Pattern.compile("(?i)[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*");
	
	private static final Pattern				NON_DIGIT_REGEX_PATTERN = Pattern.compile("\\D");
	
	
	
	public static enum AddressType {
		NORTH_AMERICAN,
		LONG_CODE,
		INTERNATIONAL,
		EMAIL,
		GATEWAYEMAIL,
		INVALID
	}
	
	public static enum Flag {
		MDN_ONLY,
		EMAIL_ONLY,
		ALL
	}
	
	public static class RecipientAddress {
		private AddressType			type = AddressType.INVALID;
		private String				normalized = null;		// normalized value of mdn, if invalid, the input is stored here
		
		private String				formatted = null;		// formatted value of mdn (formatted)
		
		// only for email
		private int					emailPrefixLength = 0;
		private int					emailDomainLength = 0;

		public boolean isValid() {
			return type != AddressType.INVALID;
		}
		
		public AddressType getType() {
			return type;
		}

		public void setType(AddressType type) {
			this.type = type;
		}

		public String getNormalized() {
			return normalized;
		}

		public void setNormalized(String normalized) {
			this.normalized = normalized;
		}

		public String getFormatted() {
			return formatted;
		}

		public void setFormatted(String formatted) {
			this.formatted = formatted;
		}

		public int getEmailPrefixLength() {
			return emailPrefixLength;
		}

		public void setEmailPrefixLength(int emailPrefixLength) {
			this.emailPrefixLength = emailPrefixLength;
		}

		public int getEmailDomainLength() {
			return emailDomainLength;
		}

		public void setEmailDomainLength(int emailDomainLength) {
			this.emailDomainLength = emailDomainLength;
		}

		@Override
		public String toString() {
			String ret;
			switch (type) {
				case INVALID:
					ret = String.format("RecipientAddress[%s: input=%s]", type, normalized);
					break;
					
				case NORTH_AMERICAN:
				case INTERNATIONAL:
				case LONG_CODE:
					ret = String.format("RecipientAddress[%s: normalized=%s,formatted=%s]", type, normalized, formatted);
					break;
					
				default:
				case EMAIL:
				case GATEWAYEMAIL:
					ret = String.format("RecipientAddress[%s: normalized=%s,formatted=%s,emailPrefixLength=%d,emailDomainLength=%d]", 
							type, normalized, formatted, emailPrefixLength, emailDomainLength);
					break;
			}
			
			return ret;
		}
		
		
		
		
	}
	
	
	
	private static RecipientAddressUtil						instance = new RecipientAddressUtil();
	
	
	
	public static RecipientAddressUtil getInstance() {
		return instance;
	}
	
	/**
	 * The input can be anything (formatted or unformatted)
	 * 
	 * @param input
	 * @param flag
	 * @param loose
	 *		only used for mdn parsing, if true, the mdn will be pre-processed by removing all non-digit characters except leading "+"
	 * @param areaCode
	 * @return 
	 */
	public RecipientAddress parse(String input, Flag flag, boolean loose, String areaCode) {
		RecipientAddress res = null;
		
		switch (flag) {
			case MDN_ONLY:
				res = parseMdn(input, loose, areaCode);
				break;
				
				
				
			case EMAIL_ONLY:
				res = parseEmail(input);
				break;
				
				
			case ALL:
				// added .com check to handle case when we get SMS from 6245 it comes as Unverified-Vtext.com-Sender
				if(input != null && (input.contains("@") || input.contains(".com"))) {
					res = parseEmail(input);
				} else {
					res = parseMdn(input, loose, areaCode);
					if (res.getType() == AddressType.INVALID) {
						res = parseEmail(input);
					}
				}
				break;
		}
		
		return res;
	}
	
	
	/**
	 * 
	 * @param input
	 * @return 
	 */
	public RecipientAddress parseEmail(String input) {
		RecipientAddress res = new RecipientAddress();
		
		if (!TextUtils.isEmpty(input)) {
			String _input = input.trim();
			
			Matcher m = EMAIL_FULL_REGEX_PATTERN.matcher(_input);
			
			if (m.matches()) {
				res.setType(AddressType.EMAIL);
				res.setEmailPrefixLength(m.group(1).length());
				res.setEmailDomainLength(m.group(2).length());
				res.setNormalized(_input.toLowerCase());
				res.setFormatted(formatEmail(res.getNormalized()));
			}
			else {
				// still move to lower case
				res.setType(AddressType.GATEWAYEMAIL);
				res.setNormalized(_input.toLowerCase());
				res.setFormatted(formatEmail(res.getNormalized()));
				if (Logger.IS_DEBUG_ENABLED) {
					Logger.debug("Failed to parse email: " + _input);
				}
			}
			
		}
		
		return res;
	}
	
	
	/**
	 * 
	 * @param input
	 * @param loose
	 *		if true, the input will be pre-processed by removing all non-digit characters
	 * @param areaCode
	 *		optional, if provided and the north America number is 7-digit, append it in front
	 *		otherwise, leave it as s
	 * @return 
	 */
	public RecipientAddress parseMdn(String input, boolean loose, String areaCode) {
		RecipientAddress res = new RecipientAddress();
		
		if (!TextUtils.isEmpty(input)) {
			String _input1 = input.trim();
			
			// for safety purpose, let's get rid of any non-digit number except the
			// first "+" sign
			String _input2;
			if (loose) {
				_input2 = toDigits(_input1);
				if (_input1.startsWith("+")) {
					_input2 = "+" + _input2;
				}
			}
			else {
				_input2 = _input1;
			}
			
			
			
			String s1;
			
			// international 
			
			Matcher m = INTERNATIONAL_REGEX_PATTERN.matcher(_input2);
			if (m.matches()) {
				res.setType(AddressType.INTERNATIONAL);
				s1 = toDigits(m.group(3));	// non-contry code part
				res.setNormalized("011" + s1);	// normalized is always a number
				
				// format +xxxxxx
				res.setFormatted(formatInternational(res.getNormalized()));
			}
			else {
				// north america
				m = NORTH_AMERICAN_REGEX_PATTERN.matcher(_input2);
				if (m.matches()) {
					res.setType(AddressType.NORTH_AMERICAN);
					
					// check full number
					s1 = toDigits(m.group(5));
					if (s1.length() == 10) {
						res.setNormalized(s1);
						res.setFormatted(formatFullNorthAmerican(s1));
					}
					else if (s1.length() == 7) {
						if (areaCode != null) {
							s1 = areaCode + s1;
							res.setNormalized(s1);
							res.setFormatted(formatFullNorthAmerican(s1));
						}
						else {
							res.setNormalized(s1);
							res.setFormatted(format7DigitNorthAmerican(s1));
						}
					}
					else {
						// not possible
						res.setNormalized(_input2);
						Logger.debug("Failed to parse mdn as north American number (not possible to reach here): {0}", input);
					}
					
				}
				else {
					// long code
					m = LONG_CODE_REGEX_PATTERN.matcher(_input2);
					if (m.matches()) {
						res.setType(AddressType.LONG_CODE);
						res.setNormalized(_input2);
						res.setFormatted(_input2);	
						// we do not format long code
					}
					else {
						res.setNormalized(_input2);
						Logger.debug("Failed to parse mdn: {0}", input);
					}
				}
			}
		}
		
		
		return res;
	}
	
	/**
	 * 
	 * @param input
	 * @return 
	 */
	public String formatInternational(String input) {
		if (!TextUtils.isEmpty(input)) {
			
			String _input = input.trim();
			if (_input.startsWith("011") && _input.length() > 3) {
				return "+" + _input.substring(3);
			}
			else {
				return _input;
			}
		}		
		else {
			return input;
		}
	}
	
	/**
	 * 
	 * @param input
	 * @return 
	 */
	public String format7DigitNorthAmerican(String input) {
		if (!TextUtils.isEmpty(input)) {
			String _input = input.trim();
			if (_input.length() != 7) {
				return _input;
			}
			else {
				return new StringBuilder(_input.substring(0, 3))
							.append('-')
							.append(_input.substring(3))
							.toString();
			}
		}
		else {
			return input;
		}
	}

	/**
	 * 
	 * @param input
	 *		must be 10-digit
	 * @return 
	 */
	public String formatFullNorthAmerican(String input) {
		if (!TextUtils.isEmpty(input)) {
			String _input = input.trim();
			if (_input.length() != 10) {
				return _input;		// do nothing
			}
			else {
				return new StringBuilder("(")
							.append(_input.substring(0, 3))
							.append(')')
							.append(_input.substring(3, 6))
							.append('-')
							.append(_input.substring(6))
							.toString();
			}
		}
		else {
			return input;
		}
		
	}
	
	/**
	 * 
	 * @param input
	 * @return 
	 */
	public String formatEmail(String input) {
		if (!TextUtils.isEmpty(input)) {
			return input.trim().toLowerCase();
		}
		else {
			return input;
		}
	}
	
	/**
	 * 
	 * @param input
	 * @param type
	 * @return 
	 */
	public String format(String input, AddressType type) {
		if (TextUtils.isEmpty(input)) {
			return input;
		}
		
		switch (type) {
			case NORTH_AMERICAN:
				return formatFullNorthAmerican(input);
				
			case INTERNATIONAL:
				return formatInternational(input);
				
			case EMAIL:
				return formatEmail(input);
				
			case LONG_CODE:
			case INVALID:
			default:
				return input.trim();
		}
	}
	
	/**
	 * Simply replace occurrence of the control characters with empty
	 * @param input
	 * @return 
	 */
	public String toDigits(String input) {
		if (TextUtils.isEmpty(input)) {
			return "";
		}
		else {
			return NON_DIGIT_REGEX_PATTERN.matcher(input).replaceAll("");
		}
	}
	
}
