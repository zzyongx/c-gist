/**
You are given a string s and an array of strings words of the same length. Return all starting indices of substring(s) in s that is a concatenation of each word in words exactly once, in any order, and without any intervening characters.

You can return the answer in any order.



Example 1:

Input: s = "barfoothefoobarman", words = ["foo","bar"]
Output: [0,9]
Explanation: Substrings starting at index 0 and 9 are "barfoo" and "foobar" respectively.
The output order does not matter, returning [9,0] is fine too.

Example 2:

Input: s = "wordgoodgoodgoodbestword", words = ["word","good","best","word"]
Output: []

Example 3:

Input: s = "barfoofoobarthefoobarman", words = ["bar","foo","the"]
Output: [6,9,12]



Constraints:

    1 <= s.length <= 104
    s consists of lower-case English letters.
    1 <= words.length <= 5000
    1 <= words[i].length <= 30
    words[i] consists of lower-case English letters.

**/

#include "test.h"
#include <unordered_map>
#include <unordered_set>
#include <string>
#include <vector>

using namespace std;

class Solution {
public:
  struct Word {
    size_t num;
    size_t cnt;

    Word() : num(1), cnt(0) {}
  };

  struct Words {
    size_t slen;

    size_t num;
    size_t cnt;
    unordered_map<string, Word> m;
    unordered_set<string> hits;

    Words(const vector<string> &words) : num(words.size()), cnt(0) {
      slen = 0;
      for (auto ite = words.begin(); ite != words.end(); ++ite) {
        auto pos = m.find(*ite);
        if (pos == m.end()) {
          m.insert({*ite, Word()});
        } else {
          pos->second.num++;
        }
        slen += ite->size();
      }
    }

    size_t strlen() const {
      return slen;
    }

    bool exist(const string &word) {
      auto pos = m.find(word);
      if (pos != m.end() && pos->second.cnt != pos->second.num) {
        ++(pos->second.cnt);
        ++cnt;
        hits.insert(word);
        return true;
      }
      return false;
    }

    bool allExist() const {
      return cnt == num;
    }

    void reset() {
      cnt = 0;
      for (auto ite = hits.begin(); ite != hits.end(); ++ite) {
        m[*ite].cnt = 0;
      }
      hits.clear();
    }
  };

  vector<int> findSubstring(string s, vector<string>& words) {
    vector<int> ret;
    if (words.empty()) return ret;

    unordered_set<int> blacklist;

    Words ws(words);
    size_t wlen = words[0].size();

    for (size_t left = 0; left < s.size(); ++left) {
      if (left + ws.strlen() > s.size()) break;

      ws.reset();
      for (size_t start = left; start < s.size(); start+=wlen) {
        if (!ws.exist(s.substr(start, wlen))) break;

        if (ws.allExist()) {
          ret.push_back(left);
          break;
        }
      }
    }

    return ret;
  }
};


int main() {
  Solution solution;
  {
    string s = "barfoothefoobarman";
    vector<string> words = {"foo", "bar"};

    auto got = solution.findSubstring(s, words);
    vector<int> want = {0, 9};

    if (want != got) {
      fatal("case1", want, got);
    }
  }

  {
    string s = "wordgoodgoodgoodbestword";
    vector<string> words = {"word","good","best","word"};

    auto got = solution.findSubstring(s, words);
    vector<int> want;

    if (want != got) {
      fatal("case1", want, got);
    }
  }

  {
    string s = "barfoofoobarthefoobarman";
    vector<string> words = {"bar","foo","the"};

    auto got = solution.findSubstring(s, words);
    vector<int> want = {6,9,12};

    if (want != got) {
      fatal("case3", want, got);
    }
  }

  {
    string s = "wordgoodgoodgoodbestword";
    vector<string> words = {"word","good","best","good"};

    auto got = solution.findSubstring(s, words);
    vector<int> want = {8};

    if (want != got) {
      fatal("case4", want, got);
    }
  }

  {
    string s = "aaaaaaaa";
    vector<string> words = {"aa","aa","aa"};

    auto got = solution.findSubstring(s, words);
    vector<int> want = {0,1,2};

    if (want != got) {
      fatal("case5", want, got);
    }
  }

  return 0;
}
