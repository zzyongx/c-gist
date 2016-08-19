package com.example.zhengzhiyong.officecrimes;

import java.util.List;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

public class CrimeListFragment extends ListFragment {
  private static final String TAG = "CrimeListFragment";
  private List<Crime> mCrimes;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    getActivity().setTitle(R.string.crimes_title);
    mCrimes = CrimeLab.getInstance(getActivity()).getCrimes();

    CrimeAdapter adapter = new CrimeAdapter(mCrimes);
    setListAdapter(adapter);
    Log.i(TAG, "onCreate");
  }

  @Override
  public void onListItemClick(ListView l, View v, int position, long id) {
    Log.i(TAG, "click on " + String.valueOf(position));
    Crime c = ((CrimeAdapter) getListAdapter()).getItem(position);

    Intent i = new Intent(getActivity(), CrimePagerActivity.class);
    i.putExtra(CrimeFragment.EXTRA_CRIME_ID, c.getId());
    startActivityForResult(i, 0);
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent date) {
    ((CrimeAdapter) getListAdapter()).notifyDataSetChanged();
  }

  class CrimeAdapter extends ArrayAdapter<Crime> {
    public CrimeAdapter(List<Crime> crimes) {
      super(getActivity(), 0, crimes);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      if (convertView == null) {
        convertView = getActivity().getLayoutInflater().inflate(R.layout.list_item_crime, null);
      }

      Crime crime = getItem(position);
      TextView titleTextView = (TextView) convertView.findViewById(R.id.crime_list_item_title);
      titleTextView.setText(crime.getTitle());

      TextView dateTextView = (TextView) convertView.findViewById(R.id.crime_list_item_date);
      dateTextView.setText(crime.getDate().toString());

      CheckBox solvedCheckBox = (CheckBox) convertView.findViewById(R.id.crime_list_item_solved);
      solvedCheckBox.setChecked(crime.getSolved());
      
      return convertView;
    }
  }
}
