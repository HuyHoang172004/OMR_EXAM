package exam.app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnGoSingle = findViewById<Button>(R.id.btnGoSingle)
        val btnGoMulti = findViewById<Button>(R.id.btnGoMulti)

        btnGoSingle.setOnClickListener {
            startActivity(Intent(this, SingleActivity::class.java))
        }

        btnGoMulti.setOnClickListener {
            startActivity(Intent(this, MultiActivity::class.java))
        }
    }
}
