
package uz.oddiylik.app

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.BatteryManager
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : Activity() {

    private val black = Color.rgb(15, 15, 18)
    private val panel = Color.rgb(29, 28, 31)
    private val gold = Color.rgb(220, 177, 83)
    private val white = Color.WHITE
    private val muted = Color.rgb(180, 175, 165)

    private lateinit var root: LinearLayout
    private lateinit var battery: TextView
    private lateinit var locationText: TextView
    private lateinit var status: TextView
    private val requestCode = 41

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = black
        window.navigationBarColor = black
        showLogin()
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun rounded(color: Int, radius: Int): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
            setStroke(dp(1), gold)
        }
    }

    private fun makeText(
        value: String,
        size: Float,
        color: Int = white,
        bold: Boolean = false
    ): TextView {
        return TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            gravity = Gravity.CENTER
            if (bold) setTypeface(null, Typeface.BOLD)
            setPadding(dp(8), dp(10), dp(8), dp(10))
        }
    }

    private fun makeCard(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(12))
            background = rounded(panel, 16)
        }
    }

    private fun addCard(view: View) {
        root.addView(
            view,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(12)
            }
        )
    }

    private fun addButton(label: String, action: () -> Unit) {
        val button = Button(this).apply {
            text = label
            isAllCaps = false
            textSize = 15f
            setTextColor(black)
            typeface = Typeface.DEFAULT_BOLD
            background = rounded(gold, 14)
            setPadding(dp(12), dp(8), dp(12), dp(8))
            setOnClickListener { action() }
        }

        addCard(button)
    }

    private fun page(): LinearLayout {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(24), dp(22), dp(28))
            setBackgroundColor(black)
        }

        val scroll = ScrollView(this)
        scroll.addView(root)
        setContentView(scroll)
        return root
    }

    private fun addHeader(subtitle: String) {
        root.addView(
            makeText("ODDIYLIK", 32f, gold, true)
        )
        root.addView(
            makeText("ОИЛАВИЙ АЛОҚА ВА ХАВФСИЗЛИК", 12f, muted, true)
        )
        root.addView(
            makeText(subtitle, 15f, white)
        )
    }

    private fun showLogin() {
        page()
        addHeader("Хуш келибсиз")

        val card = makeCard()
        card.addView(makeText("ХАВФСИЗ КИРИШ", 18f, gold, true))
        card.addView(
            makeText("Шахсий PIN-кодингизни киритинг", 14f, muted)
        )

        val pin = EditText(this).apply {
            hint = "PIN-код"
            inputType = 2
            textSize = 20f
            gravity = Gravity.CENTER
            setTextColor(white)
            setHintTextColor(muted)
            background = rounded(black, 12)
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }

        card.addView(
            pin,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(58)
            ).apply {
                topMargin = dp(12)
            }
        )

        root.addView(card)

        addButton("КИРИШ") {
            when (pin.text.toString()) {
                "1111" -> showDashboard("Оила аъзоси")
                "2222" -> showDashboard("Бошқарув панели")
                else -> Toast.makeText(
                    this,
                    "PIN-код нотўғри",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        root.addView(
            makeText(
                "ODDIYLIK • ШАХСИЙ МАКОН • ИШОНЧ",
                11f,
                muted
            )
        )
    }

    private fun showDashboard(role: String) {        page()
        addHeader("Шахсий бошқарув панели")

        val profile = makeCard()
        profile.addView(makeText("СИЗНИНГ ПРОФИЛИНГИЗ", 13f, gold, true))
        profile.addView(makeText(role, 23f, white, true))
        profile.addView(makeText("Хуш келибсиз!", 14f, muted))
        addCard(profile)

        val batteryCard = makeCard()
        batteryCard.addView(makeText("🔋  БАТАРЕЯ ҲОЛАТИ", 15f, gold, true))
        battery = makeText("Батарея: ${batteryLevel()}%", 22f, white, true)
        batteryCard.addView(battery)
        addCard(batteryCard)

        val locationCard = makeCard()
        locationCard.addView(makeText("📍  ЖОЙЛАШУВ", 15f, gold, true))
        locationText = makeText(
            "Жойлашув ҳали олинмаган",
            14f,
            muted
        )
        locationCard.addView(locationText)
        addCard(locationCard)

        status = makeText("Ҳолат: тайёр", 13f, gold)
        addCard(status)

        addButton("📍  ЖОЙЛАШУВНИ ОЛИШ") {
            requestLocation()
        }

        addButton("🔋  БАТАРЕЯНИ ЯНГИЛАШ") {
            battery.text = "Батарея: ${batteryLevel()}%"
            status.text = "Ҳолат: янгиланди"
        }

        addButton("↩  ЧИҚИШ") {
            showLogin()
        }

        root.addView(
            makeText(
                "Жойлашув фақат сизнинг рухсатингиз билан олинади. Масофавий маълумот алмашиш ҳали қўшилмаган.",
                12f,
                muted
            )
        )
    }

    private fun batteryLevel(): Int {
        val manager =
            getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return manager.getIntProperty(
            BatteryManager.BATTERY_PROPERTY_CAPACITY
        ).coerceIn(0, 100)
    }

    private fun requestLocation() {
        val fine = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        val coarse = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (fine != PackageManager.PERMISSION_GRANTED &&
            coarse != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                requestCode
            )
            return
        }

        readLastLocation()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        if (requestCode == this.requestCode &&
            grantResults.any { it == PackageManager.PERMISSION_GRANTED }
        ) {
            readLastLocation()
        } else if (requestCode == this.requestCode) {
            locationText.text = "Жойлашув рухсати берилмади"
        }
    }

    @Suppress("MissingPermission")
    private fun readLastLocation() {
        try {
            val manager =
                getSystemService(Context.LOCATION_SERVICE) as LocationManager

            var best: Location? = null

            for (provider in manager.getProviders(true)) {
                val location =
                    manager.getLastKnownLocation(provider) ?: continue

                if (best == null || location.accuracy < best!!.accuracy) {
                    best = location
                }
            }

            locationText.text = if (best != null) {
                "Кенглик: ${best!!.latitude}\nУзунлик: ${best!!.longitude}"
            } else {
                "Жойлашув топилмади. GPS ни ёқиб қайта урининг."
            }

            status.text = "Ҳолат: жойлашув текширилди"

        } catch (e: Exception) {
            locationText.text = "Жойлашувни олиб бўлмади"
        }
    }
}

