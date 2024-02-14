package com.instantpaycallauth

import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.provider.CallLog
import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log
import androidx.annotation.RequiresApi
import org.json.JSONObject
import org.json.JSONTokener
import java.lang.reflect.InvocationTargetException


@RequiresApi(Build.VERSION_CODES.N)
class CallAuthScreeningService : CallScreeningService() {

    companion object {
        const val EXTRA_PHONE_NUMBER = "phoneNumber"
        var verificationOptions: JSONObject? = null
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onScreenCall(callDetails: Call.Details) {

        try {

            //logPrint("CallAuthScreeningService -> getIntent: " + callDetails);

            if(verificationOptions!=null && verificationOptions is JSONObject){

                val requestParams = verificationOptions!!

                if(requestParams.has("requiredAction")
                    && requestParams.get("requiredAction") !=null
                        && requestParams.get("requiredAction") is JSONObject){

                    val takeAction =  JSONTokener(requestParams.getString("requiredAction")).nextValue() as JSONObject

                    if(takeAction.getBoolean("matchWithVerifyFor")){

                        if(requestParams.has("verifyFor")
                            && requestParams.getString("verifyFor") != null
                            && requestParams.getString("verifyFor").isNotEmpty()){

                            var countyCode = "+91"

                            if(requestParams.has("countryCode") && requestParams.getString("countryCode").isNotEmpty()){
                                countyCode = requestParams.getString("countryCode")
                            }

                            val verifyFor = requestParams.getString("verifyFor")

                            val fullVerifyNo = countyCode + verifyFor

                            val isIncoming = callDetails.callDirection == Call.Details.DIRECTION_INCOMING

                            var callResponse  = CallResponse.Builder()

                            //Sets whether the incoming call should be blocked.
                            if(takeAction.has("disallowCall") && takeAction.getBoolean("disallowCall") == true){

                                callResponse = callResponse.setDisallowCall(true)

                                //Sets whether the incoming call should be disconnected as if the user had manually
                                // rejected it. This property should only be set to true if the call is disallowed.
                                if(takeAction.has("rejectCall") && takeAction.getBoolean("rejectCall") == true){
                                    callResponse = callResponse.setRejectCall(true)
                                }

                                //Sets whether the incoming call should not be displayed in the call log. This property
                                // should only be set to true if the call is disallowed.
                                if(takeAction.has("skipCallLog") && takeAction.getBoolean("skipCallLog") == true){
                                    callResponse = callResponse.setSkipCallLog(true)
                                }

                                //Sets whether a missed call notification should not be shown for the incoming call.
                                //This property should only be set to true if the call is disallowed.
                                if(takeAction.has("skipNotification") && takeAction.getBoolean("skipNotification") == true){
                                    callResponse = callResponse.setSkipNotification(true)
                                }

                            }
                            else{
                                callResponse = callResponse.setDisallowCall(false)
                            }

                            if(takeAction.has("silenceCall") && takeAction.getBoolean("silenceCall") == true){
                                callResponse = callResponse.setSilenceCall(true)
                            }
                            else{
                                callResponse = callResponse.setSilenceCall(false)
                            }

                            val callResponseBody = callResponse.build()

                            if (isIncoming) {

                                val incomingNumber = extractPhoneNumber(callDetails)

                                if (incomingNumber.equals(fullVerifyNo)) {

                                    respondToCall(callDetails, callResponseBody)

                                    //logPrint("rejectCall CallNumber : " + callResponseBody.rejectCall)

                                    sendToMainModule(callDetails)
                                }
                            }
                        }
                    }
                }
            }

        } catch (ei: InvocationTargetException) {

            //logPrint("Error InvocationTargetException: " + ei.cause.toString())
            sendToMainModule(null , ei.cause.toString())

        } catch (e: Exception) {
            //logPrint("Error CallAuthScreeningService Exception: " + e.toString())
            sendToMainModule(null , e.message.toString())

        }

    }

    /**
     * Get Incoming Call Mobile Number
     */
    private fun extractPhoneNumber(callDetails: Call.Details): String? {
        val handle = callDetails.handle
        if (handle != null) {
            // Attempt to extract phone number from the handle
            return handle.schemeSpecificPart
        } else {
            // Handle is null, try other methods if available
            val gatewayInfo = callDetails.gatewayInfo
            if (gatewayInfo != null) {
                return gatewayInfo.originalAddress.schemeSpecificPart
            }
        }
        return null
    }

    /**
     * Fire broadcast event
     */
    private fun sendToMainModule(details: Call.Details?, error: String? = null) {

        val intent = Intent(InstantpayCallAuthModule.ACTION_RESULT_FOR_CALL_SCREENING)

        if(details!=null){
            val incomingNumber = extractPhoneNumber(details)
            intent.putExtra("status", "SUCCESS")
            intent.putExtra("incomingNumber", incomingNumber)
        }
        else{
            intent.putExtra("status", "FAILED")
            if(error!=null){
                intent.putExtra("message", error)
            }
            else{
                intent.putExtra("message", "something went wrong #CSS")
            }
        }

        sendBroadcast(intent)
    }


    override fun onBind(intent: Intent?): IBinder? {
        return super.onBind(intent)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        return super.onUnbind(intent)
    }

    private fun logPrint(value: String?) {
        if (value == null) {
            return
        }
        Log.i(InstantpayCallAuthModule.LOG_TAG, value)
    }
}
