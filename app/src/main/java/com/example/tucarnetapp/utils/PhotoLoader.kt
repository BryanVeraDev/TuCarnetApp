import android.content.Context
import android.graphics.drawable.Drawable
import android.util.Log
import android.widget.ImageView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.signature.ObjectKey
import com.example.tucarnetapp.R
import com.example.tucarnetapp.data.repository.LivenessRepository
import kotlinx.coroutines.launch

object PhotoLoader {

    fun load(
        context: Context,
        imageView: ImageView,
        photoKey: String?,
        onFinished: (() -> Unit)? = null
    ) {
        if (photoKey.isNullOrBlank()) {
            imageView.setImageResource(R.drawable.no_image)
            onFinished?.invoke()
            return
        }

        val lifecycleOwner = context as? LifecycleOwner ?: return

        lifecycleOwner.lifecycleScope.launch {
            try {
                val response = LivenessRepository().getPhotoUrl(photoKey)
                val url = response.url

                Glide.with(context)
                    .load(url)
                    .signature(ObjectKey(photoKey))
                    .placeholder(R.drawable.no_image)
                    .error(R.drawable.no_image)
                    .listener(object :
                        com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable> {

                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: Target<Drawable>,
                            isFirstResource: Boolean
                        ): Boolean {
                            onFinished?.invoke()
                            return false
                        }

                        override fun onResourceReady(
                            resource: Drawable,
                            model: Any,
                            target: Target<Drawable>,
                            dataSource: DataSource,
                            isFirstResource: Boolean
                        ): Boolean {
                            onFinished?.invoke()
                            return false
                        }
                    })
                    .into(imageView)

            } catch (e: Exception) {
                imageView.setImageResource(R.drawable.no_image)
                onFinished?.invoke()
            }
        }
    }
}

