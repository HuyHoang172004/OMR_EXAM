package exam.app

import android.content.Context
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

object ModelLoader {
    fun loadMappedFile(context: Context, assetPath: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(assetPath)
        FileInputStream(fileDescriptor.fileDescriptor).use { input ->
            val channel = input.channel
            val startOffset = fileDescriptor.startOffset
            val declaredLength = fileDescriptor.declaredLength
            return channel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
        }
    }
}
