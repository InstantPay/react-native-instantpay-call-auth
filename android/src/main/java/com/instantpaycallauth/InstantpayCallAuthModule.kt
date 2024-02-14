package com.instantpaycallauth

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.IBinder
import android.provider.CallLog
import android.util.Log
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.WritableMap
import com.facebook.react.modules.core.DeviceEventManagerModule
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener


class InstantpayCallAuthModule(reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {

    val SUCCESS: String = "SUCCESS"
    val FAILED: String = "FAILED"
    lateinit var DATA: String
    private var responsePromise: Promise? = null
    lateinit var callScreeningHelper: CallScreeningHelper
    private var callerIdPermissionResultReceiver: BroadcastReceiver? = null

    companion object {
        const val NAME = "InstantpayCallAuth"

        private lateinit var reactContexts: ReactApplicationContext

        private var isServiceConnected = false

        private var callScreeningResultReceiver: BroadcastReceiver? = null

        private var permissionResultReceiver: BroadcastReceiver? = null

        const val LOG_TAG = "CallAuthLog*"

        const val ACTION_RESULT_FOR_PERMISSION = "com.instantpaycallauth.IPAY_CALL_AUTH_PERMISSION_RESULT"

        const val ACTION_RESULT_FOR_CALLER_ID_PERMISSION = "com.instantpaycallauth.IPAY_CALL_AUTH_PERMISSION_CALLER_ID"

        const val ACTION_RESULT_FOR_CALL_SCREENING = "com.instantpaycallauth.IPAY_CALL_AUTH_CALL_SCREENING_INFO"

    }

    override fun getName(): String {
        return NAME
    }


    init {
        reactContexts = reactContext

        callScreeningHelper = CallScreeningHelper(reactContexts)
    }

    /**
     * Request for required Permission
     */
    @ReactMethod
    fun requestForPermission(promise: Promise){

        responsePromise = promise

        try {

            val permissionResp = callScreeningHelper.requestRequiredPermission()

            val sendStatus = if(permissionResp["status"] == "false")  FAILED else SUCCESS

            if(permissionResp["status"].equals("true")){
                registerPermissionResultReceiver()
            }

            resolve(
                permissionResp["msg"].toString(),
                sendStatus,
                "",
                permissionResp["actCode"].toString()
            )

        }
        catch (e:Exception){
            resolve(e.message.toString() + " #IAMRFP1")
        }
    }

    /**
     * Register Permission Result Receiver
     */
    private fun registerPermissionResultReceiver(){

        if(permissionResultReceiver == null){

            permissionResultReceiver = object : BroadcastReceiver(){
                override fun onReceive(context: Context?, intent: Intent?) {
                    intent?.let {
                        if(it.action == ACTION_RESULT_FOR_PERMISSION){

                            //logPrint("get intent " + it.getStringExtra("result"))

                            val perms = mutableMapOf<String, String>()
                            perms["status"] = "SUCCESS"
                            perms["message"] = "Permission Result"

                            if(it.getStringExtra("result")!!.isNotEmpty()){
                                perms["data"] = JSONArray(it.getStringExtra("result").toString()).toString()
                            }

                            val outPer = CommonHelpers.response(perms)

                            sendEvent(reactContexts, "RequiredPermissionResult", outPer)
                        }
                    }
                }
            }

            reactContexts.registerReceiver(
                permissionResultReceiver,
                IntentFilter(ACTION_RESULT_FOR_PERMISSION)
            )
        }

    }

    /**
     * Un-Register Permission Result Receiver
     */
    private fun unregisterPermissionResultReceiver(){
        if(permissionResultReceiver!=null){
            permissionResultReceiver.let {
                reactContexts.unregisterReceiver(it)
                permissionResultReceiver = null
            }
        }

    }

    /**
     * To check User gave permission and set App as default CallId
     */
    @ReactMethod
    fun isAppAllowForScreening(promise: Promise){

        responsePromise = promise

        try {

            val permissionResponse =  callScreeningHelper.checkRequiredPermission()

            //logPrint("permissionResponse : ${permissionResponse}")

            if(permissionResponse["status"].equals("false")){
                return resolve(permissionResponse["msg"].toString())
            }

            val defaultResponse = callScreeningHelper.isSelectedDefaultApp()

            var getStatusType =  "";

            if(defaultResponse["status"].equals("true")){
                getStatusType = SUCCESS
            }
            else{
                getStatusType = FAILED
            }


            if(defaultResponse.containsKey("actCode")
                && defaultResponse["actCode"]!!.isNotEmpty()
                && defaultResponse["actCode"]!!.equals("ListenForCallerPermission")){

                registerCallerIdPermissionResultReceiver()
            }

            return resolve(
                defaultResponse["msg"].toString(),
                getStatusType,
                defaultResponse["data"].toString(),
                defaultResponse["actCode"].toString(),
            )

        }
        catch (e:Exception){
            resolve(e.message.toString() + " #IAMAAS1")
        }
    }

    /**
     * Register Caller Id Permission Result Receiver
     */
    private fun registerCallerIdPermissionResultReceiver(){
        if(callerIdPermissionResultReceiver == null){
            callerIdPermissionResultReceiver = object : BroadcastReceiver(){
                override fun onReceive(context: Context?, intent: Intent?) {
                    intent?.let {
                        if(it.action == ACTION_RESULT_FOR_CALLER_ID_PERMISSION){
                            //logPrint("get intent " + it.getStringExtra("result"))

                            val perms = mutableMapOf<String, String>()
                            perms["status"] = "SUCCESS"
                            perms["message"] = "Caller Id Permission Result"

                            if(it.getStringExtra("result")!!.isNotEmpty()){

                                val statusObj = mutableMapOf<String, String>()
                                statusObj["permissionStatus"] = it.getStringExtra("result").toString()
                                perms["data"] = JSONObject(statusObj.toString()).toString()
                            }

                            val outPer = CommonHelpers.response(perms)

                            sendEvent(reactContexts, "CallerIdPermissionResult", outPer)
                        }
                    }
                }
            }

            reactContexts.registerReceiver(callerIdPermissionResultReceiver, IntentFilter(
                ACTION_RESULT_FOR_CALLER_ID_PERMISSION)
            )
        }
    }

    /**
     * Un-Register Caller Id Permission Result Receiver
     */
    private fun unregisterCallerIdPermissionResultReceiver(){
        if(callerIdPermissionResultReceiver!=null){
            callerIdPermissionResultReceiver.let {
                reactContexts.unregisterReceiver(it)
                callerIdPermissionResultReceiver = null
            }
        }

    }

    /**
     * Start Call Screening
     */
    @ReactMethod
    fun startCallScreening(options: String?, promise: Promise) {
        if (!isServiceConnected) {

            responsePromise = promise

            if(options!=null){
                val items = JSONTokener(options).nextValue() as JSONObject;

                // Set the target phone number in the service
                CallAuthScreeningService.verificationOptions = items
            }

            // Start the service dynamically
            val serviceIntent =
                Intent(reactContexts, CallAuthScreeningService::class.java)
            reactContexts.startService(serviceIntent)

            // Bind to the service
            val bindIntent = Intent(reactContexts, CallAuthScreeningService::class.java)
            reactContexts.bindService(
                bindIntent,
                serviceConnection,
                Context.BIND_AUTO_CREATE
            )

            // Register the broadcast receiver to listen for results
            registerResultForCallScreeningReceiver()
        }
    }

    /**
     * Stop Call Screening
     */
    @ReactMethod
    fun stopCallScreening(promise: Promise) {
        if (isServiceConnected && serviceConnection!=null) {

            responsePromise = promise

            // Stop the service
            val serviceIntent =
                Intent(reactContexts, CallAuthScreeningService::class.java)
            reactContexts.stopService(serviceIntent)

            // Unbind from the service
            reactContexts.unbindService(serviceConnection)
            isServiceConnected = false

            // Reset the target phone number in the service
            CallAuthScreeningService.verificationOptions = null

            // Unregister the broadcast receiver
            unregisterResultForCallScreeningReceiver()
        }
    }

    /**
     * Service Connection
     */
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {

            //logPrint("Connect to Service")
            isServiceConnected = true

            resolve("Conneted to Call Services", SUCCESS, "", "CONNECTED")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isServiceConnected = false
        }
    }

    /**
     * Register Result for Call Screening
     */
    private fun registerResultForCallScreeningReceiver() {
        if (callScreeningResultReceiver == null) {
            callScreeningResultReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    intent?.let {
                        if (it.action == ACTION_RESULT_FOR_CALL_SCREENING) {
                            /*val phoneNumber =
                                it.getStringExtra(
                                    CallAuthScreeningService.EXTRA_PHONE_NUMBER
                                )
                            sendPhoneNumberToReactNative(phoneNumber)*/

                            val perms = mutableMapOf<String, String>()
                            perms["status"] = "SUCCESS"
                            perms["message"] = "Call Screening Result"

                            if(it.getStringExtra("status")!!.isNotEmpty() && it.getStringExtra("status").equals("SUCCESS")){

                                if(it.getStringExtra("incomingNumber")!!.isNotEmpty()){

                                    val statusObj = mutableMapOf<String, String>()
                                    statusObj["incomingNumber"] = it.getStringExtra("incomingNumber").toString()
                                    perms["data"] = JSONObject(statusObj.toString()).toString()
                                }
                            }
                            else{
                                perms["status"] = "FAILED"
                                perms["message"] = it.getStringExtra("message").toString()
                            }

                            val outPer = CommonHelpers.response(perms)

                            sendEvent(reactContexts, "CallScreeningResult", outPer)
                        }
                    }
                }
            }

            reactContexts.registerReceiver(
                callScreeningResultReceiver,
                IntentFilter(ACTION_RESULT_FOR_CALL_SCREENING)
            )
        }
    }

    /**
     * Un-register Result for Call Screening
     */
    private fun unregisterResultForCallScreeningReceiver() {
        if(callScreeningResultReceiver!=null){

            callScreeningResultReceiver?.let {
                reactContexts.unregisterReceiver(it)
                callScreeningResultReceiver = null

                resolve("Disconnected to Call Services", SUCCESS, "", "DISCONNECTED")
            }
        }

    }

    /**
     * Remove Verified Call Log
     */
    @ReactMethod
    fun removeVerifiedCallLog(numberTag: String, promise: Promise) {

        responsePromise = promise

        try {

            val queryString = CallLog.Calls.NUMBER + " LIKE '%" + numberTag + "%'"

            reactContexts.contentResolver.delete(CallLog.Calls.CONTENT_URI, queryString, null)

            resolve("Successfully Removed", SUCCESS)
        }
        catch (e:Exception){
            resolve(e.message.toString() + " #IAMRVC1")
        }
    }

    // Required for rn built in EventEmitter Calls.
    @ReactMethod
    fun addListener(eventName: String?) {
    }

    @ReactMethod
    fun removeListeners(count: Int?) {
        unregisterPermissionResultReceiver()
    }

    private fun sendPhoneNumberToReactNative(phoneNumber: String?) {
        if (phoneNumber != null) {

            //logPrint("send to reactNative : " + phoneNumber);

            // Send the phone number back to React Native
            reactContexts
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
                .emit("callScreeningResult", phoneNumber)
        }
    }

    /**
     * Help to return data to React native
     */
    private fun resolve(
        message: String,
        status: String = FAILED,
        data: String = "",
        actCode: String = ""
    ) {

        if (responsePromise == null) {
            return;
        }

        val map: WritableMap = Arguments.createMap()
        map.putString("status", status)
        map.putString("message", message)
        map.putString("data", data)
        map.putString("actCode", actCode)

        responsePromise!!.resolve(map)
        responsePromise = null
    }

    /**
     * Send Event to React Native
     */
    private fun sendEvent(reactContext: ReactContext, eventName: String, params: WritableMap?) {
        if (reactContext.hasCatalystInstance()) {

            reactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
                .emit(eventName, params)
        }
    }

    /**
     * For Show Log
     */
    private fun logPrint(value: String?) {
        if (value == null) {
            return
        }
        Log.i(LOG_TAG, value)
    }

}
