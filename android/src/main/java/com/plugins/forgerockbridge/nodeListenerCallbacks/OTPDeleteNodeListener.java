package com.plugins.forgerockbridge.nodeListenerCallbacks;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.getcapacitor.JSObject;
import com.getcapacitor.PluginCall;
import com.plugins.forgerockbridge.state.PluginState;

import org.forgerock.android.auth.FRSession;
import org.forgerock.android.auth.Node;
import org.forgerock.android.auth.NodeListener;
import org.forgerock.android.auth.Mechanism;
import org.forgerock.android.auth.FRAClient;
import org.forgerock.android.auth.*;


import java.util.List;

import com.plugins.forgerockbridge.ErrorHandler;
import com.plugins.forgerockbridge.ErrorHandler.FRException;
public class OTPDeleteNodeListener implements NodeListener<FRSession> {

    private static final String TAG = "ForgeRockBridge";
    private final PluginCall call;
    private final Context context;
    private final PluginState pluginState;

    public OTPDeleteNodeListener(PluginCall call, Context context, PluginState pluginState) {
        this.call = call;
        this.context = context;
        this.pluginState = pluginState;
    }

    @Override
    public void onCallbackReceived(Node node) {
    }

    @Override
    public void onException(@NonNull Exception e) {
        pluginState.reset();
        ErrorHandler.reject(call, ErrorHandler.ErrorCode.AUTHENTICATE_FAILED);
    }

    @Override
    public void onSuccess(FRSession frSession) {
        try {
            deleteOtpRegister();

        } catch (Exception e) {
          ErrorHandler.reject(call, ErrorHandler.ErrorCode.DELETE_OTP_FAILED);
        }
        pluginState.reset();

    }

    private void deleteOtpRegister() {
        try {

            boolean isMechanismDeleted = deleteOnlyMechanism();
            if(isMechanismDeleted){
                finalStepToDeleteOTP();
            }else{
                ErrorHandler.reject(call, ErrorHandler.ErrorCode.DELETE_OTP_MECHANISM_FAILED);
            }


        } catch (Exception e) {
            Log.e(TAG, "[OTPDeleteNodeListener]  authenticate error", e);
          ErrorHandler.reject(call, ErrorHandler.ErrorCode.AUTHENTICATE_FAILED);
        }
    }

    private boolean deleteOnlyMechanism() throws FRException{
        try {
            FRAClient fraClient = new FRAClient.FRAClientBuilder()
                    .withContext(this.context)
                    .start();

            List<Account> accounts = fraClient.getAllAccounts();
            if (!accounts.isEmpty()) {
                Account account = accounts.get(0);

                List<Mechanism> mechanisms = account.getMechanisms();
                if (!mechanisms.isEmpty()) {
                    Mechanism mechanism = mechanisms.get(0);
                    fraClient.removeMechanism(mechanism);

                    return true;
                }
                return false;
            }

        } catch (Exception e) {
            Log.e(TAG, "[OTPDeleteNodeListener]  authenticate error", e);
            throw new FRException(ErrorHandler.ErrorCode.AUTHENTICATE_FAILED);
        }

        return false;
    }

    private void finalStepToDeleteOTP(){
      JSObject result = new JSObject();
      result.put("status", "success");
      result.put("message", "OTP eliminado correctamente");
      call.resolve(result);
    }
}
