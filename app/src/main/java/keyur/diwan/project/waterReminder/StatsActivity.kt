package keyur.diwan.project.waterReminder

// Import Build to check the Android API version (used for @RequiresApi annotation)
import android.os.Build
// Import Bundle to handle saved instance state in onCreate
import android.os.Bundle
// Import RequiresApi annotation to specify the minimum API level
import androidx.annotation.RequiresApi
// Import AppCompatActivity as the base class for this activity
import androidx.appcompat.app.AppCompatActivity
// Import LineChart for potential charting functionality (not currently used)
import com.github.mikephil.charting.charts.LineChart
// Import chart data classes (not currently used)
import com.github.mikephil.charting.data.*
// Import chart components (not currently used)
import com.github.mikephil.charting.components.*
// Import app resources (e.g., layouts, drawables)
import io.github.z3r0c00l_2k.aquadroid.R
// Import View Binding class for this activity, auto-generated from activity_stats.xml
import io.github.z3r0c00l_2k.aquadroid.databinding.ActivityStatsBinding
// Import WaveLoadingView to display the water intake progress visually
import me.itangqi.waveloadingview.WaveLoadingView

// StatsActivity displays the user’s water intake statistics, including a progress view
class StatsActivity : AppCompatActivity() {

    // View Binding instance to access UI elements (e.g., waterLevelView, btnBack) from activity_stats.xml
    private lateinit var binding: ActivityStatsBinding
    // WaveLoadingView instance to display the water intake progress as a wave animation
    private lateinit var waveLoadingView: WaveLoadingView

    // Requires API 23 (Android 6.0) or higher, specified by the @RequiresApi annotation
    @RequiresApi(Build.VERSION_CODES.M)
    // onCreate is called when the activity is first created
    override fun onCreate(savedInstanceState: Bundle?) {
        // Call the parent class’s onCreate method to perform default initialization
        super.onCreate(savedInstanceState)
        // Inflate the layout using View Binding (activity_stats.xml) and set it as the content view
        binding = ActivityStatsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set a click listener for the back button to finish the activity
        binding.btnBack.setOnClickListener {
            // Finish the activity and return to the previous screen (MainActivity.kt)
            finish()
        }

        // Initialize the WaveLoadingView to display the water intake progress
        waveLoadingView = findViewById(R.id.waterLevelView)

        // Retrieve the current water intake (in ml) passed from MainActivity.kt
        val intook = intent.getIntExtra("intook", 0)
        // Retrieve the target water intake (in ml) passed from MainActivity.kt, defaulting to 3000 ml
        val targetIntake = intent.getIntExtra("totalIntake", 3000)

        // Calculate the progress percentage (intook / targetIntake * 100), capped at 100%
        val progress = ((intook.toFloat() / targetIntake.toFloat()) * 100).toInt().coerceAtMost(100)
        // Set the progress value on the WaveLoadingView (updates the wave animation)
        waveLoadingView.progressValue = progress
        // Display the progress percentage as the center title of the WaveLoadingView
        waveLoadingView.centerTitle = "$progress%"

        // Update the remaining intake TextView (targetIntake - intook, minimum 0)
        binding.remainingIntake.text = "${(targetIntake - intook).coerceAtLeast(0)} ml"
        // Update the target intake TextView with the total intake goal
        binding.targetIntake.text = "$targetIntake ml"
    }
}