package com.elyudde.sms_advanced

import android.Manifest
import android.annotation.TargetApi
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SmsManager
import com.elyudde.sms_advanced.permisions.Permissions
import io.flutter.embedding.android.FlutterFragmentActivity
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.PluginRegistry.RequestPermissionsResultListener
import java.util.*
import android.util.Log
import android.telephony.SubscriptionManager

/**
 * Created by crazygenius on 1/08/21.
 */
@TargetApi(Build.VERSION_CODES.DONUT)
internal class SmsSenderMethodHandler(
    val context: Context,
    private val result: MethodChannel.Result,
    private val address: String,
    private val body: String,
    private val sentId: Int,
    private val subId: Int?,
    private val forceMms: Boolean?
) :
    RequestPermissionsResultListener {
    private val permissionsList =
        arrayOf(Manifest.permission.SEND_SMS, Manifest.permission.READ_PHONE_STATE)

    fun handle(permissions: Permissions) {
        if (permissions.checkAndRequestPermission(permissionsList, Permissions.SEND_SMS_ID_REQ)) {
            sendSmsMessage()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ): Boolean {
        if (requestCode != Permissions.SEND_SMS_ID_REQ) {
            return false
        }
        var isOk = true
        for (res in grantResults) {
            if (res != PackageManager.PERMISSION_GRANTED) {
                isOk = false
                break
            }
        }
        if (isOk) {
            sendSmsMessage()
            return true
        }
        result.error("#01", "permission denied for sending sms", null)
        return false
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private fun sendSmsMessage() {
        val sentIntent = Intent("SMS_SENT")
        .putExtra("sentId", sentId)
        val sentPendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            sentIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        val deliveredIntent = Intent("SMS_DELIVERED")
        deliveredIntent.putExtra("sentId", sentId)
        val deliveredPendingIntent = PendingIntent.getBroadcast(
            context,
            UUID.randomUUID().hashCode(),
            deliveredIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        Log.d("DoctaSms", "subId:  $subId")
        //val subscriptionManager = SubscriptionManager.from(context)

        val subscriptionManager = if (Build.VERSION.SDK_INT >= 28) {
            context.getSystemService(SubscriptionManager::class.java)
        } else {
            SubscriptionManager.from(context) // Deprecated method for API level < 28
        }

        val subscriptionList = subscriptionManager.activeSubscriptionInfoList
        val subscriptionIds = mutableListOf<Int>()
        
        for (subscriptionInfo in subscriptionList) {
            subscriptionIds.add(subscriptionInfo.subscriptionId)
        }
        Log.d("DoctaSms", "subscriptionIds:  $subscriptionIds")
        val sms: SmsManager = if (subId == null) {
            SmsManager.getDefault()
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                SmsManager.getSmsManagerForSubscriptionId(subId)
            } else {
                result.error("#03", "this version of android does not support multicard SIM", null)
                return
            }
        }
        Log.d("DoctaSms", "forceMms:  $forceMms")
        if (forceMms == null){
          sms.sendTextMessage(address, null, body, sentPendingIntent, deliveredPendingIntent)
        }
        else{
            val parts = sms.divideMessage(body)
            val sentIntents = ArrayList<PendingIntent>()
            val deliveredIntents = ArrayList<PendingIntent>()

            for (i in 0 until parts.size) {
                sentIntents.add(PendingIntent.getBroadcast(context, 0, Intent("SMS_SENT"), PendingIntent.FLAG_IMMUTABLE))
                deliveredIntents.add(PendingIntent.getBroadcast(context, 0, Intent("SMS_DELIVERED"), PendingIntent.FLAG_IMMUTABLE))
            }
            sms.sendMultipartTextMessage(address, null, parts, sentIntents, deliveredIntents)
        }
        result.success(null)
    }

    companion object {
        private val sms = SmsManager.getDefault()
    }
}

@TargetApi(Build.VERSION_CODES.DONUT)
internal class SmsSender(val context: Context, private val binding: ActivityPluginBinding) : MethodCallHandler {
    private val permissions: Permissions = Permissions(context, binding.activity as FlutterFragmentActivity)
    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        if (call.method == "sendSMS") {
            val address = call.argument<Any>("address").toString()
            val body = call.argument<Any>("body").toString()
            val sentId = call.argument<Int>("sentId")!!
            val subId = call.argument<Int>("subId")
            val forceMms = call.argument<Boolean>("forceMms")
            if (address == null) {
                result.error("#02", "missing argument 'address'", null)
            } else if (body == null) {
                result.error("#02", "missing argument 'body'", null)
            } else {
                val handler =
                    SmsSenderMethodHandler(context, result, address, body, sentId, subId, forceMms)
                binding.addRequestPermissionsResultListener(handler)
                handler.handle(permissions)
            }
        } else {
            result.notImplemented()
        }
    }

}
