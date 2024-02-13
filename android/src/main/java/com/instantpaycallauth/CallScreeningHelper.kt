package com.instantpaycallauth

import android.app.Activity
import android.app.role.RoleManager
import android.content.Context
import android.content.Context.ROLE_SERVICE
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.BaseActivityEventListener
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.WritableMap
import com.facebook.react.modules.core.PermissionAwareActivity
import com.facebook.react.modules.core.PermissionListener
import org.json.JSONObject
import java.util.Objects


class CallScreeningHelper : PermissionListener  {

    lateinit var mContext: ReactApplicationContext
    private val REQUEST_ID_FOR_CALLER_ID_PERMISSION = 22
    private val REQUEST_ID_FOR_PERMISSION = 12
    private val LOCATION_PERMISSION = "android.permission.ACCESS_FINE_LOCATION"
    private val CALL_PERMISSION = arrayOf(
        "android.permission.READ_PHONE_STATE",
        "android.permission.READ_CALL_LOG",
        "android.permission.ANSWER_PHONE_CALLS",
        "android.permission.CALL_PHONE"
    )

    private val roleManagerActivityStatus = object : BaseActivityEventListener() {
        override fun onActivityResult(
            activity: Activity?,
            requestCode: Int,
            resultCode: Int,
            data: Intent?
        ) {
            super.onActivityResult(activity, requestCode, resultCode, data)

            logPrint("Reached on onActivityResultw ${requestCode}")

            if (requestCode == REQUEST_ID_FOR_CALLER_ID_PERMISSION) {
                when (resultCode) {

                    Activity.RESULT_OK -> {

                        val intent = Intent(InstantpayCallAuthModule.ACTION_RESULT_FOR_CALLER_ID_PERMISSION)
                        intent.putExtra("result", "SUCCESS")
                        mContext.currentActivity?.sendBroadcast(intent)
                    }

                    Activity.RESULT_CANCELED -> {
                        
                        val intent = Intent(InstantpayCallAuthModule.ACTION_RESULT_FOR_CALLER_ID_PERMISSION)
                        intent.putExtra("result", "CANCELLED")
                        mContext.currentActivity?.sendBroadcast(intent)
                    }

                    else -> {
                        val intent = Intent(InstantpayCallAuthModule.ACTION_RESULT_FOR_CALLER_ID_PERMISSION)
                        intent.putExtra("result", "ERROR")
                        mContext.currentActivity?.sendBroadcast(intent)
                    }
                }
            }

        }
    }

    constructor(mContext: ReactApplicationContext) {
        this.mContext = mContext
    }

    /**
     * Check necessary Permission
     */
    fun checkRequiredPermission(): MutableMap<String,String> {

        val output = mutableMapOf<String,String>()

        output["status"] = "false"
        output["msg"] = ""
        output["data"] = ""

        try {

            val currentActivity = mContext.currentActivity

            if (currentActivity is PermissionAwareActivity) {
                val getPermissionStatus = hasPermissions(currentActivity, CALL_PERMISSION)

                if(getPermissionStatus["status"].equals("false")){
                    
                    output["status"] = "false"
                    output["msg"] = "Permission Required (${getPermissionStatus["permisssionName"]})"
                }
                else{
                    output["status"] = "true"
                    output["msg"] = "All Permission Granted."
                }

                return output;
            }
            else{
                return output;
            }

            /*if(ContextCompat.checkSelfPermission(mContext, LOCATION_PERMISSION) !== PackageManager.PERMISSION_GRANTED ){

                val currentActivity = mContext.currentActivity
                if (currentActivity is PermissionAwareActivity) {
                    currentActivity.requestPermissions(arrayOf(LOCATION_PERMISSION), REQUEST_ID_FOR_PERMISSION, this)
                }
            }
            else{
                logPrint("Call not permission")
            }*/
        }
        catch (e : Exception){
            output["msg"] = "Error : ${e.message.toString()} #CSHCRP1"
            return output;
        }
    }

    private fun hasPermissions(activity: PermissionAwareActivity?, permissions: Array<String>?): MutableMap<String,String> {

        val output = mutableMapOf<String, String>()

        output["status"] = "false"
        output["permisssionName"] = ""

        if (activity != null && permissions != null) {
            for (permission in permissions) {
                if (activity.checkSelfPermission(permission!!.toString()) != PackageManager.PERMISSION_GRANTED) {

                    output["permisssionName"] = permission!!.toString()

                    return output
                }
            }
        }

        output["status"] = "true"
        return output
    }

