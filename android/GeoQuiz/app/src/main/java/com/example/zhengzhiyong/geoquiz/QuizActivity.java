package com.example.zhengzhiyong.geoquiz;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class QuizActivity extends AppCompatActivity {
  static final String TAG = "QuizActivity";
  static final String KEY_INDEX = "QuestionIndex";
  
  Button mTrueButton;
  Button mFalseButton;
  Button mNextButton;
  TextView mQuestionTextView;

  TrueFalse[] mAnswerKey = new TrueFalse[] {
    new TrueFalse(R.string.question_oceans, true),
    new TrueFalse(R.string.question_mideast, false),
    new TrueFalse(R.string.question_africa, false),
    new TrueFalse(R.string.question_americas, true),
    new TrueFalse(R.string.question_asia, true)
  };

  int mCurrentIndex = 0;
  void updateQuestion() {
    int question = mAnswerKey[mCurrentIndex].getQuestion();
    mQuestionTextView.setText(question);
  }

  void checkAnswer(boolean userPressedTrue) {
    boolean answerIsTrue = mAnswerKey[mCurrentIndex].isTrueQuestion();
    
    int messageResId;
    if (userPressedTrue == answerIsTrue) {
      messageResId = R.string.correct_toast;
    } else {
      messageResId = R.string.incorrect_toast;
    }

    Toast.makeText(this, messageResId, Toast.LENGTH_SHORT).show();
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Log.d(TAG, "onCreate()");
    setContentView(R.layout.activity_quiz);

    mQuestionTextView = (TextView) findViewById(R.id.question_text_view);

    mTrueButton = (Button) findViewById(R.id.true_button);
    mTrueButton.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          checkAnswer(true);
        }
      });

    mFalseButton = (Button) findViewById(R.id.false_button);
    mFalseButton.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          checkAnswer(false);
        }
      });

    mNextButton = (Button) findViewById(R.id.next_button);
    mNextButton.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          mCurrentIndex = (mCurrentIndex+1) % mAnswerKey.length;
          updateQuestion();
        }
      });

    if (savedInstanceState != null) {
      mCurrentIndex = savedInstanceState.getInt(KEY_INDEX, 0);
    }

    updateQuestion();
  }

  @Override
  public void onSaveInstanceState(Bundle savedInstanceState) {
    super.onSaveInstanceState(savedInstanceState);
    Log.i(TAG, "onSaveInstanceState");
    savedInstanceState.putInt(KEY_INDEX, mCurrentIndex);
  }

  @Override
  public void onStart() {
    super.onStart();
    Log.i(TAG, "onStart()");
  }

  @Override
  public void onResume() {
    super.onResume();
    Log.i(TAG, "onResume()");
  }

  @Override
  public void onPause() {
    super.onPause();
    Log.i(TAG, "onPause()");
  }

  @Override
  public void onStop() {
    super.onStop();
    Log.i(TAG, "onStop()");
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    Log.i(TAG, "onDestroy()");
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.activity_quiz, menu);
    return true;
  }
}
