package exam.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ImageListAdapter(
    private val context: Context,
    private val onRemove: (Int) -> Unit
) : RecyclerView.Adapter<ImageListAdapter.ImageViewHolder>() {

    private val items: MutableList<Uri> = mutableListOf()

    fun setItems(newItems: List<Uri>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun getItems(): List<Uri> = items.toList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_image_row, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val uri = items[position]
        holder.txtName.text = resolveDisplayName(uri)
        holder.imgThumb.setImageBitmap(loadThumbnail(uri))
        holder.btnRemove.setOnClickListener { onRemove(position) }
    }

    override fun getItemCount(): Int = items.size

    private fun resolveDisplayName(uri: Uri): String {
        val projection = arrayOf(android.provider.MediaStore.MediaColumns.DISPLAY_NAME)
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(android.provider.MediaStore.MediaColumns.DISPLAY_NAME)
            if (idx >= 0 && cursor.moveToFirst()) {
                return cursor.getString(idx)
            }
        }
        return uri.lastPathSegment ?: "(unknown)"
    }

    private fun loadThumbnail(uri: Uri): Bitmap? {
        return context.contentResolver.openInputStream(uri)?.use { input ->
            val bitmap = BitmapFactory.decodeStream(input)
            if (bitmap != null) {
                Bitmap.createScaledBitmap(bitmap, 120, 120, true)
            } else {
                null
            }
        }
    }

    class ImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgThumb: ImageView = view.findViewById(R.id.imgThumb)
        val txtName: TextView = view.findViewById(R.id.txtName)
        val btnRemove: Button = view.findViewById(R.id.btnRemove)
    }
}
