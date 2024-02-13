package com.instantpaycallauth

import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.WritableMap

object CommonHelpers {

    /**
     * Make Output before send to React Native
     */
    fun response(params : MutableMap<String, String>) : WritableMap {

        val map: WritableMap = Arguments.createMap()
        //map.putString("status", status)

        var statusType = ""

        if(params.containsKey("status") && params["status"]!!.isNotEmpty()){
            statusType = ""
            map.putString("status", params["status"])
        }
        else{
            statusType = "FAILED"
            map.putString("status", "FAILED")
        }

        if(params.containsKey("message") && params["message"]!!.isNotEmpty()){
            map.putString("message", params["message"])
        }
        else{

            if(statusType == "FAILED"){
                map.putString("message", "Something went wrong, #ICA")
            }
            else{
                map.putString("message", "")
            }
        }

        if(params.containsKey("actCode") && params["actCode"]!!.isNotEmpty()){
            map.putString("actCode", params["actCode"])
        }
        else{
            map.putString("actCode", "")
        }

        if(params.containsKey("data")){
            map.putString("data", params["data"])
        }
        else{
            map.putString("data", "")
        }

        return map;
    }
}
