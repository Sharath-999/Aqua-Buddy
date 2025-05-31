package keyur.diwan.project.waterReminder

// Import Intent to launch InitUserInfoActivity when the user clicks "Get Started"
import android.content.Intent
// Import Build to check the Android API version for status bar customization
import android.os.Build
// Import Bundle to handle saved instance state in onCreate
import android.os.Bundle
// Import LayoutInflater to inflate fragment layouts
import android.view.LayoutInflater
// Import View and ViewGroup for fragment view creation
import android.view.View
import android.view.ViewGroup
// Import AppCompatActivity as the base class for this activity
import androidx.appcompat.app.AppCompatActivity
// Import Fragment to create the walkthrough screens
import androidx.fragment.app.Fragment
// Import FragmentManager to manage fragments in the ViewPager
import androidx.fragment.app.FragmentManager
// Import FragmentPagerAdapter to populate the ViewPager with fragments
import androidx.fragment.app.FragmentPagerAdapter
// Import View Binding classes for the activity and fragments, auto-generated from layouts
import io.github.z3r0c00l_2k.aquadroid.databinding.ActivityWalkThroughBinding
import io.github.z3r0c00l_2k.aquadroid.databinding.WalkThroughOneBinding
import io.github.z3r0c00l_2k.aquadroid.databinding.WalkThroughThreeBinding
import io.github.z3r0c00l_2k.aquadroid.databinding.WalkThroughTwoBinding

// WalkThroughActivity is the onboarding screen shown on the app’s first run
class WalkThroughActivity : AppCompatActivity() {

    // View Binding instance to access UI elements (e.g., walkThroughPager) from activity_walk_through.xml
    private lateinit var binding: ActivityWalkThroughBinding
    // Adapter for the ViewPager to display the walkthrough fragments
    private var viewPagerAdapter: WalkThroughAdapter? = null

    // onCreate is called when the activity is first created
    override fun onCreate(savedInstanceState: Bundle?) {
        // Call the parent class’s onCreate method to perform default initialization
        super.onCreate(savedInstanceState)

        // Check if the device is running Android 6.0 (API 23) or later
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Set the status bar to light mode (black icons on a light background)
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }

        // Inflate the layout using View Binding (activity_walk_through.xml) and set it as the content view
        binding = ActivityWalkThroughBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize the ViewPager adapter with the FragmentManager
        viewPagerAdapter = WalkThroughAdapter(supportFragmentManager)
        // Set the adapter on the ViewPager to display the walkthrough fragments
        binding.walkThroughPager.adapter = viewPagerAdapter
        // Connect the ViewPager to the CircleIndicator for pagination dots
        binding.indicator.setViewPager(binding.walkThroughPager)
    }

    // onStart is called when the activity becomes visible to the user
    override fun onStart() {
        // Call the parent class’s onStart method to perform default behavior
        super.onStart()
        // Set a click listener for the "Get Started" button
        binding.getStarted.setOnClickListener {
            // Launch InitUserInfoActivity to collect user information
            startActivity(Intent(this, InitUserInfoActivity::class.java))
            // Finish this activity to prevent the user from returning to it
            finish()
        }
    }

    // Inner class WalkThroughAdapter manages the fragments in the ViewPager
    private inner class WalkThroughAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {
        // Returns the total number of fragments (3 walkthrough screens)
        override fun getCount(): Int = 3

        // Returns the fragment for the given position in the ViewPager
        override fun getItem(i: Int): Fragment {
            // Return the appropriate fragment based on the position
            return when (i) {
                0 -> WalkThroughOne()  // First screen
                1 -> WalkThroughTwo()  // Second screen
                2 -> WalkThroughThree() // Third screen
                else -> WalkThroughOne() // Default to first screen if position is invalid
            }
        }
    }

    // WalkThroughOne is the first fragment in the walkthrough sequence
    class WalkThroughOne : Fragment() {
        // Nullable View Binding instance for the fragment, auto-generated from walk_through_one.xml
        private var _binding: WalkThroughOneBinding? = null
        // Non-null accessor for the binding, throws an exception if accessed after onDestroyView
        private val binding get() = _binding!!

        // onCreateView is called to create the fragment’s view
        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
            // Inflate the layout for the fragment using View Binding (walk_through_one.xml)
            _binding = WalkThroughOneBinding.inflate(inflater, container, false)
            // Return the root view of the fragment
            return binding.root
        }

        // onDestroyView is called when the fragment’s view is destroyed
        override fun onDestroyView() {
            // Call the parent class’s onDestroyView method
            super.onDestroyView()
            // Set the binding to null to prevent memory leaks
            _binding = null
        }
    }

    // WalkThroughTwo is the second fragment in the walkthrough sequence
    class WalkThroughTwo : Fragment() {
        // Nullable View Binding instance for the fragment, auto-generated from walk_through_two.xml
        private var _binding: WalkThroughTwoBinding? = null
        // Non-null accessor for the binding, throws an exception if accessed after onDestroyView
        private val binding get() = _binding!!

        // onCreateView is called to create the fragment’s view
        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
            // Inflate the layout for the fragment using View Binding (walk_through_two.xml)
            _binding = WalkThroughTwoBinding.inflate(inflater, container, false)
            // Return the root view of the fragment
            return binding.root
        }

        // onDestroyView is called when the fragment’s view is destroyed
        override fun onDestroyView() {
            // Call the parent class’s onDestroyView method
            super.onDestroyView()
            // Set the binding to null to prevent memory leaks
            _binding = null
        }
    }

    // WalkThroughThree is the third fragment in the walkthrough sequence
    class WalkThroughThree : Fragment() {
        // Nullable View Binding instance for the fragment, auto-generated from walk_through_three.xml
        private var _binding: WalkThroughThreeBinding? = null
        // Non-null accessor for the binding, throws an exception if accessed after onDestroyView
        private val binding get() = _binding!!

        // onCreateView is called to create the fragment’s view
        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
            // Inflate the layout for the fragment using View Binding (walk_through_three.xml)
            _binding = WalkThroughThreeBinding.inflate(inflater, container, false)
            // Return the root view of the fragment
            return binding.root
        }

        // onDestroyView is called when the fragment’s view is destroyed
        override fun onDestroyView() {
            // Call the parent class’s onDestroyView method
            super.onDestroyView()
            // Set the binding to null to prevent memory leaks
            _binding = null
        }
    }
}