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
#include <vector>

using namespace std;

// 回溯法不能解
class Solution1 {
public:
  bool isMatch(string s, string p) {
    size_t i = 0, j = 0;
    size_t starLen = 0;
    do {
      if (i == s.size() && j == p.size()) {
        break;
      }

      if (i < s.size() && j < p.size()) {
        if (s[i] == p[j] || p[j] == '.') {
          i++;

          if ((j+1 < p.size() && p[j+1] == '*')) {
            starLen++;
          } else {
            j++;
            starLen = 0;
          }
          continue;
        }
      }

      // 不匹配，跳过通配
      if (j+1 < p.size() && p[j+1] == '*') {
        j+=2;
        continue;
      }

      std::cout << "notmatch: " << s.substr(i) << " " << p.substr(j) << " " << starLen <<"\n";

      // 不匹配 && 能够回溯
      if (starLen > 0 && j < p.size()) {
        p = p.substr(j);
        for (size_t back = 1; back <= starLen; ++back) {
          std::cout << "backtrace: " << s.substr(i-back) << " " << p << "\n";
          if (isMatch(s.substr(i-back), p)) {
            return true;
          }
        }
        return false;
      }
      break;
    } while (true);

    // 未使用的通配符
    if (j+1 < p.size() && p[j+1] == '*') {
      j+=2;
    }

    return i == s.size() && j == p.size();
  }
};

class Solution {
public:
  // dp[i,j] 表示 s[0:i] 和 p[0:j] 是否匹配
  // 如果s[i] == p[j] || p[j] ==
  struct PatternChar {
    char c;
    bool star;
  };

  bool isMatch(string s, string p) {
    vector<PatternChar> pp;
    for (size_t j = 0; j < p.size(); ++j) {
      PatternChar pc = {.c = p[j], .star = false};
      if (j + 1 < p.size() && p[j+1] == '*') {
        pc.star = true;
        ++j;
      }
      pp.push_back(pc);
    }

    size_t m = s.size();
    size_t n = pp.size();
    vector<vector<bool>> dp(m+1, vector<bool>(n+1));

    dp[0][0] = true;
    // dp[i][0] = false; 空模式不匹配任何串
    // 空字符串匹配 .* x*
    for (size_t j = 1; j < n+1; ++j) {
      if (pp[j-1].star) {
        dp[0][j] = true;
      } else {
        break;
      }
    }

    for (size_t i = 1; i < m+1; ++i) {
      for (size_t j = 1; j < n+1; ++j) {
        if (pp[j-1].star) {
          auto match = (s[i-1] == pp[j-1].c || pp[j-1].c == '.');
          dp[i][j] = (match && dp[i-1][j]) || dp[i][j-1];
        } else if (s[i-1] == pp[j-1].c || pp[j-1].c == '.') {
          dp[i][j] = dp[i-1][j-1];
        } else {
          dp[i][j] = false;
        }
      }
    }

    return dp[m][n];
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
    auto match = solution.isMatch("abbbcd", "ab*bbbcd");
    if (!match) {
      fatal("case16", true, false);
    }
  }

  {
    auto match = solution.isMatch("aaba", "ab*a*c*a");
    if (match) {
      fatal("case17", false, true);
    }
  }

  {
    // 这个case说明只简单的回溯s不够，还得回溯p
    // 这个case最后用到的p是 .b.*
    auto match = solution.isMatch(
      "bbcacbabbcbaaccabc", "b*a*a*.c*bb*b*.*.*");
    if (!match) {
      fatal("case18", true, false);
    }
  }
}
