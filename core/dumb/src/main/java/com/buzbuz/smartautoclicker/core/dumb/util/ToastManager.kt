import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast

class ToastManager {
//    private var isToastShown = false
//    private var toast: Toast? = null
//
//    private val handler = Handler(Looper.getMainLooper())

    fun showToast(context: Context, message: String, duration: Int = Toast.LENGTH_SHORT) {
//        if (isToastShown) {
//            Log.d("TOAST", "TOAST IS SHOW, UPDATE MESSAGE :$message")
//            toast?.cancel()
//            toast?.setText(message)
//            toast?.show()
//        }
//        else {
//            isToastShown = true
//            Log.d("TOAST", "TOAST NOT SHOW, CREATE MESSAGE :$message")
//            toast?.cancel()
//            toast = Toast.makeText(context, message, duration).apply {
//                show()
//            }
//
//            // Reset the flag when the Toast finishes
//            val toastDuration = if (duration == Toast.LENGTH_SHORT) 2000 else 3500
//            handler.postDelayed({
//                isToastShown = false
//            }, toastDuration.toLong())
//        }
        Toast.makeText(context, message, duration).show()
    }

//    fun isToastCurrentlyShown(): Boolean = isToastShown
//
//    fun cancelToast() {
//        toast?.cancel()
//        isToastShown = false
//    }
}