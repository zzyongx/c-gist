/**
Given an input string (s) and a pattern (p), implement regular expression matching with support for '.' and '*' where:

    '.' Matches any single character.
    '*' Matches zero or more of the preceding element.

The matching should cover the entire input string (not partial).



Example 1:

Input: s = "aa", p = "a"
Output: false
Explanation: "a" does not match the entire string "aa".

Example 2:

Input: s = "aa", p = "a*"
Output: true
Explanation: '*' means zero or more of the preceding element, 'a'. Therefore, by repeating 'a' once, it becomes "aa".

Example 3:

Input: s = "ab", p = ".*"
Output: true
Explanation: ".*" means "zero or more (*) of any character (.)".

Example 4:

Input: s = "aab", p = "c*a*b"
Output: true
Explanation: c can be repeated 0 times, a can be repeated 1 time. Therefore, it matches "aab".

Example 5:

Input: s = "mississippi", p = "mis*is*p*."
Output: false



Constraints:

    0 <= s.length <= 20
    0 <= p.length <= 30
    s contains only lowercase English letters.
    p contains only lowercase English letters, '.', and '*'.
    It is guaranteed for each appearance of the character '*', there will be a previous valid character to match.


**/

#include "test.h"
#include <cassert>
#include <string>

using namespace std;

class Solution {
public:
  bool isMatch(string s, string p) {
    size_t i = 0, j = 0;
    size_t backTrackStart = 0;
    while (i < s.size() && j < p.size()) {
      if (s[i] == p[j] || p[j] == '.') {
        i++;
        if ((j+1 < p.size() && p[j+1] == '*')) {
          if (p[j] == '.') i = s.size(); // ready to backtrace
        } else {
          backTrackStart = i;
          j++;
        }
      } else {
        if (j+1 < p.size() && p[j+1] == '*') {
          j+=2;
        } else {
          break;
        }
      }
    }

    std::cout << i << " " << j << "\n";

    // backtrack .*
    if (i == s.size() || (j+1 < p.size() && p[j+1] == '*')) {
      if (j+1 < p.size() && p[j+1] == '*') j += 2;

     std::cout << backTrackStart << "\n";
        while (j < p.size() && backTrackStart < s.size()) {
          std::cout << backTrackStart << "\n";
          std::cout << s.substr(backTrackStart) << "\n";
          std::cout << p.substr(j) << "\n\n";
          if (isMatch(s.substr(backTrackStart), p.substr(j))) {
            return true;
          }

          if (j+1 < p.size() && p[j+1] == '*') {
            j+=2;
          } else {
            backTrackStart++;
          }
        }

    }
    return i == s.size() && j == p.size();
  }
};

int main() {
  Solution solution;

  {
    auto match = solution.isMatch("aa", "a");
    if (match) {
      fatal("case1", false, match);
    }
  }

  {
    auto match = solution.isMatch("aa", "a*");
    if (!match) {
      fatal("case2", true, match);
    }
  }

  {
    auto match = solution.isMatch("ab", ".*");
    if (!match) {
      fatal("case3", true, match);
    }
  }

  {
    auto match = solution.isMatch("aab", "c*a*b");
    if (!match) {
      fatal("case4", true, match);
    }
  }

  {
    auto match = solution.isMatch("mississippi", "mis*is*p*.");
    if (match) {
      fatal("case5", false, match);
    }
  }

  {
    auto match = solution.isMatch("aaa", "a*a");
    if (!match) {
      fatal("case6", true, match);
    }
  }

  {
    auto match = solution.isMatch("aaa", "a*aaa");
    if (!match) {
      fatal("case7", true, match);
    }
  }

  {
    auto match = solution.isMatch("aaa", "aaaa");
    if (match) {
      fatal("case8", false, match);
    }
  }

  {
    auto match = solution.isMatch("abcd", ".*cd");
    if (!match) {
      fatal("case9", true, match);
    }
  }

  {
    auto match = solution.isMatch("abcdXcd", ".*cd");
    if (!match) {
      fatal("case10", true, match);
    }
  }

  {
    auto match = solution.isMatch("aaa", "ab*a*c*a");
    if (!match) {
      fatal("case11", true, match);
    }
  }

  {
    auto match = solution.isMatch("a", "ab*a");
    if (match) {
      fatal("case12", false, match);
    }
  }

  {
    auto match = solution.isMatch("ab", ".*..c*");
    if (!match) {
      fatal("case13", true, match);
    }
  }

  {
    auto match = solution.isMatch("a", ".*..a*");
    if (match) {
      fatal("case14", false, match);
    }
  }

  {
    auto match = solution.isMatch(
      "aasdfasdfasdfasdfas", "aasdf.*asdf.*asdf.*asdf.*s");
    if (!match) {
      fatal("case15", true, false);
    }
  }

  {
    std::cout << "case16\n";
    auto match = solution.isMatch("abbbcd", "ab*bbbcd");
    if (!match) {
      fatal("case16", true, false);
    }
  }
}
