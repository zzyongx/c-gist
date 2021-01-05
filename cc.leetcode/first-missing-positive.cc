/**
Given an unsorted integer array nums, find the smallest missing positive integer.

Follow up: Could you implement an algorithm that runs in O(n) time and uses constant extra space.?



Example 1:

Input: nums = [1,2,0]
Output: 3

Example 2:

Input: nums = [3,4,-1,1]
Output: 2

Example 3:

Input: nums = [7,8,9,11,12]
Output: 1
**/

#include "test.h"
#include <vector>

using std::vector;

// 假设数组A，长度为N，设第一个缺失的正整数为x
// 则x的范围是[1, N+1]，这点最关键
// 例如长度为4的数组，f({1,2,3,4}) = 5, f({1,-1,4,7}) = 2

inline void swap(vector<int> &v, int i, int j) {
  int t = v[i];
  v[i] = v[j];
  v[j] = t;
}

class Solution {
public:
  bool validMissingPositive(const vector<int> &nums, int i) {
    return nums[i] > 0 && nums[i] <= int(nums.size());
  }

  int firstMissingPositive(vector<int> &nums) {
    for (size_t i = 0; i < nums.size(); i++) {
      // 把Y放到第Y-1个位置，除非它不在取值范围[1,nums.size()]
      // 如果第Y-1个位置已经等于Y，放弃，说明出现了重复数字
      while (nums[i] != int(i+1) && validMissingPositive(nums, i)) {
        if (nums[nums[i]-1] == nums[i]) break;
        else swap(nums, i, nums[i]-1);
      }
    }

    for (size_t i = 0; i < nums.size(); i++) {
      if (nums[i] != int(i+1) || !validMissingPositive(nums, i)) {
        return i+1;
      }
    }
    return nums.size()+1;
  }
};

int main() {
  Solution solution;
  {
    vector<int> nums = {1,2,0};
    int want = 3;

    int got = solution.firstMissingPositive(nums);
    if (got != want) {
      fatal("case1", want, got);
    }
  }

  {
    vector<int> nums = {3,4,-1,1};
    int want = 2;

    int got = solution.firstMissingPositive(nums);
    if (got != want) {
      fatal("case2", want, got);
    }
  }

  {
    vector<int> nums = {7,8,9,11,12};
    int want = 1;

    int got = solution.firstMissingPositive(nums);
    if (got != want) {
      fatal("case3", want, got);
    }
  }

  {
    vector<int> nums = {1,1};
    int want = 2;

    int got = solution.firstMissingPositive(nums);
    if (got != want) {
      fatal("case4", want, got);
    }
  }

  return 0;
}
