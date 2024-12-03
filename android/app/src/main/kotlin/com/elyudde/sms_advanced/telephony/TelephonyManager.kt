package com.elyudde.sms_advanced.telephony

import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import java.lang.reflect.InvocationTargetException
import android.provider.Settings
import java.util.UUID

class TelephonyManager(private val context: Context) {
    private var manager: TelephonyManager? = null
        private get() {
            if (field == null) {
                field = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            }
            return field
        }

    @get:TargetApi(Build.VERSION_CODES.M)
    val simCount: Int
        get() = manager!!.phoneCount

    fun generateDeviceIdentifier(): String {
        val uniqueDevicePseudoID = "35" +
            Build.BOARD.length % 10 +
            Build.BRAND.length % 10 +
            Build.DEVICE.length % 10 +
            Build.DISPLAY.length % 10 +
            Build.HOST.length % 10 +
            Build.ID.length % 10 +
            Build.MANUFACTURER.length % 10 +
            Build.MODEL.length % 10 +
            Build.PRODUCT.length % 10 +
            Build.TAGS.length % 10 +
            Build.TYPE.length % 10 +
            Build.USER.length % 10
        val serial = Build.getRadioVersion()
        return UUID(uniqueDevicePseudoID.hashCode().toLong(), serial.hashCode().toLong()).toString()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun getSimId(slotId: Int): String {
        val deviceId = try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        } catch (e: Exception) {
            generateDeviceIdentifier()
        } ?: UUID.randomUUID().toString()
        return deviceId
        //return manager!!.getDeviceId(slotId)
    }

    fun getSimState(slotId: Int): Int {
        try {
            val getSimStateMethod = manager!!.javaClass.getMethod(
                "getSimState",
                Int::class.javaPrimitiveType
            )
            val result = getSimStateMethod.invoke(manager, slotId)
            if (result != null) {
                return result as Int
            }
        } catch (e: NoSuchMethodException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        } catch (e: InvocationTargetException) {
            e.printStackTrace()
        }
        return TelephonyManager.SIM_STATE_UNKNOWN
    }
}
