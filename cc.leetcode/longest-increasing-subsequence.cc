/**
Given an integer array nums, return the length of the longest strictly increasing subsequence.

A subsequence is a sequence that can be derived from an array by deleting some or no elements without changing the order of the remaining elements. For example, [3,6,2,7] is a subsequence of the array [0,3,1,6,2,2,7].



Example 1:

Input: nums = [10,9,2,5,3,7,101,18]
Output: 4
Explanation: The longest increasing subsequence is [2,3,7,101], therefore the length is 4.

Example 2:

Input: nums = [0,1,0,3,2,3]
Output: 4

Example 3:

Input: nums = [7,7,7,7,7,7,7]
Output: 1



Constraints:

    1 <= nums.length <= 2500
    -104 <= nums[i] <= 104



Follow up:

    Could you come up with the O(n2) solution?
    Could you improve it to O(n log(n)) time complexity?
**/

#include "test.h"
#include <vector>
#include <algorithm>

using namespace std;

// 假设dp[j]是以nums[j]结尾的LIS的长度，j>=0 and j<i
// 则以dp[i]的值是 max(dp[j]+1 if nums[i] > nums[j])

// nums = {10, 9, 2, 5, 3, 7, 101, 18}
//  10 dp[0] = 1 {10}
//   9 dp[1] = 1 {9}
//   2 dp[2] = 1 {2}
//   5 dp[3] = 2 {2,5}
//   3 dp[4] = 2 {2,3}
//   7 dp[5] = 3 {2,5,7},{2,3,7}
// 101 dp[6] = 4 {2,5,7,101},{2,3,7,101}
//  18 dp[7] = 4 {2,5,7,18}, {2,3,7,18}

class Solution {
public:
  int lengthOfLIS(vector<int>& nums) {
    auto dp = vector<int>(nums.size(), 0);

    for (size_t i = 0; i < nums.size(); ++i) {
      int max = 0;
      for (size_t j = 0; j < i; ++j) {
        if (nums[i] > nums[j] && max < dp[j]) {
          max = dp[j];
        }
      }
      dp[i] = max+1;
    }

    return *max_element(dp.begin(), dp.end());
  }
};

int main() {
  Solution solution;

  {
    vector<int> nums = {10,9,2,5,3,7,101,18};
    auto got = solution.lengthOfLIS(nums);

    auto want = 4;
    if (got != want) {
      fatal("case1", want, got);
    }
  }

  {
    vector<int> nums = {0,1,0,3,2,3};
    auto got = solution.lengthOfLIS(nums);

    auto want = 4;
    if (got != want) {
      fatal("case2", want, got);
    }
  }

  {
    vector<int> nums = {7,7,7,7,7,7,7};
    auto got = solution.lengthOfLIS(nums);

    auto want = 1;
    if (got != want) {
      fatal("case3", want, got);
    }
  }

  return 0;
}
