/* ************************************************************************
#
#  DivConq
#
#  http://divconq.com/
#
#  Copyright:
#    Copyright 2014 eTimeline, LLC. All rights reserved.
#
#  License:
#    See the license.txt file in the project's top-level directory for details.
#
#  Authors:
#    * Andy White
#
************************************************************************ */
package divconq.util;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import divconq.lang.chars.Utf8Encoder;
import divconq.lang.op.FuncResult;

public class HashUtil {
	public static FuncResult<String> hash(String method, InputStream in) {
		FuncResult<String> or = new FuncResult<>();
		
		try {
			if ("SHA512".equals(method))
				or.setResult(HashUtil.getSha512(in));		
			else if ("SHA256".equals(method))
				or.setResult(HashUtil.getSha256(in));
			else if ("SHA128".equals(method))
				or.setResult(HashUtil.getSha1(in));
			else if ("MD5".equals(method))
				or.setResult(HashUtil.getMd5(in));
			else
				or.error(1, "Method not supported!");
		}
		catch (Exception x) {
			or.error(1, "Hash errored: " + x);
		}
		
		return or;
	}
	
	/**
	 * Calculate an MD5 on a string, return a hex formated string of the MD5
	 * 
	 * @param str source to hash
	 * @return hex MD5 value
	 */
	public static String getMd5(String str) {
		try {
	        MessageDigest md = MessageDigest.getInstance("MD5");
	        md.update(Utf8Encoder.encode(str));
	        return HexUtil.bufferToHex(md.digest(), 0, md.digest().length);
		}
		catch (Exception x) {
		}
		
		return null;
    }
	
	/**
	 * Calculate an MD5 on a stream, return a hex formated string of the MD5
	 * 
	 * @param str source to hash
	 * @return hex MD5 value
	 */
	public static String getMd5(InputStream str) {
		try {
	        MessageDigest md = MessageDigest.getInstance("MD5");
	        
	        byte[] buffer = new byte[16 * 4096];		// TODO config?
	        
	        int cnt = str.read(buffer);
	        
	        while (cnt != -1) {
	        	md.update(buffer, 0, cnt);
	        	cnt = str.read(buffer);
	        }
	        
	        byte[] rv = md.digest();
	        return HexUtil.bufferToHex(rv, 0, rv.length);
		}
		catch (Exception x) {
		}
		finally {
			try {
				str.close();
			} 
			catch (IOException x) {
			}
		}
		
		return null;
    }
	
	/**
	 * Calculate an SHA-512 on a string, return a hex formated string of the SHA
	 * 
	 * @param str source to hash
	 * @return hex SHA value
	 */
	public static String getSha1(String str) {
		try {
	        MessageDigest md = MessageDigest.getInstance("SHA-1");
	        md.update(Utf8Encoder.encode(str));
	        byte[] rv = md.digest();
	        return HexUtil.bufferToHex(rv, 0, rv.length);
		}
		catch (Exception x) {
		}
		
		return null;
    }
	
	/**
	 * Calculate an SHA-512 on a stream, return a hex formated string of the SHA
	 * 
	 * @param str source to hash
	 * @return hex SHA value
	 */
	public static String getSha1(InputStream str) {
		try {
	        MessageDigest md = MessageDigest.getInstance("SHA-1");
	        
	        byte[] buffer = new byte[16 * 4096];		// TODO config?
	        
	        int cnt = str.read(buffer);
	        
	        while (cnt != -1) {
	        	md.update(buffer, 0, cnt);
	        	cnt = str.read(buffer);
	        }
	        
	        byte[] rv = md.digest();
	        return HexUtil.bufferToHex(rv, 0, rv.length);
		}
		catch (Exception x) {
		}
		finally {
			try {
				str.close();
			} 
			catch (IOException x) {
			}
		}
		
		return null;
    }
	
	/**
	 * Calculate an SHA-512 on a string, return a hex formated string of the SHA
	 * 
	 * @param str source to hash
	 * @return hex SHA value
	 */
	public static String getSha512(String str) {
		try {
	        MessageDigest md = MessageDigest.getInstance("SHA-512");
	        md.update(Utf8Encoder.encode(str));
	        byte[] rv = md.digest();
	        return HexUtil.bufferToHex(rv, 0, rv.length);
		}
		catch (Exception x) {
		}
		
		return null;
    }
	
	/**
	 * Calculate an SHA-512 on a stream, return a hex formated string of the SHA
	 * 
	 * @param str source to hash
	 * @return hex SHA value
	 */
	public static String getSha512(InputStream str) {
		try {
	        MessageDigest md = MessageDigest.getInstance("SHA-512");
	        
	        byte[] buffer = new byte[16 * 4096];		// TODO config?
	        
	        int cnt = str.read(buffer);
	        
	        while (cnt != -1) {
	        	md.update(buffer, 0, cnt);
	        	cnt = str.read(buffer);
	        }
	        
	        byte[] rv = md.digest();
	        return HexUtil.bufferToHex(rv, 0, rv.length);
		}
		catch (Exception x) {
		}
		finally {
			try {
				str.close();
			} 
			catch (IOException x) {
			}
		}
		
		return null;
    }
	
	/**
	 * Calculate an SHA-256 on a string, return a hex formated string of the SHA
	 * 
	 * @param str source to hash
	 * @return hex SHA value
	 */
	public static String getSha256(String str) {
		try {
	        MessageDigest md = MessageDigest.getInstance("SHA-256");
	        md.update(Utf8Encoder.encode(str));
	        byte[] rv = md.digest();
	        return HexUtil.bufferToHex(rv, 0, rv.length);
		}
		catch (Exception x) {
		}
		
		return null;
    }
	
	/**
	 * Calculate an SHA-256 on a stream, return a hex formated string of the SHA
	 * 
	 * @param str source to hash
	 * @return hex SHA value
	 */
	public static String getSha256(InputStream str) {
		try {
	        MessageDigest md = MessageDigest.getInstance("SHA-256");
	        
	        byte[] buffer = new byte[16 * 4096];		// TODO config?
	        
	        int cnt = str.read(buffer);
	        
	        while (cnt != -1) {
	        	md.update(buffer, 0, cnt);
	        	cnt = str.read(buffer);
	        }
	        
	        byte[] rv = md.digest();
	        return HexUtil.bufferToHex(rv, 0, rv.length);
		}
		catch (Exception x) {
		}
		finally {
			try {
				str.close();
			} 
			catch (IOException x) {
			}
		}
		
		return null;
    }
	
	/**
	 * Calculate an SHA-512 on a string, return a hex formated string of the SHA.
	 * This is a keyed hash (HMAC), provide a key.  Any size will work, however,
	 * SHA-512 uses a 1024 bit block size so 1024 is the largest size useful,
	 * anything larger will be truncated.  Anything smaller will be zero padded
	 * which means it is less unique.
	 * 
	 * @param str source to hash
	 * @param key for hashing algorithm  
	 * @return hex SHA value
	 */
	public static String getMacSha512(String str, byte[] key) {
		try {
			SecretKeySpec skey = new SecretKeySpec(key, "hmacSHA512");
			Mac mac = Mac.getInstance("hmacSHA512");
			mac.init(skey);
			
			byte[] rv = mac.doFinal(Utf8Encoder.encode(str));
	        return HexUtil.bufferToHex(rv, 0, rv.length);
		} 
		catch (Exception x) {
		}	
		
		return null;
	}
}
