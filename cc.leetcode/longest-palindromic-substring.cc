/**
Given a string s, return the longest palindromic substring in s.



Example 1:

Input: s = "babad"
Output: "bab"
Note: "aba" is also a valid answer.

Example 2:

Input: s = "cbbd"
Output: "bb"

Example 3:

Input: s = "a"
Output: "a"

Example 4:

Input: s = "ac"
Output: "a"



Constraints:

    1 <= s.length <= 1000
    s consist of only digits and English letters (lower-case and/or upper-case),

**/

#include "test.h"
#include <string>
using namespace std;

class Solution1 {
public:
  bool isPalindrome(const string &s, size_t left, size_t right) {
    while (left < right) {
      if (s[left++] != s[--right]) return false;
    }
    return true;
  }

  // 每次从最长的计算，例如判断s[0,9] == false，对判断s[1,8]是否为false没有帮助，
  // 仍然需要循环判断，这里就存在重复计算
  // 如果从最短的开始计算，s[1,8]如果为true，则只需要判断s[0]是否等于s[9]即可。

  // 缓存可以提升空间换时间，但得先找到重复计算，才能缓存
  string longestPalindrome(string s) {
    size_t left = 0;
    size_t right = 0;
    for (size_t i = 0; i < s.size(); ++i) {
      if (s.size() - i < right - left) break;

      for (size_t j = s.size(); j > i; --j) {
        if (j - i < right - left) break;

        if (isPalindrome(s, i, j) && j - i > right - left) {
          left = i;
          right = j;
        }
      }
    }
    return s.substr(left, right - left);
  }
};

class Solution {
public:
  // s[i,j] == ture, if s[i] == s[j] && s[i+1,j-1] == true
  // 0            => s[0,0] = true
  // 0 1          => s[0,1] = s[0] == s[1]
  // 0 1 2        => s[0,2] = s[0] == s[2]
  // 0 1 2 3      => s[0,3] = s[0] == s[3] && s[1,2]
  // 0 1 2 3 4    => s[0,4] = s[0] == s[4] && s[1,3]
  // 0 1 2 3 4 5  => s[0,5] = s[0] == s[5] && s[1,4]

  string longestPalindrome(string s) {
    size_t left = 0;
    size_t right = 0;
    vector<vector<bool>> cache(n, vector<bool>(n, false));

    for (size_t i = 0; i < s.size(); i++) {
      for (size_t j = 0; j < i; j++) {
        if (s[i] == s[j]

      }
      if (s.size() - i < right - left) break;

      for (size_t j = s.size(); j > i; --j) {

      }
    }
  }
};

class Solution2 {
public:
  pair<int, int> expendCenter(const string &s, int left, int right) {
    while (left >= 0 && right < int(s.size()) && s[left] == s[right]) {
      --left;
      ++right;
    }
    return {left+1, right-1};
  }

  // O(n^2)
  string longestPalindrome(string s) {
    int left = 0, right = 0;
    for (size_t i = 0; i < s.size(); ++i) {
      for (size_t j : {0,1}) { // odd, even
        auto [l, r] = expendCenter(s, i, i+j);
        if (r-l > right - left) {
          left = l;
          right = r;
        }
      }
    }
    return s.substr(left, right+1 - left);
  }
};

int main() {
  Solution solution;

  {
    string s = "babad";
    auto got = solution.longestPalindrome(s);
    string want = "bab";

    if (got != want) {
      fatal("case1", want, got);
    }
  }
  {
    string s = "cbbd";
    auto got = solution.longestPalindrome(s);
    string want = "bb";

    if (got != want) {
      fatal("case2", want, got);
    }
  }
  {
    string s = "a";
    auto got = solution.longestPalindrome(s);
    string want = "a";

    if (got != want) {
      fatal("case3", want, got);
    }
  }
  {
    string s = "ac";
    auto got = solution.longestPalindrome(s);
    string want = "a";

    if (got != want) {
      fatal("case4", want, got);
    }
  }
}
