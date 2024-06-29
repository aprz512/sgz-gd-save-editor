package com.aprz.gdsaveeditor

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.aprz.gdsaveeditor.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {


    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.editor -> {
                    binding.viewPager2.setCurrentItem(0, true)
                    true
                }

                R.id.settings -> {
                    binding.viewPager2.setCurrentItem(1, true)
                    true
                }

                else -> false
            }
        }

        binding.viewPager2.adapter = MainPagerAdapter(
            supportFragmentManager, lifecycle, arrayListOf(
                EditorListFragment(),
                SettingsFragment()
            )
        )

        binding.viewPager2.registerOnPageChangeCallback(object : OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                if (position == 0) {
                    binding.bottomNavigation.selectedItemId = R.id.editor
                } else if (position == 1) {
                    binding.bottomNavigation.selectedItemId = R.id.settings
                }
            }
        })

        DiySkills.main(arrayOf());
    }


}


class MainPagerAdapter(
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle,
    private val fragments: List<Fragment>
) :
    FragmentStateAdapter(fragmentManager, lifecycle) {


    override fun createFragment(position: Int): Fragment {
        return fragments[position]
    }

    override fun getItemCount(): Int {
        return fragments.size
    }

    companion object {
        private const val TAG = "MyAdapter"
    }
}