    /**
     * Request for necessary Permission
     */
    fun requestRequiredPermission() : MutableMap<String,String>{

        val output = mutableMapOf<String,String>()

        output["status"] = "false"
        output["msg"] = ""
        output["data"] = arrayOf<String>().toString()

        val currentActivity = mContext.currentActivity

        if(currentActivity is PermissionAwareActivity){

            currentActivity.requestPermissions(CALL_PERMISSION, REQUEST_ID_FOR_PERMISSION, this)

            output["actCode"] = "ListenForPermission"
            output["status"] = "true"
            output["msg"] = "For Permission Response check for listener"
            return output
        }
        else{
            return output
        }
    }

    /**
     * Permission Response
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>?,
        grantResults: IntArray?
    ): Boolean {

        when(requestCode){
            REQUEST_ID_FOR_PERMISSION -> {
                var permsOut = ArrayList<MutableMap<String, String>>()
                if(grantResults!!.isNotEmpty()){

                    if(grantResults.size > 0){

                        for (i in 0..(grantResults.size - 1)){

                            val permsObj = mutableMapOf<String, String>()

                            if(grantResults[i] == PackageManager.PERMISSION_GRANTED){

                                permsObj["permissionName"] = permissions!!.get(i)
                                permsObj["permissionStatus"] = "GRANTED"

                            }
                            else{

                                permsObj["permissionName"] = permissions!!.get(i)
                                permsObj["permissionStatus"] = "BLOCKED"

                            }
                            permsOut.add(permsObj)
                        }
                    }
                }

                val intent = Intent(InstantpayCallAuthModule.ACTION_RESULT_FOR_PERMISSION)

                if(permsOut.size == 0){
                    intent.putExtra("result", "")
                }
                else{
                    intent.putExtra("result", permsOut.toString())
                }

                mContext?.currentActivity?.sendBroadcast(intent)

                /*if(grantResults!!.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){

                    val currentActivity = mContext.currentActivity
                    if (currentActivity is PermissionAwareActivity) {

                        if(currentActivity.checkSelfPermission(LOCATION_PERMISSION) == PackageManager.PERMISSION_GRANTED){
                            logPrint("Permission Granted after check : ${permissions?.get(0)}")
                        }
                        else{
                            logPrint("Permission Denied after check : ${permissions?.get(0)}")
                        }
                    }
                }
                else{
                    logPrint("Permission Denied : ${permissions?.get(0)}")
                }*/
            }
        }

        return true;
    }

    /**
     * Check App choose as Default CallId
     */
    fun isSelectedDefaultApp() : MutableMap<String,String> {

        val output = mutableMapOf<String,String>()

        output["status"] = "false"
        output["msg"] = ""
        output["data"] = ""

        try {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                logPrint("reached Build.VERSION_CODES.Q")
                val roleManager = mContext.getSystemService(ROLE_SERVICE) as RoleManager

                if (roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
                    if (roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)) {

                        output["status"] = "true"
                        output["msg"] = "App Selected as Default Caller Id"
                        return output

                    } else {
                        val roleRequestIntent = roleManager.createRequestRoleIntent(
                            RoleManager.ROLE_CALL_SCREENING
                        )

                        mContext.startActivityForResult(roleRequestIntent, REQUEST_ID_FOR_CALLER_ID_PERMISSION, null)

                        mContext.addActivityEventListener(roleManagerActivityStatus);

                        output["status"] = "true"
                        output["msg"] = "For Default Caller Id Permission Response check for listener"
                        output["actCode"] = "ListenForCallerPermission"
                        return output
                    }
                } else {
                    //Role is not available in system
                    output["status"] = "false"
                    output["msg"] = "Error : Something went wrong with Device Android  ${Build.VERSION.RELEASE}  #CSHSDA5"
                    return output
                }
            } else {
                //Not Supported Android Version
                output["status"] = "false"
                output["msg"] = "Error : Not Supported for Android Version ${Build.VERSION.RELEASE}  #CSHSDA3"
                return output
            }
        } catch (e: Exception) {

            output["status"] = "false"
            output["msg"] = "Error : ${e.message.toString()} #CSHSDA1"
            return output
        }
    }


    /**
     * For Show Log
     */
    private fun logPrint(value: String?) {
        if (value == null) {
            return
        }
        Log.i(InstantpayCallAuthModule.LOG_TAG, value)
    }
}
