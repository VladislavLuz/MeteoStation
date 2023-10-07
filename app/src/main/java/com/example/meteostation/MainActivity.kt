package com.example.meteostation


import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.transition.Visibility
import com.example.meteostation.constances.MA_const
import com.example.meteostation.databinding.ActivityMainBinding
import com.example.meteostation.models.meteoData
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.math.roundToInt


class MainActivity : AppCompatActivity() {
    lateinit var binding : ActivityMainBinding
    private lateinit var request : Request
    private var client = OkHttpClient()
    private var requestHour: Int =0
    private var lightData:Double = 0.0
    private lateinit var pref : SharedPreferences


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        pref = getSharedPreferences("ipInfo", MODE_PRIVATE)
        syncOnClick()
        getIP()
        saveIP_click()
    }

    override fun onResume() {
        super.onResume()
        if (binding.ipEDT.text.isNotBlank() && binding.ipEDT.text != null){
            post()
        }else{
            binding.syncButton.setImageResource(R.drawable.ic_baseline_warning_amber_24)
        }
    }

    /*fun getIpInt(view: View){
        var intent = Intent(this,SecondActivity::class.java)
        startActivityForResult(intent,MA_const.REQUEST_CODE_GETIP)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == MA_const.REQUEST_CODE_GETIP){
            ip = data?.getStringExtra(MA_const.IP).toString()
            binding.cityTV.text = ip
        }

    }*/
    fun getSetting(view: View){
        if (!binding.ipEDT.isVisible && !binding.ipSaveButton.isVisible){
            binding.ipEDT.visibility = View.VISIBLE
            binding.ipSaveButton.visibility = View.VISIBLE
        }else{
            binding.ipEDT.visibility = View.GONE
            binding.ipSaveButton.visibility = View.GONE
            binding.ipSaveButton.setImageResource(R.drawable.ic_checkip)
        }
    }

    private fun getIP(){
        var ip = pref.getString("ip","")
        if (ip != null && ip.isNotBlank()) binding.ipEDT.setText(ip)
    }

    private fun saveIP(ip:String){
        var editor = pref.edit()
        editor.putString("ip",ip)
        editor.apply()
    }

    private fun saveIP_click(){
        binding.ipSaveButton.setOnClickListener(){
            if (binding.ipEDT.text.isNotBlank() && binding.ipEDT.text != null){
                saveIP(binding.ipEDT.text.toString())
                binding.syncButton.setImageResource(R.drawable.ic_sync)
                binding.ipSaveButton.setImageResource(R.drawable.ic_checkip_success)
            }
        }
    }


    private fun post(){
            Thread{
                runOnUiThread {binding.syncButton.isEnabled = false}
                request = Request.Builder().url("http://${binding.ipEDT.text.trim()}:10285/").build()
                try {
                    val response = client.newCall(request).execute()
                    if(response.isSuccessful){
                        val resultText = response.body()?.string().toString()
                        val gson = Gson()
                        val parsString = gson.fromJson(resultText, meteoData::class.java)
                        runOnUiThread {
                            /*for Android v8 and greater(v26 API)
                            val dateTime = LocalDateTime.now()
                                .format(DateTimeFormatter.ofPattern("dd MMM yyyy | hh:mm"))*/
                            val formatter = SimpleDateFormat("EEEE, dd MMMM yyyy | HH:mm", Locale.getDefault())
                            val requestDate = formatter.format(Date()).replaceFirstChar { it.uppercase() }
                            val formatterH = SimpleDateFormat("HH", Locale.getDefault())
                            requestHour = formatterH.format(Date()).toInt()

                            lightData = parsString.light
                            val temperData = "${parsString.temperature.roundToInt()}°"
                            val humidityData = "${parsString.humidity.roundToInt()}%"
                            val pressureData = "${parsString.pressure.roundToInt()}"
                            val altitudeData = "${parsString.altitude.roundToInt()}"


                            binding.tempValueTV.text = temperData
                            binding.pressureValueTV.text = pressureData
                            binding.humidityValueTV.text = humidityData
                            binding.altitudeValueTV.text = altitudeData
                            binding.DateInfoTV.text = requestDate
                            changeWeatherIcon()
                        }
                    }
                } catch (i: IOException){
                    Log.d("MyLog",i.toString())
                    runOnUiThread {
                        makeToast()
                    }
                }
                runOnUiThread {binding.syncButton.isEnabled = true}
            }.start()

    }


    fun syncOnClick(){
        binding.syncButton.setOnClickListener(){
            if (binding.ipEDT.text.isNotBlank() && binding.ipEDT.text != null){
                post()
            }else{
                binding.syncButton.setImageResource(R.drawable.ic_baseline_warning_amber_24)
                makeToast()
            }
        }
    }

    fun changeWeatherIcon(){
        if(requestHour>=22 || requestHour<=4 || lightData<=15){
            binding.weatherImg.setImageResource(R.drawable.weather_icon_sunny_night)
            binding.textWeather.text = getText(R.string.weather_sunny)
        }else if(requestHour in 6..19 && lightData<= 70){
            binding.weatherImg.setImageResource(R.drawable.weather_icon_rain_day)
            binding.textWeather.text = getText(R.string.weather_rain)
        }else if(requestHour>=4 && lightData<= 250){
            binding.weatherImg.setImageResource(R.drawable.weather_icon_cloudy)
            binding.textWeather.text = getText(R.string.weather_cloudy)
        }else{
            binding.weatherImg.setImageResource(R.drawable.weather_icon_sunny)
            binding.textWeather.text = getText(R.string.weather_sunny)
        }

    }

    // Declare the onBackPressed method when the back button is pressed this method will call
    fun makeToast() {
        // Create the object of AlertDialog Builder class
        val builder = AlertDialog.Builder(this)

        // Set the message show for the Alert time
        builder.setMessage(getText(R.string.toast_message))

        // Set Alert Title
        builder.setTitle(getText(R.string.toast_title))

        // Set Cancelable false for when the user clicks on the outside the Dialog Box then it will remain show
        builder.setCancelable(false)

        // Set the positive button with yes name Lambda OnClickListener method is use of DialogInterface interface.
        builder.setPositiveButton(getText(R.string.close)) {
            // When the user click yes button then app will close
                dialog, which -> dialog.cancel()
        }

        // Create the Alert dialog
        val alertDialog = builder.create()
        // Show the Alert Dialog box
        alertDialog.show()
    }


}
