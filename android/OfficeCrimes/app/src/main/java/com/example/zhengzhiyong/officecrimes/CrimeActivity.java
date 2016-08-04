package com.example.zhengzhiyong.officecrimes;

import java.util.UUID;
import android.support.v4.app.Fragment;

public class CrimeActivity extends SingleFragmentActivity {
  @Override
  protected Fragment createFragment() {
    UUID crimeId = (UUID) getIntent().getSerializableExtra(CrimeFragment.EXTRA_CRIME_ID);
    return CrimeFragment.newInstance(crimeId);
  }
}
