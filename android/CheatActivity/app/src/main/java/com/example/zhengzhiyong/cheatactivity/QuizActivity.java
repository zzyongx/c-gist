package com.example.zhengzhiyong.cheatactivity;

import android.support.v7.app.AppCompatActivity;
import android.content.Intent;
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
  static final String KEY_CHEATER = "Cheater";
  
  Button mTrueButton;
  Button mFalseButton;
  Button mNextButton;
  Button mCheatButton;
  TextView mQuestionTextView;

  TrueFalse[] mAnswerKey = new TrueFalse[] {
    new TrueFalse(R.string.question_oceans, true),
    new TrueFalse(R.string.question_mideast, false),
    new TrueFalse(R.string.question_africa, false),
    new TrueFalse(R.string.question_americas, true),
    new TrueFalse(R.string.question_asia, true)
  };

  boolean[] mIsCheater = new boolean[mAnswerKey.length];

  int mCurrentIndex = 0;
  void updateQuestion() {
    int question = mAnswerKey[mCurrentIndex].getQuestion();
    mQuestionTextView.setText(question);
  }

  void checkAnswer(boolean userPressedTrue) {
    boolean answerIsTrue = mAnswerKey[mCurrentIndex].isTrueQuestion();
    
    int messageResId;
    if (mIsCheater[mCurrentIndex]) {
      messageResId = userPressedTrue == answerIsTrue ?
        R.string.judgment_toast : R.string.incorrect_judgement_toast;
    } else {
      messageResId = userPressedTrue == answerIsTrue ?
        R.string.correct_toast : R.string.incorrect_toast;
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

    mCheatButton = (Button) findViewById(R.id.cheat_button);
    mCheatButton.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          Log.d(TAG, "cheat button click");
          
          Intent i = new Intent(QuizActivity.this, CheatActivity.class);
          Log.d(TAG, "intent cheated");
          boolean answerIsTrue = mAnswerKey[mCurrentIndex].isTrueQuestion();
          i.putExtra(CheatActivity.EXTRA_ANSWER_IS_TRUE, answerIsTrue);
          startActivityForResult(i, 0);
        }
      });

    if (savedInstanceState != null) {
      mCurrentIndex = savedInstanceState.getInt(KEY_INDEX, 0);
      mIsCheater = savedInstanceState.getBooleanArray(KEY_CHEATER);
    }

    updateQuestion();
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    mIsCheater[mCurrentIndex] = data.getBooleanExtra(CheatActivity.EXTRA_ANSWER_SHOWN, false);
  }

  @Override
  public void onSaveInstanceState(Bundle savedInstanceState) {
    super.onSaveInstanceState(savedInstanceState);
    Log.i(TAG, "onSaveInstanceState");
    savedInstanceState.putInt(KEY_INDEX, mCurrentIndex);
    savedInstanceState.putBooleanArray(KEY_CHEATER, mIsCheater);
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
