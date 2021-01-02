/**
Given two strings s and t, return the minimum window in s which will contain all the characters in t. If there is no such window in s that covers all characters in t, return the empty string "".

Note that If there is such a window, it is guaranteed that there will always be only one unique minimum window in s.



Example 1:

Input: s = "ADOBECODEBANC", t = "ABC"
Output: "BANC"

Example 2:

Input: s = "a", t = "a"
Output: "a"



Constraints:

    1 <= s.length, t.length <= 105
    s and t consist of English letters.


Follow up: Could you find an algorithm that runs in O(n) time?
**/

#include "test.h"
#include <cassert>
#include <string>
#include <queue>
#include <unordered_map>
using namespace std;

class Solution {
public:
  struct CharStat {
    queue<size_t> positions;
    size_t num;

    CharStat() : num(1) {}

    bool fullFill() const {
      return positions.size() == num;
    }

    void add(int i) {
      positions.push(i);
      if (positions.size() > num) {
        positions.pop();
      }
    }

    void remove() {
      assert(!positions.empty());
      positions.pop();
    }

    size_t left() {
      assert(!positions.empty());
      return positions.front();
    }

  };

  string minWindow(string s, string t) {
    if (t.size() > s.size()) return "";

    unordered_map<char, CharStat> m;
    for (auto ite = t.begin(); ite != t.end(); ++ite) {
      auto pos = m.find(*ite);
      if (pos == m.end()) {
        m.insert(make_pair(*ite, CharStat()));
      } else {
        pos->second.num++;
      }
    }

    size_t start = 0, end = 0;
    size_t minWin = s.size();

    size_t matchCnt = 0;

    for (size_t i = 0; i < s.size(); ++i) {
      auto pos = m.find(s[i]);
      if (pos == m.end()) continue;

      if (pos->second.fullFill()) {
        pos->second.add(i);
      } else {
        pos->second.add(i);
        matchCnt++;

        if (matchCnt == t.size()) {  // window cover
          // get window size
          size_t left = s.size();
          for (auto ite = m.begin(); ite != m.end(); ++ite) {
            // std::cout << ite->first << " " << ite->second.string() << "\n";
            if (ite->second.left() < left) left = ite->second.left();
          }

          if (i+1 - left <= minWin) {
            minWin = i+1-left;
            start = left;
            end = i+1;
          }

          m[s[left]].remove();
          matchCnt--;
        }
      }
    }

    return s.substr(start, end - start);
  }
};

int main() {
  Solution solution;

  {
    string s = "ADOBECODEBANC";
    string t = "ABC";
    string got = solution.minWindow(s, t);

    string want = "BANC";
    if (got != want) fatal("case1", want, got);
  }
  {
    string s = "a";
    string t = "a";
    string got = solution.minWindow(s, t);

    string want = "a";
    if (got != want) fatal("case2", want, got);
  }
  {
    string s = "a";
    string t = "aa";
    string got = solution.minWindow(s, t);

    string want = "";
    if (got != want) fatal("case3", want, got);
  }

  {
    string s = "aa";
    string t = "aa";
    string got = solution.minWindow(s, t);

    string want = "aa";
    if (got != want) fatal("case4", want, got);
  }
}
