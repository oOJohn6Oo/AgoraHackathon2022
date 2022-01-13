package io.agora.example.familygame.util

import android.animation.ObjectAnimator
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Color
import android.graphics.Point
import android.graphics.PointF
import android.os.Build
import android.util.Log
import android.util.TypedValue
import android.view.DragAndDropPermissions
import android.view.HapticFeedbackConstants.VIRTUAL_KEY
import android.view.MotionEvent
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.Window
import android.view.animation.OvershootInterpolator
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.AttrRes
import androidx.annotation.MainThread
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.google.android.material.transition.MaterialArcMotion
import com.google.android.material.transition.MaterialContainerTransform
import io.agora.rtm.*
import java.lang.reflect.ParameterizedType
import kotlin.reflect.KClass

//<editor-fold desc="Must Have">
fun String.log(debug: Boolean = true) {
    if (debug)
        Log.d("lq", this)
    else
        Log.e("lq", this)
}

fun String.toast(context: Context){
    Toast.makeText(context, this, Toast.LENGTH_SHORT).show()
}

val Int.dp
    get() = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        this.toFloat(),
        Resources.getSystem().displayMetrics
    )

val Float.dp
    get() = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        this,
        Resources.getSystem().displayMetrics
    )

val Int.sp
    get() = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP,
        this.toFloat(),
        Resources.getSystem().displayMetrics
    )

val Float.sp
    get() = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP,
        this,
        Resources.getSystem().displayMetrics
    )
//</editor-fold>


//<editor-fold desc="Tool">
fun View.hideKeyboard() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        this.windowInsetsController?.hide(WindowInsetsCompat.Type.ime())
    }else{
        (context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(windowToken, 0)
    }
}

fun View.showKeyboard() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        this.windowInsetsController?.show(WindowInsetsCompat.Type.ime())
    }else{
        (context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(this, InputMethodManager.SHOW_FORCED)
    }
}

fun Class<*>.getGenericType(index: Int) =
    (this.genericSuperclass as ParameterizedType).actualTypeArguments[index] as Class<*>
//</editor-fold>

//<editor-fold desc="Theme & UI related">
val isNightModeNow: Boolean
    get() = Resources.getSystem().configuration.isNightModeNow

val Configuration.isNightModeNow
    get() = this.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES


/**
 * Get the id you want
 * eg: {@link android.R.attr.actionBarSize}
 */
fun Context.getAttrResId(@AttrRes attr: Int) =
    TypedValue().let {
        this.theme.resolveAttribute(attr, it, true)
        it.resourceId
    }

/**
 * I found some color attrs do not have Id
 * So {@link this#getAttrResId} do not work
 */
fun Context.getColorInt(@AttrRes colorAttrId: Int) =
    TypedValue().let { tv ->
        this.theme.resolveAttribute(colorAttrId, tv, true)
        ContextCompat.getColor(this, tv.resourceId).takeIf { tv.type == TypedValue.TYPE_STRING }
            ?: tv.data
    }

val Int.tint
    get() = ColorStateList.valueOf(this)

fun View.visible(show: Boolean = true) {
    this.visibility = if (show) VISIBLE else GONE
}
//</editor-fold>

//<editor-fold desc="Animation Stuff">

fun EditText.enableInput(enable:Boolean = true){
        this.isEnabled = enable
        this.inputType = EditorInfo.TYPE_CLASS_TEXT.takeIf { enable } ?: EditorInfo.TYPE_NULL
}

fun View.shake(duration: Long = 500L, p: Float) {
    performHapticFeedback(VIRTUAL_KEY)
    postDelayed({ this.performHapticFeedback(VIRTUAL_KEY) }, duration / 5)
    postDelayed({ this.performHapticFeedback(VIRTUAL_KEY) }, duration * 3 / 5)
    ObjectAnimator.ofFloat(this, View.TRANSLATION_X, 0f, p, -p, 0f, p, 0f).apply {
        interpolator = OvershootInterpolator()
        this.duration = duration
        start()
    }
}

operator fun View.times(endView: View) = MaterialContainerTransform().let {
    it.startView = this
    it.endView = endView
    it.addTarget(endView)
    it.setPathMotion(MaterialArcMotion())
    it.duration = 300
    it.scrimColor = Color.TRANSPARENT
    it
}
//</editor-fold>

fun <T> Fragment.observe(liveData: LiveData<T>, observer: (T) -> Unit) {
    liveData.observe(this.viewLifecycleOwner, {it?.also { observer(it)} })
}

fun <T> ComponentActivity.observe(liveData: LiveData<T>, observer: (T) -> Unit) {
    liveData.observe(this, { it?.also{ observer(it) } })
}

fun RtmClient.deleteAttr(channelId:String, key:String, callback: ResultCallback<Void>?){
    this.deleteChannelAttributesByKeys(channelId, mutableListOf(key), ChannelAttributeOptions(false), callback)
}

fun RtmClient.addOrUpdateAttr(channelId:String, key:String,value:String, callback: ResultCallback<Void>?){
    this.addOrUpdateChannelAttributes(channelId, mutableListOf(RtmChannelAttribute(key, value)), ChannelAttributeOptions(true), callback)
}

fun Fragment.permissionCheckBeforeOp(permissions: Array<String>, op:() -> Any, launcher: ActivityResultLauncher<Array<String>>){
    var allGranted = true
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        for (permission in permissions) {
            if (requireContext().checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED)
                allGranted = false
        }
        if (allGranted) op()
        else {
            launcher.launch(permissions)
//            var shouldShowReason = true
//            permissions.forEach {
//                if (!shouldShowRequestPermissionRationale(it))
//                    shouldShowReason = false
//            }
//            if (shouldShowReason)
//                launcher.launch(permissions)
//            else
//                "Permission is Permanently Banned❌\nGo Setting Page To See More".toast(requireContext())
        }
    }else{
        op()
    }
}