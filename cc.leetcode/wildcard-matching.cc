/**
Given an input string (s) and a pattern (p), implement wildcard pattern matching with support for '?' and '*' where:

    '?' Matches any single character.
    '*' Matches any sequence of characters (including the empty sequence).

The matching should cover the entire input string (not partial).



Example 1:

Input: s = "aa", p = "a"
Output: false
Explanation: "a" does not match the entire string "aa".

Example 2:

Input: s = "aa", p = "*"
Output: true
Explanation: '*' matches any sequence.

Example 3:

Input: s = "cb", p = "?a"
Output: false
Explanation: '?' matches 'c', but the second letter is 'a', which does not match 'b'.

Example 4:

Input: s = "adceb", p = "*a*b"
Output: true
Explanation: The first '*' matches the empty sequence, while the second '*' matches the substring "dce".

Example 5:

Input: s = "acdcb", p = "a*c?b"
Output: false



Constraints:

    0 <= s.length, p.length <= 2000
    s contains only lowercase English letters.
    p contains only lowercase English letters, '?' or '*'.
**/

#include "test.h"
#include <string>
#include <vector>

using namespace std;

class Solution {
public:
  /* 假设 dp[i,j] 表示 s[0:i] 和 p[0:j] 是否匹配
   * 如果s[i] == p[j] || p[j] == '?' 则 dp[i,j] = dp[i-1,j-1]
   *
   * 如果p[j] == '*' 分两种情况
   * 使用*，dp[i][j] = dp[i-1][j]
   * 不使用*, dp[i][j] = dp[i][j-1]
   *
   */
  bool isMatch(string s, string p) {
    size_t m = s.size();
    size_t n = p.size();

    vector<vector<bool>> dp(m+1, vector<bool>(n+1, false));
    dp[0][0] = true;  // 空字符串匹配空模式
    // dp[i][0] = false; 空模式不匹配任何字符串，默认值
    // dp[0][j] 只有*才能匹配空字符串
    for (size_t j = 1; j <= n; ++j) {
      if (p[j-1] == '*') {
        dp[0][j] = true;
      } else {
        break;
      }
    }

    for (size_t i = 1; i <= m; ++i) {
      for (size_t j = 1; j <= n; ++j) {
        if (s[i-1] == p[j-1] || p[j-1] == '?') {
          dp[i][j] = dp[i-1][j-1];
        } else if (p[j-1] == '*') {
          dp[i][j] = dp[i][j-1] || dp[i-1][j];
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
      fatal("case1", false, true);
    }
  }
  {
    auto match = solution.isMatch("aa", "*");
    if (!match) {
      fatal("case2", true, false);
    }
  }
  {
    auto match = solution.isMatch("cb", "?a");
    if (match) {
      fatal("case3", false, true);
    }
  }
  {
    auto match = solution.isMatch("adceb", "*a*b");
    if (!match) {
      fatal("case4", true, false);
    }
  }
  {
    auto match = solution.isMatch("acdcb", "a*c?b");
    if (match) {
      fatal("case5", false, true);
    }
  }
  return 0;
}
