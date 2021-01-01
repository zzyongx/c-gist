/**
Given two sorted arrays nums1 and nums2 of size m and n respectively, return the median of the two sorted arrays.

Follow up: The overall run time complexity should be O(log (m+n)).

Example 1:

Input: nums1 = [1,3], nums2 = [2]
Output: 2.00000
Explanation: merged array = [1,2,3] and median is 2.

Example 2:

Input: nums1 = [1,2], nums2 = [3,4]
Output: 2.50000
Explanation: merged array = [1,2,3,4] and median is (2 + 3) / 2 = 2.5.

Example 3:

Input: nums1 = [0,0], nums2 = [0,0]
Output: 0.00000

Example 4:

Input: nums1 = [], nums2 = [1]
Output: 1.00000

Example 5:

Input: nums1 = [2], nums2 = []
Output: 2.00000

**/

/**
 * log(m) + log(n) = log(mn) > log(m+n)
 */

#include <cstddef>
#include <cassert>
#include <vector>
#include <iostream>

using std::vector;
using std::size_t;

/* log(n) */
size_t binSearch(const vector<int> &nums, int num) {
  size_t left = 0;
  size_t right = nums.size();

  /* [x y)
   * = x return 0
   * > x return 0+1
   * < x return 0
   */
  while (left < right) {
    auto m = left + (right - left)/2;
    if (nums[m] == num) {
      return m;
    } else if (num > nums[m]) {
      left = m+1;
    } else {
      right = m;
    }
  }
  return left;
}

class Solution {
private:
  int topkInFirst(vector<int> &nums1, vector<int>& nums2, size_t k) {
    size_t left = 0;
    size_t right = nums1.size();

    size_t m = 0;
    while (left < right) {
      m = left + (right - left) / 2;
      size_t pos = binSearch(nums2, nums1[m]);
      if (m + pos == k) {
        return m;
      } else if (m + pos > k) {
        right = m;
      } else {
        left = m+1;
      }
    }
    return -1;
  }

  int findMedianSortedArraysImpl(vector<int> &nums1, vector<int>& nums2, size_t k) {
    int p = topkInFirst(nums1, nums2, k);
    if (p >= 0) return nums1[p];

    p = topkInFirst(nums2, nums1, k);
    if (p < 0) p = 0;
    return nums2[p];
  }

public:
  double findMedianSortedArrays(vector<int>& nums1, vector<int>& nums2) {
    size_t total = nums1.size() + nums2.size();
    size_t middle = total / 2;
    if (middle * 2 == total) { // even
      auto x1 = findMedianSortedArraysImpl(nums1, nums2, middle);
      auto x2 = findMedianSortedArraysImpl(nums1, nums2, middle-1);
      return (x1+x2)/double(2);
    } else {
      return findMedianSortedArraysImpl(nums1, nums2, middle);
    }
  }
};

#define ERR(test, want, got) \
  do {std::cerr << test <<  " ERROR: want " << want << ", got " << got << std::endl; exit(1); } while(0)

int main() {
  {
    std::cout << "binSearch:\n";

    vector<int> nums = {1,3};
    auto pos = binSearch(nums, 2);
    if (pos != 1) ERR("case1", 1, pos);

    nums = {1};
    pos = binSearch(nums, 1);
    if (pos != 0) ERR("case2", 0, pos);
  }

  Solution solution;
  {
    vector<int> nums1 = {1,3};
    vector<int> nums2 = {2};

    auto m = solution.findMedianSortedArrays(nums1, nums2);
    if (m != 2) ERR("case1", 2, m);
  }

  {
    vector<int> nums1 = {1,2};
    vector<int> nums2 = {3,4};

    auto m = solution.findMedianSortedArrays(nums1, nums2);
    if (m != 2.5) ERR("case2", 2.5, m);
  }

  {
    vector<int> nums1 = {0,0};
    vector<int> nums2 = {0,0};

    auto m = solution.findMedianSortedArrays(nums1, nums2);
    if (m != 0.0) ERR("case3", 0.0, m);
  }

  {
    vector<int> nums1 = {};
    vector<int> nums2 = {2};

    auto m = solution.findMedianSortedArrays(nums1, nums2);
    if (m != 2.0) ERR("case4", 2.0, m);
  }

  {
    vector<int> nums1 = {2};
    vector<int> nums2 = {};

    auto m = solution.findMedianSortedArrays(nums1, nums2);
    if (m != 2.0) ERR("case5", 2.0, m);
  }

  {
    vector<int> nums1 = {1};
    vector<int> nums2 = {1};

    auto m = solution.findMedianSortedArrays(nums1, nums2);
    if (m != 1.0) ERR("case6", 1.0, m);
  }

  {
    vector<int> nums1 = {1,2};
    vector<int> nums2 = {1,2};

    auto m = solution.findMedianSortedArrays(nums1, nums2);
    if (m != 1.5) ERR("case7", 1.5, m);
  }


  return 0;
}
