/**
Given a non-empty string s and a dictionary wordDict containing a list of non-empty words, add spaces in s to construct a sentence where each word is a valid dictionary word. Return all such possible sentences.

Note:

    The same word in the dictionary may be reused multiple times in the segmentation.
    You may assume the dictionary does not contain duplicate words.

Example 1:

Input:
s = "catsanddog"
wordDict = ["cat", "cats", "and", "sand", "dog"]
Output:
[
  "cats and dog",
  "cat sand dog"
]

Example 2:

Input:
s = "pineapplepenapple"
wordDict = ["apple", "pen", "applepen", "pine", "pineapple"]
Output:
[
  "pine apple pen apple",
  "pineapple pen apple",
  "pine applepen apple"
]
Explanation: Note that you are allowed to reuse a dictionary word.

Example 3:

Input:
s = "catsandog"
wordDict = ["cats", "dog", "sand", "and", "cat"]
Output:
[]

**/

#include "test.h"
#include <string>
#include <vector>
#include <unordered_map>
#include <unordered_set>

using namespace std;

class Solution1 {
public:
  vector<string> wordBreak(const string &s, size_t start, const unordered_set<string>& dict) {
    vector<string> sentences;
    for (size_t i = start; i < s.size(); ++i) {
      string word = s.substr(start, i+1-start);
      if (dict.count(word) > 0) {
        if (i+1 < s.size()) {
          auto ret = wordBreak(s, i+1, dict);
          if (!ret.empty()) {
            for (auto ite = ret.begin(); ite != ret.end(); ++ite) {
              sentences.push_back(word + " " + *ite);
            }
          }
        } else {
          sentences.push_back(word);
        }
      }
    }
    return sentences;
  }

  vector<string> wordBreak(string s, vector<string>& wordDict) {
    unordered_set<string> dict(wordDict.begin(), wordDict.end());
    return wordBreak(s, 0, dict);
  }
};

class Solution {
private:
  unordered_map<size_t, vector<string>> cache;

public:
  vector<string> wordBreak(string s, size_t start, const unordered_set<string>& dict) {
    vector<string> sentences;
    for (size_t i = start; i < s.size(); ++i) {
      string word = s.substr(start, i+1-start);
      if (dict.count(word) > 0) {
        if (i+1 < s.size()) {
          auto pos = cache.find(i+1);
          if (pos == cache.end()) {
            auto ret = wordBreak(s, i+1, dict);
            pos = cache.insert({i+1, ret}).first;
          }

          auto ret = pos->second;
          if (!ret.empty()) {
            for (auto ite = ret.begin(); ite != ret.end(); ++ite) {
              sentences.push_back(word + " " + *ite);
            }
          }
        } else {
          sentences.push_back(word);
        }
      }
    }
    return sentences;
  }

  vector<string> wordBreak(string s, vector<string>& wordDict) {
    cache.clear();
    unordered_set<string> dict(wordDict.begin(), wordDict.end());
    return wordBreak(s, 0, dict);
  }
};


int main() {
  Solution solution;

  {
    string s = "catsanddog";
    vector<string> dict = {"cat", "cats", "and", "sand", "dog"};

    auto got = solution.wordBreak(s, dict);
    vector<string> want = {
      "cats and dog",
      "cat sand dog",
    };

    if (vsort(got) != vsort(want)) {
      fatal("case1", want, got, "$$\n");
    }
  }

  {
    string s = "pineapplepenapple";
    vector<string> dict = {"apple", "pen", "applepen",
                           "pine", "pineapple"};

    auto got = solution.wordBreak(s, dict);
    vector<string> want = {
      "pine apple pen apple",
      "pineapple pen apple",
      "pine applepen apple",
    };

    if (vsort(got) != vsort(want)) {
      fatal("case2", want, got, "$$\n");
    }
  }

  {
    string s = "catsandog";
    vector<string> dict = {"cats", "dog", "sand", "and", "cat"};

    auto got = solution.wordBreak(s, dict);
    vector<string> want;

    if (vsort(got) != vsort(want)) {
      fatal("case3", want, got);
    }
  }

  return 0;
}
