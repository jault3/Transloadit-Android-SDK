package com.aultcoding.transloaditsdk;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.json.JSONObject;

import android.net.Uri;
import android.util.Base64;
import android.util.Log;

public class TransloaditRequest {//extends ASIFormDataRequest {
	
	private String secret;
	private JSONObject params;
	private JSONObject response;
	private int uploads;
	private boolean backgroundTasks;
	private boolean readyToStart;
	
	public TransloaditRequest() {
		//super("http://api2.transloadit.com/assemblies?pretty=true");
		params = new JSONObject();
	}

	public TransloaditRequest initWithCredentials(String key, String secretKey) {
		TransloaditRequest instance = new TransloaditRequest();
		
		secret = secretKey;

		JSONObject auth = new JSONObject();
		auth.put("key", key);
		params.put("auth", auth);

		return instance;
	}
	
	public void addRawFile(String path, String filename, String contentType) {
		uploads++;
	    String field = "upload_"+uploads;
	    [self setFile:path withFileName:filename andContentType:type forKey:field];
	}
	
	public void addRawData(File file, String filename, String contentType) {
		uploads++;
	    String field = "upload_"+uploads;
	    [self setData:data withFileName:filename andContentType:type forKey:field];
	}
	
	public void addPickedFile(Map<String, Object> info) {
		uploads++;
		String field = "upload_"+uploads;
		[self addPickedFile:info forField:field];
	}
	
	public void addPickedFile(Map<String, Object> info, String field) {
		String mediaType = (String)info.get(UIImagePickerControllerMediaType);

		if (mediaType.equals("public.image")) {
			backgroundTasks++;
			final Map<String, Object> file = new HashMap<String, Object>();
			file.put("info", info);
			file.put("field", field);
			new Thread(new Runnable() {
				@Override
				public void run() {
					saveImageToDisk(file);
				}
			}).start();
		} else if (mediaType.equals("public.movie")) {
			File file = new File(info.get(UIImagePickerControllerMediaURL));
			String filePath = fileUri.getPath();
			[self setFile:filePath withFileName:"android_video.mov" andContentType:"video/quicktime" forKey:field];
		}
	}
	
	public void setTemplateId(String templateId) {
		params.put("template_id", templateId);
	}
	
	public boolean hadError() {
		boolean retval = false;
		if (response.has("error")) {
			retval = true;
		}
		return retval;
	}
	
	public void startAsynchronous {
		readyToStart = true;
		if (backgroundTasks) {
			return;
		}

		SimpleDateFormat format = new SimpleDateFormat();
		format.applyPattern("yyyy-MM-dd HH:mm-ss 'GMT'");

		Date localExpires = new Date(System.currentTimeMillis()+3600000);
		
		NSTimeInterval timeZoneOffset = [[NSTimeZone defaultTimeZone] secondsFromGMT];
		NSTimeInterval gmtTimeInterval = [localExpires timeIntervalSinceReferenceDate] - timeZoneOffset;
		Date gmtExpires = [NSDate dateWithTimeIntervalSinceReferenceDate:gmtTimeInterval];

		params.getJSONObject("auth").put("expires", format.stringFromDate(gmtExpires));
		((Map<String,Object>)params.get("auth")).put("expires", format.stringFromDate(gmtExpires));
		
		String paramsField = params.toString();
		String signatureField = stringWithHexBytes(hmacSha1withKey(secret, paramsField));
		
		setPostValue("params", paramsField);
		setPostValue("signature", signatureField);
	    
		super.startAsynchronous();
	}
	
	public void requestFinished {
		response = new JSONObject(responseString);
	    
		super.requestFinished();
	}

	public void saveImageToDisk(Map<String, Object> file) {
		String *tmpFile = [NSTemporaryDirectory() stringByAppendingPathComponent:[@"transloadfile" stringByAppendingString:[[NSProcessInfo processInfo] globallyUniqueString]]];	
		UIImage *image = [[file objectForKey:@"info"] objectForKey:@"UIImagePickerControllerOriginalImage"];
		[UIImageJPEGRepresentation(image, 0.9f) writeToFile:tmpFile atomically:YES];
		[file setObject:tmpFile forKey:@"path"];
		[self performSelectorOnMainThread:@selector(addImageFromDisk:) withObject:file waitUntilDone:NO];
	}

	public void addImageFromDisk(Map<String, Object> file)
	{
		[self setFile:[file objectForKey:@"path"] withFileName:@"iphone_image.jpg" andContentType: @"image/jpeg" forKey:[file objectForKey:@"field"]];
		backgroundTasks--;
		if (readyToStart) {
			startAsynchronous();
		}
	}

	// from: http://stackoverflow.com/questions/7047487/android-hmac-sha1-different-than-standard-java-hmac-sha1
	public static byte[] hmacSha1withKey(String key, String string) {
		byte[] enc;
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            SecretKeySpec secret = new SecretKeySpec(key.getBytes("UTF-8"), mac.getAlgorithm());
            mac.init(secret);
            byte[] digest = mac.doFinal(string.getBytes());

            // Base 64 Encode the results
            enc = Base64.encode(digest, Base64.DEFAULT);
            Log.v("Transloadit", "String: " + string);
            Log.v("Transloadit", "key: " + key);
            Log.v("Transloadit", "result: " + enc);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        
        return enc;
	}

	public static String bytesToHex(byte[] bytes) {
	    final char[] hexArray = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
	    char[] hexChars = new char[bytes.length * 2];
	    int v;
	    for ( int j = 0; j < bytes.length; j++ ) {
	        v = bytes[j] & 0xFF;
	        hexChars[j * 2] = hexArray[v >>> 4];
	        hexChars[j * 2 + 1] = hexArray[v & 0x0F];
	    }
	    return new String(hexChars);
	}
}
