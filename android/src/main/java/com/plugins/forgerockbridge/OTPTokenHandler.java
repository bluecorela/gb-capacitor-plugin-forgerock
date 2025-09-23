package com.plugins.forgerockbridge;

import android.content.Context;
import android.util.Log;


import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.PluginCall;

import org.forgerock.android.auth.Account;
import org.forgerock.android.auth.FRAClient;
import org.forgerock.android.auth.FRListener;
import org.forgerock.android.auth.FRSession;
import org.forgerock.android.auth.FRUser;
import org.forgerock.android.auth.Logger;
import org.forgerock.android.auth.Mechanism;
import org.forgerock.android.auth.NodeListener;
import org.forgerock.android.auth.OathMechanism;
import org.forgerock.android.auth.OathTokenCode;
import org.forgerock.android.auth.UserInfo;
import org.forgerock.android.auth.exception.AuthenticatorException;
import org.json.JSONObject;

import com.plugins.forgerockbridge.ErrorHandler.FRException;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

import org.json.*;

public class OTPTokenHandler {
    private static final String TAG = "ForgeRockBridge";
    private static final MediaType MEDIA_JSON = MediaType.get("application/json; charset=utf-8");

    private static OkHttpClient httpClient() {
        HttpLoggingInterceptor httpLog = new HttpLoggingInterceptor(msg -> Log.d(TAG, msg));
        httpLog.setLevel(HttpLoggingInterceptor.Level.BODY);
        return new OkHttpClient.Builder()
                .addInterceptor(httpLog)
                .followRedirects(false)
                .build();
    }

  public static void startOtpJourney(PluginCall call, Context context, NodeListener<FRSession> listener ) {
        String journey = call.getString("journey");

        if (journey == null) {
            Log.e(TAG, "[OTPTokenHandler]: Error in get Journey");
          ErrorHandler.reject(call, ErrorHandler.ErrorCode.MISSING_JOURNEY);
            return;
        }
      Log.d(TAG, "[OTPTokenHandler]: PASO AQUI "+journey);

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

    public static void isValidAuthMethod(PluginCall call) {

        String baseUrl = call.getString("url");
        String trxId   = call.getString("trxId");
        JSObject payloadObj = call.getObject("payload");
        String payload = "{}";

        if (payloadObj != null) {
            payload = payloadObj.toString();
            Log.d(TAG, "PAYLOAD: " + payload);
        }

        String adviceXml = "<Advices><AttributeValuePair><Attribute name='TransactionConditionAdvice'/>"
                + "<Value>" + trxId + "</Value></AttributeValuePair></Advices>";
        String qs = "authIndexType=composite_advice&authIndexValue="
                + URLEncoder.encode(adviceXml, StandardCharsets.UTF_8);

        String url = baseUrl + (baseUrl.contains("?") ? "&" : "?") + qs;

        executeHttpQuery(url, payload, call);
    }

    private static void executeHttpQuery(String url, String payload, PluginCall call) {
        String sessionToken = FRSession.getCurrentSession().getSessionToken().getValue();
        String cookie = "iPlanetDirectoryPro=" + sessionToken;
        RequestBody body = RequestBody.create(payload, MEDIA_JSON);

        Request req = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .addHeader("Accept-API-Version", "protocol=1.0,resource=2.0")
                .addHeader("Cookie", cookie)
                .build();

        handleHttpRequest(req, call);
    }

    private static void handleHttpRequest( Request req, PluginCall call){
        OkHttpClient client = httpClient();
        new Thread(() -> {
            try (Response resp = client.newCall(req).execute()) {

                String text = resp.body() != null ? resp.body().string() : "";
                Log.d(TAG, "RESPONSE"+ text);
                if (text.trim().isEmpty()) {
                    Log.e(TAG, "Without JSON");
                    return;
                }
                AuthMethodResponse(call, text);

            } catch (Exception e) {
                Log.e(TAG, "[OTPTokenHandler]: handleHttpRequest exception: " + e.getMessage(), e);
                ErrorHandler.reject(call, ErrorHandler.ErrorCode.HTTP_REQUEST_ERROR);
            }
        }).start();

    }

    private static void AuthMethodResponse(PluginCall call, String text) throws JSONException {
        JSONObject json = new JSONObject(text);

        String successResponse = json.optString("tokenId", null);
        String errorResponse = json.optString("code", null);
  

        if(successResponse != null || errorResponse != null){
            Log.d(TAG, "ENTRO AQUI: ");
            String status = successResponse != null ? "success": "failed";
            JSObject buildSuccessCallback =  buildSuccessErrorCallback(status);
            Log.d(TAG, "buildSuccessCallback r: " + buildSuccessCallback);
            call.resolve(buildSuccessCallback);
           
        }else{
            Log.d(TAG, "ENTRO ACA: ");
            Log.d(TAG, "JSON OK: " + json);
            call.resolve(JSObject.fromJSONObject(json));
        }

    }

    private static JSObject buildSuccessErrorCallback(String status) {
        try {

            JSObject outMessage = new JSObject()
                    .put("name", "status")
                    .put("value", status);

            JSArray output = (JSArray) new JSArray().put(outMessage);

            JSObject successCallback = new JSObject()
                    .put("type", "SuccessCallback")
                    .put("output", output);

            JSArray callbacks = (JSArray) new JSArray().put(successCallback);

            return new JSObject()
                    .put("callbacks", callbacks);
        } catch (Exception e) {
            Log.e(TAG, "SUCCESS ERROR "+e);
          throw new RuntimeException("Error building TextOutputCallback", e);
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
        Log.d(TAG, "sessionToken"+sessionToken);
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
