package com.example.zhengzhiyong.officecrimes;

import java.util.Date;
import java.util.UUID;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;

public class CrimeFragment extends Fragment {
  public static final String EXTRA_CRIME_ID = "criminalintent.CRIME_ID";
  static final int REQUEST_DATE = 0;
  
  Crime mCrime;
  EditText mTitleEditText;
  Button mDateButton;
  CheckBox mSolvedCheckBox;

  public static CrimeFragment newInstance(UUID crimeId) {
    Bundle args = new Bundle();
    args.putSerializable(EXTRA_CRIME_ID, crimeId);

    CrimeFragment fragment = new CrimeFragment();
    fragment.setArguments(args);
    return fragment;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    UUID crimeId = (UUID) getArguments().getSerializable(EXTRA_CRIME_ID);
    mCrime = CrimeLab.getInstance(getActivity()).getCrime(crimeId);    
  }
  
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    View v = inflater.inflate(R.layout.fragment_crime, container, false);

    mTitleEditText = (EditText) v.findViewById(R.id.crime_title);
    mTitleEditText.setText(mCrime.getTitle());
    mTitleEditText.addTextChangedListener(new TextWatcher() {
        public void onTextChanged(CharSequence c, int start, int before, int count) {
          mCrime.setTitle(c.toString());
        }
        public void beforeTextChanged(CharSequence c, int start, int count, int after) {}
        public void afterTextChanged(Editable c) {}
      });

    mDateButton = (Button) v.findViewById(R.id.crime_date);
    mDateButton.setText(mCrime.getDate().toString());
    mDateButton.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
          FragmentManager fm = getActivity().getSupportFragmentManager();
          DatePickerFragment dialog = DatePickerFragment.newInstance(mCrime.getDate());
          dialog.setTargetFragment(CrimeFragment.this, REQUEST_DATE);
          dialog.show(fm, "date");
        }
      });

    mSolvedCheckBox = (CheckBox) v.findViewById(R.id.crime_solved);
    mSolvedCheckBox.setChecked(mCrime.getSolved());
    mSolvedCheckBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
          mCrime.setSolved(isChecked);
        }
      });
    
    // Inflate the layout for this fragment
    return v;
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (resultCode != Activity.RESULT_OK) return;
    if (requestCode == REQUEST_DATE) {
      Date date = (Date) data.getSerializableExtra(DatePickerFragment.EXTRA_DATE);
      mCrime.setDate(date);
      mDateButton.setText(date.toString());
    }
  }
}
