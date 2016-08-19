package com.example.zhengzhiyong.officecrimes;

import java.util.List;
import java.util.UUID;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;

public class CrimePagerActivity extends FragmentActivity {
  ViewPager mViewPager;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    mViewPager = new ViewPager(this);
    mViewPager.setId(R.id.viewPager);
    setContentView(mViewPager);

    final List<Crime> crimes = CrimeLab.getInstance(this).getCrimes();
    
    FragmentManager fm = getSupportFragmentManager();
    mViewPager.setAdapter(new FragmentStatePagerAdapter(fm) {
        @Override
        public int getCount() {
          return crimes.size();
        }

        @Override
        public Fragment getItem(int pos) {
          UUID crimeId = crimes.get(pos).getId();
          return CrimeFragment.newInstance(crimeId);
        }
      });

    mViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
        public void onPageScrollStateChanged(int state) { }
        public void onPageScrolled(int pos, float posOffset, int posOffsetPixels) { }
        public void onPageSelected(int pos) {
          Crime crime = crimes.get(pos);
          if (crime.getTitle() != null) {
            setTitle(crime.getTitle());
          }
        }
      });

    UUID crimeId = (UUID) getIntent().getSerializableExtra(CrimeFragment.EXTRA_CRIME_ID);
    for (int i = 0; i < crimes.size(); ++i) {
      if (crimes.get(i).getId().equals(crimeId)) {
        mViewPager.setCurrentItem(i);
        break;
      }
    }
  }
}
