package uz.oddiylik.app

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.BatteryManager
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : Activity() {
    private lateinit var root: LinearLayout
    private lateinit var status: TextView
    private lateinit var battery: TextView
    private lateinit var locationText: TextView
    private val requestCode = 41

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showLogin()
    }

    private fun baseLayout(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 36, 28, 28)
            setBackgroundColor(Color.rgb(12, 24, 43))
            gravity = Gravity.CENTER_HORIZONTAL
        }
    }

    private fun title(text: String, size: Float): TextView =
        TextView(this).apply {
            this.text = text
            textSize = size
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(8, 16, 8, 16)
        }

    private fun addButton(label: String, action: () -> Unit) {
        val b = Button(this).apply {
            text = label
            isAllCaps = false
            setOnClickListener { action() }
        }
        root.addView(b, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = 12 })
    }

    private fun showLogin() {
        root = baseLayout()
        root.addView(title("ODDIYLIK", 32f))
        root.addView(title("Оилавий алоқа ва хавфсизлик", 16f))
        val hint = title("Ролингизни танланг ва шахсий PIN киритинг", 14f)
        root.addView(hint)
        val pin = EditText(this).apply {
            this.hint = "PIN (демо: 1111 ёки 2222)"
            inputType = 2
            setTextColor(Color.WHITE)
            setHintTextColor(Color.LTGRAY)
        }
        root.addView(pin, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ))
        addButton("Кириш") {
            when (pin.text.toString()) {
                "1111" -> showDashboard("Оила аъзоси")
                "2222" -> showDashboard("Бошқарув панели")
                else -> Toast.makeText(this, "PIN нотўғри", Toast.LENGTH_SHORT).show()
            }
        }
        root.addView(title("Эслатма: бу биринчи демо версия. PIN ҳозирча намунавий.", 12f))
        setContentView(root)
    }

    private fun showDashboard(role: String) {
        root = baseLayout()
        root.addView(title("ODDIYLIK", 30f))
        root.addView(title("Роль: $role", 18f))
        status = title("Ҳолат: тайёр", 15f)
        battery = title("Батарея: ${batteryLevel()}%", 15f)
        locationText = title("Жойлашув ҳали олинмаган", 14f)
        root.addView(status)
        root.addView(battery)
        root.addView(locationText)
        addButton("Жойлашувни менинг розилигим билан олиш") { requestLocation() }
        addButton("Батарея ҳолатини янгилаш") {
            battery.text = "Батарея: ${batteryLevel()}%"
        }
        addButton("Чиқиш") { showLogin() }
        root.addView(title(
            "Бу демо версия жойлашувни фақат тугма босилганда ва Android рухсати берилганда ўқийди. Масофавий улашиш сервери ҳали қўшилмаган.",
            12f
        ))
        setContentView(root)
    }

    private fun batteryLevel(): Int {
        val bm = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY).coerceIn(0, 100)
    }

    private fun requestLocation() {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (fine != PackageManager.PERMISSION_GRANTED && coarse != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                requestCode
            )
            return
        }
        readLastLocation()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == this.requestCode && grantResults.any { it == PackageManager.PERMISSION_GRANTED }) {
            readLastLocation()
        } else if (requestCode == this.requestCode) {
            locationText.text = "Жойлашув рухсати берилмади"
        }
    }

    @Suppress("MissingPermission")
    private fun readLastLocation() {
        try {
            val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val providers = lm.getProviders(true)
            var best: Location? = null
            for (p in providers) {
                val loc = lm.getLastKnownLocation(p) ?: continue
                if (best == null || loc.accuracy < best!!.accuracy) best = loc
            }
            locationText.text = if (best != null) {
                "Кенглик: ${best!!.latitude}\nУзунлик: ${best!!.longitude}"
            } else {
                "Ҳозирча жойлашув топилмади. GPS ни ёқиб қайта урининг."
            }
        } catch (e: Exception) {
            locationText.text = "Жойлашувни олиб бўлмади: ${e.localizedMessage}"
        }
    }
}
