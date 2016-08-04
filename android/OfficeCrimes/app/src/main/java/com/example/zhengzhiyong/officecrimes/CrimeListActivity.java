package com.example.zhengzhiyong.officecrimes;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.os.Bundle;

public class CrimeListActivity extends SingleFragmentActivity {
  @Override
  protected Fragment createFragment() {
    return new CrimeListFragment();
  }
}
