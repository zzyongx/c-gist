package com.example.zhengzhiyong.cheatactivity;

import android.support.v7.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class CheatActivity extends AppCompatActivity {
  static final String TAG = "CheatActivity";
  static final String EXTRA_ANSWER_IS_TRUE = "tfquiz.ANSWER_IS_TRUE";
  static final String EXTRA_ANSWER_SHOWN = "tfquiz.ANSWER_SHOWN";
  
  Button mShowAnswerButton;
  TextView mAnswerTextView;

  boolean mAnswerIsTrue;

  void setAnswerShownResult(boolean isAnswerShown) {
    Intent i = new Intent();
    i.putExtra(EXTRA_ANSWER_SHOWN, isAnswerShown);
    setResult(RESULT_OK, i);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Log.d(TAG, "onCreate()");
    setContentView(R.layout.activity_cheat);

    mAnswerIsTrue = getIntent().getBooleanExtra(EXTRA_ANSWER_IS_TRUE, false);
    
    mAnswerTextView = (TextView) findViewById(R.id.answer_text_view);

    mShowAnswerButton = (Button) findViewById(R.id.show_answer_button);
    mShowAnswerButton.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          mAnswerTextView.setText(mAnswerIsTrue ? R.string.true_button : R.string.false_button);
          setAnswerShownResult(true);
        }
      });
  }
}
