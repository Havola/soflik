package uz.oddiylik.app

import android.Manifest
import android.app.Activity
import android.content.*
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.media.MediaRecorder
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Bundle
import android.util.Log
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.provider.MediaStore
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File

class MainActivity : Activity() {

    private val black = Color.rgb(13, 13, 16)
    private val panel = Color.rgb(30, 29, 33)
    private val gold = Color.rgb(224, 183, 91)
    private val white = Color.WHITE
    private val muted = Color.rgb(180, 175, 165)

    private lateinit var root: LinearLayout
    private lateinit var status: TextView
    private lateinit var locationText: TextView
    private lateinit var batteryText: TextView
    private lateinit var sharedTextView: TextView

    private val prefs by lazy {
        getSharedPreferences("oddiylik_settings", MODE_PRIVATE)
    }

    private var recorder: MediaRecorder? = null
    private var recording = false
    private var recordFile: File? = null
    private var pendingMic = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val oldHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            try {
                File(filesDir, "oddiylik_crash.txt").writeText(
                    Log.getStackTraceString(error)
                )
            } catch (_: Exception) {}
            oldHandler?.uncaughtException(thread, error)
        }

        window.statusBarColor = black
        window.navigationBarColor = black
        showLogin()

        val crashFile = File(filesDir, "oddiylik_crash.txt")
        if (crashFile.exists()) {
            val crash = crashFile.readText()
            crashFile.delete()
            window.decorView.post {
                android.app.AlertDialog.Builder(this)
                    .setTitle("ODDIYLIK — ХАТО")
                    .setMessage(crash.take(3500))
                    .setPositiveButton("ЁПИШ", null)
                    .show()
            }
        }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun rounded(color: Int, radius: Int): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
            setStroke(dp(1), gold)
        }

    private fun makeText(
        value: String,
        size: Float,
        color: Int = white,
        bold: Boolean = false
    ) = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(color)
        gravity = Gravity.CENTER
        if (bold) setTypeface(null, Typeface.BOLD)
        setPadding(dp(8), dp(9), dp(8), dp(9))
    }

    private fun card(): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = rounded(panel, 16)
        }

    private fun addCard(v: View) {
        root.addView(v, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(12) })
    }

    private fun button(label: String, action: () -> Unit) {
        val b = Button(this).apply {
            text = label
            isAllCaps = false
            textSize = 15f
            setTextColor(black)
            typeface = Typeface.DEFAULT_BOLD
            background = rounded(gold, 14)
            setOnClickListener { action() }
        }
        addCard(b)
    }

    private fun page() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(28))
            setBackgroundColor(black)
        }
        val scroll = ScrollView(this)
        scroll.addView(root)
        setContentView(scroll)
    }

    private fun header(subtitle: String) {
        root.addView(makeText("ODDIYLIK", 31f, gold, true))
        root.addView(makeText("ОИЛАВИЙ АЛОҚА ВА ХАВФСИЗЛИК", 11f, muted, true))
        root.addView(makeText(subtitle, 15f))
    }

    private fun showLogin() {
        page()
        header("Хуш келибсиз")

        val c = card()
        c.addView(makeText("ХАВФСИЗ КИРИШ", 18f, gold, true))
        c.addView(makeText("Шахсий PIN-кодингизни киритинг", 14f, muted))

        val pin = EditText(this).apply {
            hint = "PIN-код"
            inputType = 2
            textSize = 20f
            gravity = Gravity.CENTER
            setTextColor(white)
            setHintTextColor(muted)
            background = rounded(black, 12)
        }
        c.addView(pin, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(55)
        ))
        addCard(c)

        button("КИРИШ") {
            when (pin.text.toString()) {
                "1111" -> showDashboard("Оила аъзоси")
                "2222" -> showDashboard("Бошқарув панели")
                else -> Toast.makeText(this, "PIN-код нотўғри", Toast.LENGTH_SHORT).show()
            }
        }
        root.addView(makeText("ODDIYLIK • ИШОНЧ • ХАВФСИЗЛИК", 11f, muted))
    }

    private fun showDashboard(role: String) {
        page()
        header("Шахсий бошқарув панели")

        val profile = card()
        profile.addView(makeText("СИЗНИНГ ПРОФИЛИНГИЗ", 13f, gold, true))
        profile.addView(makeText(role, 22f, white, true))
        profile.addView(makeText("Хуш келибсиз!", 13f, muted))
        addCard(profile)

        val info = card()
        info.addView(makeText("ҚУРИЛМА ҲОЛАТИ", 15f, gold, true))
        batteryText = makeText("Батарея: ${batteryLevel()}%", 18f, white, true)
        info.addView(batteryText)
        info.addView(makeText(
            "Интернет: ${if (isOnline()) "Уланган" else "Уланмаган"}",
            14f, muted
        ))
        addCard(info)

        val loc = card()
        loc.addView(makeText("ЖОЙЛАШУВ", 15f, gold, true))
        locationText = makeText("Жойлашув ҳали олинмаган", 13f, muted)
        loc.addView(locationText)
        addCard(loc)

        status = makeText("Ҳолат: тайёр", 13f, gold)
        addCard(status)

        val consent = card()
        consent.addView(makeText("РУХСАТ ВА МАХФИЙЛИК", 16f, gold, true))
        consent.addView(makeText(
            "Ҳар бир функцияни ўзингиз алоҳида ёқасиз ёки ўчирасиз.",
            12f, muted
        ))
        addSwitch(consent, "Жойлашувдан фойдаланиш", "location")
        addSwitch(consent, "Камерани ишлатиш", "camera")
        addSwitch(consent, "Микрофондан фойдаланиш", "microphone")
        addCard(consent)

        button("📍  ЖОЙЛАШУВНИ ОЛИШ") {
            if (allowed("location")) requestLocation()
            else message("Аввал жойлашув рухсатини ёқинг")
        }

        button("📷  КАМЕРАНИ ОЧИШ") {
            if (allowed("camera")) {
                try {
                    startActivity(Intent(MediaStore.ACTION_IMAGE_CAPTURE))
                } catch (e: Exception) {
                    message("Камера очилмади")
                }
            } else message("Аввал камера рухсатини ёқинг")
        }

        button(if (recording) "⏹  ЁЗИШНИ ТЎХТАТИШ" else "🎙  ОВОЗ ЁЗИШНИ БОШЛАШ") {
            if (recording) stopRecording()
            else if (allowed("microphone")) requestMic()
            else message("Аввал микрофон рухсатини ёқинг")
        }

        button("☎  ҚЎНҒИРОҚ ҚИЛИШ") {
            startActivity(Intent(Intent.ACTION_DIAL))
        }

        button("✉  TELEGRAM МАЪЛУМОТИНИ УЛАШИШ") {
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "ODDIYLIK орқали ихтиёрий улашиш")
            }
            startActivity(Intent.createChooser(send, "Улашиш учун иловани танланг"))
        }

        val shared = card()
        shared.addView(makeText("УЛАШИЛГАН МАТН", 14f, gold, true))
        sharedTextView = makeText("Ҳозирча маълумот йўқ", 12f, muted)
        shared.addView(sharedTextView)
        addCard(shared)
        receiveSharedText(intent)

        button("🔄  ҲОЛАТНИ ЯНГИЛАШ") {
            batteryText.text = "Батарея: ${batteryLevel()}%"
            status.text = "Интернет: ${if (isOnline()) "Уланган" else "Уланмаган"}"
        }

        button("⚙  РУХСАТЛАРНИ БОШҚАРИШ") {
            startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = android.net.Uri.parse("package:$packageName")
            })
        }

        button("↩  ЧИҚИШ") { showLogin() }

        root.addView(makeText(
            "Эслатма: бу версияда маълумотлар телефоннинг ўзида сақланади. Икки телефон ўртасида масофавий синхронлаш ҳали уланмаган.",
            11f, muted
        ))
    }

    private fun addSwitch(parent: LinearLayout, label: String, key: String) {
        val sw = CheckBox(this).apply {
            text = label
            textSize = 14f
            setTextColor(white)
            buttonTintList = android.content.res.ColorStateList.valueOf(gold)
            isChecked = prefs.getBoolean(key, false)
            setOnCheckedChangeListener { _, checked ->
                prefs.edit().putBoolean(key, checked).apply()
            }
        }
        parent.addView(sw)
    }

    private fun allowed(key: String) = prefs.getBoolean(key, false)

    private fun message(value: String) {
        Toast.makeText(this, value, Toast.LENGTH_SHORT).show()
    }

    private fun batteryLevel(): Int {
        val bm = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY).coerceIn(0, 100)
    }

    private fun isOnline(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun requestLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ), 41)
        } else readLastLocation()
    }

    private fun readLastLocation() {
        try {
            val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            var best: Location? = null
            for (provider in lm.getProviders(true)) {
                val item = lm.getLastKnownLocation(provider) ?: continue
                if (best == null || item.accuracy < best!!.accuracy) best = item
            }
            locationText.text = if (best != null)
                "Кенглик: ${best!!.latitude}\nУзунлик: ${best!!.longitude}"
            else "Жойлашув топилмади. GPS ни текширинг."
            status.text = "Жойлашув текширилди"
        } catch (e: Exception) {
            locationText.text = "Жойлашувни олиб бўлмади"
        }
    }

    private fun requestMic() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            pendingMic = true
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.RECORD_AUDIO), 42)
        } else startRecording()
    }

    private fun startRecording() {
        try {
            recordFile = File(cacheDir, "oddiylik_voice_${System.currentTimeMillis()}.m4a")
            val r = MediaRecorder()
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setOutputFile(recordFile!!.absolutePath)
            r.prepare()
            r.start()
            recorder = r
            recording = true
            status.text = "Овоз ёзилмоқда — тўхтатиш тугмасини босинг"
            showDashboardAfterRecording()
        } catch (e: Exception) {
            recording = false
            recorder?.release()
            recorder = null
            message("Овоз ёзишни бошлаб бўлмади")
        }
    }

    private fun showDashboardAfterRecording() {
        // Экрандаги тугмани янгилаш учун панелни қайта чизамиз.
        // Ёзиш жараёни экран очиқ турган пайтда давом этади.
        val currentRole = "Оила аъзоси"
        showDashboard(currentRole)
        status.text = "Овоз ёзилмоқда — тўхтатиш тугмасини босинг"
    }

    private fun stopRecording() {
        try {
            recorder?.stop()
            message("Овоз ёзуви телефонда вақтинча сақланди")
        } catch (e: Exception) {
            message("Ёзув сақланмади ёки жуда қисқа бўлди")
        } finally {
            recorder?.release()
            recorder = null
            recording = false
            showDashboard("Оила аъзоси")
        }
    }

    private fun receiveSharedText(i: Intent?) {
        if (i?.action == Intent.ACTION_SEND && i.type == "text/plain") {
            val value = i.getStringExtra(Intent.EXTRA_TEXT)
            if (!value.isNullOrBlank()) sharedTextView.text = value
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (::sharedTextView.isInitialized) receiveSharedText(intent)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val granted = grantResults.any { it == PackageManager.PERMISSION_GRANTED }
        if (requestCode == 41) {
            if (granted) readLastLocation()
            else locationText.text = "Жойлашув рухсати берилмади"
        }
        if (requestCode == 42) {
            pendingMic = false
            if (granted) startRecording()
            else message("Микрофон рухсати берилмади")
        }
    }

    override fun onDestroy() {
        if (recording) {
            try { recorder?.stop() } catch (_: Exception) {}
            recorder?.release()
            recorder = null
        }
        super.onDestroy()
    }
}
