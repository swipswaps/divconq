﻿/* ************************************************************************
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

dc.user = {
	/**
	 * Tracks user info for the current logged in user.
	 * See "Info" property for collected user data.  
	 * Here is the structure for that data.
	 *
	 *	{
	 *		Credentials: object,		// only if RememberMe - possible security hole use with care
	 *		UserId: string,
	 *		Username: string,
	 *		FirstName: string,
	 *		LastName: string,
	 *		Email: string,
	 *		RememberMe: boolean,			
	 *		DomainId: string,
	 *		SessionId: string,			// note this does not have session key which is why session hijacking cannot work
	 *		Locale: string,
	 *		Chronology: string,
	 *		Verified: boolean,			// logged in
	 *		AuthTags: array of string
	 *	}
	 *
	 * @type object
	 */
	_info: { },
	_signinhandler: null,
	
	getRememberedPhrase : function() {
		return "152c8abccbbf880db5cd5c5a9487029d40c43c10265f1248bf170ee181bef52f";
	},
	
	// fairly unsafe, use RemberMe only on trusted computer
	loadRememberedUser : function() {
		dc.user._info = { };
			
		try {
			var encrypted = localStorage.getItem("adinfo.remeber");
			
			if (encrypted) {
				var plain = dc.util.Crypto.decrypt(encrypted, dc.user.getRememberedPhrase());
				dc.user._info = JSON.parse(plain);
				
				delete dc.user._info.Verified;
		
				return true;
			}
		}
		catch (x) {
		}
		
		return false;
	},

	/**
	 *  If RememberMe is true then store the current user info.  If not, make sure it is not present on disk and that Credentials are not in memory (much safer approach).
	 */
	saveRememberedUser : function() {
		try {
			if (!dc.user._info || !dc.user._info.RememberMe) {
				if (dc.user._info)
					delete dc.user._info.Credentials;
			
				localStorage.removeItem("adinfo.remeber");
				return;
			}
		
			var plain = JSON.stringify( dc.user._info );
			var crypted = dc.util.Crypto.encrypt(plain, dc.user.getRememberedPhrase());
			localStorage.setItem("adinfo.remeber", crypted);
		}
		catch (x) {
		}
	},
	
	/**
	 *  User is signed in
	 */
	isVerified : function() {
		return (dc.user._info.Verified === true);
	},
	
	isAuthorized: function(tags) {
		if (!tags)
			return true;
			
		if (!dc.user._info.AuthTags)
			return false;
			
		var ret = false;

		$.each(tags, function(i1, itag) {
			$.each(dc.user._info.AuthTags, function(i2, htag) {
				if (itag === htag)
					ret = true;
			});
		});
		
		return ret;
	},
	
	getUserInfo : function() {
		return dc.user._info;
	},
	
	setSignInHandler : function(v) {
		dc.user._signinhandler = v;
	},
	
	signin : function(uname, pass, remember, callback) {
		dc.user.signin2(
			{
				Username: uname,
				Password: pass
			}, 
			remember, 
			callback
		);
	},
	
	/**
	 * Given the current user info, try to sign in.  Trigger the callback whether sign in works or fails.
	 */
	signin2 : function(creds, remember, callback) {	
		/* TODO
		if (window.location.protocol != "https:") {
			// TODO turn this into a handler event 
			console.log('Connection is not secure, unable to sign in!');
			
			if (callback)
				callback();
			
			return;
		}
			*/
		
		dc.user._info = { };

		// we take what ever Credentials are supplied, so custom Credentials may be used		
		var msg = {
			Service: 'Session',
			Feature: 'Control',
			Op: 'Start',
			Credentials: creds
		};
		
		dc.comm.sendMessage(msg, function(rmsg) { 
			if (rmsg.Result == 0) {
				var resdata = rmsg.Body;
				
				if (resdata.Verified) {
					// copy only select fields for security reasons
					var uinfo = {
						Verified: ("00000_000000000000002" != resdata.UserId),	// guest is not treated as verified in client
						UserId: resdata.UserId,
						Username: resdata.Username,
						FullName: resdata.FullName,
						Email: resdata.Email,
						AuthTags: resdata.AuthTags,
						DomainId: resdata.DomainId,
						SessionId: resdata.SessionId,
						Locale: resdata.Locale,
						Chronology: resdata.Chronology
					}
					
					if (remember) {
						uinfo.Credentials = creds;		
						uinfo.RememberMe = remember;
					}
					
					dc.user._info = uinfo;
 
					// failed login will not wipe out remembered user (could be a server issue or timeout),
					// only set on success - successful logins will save or wipe out depending on Remember
					dc.user.saveRememberedUser();
					
					if (dc.user._signinhandler)
						dc.user._signinhandler.call(dc.user._info);
				}
			}
			
			if (callback)
				callback();
		});
	},

	signinFacebook: function(page, callback) {
		if (dc.user.isVerified()) {
			callback();
			return;
		}
	
		var fbsignin = function(auth) {
			dc.comm.sendMessage({ 
					Service: 'dcAuth',
					Feature: 'Authentication',
					Op: 'SignInFacebook',
					Body: {
						AccessToken: auth.accessToken
						//UserId: auth.userID
					}
				}, 
				function(rmsg) {
					if (rmsg.Result > 0) 
						callback();
					else 
						dc.user.updateUser(false, callback);
				});
		};
		
		var lstatus = function(response) {
			if (response.status === 'connected') {
				fbsignin(response.authResponse);
			}
			else if (page) {
				window.location = 'https://www.facebook.com/dialog/oauth?state=signin&client_id='
					+ dc.handler.settings.fbAppId + '&response_type=token&scope=public_profile,email&redirect_uri='
					+ window.location.origin + page
			}
			else {
				FB.login(function(response2) {
						if (response2.status === 'connected') 
							fbsignin(response2.authResponse);
						else
							callback();
					}, 
					{ scope: 'public_profile,email' }
				);	
			}
		};
	
		dc.util.SocialMedia.withFacebook(function() {
			FB.getLoginStatus(lstatus);			
		});
	},

	signinFacebookToken: function(accessToken, callback) {
		if (dc.user.isVerified()) {
			callback();
			return;
		}
		
		dc.comm.sendMessage({ 
				Service: 'dcAuth',
				Feature: 'Authentication',
				Op: 'SignInFacebook',
				Body: {
					AccessToken: accessToken
				}
			}, 
			function(rmsg) {
				if (rmsg.Result > 0) 
					callback();
				else 
					dc.user.updateUser(false, callback);
			});
	},

	linkFacebook: function(page, callback) {
		if (!dc.user.isVerified()) {
			callback(false);
			return;
		}
	
		var fblink = function(auth) {
			dc.comm.sendMessage({ 
					Service: 'dcAuth',
					Feature: 'Facebook',
					Op: 'LinkAccount',
					Body: {
						AccessToken: auth.accessToken
						//UserId: auth.userID
					}
				}, 
				function(rmsg) {
					callback(rmsg.Result == 0);
				});
		};
		
		var lstatus = function(response) {
			if (response.status === 'connected') {
				fblink(response.authResponse);
			}
			else if (page) {
				window.location = 'https://www.facebook.com/dialog/oauth?state=link&client_id='
					+ dc.handler.settings.fbAppId + '&response_type=token&scope=public_profile,email&redirect_uri='
					+ window.location.origin + page
			}
			else {
				FB.login(function(response2) {
						if (response2.status === 'connected') 
							fblink(response2.authResponse);
						else
							callback(false);
					}, 
					{ scope: 'public_profile,email' }
				);	
			}
		};
		
		dc.util.SocialMedia.withFacebook(function() {
			FB.getLoginStatus(lstatus);			
		});
	},
	
	updateUser : function(remember, callback, reload) {		
		dc.user._info = { };

		var msg = {
			Service: 'Session',
			Feature: 'Control',
			Op: reload ? 'ReloadUser' : 'LoadUser'
		};
		
		dc.comm.sendMessage(msg, function(rmsg) { 
			if (rmsg.Result == 0) {
				var resdata = rmsg.Body;
				
				if (resdata.Verified) {
					// copy only select fields for security reasons
					var uinfo = {
						Verified: ("00000_000000000000002" != resdata.UserId),	// guest is not treated as verified in client
						UserId: resdata.UserId,
						Username: resdata.Username,
						FullName: resdata.FullName,
						Email: resdata.Email,
						AuthTags: resdata.AuthTags,
						DomainId: resdata.DomainId,
						SessionId: resdata.SessionId,
						Locale: resdata.Locale,
						Chronology: resdata.Chronology
					}
					
					if (remember) {
						uinfo.RememberMe = remember;
					}
					
					dc.user._info = uinfo;
 
					// failed login will not wipe out remembered user (could be a server issue or timeout),
					// only set on success - successful logins will save or wipe out depending on Remember
					dc.user.saveRememberedUser();
					
					if (dc.user._signinhandler)
						dc.user._signinhandler.call(dc.user._info);
				}
			}
			
			if (callback)
				callback();
		});
	},
	
	/**
	 *  Sign out the current user, kill session on server
	 */
	signout : function() {
		dc.user._info = { };

		// TODO really should remove the remembered user too
		//localStorage.removeItem("adinfo.remeber");
		
		dc.comm.sendMessage({ 
			Service: 'Session',
			Feature: 'Control',
			Op: 'Stop'
		}, function() {
			//dc.comm.close();
		},
		1000);			
	}
	
}