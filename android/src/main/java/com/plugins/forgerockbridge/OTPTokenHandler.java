package com.plugins.forgerockbridge;

import android.content.Context;
import android.util.Log;


import com.getcapacitor.JSObject;
import com.getcapacitor.PluginCall;

import org.forgerock.android.auth.Account;
import org.forgerock.android.auth.FRAClient;
import org.forgerock.android.auth.FRListener;
import org.forgerock.android.auth.FRSession;
import org.forgerock.android.auth.FRUser;
import org.forgerock.android.auth.Mechanism;
import org.forgerock.android.auth.NodeListener;
import org.forgerock.android.auth.OathMechanism;
import org.forgerock.android.auth.OathTokenCode;
import org.forgerock.android.auth.UserInfo;
import org.forgerock.android.auth.exception.AuthenticatorException;
import org.json.JSONObject;

import com.plugins.forgerockbridge.ErrorHandler.FRException;

import java.util.List;
import java.util.Objects;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class OTPTokenHandler {
    private static final String TAG = "ForgeRockBridge";

  public static void startOtpJourney(PluginCall call, Context context, NodeListener<FRSession> listener ) {
        String journey = call.getString("journey");

        if (journey == null) {
            Log.e(TAG, "[OTPTokenHandler]: Error in get Journey");
          ErrorHandler.reject(call, ErrorHandler.ErrorCode.MISSING_JOURNEY);
            return;
        }
        Log.d(TAG, "[OTPTokenHandler]: Esin");
        FRSession.authenticate(context, journey, listener);

    }

  public static void hasRegisteredMechanism(PluginCall call, Context context) {
        try {
            Log.d(TAG, "[OTPTokenHandler]: Sending existence of mechanism true or false");
            JSObject response = validateExistMechanism(context);
            boolean emptyMechanism = response.getBool("empty");

            JSObject result = new JSObject();

            result.put("empty", emptyMechanism);

            call.resolve(result);
        } catch (Exception e) {
          Log.e(TAG, "[OTPTokenHandler]: RETURN FRE026 ERROR from onException");
          ErrorHandler.reject(call, ErrorHandler.ErrorCode.NO_ACCOUNTS_REGISTERED);
        }
  }

  public static void validateExistenceOTP(PluginCall call, Context context) {
        FRUser user = FRUser.getCurrentUser();
        if (user == null) {
            Log.e(TAG, "[OTPTokenHandler]: FRUser is null");
            ErrorHandler.reject(call, ErrorHandler.ErrorCode.GETTING_USER_INFO);
        }

        user.getUserInfo(new FRListener<UserInfo>() {
            @Override
            public void onSuccess(UserInfo userInfo) {
                String uuid = userInfo.getSub();
                checkServerAndDeviceOtpState(call, context, uuid);

            }

            @Override
            public void onException(Exception e) {
                Log.e(TAG, "[OTPTokenHandler]: Error getting userInfo", e);
                ErrorHandler.reject(call, ErrorHandler.ErrorCode.GETTING_USER_INFO);
            }
        });
  }

  public static void generateOTP(PluginCall call, Context context) {
        try {
            Log.d(TAG, "[OTPTokenHandler]: Generate OTP with OathTokenCode");

            FRAClient fraClient = initClient(context);

            Account account = getAccount(fraClient);
            OathMechanism mechanism = getOathMechanism(account);
            OathTokenCode token = mechanism.getOathTokenCode();
            String otp = token.getCurrentCode();

            var expiresIn = getRemainingTime(token);


            JSObject result = new JSObject();
            result.put("otp", otp);
            result.put("expiresIn", expiresIn);

            call.resolve(result);

        } catch (FRException e) {
            Log.e(TAG, "[OTPTokenHandler]: RETURN ERROR in get OTP with FRException");
          ErrorHandler.reject(call, e.getCode());
        } catch (Exception e) {
            Log.e(TAG, "[OTPTokenHandler]: RETURN FRE000 ERROR from onException");
            ErrorHandler.reject(call, ErrorHandler.ErrorCode.UNKNOWN_ERROR);
        }
    }

  private static JSObject validateExistMechanism(Context context) throws FRException {
      try{
        FRAClient fraClient = initClient(context);

        List<Account> accounts = fraClient.getAllAccounts();

        Mechanism found = null;

          if (accounts != null && !accounts.isEmpty()) {
              for (Account account : accounts) {
                  List<Mechanism> mechanisms = account.getMechanisms();
                  if (mechanisms != null && !mechanisms.isEmpty()) {
                      found = mechanisms.get(0);
                      break;
                  }
              }
          }

          JSObject result = new JSObject();
          result.put("empty",  accounts.isEmpty());

          if (found != null) {
              JSObject mechJson = new JSObject();
              mechJson.put("id", found.getId());
              mechJson.put("accountName", found.getAccountName());
              mechJson.put("issuer", found.getIssuer());
              result.put("mechanism", mechJson);
          } else {
              result.put("mechanism", null);
          }

          return result;
      } catch (Exception e) {
          Log.e(TAG, "error "+e);
          throw new FRException(ErrorHandler.ErrorCode.NO_ACCOUNTS_REGISTERED);
      }
    }

  private static long getRemainingTime(OathTokenCode token){
      long until = token.getUntil();
      long now = System.currentTimeMillis();

      return (until - now) / 1000;
    }

  private static FRAClient initClient(Context context) throws AuthenticatorException {
        return new FRAClient.FRAClientBuilder().withContext(context).start();
  }

  private static Account getAccount(FRAClient fraClient) throws FRException  {
        List<Account> accounts = fraClient.getAllAccounts();
        if (accounts == null || accounts.isEmpty()) {
          throw new FRException(ErrorHandler.ErrorCode.NO_ACCOUNTS_REGISTERED);
        }
        return accounts.get(0);
  }

   private static OathMechanism getOathMechanism(Account account) throws FRException  {
        for (Mechanism mechanism : account.getMechanisms()) {
            if (mechanism instanceof OathMechanism) {
                return (OathMechanism) mechanism;
            }
        }
      throw new FRException(ErrorHandler.ErrorCode.NO_OTP_REGISTERED);
    }

    private static void checkServerAndDeviceOtpState(PluginCall call, Context context, String uuid){
        String sessionToken = FRSession.getCurrentSession().getSessionToken().getValue();
        String cookie = "iPlanetDirectoryPro=" + sessionToken;

        String base_url = call.getString("url");

        if (base_url == null) {
            Log.e(TAG, "[OTPTokenHandler]: Error in get url");
            ErrorHandler.reject(call, ErrorHandler.ErrorCode.MISSING_JOURNEY);
            return;
        }

        String url = base_url+"/"+uuid+"/devices/2fa/oath?_queryFilter=true";

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Cookie", cookie)
                .addHeader("Accept", "application/json")
                .get()
                .build();

        new Thread(() -> {
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {

                    String json = response.body().string();

                    JSONObject jsonObject = new JSONObject(json);

                    Integer resultCount = jsonObject.getInt("resultCount");

                    boolean hasServerToken = !(resultCount == 0);
                    JSObject result = validateExistMechanism(context);
                    boolean hasDeviceToken = !result.getBool("empty");

                    if(hasDeviceToken && hasServerToken){
                        JSObject mechanism =result.getJSObject("mechanism");

                        hasServerToken = isOtpConsistentWithServer(uuid, mechanism);
                    }

                    Log.d(TAG, hasServerToken+ "hasDeviceToken"+hasDeviceToken);
                    sendOtpStatusResult(call, hasServerToken, hasDeviceToken);
                } else {
                    Log.e(TAG, "[OTPTokenHandler]:  Error HTTP: " + response.code());
                    ErrorHandler.reject(call, ErrorHandler.ErrorCode.HTTP_REQUEST_ERROR);
                }
            } catch (Exception e) {
                Log.e(TAG, "[OTPTokenHandler]:  Exception: " + e.getMessage(), e);
                ErrorHandler.reject(call, ErrorHandler.ErrorCode.HTTP_REQUEST_ERROR);
            }
        }).start();
    }

    private static boolean isOtpConsistentWithServer(String uuid , JSObject mechanism) {
         if(Objects.equals(uuid, mechanism.getString("accountName"))){
            return true;
        }
        return false;
    }

    private static void sendOtpStatusResult(PluginCall call, Boolean hasServerToken, Boolean hasDeviceToken ){
        JSObject result = new JSObject();

        result.put("hasServerToken", hasServerToken);
        result.put("hasDeviceToken", hasDeviceToken);
        Log.d(TAG, "result: " + result);
        call.resolve(result);
    }

}
